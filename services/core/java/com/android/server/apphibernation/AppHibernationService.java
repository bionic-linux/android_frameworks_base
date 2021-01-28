/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.apphibernation;

import static android.content.Intent.ACTION_PACKAGE_ADDED;
import static android.content.Intent.ACTION_PACKAGE_REMOVED;
import static android.content.Intent.EXTRA_REMOVED_FOR_ALL_USERS;
import static android.content.Intent.EXTRA_REPLACING;
import static android.content.pm.PackageManager.MATCH_ALL;
import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.apphibernation.IAppHibernationService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemService;

import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * System service that manages app hibernation state, a state apps can enter that means they are
 * not being actively used and can be optimized for storage. The actual policy for determining
 * if an app should hibernate is managed by PermissionController code.
 */
public final class AppHibernationService extends SystemService {
    private static final String TAG = "AppHibernationService";

    /**
     * Lock for accessing any in-memory hibernation state
     */
    private final Object mLock = new Object();
    private final Context mContext;
    private final IPackageManager mIPackageManager;
    private final IActivityManager mIActivityManager;
    private final UserManager mUserManager;
    @GuardedBy("mLock")
    private final SparseArray<Map<String, UserLevelState>> mUserStates = new SparseArray<>();
    private final SparseArray<HibernationStateDiskStore<UserLevelState>> mUserDiskStores =
            new SparseArray<>();
    @GuardedBy("mLock")
    private final Set<String> mGloballyHibernatedPackages = new ArraySet<>();
    private final HibernationStateDiskStoreFactory<UserLevelState> mUserDiskStoreFactory;
    private final UserLevelHibernationProto mUserLevelHibernationProto =
            new UserLevelHibernationProto();
    private final ScheduledExecutorService mScheduledExecutor =
            Executors.newSingleThreadScheduledExecutor();

    /**
     * Initializes the system service.
     * <p>
     * Subclasses must define a single argument constructor that accepts the context
     * and passes it to super.
     * </p>
     *
     * @param context The system server context.
     */
    public AppHibernationService(@NonNull Context context) {
        this(context,
                IPackageManager.Stub.asInterface(ServiceManager.getService("package")),
                ActivityManager.getService(),
                context.getSystemService(UserManager.class),
                ((file, readWriter, executorService) ->
                        new HibernationStateDiskStore<>(file, readWriter, executorService)));
    }

