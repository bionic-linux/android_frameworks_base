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

package android.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Parcel;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class IpPrefixCommonTest {

    private static InetAddress address(String addr) {
        return InetAddresses.parseNumericAddress(addr);
    }

    // Explicitly cast everything to byte because "error: possible loss of precision".
    private static final byte[] IPV4_BYTES = { (byte) 192, (byte) 0, (byte) 2, (byte) 4};
    private static final byte[] IPV6_BYTES = {
        (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
        (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef,
        (byte) 0x0f, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xa0
    };

    @Test
    public void testConstructor() {
        IpPrefix p;

        try {
            p = new IpPrefix((InetAddress) null, 10);
            fail("Expected NullPointerException: null InetAddress");
        } catch (RuntimeException expected) { }

        try {
            p = new IpPrefix((String) null);
            fail("Expected NullPointerException: null String");
        } catch (RuntimeException expected) { }

        try {
            p = new IpPrefix("1.2.3.4");
            fail("Expected IllegalArgumentException: no prefix length");
        } catch (IllegalArgumentException expected) { }

        try {
            p = new IpPrefix("1.2.3.4/");
            fail("Expected IllegalArgumentException: empty prefix length");
        } catch (IllegalArgumentException expected) { }

        try {
            p = new IpPrefix("foo/32");
            fail("Expected IllegalArgumentException: invalid address");
        } catch (IllegalArgumentException expected) { }

        try {
            p = new IpPrefix("1/32");
            fail("Expected IllegalArgumentException: deprecated IPv4 format");
        } catch (IllegalArgumentException expected) { }

        try {
            p = new IpPrefix("1.2.3.256/32");
            fail("Expected IllegalArgumentException: invalid IPv4 address");
        } catch (IllegalArgumentException expected) { }

        try {
            p = new IpPrefix("foo/32");
            fail("Expected IllegalArgumentException: non-address");
        } catch (IllegalArgumentException expected) { }

        try {
            p = new IpPrefix("f00:::/32");
            fail("Expected IllegalArgumentException: invalid IPv6 address");
        } catch (IllegalArgumentException expected) { }
    }

    private void assertAreEqual(Object o1, Object o2) {
        assertTrue(o1.equals(o2));
        assertTrue(o2.equals(o1));
    }

    private void assertAreNotEqual(Object o1, Object o2) {
        assertFalse(o1.equals(o2));
        assertFalse(o2.equals(o1));
    }

    @Test
    public void testEquals() throws UnknownHostException {
        IpPrefix p1, p2;

        p1 = new IpPrefix("192.0.2.251/23");
        p2 = new IpPrefix(InetAddress.getByAddress(
                new byte[] { (byte) 192, (byte) 0, (byte) 2, (byte) 251 }), 23);
        assertAreEqual(p1, p2);

        p1 = new IpPrefix("192.0.2.5/23");
        assertAreEqual(p1, p2);

        p1 = new IpPrefix("192.0.2.5/24");
        assertAreNotEqual(p1, p2);

        p1 = new IpPrefix("192.0.4.5/23");
        assertAreNotEqual(p1, p2);

        p1 = new IpPrefix("2001:db8:dead:beef:f00::80/122");
        p2 = new IpPrefix(InetAddress.getByAddress(IPV6_BYTES), 122);
        assertEquals("2001:db8:dead:beef:f00::80/122", p2.toString());
        assertAreEqual(p1, p2);

        p1 = new IpPrefix("2001:db8:dead:beef:f00::bf/122");
        assertAreEqual(p1, p2);

        p1 = new IpPrefix("2001:db8:dead:beef:f00::8:0/123");
        assertAreNotEqual(p1, p2);

        p1 = new IpPrefix("2001:db8:dead:beef::/122");
        assertAreNotEqual(p1, p2);

        // 192.0.2.4/32 != c000:0204::/32.
        byte[] ipv6bytes = new byte[16];
        System.arraycopy(IPV4_BYTES, 0, ipv6bytes, 0, IPV4_BYTES.length);
        p1 = new IpPrefix(InetAddress.getByAddress(ipv6bytes), 32);
        assertAreEqual(p1, new IpPrefix("c000:0204::/32"));

        p2 = new IpPrefix(InetAddress.getByAddress(IPV4_BYTES), 32);
        assertAreNotEqual(p1, p2);
    }

    @Test
    public void testContainsInetAddress() {
        IpPrefix p = new IpPrefix("2001:db8:f00::ace:d00d/127");
        assertTrue(p.contains(address("2001:db8:f00::ace:d00c")));
        assertTrue(p.contains(address("2001:db8:f00::ace:d00d")));
        assertFalse(p.contains(address("2001:db8:f00::ace:d00e")));
        assertFalse(p.contains(address("2001:db8:f00::bad:d00d")));
        assertFalse(p.contains(address("2001:4868:4860::8888")));
        assertFalse(p.contains(address("8.8.8.8")));

        p = new IpPrefix("192.0.2.0/23");
        assertTrue(p.contains(address("192.0.2.43")));
        assertTrue(p.contains(address("192.0.3.21")));
        assertFalse(p.contains(address("192.0.0.21")));
        assertFalse(p.contains(address("8.8.8.8")));
        assertFalse(p.contains(address("2001:4868:4860::8888")));

        IpPrefix ipv6Default = new IpPrefix("::/0");
        assertTrue(ipv6Default.contains(address("2001:db8::f00")));
        assertFalse(ipv6Default.contains(address("192.0.2.1")));

        IpPrefix ipv4Default = new IpPrefix("0.0.0.0/0");
        assertTrue(ipv4Default.contains(address("255.255.255.255")));
        assertTrue(ipv4Default.contains(address("192.0.2.1")));
        assertFalse(ipv4Default.contains(address("2001:db8::f00")));
    }

    private void runHashCodeTest(int addrLength) throws UnknownHostException {
        final Random random = new Random();

        IpPrefix oldP = new IpPrefix(InetAddress.getByAddress(new byte[addrLength]), 0);

        // Test IPv4
        final byte[] v4Bytes = new byte[addrLength];
        for (int i = 0; i < 100; i++) {
            random.nextBytes(v4Bytes);
            final InetAddress addr = InetAddress.getByAddress(v4Bytes);
            final IpPrefix p = new IpPrefix(addr, random.nextInt(8 * addrLength + 1));

            assertEquals(p.hashCode(), new IpPrefix(addr, p.getPrefixLength()).hashCode());
            if (oldP.hashCode() != p.hashCode()) {
                assertNotEquals(oldP, p);
            }

            oldP = p;
        }
    }

    @Test
    public void testHashCode() throws UnknownHostException {
        runHashCodeTest(4);
        runHashCodeTest(16);
    }

    @Test
    public void testHashCodeIsNotConstant() {
        IpPrefix[] prefixes = {
            new IpPrefix("2001:db8:f00::ace:d00d/127"),
            new IpPrefix("192.0.2.0/23"),
            new IpPrefix("::/0"),
            new IpPrefix("0.0.0.0/0"),
        };
        for (int i = 0; i < prefixes.length; i++) {
            for (int j = i + 1; j < prefixes.length; j++) {
                assertNotEquals(prefixes[i].hashCode(), prefixes[j].hashCode());
            }
        }
    }

    public IpPrefix passThroughParcel(IpPrefix p) {
        Parcel parcel = Parcel.obtain();
        IpPrefix p2 = null;
        try {
            p.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            p2 = IpPrefix.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
        assertNotNull(p2);
        return p2;
    }

    public void assertParcelingIsLossless(IpPrefix p) {
        IpPrefix p2 = passThroughParcel(p);
        assertEquals(p, p2);
    }

    @Test
    public void testParceling() {
        IpPrefix p;

        p = new IpPrefix("2001:4860:db8::/64");
        assertParcelingIsLossless(p);
        assertTrue(p.getAddress() instanceof Inet6Address);

        p = new IpPrefix("192.0.2.0/25");
        assertParcelingIsLossless(p);
        assertTrue(p.getAddress() instanceof Inet4Address);
    }
}
