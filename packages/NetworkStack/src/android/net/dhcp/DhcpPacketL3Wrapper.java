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

import static android.net.dhcp.DhcpPacket.DHCP_CLIENT;
import static android.net.dhcp.DhcpPacket.DHCP_SERVER;
import static android.net.dhcp.DhcpPacket.ENCAP_L2;
import static android.net.dhcp.DhcpPacket.ENCAP_L3;
import static android.net.dhcp.DhcpPacket.ETHER_BROADCAST;
import static android.net.dhcp.DhcpPacket.INADDR_ANY;
import static android.net.dhcp.DhcpPacket.MAX_LENGTH;
import static android.net.dhcp.DhcpPacket.readIpAddress;

import android.net.metrics.DhcpErrorEvent;
import android.system.OsConstants;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Defines L3 layer wrapper class used for encapsulation and parse of the
 * DHCP packet including IP header and upper layer header.
 *
 */
public class DhcpPacketL3Wrapper {
    static final int MIN_PACKET_LENGTH_L3 = DhcpPacket.MIN_PACKET_LENGTH_BOOTP + 20 + 8;
    static final int MIN_PACKET_LENGTH_L2 = MIN_PACKET_LENGTH_L3 + 14;

    /**
     * IP layer definitions.
     */
    static final byte IP_TYPE_UDP = (byte) 0x11;
    /**
     * IP: Version 4, Header Length 20 bytes
     */
    private static final byte IP_VERSION_HEADER_LEN = (byte) 0x45;
    /**
     * IP: TOS
     */
    private static final byte IP_TOS_LOWDELAY = (byte) 0x10;
    /**
     * IP: Flags 0, Fragment Offset 0, Don't Fragment
     */
    private static final short IP_FLAGS_OFFSET = (short) 0x4000;
    /**
     * IP: TTL -- use default 64 from RFC1340
     */
    private static final byte IP_TTL = (byte) 0x40;
    public final Inet4Address mSrcAddr;
    public final Inet4Address mDstAddr;
    public final int mSrcPort;
    public final int mDstPort;
    public final DhcpPacket mPacket;

    public DhcpPacketL3Wrapper(Inet4Address srcAddr, Inet4Address dstAddr, boolean clientPacket,
            DhcpPacket packet) {
        this(srcAddr, dstAddr,
                clientPacket ? DHCP_CLIENT : DHCP_SERVER /* srcPort */,
                clientPacket ? DHCP_SERVER : DHCP_CLIENT /* dstPort */,
                packet);
    }

    public DhcpPacketL3Wrapper(Inet4Address srcAddr, Inet4Address dstAddr, int srcPort, int dstPort,
            DhcpPacket packet) {
        mSrcAddr = srcAddr;
        mDstAddr = dstAddr;
        mSrcPort = srcPort;
        mDstPort = dstPort;
        mPacket = packet;
    }

    /**
     * Create a new L2 packet (including ethernet header) containing the upper layer payload.
     *
     */
    public ByteBuffer buildL2Packet(byte[] clientMac) {
        ByteBuffer buf = ByteBuffer.allocate(MAX_LENGTH);

        buf.put(ETHER_BROADCAST);
        buf.put(clientMac);
        buf.putShort((short) OsConstants.ETH_P_IP);

        return buildPacket(buf);
    }

    /**
     * Create a new L3 packet (including IP header and UDP header) containing the DHCP payload.
     *
     */
    public ByteBuffer buildPacket() {
        ByteBuffer buf = ByteBuffer.allocate(MAX_LENGTH);
        return buildPacket(buf);
    }

