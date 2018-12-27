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
package com.android.server.connectivity;

import static android.net.NetworkAgent.EVENT_SOCKET_KEEPALIVE;
import static android.net.SocketKeepalive.DATA_RECEIVED;
import static android.net.SocketKeepalive.ERROR_INVALID_SOCKET;
import static android.net.SocketKeepalive.ERROR_SOCKET_NOT_IDLE;
import static android.system.OsConstants.FIONREAD;
import static android.system.OsConstants.IPPROTO_TCP;
import static android.system.OsConstants.TIOCOUTQ;

import android.annotation.NonNull;
import android.net.NetworkUtils;
import android.net.SocketKeepalive.InvalidSocketException;
import android.net.TcpKeepalivePacketData.TcpSocketInfo;
import android.net.TcpRepairWindow;
import android.net.shared.FdEventsReader;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Int32Ref;
import android.system.Os;
import android.util.Log;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;

/**
 * Manage tcp socket which offloads tcp keepalive.
 *
 * The input socket will be changed to repair mode and the application
 * will not have permission to read/write data. If the application wants
 * to write data, it must stop tcp keepalive offload to leave repair mode
 * first. If a remote packet arrives, repair mode will be turned off and
 * offload will be stopped. The application will receive a callback to know
 * it can start reading data.
 *
 * Please note that this class is not thread-safe : all its public methods
 * must be called on a single thread. Namely, they are called by KeepaliveTracker,
 * which in turn is only called on the ConnectivityService thread.
 * @hide
 */
public class TcpKeepaliveController {
    private static final String TAG = "TcpKeepaliveController";
    private static final boolean DBG = false;

    private final Handler mConnectivityServiceHandler;

    // Reference include/uapi/linux/tcp.h
    private static final int TCP_REPAIR = 19;
    private static final int TCP_REPAIR_QUEUE = 20;
    private static final int TCP_QUEUE_SEQ = 21;
    private static final int TCP_NO_QUEUE = 0;
    private static final int TCP_RECV_QUEUE = 1;
    private static final int TCP_SEND_QUEUE = 2;
    private static final int TCP_REPAIR_OFF = 0;
    private static final int TCP_REPAIR_ON = 1;
    // Reference include/uapi/linux/sockios.h
    private static final int SIOCINQ = FIONREAD;
    private static final int SIOCOUTQ = TIOCOUTQ;

    /**
     * The default ipv4 MSS size.
     * 1500(mtu) - 20(ipv4) - 20(tcp) = 1460(max tcp mss)
     */
    private static final int MAX_DATA_LENGTH = 1460;

    /**
     * Keeps track of packet listeners.
     * Key: slot number of keepalive offload.
     * Value: {@link #PacketListener}
     */
    private final HashMap<Integer, PacketListener> mListeners;

    public TcpKeepaliveController(Handler handler) {
        mConnectivityServiceHandler = handler;
        mListeners = new HashMap<>();
    }

    /**
     * Switch the tcp socket to repair mode and query tcp socket information.
     *
     * @param fd the fd of socket on which to use keepalive offload
     * @return a {@link TcpKeepalivePacketData#TcpSocketInfo} object for current
     * tcp/ip information.
     */
    public static TcpSocketInfo switchToRepairMode(FileDescriptor fd)
            throws InvalidSocketException {
        if (DBG) Log.i(TAG, "switchToRepairMode to start tcp keepalive : " + fd);
        final boolean isIPV4;
        final byte[] data;
        final SocketAddress srcSockAddr;
        final SocketAddress dstSockAddr;
        final InetAddress srcAddress;
        final InetAddress dstAddress;
        final int srcPort;
        final int dstPort;
        int seq = 0;
        final int ack;
        final TcpRepairWindow trw;

        // Query source address and port.
        try {
            srcSockAddr = Os.getsockname(fd);
        } catch (ErrnoException e) {
            Log.e(TAG, "Get sockname fail: ", e);
            throw new InvalidSocketException(ERROR_INVALID_SOCKET, e);
        }
        if (srcSockAddr instanceof InetSocketAddress) {
            srcAddress = getAddress((InetSocketAddress) srcSockAddr);
            srcPort = getPort((InetSocketAddress) srcSockAddr);
        } else {
            Log.e(TAG, "Invalid or mismatched SocketAddress");
            throw new InvalidSocketException(ERROR_INVALID_SOCKET);
        }
        // Query destination address and port.
        try {
            dstSockAddr = Os.getpeername(fd);
        } catch (ErrnoException e) {
            Log.e(TAG, "Get peername fail: ", e);
            throw new InvalidSocketException(ERROR_INVALID_SOCKET, e);
        }
        if (dstSockAddr instanceof InetSocketAddress) {
            dstAddress = getAddress((InetSocketAddress) dstSockAddr);
            dstPort = getPort((InetSocketAddress) dstSockAddr);
        } else {
            Log.e(TAG, "Invalid or mismatched peer SocketAddress");
            throw new InvalidSocketException(ERROR_INVALID_SOCKET);
        }

        // Query sequence and ack number
        dropAllIncomingPackets(fd, true);
        try {
            // Enter tcp repair mode.
            Os.setsockoptInt(fd, IPPROTO_TCP, TCP_REPAIR, TCP_REPAIR_ON);
            // Check if socket is idle.
            if (!isSocketIdle(fd)) {
                throw new InvalidSocketException(ERROR_SOCKET_NOT_IDLE);
            }
            // Query write sequence number from SEND_QUEUE.
            Os.setsockoptInt(fd, IPPROTO_TCP, TCP_REPAIR_QUEUE, TCP_SEND_QUEUE);
            seq = Os.getsockoptInt(fd, IPPROTO_TCP, TCP_QUEUE_SEQ);
            // Query read sequence number from RECV_QUEUE.
            Os.setsockoptInt(fd, IPPROTO_TCP, TCP_REPAIR_QUEUE, TCP_RECV_QUEUE);
            ack = Os.getsockoptInt(fd, IPPROTO_TCP, TCP_QUEUE_SEQ);
            // Switch to NO_QUEUE to prevent illegal socket read/write in repair mode.
            Os.setsockoptInt(fd, IPPROTO_TCP, TCP_REPAIR_QUEUE, TCP_NO_QUEUE);
            // Finally, check if socket is still idle. TODO : this check needs to move to
            // after starting polling to prevent a race.
            if (!isSocketIdle(fd)) {
                throw new InvalidSocketException(ERROR_INVALID_SOCKET);
            }

            // Query tcp window size.
            trw = NetworkUtils.getTcpRepairWindow(fd);
        } catch (ErrnoException e) {
            Log.e(TAG, "Exception reading TCP state from socket", e);
            try {
                Os.setsockoptInt(fd, IPPROTO_TCP, TCP_REPAIR, TCP_REPAIR_OFF);
            } catch (ErrnoException ex) {
                Log.e(TAG, "Exception while turning off repair mode due to exception", ex);
            }
            throw new InvalidSocketException(ERROR_INVALID_SOCKET, e);
        } finally {
            dropAllIncomingPackets(fd, false);
        }

        // Keepalive sequence number is last sequence number - 1. If it couldn't be retrieved,
        // then it must be set to -1, so decrement in all cases.
        seq = seq - 1;

        return new TcpSocketInfo(srcAddress, srcPort, dstAddress, dstPort, seq, ack, trw.rcvWnd);
    }

