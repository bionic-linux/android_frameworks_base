/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.os;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.SystemService;
import android.content.Context;
import android.os.IGestureLauncher;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

/**
 * Allow accessing gesture-based features
 *
 * @hide
 */
@SystemService(Context.GESTURE_LAUNCHER_SERVICE)
public class GestureLauncherManager {

    private Context mContext;
    private IGestureLauncher mService;

    /** @hide */
    public GestureLauncherManager(IGestureLauncher service) {
        mService = checkNotNull(service, "missing IGestureLauncher");
    }

    public GestureLauncherManager(Context context) {
        mContext = context;
    }

    private IGestureLauncher getService() {
        if (mService == null) {
            mService = IGestureLauncher.Stub.asInterface(
                    ServiceManager.getService(Context.GESTURE_LAUNCHER_SERVICE));
            if (mService == null) {
                Slog.w("GestureLauncherManager", "warning: no GESTURE_LAUNCHER_SERVICE");
            }
        }
        return mService;
    }

    public boolean isCameraButtonLaunchSettingEnabled() {
        try {
            final IGestureLauncher svc = getService();
            if (svc != null) {
                return svc.isCameraButtonLaunchSettingEnabled();
            }
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
        return false;
    }

    public boolean handleCameraGesture(int source) {
        try {
            final IGestureLauncher svc = getService();
            if (svc != null) {
                svc.handleCameraGesture(source);
            }
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

}
