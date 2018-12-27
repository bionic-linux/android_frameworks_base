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

import android.net.util.IpUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.system.OsConstants;
import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents the actual tcp keep alive packets which would be used for hardware offload.
 * @hide
 */

public class TcpKeepalivePacketData extends KeepalivePacketData implements Parcelable {
    private static final String TAG = "TcpKeepalivePacketData";

     /** TCP sequence number */
    public final int tcpSequence;

    /** TCP ACK number */
    public final int tcpAck;

    private final byte[] mApfPacket;
    private final byte[] mApfMask;

    private static final int IPV4_HEADER_LENGTH = 20;
    private static final int IPV6_HEADER_LENGTH = 40;
    private static final int TCP_HEADER_LENGTH = 20;

    // This should only be constructed via static factory methods, such as
    // tcpKeepalivePacket
    protected TcpKeepalivePacketData(InetAddress srcAddress, int srcPort, InetAddress dstAddress,
            int dstPort, byte[] data, int sequence, int ack) throws InvalidPacketException {
        super(srcAddress, srcPort, dstAddress, dstPort, data);
        tcpSequence = sequence;
        tcpAck = ack;
        // Base class already ensure srcAddress and dstAddress are same protocol.
        if (srcAddress instanceof Inet4Address) {
            mApfPacket = buildV4Packet(dstAddress, dstPort, srcAddress, srcPort, ack, sequence + 1);
            mApfMask = buildV4FilterMask();
        } else {
            mApfPacket = buildV6Packet(dstAddress, dstPort, srcAddress, srcPort, ack, sequence + 1);
            mApfMask = buildV6FilterMask();
        }
    }

    /**
     * Factory method to create tcp keepalive packet structure.
     */
    public static TcpKeepalivePacketData tcpKeepalivePacket(
            TcpSocketInfo tcpInfo) throws InvalidPacketException {
        final byte[] packet;
        if ((tcpInfo.srcAddress instanceof Inet4Address)
                && (tcpInfo.dstAddress instanceof Inet4Address)) {
            packet = buildV4Packet(
                    tcpInfo.srcAddress, tcpInfo.srcPort,
                    tcpInfo.dstAddress, tcpInfo.dstPort,
                    tcpInfo.sequence, tcpInfo.ack);
        } else if ((tcpInfo.srcAddress instanceof Inet6Address)
                && (tcpInfo.dstAddress instanceof Inet6Address)) {
            packet = buildV6Packet(
                    tcpInfo.srcAddress, tcpInfo.srcPort,
                    tcpInfo.dstAddress, tcpInfo.dstPort,
                    tcpInfo.sequence, tcpInfo.ack);
        } else {
            packet = null;
            throw new InvalidPacketException(SocketKeepalive.ERROR_INVALID_IP_ADDRESS);
        }

        return new TcpKeepalivePacketData(tcpInfo.srcAddress, tcpInfo.srcPort, tcpInfo.dstAddress,
                tcpInfo.dstPort, packet, tcpInfo.sequence, tcpInfo.ack);
    }

    /**
     * Need to drop the resulting ACKs in APF. Get expected ACK packet from this API.
     * The returned result can be used to identify if incoming packet need to be dropped.
     * @return A raw byte string of keepalive response packet data, not including the
     * link-layer header.
     */
    public byte[] getApfPacket() {
        return mApfPacket.clone();
    }

    /**
     * Get the a bitmask to determine whether packet is resulting ACK.
     * @return A raw byte string of response packet bitmask.
     */
    public byte[] getApfMask() {
        return mApfMask.clone();
    }

    /**
     * Bulid ipv4 tcp keepalive packet, not including the link-layer header.
     * @param srcAddress ipv4 source address
     * @param srcPort source port
     * @param dstAddress ipv4 destination address
     * @param dstPort destination port
     * @param sequence tcp sequence number
     * @param ack tcp ack number
     */
    private static byte[] buildV4Packet(InetAddress srcAddress,
            int srcPort, InetAddress dstAddress, int dstPort, int sequence, int ack) {

        int length = IPV4_HEADER_LENGTH + TCP_HEADER_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 0x4500);             // IP version and TOS
        buf.putShort((short) length);
        buf.putInt(0);                            // ID, flags, offset
        buf.put((byte) 64);                       // TTL
        buf.put((byte) OsConstants.IPPROTO_TCP);
        int ipChecksumOffset = buf.position();
        buf.putShort((short) 0);                  // IP checksum
        buf.put(srcAddress.getAddress());
        buf.put(dstAddress.getAddress());
        buf.putShort((short) srcPort);
        buf.putShort((short) dstPort);
        buf.putInt(sequence);                     // Sequence Number
        buf.putInt(ack);                          // ACK
        buf.putShort((short) 0x5010);             // TCP length, flags
        buf.putShort((short) 0x0100);             // Window size
        int tcpChecksumOffset = buf.position();
        buf.putShort((short) 0);                  // TCP checksum
        buf.putShort(ipChecksumOffset, IpUtils.ipChecksum(buf, 0));
        buf.putShort(tcpChecksumOffset, IpUtils.tcpChecksum(
                buf, 0, IPV4_HEADER_LENGTH, TCP_HEADER_LENGTH));
        Log.e(TAG, "v4 keepalive packet: " + buf);

