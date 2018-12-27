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
package com.android.server.connectivity;

import static android.net.ConnectivityManager.PacketKeepalive.*;
import static android.net.NetworkAgent.EVENT_PACKET_KEEPALIVE;

import android.annotation.NonNull;
import android.net.KeepalivePacketData.InvalidPacketException;
import android.net.NetworkUtils;
import android.net.TcpKeepalivePacketData;
import android.net.util.FdEventsReader;
import android.net.util.IpUtils;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import java.io.FileDescriptor;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

public class TcpKeepaliveController {
    private static final String TAG = "SocketKeepaliveController";

    private final Handler mConnectivityServiceHandler;

    // Reference include/linux/socket.h
    private static final int SOL_TCP = 6;
    // Reference include/uapi/linux/tcp.h
    private static final int TCP_REPAIR = 19;
    private static final int TCP_REPAIR_QUEUE = 20;
    private static final int TCP_QUEUE_SEQ = 21;
    private static final int TCP_NO_QUEUE = 0;
    private static final int TCP_RECV_QUEUE = 1;
    private static final int TCP_SEND_QUEUE = 2;
    private static final int TCP_REPAIR_ON = 1;
    private static final int TCP_REPAIR_OFF = 0;

    /**
     * The default ipv4 MSS size.
     */
    protected static final int MAX_DATA_LENGTH = 1460;

    /** Keeps track of packet listners. */
    private final HashMap <Integer, PacketListener> mListeners;

    public TcpKeepaliveController(Handler handler) {
        mConnectivityServiceHandler = handler;
        mListeners = new HashMap<> ();
    }

    public static TcpKeepalivePacketData tcpKeepalivePacket(
            FileDescriptor fd) throws InvalidPacketException {
        Log.e(TAG, "start tcpKeepalivePacket....");
        final boolean isIPV4;
        final byte[] data;
        SocketAddress srcSockAddr = null;
        SocketAddress dstSockAddr = null;
        InetAddress srcAddress = null;
        InetAddress dstAddress = null;
        int srcPort = 0;
        int dstPort = 0;
        int sequence = 0;
        int ack = 0;
        // Query source address and port
        try {
            srcSockAddr = Os.getsockname(fd);
        } catch(ErrnoException e) {
            Log.e(TAG, "Get sockname fail: " + e);
            throw new InvalidPacketException(ERROR_INVALID_IP_ADDRESS);//TODO
        }
        if (srcSockAddr != null && srcSockAddr instanceof InetSocketAddress) {
            //InetSocketAddress inetAddr = (InetSocketAddress) srcSockAddr;
            //srcAddress = inetAddr.getAddress();
            //srcPort = inetAddr.getPort();
            srcAddress = getAddress((InetSocketAddress) srcSockAddr);
            srcPort = getPort((InetSocketAddress) srcSockAddr);
        } else {
            Log.e(TAG, "Invalid or mismatched SocketAddress");
        }
        // Query destination address and port
        try {
            dstSockAddr = Os.getpeername(fd);
        } catch(ErrnoException e) {
            Log.e(TAG, "Get peername fail: " + e);
            throw new InvalidPacketException(ERROR_INVALID_IP_ADDRESS);//TODO
        }
        if (dstSockAddr != null && dstSockAddr instanceof InetSocketAddress) {
            //InetSocketAddress inetAddr = (InetSocketAddress) dstSockAddr;
            //dstAddress = inetAddr.getAddress();
            //dstPort = inetAddr.getPort();
            dstAddress = getAddress((InetSocketAddress) dstSockAddr);
            dstPort = getPort((InetSocketAddress) dstSockAddr);
        } else {
            Log.e(TAG, "Invalid or mismatched peer SocketAddress");
        }

        if ((srcAddress instanceof Inet4Address) && (dstAddress instanceof Inet4Address)) {
            isIPV4 = true;
        } else if ((srcAddress instanceof Inet6Address) && (dstAddress instanceof Inet6Address)) {
            isIPV4 = false;
        } else {
            throw new InvalidPacketException(ERROR_INVALID_IP_ADDRESS);
        }
        // Query sequence and ack number
        try {
            NetworkUtils.attachDropAllPacketFilter(fd);
        } catch (SocketException e) {
            Log.e(TAG, "Socket Exception: " + e);
        }
        try {
            Os.setsockoptInt(fd, SOL_TCP, TCP_REPAIR, TCP_REPAIR_ON);
            Os.setsockoptInt(fd, SOL_TCP, TCP_REPAIR_QUEUE, TCP_SEND_QUEUE);
            sequence = Os.getsockoptInt(fd, SOL_TCP, TCP_QUEUE_SEQ);
            Os.setsockoptInt(fd, SOL_TCP, TCP_REPAIR_QUEUE, TCP_RECV_QUEUE);
            ack = Os.getsockoptInt(fd, SOL_TCP, TCP_QUEUE_SEQ);
            Os.setsockoptInt(fd, SOL_TCP, TCP_REPAIR_QUEUE, TCP_NO_QUEUE);
        } catch(ErrnoException e) {
            Log.e(TAG, "Errono Exception: " + e);
            throw new InvalidPacketException(ERROR_INVALID_IP_ADDRESS);//TODO
        }

        try {
            NetworkUtils.detachFilter(fd);
        } catch (SocketException e) {
            Log.e(TAG, "Socket Exception: " + e);
        }

        // TODO: consider overflow ??
        if (sequence != 0) sequence--;

        if(isIPV4) {
            data = TcpKeepalivePacketData.buildV4Packet(srcAddress, srcPort, dstAddress, dstPort, sequence, ack);
        } else {
            data = TcpKeepalivePacketData.buildV6Packet(srcAddress, srcPort, dstAddress, dstPort, sequence, ack);
        }
        return new TcpKeepalivePacketData(srcAddress, srcPort, dstAddress, dstPort, data, sequence, ack);
    }

