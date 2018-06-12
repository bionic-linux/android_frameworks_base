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

import static android.net.NetworkUtils.getBroadcastAddress;
import static android.net.NetworkUtils.getPrefixMaskAsInet4Address;
import static android.net.TrafficStats.TAG_SYSTEM_DHCP_SERVER;
import static android.net.dhcp.DhcpPacket.DHCP_SERVER;
import static android.net.dhcp.DhcpPacket.ENCAP_BOOTP;
import static android.net.dhcp.DhcpPacket.INFINITE_LEASE;
import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.IPPROTO_UDP;
import static android.system.OsConstants.SOCK_DGRAM;
import static android.system.OsConstants.SOL_SOCKET;
import static android.system.OsConstants.SO_BINDTODEVICE;
import static android.system.OsConstants.SO_BROADCAST;
import static android.system.OsConstants.SO_REUSEADDR;

import static com.android.internal.util.Protocol.BASE_DHCP_SERVER;

import static java.lang.Integer.toUnsignedLong;

import android.annotation.Nullable;
import android.net.IpPrefix;
import android.net.MacAddress;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.util.InterfaceParams;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;

import libcore.io.Libcore;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * A DHCPv4 server.
 *
 * <p>This server listens for and responds to packets on a single interface and considers itself
 * authoritative for all leases on the subnet.
 *
 * <p>The server is single-threaded (including send/receive operations): all operations are
 * done on the provided {@link Looper}. Public methods are thread-safe and will schedule operations
 * on the looper asynchronously.
 * @hide
 */
public class DhcpServer {
    private static final String REPO_TAG = "Repository";

    private static final int MIN_LEASE_TIME_SECS = 120;

    private static final int CMD_START_DHCP_SERVER = BASE_DHCP_SERVER + 1;
    private static final int CMD_STOP_DHCP_SERVER = BASE_DHCP_SERVER + 2;
    private static final int CMD_UPDATE_PARAMS = BASE_DHCP_SERVER + 3;

    private final ServerHandler mHandler;
    private final InterfaceParams mIface;
    private final DhcpLeaseRepository mLeaseRepo;
    private final SharedLog mLog;
    private final Dependencies mDeps;
    private final Clock mClock;
    private final DhcpPacketListener mPacketListener;

    private FileDescriptor mSocket;
    private DhcpServingParams mServingParams;

