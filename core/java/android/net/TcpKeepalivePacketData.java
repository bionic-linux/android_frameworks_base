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

import android.net.util.IpUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.system.OsConstants;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents the actual tcp keep alive packets which will be used for hardware offload.
 * @hide
 */

public class TcpKeepalivePacketData extends KeepalivePacketData implements Parcelable {
    private static final String TAG = "TcpKeepalivePacketData";

     /** TCP sequence number. */
    public final int tcpSeq;

    /** TCP ACK number. */
    public final int tcpAck;

    private static final int IPV4_HEADER_LENGTH = 20;
    private static final int IPV6_HEADER_LENGTH = 40;
    private static final int TCP_HEADER_LENGTH = 20;

    // This should only be constructed via static factory methods, such as
    // tcpKeepalivePacket.
    private TcpKeepalivePacketData(TcpSocketInfo tcpDetails, byte[] data)
            throws InvalidPacketException {
        super(tcpDetails.srcAddress, tcpDetails.srcPort, tcpDetails.dstAddress,
                tcpDetails.dstPort, data);
        tcpSeq = tcpDetails.seq;
        tcpAck = tcpDetails.ack;
    }

    /**
     * Factory method to create tcp keepalive packet structure.
     */
    public static TcpKeepalivePacketData tcpKeepalivePacket(
            TcpSocketInfo tcpDetails) throws InvalidPacketException {
        final byte[] packet;
        if ((tcpDetails.srcAddress instanceof Inet4Address)
                && (tcpDetails.dstAddress instanceof Inet4Address)) {
            packet = buildV4Packet(tcpDetails);
        } else {
            // TODO: support ipv6
            packet = null;
            throw new InvalidPacketException(SocketKeepalive.ERROR_INVALID_IP_ADDRESS);
        }

        return new TcpKeepalivePacketData(tcpDetails, packet);
    }

    /**
     * Build ipv4 tcp keepalive packet, not including the link-layer header.
     */
    private static byte[] buildV4Packet(TcpSocketInfo tcpDetails) {
        int length = IPV4_HEADER_LENGTH + TCP_HEADER_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 0x4500);               // IP version and TODO: TOS
        buf.putShort((short) length);
        buf.putInt((int) 0x400);                    // ID, flags=DF, offset
        buf.put((byte) 64);                         // TODO: TTL
        buf.put((byte) OsConstants.IPPROTO_TCP);
        int ipChecksumOffset = buf.position();
        buf.putShort((short) 0);                    // IP checksum
        buf.put(tcpDetails.srcAddress.getAddress());
        buf.put(tcpDetails.dstAddress.getAddress());
        buf.putShort((short) tcpDetails.srcPort);
        buf.putShort((short) tcpDetails.dstPort);
        buf.putInt(tcpDetails.seq);                 // Sequence Number
        buf.putInt(tcpDetails.ack);                 // ACK
        buf.putShort((short) 0x5010);               // TCP length=5, flags=ACK
        buf.putShort((short) 0x0100);               // TODO: Window size
        int tcpChecksumOffset = buf.position();
        buf.putShort((short) 0);                    // TCP checksum
        buf.putShort(ipChecksumOffset, IpUtils.ipChecksum(buf, 0));
        buf.putShort(tcpChecksumOffset, IpUtils.tcpChecksum(
                buf, 0, IPV4_HEADER_LENGTH, TCP_HEADER_LENGTH));
        Log.i(TAG, "v4 keepalive packet: " + buf);

        return buf.array();
    }

    /**
     * Build ipv6 tcp keepalive packet, not including the link-layer header.
     */
    private static byte[] buildV6Packet(TcpSocketInfo tcpDetails) {
        final int length = IPV6_HEADER_LENGTH + TCP_HEADER_LENGTH;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        // TODO: support ipv6
        Log.i(TAG, "v6 keepalive packet: " + buf);

        return buf.array();
    }

    /** Represents tcp/ip infromation. */
    public static class TcpSocketInfo {
        public final InetAddress srcAddress;
        public final InetAddress dstAddress;
        public final int srcPort;
        public final int dstPort;
        public final int seq;
        public final int ack;

        public TcpSocketInfo(InetAddress sAddr, int sPort, InetAddress dAddr,
                int dPort, int writeSeq, int readSeq) {
            srcAddress = sAddr;
            dstAddress = dAddr;
            srcPort = sPort;
            dstPort = dPort;
            seq = writeSeq;
            ack = readSeq;
        }
    }

    /* Parcelable Implementation. */
    /** No special parcel contents. */
    public int describeContents() {
        return 0;
    }

    /** Write to parcel. */
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(tcpSeq);
        out.writeInt(tcpAck);
    }

    private TcpKeepalivePacketData(Parcel in) {
        super(in);
        tcpSeq = in.readInt();
        tcpAck = in.readInt();
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
