
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
import android.util.Log;

package android.bluetooth;

/**
 * This abstract class is used to implement {@link ConnectionSettings} callbacks.
 */
public abstract class ConnectionSettingsCallback {

    /**
     * Callback indicating the connection parameters were updated.
     *
     * @param connection The connection settings to remote device
     * @param interval Connection interval used on this connection, 1.25ms unit. Valid range is from
     * 6 (7.5ms) to 3200 (4000ms).
     * @param latency Slave latency for the connection in number of connection events. Valid range
     * is from 0 to 499
     * @param timeout Supervision timeout for this connection, in 10ms unit. Valid range is from 10
     * (0.1s) to 3200 (32s)
     */
    public void onConnectionParamUpdated(ConnectionSettings connection, int interval,
                                         int latency, int timeout) {
    }

    /**
     * Callback indicating the MTU for a given device connection has changed.
     *
     * This callback is triggered in response to the
     * {@link ConnectionSettings#requestMtu} function or as a result of this MTU size being changed.
     *
     * @param connection object {@link ConnectionSettings#requestMtu}
     * @param mtu The new MTU size
     * @param status {@link ConnectionSettings#SUCCESS} if the MTU has been changed successfully
     */
    public void onMtuChanged(ConnectionSettings connection, int mtu, int status) {
    }

    /**
     * Callback triggered as result of {@link ConnectionSettings#setPreferredPhy}, or as a result of
     * remote device changing the PHY.
     *
     * @param connection object
     * @param txPhy the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     * BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param rxPhy the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     * BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param status Status of the PHY update operation. {@link ConnectionSettings#GATT_SUCCESS}
     * if the operation succeeds.
     */
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
    }

    /**
     * Callback triggered as result of {@link ConnectionSettings#readPhy}
     *
     * @param connection object
     * @param txPhy the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     * BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param rxPhy the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     * BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}. 
     * @param txDataLength the negotiated transmit data length
     * @param rxDataLength the negotiated receive data length
     * @param status Status of the PHY read operation. {@link ConnectionSettings#GATT_SUCCESS} if
     * the operation succeeds.
     */
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int txDataLength,
                          int rxDataLength, int status) {
    }
}