    @VisibleForTesting
    AppHibernationService(@NonNull Context context, IPackageManager packageManager,
            IActivityManager activityManager, UserManager userManager,
            HibernationStateDiskStoreFactory<UserLevelState> userDiskStoreFactory) {
        super(context);
        mContext = context;
        mIPackageManager = packageManager;
        mIActivityManager = activityManager;
        mUserManager = userManager;
        mUserDiskStoreFactory = userDiskStoreFactory;

        final Context userAllContext = mContext.createContextAsUser(UserHandle.ALL, 0 /* flags */);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PACKAGE_ADDED);
        intentFilter.addAction(ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        userAllContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStart() {
        publishBinderService(Context.APP_HIBERNATION_SERVICE, mServiceStub);
    }

    /**
     * Whether a package is hibernating for a given user.
     *
     * @param packageName the package to check
     * @param userId the user to check
     * @return true if package is hibernating for the user
     */
    boolean isHibernatingForUser(String packageName, int userId) {
        userId = handleIncomingUser(userId, "isHibernating");
        if (!mUserManager.isUserUnlockingOrUnlocked(userId)) {
            Slog.e(TAG, "Attempt to get hibernation state of stopped or nonexistent user "
                    + userId);
            return false;
        }
        synchronized (mLock) {
            final Map<String, UserLevelState> packageStates = mUserStates.get(userId);
            final UserLevelState pkgState = packageStates.get(packageName);
            if (pkgState == null) {
                throw new IllegalArgumentException(
                        String.format("Package %s is not installed for user %s",
                                packageName, userId));
            }
            return pkgState.hibernated;
        }
    }

    /**
     * Whether a package is hibernated globally. This only occurs when a package is hibernating for
     * all users and allows us to make optimizations at the package or APK level.
     *
     * @param packageName package to check
     */
    boolean isHibernatingGlobally(String packageName) {
        synchronized (mLock) {
            return mGloballyHibernatedPackages.contains(packageName);
        }
    }

    /**
     * Set whether the package is hibernating for the given user.
     *
     * @param packageName package to modify state
     * @param userId user
     * @param isHibernating new hibernation state
     */
    void setHibernatingForUser(String packageName, int userId, boolean isHibernating) {
        userId = handleIncomingUser(userId, "setHibernating");
        if (!mUserManager.isUserUnlockingOrUnlocked(userId)) {
            Slog.w(TAG, "Attempt to set hibernation state for a stopped or nonexistent user "
                    + userId);
            return;
        }
        synchronized (mLock) {
            final Map<String, UserLevelState> packageStates = mUserStates.get(userId);
            final UserLevelState pkgState = packageStates.get(packageName);
            if (pkgState == null) {
                throw new IllegalArgumentException(
                        String.format("Package %s is not installed for user %s",
                                packageName, userId));
            }

            if (pkgState.hibernated == isHibernating) {
                return;
            }

            if (isHibernating) {
                hibernatePackageForUserL(packageName, userId, pkgState);
            } else {
                unhibernatePackageForUserL(packageName, userId, pkgState);
            }
        }
    }

    /**
     * Set whether the package should be hibernated globally at a package level, allowing the
     * the system to make optimizations at the package or APK level.
     *
     * @param packageName package to hibernate globally
     * @param isHibernating new hibernation state
     */
    void setHibernatingGlobally(String packageName, boolean isHibernating) {
        if (isHibernating != mGloballyHibernatedPackages.contains(packageName)) {
            synchronized (mLock) {
                if (isHibernating) {
                    hibernatePackageGloballyL(packageName);
                } else {
                    unhibernatePackageGloballyL(packageName);
                }
            }
        }
    }

    /**
     * Put an app into hibernation for a given user, allowing user-level optimizations to occur.
     * The caller should hold {@link #mLock}
     *
     * @param pkgState package hibernation state
     */
    private void hibernatePackageForUserL(@NonNull String packageName, int userId,
            @NonNull UserLevelState pkgState) {
        Trace.traceBegin(Trace.TRACE_TAG_SYSTEM_SERVER, "hibernatePackage");
        final long caller = Binder.clearCallingIdentity();
        try {
            mIActivityManager.forceStopPackage(packageName, userId);
            mIPackageManager.deleteApplicationCacheFilesAsUser(packageName, userId,
                    null /* observer */);
            pkgState.hibernated = true;
            List<UserLevelState> states = new ArrayList<>(mUserStates.get(userId).values());
            mUserDiskStores.get(userId).scheduleWriteHibernationStates(states);
        } catch (RemoteException e) {
            throw new IllegalStateException(
                    "Failed to hibernate due to manager not being available", e);
        } finally {
            Binder.restoreCallingIdentity(caller);
            Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
        }
    }

    /**
     * Remove a package from hibernation for a given user. The caller should hold {@link #mLock}.
     *
     * @param pkgState package hibernation state
     */
    private void unhibernatePackageForUserL(@NonNull String packageName, int userId,
            UserLevelState pkgState) {
        Trace.traceBegin(Trace.TRACE_TAG_SYSTEM_SERVER, "unhibernatePackage");
        final long caller = Binder.clearCallingIdentity();
        try {
            mIPackageManager.setPackageStoppedState(packageName, false, userId);
            pkgState.hibernated = false;
            List<UserLevelState> states = new ArrayList<>(mUserStates.get(userId).values());
            mUserDiskStores.get(userId).scheduleWriteHibernationStates(states);
        } catch (RemoteException e) {
            throw new IllegalStateException(
                    "Failed to unhibernate due to manager not being available", e);
        } finally {
            Binder.restoreCallingIdentity(caller);
            Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
        }
    }

    /**
     * Put a package into global hibernation, optimizing its storage at a package / APK level.
     * The caller should hold {@link #mLock}.
     */
    private void hibernatePackageGloballyL(@NonNull String packageName) {
        Trace.traceBegin(Trace.TRACE_TAG_SYSTEM_SERVER, "hibernatePackageGlobally");
        // TODO(175830194): Delete vdex/odex when DexManager API is built out
        mGloballyHibernatedPackages.add(packageName);
        Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
    }

    /**
     * Unhibernate a package from global hibernation. The caller should hold {@link #mLock}.
     */
    private void unhibernatePackageGloballyL(@NonNull String packageName) {
        Trace.traceBegin(Trace.TRACE_TAG_SYSTEM_SERVER, "unhibernatePackageGlobally");
        mGloballyHibernatedPackages.remove(packageName);
        Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
    }

    /**
     * Populates {@link #mUserStates} with the users installed packages. The caller should hold
     * {@link #mLock}.
     *
     * @param userId user id to add installed packages for
     */
    private void addUserPackageStatesL(int userId) {
        Map<String, UserLevelState> packages = new ArrayMap<>();
        List<PackageInfo> packageList;
        try {
            packageList = mIPackageManager.getInstalledPackages(MATCH_ALL, userId).getList();
        } catch (RemoteException e) {
            throw new IllegalStateException("Package manager not available.", e);
        }

        for (int i = 0, size = packageList.size(); i < size; i++) {
            String packageName = packageList.get(i).packageName;
            UserLevelState packageState = new UserLevelState();
            packageState.packageName = packageName;
            packages.put(packageName, packageState);
        }
        mUserStates.put(userId, packages);
    }

    @Override
    public void onUserUnlocking(@NonNull TargetUser user) {
        int userId = user.getUserIdentifier();
        File hibernationDir = new File(Environment.getDataSystemCeDirectory(userId), "hibernation");
        HibernationStateDiskStore<UserLevelState> diskStore =
                mUserDiskStoreFactory.makeHibernationStateDiskStore(hibernationDir,
                        mUserLevelHibernationProto, mScheduledExecutor);
        mUserDiskStores.put(userId, diskStore);
        List<UserLevelState> storedStates = diskStore.readHibernationStates();
        synchronized (mLock) {
            addUserPackageStatesL(userId);
            Map<String, UserLevelState> userStates = mUserStates.get(userId);
            if (storedStates == null) {
                // First time unlocking user
                return;
            }
            for (int i = 0, size = storedStates.size(); i < size; i++) {
                String packageName = storedStates.get(i).packageName;
                UserLevelState stateInMemory = userStates.get(packageName);
                if (stateInMemory == null) {
                    Slog.w(TAG, String.format(
                            "No hibernation state associated with package %s user %d. Maybe"
                                    + "the package was uninstalled? ", packageName, userId));
                    continue;
                }
                userStates.put(packageName, storedStates.get(i));
            }
        }
    }

    @Override
    public void onUserStopping(@NonNull TargetUser user) {
        int userId = user.getUserIdentifier();
        // TODO: Flush any scheduled writes to disk immediately on user stopping / power off.
        synchronized (mLock) {
            mUserDiskStores.remove(userId);
            mUserStates.remove(userId);
        }
    }

    private void onPackageAdded(@NonNull String packageName, int userId) {
        UserLevelState newState = new UserLevelState();
        newState.packageName = packageName;
        synchronized (mLock) {
            mUserStates.get(userId).put(packageName, newState);
        }
    }

    private void onPackageRemoved(@NonNull String packageName, int userId) {
        synchronized (mLock) {
            mUserStates.get(userId).remove(packageName);
        }
    }

    private void onPackageRemovedForAllUsers(@NonNull String packageName) {
        synchronized (mLock) {
            mGloballyHibernatedPackages.remove(packageName);
        }
    }

    /**
     * Private helper method to get the real user id and enforce permission checks.
     *
     * @param userId user id to handle
     * @param name name to use for exceptions
     * @return real user id
     */
    private int handleIncomingUser(int userId, @NonNull String name) {
        int callingUid = Binder.getCallingUid();
        try {
            return mIActivityManager.handleIncomingUser(Binder.getCallingPid(), callingUid, userId,
                    false /* allowAll */, true /* requireFull */, name, null);
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    private final AppHibernationServiceStub mServiceStub = new AppHibernationServiceStub(this);

    static final class AppHibernationServiceStub extends IAppHibernationService.Stub {
        final AppHibernationService mService;

        AppHibernationServiceStub(AppHibernationService service) {
            mService = service;
        }

        @Override
        public boolean isHibernatingForUser(String packageName, int userId) {
            return mService.isHibernatingForUser(packageName, userId);
        }

        @Override
        public void setHibernatingForUser(String packageName, int userId, boolean isHibernating) {
            mService.setHibernatingForUser(packageName, userId, isHibernating);
        }

        @Override
        public void setHibernatingGlobally(String packageName, boolean isHibernating) {
            mService.setHibernatingGlobally(packageName, isHibernating);
        }

        @Override
        public boolean isHibernatingGlobally(String packageName) {
            return mService.isHibernatingGlobally(packageName);
        }

        @Override
        public void onShellCommand(@Nullable FileDescriptor in, @Nullable FileDescriptor out,
                @Nullable FileDescriptor err, @NonNull String[] args,
                @Nullable ShellCallback callback, @NonNull ResultReceiver resultReceiver) {
            new AppHibernationShellCommand(mService).exec(this, in, out, err, args, callback,
                    resultReceiver);
        }
    }

    // Broadcast receiver for package add/removal events
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL);
            if (userId == UserHandle.USER_NULL) {
                return;
            }

            final String action = intent.getAction();
            if (ACTION_PACKAGE_ADDED.equals(action) || ACTION_PACKAGE_REMOVED.equals(action)) {
                final String packageName = intent.getData().getSchemeSpecificPart();
                if (intent.getBooleanExtra(EXTRA_REPLACING, false)) {
                    // Package removal/add is part of an update, so no need to modify package state.
                    return;
                }

                if (ACTION_PACKAGE_ADDED.equals(action)) {
                    onPackageAdded(packageName, userId);
                } else if (ACTION_PACKAGE_REMOVED.equals(action)) {
                    onPackageRemoved(packageName, userId);
                    if (intent.getBooleanExtra(EXTRA_REMOVED_FOR_ALL_USERS, false)) {
                        onPackageRemovedForAllUsers(packageName);
                    }
                }
            }
        }
    };

    /**
     * Whether app hibernation is enabled on this device.
     *
     * @return true if enabled, false otherwise
     */
    public static boolean isAppHibernationEnabled() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION,
                AppHibernationConstants.KEY_APP_HIBERNATION_ENABLED,
                false /* defaultValue */);
    }

    /**
     * Factory for {@link HibernationStateDiskStore} which can be injected.
     * @param <T> data type to be written to disk
     */
    @VisibleForTesting
    interface HibernationStateDiskStoreFactory<T> {

        /**
         * Returns a new {@link HibernationStateDiskStore}.
         *
         * @param stateDir directory where data will be saved
         * @param readWriter proto writer/reader
         * @param executorService executor service for running writes
         */
        HibernationStateDiskStore<T> makeHibernationStateDiskStore(File stateDir,
                ProtoReadWriter<List<T>> readWriter, ScheduledExecutorService executorService);
    }
}
