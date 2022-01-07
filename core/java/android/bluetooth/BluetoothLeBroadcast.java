/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * This class provides the public APIs to control the Bluetooth LE Broadcast Source profile.
 *
 * <p>BluetoothLeBroadcast is a proxy object for controlling the Bluetooth LE Broadcast Source
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the BluetoothLeBroadcast
 * proxy object.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcast implements BluetoothProfile {
    private static final String TAG = "BluetoothLeBroadcast";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    /**
     * Constants used by the LE Audio Broadcast profile for the Broadcast state
     *
     * @hide
     */
    @SystemApi
    @IntDef(
            prefix = {"LE_AUDIO_BROADCAST_STATE_"},
            value = {
                LE_AUDIO_BROADCAST_STATE_DISABLED,
                LE_AUDIO_BROADCAST_STATE_ENABLING,
                LE_AUDIO_BROADCAST_STATE_ENABLED,
                LE_AUDIO_BROADCAST_STATE_DISABLING,
                LE_AUDIO_BROADCAST_STATE_PLAYING,
                LE_AUDIO_BROADCAST_STATE_NOT_PLAYING
            })
    @Retention(RetentionPolicy.SOURCE)
    /** @hide */
    public @interface LeAudioBroadcastState {}

    /**
     * Indicates that LE Audio Broadcast mode is currently disabled
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_STATE_DISABLED = 10;

    /**
     * Indicates that LE Audio Broadcast mode is being enabled
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_STATE_ENABLING = 11;

    /**
     * Indicates that LE Audio Broadcast mode is currently enabled
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_STATE_ENABLED = 12;
    /**
     * Indicates that LE Audio Broadcast mode is being disabled
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_STATE_DISABLING = 13;

    /**
     * Indicates that an LE Audio Broadcast mode is currently playing
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_STATE_PLAYING = 14;

    /**
     * Indicates that LE Audio Broadcast is currently not playing
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_STATE_NOT_PLAYING = 15;

    /**
     * Constants used by the LE Audio Broadcast profile for encryption key length
     *
     * @hide
     */
    @SystemApi
    @IntDef(
            prefix = {"LE_AUDIO_BROADCAST_ENCRYPTION_KEY_"},
            value = {
                LE_AUDIO_BROADCAST_ENCRYPTION_KEY_32BIT,
                LE_AUDIO_BROADCAST_ENCRYPTION_KEY_128BIT
            })
    @Retention(RetentionPolicy.SOURCE)
    /** @hide */
    public @interface LeAudioEncryptionKeyLength {}

    /**
     * Indicates that the LE Audio Broadcast encryption key size is 32 bits.
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_ENCRYPTION_KEY_32BIT = 16;

    /**
     * Indicates that the LE Audio Broadcast encryption key size is 128 bits.
     *
     * @hide
     */
    @SystemApi public static final int LE_AUDIO_BROADCAST_ENCRYPTION_KEY_128BIT = 17;

    /**
     * Interface for receiving events related to broadcasts
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /**
         * Called when broadcast state has changed
         *
         * @param prevState broadcast state before the change
         * @param newState broadcast state after the change
         */
        @LeAudioBroadcastState
        void onBroadcastStateChange(int prevState, int newState);
        /**
         * Called when encryption key has been updated
         *
         * @param success true if the key was updated successfully, false otherwise
         */
        void onEncryptionKeySet(boolean success);
    }

    /**
     * Create a BluetoothLeBroadcast proxy object for interacting with the local LE Audio Broadcast
     * Source service.
     *
     * @hide
     */
    @SystemApi
    /*package*/ BluetoothLeBroadcast(Context context, BluetoothProfile.ServiceListener listener) {}

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @throws UnsupportedOperationException
     * @hide
     */
    @SystemApi
    @Override
    public int getConnectionState(@NonNull BluetoothDevice device) {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @throws UnsupportedOperationException
     * @hide
     */
    @SystemApi
    @Override
    public @NonNull List<BluetoothDevice> getDevicesMatchingConnectionStates(
            @NonNull int[] states) {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Not supported since LE Audio Broadcasts do not establish a connection
     *
     * @throws UnsupportedOperationException
     * @hide
     */
    @SystemApi
    @Override
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException("LE Audio Broadcasts are not connection-oriented.");
    }

    /**
     * Enable LE Audio Broadcast mode.
     *
     * <p>Generates a new broadcast ID and enables sending of encrypted or unencrypted isochronous
     * PDUs
     *
     * @hide
     */
    @SystemApi
    public int enableBroadcastMode() {
        if (DBG) log("enableBroadcastMode");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_SOURCE_SET_BROADCAST_MODE_FAILED;
    }

    /**
     * Disable LE Audio Broadcast mode.
     *
     * @hide
     */
    @SystemApi
    public int disableBroadcastMode() {
        if (DBG) log("disableBroadcastMode");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_SOURCE_SET_BROADCAST_MODE_FAILED;
    }

    /**
     * Get the current LE Audio broadcast state
     *
     * @hide
     */
    @SystemApi
    @LeAudioBroadcastState
    public int getBroadcastState() {
        if (DBG) log("getBroadcastState");
        return LE_AUDIO_BROADCAST_STATE_DISABLED;
    }

    /**
     * Enable LE Audio broadcast encryption
     *
     * @param keyLength if useExisting is true, this specifies the length of the key that should be
     *     generated
     * @param useExisting true, if an existing key should be used false, if a new key should be
     *     generated
     * @hide
     */
    @SystemApi
    public int enableEncryption(boolean useExisting, @LeAudioEncryptionKeyLength int keyLength) {
        if (DBG) log("enableEncryption useExisting=" + useExisting + " keyLength=" + keyLength);
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_SOURCE_ENABLE_ENCRYPTION_FAILED;
    }

    /**
     * Disable LE Audio broadcast encryption
     *
     * @param removeExisting true, if the existing key should be removed false, otherwise
     * @hide
     */
    @SystemApi
    public int disableEncryption(boolean removeExisting) {
        if (DBG) log("disableEncryption removeExisting=" + removeExisting);
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_SOURCE_DISABLE_ENCRYPTION_FAILED;
    }

    /**
     * Enable or disable LE Audio broadcast encryption
     *
     * @param key use the provided key if non-null, generate a new key if null
     * @param keyLength 0 if encryption is disabled, 4 bytes (low security), 16 bytes (high
     *     security)
     * @hide
     */
    @SystemApi
    public int setEncryptionKey(@NonNull byte[] key, @LeAudioEncryptionKeyLength int keyLength) {
        if (DBG) log("setEncryptionKey key=" + key + " keyLength=" + keyLength);
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_SOURCE_SET_ENCRYPTION_KEY_FAILED;
    }

    /**
     * Get the encryption key that was set before
     *
     * @return encryption key as a byte array or null if no encryption key was set
     * @hide
     */
    @SystemApi
    public @Nullable byte[] getEncryptionKey() {
        if (DBG) log("getEncryptionKey");
        return null;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
