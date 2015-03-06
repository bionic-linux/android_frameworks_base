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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class representing a Wi-Fi Mesh configuration list.
 *
 * Note that the operations are not thread safe.
 * {@see WifiMeshManager}
 */
public class WifiMeshGroupList implements Parcelable {

    private final HashMap<Integer, WifiMeshGroup> mConfigs =
            new HashMap<Integer, WifiMeshGroup>();

    /**
     * constructor
     * @hide
     */
    public WifiMeshGroupList() {
    }

    /**
     * constructor
     * @param source
     * @hide
     */
    public WifiMeshGroupList(WifiMeshGroupList source) {
        if (source != null) {
            for (WifiMeshGroup c : source.getWifiMeshGroups()) {
                mConfigs.put(c.getNetworkId(), new WifiMeshGroup(c));
            }
        }
    }

    /**
     * constructor
     * @param configs
     * @hide
     */
    public WifiMeshGroupList(ArrayList<WifiMeshGroup> configs) {
        for (WifiMeshGroup c : configs) {
            mConfigs.put(c.getNetworkId(), new WifiMeshGroup(c));
        }
    }

    /**
     * Clear the list.
     * @return true if remove the more than one node.
     * @hide
     */
    public boolean clear() {
        if (mConfigs.isEmpty()) return false;
        mConfigs.clear();
        return true;
    }

    /**
     * Return whether that the specified network id is contains in this list.
     * @param networkId
     * @return
     */
    public boolean contains(int networkId) {
        return mConfigs.get(networkId) != null;
    }

    /**
     * Add mesh group to the list.
     * @param c mesh group
     * @hide
     */
    public void add(WifiMeshGroup c) {
        mConfigs.put(c.getNetworkId(), c);
    }

    /**
     * Fetch a device from the list
     * @param networkId
     * @return mesh group
     */
    public WifiMeshGroup get(int networkId) {
        return mConfigs.get(networkId);
    }

    /**
     * Remove the specified mesh group from the list.
     * @param c
     * @return true if success, otherwise false.
     * @hide
     */
    public boolean remove(WifiMeshGroup c) {
        return mConfigs.remove(c.getNetworkId()) != null;
    }

    /**
     * Remove the mesh group with the specified network id from the list
     * @param networkId
     * @return true if success, otherwise false.
     */
    public WifiMeshGroup remove(int networkId) {
        return mConfigs.remove(networkId);
    }

    /**
     * Get the list of mesh groups.
     * @return the list of mesh groups.
     */
    public Collection<WifiMeshGroup> getWifiMeshGroups() {
        return Collections.unmodifiableCollection(mConfigs.values());
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        for (WifiMeshGroup c : mConfigs.values()) {
            sbuf.append("\n").append(c);
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
        dest.writeInt(mConfigs.size());
        for(WifiMeshGroup c : mConfigs.values()) {
            dest.writeParcelable(c, flags);
        }
    }

    /** Implement the Parcelable interface */
    public static final Creator<WifiMeshGroupList> CREATOR =
        new Creator<WifiMeshGroupList>() {
            @Override
            public WifiMeshGroupList createFromParcel(Parcel in) {
                WifiMeshGroupList configs = new WifiMeshGroupList();

                int deviceCount = in.readInt();
                for (int i = 0; i < deviceCount; i++) {
                    configs.add((WifiMeshGroup)in.readParcelable(null));
                }
                return configs;
            }

            @Override
            public WifiMeshGroupList[] newArray(int size) {
                return new WifiMeshGroupList[size];
            }
        };
}
