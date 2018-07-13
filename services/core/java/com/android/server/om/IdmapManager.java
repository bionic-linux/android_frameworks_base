/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.server.om;

import static android.content.Context.IDMAP_SERVICE;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import static com.android.server.om.OverlayManagerService.DEBUG;
import static com.android.server.om.OverlayManagerService.TAG;

import android.annotation.NonNull;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.IIdmap2;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.os.BackgroundThread;

import java.io.File;

/**
 * Handle the creation and deletion of idmap files.
 *
 * The actual work is performed by the idmap2d binary.
 *
 * Note: this class is subclassed in the OMS unit tests, and hence not marked as final.
 */
class IdmapManager {
    private IIdmap2 mIdmap2Service;

    IdmapManager() {
        connect();
    }

    boolean createIdmap(@NonNull final PackageInfo targetPackage,
            @NonNull final PackageInfo overlayPackage, int userId) {
        if (DEBUG) {
            Slog.d(TAG, "create idmap for " + targetPackage.packageName + " and "
                    + overlayPackage.packageName);
        }
        final int sharedGid = UserHandle.getSharedAppGid(targetPackage.applicationInfo.uid);
        // FIXME: use sharedGid
        final String targetPath = targetPackage.applicationInfo.getBaseCodePath();
        final String overlayPath = overlayPackage.applicationInfo.getBaseCodePath();
        try {
            mIdmap2Service.createIdmap(targetPath, overlayPath, false, userId);
        } catch (Exception e) {
            Slog.w(TAG, "failed to generate idmap for " + targetPath + " and "
                    + overlayPath + ": " + e.getMessage());
            return false;
        }
        return true;
    }

    boolean removeIdmap(@NonNull final OverlayInfo oi, final int userId) {
        if (DEBUG) {
            Slog.d(TAG, "remove idmap for " + oi.baseCodePath);
        }
        try {
            mIdmap2Service.removeIdmap(oi.baseCodePath, userId);
        } catch (Exception e) {
            Slog.w(TAG, "failed to remove idmap for " + oi.baseCodePath + ": " + e.getMessage());
            return false;
        }
        return true;
    }

    boolean idmapExists(@NonNull final OverlayInfo oi) {
        return new File(getIdmapPath(oi.baseCodePath, oi.userId)).isFile();
    }

    boolean idmapExists(@NonNull final PackageInfo overlayPackage, final int userId) {
        return new File(getIdmapPath(overlayPackage.applicationInfo.getBaseCodePath(), userId))
            .isFile();
    }

    public String getIdmapPath(@NonNull final String overlayPackagePath, final int userId) {
        try {
            return mIdmap2Service.getIdmapPath(overlayPackagePath, userId);
        } catch (Exception e) {
            Slog.w(TAG,
                    "failed to get idmap path for " + overlayPackagePath + ": " + e.getMessage());
            return "";
        }
    }

    private void connect() {
        IBinder binder = ServiceManager.getService(IDMAP_SERVICE);
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Slog.w(TAG, "service '" + IDMAP_SERVICE + "' died; reconnecting...");
                        connect();
                    }

                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder != null) {
            mIdmap2Service = IIdmap2.Stub.asInterface(binder);
            Slog.d(TAG, "service '" + IDMAP_SERVICE + "' connected");
        } else {
            Slog.w(TAG, "service '" + IDMAP_SERVICE + "' not found; trying again...");
            BackgroundThread.getHandler().postDelayed(() -> {
                connect();
            }, SECOND_IN_MILLIS);
        }
    }
}
