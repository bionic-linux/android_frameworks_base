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

package android.security;

import android.annotation.NonNull;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.security.keystore.AndroidKeyStoreProvider;
import android.security.vpnprofilestore.IVpnProfileStore;

/**
 * @hide This calss allows legacy VPN access to the its profiles that were stored in Keystore.
 * The storage of unstructured blobs in Android Keystore is going away, because there is no
 * architectural or security benefit of storing profiles in keystore over storing them
 * in the file system. This class allows access to the blobs that still exist in keystore.
 * And it stores new blob in a database that is still owned by Android Keystore.
 */
public class LegacyVpnProfileStore {
    private static final String TAG = "LegacyVpnProfileStore";

    public static final int SYSTEM_ERROR = IVpnProfileStore.ERROR_SYSTEM_ERROR;
    public static final int PROFILE_NOT_FOUND = IVpnProfileStore.ERROR_PROFILE_NOT_FOUND;

    private static synchronized IVpnProfileStore getService() {
        return IVpnProfileStore.Stub.asInterface(
                    ServiceManager.checkService("android.security.vpnprofilestore"));
    }

    /**
     * Stores the profile under the alias in the profile database.
     * @param alias The name of the profile
     * @param profile The profile.
     * @throws LegacyVpnStoreException on error.
     * @hide
     */
    public static void put(@NonNull String alias, @NonNull byte[] profile) {
        try {
            if (AndroidKeyStoreProvider.isKeystore2Enabled()) {
                getService().put(alias, profile);
            } else {
                if (!KeyStore.getInstance().put(
                        alias, profile, KeyStore.UID_SELF, 0)) {
                    throw new LegacyVpnStoreException(
                            "Failed to put profile with legacy keystore.");
                }
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new LegacyVpnStoreException("Failed to put profile.", e);
        }
    }

    /**
     * Retrieves a profile by the name alias from the profile database.
     * @param alias Name of the profile to retrieve.
     * @return The unstructured blob, that is the profile that was stored using
     *         LegacyVpnProfileStore#put or with
     *         android.security.Keystore.put(Credentials.VPN + alias).
     *         Returns null if no profile was found.
     * @throws LegacyVpnStoreException on error.
     * @hide
     */
    public static byte[] get(@NonNull String alias) {
        try {
            if (AndroidKeyStoreProvider.isKeystore2Enabled()) {
                return getService().get(alias);
            } else {
                return KeyStore.getInstance().get(alias, true /* suppressKeyNotFoundWarning */);
            }
        } catch (ServiceSpecificException e) {
            if (e.errorCode != PROFILE_NOT_FOUND) {
                throw new LegacyVpnStoreException("Failed to get profile.", e);
            }
        } catch (RemoteException e) {
            throw new LegacyVpnStoreException("Failed to get profile.", e);
        }
        return null;
    }

    /**
     * Removes a profile by the name alias from the profile database.
     * @param alias Name of the profile to be removed.
     * @return True if a profile was removed. False if no such profile was found.
     * @throws LegacyVpnStoreException on error.
     * @hide
     */
    public static boolean remove(@NonNull String alias) {
        try {
            if (AndroidKeyStoreProvider.isKeystore2Enabled()) {
                getService().remove(alias);
                return true;
            } else {
                return KeyStore.getInstance().delete(alias);
            }
        } catch (ServiceSpecificException e) {
            if (e.errorCode != PROFILE_NOT_FOUND) {
                throw new LegacyVpnStoreException("Failed to get profile.", e);
            }
        } catch (RemoteException e) {
            throw new LegacyVpnStoreException("Failed to put profile.", e);
        }
        return false;
    }

    /**
     * Lists the vpn profiles stored in the database.
     * @return An array of strings representing the aliases stored in the profile database.
     *         The return value may be empty but never null.
     * @throws LegacyVpnStoreException on error.
     * @hide
     */
    public static @NonNull String[] list(@NonNull String prefix) {
        try {
            if (AndroidKeyStoreProvider.isKeystore2Enabled()) {
                return getService().list(prefix);
            } else {
                String [] result = KeyStore.getInstance().list(prefix);
                if (result == null) {
                    result = new String[0];
                }
                return result;
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new LegacyVpnStoreException("Failed to list profiles.", e);
        }
    }
}