        return buf.array();
    }

    /**
     * Bulid ipv6 tcp keepalive packet, not including the link-layer header.
     * @param srcAddress ipv6 source address
     * @param srcPort source port
     * @param dstAddress ipv6 destination address
     * @param dstPort destination port
     * @param sequence tcp sequence number
     * @param ack tcp ack number
     */
    private static byte[] buildV6Packet(InetAddress srcAddress,
            int srcPort, InetAddress dstAddress, int dstPort, int sequence, int ack) {

        int length = IPV6_HEADER_LENGTH + TCP_HEADER_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putInt(0x60000000);                   // IP version, traffic class, flow label
        buf.putShort((short) TCP_HEADER_LENGTH);  // Payload length
        buf.put((byte) OsConstants.IPPROTO_TCP);  // Next header
        buf.put((byte) 255);                       // Hop limit
        buf.put(srcAddress.getAddress());
        buf.put(dstAddress.getAddress());
        buf.putShort((short) srcPort);
        buf.putShort((short) dstPort);
        buf.putInt(sequence);                     // Sequence Number
        buf.putInt(ack);                          // ACK
        buf.putShort((short) 0x5010);             // TCP length, flags
        buf.putShort((short) 0x0100);             // Window size
        int tcpChecksumOffset = buf.position();
        buf.putShort((short) 0);                  // TCP checksum
        buf.putShort(tcpChecksumOffset, IpUtils.tcpChecksum(
                buf, 0, IPV6_HEADER_LENGTH, TCP_HEADER_LENGTH));
        Log.e(TAG, "v6 keepalive packet: " + buf);

        return buf.array();
    }

    private static byte[] buildV4FilterMask() {

        int length = IPV4_HEADER_LENGTH + TCP_HEADER_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 0);                  // IP version and TOS
        buf.putShort((short) 0);
        buf.putInt(0);                            // ID, flags, offset
        buf.put((byte) 0);                        // TTL
        buf.put((byte) 0);
        buf.putShort((short) 0);                  // IP checksum
        buf.putInt(0xffffffff);
        buf.putInt(0xffffffff);
        buf.putShort((short) 0xffff);
        buf.putShort((short) 0xffff);
        buf.putInt(0xffffffff);                   // Sequence Number
        buf.putInt(0xffffffff);                   // ACK
        buf.putShort((short) 0);                  // TCP length, flags
        buf.putShort((short) 0);                  // Window size
        buf.putShort((short) 0);                  // TCP checksum
        Log.e(TAG, "v4 mask: " + buf);

        return buf.array();
    }

    private static byte[] buildV6FilterMask() {

        int length = IPV6_HEADER_LENGTH + TCP_HEADER_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putInt(0);                            // IP version, traffic class, flow label
        buf.putShort((short) 0);                  // Payload length
        buf.putShort((short) 0);                   // Next header, hop limit
        buf.putInt(0xffffffff);                   // Source IP
        buf.putInt(0xffffffff);
        buf.putInt(0xffffffff);
        buf.putInt(0xffffffff);
        buf.putInt(0xffffffff);                   // Destination IP
        buf.putInt(0xffffffff);
        buf.putInt(0xffffffff);
        buf.putInt(0xffffffff);
        buf.putShort((short) 0xffff);
        buf.putShort((short) 0xffff);
        buf.putInt(0xffffffff);                   // Sequence Number
        buf.putInt(0xffffffff);                   // ACK
        buf.putShort((short) 0);                  // TCP length, flags
        buf.putShort((short) 0);                  // Window size
        buf.putShort((short) 0);                  // TCP checksum
        Log.e(TAG, "v6 mask: " + buf);

        return buf.array();
    }

    /** Represents tcp/ip infromation */
    public static class TcpSocketInfo {
        public final InetAddress srcAddress;
        public final InetAddress dstAddress;
        public final int srcPort;
        public final int dstPort;
        public final int sequence;
        public final int ack;

        public TcpSocketInfo(InetAddress sAddr, int sPort, InetAddress dAddr,
                int dPort, int writeSeq, int readSeq) {
            srcAddress = sAddr;
            dstAddress = dAddr;
            srcPort = sPort;
            dstPort = dPort;
            sequence = writeSeq;
            ack = readSeq;
        }
    }

    /* Parcelable Implementation */
    /** No special parcel contents. */
    public int describeContents() {
        return 0;
    }

    /** Write to parcel. */
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(tcpSequence);
        out.writeInt(tcpAck);
        out.writeByteArray(mApfPacket);
        out.writeByteArray(mApfMask);
    }

    private TcpKeepalivePacketData(Parcel in) {
        super(in);
        tcpSequence = in.readInt();
        tcpAck = in.readInt();
        mApfPacket = in.createByteArray();
        mApfMask = in.createByteArray();
    }

    /** Parcelable Creator. */
    public static final Parcelable.Creator<TcpKeepalivePacketData> CREATOR =
            new Parcelable.Creator<TcpKeepalivePacketData>() {
                public TcpKeepalivePacketData createFromParcel(Parcel in) {
                    return new TcpKeepalivePacketData(in);
                }

                public TcpKeepalivePacketData[] newArray(int size) {
                    return new TcpKeepalivePacketData[size];
                }
            };
}
