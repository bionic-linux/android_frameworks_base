/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net.ip;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.net.util.NetworkConstants;

import java.util.Arrays;
import java.util.Random;


/**
 * This controller is responsible for unique local address assignment.
 *
 * @hide
 */
public class UniqueLocalAddressController {
    private static final String TAG = UniqueLocalAddressController.class.getSimpleName();

    private final byte[] mUniqueLocalPrefix;
    private short mLastSubnetId;

    public UniqueLocalAddressController() {
        mUniqueLocalPrefix = generateUniqueLocalPrefix();
        mLastSubnetId = 0;
    }

    /** Get new subnet ID. */
    public short getNewSubnetId() {
        // Make sure subnet IDs are always positive. They are appended
        // to a ULA /48 to make a ULA /64 for local use.
        mLastSubnetId = (short) Math.max(0,  mLastSubnetId + 1);
        return mLastSubnetId;
    }

    /** Clear subnet ID counter. */
    public void clearSubnetCounter() {
        mLastSubnetId = 0;
    }

    /** Get Unique local address configuration. */
    public LinkProperties getUniqueLocalConfig(short subnetId) {
        return getUniqueLocalConfig(mUniqueLocalPrefix, subnetId);
    }

    private static LinkProperties getUniqueLocalConfig(byte[] ulp, short subnetId) {
        final LinkProperties lp = new LinkProperties();

        final IpPrefix local48 = makeUniqueLocalPrefix(ulp, (short) 0, 48);
        lp.addRoute(new RouteInfo(local48, null, null));

        final IpPrefix local64 = makeUniqueLocalPrefix(ulp, subnetId, 64);
        // Because this is a locally-generated ULA, we don't have an upstream
        // address. But because the downstream IP address management code gets
        // its prefix from the upstream's IP address, we create a fake one here.
        lp.addLinkAddress(new LinkAddress(local64.getAddress(), 64));

        lp.setMtu(NetworkConstants.ETHER_MTU);
        return lp;
    }

    private static IpPrefix makeUniqueLocalPrefix(byte[] in6addr, short subnetId, int prefixlen) {
        final byte[] bytes = Arrays.copyOf(in6addr, in6addr.length);
        bytes[6] = (byte) (subnetId >> 8);
        bytes[7] = (byte) subnetId;
        return new IpPrefix(bytes, prefixlen);
    }

    // Generates a Unique Locally-assigned Prefix:
    //
    //     https://tools.ietf.org/html/rfc4193#section-3.1
    //
    // The result is a /48 that can be used for local-only communications.
    private static byte[] generateUniqueLocalPrefix() {
        final byte[] ulp = new byte[6];  // 6 = 48bits / 8bits/byte
        (new Random()).nextBytes(ulp);

        final byte[] in6addr = Arrays.copyOf(ulp, NetworkConstants.IPV6_ADDR_LEN);
        in6addr[0] = (byte) 0xfd;  // fc00::/7 and L=1

        return in6addr;
    }
}
