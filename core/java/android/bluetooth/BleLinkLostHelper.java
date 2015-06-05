/*
 * Copyright (C) 2015 Tieto Corporation
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

package android.bluetooth;

import android.content.Context;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/*
 * this class provide API for Link Lost profile 
 */

public class BleLinkLostHelper {
    private static final String TAG = "BleLinkLostHelper";
    private Context mContext = null;
    public static int No_Alert = 0, Mid_Alert = 1, High_Alert = 2;
    private static final UUID LINK_LOSS_SERVICE_UUID = UUID
            .fromString("00001803-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARAC = UUID
            .fromString("00002A06-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothGatt mBluetoothGatt = null;
    
    private BleLinkLostHelperCallback mClientCallback = null;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.discoverServices();
                }
            }
            if (mClientCallback != null) {
                mClientCallback.onConnectionStateChanged(status, newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered");
        }

        @Override
        public void onDescriptorRead(android.bluetooth.BluetoothGatt gatt,
                android.bluetooth.BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (characteristic.getUuid().equals(ALERT_LEVEL_CHARAC)) {
                Log.d(TAG, "onCharacteristicWrite success, status=" + status);
            } else {
                Log.d(TAG, "onCharacteristicWrite fail, status=" + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic charac) {
            UUID charUid = charac.getUuid();
            int alertLevel = 0;
            if (charUid.equals(ALERT_LEVEL_CHARAC)) {
                alertLevel = parseCharac(charac);
                if ((mClientCallback != null) && (alertLevel >= 0)) {
                    mClientCallback.onAlertLevelChanged(alertLevel);
                    Log.d(TAG, "onCharacteristicChanged:alertLevel="
                            + alertLevel);
                }
            }
        }

        @Override
        public void onReliableWriteCompleted(
                android.bluetooth.BluetoothGatt gatt, int status) {
            Log.d(TAG, "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(android.bluetooth.BluetoothGatt gatt,
                int rssi, int status) {
            Log.d(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(android.bluetooth.BluetoothGatt gatt, int mtu,
                int status) {
            Log.d(TAG, "onMtuChanged");
        }
    };


    private int parseCharac(BluetoothGattCharacteristic charac) {
        int alertLevel;
        alertLevel = charac.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (alertLevel > High_Alert) {
            return -1;
        }

        return alertLevel;
    }

    public BleLinkLostHelper(Context context, BleLinkLostHelperCallback callback) {
        mContext = context;
        mClientCallback = callback;
    }
    
    //connect to device
    public boolean connect(BluetoothDevice device, boolean autoConnect){
        if (device == null) {
            return false;
        }

        if (mBluetoothDevice != null && device.equals(mBluetoothDevice)
                && mBluetoothGatt != null) {
            Log.i(TAG,
                    "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        mBluetoothGatt = device.connectGatt(mContext, autoConnect,
                mGattCallback);
        if (mBluetoothGatt != null) {
            Log.i(TAG, "connect: Trying to create a new connection.");
        } else {
            Log.i(TAG, "connect : mBluetoothGatt is null");
        }
        mBluetoothDevice = device;

        return true;
    }
    public void disconnect(){
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }
    //close gatt
    public boolean close(){
        if (mBluetoothGatt == null) {
            return false;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mBluetoothDevice = null;

        return true;
    }
    
    /**
     * set remote device character
     * 
     * {@link BluetoothGattCallback#onCharacteristicWrite will be invoked}
     * 
     * @param gatt
     *            Bluetooth GATT.
     * @param alertLevel
     *            the character set to remote device.
     */
    public void setAlertLevelCharac(int alertLevel){
        if (null == mBluetoothGatt) {
            return;
        }
        BluetoothGattService llcService = mBluetoothGatt
                .getService(LINK_LOSS_SERVICE_UUID);
        if (null == llcService) {
            return;
        }
        BluetoothGattCharacteristic charac = llcService
                .getCharacteristic(ALERT_LEVEL_CHARAC);
        if (null == charac) {
            return;
        }
        UUID charUid = charac.getUuid();
        if (charUid.equals(ALERT_LEVEL_CHARAC)) {
            byte[] value = new byte[1];
            value[0] = (byte) alertLevel;
            charac.setValue(value);
            if (!mBluetoothGatt.writeCharacteristic(charac)) {
                return;
            }
        }
    }
    /**
     * get remote device character
     */
    public int getAlertLevelCharac(){
        if (null == mBluetoothGatt) {
            return -1;
        }
        BluetoothGattService llcService = mBluetoothGatt
                .getService(LINK_LOSS_SERVICE_UUID);
        if (null == llcService) {
            return -1;
        }
        BluetoothGattCharacteristic charac = llcService
                .getCharacteristic(ALERT_LEVEL_CHARAC);
        if (null == charac) {
            return -1;
        }
        int alertLevel = -1;
        alertLevel = parseCharac(charac);
        return alertLevel;
    }
}

