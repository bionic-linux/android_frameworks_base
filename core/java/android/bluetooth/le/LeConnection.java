
/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.bluetooth.le;
import android.bluetooth.ConnectionSettingsCallback;
import android.util.Log;

/**
 * Represents the LE Connection to remote device. 
 * Please note that the Bluetooth Controller may not be able to satisfy all requests especially 
 * competing ones. For each client, the last request will have the highest priority. For example, if
 * low latency is more important that data throughput, then setDataThroughput should be called 
 * before setLinkLatency.
 */
public class LeConnection {

    private BluetoothDevice mDevice;

    public static final int SUCCESS = 0;

    /*package*/ LeConnection(BluetoothDevice device) {
        //TODO: Implementation
    }

    /**
     * Request the various Connection Settings for this LE Connection. The settings parameter is 
     * created by the ConnectionSettings.Builder method. The callback parameter is used when the 
     * setting changes and the caller is informed. 
     * 
     */
    public requestSettings(ConnectionSettings settings, ConnectionSettingsCallback callback) {
        //TODO: Implementation
    }

    /**
     * Return the remote bluetooth device this GATT client targets to
     *
     * @return remote bluetooth device
     */
    public BluetoothDevice getDevice() {
        return mDevice;
    }
}

