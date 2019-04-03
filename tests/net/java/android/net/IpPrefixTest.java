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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.UnknownHostException;

/**
 * Tests for {@link IpPrefix}.
 *
 * TODO: mark IpPrefix(byte[], int) and containsPrefix as @TestApi & move to IpPrefixCommonTest.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class IpPrefixTest {
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
            p = new IpPrefix((byte[]) null, 9);
            fail("Expected NullPointerException: null byte array");
        } catch(RuntimeException expected) {}

        try {
            byte[] b2 = {1, 2, 3, 4, 5};
            p = new IpPrefix(b2, 29);
            fail("Expected IllegalArgumentException: invalid array length");
        } catch(IllegalArgumentException expected) {}
    }

    @Test
    public void testTruncation() {
        IpPrefix p;

        p = new IpPrefix(IPV4_BYTES, 32);
        assertEquals("192.0.2.4/32", p.toString());

        p = new IpPrefix(IPV4_BYTES, 29);
        assertEquals("192.0.2.0/29", p.toString());

        p = new IpPrefix(IPV4_BYTES, 8);
        assertEquals("192.0.0.0/8", p.toString());

        p = new IpPrefix(IPV4_BYTES, 0);
        assertEquals("0.0.0.0/0", p.toString());

        try {
            p = new IpPrefix(IPV4_BYTES, 33);
            fail("Expected IllegalArgumentException: invalid prefix length");
        } catch(RuntimeException expected) {}

        try {
            p = new IpPrefix(IPV4_BYTES, 128);
            fail("Expected IllegalArgumentException: invalid prefix length");
        } catch(RuntimeException expected) {}

        try {
            p = new IpPrefix(IPV4_BYTES, -1);
            fail("Expected IllegalArgumentException: negative prefix length");
        } catch(RuntimeException expected) {}

        p = new IpPrefix(IPV6_BYTES, 128);
        assertEquals("2001:db8:dead:beef:f00::a0/128", p.toString());

        p = new IpPrefix(IPV6_BYTES, 122);
        assertEquals("2001:db8:dead:beef:f00::80/122", p.toString());

        p = new IpPrefix(IPV6_BYTES, 64);
        assertEquals("2001:db8:dead:beef::/64", p.toString());

        p = new IpPrefix(IPV6_BYTES, 3);
        assertEquals("2000::/3", p.toString());

        p = new IpPrefix(IPV6_BYTES, 0);
        assertEquals("::/0", p.toString());

        try {
            p = new IpPrefix(IPV6_BYTES, -1);
            fail("Expected IllegalArgumentException: negative prefix length");
        } catch(RuntimeException expected) {}

        try {
            p = new IpPrefix(IPV6_BYTES, 129);
            fail("Expected IllegalArgumentException: negative prefix length");
        } catch(RuntimeException expected) {}

    }

    @Test
    public void testContainsIpPrefix() {
        assertTrue(new IpPrefix("0.0.0.0/0").containsPrefix(new IpPrefix("0.0.0.0/0")));
        assertTrue(new IpPrefix("0.0.0.0/0").containsPrefix(new IpPrefix("1.2.3.4/0")));
        assertTrue(new IpPrefix("0.0.0.0/0").containsPrefix(new IpPrefix("1.2.3.4/8")));
        assertTrue(new IpPrefix("0.0.0.0/0").containsPrefix(new IpPrefix("1.2.3.4/24")));
        assertTrue(new IpPrefix("0.0.0.0/0").containsPrefix(new IpPrefix("1.2.3.4/23")));

        assertTrue(new IpPrefix("1.2.3.4/8").containsPrefix(new IpPrefix("1.2.3.4/8")));
        assertTrue(new IpPrefix("1.2.3.4/8").containsPrefix(new IpPrefix("1.254.12.9/8")));
        assertTrue(new IpPrefix("1.2.3.4/21").containsPrefix(new IpPrefix("1.2.3.4/21")));
        assertTrue(new IpPrefix("1.2.3.4/32").containsPrefix(new IpPrefix("1.2.3.4/32")));

        assertTrue(new IpPrefix("1.2.3.4/20").containsPrefix(new IpPrefix("1.2.3.0/24")));

        assertFalse(new IpPrefix("1.2.3.4/32").containsPrefix(new IpPrefix("1.2.3.5/32")));
        assertFalse(new IpPrefix("1.2.3.4/8").containsPrefix(new IpPrefix("2.2.3.4/8")));
        assertFalse(new IpPrefix("0.0.0.0/16").containsPrefix(new IpPrefix("0.0.0.0/15")));
        assertFalse(new IpPrefix("100.0.0.0/8").containsPrefix(new IpPrefix("99.0.0.0/8")));

        assertTrue(new IpPrefix("::/0").containsPrefix(new IpPrefix("::/0")));
        assertTrue(new IpPrefix("::/0").containsPrefix(new IpPrefix("2001:db8::f00/1")));
        assertTrue(new IpPrefix("::/0").containsPrefix(new IpPrefix("3d8a:661:a0::770/8")));
        assertTrue(new IpPrefix("::/0").containsPrefix(new IpPrefix("2001:db8::f00/8")));
        assertTrue(new IpPrefix("::/0").containsPrefix(new IpPrefix("2001:db8::f00/64")));
        assertTrue(new IpPrefix("::/0").containsPrefix(new IpPrefix("2001:db8::f00/113")));
        assertTrue(new IpPrefix("::/0").containsPrefix(new IpPrefix("2001:db8::f00/128")));

        assertTrue(new IpPrefix("2001:db8:f00::ace:d00d/64").containsPrefix(
                new IpPrefix("2001:db8:f00::ace:d00d/64")));
        assertTrue(new IpPrefix("2001:db8:f00::ace:d00d/64").containsPrefix(
                new IpPrefix("2001:db8:f00::ace:d00d/120")));
        assertFalse(new IpPrefix("2001:db8:f00::ace:d00d/64").containsPrefix(
                new IpPrefix("2001:db8:f00::ace:d00d/32")));
        assertFalse(new IpPrefix("2001:db8:f00::ace:d00d/64").containsPrefix(
                new IpPrefix("2006:db8:f00::ace:d00d/96")));

        assertTrue(new IpPrefix("2001:db8:f00::ace:d00d/128").containsPrefix(
                new IpPrefix("2001:db8:f00::ace:d00d/128")));
        assertTrue(new IpPrefix("2001:db8:f00::ace:d00d/100").containsPrefix(
                new IpPrefix("2001:db8:f00::ace:ccaf/110")));

        assertFalse(new IpPrefix("2001:db8:f00::ace:d00d/128").containsPrefix(
                new IpPrefix("2001:db8:f00::ace:d00e/128")));
        assertFalse(new IpPrefix("::/30").containsPrefix(new IpPrefix("::/29")));
    }
    @Test
    public void testMappedAddressesAreBroken() throws UnknownHostException {
        // 192.0.2.0/24 != ::ffff:c000:0204/120, but because we use InetAddress,
        // we are unable to comprehend that.
        byte[] ipv6bytes = {
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0xff, (byte) 0xff,
                (byte) 192, (byte) 0, (byte) 2, (byte) 0 };
        IpPrefix p = new IpPrefix(ipv6bytes, 120);
        assertEquals(16, p.getRawAddress().length);       // Fine.
        assertArrayEquals(ipv6bytes, p.getRawAddress());  // Fine.

        // Broken.
        assertEquals("192.0.2.0/120", p.toString());
        assertEquals(InetAddresses.parseNumericAddress("192.0.2.0"), p.getAddress());
    }

}