    private ByteBuffer buildPacket(ByteBuffer buf) {
        final int ipHeaderOffset = buf.position();
        buf.put(IP_VERSION_HEADER_LEN);
        buf.put(IP_TOS_LOWDELAY);  // tos: IPTOS_LOWDELAY
        final int ipLengthOffset = buf.position();
        buf.putShort((short) 0);  // length
        buf.putShort((short) 0);  // id
        buf.putShort(IP_FLAGS_OFFSET);  // ip offset: don't fragment
        buf.put(IP_TTL);   // TTL: use default 64 from RFC1340
        buf.put(IP_TYPE_UDP);
        final int ipChecksumOffset = buf.position();
        buf.putShort((short) 0);  // checksum

        buf.put(mSrcAddr.getAddress());
        buf.put(mDstAddr.getAddress());
        final int endIpHeader = buf.position();

        // UDP header
        final int udpHeaderOffset = buf.position();
        buf.putShort((short) mSrcPort);
        buf.putShort((short) mDstPort);
        final int udpLengthOffset = buf.position();
        buf.putShort((short) 0);  // length
        final int udpChecksumOffset = buf.position();
        buf.putShort((short) 0);  // UDP checksum -- initially zero

        // Payload
        mPacket.writePacket(buf);

        // Compute IP & UDP checksums
        // fix UDP header: insert length
        short udpLen = (short) (buf.position() - udpHeaderOffset);
        buf.putShort(udpLengthOffset, udpLen);
        // fix UDP header: checksum
        // checksum for UDP at udpChecksumOffset
        int udpSeed = 0;

        // apply IPv4 pseudo-header.  Read IP address src and destination
        // values from the IP header and accumulate checksum.
        udpSeed += intAbs(buf.getShort(ipChecksumOffset + 2));
        udpSeed += intAbs(buf.getShort(ipChecksumOffset + 4));
        udpSeed += intAbs(buf.getShort(ipChecksumOffset + 6));
        udpSeed += intAbs(buf.getShort(ipChecksumOffset + 8));

        // accumulate extra data for the pseudo-header
        udpSeed += IP_TYPE_UDP;
        udpSeed += udpLen;
        // and compute UDP checksum
        buf.putShort(udpChecksumOffset, (short) checksum(buf, udpSeed,
                udpHeaderOffset,
                buf.position()));
        // fix IP header: insert length
        buf.putShort(ipLengthOffset, (short) (buf.position() - ipHeaderOffset));
        // fixup IP-header checksum
        buf.putShort(ipChecksumOffset,
                (short) checksum(buf, 0, ipHeaderOffset, endIpHeader));

        buf.flip();
        return buf;
    }

