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

package android.net.dhcp;

import static android.net.dhcp.DhcpPacket.ENCAP_BOOTP;
import static android.net.dhcp.DhcpPacket.INADDR_ANY;
import static android.net.dhcp.DhcpPacket.INADDR_BROADCAST;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.net.InetAddress.parseNumericAddress;

import android.net.IpPrefix;
import android.net.MacAddress;
import android.net.dhcp.DhcpLeaseRepository.InvalidAddressException;
import android.net.dhcp.DhcpLeaseRepository.OutOfAddressesException;
import android.net.dhcp.DhcpServer.Clock;
import android.net.dhcp.DhcpServer.Dependencies;
import android.net.util.InterfaceParams;
import android.net.util.SharedLog;
import android.os.test.TestLooper;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DhcpServerTest {
    private static final String TEST_IFACE = "testiface";
    private static final MacAddress TEST_IFACE_MAC = MacAddress.fromString("11:22:33:44:55:66");
    private static final InterfaceParams TEST_IFACEPARAMS =
            new InterfaceParams(TEST_IFACE, 1, TEST_IFACE_MAC);

    private static final Set<Inet4Address> TEST_DEFAULT_ROUTERS = new HashSet<>(
            Arrays.asList(parseAddr("192.168.0.123"), parseAddr("192.168.0.124")));
    private static final long TEST_LEASE_TIME_SECS = 3600L;
    private static final Set<Inet4Address> TEST_DNS_SERVERS = new HashSet<>(
            Arrays.asList(parseAddr("192.168.0.126"), parseAddr("192.168.0.127")));
    private static final Inet4Address TEST_SERVER_ADDR = parseAddr("192.168.0.2");
    private static final IpPrefix TEST_PREFIX = new IpPrefix(parseAddr("192.168.0.0"), 20);
    private static final int TEST_MTU = 1500;
    private static final Set<Inet4Address> TEST_EXCLUDED_ADDRS = new HashSet<>(
            Arrays.asList(parseAddr("192.168.0.200"), parseAddr("192.168.0.201")));

    private static final long TEST_TIME = 1234L;

    private static final int TEST_TRANS_ID = 123;
    private static final byte[] TEST_CLIENT_MAC_BYTES = new byte [] { 1, 2, 3, 4, 5, 6 };
    private static final MacAddress TEST_CLIENT_MAC = MacAddress.fromBytes(TEST_CLIENT_MAC_BYTES);
    private static final Inet4Address TEST_CLIENT_ADDR = parseAddr("192.168.0.42");
    private static final int TEST_EXPTIME_SECS = 3600;
    private static final DhcpLease TEST_LEASE = new DhcpLease(null, TEST_CLIENT_MAC,
            TEST_CLIENT_ADDR, TEST_EXPTIME_SECS + TEST_TIME, null /* hostname */);

    private @Mock Dependencies mDeps;
    private @Mock DhcpLeaseRepository mRepository;
    private @Mock DhcpPacketListener mPacketListener;
    private @Mock Clock mClock;

    private @Captor ArgumentCaptor<ByteBuffer> mSentPacketCaptor;
    private @Captor ArgumentCaptor<Inet4Address> mResponseDstAddrCaptor;

    private TestLooper mLooper;

    private DhcpServer mServer;

    @Before
    public void setUp() throws Exception {
        // Allow mocking package-private classes
        System.setProperty("dexmaker.share_classloader", "true");
        MockitoAnnotations.initMocks(this);

        final DhcpServingParams servingParams = new DhcpServingParams.Builder()
                .setDefaultRouters(TEST_DEFAULT_ROUTERS)
                .setDhcpLeaseTimeSecs(TEST_LEASE_TIME_SECS)
                .setDnsServers(TEST_DNS_SERVERS)
                .setServerAddr(TEST_SERVER_ADDR)
                .setOnlinkPrefix(TEST_PREFIX)
                .setLinkMtu(TEST_MTU)
                .setExcludedAddrs(TEST_EXCLUDED_ADDRS)
                .build();

        when(mDeps.makeLeaseRepository()).thenReturn(mRepository);
        when(mDeps.makeClock()).thenReturn(mClock);
        when(mDeps.makePacketListener()).thenReturn(mPacketListener);
        doNothing().when(mDeps)
                .sendPacket(any(), mSentPacketCaptor.capture(), mResponseDstAddrCaptor.capture());

        when(mClock.elapsedRealtime()).thenReturn(TEST_TIME);

        mLooper = new TestLooper();
        mServer = new DhcpServer(mLooper.getLooper(), TEST_IFACEPARAMS, servingParams,
                new SharedLog(DhcpServerTest.class.getSimpleName()), mDeps);

        // TEST
        Thread.sleep(10000);

        mServer.start();
        mLooper.dispatchAll();
    }

    @After
    public void tearDown() {
        // Calling stop() several times is not an issue
        mServer.stop();
    }

    @Test
    public void testStart() throws Exception {
        verify(mPacketListener, times(1)).start();
    }

    @Test
    public void testStop() throws Exception {
        mServer.stop();
        mLooper.dispatchAll();
        verify(mPacketListener, times(1)).stop();
    }

    @Test
    public void testDiscover() throws Exception {
        // TODO: refactor packet construction to eliminate unnecessary/confusing/duplicate fields
        when(mRepository.getOffer(isNull(), eq(MacAddress.fromBytes(TEST_CLIENT_MAC_BYTES)),
                eq(INADDR_ANY) /* reqSrcAddr */, isNull() /* relayAddr */, isNull() /* reqAddr */,
                isNull() /* hostname */))
                .thenReturn(TEST_LEASE);

        final DhcpDiscoverPacket discover = new DhcpDiscoverPacket(TEST_TRANS_ID,
                (short)0 /* secs */,
                INADDR_ANY /* relayIp */, TEST_CLIENT_MAC_BYTES, false /* broadcast */,
                INADDR_ANY /* srcIp */);
        mServer.processPacket(discover, INADDR_ANY /* srcAddr */);

        assertEquals(TEST_CLIENT_ADDR, mResponseDstAddrCaptor.getValue());
        final DhcpOfferPacket packet = assertOffer(getPacket());
        assertMatchesTestLease(packet);
    }

    @Test
    public void testDiscover_OutOfAddresses() throws Exception {
        when(mRepository.getOffer(isNull(), eq(MacAddress.fromBytes(TEST_CLIENT_MAC_BYTES)),
                eq(INADDR_ANY) /* reqSrcAddr */, isNull() /* relayAddr */, isNull() /* reqAddr */,
                isNull() /* hostname */))
                .thenThrow(new OutOfAddressesException("Test exception"));

        final DhcpDiscoverPacket discover = new DhcpDiscoverPacket(TEST_TRANS_ID,
                (short)0 /* secs */,
                INADDR_ANY /* relayIp */, TEST_CLIENT_MAC_BYTES, false /* broadcast */,
                INADDR_ANY /* srcIp */);
        mServer.processPacket(discover, INADDR_ANY /* srcAddr */);

        assertEquals(INADDR_BROADCAST, mResponseDstAddrCaptor.getValue());
        final DhcpNakPacket packet = assertNak(getPacket());
        assertMatchesClient(packet);
    }

    @Test
    public void testRequest_Selecting_Ack() throws Exception {
        when(mRepository.requestLease(isNull(), eq(MacAddress.fromBytes(TEST_CLIENT_MAC_BYTES)),
                eq(INADDR_ANY) /* clientAddr */, TEST_CLIENT_ADDR /* reqAddr */, true,
                isNull() /* hostname */))
                .thenReturn(TEST_LEASE);

        final DhcpRequestPacket request = new DhcpRequestPacket(TEST_TRANS_ID, (short)0 /* secs */,
                INADDR_ANY /* clientIp */, INADDR_ANY /* relayIp */, TEST_CLIENT_MAC_BYTES, false);
        request.mServerIdentifier = TEST_SERVER_ADDR;
        mServer.processPacket(request, INADDR_ANY /* srcAddr */);

        assertEquals(TEST_CLIENT_ADDR, mResponseDstAddrCaptor.getValue());
        final DhcpAckPacket packet = assertAck(getPacket());
        assertMatchesTestLease(packet);
    }

    @Test
    public void testRequest_Selecting_Nak() throws Exception {
        when(mRepository.requestLease(isNull(), eq(MacAddress.fromBytes(TEST_CLIENT_MAC_BYTES)),
                eq(INADDR_ANY) /* clientAddr */, TEST_CLIENT_ADDR /* reqAddr */, true,
                isNull() /* hostname */))
                .thenThrow(new InvalidAddressException("Test error"));

        final DhcpRequestPacket request = new DhcpRequestPacket(TEST_TRANS_ID, (short)0 /* secs */,
                INADDR_ANY /* clientIp */, INADDR_ANY /* relayIp */, TEST_CLIENT_MAC_BYTES, false);
        request.mServerIdentifier = TEST_SERVER_ADDR;
        mServer.processPacket(request, INADDR_ANY /* srcAddr */);

        assertEquals(INADDR_BROADCAST, mResponseDstAddrCaptor.getValue());
        final DhcpNakPacket packet = assertNak(getPacket());
        assertMatchesClient(packet);
    }

    @Test
    public void testRelease() throws Exception {
        final DhcpReleasePacket release = new DhcpReleasePacket(TEST_TRANS_ID, TEST_SERVER_ADDR,
                TEST_CLIENT_ADDR, INADDR_ANY /* relayIp */, TEST_CLIENT_MAC_BYTES);
        mServer.processPacket(release, TEST_CLIENT_ADDR);

        verify(mRepository, times(1))
                .releaseLease(isNull(), eq(TEST_CLIENT_MAC), eq(TEST_CLIENT_ADDR));
    }

    /* TODO: add more tests once packet construction is refactored, including:
     *  - usage of giaddr
     *  - usage of broadcast bit
     *  - other request states (init-reboot/renewing/rebinding)
     */

    private void assertMatchesTestLease(DhcpPacket packet) {
        assertMatchesClient(packet);
        assertFalse(packet.hasExplicitClientId());
        assertEquals(TEST_SERVER_ADDR, packet.mServerIdentifier);
        assertEquals(TEST_CLIENT_ADDR, packet.mYourIp);
        assertNotNull(packet.mLeaseTime);
        assertEquals(TEST_EXPTIME_SECS, (int)packet.mLeaseTime);
        assertNull(packet.mHostName);
    }

    private void assertMatchesClient(DhcpPacket packet) {
        assertEquals(TEST_TRANS_ID, packet.mTransId);
        assertEquals(TEST_CLIENT_MAC, MacAddress.fromBytes(packet.mClientMac));
    }

    private static DhcpNakPacket assertNak(DhcpPacket packet) {
        assertTrue(packet instanceof DhcpNakPacket);
        return (DhcpNakPacket)packet;
    }

    private static DhcpAckPacket assertAck(DhcpPacket packet) {
        assertTrue(packet instanceof DhcpAckPacket);
        return (DhcpAckPacket)packet;
    }

    private static DhcpOfferPacket assertOffer(DhcpPacket packet) {
        assertTrue(packet instanceof DhcpOfferPacket);
        return (DhcpOfferPacket)packet;
    }

    private DhcpPacket getPacket() throws Exception {
        verify(mDeps, times(1)).sendPacket(any(), any(), any());
        return DhcpPacket.decodeFullPacket(mSentPacketCaptor.getValue(), ENCAP_BOOTP);
    }

    private static Inet4Address parseAddr(String inet4Addr) {
        return (Inet4Address) parseNumericAddress(inet4Addr);
    }
}
