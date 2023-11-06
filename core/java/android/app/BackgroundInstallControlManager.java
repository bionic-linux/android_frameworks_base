/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.app;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import static android.Manifest.permission.QUERY_ALL_PACKAGES;
import static android.annotation.SystemApi.Client.PRIVILEGED_APPS;
import android.content.pm.IBackgroundInstallControlService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * BackgroundInstallControl client allows apps to query apps installed in background
 * @hide
 */
@SystemApi(client=PRIVILEGED_APPS)
@SystemService(Context.BACKGROUND_INSTALL_CONTROL_SERVICE)
public final class BackgroundInstallControlManager {
    private static final String TAG = "BackgroundInstallControlManager";
    private static IBackgroundInstallControlService sService;
    private static ArrayList<CallbackDelegate> registeredDelegates = new ArrayList<>();

    BackgroundInstallControlManager() {}
    private static IBackgroundInstallControlService getService() {
        if (sService == null) {
            sService = IBackgroundInstallControlService.Stub.asInterface(
                ServiceManager.getService(Context.BACKGROUND_INSTALL_CONTROL_SERVICE));
        }
        return sService;
    }

    /**
     * Calls BackgroundInstallControlService getBackgroundInstalledPackages method
     */
    @RequiresPermission(QUERY_ALL_PACKAGES)
    public @NonNull List<PackageInfo> getBackgroundInstalledPackages(
            @PackageManager.PackageInfoFlagsBits long flags, int userId) throws RuntimeException {
        try {
            return getService().getBackgroundInstalledPackages(flags, userId).getList();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RequiresPermission(QUERY_ALL_PACKAGES)
    public void registerBackgroundInstallControlCallback(@NonNull @CallbackExecutor Executor executor,
                                                         @NonNull BackgroundInstallControlManager.Callback callback
                                                           ) throws RuntimeException {
        CallbackDelegate delegate = new CallbackDelegate(executor, callback);
        try {
            getService().registerBackgroundInstallControlCallback(delegate.mBicCallback);
            registeredDelegates.add(delegate);
        } catch(RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @RequiresPermission(QUERY_ALL_PACKAGES)
    public void unregisterBackgroundInstallControlCallback(@NonNull BackgroundInstallControlManager.Callback callback) throws RuntimeException {
        try {
            Iterator<CallbackDelegate> it = registeredDelegates.iterator();
            CallbackDelegate delegate;
            while(it.hasNext()) {
                delegate = it.next();
                if(delegate.mCallback.equals(callback)) {
                    getService().unregisterBackgroundInstallControlCallback(delegate.mBicCallback);
                    it.remove();
                    return;
                }
            }
        } catch(RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Callback {
        String FLAGGED_PACKAGE_NAME_KEY = "backgroundinstallcontrol.flagged.packagename";
        String FLAGGED_PACAKGE_USER_ID_KEY = "backgroundinstallcontrol.flagged.userid";
        void onMbaDetected(@NonNull Bundle extras);
    }

    private static class CallbackDelegate {
        final Executor mExecutor;
        final BackgroundInstallControlManager.Callback mCallback;
        final IRemoteCallback mBicCallback = new IRemoteCallback.Stub() {
            @Override
            public void sendResult(Bundle extras) {
                mExecutor.execute(() -> mCallback.onMbaDetected(extras));
            }
        };

        public CallbackDelegate(Executor executor, BackgroundInstallControlManager.Callback callback) {
            mExecutor = executor;
            mCallback = callback;
        }
    }
}