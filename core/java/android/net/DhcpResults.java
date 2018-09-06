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

package android.net;

import android.annotation.UnsupportedAppUsage;
import android.net.NetworkUtils;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Objects;

/**
 * A simple object for retrieving the results of a DHCP request.
 * Optimized (attempted) for that jni interface
 * TODO - remove when DhcpInfo is deprecated.  Move the remaining api to LinkProperties.
 * @hide
 */
public class DhcpResults extends StaticIpConfiguration {
    private static final String TAG = "DhcpResults";

    @UnsupportedAppUsage
    public Inet4Address serverAddress;

    /** Vendor specific information (from RFC 2132). */
    @UnsupportedAppUsage
    public String vendorInfo;

    /** List of time servers (NTP) addresses (from RFC 2132) **/
    @UnsupportedAppUsage
    public ArrayList<InetAddress> ntpServers = new ArrayList<>();

    @UnsupportedAppUsage
    public int leaseDuration;

    /** Link MTU option. 0 means unset. */
    @UnsupportedAppUsage
    public int mtu;

    @UnsupportedAppUsage
    public DhcpResults() {
        super();
    }

    @UnsupportedAppUsage
    public DhcpResults(StaticIpConfiguration source) {
        super(source);
    }

    /** copy constructor */
    @UnsupportedAppUsage
    public DhcpResults(DhcpResults source) {
        super(source);

        if (source != null) {
            // Make copy of ntp servers addresses
            ntpServers = new ArrayList<InetAddress>(source.ntpServers);
            // All these are immutable, so no need to make copies.
            serverAddress = source.serverAddress;
            vendorInfo = source.vendorInfo;
            leaseDuration = source.leaseDuration;
            mtu = source.mtu;
        }
    }

    /**
     * Test if this DHCP lease includes vendor hint that network link is
     * metered, and sensitive to heavy data transfers.
     */
    public boolean hasMeteredHint() {
        if (vendorInfo != null) {
            return vendorInfo.contains("ANDROID_METERED");
        } else {
            return false;
        }
    }

    public void clear() {
        super.clear();
        vendorInfo = null;
        leaseDuration = 0;
        mtu = 0;
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer(super.toString());

        str.append(" DHCP server ").append(serverAddress);
        str.append(" Vendor info ").append(vendorInfo);
        str.append(" lease ").append(leaseDuration).append(" seconds");
        if (mtu != 0) str.append(" MTU ").append(mtu);
        str.append(" Ntp server(s) : ").append(ntpServers);

        return str.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof DhcpResults)) return false;

        DhcpResults target = (DhcpResults)obj;

        return super.equals((StaticIpConfiguration) obj) &&
                Objects.equals(serverAddress, target.serverAddress) &&
                ntpServers.equals(target.ntpServers) && // same NTP servers list (content check)
                Objects.equals(vendorInfo, target.vendorInfo) &&
                leaseDuration == target.leaseDuration &&
                mtu == target.mtu;
    }

    /** Implement the Parcelable interface */
    public static final Creator<DhcpResults> CREATOR =
        new Creator<DhcpResults>() {
            public DhcpResults createFromParcel(Parcel in) {
                DhcpResults dhcpResults = new DhcpResults();
                readFromParcel(dhcpResults, in);
                return dhcpResults;
            }

            public DhcpResults[] newArray(int size) {
                return new DhcpResults[size];
            }
        };

    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(leaseDuration);
        dest.writeInt(mtu);
        NetworkUtils.parcelInetAddress(dest, serverAddress, flags);
        dest.writeString(vendorInfo);
    }

    private static void readFromParcel(DhcpResults dhcpResults, Parcel in) {
        StaticIpConfiguration.readFromParcel(dhcpResults, in);
        dhcpResults.leaseDuration = in.readInt();
        dhcpResults.mtu = in.readInt();
        dhcpResults.serverAddress = (Inet4Address) NetworkUtils.unparcelInetAddress(in);
        dhcpResults.vendorInfo = in.readString();
    }

    // Utils for jni population - false on success
    // Not part of the superclass because they're only used by the JNI iterface to the DHCP daemon.
    public boolean setIpAddress(String addrString, int prefixLength) {
        try {
            Inet4Address addr = (Inet4Address) NetworkUtils.numericToInetAddress(addrString);
            ipAddress = new LinkAddress(addr, prefixLength);
        } catch (IllegalArgumentException|ClassCastException e) {
            Log.e(TAG, "setIpAddress failed with addrString " + addrString + "/" + prefixLength);
            return true;
        }
        return false;
    }

    public boolean setGateway(String addrString) {
        try {
            gateway = NetworkUtils.numericToInetAddress(addrString);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setGateway failed with addrString " + addrString);
            return true;
        }
        return false;
    }

    public boolean addDns(String addrString) {
        if (TextUtils.isEmpty(addrString) == false) {
            try {
                dnsServers.add(NetworkUtils.numericToInetAddress(addrString));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "addDns failed with addrString " + addrString);
                return true;
            }
        }
        return false;
    }

    public boolean setServerAddress(String addrString) {
        try {
            serverAddress = (Inet4Address) NetworkUtils.numericToInetAddress(addrString);
        } catch (IllegalArgumentException|ClassCastException e) {
            Log.e(TAG, "setServerAddress failed with addrString " + addrString);
            return true;
        }
        return false;
    }

    public void setLeaseDuration(int duration) {
        leaseDuration = duration;
    }

    public void setVendorInfo(String info) {
        vendorInfo = info;
    }

    public void setDomains(String newDomains) {
        domains = newDomains;
    }

    /**
    * Define a NTP servers list
    * @param newNtpServers NTP servers list to be used
    */
    public void setNtpServers(ArrayList<InetAddress> newNtpServers) {
        ntpServers = newNtpServers;
    }
}
