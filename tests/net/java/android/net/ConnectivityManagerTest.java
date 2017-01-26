/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;
import org.junit.Test;



@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConnectivityManagerTest {
    @Test
    public void testNetworkCapabilitiesForTypeMobile() {
        final int type = ConnectivityManager.TYPE_MOBILE;
        final NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
        assertNotNull(nc);
        assertTrue(nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED));
    }

    @Test
    public void testNetworkCapabilitiesForTypeMobileDun() {
        final int type = ConnectivityManager.TYPE_MOBILE_DUN;
        final NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
        assertNotNull(nc);
        assertTrue(nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_DUN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED));
        assertFalse(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        assertFalse(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED));
    }

    @Test
    public void testNetworkCapabilitiesForTypeMobileHipri() {
        final int type = ConnectivityManager.TYPE_MOBILE_HIPRI;
        final NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
        assertNotNull(nc);
        assertTrue(nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED));
    }

    @Test
    public void testNetworkCapabilitiesForTypeWifi() {
        final int type = ConnectivityManager.TYPE_WIFI;
        final NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
        assertNotNull(nc);
        assertTrue(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED));
    }

    @Test
    public void testNetworkCapabilitiesForTypeWifiP2p() {
        final int type = ConnectivityManager.TYPE_WIFI_P2P;
        final NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
        assertNotNull(nc);
        assertTrue(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P));
        assertFalse(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
    }

    @Test
    public void testNetworkCapabilitiesForTypeBluetooth() {
        final int type = ConnectivityManager.TYPE_BLUETOOTH;
        final NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
        assertNotNull(nc);
        assertTrue(nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED));
    }

    @Test
    public void testNetworkCapabilitiesForTypeEthernet() {
        final int type = ConnectivityManager.TYPE_ETHERNET;
        final NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
        assertNotNull(nc);
        assertTrue(nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
        assertTrue(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED));
    }

    @Test
    public void testNetworkCapabilitiesForTypeUnsupported() {
        final int sampleUnsupportedTypes[] = {
                ConnectivityManager.TYPE_MOBILE_CBS,
                ConnectivityManager.TYPE_MOBILE_FOTA,
                ConnectivityManager.TYPE_NONE,
        };
        for (int type : sampleUnsupportedTypes) {
            NetworkCapabilities nc = ConnectivityManager.networkCapabilitiesForType(type);
            assertNull(nc);
        }
    }
}
