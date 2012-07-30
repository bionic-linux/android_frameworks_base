/*
 * Copyright (C) 2012 The Android Open Source Project
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
package android.net.wifi.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * A class representing a Wi-Fi P2p group list
 *
 * {@see WifiP2pManager}
 * @hide
 */
public class WifiP2pGroupList {

    private List<WifiP2pGroup> mGroups;

    public WifiP2pGroupList() {
        mGroups = new ArrayList<WifiP2pGroup>();
    }

    /** copy constructor */
    public WifiP2pGroupList(WifiP2pGroupList source) {
        if (source != null) {
            mGroups = source.getGroupList();
        }
    }

    /**
     * Remove all of the groups from this group list.
     * @return true if remove over one or more elements.
     * @hide
     */
    public boolean clear() {
        if (mGroups.isEmpty()) return false;
        mGroups.clear();
        return true;
    }

    /**
     * Add the specified group to this group list.
     * @param group
     * @return true if the element is added.
     * @hide
     */
    public boolean add(WifiP2pGroup group) {
        return mGroups.add(group);
    }

    /**
     * Remove the group with the specified network id from this group list.
     *
     * @param netId
     * @return true if the element is removed.
     * @hide
     */
    public boolean remove(int netId) {
        for (Iterator<WifiP2pGroup> i = mGroups.iterator(); i.hasNext();) {
            if (i.next().getNetworkId() == netId) {
                i.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Remove the group with the specified device address from this group list.
     *
     * @param netId
     * @return true if the element is removed.
     * @hide
     */
    public boolean remove(String deviceAddress) {
        return remove(getNetworkId(deviceAddress));
    }

    /**
     * Return the network id of the group owner profile with the specified p2p device
     * address.
     * If more than one persistent group of the same address is present in the list,
     * return the first one.
     *
     * @param deviceAddress p2p device address.
     * @return the network id. if not found, return -1.
     * @hide
     */
    public int getNetworkId(String deviceAddress) {
        if (deviceAddress == null) return -1;

        for (WifiP2pGroup group:mGroups) {
            if (deviceAddress.equalsIgnoreCase(group.getOwner().deviceAddress)) {
                return group.getNetworkId();
            }
        }
        return -1;
    }

    /**
     * Return the network id of the group with the specified p2p device address
     * and the ssid.
     *
     * @param deviceAddress p2p device address.
     * @param ssid ssid.
     * @return the network id. if not found, return -1.
     * @hide
     */
    public int getNetworkId(String deviceAddress, String ssid) {
        if (deviceAddress == null || ssid == null) {
            return -1;
        }
        for (WifiP2pGroup group:mGroups) {
            if (deviceAddress.equalsIgnoreCase(group.getOwner().deviceAddress) &&
                    ssid.equals(group.getNetworkName())) {
                return group.getNetworkId();
            }
        }
        return -1;
    }

    /**
     * Return the group owner address of the group with the specified network id
     *
     * @param netId network id.
     * @return the address. if not found, return null.
     * @hide
     */
    public String getOwnerAddr(int netId) {
        if (netId < 0) {
            return null;
        }
        for (WifiP2pGroup group:mGroups) {
            if (group.getNetworkId() == netId) {
                return group.getOwner().deviceAddress;
            }
        }
        return null;
    }

    /**
     * Return true if this group list contains the specified network id.
     * @param netId network id.
     * @return true if the specified network id is present in this group list.
     * @hide
     */
    public boolean contains(int netId) {
        for (WifiP2pGroup group:mGroups) {
            if (group.getNetworkId() == netId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the number of the groups in this list.
     * @return the number of the groups in this list.
     * @hide
     */
    public int size() {
        return mGroups.size();
    }

    /**
     * Return true if this group list contains no elements.
     * @return true if this group list contains no elements.
     * @hide
     */
    public boolean isEmpty() {
        return mGroups.isEmpty();
    }

    /**
     * Return the list of p2p group.
     * @return the list of p2p group.
     */
    public List<WifiP2pGroup> getGroupList() {
        return Collections.unmodifiableList(mGroups);
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        for (WifiP2pGroup group : mGroups) {
            sbuf.append("\n").append(group);
        }
        return sbuf.toString();
    }
}