    public static class Clock {
        /**
         * @see SystemClock#elapsedRealtime()
         */
        public long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }
    }

    public interface Dependencies {
        void sendPacket(FileDescriptor fd, ByteBuffer buffer, InetAddress dst)
                throws ErrnoException, IOException;
        DhcpLeaseRepository makeLeaseRepository();
        DhcpPacketListener makePacketListener();
        Clock makeClock();
        void addArpEntry(Inet4Address ipv4Addr, MacAddress ethAddr, String ifname,
                FileDescriptor fd) throws IOException;
    }

    private class DependenciesImpl implements Dependencies {
        @Override
        public void sendPacket(FileDescriptor fd, ByteBuffer buffer, InetAddress dst)
                throws ErrnoException, IOException {
            Os.sendto(fd, buffer, 0, dst, DhcpPacket.DHCP_CLIENT);
        }

        @Override
        public DhcpLeaseRepository makeLeaseRepository() {
            return new DhcpLeaseRepository(
                    DhcpServingParams.makeIpPrefix(mServingParams.serverAddr),
                    mServingParams.excludedAddrs,
                    mServingParams.dhcpLeaseTimeSecs*1000, mLog.forSubComponent(REPO_TAG), mClock);
        }

        @Override
        public DhcpPacketListener makePacketListener() {
            return new PacketListener();
        }

        @Override
        public Clock makeClock() {
            return new Clock();
        }

        @Override
        public void addArpEntry(Inet4Address ipv4Addr, MacAddress ethAddr, String ifname,
                FileDescriptor fd) throws IOException {
            NetworkUtils.addArpEntry(ipv4Addr, ethAddr, ifname, fd);
        }
    }

    public DhcpServer(Looper looper, InterfaceParams iface, DhcpServingParams params,
            SharedLog log) {
        this(looper, iface, params, log, null);
    }

    public DhcpServer(Looper looper, InterfaceParams iface, DhcpServingParams params,
            SharedLog log, Dependencies deps) {
        if (deps == null) {
            deps = new DependenciesImpl();
        }
        mHandler = new ServerHandler(looper);
        mIface = iface;
        mServingParams = params;
        mLog = log;
        mDeps = deps;
        mClock = deps.makeClock();
        mPacketListener = deps.makePacketListener();
        mLeaseRepo = deps.makeLeaseRepository();
    }

    /**
     * Start listening for and responding to packets.
     */
    public void start() {
        sendMessage(CMD_START_DHCP_SERVER, mServingParams);
    }

    /**
     * Update serving parameters. All subsequently received requests will be handled with the new
     * parameters, and current leases that are incompatible with the new parameters are dropped.
     */
    public void updateParams(DhcpServingParams params) {
        sendMessage(CMD_UPDATE_PARAMS, params);
    }

    /**
     * Stop listening for packets.
     */
    public void stop() {
        mHandler.sendEmptyMessage(CMD_STOP_DHCP_SERVER);
    }

    private void sendMessage(int what, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }

    private class ServerHandler extends Handler {
        public ServerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CMD_UPDATE_PARAMS:
                    final DhcpServingParams params = (DhcpServingParams)msg.obj;
                    mServingParams = params;
                    mLeaseRepo.updateParams(
                            DhcpServingParams.makeIpPrefix(mServingParams.serverAddr),
                            params.excludedAddrs,
                            params.dhcpLeaseTimeSecs);
                    break;
                case CMD_START_DHCP_SERVER:
                    mServingParams = (DhcpServingParams)msg.obj;
                    mPacketListener.start();
                    break;
                case CMD_STOP_DHCP_SERVER:
                    mPacketListener.stop();
                    break;
            }
        }
    }

    @VisibleForTesting
    void processPacket(DhcpPacket packet, Inet4Address srcAddr) {
        mLog.log("Received packet " + packet.getClass().getSimpleName());
        Inet4Address sid = packet.mServerIdentifier;
        if (sid != null && !sid.equals(mServingParams.serverAddr.getAddress())) {
            mLog.log("Packet ignored due to wrong server identifier: " + sid);
            return;
        }

        if (packet instanceof DhcpDiscoverPacket) {
            processDiscover((DhcpDiscoverPacket)packet, srcAddr);
        } else if (packet instanceof DhcpRequestPacket) {
            processRequest((DhcpRequestPacket)packet);
        } else if (packet instanceof DhcpReleasePacket) {
            processRelease((DhcpReleasePacket)packet);
        }
    }

    private void processDiscover(DhcpDiscoverPacket packet, Inet4Address srcIp) {
        final DhcpLease lease;
        final byte[] clientId = getClientId(packet);
        try {
            lease = mLeaseRepo.getOffer(clientId, MacAddress.fromBytes(packet.getClientMac()),
                    srcIp, packet.mRelayIp, packet.mRequestedIp, packet.mHostName);
        } catch (DhcpLeaseRepository.OutOfAddressesException e) {
            transmitNak(packet, "Out of DHCP addresses");
            return;
        } catch (DhcpLeaseRepository.InvalidAddressException e) {
            transmitNak(packet, "Invalid requested address");
            return;
        }

        transmitOffer(packet, lease);
    }

    private void processRequest(DhcpRequestPacket packet) {
        // If set, server ID matches as checked earlier
        final boolean sidSet = packet.mServerIdentifier != null;
        final byte[] clientId = getClientId(packet);

        final DhcpLease lease;
        try {
            lease = mLeaseRepo.requestLease(clientId,
                    MacAddress.fromBytes(packet.getClientMac()), packet.mClientIp,
                    packet.mRequestedIp, sidSet, packet.mHostName);
        } catch (DhcpLeaseRepository.InvalidAddressException e) {
            transmitNak(packet, "Invalid requested address");
            return;
        }

        transmitAck(packet, lease);
    }

    private void processRelease(DhcpReleasePacket packet) {
        final byte[] clientId = getClientId(packet);
        final MacAddress macAddr = MacAddress.fromBytes(packet.getClientMac());
        // Don't care about return value (no ACK/NAK); logging already done in the method.
        mLeaseRepo.releaseLease(clientId, macAddr, packet.mClientIp);
    }

    private Inet4Address getAckOrOfferDst(DhcpPacket request, DhcpLease lease) {
        if (!isEmpty(request.mClientIp)) {
            return request.mClientIp;
        } else if (request.mBroadcast) {
            return (Inet4Address)Inet4Address.ALL;
        } else {
            return lease.getNetAddr();
        }
    }

    private boolean transmitOffer(DhcpPacket request, DhcpLease lease) {
        mLog.log("Transmitting offer " + lease);

        Inet4Address dst = getAckOrOfferDst(request, lease);
        final boolean broadcastFlag = Inet4Address.ALL.equals(dst);
        if (!isEmpty(request.mRelayIp)) {
            dst = request.mRelayIp;
        }

        final int timeout = getLeaseTimeout(lease);
        final Inet4Address prefixMask =
                getPrefixMaskAsInet4Address(mServingParams.serverAddr.getPrefixLength());
        final Inet4Address broadcastAddr = getBroadcastAddress(
                mServingParams.getServerInet4Addr(), mServingParams.serverAddr.getPrefixLength());
        final ByteBuffer offerPacket = DhcpPacket.buildOfferPacket(
                ENCAP_BOOTP, request.mTransId, broadcastFlag, mServingParams.getServerInet4Addr(),
                lease.getNetAddr(), request.mClientMac, timeout,
                prefixMask,
                broadcastAddr,
                new ArrayList<>(mServingParams.defaultRouters),
                new ArrayList<>(mServingParams.dnsServers),
                mServingParams.getServerInet4Addr(), null /* domainName */);

        if (!addArpEntry(request, lease)) {
            // Error already logged
            return false;
        }
        return sendPacket(offerPacket, "DHCP offer", dst);
    }

    private boolean transmitAck(DhcpPacket request, DhcpLease lease) {
        mLog.log("Transmitting ACK " + lease);

        Inet4Address dst = getAckOrOfferDst(request, lease);
        final boolean broadcastFlag = Inet4Address.ALL.equals(dst);
        if (!isEmpty(request.mRelayIp)) {
            dst = request.mRelayIp;
        }

        final int timeout = getLeaseTimeout(lease);
        final ByteBuffer ackPacket = DhcpPacket.buildAckPacket(ENCAP_BOOTP, request.mTransId,
                broadcastFlag, mServingParams.getServerInet4Addr(), lease.getNetAddr(),
                request.mClientMac, timeout, mServingParams.getPrefixMaskAsAddress(),
                mServingParams.getBroadcastAddress(),
                new ArrayList<>(mServingParams.defaultRouters),
                new ArrayList<>(mServingParams.dnsServers),
                mServingParams.getServerInet4Addr(), null /* domainName */);

        if (!addArpEntry(request, lease)) {
            return false;
        }
        return sendPacket(ackPacket, "DHCP ACK", dst);
    }

    private boolean addArpEntry(DhcpPacket request, DhcpLease lease) {
        final MacAddress macAddr;
        try {
            macAddr = MacAddress.fromBytes(request.mClientMac);
        } catch (IllegalArgumentException e) {
            mLog.e("Invalid MAC address to transmit offer: "
                    + HexDump.dumpHexString(request.mClientMac));
            return false;
        }

        try {
            mDeps.addArpEntry(lease.getNetAddr(), macAddr, mIface.name, mSocket);
        } catch (IOException e) {
            mLog.e("Error adding client to ARP table", e);
            return false;
        }
        return true;
    }

    private boolean transmitNak(DhcpPacket request, String message) {
        mLog.w("Transmitting NAK: " + message);
        // Always set broadcast flag for NAK: client may not have a correct IP
        final ByteBuffer nakPacket = DhcpPacket.buildNakPacket(
                ENCAP_BOOTP, request.mTransId, mServingParams.getServerInet4Addr(),
                request.mClientMac, true /* broadcast */, message);

        final Inet4Address dst = isEmpty(request.mRelayIp)
                ? (Inet4Address)Inet4Address.ALL
                : request.mRelayIp;
        return sendPacket(nakPacket, "DHCP nak", dst);
    }


    private boolean sendPacket(ByteBuffer buf, String description, Inet4Address dst) {
        try {
            mDeps.sendPacket(mSocket, buf, dst);
        } catch(ErrnoException | IOException e) {
            mLog.e("Can't send packet " + description, e);
            return false;
        }
        return true;
    }

    /**
     * Get the remaining lease time in seconds, starting from {@link Clock#elapsedRealtime()}.
     *
     * <p>This is an unsigned 32-bit integer, so it cannot be read as a standard (signed) Java int.
     */
    private int getLeaseTimeout(DhcpLease lease) {
        final long remainingTimeSecs = (lease.getExpTime() - mClock.elapsedRealtime())/1000;
        if (remainingTimeSecs < 0) {
            mLog.e("Processing expired lease " + lease);
            return MIN_LEASE_TIME_SECS;
        }

        if (remainingTimeSecs >= toUnsignedLong(INFINITE_LEASE)) {
            return INFINITE_LEASE;
        }

        return (int)remainingTimeSecs;
    }

    private static @Nullable byte[] getClientId(DhcpPacket packet) {
        return packet.hasExplicitClientId() ? packet.getClientId() : null;
    }

    private static boolean isEmpty(Inet4Address address) {
        return address == null || Inet4Address.ANY.equals(address);
    }

    private class PacketListener extends DhcpPacketListener {
        public PacketListener() {
            super(mHandler);
        }

        @Override
        protected void onReceive(DhcpPacket packet, Inet4Address srcAddr) {
            processPacket(packet, srcAddr);
        }

        @Override
        protected void logError(String msg, Exception e) {
            mLog.e("Error receiving packet: " + msg, e);
        }

        @Override
        protected void logParseError(byte[] packet, int length, DhcpPacket.ParseException e) {
            mLog.e("Error parsing packet", e);
        }

        @Override
        protected FileDescriptor createFd() {
            try {
                // mSocket = Os.socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
                mSocket = Libcore.rawOs.socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
                Os.setsockoptInt(mSocket, SOL_SOCKET, SO_REUSEADDR, 1);
                // SO_BINDTODEVICE actually takes a string. This works because the first member
                // of struct ifreq is a NULL-terminated interface name.
                // TODO: add a setsockoptString()
                Os.setsockoptIfreq(mSocket, SOL_SOCKET, SO_BINDTODEVICE, mIface.name);
                Os.setsockoptInt(mSocket, SOL_SOCKET, SO_BROADCAST, 1);
                Os.bind(mSocket, Inet4Address.ANY, DHCP_SERVER);
                NetworkUtils.protectFromVpn(mSocket);
                TrafficStats.tagFileDescriptor(mSocket, TAG_SYSTEM_DHCP_SERVER);

                return mSocket;
            } catch(IOException | ErrnoException e) {
                mLog.e("Error creating UDP socket", e);
                DhcpServer.this.stop();
                return null;
            }
        }
    }
}
