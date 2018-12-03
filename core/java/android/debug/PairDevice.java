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

package android.debug;

import java.io.Serializable;

/**
 * Class containing the host device informations, also broadcast this class to
 * the setting UI, in order to updating the paired devices.
 */
public class PairDevice implements Serializable{
    private int mDeviceId;
    private String mName;
    private String mMacAddress;
    private boolean mConnected;

    public PairDevice(int deviceId, String name, String macAddress, boolean connected) {
        mDeviceId = deviceId;
        mName = name;
        mMacAddress = macAddress;
        mConnected = connected;
    }
    public void setDeviceId(int deviceId) {
        mDeviceId = deviceId;
    }
    public int getDeviceId() {
        return mDeviceId;
    }
    public void setDeviceName(String name) {
        mName = name;
    }
    public String getDeviceName() {
        return mName;
    }
    public void setMacAddress(String macAddress) {
        mMacAddress = macAddress;
    }
    public String getMacAddress() {
        return mMacAddress;
    }
    public void setConnected(boolean connected) {
        mConnected = connected;
    }
    public boolean isConnected() {
        return mConnected;
    }
    /**
     * print device string
     */
    public String toString() {
        return "\n" + mName + "\n" + mMacAddress + "\n" + mConnected;
    }
}