    /**
     * Start monitor if socket has incoming packet.
     *
     * @param fd socket fd to monitor.
     * @param messenger a callback to notify socket status.
     * @param slot keepalive slot.
     */
    public void startSocketMonitor(FileDescriptor fd, Messenger messenger, int slot) {
        if (mListeners.containsKey(slot)) return;

        PacketListener listener = new PacketListener(fd, messenger, slot);
        mListeners.put(slot, listener);
        listener.start();
    }

    /** Stop socket monitor */
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

    private static boolean isSocketIdle(FileDescriptor fd) throws ErrnoException {
        return isReceiveQueueEmpty(fd) && isSendQueueEmpty(fd);
    }

    private static boolean isReceiveQueueEmpty(FileDescriptor fd)
            throws ErrnoException {
        Int32Ref result = new Int32Ref(-1);
        Os.ioctlInt(fd, SIOCINQ, result);
        if (result.value != 0) {
            Log.e(TAG, "Read queue has data");
            return false;
        }
        return true;
    }

    private static boolean isSendQueueEmpty(FileDescriptor fd)
            throws ErrnoException {
        Int32Ref result = new Int32Ref(-1);
        Os.ioctlInt(fd, SIOCOUTQ, result);
        if (result.value != 0) {
            Log.e(TAG, "Write queue has data");
            return false;
        }
        return true;
    }

    private static void dropAllIncomingPackets(FileDescriptor fd, boolean enable)
            throws InvalidSocketException {
        try {
            if (enable) {
                NetworkUtils.attachDropAllBPFFilter(fd);
            } else {
                NetworkUtils.detachBPFFilter(fd);
            }
        } catch (SocketException e) {
            Log.e(TAG, "Socket Exception: " + e);
            throw new InvalidSocketException(ERROR_INVALID_SOCKET, e);
        }
    }

    // TODO : FdEventsReader is not the right tool for the job. It requires a non-blocking socket,
    // which this is not required to supply, but this subclass works around this by overriding
    // the readPacket method with one that does not actually read from the socket. Todo : clean up
    /** Monitor if socket is ready to read or has error. */
    private class PacketListener extends FdEventsReader<byte[]> {

        final FileDescriptor mFd;
        final Messenger mMessenger;
        final int mSlot;

        PacketListener(@NonNull FileDescriptor fd, @NonNull Messenger messenger, int slot) {
            super(mConnectivityServiceHandler, new byte[MAX_DATA_LENGTH]);
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
            Log.wtf(TAG, "Bug: handlePacket called unexpectedly.");
        }

        @Override
        protected int readPacket(@NonNull FileDescriptor fd, @NonNull byte[] packetBuffer)
                throws Exception {
            if (DBG) Log.i(TAG, "Received packet, length : " + packetBuffer);

            PacketListener listener = mListeners.remove(mSlot);
            if (null != listener) {
                listener.stop();
                Os.setsockoptInt(fd, IPPROTO_TCP, TCP_REPAIR, TCP_REPAIR_OFF);
                final int error = isReceiveQueueEmpty(mFd) ? ERROR_INVALID_SOCKET : DATA_RECEIVED;
                notifyMessenger(mMessenger, mSlot, error);
            }
            return 0;
        }

        @Override
        protected FileDescriptor createFd() {
            return mFd;
        }

        private void notifyMessenger(Messenger messenger, int slot, int err) {
            final Message message = Message.obtain();
            message.what = EVENT_SOCKET_KEEPALIVE;
            message.arg1 = slot;
            message.arg2 = err;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                // Process died?
            }
        }
    }
}
