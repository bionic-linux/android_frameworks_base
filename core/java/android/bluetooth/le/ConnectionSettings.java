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

/**
 * Represents the settings of the LE Connection to remote device. Bluetooth LE Connection Settings 
 * are passed to LeConnection.requestSettings(settings, callback). Please note that the Bluetooth 
 * Controller may not be able to satisfy all requests especially competing ones. 
 */
public class ConnectionSettings {

    /**
     * Suggest to the Bluetooth Controller that this LE Connection will have certain data throughput
     * behavior. DATA_THROUGHPUT_HIGH suggests that this connection will be sending large amount of
     * data, needs a high data throughput connection, and request that the Bluetooth Controller
     * optimize this connection for high data throughput. Applications should refrain from using
     * DATA_THROUGHPUT_HIGH for long period of time since it uses more battery power on both sides
     * and reduces the performance on other connections. DATA_THROUGHPUT_DEFAULT suggests that this
     * connection does not have any special throughput needs and the Bluetooth Controller can
     * optimize for the best overall performance and battery usage. We suggest that most 
     * applications with no special throughput needs use DATA_THROUGHPUT_DEFAULT.
     */
    public static final int DATA_THROUGHPUT_DEFAULT = 1;
    public static final int DATA_THROUGHPUT_HIGH = 2;

    /**
     * Suggest to the Bluetooth Controller that this LE Connection will have certain latency
     * requirements. LINK_LATENCY_LOW suggests that this connection needs a short turnaround time
     * for the data packet exchange and request that the Bluetooth Controller optimize this
     * connection for high responsiveness. HID devices like LE keyboard may request low latency 
     * during the periods when it is accepting key strokes. Applications should refrain from using 
     * LINK_LATENCY_LOW for long period of time since it uses more battery power on both sides and 
     * reduces the performance on other connections. LINK_LATENCY_DEFAULT suggests that this 
     * connection does not have any special throughput needs and the Bluetooth Controller can 
     * optimize for the best overall performance and battery usage. We suggest that most 
     * applications with no special latency needs use LINK_LATENCY_DEFAULT. 
     */
    public static final int LINK_LATENCY_DEFAULT = 1;
    public static final int LINK_LATENCY_LOW = 2;

    /**
     * Suggest to the Bluetooth Controller that this LE Connection will have certain power usage
     * requirements. The Bluetooth Controller will always try to conserve battery power as long as 
     * it meets the other competing LE Proporties requests. POWER_USAGE_LOW suggests that this 
     * connection needs a low power usage profile. Please note that conserving battery power on the 
     * peer may affects the data throughput, latency, and disconnection detection time. 
     * PEER_POWER_USAGE_DEFAULT suggests that this connection does not have any special power needs 
     * and the Bluetooth Controller can optimize for the best overall performance and battery usage. 
     */
    public static final int PEER_POWER_USAGE_DEFAULT = 1;
    public static final int PEER_POWER_USAGE_LOW = 2;

    public static final int SUCCESS = 0;

    private BluetoothDevice mDevice;

    // TODO: add list of parameters
    private ConnectionSettings(BluetoothDevice device) {
        //TODO: Implementation
    }

    /**
     * Returns the current Data Throughput setting. 
     */
    public int getDataThroughput() {
        //TODO: Implementation
    }

    public int getLinkLatency() {
        //TODO: Implementation
    }

    public int getPowerUsage() {
        //TODO: Implementation
    }

    public int getMtu() {
        //TODO: Implementation
    }

    /**
     * Returns the Physical layer used for this connection. 
     */
    public int getPhy() {
        //TODO: Implementation
    }

    /**
     * Builder class for {@link ConnectionSettings}.
     */
    public static final class Builder {

        /**
         * To suggest to the Bluetooth Controller to optimize the LE connection based on the desired
         * data throughput behavior. The ConnectionSettingsCallback#onConnectionParamUpdated will be 
         * triggered the result of this call. 
         * 
         * @throws IllegalArgumentException If invalid parameters.
         */ 
        public Builder requestDataThroughput(int dataThroughputBehavior) {
            //TODO: Implementation
        }

        /**
         * To suggest to the Bluetooth Controller to optimize the LE connection based on the desired
         * link latency behavior. The ConnectionSettingsCallback#onConnectionParamUpdated will be 
         * triggered the result of this call. 
         * 
         * @throws IllegalArgumentException If invalid parameters.
         */
        public Builder requestLinkLatency(int linkLatencyBehavior) {
            //TODO: Implementation
        }

        /**
         * To suggest to the Bluetooth Controller to optimize the LE connection based on the desired
         * power usage behavior. The ConnectionSettingsCallback#onConnectionParamUpdated will be 
         * triggered the result of this call.
         * 
         * @throws IllegalArgumentException If invalid parameters.
         */
        public Builder requestPowerUsage(int powerUsageBehavior) {
            //TODO: Implementation
        }

        /**
         * To suggest to the Bluetooth Controller to configure the MTU data packet size. Please note 
         * that the fragmentation and assembly of data packets are hidden to the application but sending 
         * the data packet over the socket with the configured mtu or multiples of mtu sizes may 
         * increase efficiency. The callback {@link ConnectionSettingsCallback#onMtuChanged} will be 
         * triggered as a result of this call.
         * 
         * @throws IllegalArgumentException If invalid parameters.
         */
        public Builder requestMtu(int mtu) {
            //TODO: Implementation
        }

        /**
         * The callback {@link ConnectionSettingsCallback#onPhyUpdate} will be triggered as a result of 
         * this call. 
         * 
         * @throws IllegalArgumentException If invalid parameters.
         */
        public Builder requestPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
            //TODO: Implementation
        }

        /**
         * Build {@link ConnectionSettings}.
         *
         * @throws IllegalArgumentException If the settings cannot be built.
         */
        public ConnectionSettings build() {
            // TODO: Add list of parameters
            return new ConnectionSettings(mDeviceName);
        }
    }

}

