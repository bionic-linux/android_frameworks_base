/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.net.wifi.mesh;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class representing a Wi-Fi mesh group.
 *
 * {@see WifiMeshManager}
 */
public class WifiMeshGroup implements Parcelable {

    /** The network name of mesh group */
    private String mMeshId;

    /** whether network is secured or not. */
    private boolean mIsSecure;

    /** The passphrase used for SAE */
    private String mPassphrase;

    /** operating Frequency*/
    private int mFreq;

    /** The network id in the wpa_supplicant */
    private int mNetId;

    /** {@hide} */
    public static final int INVALID_NETWORK_ID = -1;

    /** Mesh group started string pattern */
    private static final Pattern groupStartedPattern = Pattern.compile(
            "ssid=\"(.+)\" id=(\\d+)");

    public WifiMeshGroup() {
        mFreq = -1;
        mNetId = -1;
    }

    /**
     * Create WifiMeshGroup from supplicant MESH-GROUP-STARTED event.
     * Parse supplicant event and set them to the member variables.
     * @param eventStr
     */
    public WifiMeshGroup(String eventStr) {
        Matcher matcher = groupStartedPattern.matcher(eventStr);
        if (!matcher.find()) {
            return;
        }
        mMeshId = matcher.group(1);
        mFreq = -1;
        mNetId = Integer.parseInt(matcher.group(2));
    }

    /**
     * Return mesh network id.
     * @return mesh id
     */
    public String getMeshId() {
        return mMeshId;
    }

    public String getMeshIdQuote() {
        return "\"" + mMeshId + "\"";
    }

    /**
     * Set mesh network id.
     * @param meshId mesh id
     */
    public void setMeshId(String meshId) {
        mMeshId = meshId;
    }

    /**
     * Return whether to be secure network(SAE)
     * @return true if secured network.
     */
    public boolean isSecure() {
        return mIsSecure;
    }

    /**
     * Set secure mode.
     * @param isSecure
     */
    public void setSecure(boolean isSecure) {
        mIsSecure = isSecure;
    }

    /**
     * Return passphrase. Only used for secure mode.
     * @return
     */
    public String getPassphrase() {
        return mPassphrase;
    }

    /**
     * Set passphrase. Only used for secure mode.
     * @param pass
     */
    public void setPassphrase(String pass) {
        mPassphrase = pass;
    }

    /**
     * Return the operating frequency. The unit is MHz.
     * @return
     */
    public int getFrequency() {
        return mFreq;
    }

    /**
     * Set the operating frequency. The unit is MHz.
     * @param freq
     */
    public void setFrequency(int freq) {
        mFreq = freq;
    }

    /**
     * Return supplicant network id.
     * @return network id
     */
    public int getNetworkId() {
        return mNetId;
    }

    /**
     * Set network id.
     * @param netId
     * @hide
     */
    public void setNetworkId(int netId) {
        mNetId = netId;
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(" meshId: ").append(mMeshId);
        sbuf.append("\n freq: ").append(mFreq);
        sbuf.append("\n networkId: ").append(mNetId);
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    @Override
    public int describeContents() {
        return 0;
    }

    /** copy constructor */
    public WifiMeshGroup(WifiMeshGroup source) {
        if (source != null) {
            mMeshId = source.getMeshId();
            mIsSecure = source.isSecure();
            mPassphrase = source.getPassphrase();
            mFreq = source.getFrequency();
            mNetId = source.getNetworkId();
        }
    }

    /** Implement the Parcelable interface */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMeshId);
        dest.writeByte(mIsSecure ? (byte) 1: (byte) 0);
        dest.writeString(mPassphrase);
        dest.writeInt(mFreq);
        dest.writeInt(mNetId);
    }

    /** Implement the Parcelable interface */
    public static final Creator<WifiMeshGroup> CREATOR =
        new Creator<WifiMeshGroup>() {
            @Override
            public WifiMeshGroup createFromParcel(Parcel in) {
                WifiMeshGroup group = new WifiMeshGroup();
                group.setMeshId(in.readString());
                group.setSecure(in.readByte() == (byte)1);
                group.setPassphrase(in.readString());
                group.setFrequency(in.readInt());
                group.setNetworkId(in.readInt());
                return group;
            }

            @Override
            public WifiMeshGroup[] newArray(int size) {
                return new WifiMeshGroup[size];
            }
        };

}
