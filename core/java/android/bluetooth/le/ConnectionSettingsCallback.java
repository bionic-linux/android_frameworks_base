
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
     * Callback triggered as result of {@link LeConnection#requestSettings}, or as a result of
     * the connection settings were changed by remote device.
     *
     * @param connection The connection settings to remote device
     * @param interval Connection interval used on this connection, 1.25ms unit. Valid range is from
     * 6 (7.5ms) to 3200 (4000ms).
     * @param latency Slave latency for the connection in number of connection events. Valid range
     * is from 0 to 499
     * @param timeout Supervision timeout for this connection, in 10ms unit. Valid range is from 10
     * (0.1s) to 3200 (32s)
     * @param txDataLength the negotiated transmit data length
     * @param rxDataLength the negotiated receive data length
     * @param mtu The new MTU size
     * @param txPhy the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     * BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param rxPhy the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M}, {@link
     * BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
     * @param status Status of the requestSettings operation. {@link LeConnection#SUCCESS} if the
     *               operation succeeds.
     */
    public void onConnectionSettingsUpdated(LeConnection connection, int interval, int latency,
                                            int timeout, int txDataLength, int rxDataLength,
                                            int mtu, int txPhy, int rxPhy, int status) {
    }
}