    /**
     * Parse a packet from an array of bytes, stopping at the given length.
     */
    public static DhcpPacketL3Wrapper decodePacket(byte[] packet, int length, int pktType)
            throws DhcpPacket.ParseException {
        ByteBuffer buffer = ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN);
        try {
            return decodePacket(buffer, pktType);
        } catch (DhcpPacket.ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new DhcpPacket.ParseException(DhcpErrorEvent.PARSING_ERROR, e.getMessage());
        }
    }

    /**
     * Parse a packet including the L2 header or L3 header or udp header from an array of bytes,
     * stopping at the given length.
     */
    public static DhcpPacketL3Wrapper decodePacket(ByteBuffer packet, int pktType)
            throws DhcpPacket.ParseException {
        packet.order(ByteOrder.BIG_ENDIAN);
        if (pktType == ENCAP_L2) {
            if (packet.remaining() < MIN_PACKET_LENGTH_L2) {
                throw new DhcpPacket.ParseException(DhcpErrorEvent.L2_TOO_SHORT,
                        "L2 packet too short, %d < %d", packet.remaining(), MIN_PACKET_LENGTH_L2);
            }

            byte[] l2dst = new byte[6];
            byte[] l2src = new byte[6];

            packet.get(l2dst);
            packet.get(l2src);

            short l2type = packet.getShort();

            if (l2type != OsConstants.ETH_P_IP) {
                throw new DhcpPacket.ParseException(DhcpErrorEvent.L2_WRONG_ETH_TYPE,
                        "Unexpected L2 type 0x%04x, expected 0x%04x", l2type, OsConstants.ETH_P_IP);
            }
        }

        final Inet4Address ipSrc;
        final Inet4Address ipDst;
        final short udpSrcPort;
        final short udpDstPort;
        if (pktType <= ENCAP_L3) {
            if (packet.remaining() < MIN_PACKET_LENGTH_L3) {
                throw new DhcpPacket.ParseException(DhcpErrorEvent.L3_TOO_SHORT,
                        "L3 packet too short, %d < %d", packet.remaining(), MIN_PACKET_LENGTH_L3);
            }

            byte ipTypeAndLength = packet.get();
            int ipVersion = (ipTypeAndLength & 0xf0) >> 4;
            if (ipVersion != 4) {
                throw new DhcpPacket.ParseException(
                        DhcpErrorEvent.L3_NOT_IPV4, "Invalid IP version %d", ipVersion);
            }

            // System.out.println("ipType is " + ipType);
            byte ipDiffServicesField = packet.get();
            short ipTotalLength = packet.getShort();
            short ipIdentification = packet.getShort();
            byte ipFlags = packet.get();
            byte ipFragOffset = packet.get();
            byte ipTTL = packet.get();
            byte ipProto = packet.get();
            short ipChksm = packet.getShort();

            ipSrc = readIpAddress(packet);
            ipDst = readIpAddress(packet);

            if (ipProto != IP_TYPE_UDP) {
                throw new DhcpPacket.ParseException(
                        DhcpErrorEvent.L4_NOT_UDP, "Protocol not UDP: %d", ipProto);
            }

            // Skip options. This cannot cause us to read beyond the end of the buffer because the
            // IPv4 header cannot be more than (0x0f * 4) = 60 bytes long, and that is less than
            // MIN_PACKET_LENGTH_L3.
            int optionWords = ((ipTypeAndLength & 0x0f) - 5);
            for (int i = 0; i < optionWords; i++) {
                packet.getInt();
            }

            // assume UDP
            udpSrcPort = packet.getShort();
            udpDstPort = packet.getShort();
            short udpLen = packet.getShort();
            short udpChkSum = packet.getShort();

            // Only accept packets to or from the well-known client port (expressly permitting
            // packets from ports other than the well-known server port; http://b/24687559), and
            // server-to-server packets, e.g. for relays.
            if (!isPacketToOrFromClient(udpSrcPort, udpDstPort)
                    && !isPacketServerToServer(udpSrcPort, udpDstPort)) {
                // This should almost never happen because we use SO_ATTACH_FILTER on the packet
                // socket to drop packets that don't have the right source ports. However, it's
                // possible that a packet arrives between when the socket is bound and when the
                // filter is set. http://b/26696823 .
                throw new DhcpPacket.ParseException(DhcpErrorEvent.L4_WRONG_PORT,
                        "Unexpected UDP ports %d->%d", udpSrcPort, udpDstPort);
            }
        } else {
            ipSrc = INADDR_ANY;
            ipDst = INADDR_ANY;
            udpSrcPort = 0;
            udpDstPort = 0;
        }

        final DhcpPacket dhcpPacket = DhcpPacket.decodeFullPacket(packet);
        return new DhcpPacketL3Wrapper(ipSrc, ipDst, udpSrcPort, udpDstPort, dhcpPacket);
    }

    private static boolean isPacketToOrFromClient(short udpSrcPort, short udpDstPort) {
        return (udpSrcPort == DHCP_CLIENT) || (udpDstPort == DHCP_CLIENT);
    }

    private static boolean isPacketServerToServer(short udpSrcPort, short udpDstPort) {
        return (udpSrcPort == DHCP_SERVER) && (udpDstPort == DHCP_SERVER);
    }

    /**
     * Converts a signed short value to an unsigned int value.  Needed
     * because Java does not have unsigned types.
     */
    private static int intAbs(short v) {
        return v & 0xFFFF;
    }

    /**
     * Performs an IP checksum (used in IP header and across UDP
     * payload) on the specified portion of a ByteBuffer.  The seed
     * allows the checksum to commence with a specified value.
     */
    private static int checksum(ByteBuffer buf, int seed, int start, int end) {
        int sum = seed;
        int bufPosition = buf.position();

        // set position of original ByteBuffer, so that the ShortBuffer
        // will be correctly initialized
        buf.position(start);
        ShortBuffer shortBuf = buf.asShortBuffer();

        // re-set ByteBuffer position
        buf.position(bufPosition);

        short[] shortArray = new short[(end - start) / 2];
        shortBuf.get(shortArray);

        for (short s : shortArray) {
            sum += intAbs(s);
        }

        start += shortArray.length * 2;

        // see if a singleton byte remains
        if (end != start) {
            short b = buf.get(start);

            // make it unsigned
            if (b < 0) {
                b += 256;
            }

            sum += b * 256;
        }

        sum = ((sum >> 16) & 0xFFFF) + (sum & 0xFFFF);
        sum = ((sum + ((sum >> 16) & 0xFFFF)) & 0xFFFF);
        int negated = ~sum;
        return intAbs((short) negated);
    }
}
