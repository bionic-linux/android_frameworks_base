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

package android.security.keystore2;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.security.usermanager.IKeystoreUserManager;
import android.system.keystore2.ResponseCode;
import android.util.Log;

/**
 * @hide This is the client side for IKeystoreUserManager AIDL.
 * It shall only be used by the LockSettingsService.
 */
public class UserManager {
    private static final String TAG = "KeystoreUserManager";
    private static IKeystoreUserManager sIKeystoreUserManager;

    public static final int SYSTEM_ERROR = ResponseCode.SYSTEM_ERROR;

    public UserManager() {
        sIKeystoreUserManager = null;
    }

    private static synchronized IKeystoreUserManager getService() {
        if (sIKeystoreUserManager == null) {
            sIKeystoreUserManager = IKeystoreUserManager.Stub.asInterface(
                    ServiceManager.checkService("android.security.usermanager")
            );
        }
        return sIKeystoreUserManager;
    }

    /**
     * Informs keystore2 about adding a user
     *
     * @param userId - Android user id of the user being added
     * @return 0 if successful or a {@code ResponseCode}
     */
    public int onUserAdded(@NonNull int userId) {
        if (!android.security.keystore2.AndroidKeyStoreProvider.isInstalled()) return 0;
        try {
            getService().onUserAdded(userId);
            return 0;
        } catch (RemoteException e) {
            Log.w(TAG, "Can not connect to keystore", e);
            return SYSTEM_ERROR;
        } catch (ServiceSpecificException e) {
            return e.errorCode;
        }
    }

    /**
     * Informs keystore2 about removing a user
     *
     * @param userId - Android user id of the user being removed
     * @return 0 if successful or a {@code ResponseCode}
     */
    public int onUserRemoved(int userId) {
        if (!android.security.keystore2.AndroidKeyStoreProvider.isInstalled()) return 0;
        try {
            getService().onUserRemoved(userId);
            return 0;
        } catch (RemoteException e) {
            Log.w(TAG, "Can not connect to keystore", e);
            return SYSTEM_ERROR;
        } catch (ServiceSpecificException e) {
            return e.errorCode;
        }
    }

    /**
     * Informs keystore2 about changing user's password
     *
     * @param userId   - Android user id of the user
     * @param password - a secret derived from the synthetic password provided by the
     *                 LockSettingService
     * @return 0 if successful or a {@code ResponseCode}
     */
    public int onUserPasswordChanged(int userId, @Nullable byte[] password) {
        if (!android.security.keystore2.AndroidKeyStoreProvider.isInstalled()) return 0;
        try {
            getService().onUserPasswordChanged(userId, password);
            return 0;
        } catch (RemoteException e) {
            Log.w(TAG, "Can not connect to keystore", e);
            return SYSTEM_ERROR;
        } catch (ServiceSpecificException e) {
            return e.errorCode;
        }
    }
}
