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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import android.net.wifi.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * A class representing a Wi-Fi Mesh node list.
 *
 * Note that the operations are not thread safe.
 * {@see WifiMeshManager}
 */
public class WifiMeshScanResults implements Parcelable {

    private final HashMap<String, ScanResult> mScanResults =
            new HashMap<String, ScanResult>();

    /**
     * Constructor
     * @hide
     */
    public WifiMeshScanResults() {
    }

    /**
     * Copy Constructor
     * @param source
     * @hide
     */
    public WifiMeshScanResults(WifiMeshScanResults source) {
        if (source != null) {
            for (ScanResult r : source.getScanResults()) {
                mScanResults.put(r.BSSID, new ScanResult(r));
            }
        }
    }

    /**
     * Validate the scan result.
     * @param r
     */
    private void validateScanResult(ScanResult r) {
        if (r == null) throw new IllegalArgumentException("Null device");
        if (TextUtils.isEmpty(r.BSSID)) {
            throw new IllegalArgumentException("Empty bssid");
        }
    }

    /**
     * Validate the bssid.
     * @param bssid
     */
    private void validateBssid(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            throw new IllegalArgumentException("Empty bssid");
        }
    }

    /**
     * Clear the scan list.
     * @return
     * @hide
     */
    public boolean clear() {
        if (mScanResults.isEmpty()) return false;
        mScanResults.clear();
        return true;
    }

    /**
     * Add/update a mesh node to the list. If the mesh node is not found,
     * a new node entry is created. If the mesh node is already found,
     * the mesh node details are updated
     * @param mesh node to be updated
     * @hide
     */
    public void update(ScanResult scanResult) {
        updateSupplicantDetails(scanResult);
    }

    /**
     * Only updates details fetched from the supplicant
     * @param source
     * @hide
     */
    private void updateSupplicantDetails(ScanResult source) {
        validateScanResult(source);
        ScanResult r = mScanResults.get(source.BSSID);
        if (r != null) {
            r.BSSID = source.BSSID;
            r.SSID = source.SSID;
            r.wifiSsid = source.wifiSsid;
            r.capabilities = source.capabilities;
            r.frequency = source.frequency;
            r.level = source.frequency;
            r.timestamp = source.timestamp;
            return;
        }
        //Not found, add a new one
        mScanResults.put(source.BSSID, source);
    }

    /**
     * Fetch a mesh node from the list
     * @param deviceAddress is the address of the device
     * @return scan result found, or null if none found
     */
    public ScanResult get(String deviceAddress) {
        validateBssid(deviceAddress);
        return mScanResults.get(deviceAddress);
    }

    /**
     * Remove a mesh node from the list.
     * @param r
     * @return
     * @hide
     */
    public boolean remove(ScanResult r) {
        validateScanResult(r);
        return mScanResults.remove(r.BSSID) != null;
    }

    /**
     * Remove a mesh node from the list
     * @param deviceAddress is the address of the device
     * @return WifiP2pDevice device removed, or null if none removed
     * @hide
     */
    public ScanResult remove(String bssid) {
        validateBssid(bssid);
        return mScanResults.remove(bssid);
    }

    /**
     * Get the list of scan results.
     * @return scan results list.
     */
    public Collection<ScanResult> getScanResults() {
        return Collections.unmodifiableCollection(mScanResults.values());
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        for (ScanResult r : mScanResults.values()) {
            sbuf.append("\n").append(r);
        }
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mScanResults.size());
        for(ScanResult r : mScanResults.values()) {
            dest.writeParcelable(r, flags);
        }
    }

    /** Implement the Parcelable interface */
    public static final Creator<WifiMeshScanResults> CREATOR =
        new Creator<WifiMeshScanResults>() {
            @Override
            public WifiMeshScanResults createFromParcel(Parcel in) {
                WifiMeshScanResults scanResults = new WifiMeshScanResults();

                int deviceCount = in.readInt();
                for (int i = 0; i < deviceCount; i++) {
                    scanResults.update((ScanResult)in.readParcelable(null));
                }
                return scanResults;
            }

            @Override
            public WifiMeshScanResults[] newArray(int size) {
                return new WifiMeshScanResults[size];
            }
        };
}
