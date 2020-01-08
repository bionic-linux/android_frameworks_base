/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.util;

import android.Manifest;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;


/**
 * Utility methods for common functionality using by different networks.
 *
 * @hide
 */
public class ConnectivityUtil {

    private static final String TAG = "ConnectivityUtil";

    /**
     * Check the location permission and location mode.
     *
     * Check whether the application has fine/coarse location permission (depending on
     * config/targetSDK level) and the location mode is enabled for the user.
     *
     * @param pkgName package name of the application requesting access
     * @param featureId The feature in the package
     * @param uid The uid of the package
     * @param message A message describing why the permission was checked. Only needed if this is
     *                not inside of a two-way binder call from the data receiver
     */
    public static boolean checkLocationPermission(String pkgName, @Nullable String featureId,
            int uid, @Nullable String message, Context context) {
        try {
            enforceLocationPermission(pkgName, featureId, uid, message, context);
            return true;
        } catch (SecurityException e) {
            Log.d(TAG,
                    "Uid " + uid
                    + " does not hold the location permission or the location mode is disabled.",
                    e);
            return false;
        }
    }

    /**
     * API to enforce the location permission and location mode.
     *
     * Enforce the application has fine/coarse location permission (depending on config/targetSDK
     * level) and the location mode is enabled for the user. SecurityException is thrown if the
     * application has no permission or the location mode is disabled.
     *
     * @param pkgName package name of the application requesting access
     * @param featureId The feature in the package
     * @param uid The uid of the package
     * @param message A message describing why the permission was checked. Only needed if this is
     *                not inside of a two-way binder call from the data receiver
     */
    public static void enforceLocationPermission(String pkgName, @Nullable String featureId,
            int uid, @Nullable String message, Context context) throws SecurityException {

        checkPackage(uid, pkgName, context);

        // Location mode must be enabled
        if (!isLocationModeEnabled(context)) {
            // Location mode is disabled, scan results cannot be returned
            throw new SecurityException("Location mode is disabled for the device");
        }

        // LocationAccess by App: caller must have Coarse/Fine Location permission to have access to
        // location information.
        boolean canAppPackageUseLocation = checkCallersLocationPermission(pkgName, featureId,
                uid, /* coarseForTargetSdkLessThanQ */ true, message, context);

        // If neither caller or app has location access, there is no need to check
        // any other permissions. Deny access to scan results.
        if (!canAppPackageUseLocation) {
            throw new SecurityException("UID " + uid + " has no location permission");
        }
        // If the User or profile is current, permission is granted
        // Otherwise, uid must have INTERACT_ACROSS_USERS_FULL permission.
        if (!isCurrentProfile(uid, context) && !checkInteractAcrossUsersFull(uid, context)) {
            throw new SecurityException("UID " + uid + " profile not permitted");
        }
    }

    /**
     * Checks that calling process has android.Manifest.permission.ACCESS_FINE_LOCATION or
     * android.Manifest.permission.ACCESS_COARSE_LOCATION (depending on config/targetSDK level)
     * and a corresponding app op is allowed for this package and uid.
     *
     * @param pkgName PackageName of the application requesting access
     * @param featureId The feature in the package
     * @param uid The uid of the package
     * @param coarseForTargetSdkLessThanQ If true and the targetSDK < Q then will check for COARSE
     *                                    else (false or targetSDK >= Q) then will check for FINE
     * @param message A message describing why the permission was checked. Only needed if this is
     *                not inside of a two-way binder call from the data receiver
     */
    public static boolean checkCallersLocationPermission(String pkgName, @Nullable String featureId,
            int uid, boolean coarseForTargetSdkLessThanQ, @Nullable String message,
            Context context) {
        boolean isTargetSdkLessThanQ =
                isTargetSdkLessThan(pkgName, Build.VERSION_CODES.Q, uid, context);

        String permissionType = Manifest.permission.ACCESS_FINE_LOCATION;
        if (coarseForTargetSdkLessThanQ && isTargetSdkLessThanQ) {
            // Having FINE permission implies having COARSE permission (but not the reverse)
            permissionType = Manifest.permission.ACCESS_COARSE_LOCATION;
        }
        if (getUidPermission(permissionType, uid, context) == PackageManager.PERMISSION_DENIED) {
            return false;
        }

        AppOpsManager appOpsManager =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        // Always checking FINE - even if will not enforce. This will record the request for FINE
        // so that a location request by the app is surfaced to the user.
        boolean isFineLocationAllowed = noteAppOpAllowed(
                AppOpsManager.OPSTR_FINE_LOCATION, pkgName, featureId, uid, message, appOpsManager);
        if (isFineLocationAllowed) {
            return true;
        }
        if (coarseForTargetSdkLessThanQ && isTargetSdkLessThanQ) {
            return noteAppOpAllowed(AppOpsManager.OPSTR_COARSE_LOCATION, pkgName, featureId, uid,
                    message, appOpsManager);
        }
        return false;
    }

    /**
     * Retrieves a handle to LocationManager (if not already done) and check if location is enabled.
     */
    public static boolean isLocationModeEnabled(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            return locationManager.isLocationEnabledForUser(UserHandle.of(
                    getCurrentUser()));
        } catch (Exception e) {
            Log.e(TAG, "Failure to get location mode via API, falling back to settings", e);
            return false;
        }
    }

    private static boolean isTargetSdkLessThan(
            String packageName, int versionCode, int callingUid, Context context) {
        long ident = Binder.clearCallingIdentity();
        try {
            if (context.getPackageManager().getApplicationInfoAsUser(
                    packageName, 0,
                    UserHandle.getUserHandleForUid(callingUid)).targetSdkVersion
                    < versionCode) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // In case of exception, assume unknown app (more strict checking)
            // Note: This case will never happen since checkPackage is
            // called to verify validity before checking App's version.
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
        return false;
    }

    private static boolean noteAppOpAllowed(String op, String pkgName, @Nullable String featureId,
            int uid, @Nullable String message, AppOpsManager appOpsManager) {
        return appOpsManager.noteOp(op, uid, pkgName) == AppOpsManager.MODE_ALLOWED;
    }

    private static void checkPackage(
            int uid, String pkgName, Context context) throws SecurityException {
        if (pkgName == null) {
            throw new SecurityException("Checking UID " + uid + " but Package Name is Null");
        }
        AppOpsManager appOpsManager =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        appOpsManager.checkPackage(uid, pkgName);
    }

    private static boolean isCurrentProfile(int uid, Context context) {
        UserHandle currentUser = UserHandle.of(getCurrentUser());
        UserHandle callingUser = UserHandle.getUserHandleForUid(uid);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        return currentUser.equals(callingUser)
                || userManager.isSameProfileGroup(
                        currentUser.getIdentifier(), callingUser.getIdentifier());
    }

    private static boolean checkInteractAcrossUsersFull(int uid, Context context) {
        return getUidPermission(
                android.Manifest.permission.INTERACT_ACROSS_USERS_FULL, uid, context)
                == PackageManager.PERMISSION_GRANTED;
    }

    @VisibleForTesting
    protected static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    private static int getUidPermission(String permissionType, int uid, Context context) {
        // We don't care about pid, pass in -1
        return context.checkPermission(permissionType, -1, uid);
    }
}