    public void startSocketMonitor(FileDescriptor fd, Messenger messenger, int slot) {
        if (mListeners.containsKey(slot)) return;

        PacketListener listener = new PacketListener(mConnectivityServiceHandler, fd, messenger, slot);
        mListeners.put(slot, listener);
        listener.start();
    }

    public void stopSocketMonitor(int slot) {
        PacketListener listener = mListeners.get(slot);
        if (listener != null) {
            listener.stop();
            mListeners.remove(slot);
        }
    }

    private static InetAddress getAddress(InetSocketAddress inetAddr) {
        return inetAddr.getAddress();
    }

    private static int getPort(InetSocketAddress inetAddr) {
        return inetAddr.getPort();
    }

    public class PacketListener extends FdEventsReader<byte[]> {

        FileDescriptor mFd;
        Messenger mMessenger;
        int mSlot;

        public PacketListener(@NonNull Handler handler, @NonNull FileDescriptor fd,
                @NonNull Messenger messenger, int slot) {
            super(handler, new byte[MAX_DATA_LENGTH]);
            mFd = fd;
            mMessenger = messenger;
            mSlot = slot;
        }

        @Override
        protected int recvBufSize(@NonNull byte[] buffer) {
            return buffer.length;
        }

        @Override
        protected final void handlePacket(@NonNull byte[] recvbuf, int length) {
            // should not enter here
            Log.e(TAG, "handlePacket: should not see this log");
        }

        @Override
        protected int readPacket(@NonNull FileDescriptor fd, @NonNull byte[] packetBuffer)
                throws Exception {
            // TODO: Check repair queue to identify error ??
            Log.e(TAG, "redPacket happen !!");
            Os.setsockoptInt(fd, SOL_TCP, TCP_REPAIR, TCP_REPAIR_OFF);
            notifyMessenger(mMessenger, mSlot, -1); //TODO, what is the error code of onDataReceived
            mListeners.remove(mSlot);
            return 0;
        }

        @Override
        protected FileDescriptor createFd() {
            return mFd;
        }

        void notifyMessenger(Messenger messenger, int slot, int err) {
            Message message = Message.obtain();
            message.what = EVENT_PACKET_KEEPALIVE;
            message.arg1 = slot;
            message.arg2 = err;
            message.obj = null;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                // Process died?
            }
        }
    }
}
