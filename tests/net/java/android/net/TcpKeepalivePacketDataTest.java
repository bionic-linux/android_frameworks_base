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

package android.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.net.KeepalivePacketData.InvalidPacketException;
import android.net.TcpKeepalivePacketData.TcpSocketInfo;

import libcore.net.InetAddressUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.InetAddress;
import java.nio.ByteBuffer;

@RunWith(JUnit4.class)
public final class TcpKeepalivePacketDataTest {

    @Before
    public void setUp() {}

    @Test
    public void testV4TcpKeepalivePacket() {
        InetAddress srcAddr = InetAddressUtils.parseNumericAddress("192.168.0.1");
        InetAddress dstAddr = InetAddressUtils.parseNumericAddress("192.168.0.10");
        int srcPort = 1234;
        int dstPort = 4321;
        int sequence = 0x11111111;
        int ack = 0x22222222;
        TcpKeepalivePacketData resultData = null;
        TcpSocketInfo testInfo = new TcpSocketInfo(
                srcAddr, srcPort, dstAddr, dstPort, sequence, ack);
        try {
            resultData = TcpKeepalivePacketData.tcpKeepalivePacket(testInfo);
        } catch (InvalidPacketException e) {
            fail("InvalidPacketException" + e);
        }

        assertEquals(testInfo.srcAddress, resultData.srcAddress);
        assertEquals(testInfo.dstAddress, resultData.dstAddress);
        assertEquals(testInfo.srcPort, resultData.srcPort);
        assertEquals(testInfo.dstPort, resultData.dstPort);
        assertEquals(testInfo.sequence, resultData.tcpSequence);
        assertEquals(testInfo.ack, resultData.tcpAck);

        // IP version and TOS.
        ByteBuffer buf = ByteBuffer.wrap(resultData.getPacket());
        assertEquals(buf.getShort(), 0x4500);
        // Source IP address.
        byte[] ip = new byte[4];
        buf = ByteBuffer.wrap(resultData.getPacket(), 12, 4);
        buf.get(ip);
        assertArrayEquals(ip, srcAddr.getAddress());
        // Destinatin IP address.
        buf = ByteBuffer.wrap(resultData.getPacket(), 16, 4);
        buf.get(ip);
        assertArrayEquals(ip, dstAddr.getAddress());

        buf = ByteBuffer.wrap(resultData.getPacket(), 20, 12);
        // Source port.
        assertEquals(buf.getShort(), srcPort);
        // Destination port.
        assertEquals(buf.getShort(), dstPort);
        // Sequence number.
        assertEquals(buf.getInt(), sequence);
        // Ack.
        assertEquals(buf.getInt(), ack);
    }

    @Test
    public void testV6TcpKeepalivePacket() {
        InetAddress srcAddr = InetAddressUtils.parseNumericAddress("2001:4860:4860::8888");
        InetAddress dstAddr = InetAddressUtils.parseNumericAddress("2001:4860:4860::4444");
        int srcPort = 1234;
        int dstPort = 4321;
        int sequence = 0x11111111;
        int ack = 0x22222222;
        TcpKeepalivePacketData resultData = null;
        TcpSocketInfo testInfo = new TcpSocketInfo(
                srcAddr, srcPort, dstAddr, dstPort, sequence, ack);
        try {
            resultData = TcpKeepalivePacketData.tcpKeepalivePacket(testInfo);
        } catch (InvalidPacketException e) {
            fail("InvalidPacketException" + e);
        }

        assertEquals(testInfo.srcAddress, resultData.srcAddress);
        assertEquals(testInfo.dstAddress, resultData.dstAddress);
        assertEquals(testInfo.srcPort, resultData.srcPort);
        assertEquals(testInfo.dstPort, resultData.dstPort);
        assertEquals(testInfo.sequence, resultData.tcpSequence);
        assertEquals(testInfo.ack, resultData.tcpAck);

        ByteBuffer buf = ByteBuffer.wrap(resultData.getPacket());
        // IP version, traffic class, flow label.
        assertEquals(buf.getInt(), 0x60000000);
        // Source IP address.
        byte[] ip = new byte[16];
        buf = ByteBuffer.wrap(resultData.getPacket(), 8, 16);
        buf.get(ip);
        assertArrayEquals(ip, srcAddr.getAddress());
        // Destinatin IP address.
        buf = ByteBuffer.wrap(resultData.getPacket(), 24, 16);
        buf.get(ip);
        assertArrayEquals(ip, dstAddr.getAddress());

        buf = ByteBuffer.wrap(resultData.getPacket(), 40, 12);
        // Source port.
        assertEquals(buf.getShort(), srcPort);
        // Destination port.
        assertEquals(buf.getShort(), dstPort);
        // Sequence number.
        assertEquals(buf.getInt(), sequence);
        // Ack.
        assertEquals(buf.getInt(), ack);
    }
}
