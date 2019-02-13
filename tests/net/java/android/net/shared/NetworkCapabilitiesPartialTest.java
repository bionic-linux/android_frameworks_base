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

package android.net.shared;

import static android.net.NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_DUN;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_MMS;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static android.net.NetworkCapabilities.TRANSPORT_ETHERNET;
import static android.net.NetworkCapabilities.TRANSPORT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.net.shared.NetworkCapabilitiesPartial.fromStableParcelable;
import static android.net.shared.ParcelableTestUtil.assertFieldCountEquals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link NetworkCapabilitiesPartial}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class NetworkCapabilitiesPartialTest {
    private NetworkCapabilitiesPartial.Builder mCaps;

    @Before
    public void setUp() {
        mCaps = new NetworkCapabilitiesPartial.Builder()
                .addCapability(NET_CAPABILITY_NOT_METERED)
                .addCapability(NET_CAPABILITY_INTERNET)
                .addUnwantedCapability(NET_CAPABILITY_CAPTIVE_PORTAL)
                .addUnwantedCapability(NET_CAPABILITY_MMS)
                .setLinkDownBandwidthKbps(100)
                .setLinkUpBandwidthKbps(50)
                .addTransportType(TRANSPORT_WIFI)
                .addTransportType(TRANSPORT_ETHERNET)
                .setSsid("test_ssid");
        assertFieldCountEquals(6, NetworkCapabilitiesPartial.class);
    }

    @Test
    public void testEquals() {
        assertEquals(mCaps.build(), mCaps.build());
        assertEquals(mCaps.setSsid(null).build(), mCaps.setSsid(null).build());

        assertNotEquals(mCaps.build(), mCaps.addCapability(NET_CAPABILITY_MMS).build());
        assertNotEquals(mCaps.build(), mCaps.addUnwantedCapability(NET_CAPABILITY_DUN).build());
        assertNotEquals(
                mCaps.build(), mCaps.addUnwantedCapability(NET_CAPABILITY_INTERNET).build());
        assertNotEquals(mCaps.build(), mCaps.setLinkDownBandwidthKbps(200).build());
        assertNotEquals(mCaps.build(), mCaps.setLinkUpBandwidthKbps(100).build());
        assertNotEquals(mCaps.build(), mCaps.addTransportType(TRANSPORT_VPN).build());
        assertNotEquals(mCaps.build(), mCaps.setSsid("other"));
        assertNotEquals(mCaps.build(), mCaps.setSsid(null));
    }

    @Test
    public void testParcelUnparcel() {
        assertEquals(mCaps.build(), fromStableParcelable(mCaps.build().toStableParcelable()));
    }

    @Test
    public void testParcelUnparcel_NoCapabilities() {
        mCaps.setCapabilities(0L);
        assertEquals(mCaps.build(), fromStableParcelable(mCaps.build().toStableParcelable()));
    }

    @Test
    public void testParcelUnparcel_NoUnwantedCapabilities() {
        mCaps.setUnwantedCapabilities(0L);
        assertEquals(mCaps.build(), fromStableParcelable(mCaps.build().toStableParcelable()));
    }

    @Test
    public void testParcelUnparcel_NoTransportType() {
        mCaps.setTransportTypes(0L);
        assertEquals(mCaps.build(), fromStableParcelable(mCaps.build().toStableParcelable()));
    }

    @Test
    public void testParcelUnparcel_NullSsid() {
        mCaps.setSsid(null);
        assertEquals(mCaps.build(), fromStableParcelable(mCaps.build().toStableParcelable()));
    }
}
