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

package android.net.ipmemorystore;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A POD object to represent attributes of a single L2 network entry.
 * @hide
 */
public class NetworkAttributes {
    private static final boolean DBG = true;

    // The v4 address that was assigned to this device the last time it joined this network.
    // This typically comes from DHCP but could be something else like static configuration.
    // This does not apply to IPv6.
    // TODO : add a list of v6 prefixes for the v6 case.
    @Nullable
    public final InetAddress assignedV4Address;

    // Optionally supplied by the client if it has an opinion on L3 network. For example, this
    // could be a hash of the SSID + security type on WiFi.
    @Nullable
    public final String groupHint;

    // The list of DNS server addresses.
    @Nullable
    public final List<InetAddress> dnsAddresses;

    // The mtu on this network.
    @Nullable
    public final Integer mtu;

    NetworkAttributes(
            @Nullable final InetAddress assignedV4Address,
            @Nullable final String groupHint,
            @Nullable final List<InetAddress> dnsAddresses,
            @Nullable final Integer mtu) {
        if (mtu != null && mtu < 0) throw new IllegalArgumentException("MTU can't be negative");
        this.assignedV4Address = assignedV4Address;
        this.groupHint = groupHint;
        this.dnsAddresses = null == dnsAddresses ? null :
                Collections.unmodifiableList(new ArrayList<>(dnsAddresses));
        this.mtu = mtu;
    }

    @VisibleForTesting
    public NetworkAttributes(@NonNull final NetworkAttributesParceled parceled) {
        // The call to the other constructor must be the first statement of this constructor,
        // so everything has to be inline
        this(getByAddressOrNull(parceled.assignedV4Address),
                parceled.groupHint,
                null == parceled.dnsAddresses ? null : Arrays.stream(parceled.dnsAddresses)
                        .map(NetworkAttributes::blobToInetAddress)
                        .filter(addr -> addr != null) // In case blobToInetAddress returns null
                        .collect(Collectors.toList()),
                parceled.mtu >= 0 ? parceled.mtu : null);
    }

    @Nullable
    private static InetAddress getByAddressOrNull(@Nullable final byte[] address) {
        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Nullable
    private static InetAddress blobToInetAddress(@Nullable final Blob blob) {
        if (null == blob) return null;
        try {
            return InetAddress.getByAddress(blob.data);
        } catch (UnknownHostException e) {
            // Not supposed to ever happen as the blob was marshalled from a resolved address.
            // However if this happens, returning null is the only reasonable choice.
            return null;
        }
    }

    @Nullable
    private static Blob inetAddressToBlob(@Nullable final InetAddress address) {
        final Blob b = new Blob();
        b.data = address.getAddress();
        return b;
    }

    /** Converts this NetworkAttributes to a parcelable object */
    @NonNull
    public NetworkAttributesParceled toParcelable() {
        final NetworkAttributesParceled parceled = new NetworkAttributesParceled();
        parceled.assignedV4Address =
                null == assignedV4Address ? null : assignedV4Address.getAddress();
        parceled.groupHint = groupHint;
        parceled.dnsAddresses = null == dnsAddresses ? null : dnsAddresses.stream()
                .map(NetworkAttributes::inetAddressToBlob)
                .toArray(count -> new Blob[count]);
        parceled.mtu = null == mtu ? -1 : mtu;
        return parceled;
    }

    /** @hide */
    public static class Builder {
        @Nullable
        private InetAddress mAssignedAddress;
        @Nullable
        private String mGroupHint;
        @Nullable
        private List<InetAddress> mDnsAddresses;
        @Nullable
        private Integer mMtu;

        /**
         * Set the assigned address.
         * @param assignedV4Address The assigned address.
         * @return This builder.
         */
        public Builder setAssignedAddress(@Nullable final InetAddress assignedV4Address) {
            mAssignedAddress = assignedV4Address;
            return this;
        }

        /**
         * Set the group hint.
         * @param groupHint The group hint.
         * @return This builder.
         */
        public Builder setGroupHint(@Nullable final String groupHint) {
            mGroupHint = groupHint;
            return this;
        }

        /**
         * Set the DNS addresses.
         * @param dnsAddresses The DNS addresses.
         * @return This builder.
         */
        public Builder setDnsAddresses(@Nullable final List<InetAddress> dnsAddresses) {
            if (DBG && null != dnsAddresses) {
                // Parceling code crashes if one of the addresses is null, therefore validate
                // them when running in debug.
                for (final InetAddress address : dnsAddresses) {
                    if (null == address) throw new IllegalArgumentException("Null DNS address");
                }
            }
            this.mDnsAddresses = dnsAddresses;
            return this;
        }

        /**
         * Set the MTU.
         * @param mtu The MTU.
         * @return This builder.
         */
        public Builder setMtu(@Nullable final Integer mtu) {
            if (null != mtu && mtu < 0) throw new IllegalArgumentException("MTU can't be negative");
            mMtu = mtu;
            return this;
        }

        /**
         * Return the built NetworkAttributes object.
         * @return The built NetworkAttributes object.
         */
        public NetworkAttributes build() {
            return new NetworkAttributes(mAssignedAddress, mGroupHint, mDnsAddresses, mMtu);
        }
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (!(o instanceof NetworkAttributes)) return false;
        final NetworkAttributes other = (NetworkAttributes) o;
        return Objects.equals(assignedV4Address, other.assignedV4Address)
                && Objects.equals(groupHint, other.groupHint)
                && Objects.equals(dnsAddresses, other.dnsAddresses)
                && Objects.equals(mtu, other.mtu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignedV4Address, groupHint, dnsAddresses, mtu);
    }
}
