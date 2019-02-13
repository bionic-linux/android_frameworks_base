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

package android.net.util;

import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import static junit.framework.Assert.assertEquals;

import android.net.NetworkCapabilities;
import android.net.shared.NetworkCapabilitiesPartial;
import android.net.shared.ParcelableTestUtil;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

/**
 * Tests for {@link PartialViewUtil}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PartialViewUtilTest {
    @Test
    public void testNetworkCapabilitiesPartial() {
        final int[] caps = new int[] { NET_CAPABILITY_INTERNET, NET_CAPABILITY_NOT_METERED };
        final int[] unwantedCaps = new int[] { NET_CAPABILITY_NOT_VPN };
        final int[] transportTypes = new int[] { TRANSPORT_VPN, TRANSPORT_WIFI };
        final int upstreamBandwidth = 123;
        final int downstreamBandwidth = 456;
        final String ssid = "test_ssid";
        ParcelableTestUtil.assertFieldCountEquals(6, NetworkCapabilitiesPartial.class);

        final NetworkCapabilities nc = new NetworkCapabilities();
        nc.setCapabilities(caps, unwantedCaps);
        nc.setTransportTypes(transportTypes);
        nc.setLinkUpstreamBandwidthKbps(upstreamBandwidth);
        nc.setLinkDownstreamBandwidthKbps(downstreamBandwidth);
        nc.setSSID(ssid);

        final NetworkCapabilitiesPartial.Builder expectedBuilder =
                new NetworkCapabilitiesPartial.Builder();
        Arrays.stream(caps).forEach(expectedBuilder::addCapability);
        Arrays.stream(unwantedCaps).forEach(expectedBuilder::addUnwantedCapability);
        Arrays.stream(transportTypes).forEach(expectedBuilder::addTransportType);
        final NetworkCapabilitiesPartial expected = expectedBuilder
                .setLinkUpBandwidthKbps(upstreamBandwidth)
                .setLinkDownBandwidthKbps(downstreamBandwidth)
                .setSsid(ssid)
                .build();

        assertEquals(expected, PartialViewUtil.toPartial(nc));
    }
}
