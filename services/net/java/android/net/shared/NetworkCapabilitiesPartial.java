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

package android.net.shared;

import android.net.NetworkCapabilities;
import android.net.NetworkCapabilitiesPartialParcelable;

import java.util.Objects;

/**
 * Partial NetworkCapabilities. This class is used in the NetworkStack to access some parts of
 * {@link android.net.NetworkCapabilities} without having to expose its members to all @SystemApi
 * callers.
 *
 * <p>In particular, the network specifier, transport info or UIDs are not included.
 * @hide
 */
public class NetworkCapabilitiesPartial {
    private final long mCapabilities;
    private final long mUnwantedCapabilities;
    private final long mTransportTypes;
    private final int mLinkUpBandwidthKbps;
    private final int mLinkDownBandwidthKbps;
    private final String mSsid;

    private NetworkCapabilitiesPartial(long capabilities, long unwantedCapabilities,
            long transportTypes, int linkUpBandwidthKbps, int linkDownBandwidthKbps,
            String ssid) {
        mCapabilities = capabilities;
        mUnwantedCapabilities = unwantedCapabilities;
        mTransportTypes = transportTypes;
        mLinkUpBandwidthKbps = linkUpBandwidthKbps;
        mLinkDownBandwidthKbps = linkDownBandwidthKbps;
        mSsid = ssid;
    }

    /**
     * Returns whether the network has the specified capability.
     * @param capability capability to test. Behavior will be unspecified for unknown capabilities.
     */
    public boolean hasCapability(int capability) {
        return (mCapabilities & (1 << capability)) != 0;
    }

    /**
     * Returns whether the network has the specified unwanted capability.
     * @param capability capability to test. Behavior will be unspecified for unknown capabilities.
     */
    public boolean hasUnwantedCapability(int capability) {
        return (mUnwantedCapabilities & (1 << capability)) != 0;
    }

    /**
     * Returns whether the network has the specified transport.
     * @param transportType to test. Behavior will be unspecified for unknown transport types.
     */
    public boolean hasTransport(int transportType) {
        return (mTransportTypes & (1 << transportType)) != 0;
    }

    /**
     * @see NetworkCapabilities#getLinkUpstreamBandwidthKbps()
     */
    public int getLinkUpBandwidthKbps() {
        return mLinkUpBandwidthKbps;
    }

    /**
     * @see NetworkCapabilities#getLinkDownstreamBandwidthKbps()
     */
    public int getLinkDownBandwidthKbps() {
        return mLinkDownBandwidthKbps;
    }

    /**
     * @see NetworkCapabilities#getSSID()
     */
    public String getSsid() {
        return mSsid;
    }

    /**
     * Utility class to create an instance of NetworkCapabilitiesPartial.
     */
    public static class Builder {
        private long mCapabilities;
        private long mUnwantedCapabilities;
        private long mTransportTypes;
        private int mLinkUpBandwidthKbps;
        private int mLinkDownBandwidthKbps;
        private String mSsid;

        /**
         * Set the capabilities to the specified bit field.
         */
        public Builder setCapabilities(long capabilities) {
            mCapabilities = capabilities;
            return this;
        }

        /**
         * Add a capability. Behavior will be unspecified for unknown capabilities.
         */
        public Builder addCapability(int capability) {
            mCapabilities |= 1 << capability;
            mUnwantedCapabilities &= ~(1 << capability);
            return this;
        }

        /**
         * Set the unwanted capabilities to the specified bit field.
         */
        public Builder setUnwantedCapabilities(long unwantedCapabilities) {
            mUnwantedCapabilities = unwantedCapabilities;
            return this;
        }

        /**
         * Add an unwanted capability. Behavior will be unspecified for unknown capabilities.
         */
        public Builder addUnwantedCapability(int capability) {
            mUnwantedCapabilities |= 1 << capability;
            mCapabilities &= ~(1 << capability);
            return this;
        }

        /**
         * Set the transport types to the specified bit field.
         */
        public Builder setTransportTypes(long transportTypes) {
            mTransportTypes = transportTypes;
            return this;
        }

        /**
         * Add a transport type. Behavior will be unspecified for unknown transports.
         */
        public Builder addTransportType(int transportType) {
            mTransportTypes |= 1 << transportType;
            return this;
        }

        /**
         * Set the link up bandwidth to the specified value in Kbps.
         */
        public Builder setLinkUpBandwidthKbps(int linkUpBandwidthKbps) {
            mLinkUpBandwidthKbps = linkUpBandwidthKbps;
            return this;
        }

        /**
         * Set the link down bandwidth to the specified value in Kbps.
         */
        public Builder setLinkDownBandwidthKbps(int linkDownBandwidthKbps) {
            mLinkDownBandwidthKbps = linkDownBandwidthKbps;
            return this;
        }

        /**
         * Set the WiFi SSID to the specified value.
         */
        public Builder setSsid(String ssid) {
            mSsid = ssid;
            return this;
        }

        /**
         * Create a new NetworkCapabilitiesPartial with the specified values.
         */
        public NetworkCapabilitiesPartial build() {
            return new NetworkCapabilitiesPartial(mCapabilities, mUnwantedCapabilities,
                    mTransportTypes, mLinkUpBandwidthKbps, mLinkDownBandwidthKbps, mSsid);
        }
    }

    /**
     * Create a NetworkCapabilitiesPartialParcelable from this object.
     */
    public NetworkCapabilitiesPartialParcelable toStableParcelable() {
        NetworkCapabilitiesPartialParcelable p = new NetworkCapabilitiesPartialParcelable();
        p.capabilities = mCapabilities;
        p.unwantedCapabilities = mUnwantedCapabilities;
        p.transportTypes = mTransportTypes;
        p.linkUpBandwidthKbps = mLinkUpBandwidthKbps;
        p.linkDownBandwidthKbps = mLinkDownBandwidthKbps;
        p.ssid = mSsid;
        return p;
    }

    /**
     * Create a NetworkCapabilitiesPartial from a NetworkCapabilitiesPartialParcelable.
     */
    public static NetworkCapabilitiesPartial fromStableParcelable(
            NetworkCapabilitiesPartialParcelable p) {
        return new NetworkCapabilitiesPartial.Builder()
            .setCapabilities(p.capabilities)
            .setUnwantedCapabilities(p.unwantedCapabilities)
            .setTransportTypes(p.transportTypes)
            .setLinkUpBandwidthKbps(p.linkUpBandwidthKbps)
            .setLinkDownBandwidthKbps(p.linkDownBandwidthKbps)
            .setSsid(p.ssid)
            .build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkCapabilitiesPartial)) {
            return false;
        }
        final NetworkCapabilitiesPartial other = (NetworkCapabilitiesPartial) obj;
        return mCapabilities == other.mCapabilities
                && mUnwantedCapabilities == other.mUnwantedCapabilities
                && mTransportTypes == other.mTransportTypes
                && mLinkUpBandwidthKbps == other.mLinkUpBandwidthKbps
                && mLinkDownBandwidthKbps == other.mLinkDownBandwidthKbps
                && Objects.equals(mSsid, other.mSsid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCapabilities, mUnwantedCapabilities, mTransportTypes,
                mLinkUpBandwidthKbps, mLinkDownBandwidthKbps, mSsid);
    }
}
