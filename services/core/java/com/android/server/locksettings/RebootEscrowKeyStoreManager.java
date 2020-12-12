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

package com.android.server.locksettings;

import android.security.keystore.AndroidKeyStoreSpi;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore2.AndroidKeyStoreProvider;
import android.util.Slog;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class RebootEscrowKeyStoreManager {
    private static final String TAG = "RebootEscrowKeyStoreManager";

    /**
     * The key alias in keystore. This key is used to wrap both escrow key and escrow data.
     */
    public static final String REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME =
            "reboot_escrow_key_store_encryption_key";

    public static final int KEY_LENGTH = 256;

    /**
     * Hold this lock when getting or generating the encryption key in keystore.
     */
    private final Object mKeyStoreLock = new Object();

    /**
     * TODO This function redirects keystore access to the legacy keystore during a transitional
     * phase during which not all calling code has been adjusted to use Keystore 2.0.
     * This can be reverted to a constant of "AndroidKeyStore" when b/171305684 is complete.
     * The specific bug for this component is b/171305115.
     */
    static String androidKeystoreProviderName() {
        if (AndroidKeyStoreProvider.isInstalled()) {
            return "AndroidKeyStoreLegacy";
        } else {
            return "AndroidKeystore";
        }
    }

    private SecretKey getKeyStoreEncryptionKeyUnguarded() {
        try {
            KeyStore keyStore = KeyStore.getInstance(androidKeystoreProviderName());
            keyStore.load(/*param=*/ null);
            return (SecretKey) keyStore.getKey(REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME,
                    null);
        } catch (IOException | GeneralSecurityException e) {
            Slog.e(TAG, "Unable to get encryption key from keystore.", e);
        }
        return null;
    }

    protected SecretKey getKeyStoreEncryptionKey() {
        synchronized (mKeyStoreLock) {
            return getKeyStoreEncryptionKeyUnguarded();
        }
    }

    protected void clearKeyStoreEncryptionKey() {
        synchronized (mKeyStoreLock) {
            try {
                KeyStore keyStore = KeyStore.getInstance(androidKeystoreProviderName());
                keyStore.load(/*param=*/ null);
                keyStore.deleteEntry(REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME);
            } catch (IOException | GeneralSecurityException e) {
                Slog.e(TAG, "Unable to delete encryption key in keystore.", e);
            }
        }
    }

    protected SecretKey generateKeyStoreEncryptionKeyIfNeeded() {
        synchronized (mKeyStoreLock) {
            SecretKey kk = getKeyStoreEncryptionKeyUnguarded();
            if (kk != null) {
                return kk;
            }

            try {
                KeyGenerator generator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStoreSpi.NAME);
                generator.init(new KeyGenParameterSpec.Builder(
                        REBOOT_ESCROW_KEY_STORE_ENCRYPTION_KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(KEY_LENGTH)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
                return generator.generateKey();
            } catch (GeneralSecurityException e) {
                // Should never happen.
                Slog.e(TAG, "Unable to generate key from keystore.", e);
            }
            return null;
        }
    }
}