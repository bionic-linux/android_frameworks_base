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

package android.net.ip;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.ip.IpManager.InitialConfiguration;
import android.os.INetworkManagementService;
import android.provider.Settings;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.mock.MockContentResolver;

import com.android.internal.util.test.FakeSettingsProvider;
import com.android.internal.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for IpManager.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class IpManagerTest {
    private static final int DEFAULT_AVOIDBADWIFI_CONFIG_VALUE = 1;

    @Mock private Context mContext;
    @Mock private INetworkManagementService mNMService;
    @Mock private Resources mResources;
    private MockContentResolver mContentResolver;

    @Before public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getInteger(R.integer.config_networkAvoidBadWifi))
                .thenReturn(DEFAULT_AVOIDBADWIFI_CONFIG_VALUE);

        mContentResolver = new MockContentResolver();
        mContentResolver.addProvider(Settings.AUTHORITY, new FakeSettingsProvider());
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
    }

    @Test
    public void testNullCallbackDoesNotThrow() throws Exception {
        final IpManager ipm = new IpManager(mContext, "lo", null, mNMService);
    }

    @Test
    public void testInvalidInterfaceDoesNotThrow() throws Exception {
        final IpManager.Callback cb = new IpManager.Callback();
        final IpManager ipm = new IpManager(mContext, "test_wlan0", cb, mNMService);
    }

    @Test
    public void testValidInitialConfigurations() throws Exception {
        InitialConfiguration[] invalidConfigurations = {
            // valid IPv4 configuration
            conf(links("192.0.2.12/24"), prefixes("192.0.2.0/24"), ips("192.0.2.2")),
            // valid IPv6 configuration
            conf(links("2001:db8:dead:beef:f00::a0/64"), prefixes("2001:db8:dead:beef::/64"),
                    ips("2001:db8:dead:beef:f00::02")),

            // TODO valid IPv6/v4 configuration
            // TODO valid IPv6 configuration without any GUA.
        };

        for (InitialConfiguration conf : invalidConfigurations) {
            if (!conf.isValid()) {
                fail(String.format("valid configution %s was not detected valid", conf));
            }
        }
    }

    @Test
    public void testInvalidInitialConfigurations() throws Exception {
        InitialConfiguration[] invalidConfigurations = {
            // addr and dns not in prefix
            conf(links("192.0.2.12/24"), prefixes("198.51.100.0/24"), ips("192.0.2.2")),
            // addr not in prefix
            conf(links("198.51.2.12/24"), prefixes("198.51.100.0/24"), ips("192.0.2.2")),
            // dns not in prefix
            conf(links("192.0.2.12/24"), prefixes("192.0.2.0/24"), ips("198.51.100.2")),

            // FIXME default ipv6 route and no GUA
            conf(links("2001:db8:dead:beef:f00::a0/128"), prefixes("::/0"), ips("198.51.100.2"))

            // invalid prefix length
            conf(links("2001:db8:dead:beef:f00::a0/128"), prefixes("2001:db8:dead:beef::/64"),
                    ips("2001:db8:dead:beef:f00::02")),
        };

        for (InitialConfiguration conf : invalidConfigurations) {
            if (conf.isValid()) {
                fail(String.format("invalid configution %s was not detected invalid", conf));
            }
        }
    }

    static InitialConfiguration conf(
            Set<LinkAddress> links, Set<IpPrefix> prefixes, Set<InetAddress> dns) {
        InitialConfiguration conf = new InitialConfiguration();
        conf.ipAddresses.addAll(links);
        conf.directlyConnectedRoutes.addAll(prefixes);
        conf.dnsServers.addAll(dns);
        return conf;
    }

    static Set<IpPrefix> prefixes(String... prefixes) {
        return mapIntoSet(prefixes, IpPrefix::new);
    }

    static Set<LinkAddress> links(String... addresses) {
        return mapIntoSet(addresses, LinkAddress::new);
    }

    static Set<InetAddress> ips(String... addresses) {
        return mapIntoSet(addresses, InetAddress::getByName);
    }

    static <A, B> Set<B> mapIntoSet(A[] in, Fn<A, B> fn) {
        Set<B> out = new HashSet<>(in.length);
        for (A item : in) {
            try {
                out.add(fn.call(item));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return out;
    }

    interface Fn<A,B> {
        B call(A a) throws Exception;
    }
}
