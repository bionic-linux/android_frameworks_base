/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.net.util;

import static android.os.MessageQueue.OnFileDescriptorEventListener.EVENT_INPUT;
import static android.os.MessageQueue.OnFileDescriptorEventListener.EVENT_ERROR;

import android.annotation.Nullable;
import android.os.Handler;
import android.os.MessageQueue;
import android.os.MessageQueue.OnFileDescriptorEventListener;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import libcore.io.IoBridge;
import libcore.io.IoUtils;

import java.io.FileDescriptor;
import java.io.IOException;


/**
 * A thread that reads from a socket and passes the received packets to a
 * subclass's handlePacket() method.  The packet receive buffer is recycled
 * on every read call, so subclasses should make any copies they would like
 * inside their handlePacket() implementation.
 *
 * All public methods may be called from any thread.
 *
 * @hide
 */
public abstract class BlockingSocketReader {
    public static final int DEFAULT_RECV_BUF_SIZE = 2 * 1024;
    private static final int FD_EVENTS = EVENT_INPUT | EVENT_ERROR;
    private static final int UNREGISTER_THIS_FD = 0;

    private final MessageQueue mQueue;
    private final byte[] mPacket;
    private volatile FileDescriptor mSocket;
    private volatile long mPacketsReceived;

    // Make it slightly easier for subclasses to properly close a socket
    // without having to know this incantation.
    public static final void closeSocket(@Nullable FileDescriptor fd) {
        if (fd == null) return;
        try {
            IoBridge.closeAndSignalBlockedThreads(fd);
        } catch (IOException ignored) {}
    }

    protected BlockingSocketReader(Handler h) {
        this(h, DEFAULT_RECV_BUF_SIZE);
    }

    protected BlockingSocketReader(Handler h, int recvbufsize) {
        mQueue = h.getLooper().getQueue();
        if (recvbufsize < DEFAULT_RECV_BUF_SIZE) {
            recvbufsize = DEFAULT_RECV_BUF_SIZE;
        }
        mPacket = new byte[recvbufsize];
    }

    public final boolean start() {
        if (mSocket != null) return false;

        try {
            mSocket = createSocket();
            if (mSocket != null) {
                // Force sockets to be non-blocking.
                IoUtils.setBlocking(mSocket, false);
            }
        } catch (Exception e) {
            logError("Failed to create socket: ", e);
            closeSocket(mSocket);
            mSocket = null;
            return false;
        }

        if (mSocket == null) return false;

        mQueue.addOnFileDescriptorEventListener(
                mSocket,
                FD_EVENTS,
                new OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        if (fd != mSocket) return UNREGISTER_THIS_FD;

                        final boolean hasInput = ((events & EVENT_INPUT) != 0);
                        final boolean hasError = ((events & EVENT_ERROR) != 0);
                        if (hasError || (hasInput && !handleInput())) {
                            return UNREGISTER_THIS_FD;
                        }
                        return FD_EVENTS;
                    }
                });
        return true;
    }

    public final void stop() {
        if (!isRunning()) return;

        mQueue.removeOnFileDescriptorEventListener(mSocket);
        closeSocket(mSocket);
        mSocket = null;
        onExit();
    }

    public final boolean isRunning() { return (mSocket != null); }

    public final long numPacketsReceived() { return mPacketsReceived; }

    /**
     * Subclasses MUST create the listening socket here, including setting
     * all desired socket options, interface or address/port binding, etc.
     */
    protected abstract FileDescriptor createSocket();

    /**
     * Subclasses MAY override this to change the default read() implementation
     * in favour of, say, recvfrom().
     *
     * Implementations MUST return the bytes read or throw an Exception.
     */
    protected int readPacket(FileDescriptor fd, byte[] packetBuffer) throws Exception {
        return Os.read(fd, packetBuffer, 0, packetBuffer.length);
    }

    /**
     * Called by the main loop for every packet.  Any desired copies of
     * |recvbuf| should be made in here, as the underlying byte array is
     * reused across all reads.
     */
    protected void handlePacket(byte[] recvbuf, int length) {}

    /**
     * Called by the main loop to log errors.  In some cases |e| may be null.
     */
    protected void logError(String msg, Exception e) {}

    /**
     * Called by the main loop just prior to exiting.
     */
    protected void onExit() {}

    // Keep trying to read until we get EAGAIN/EWOULDBLOCK.
    private boolean handleInput() {
        while (isRunning()) {
            final int bytesRead;

            try {
                bytesRead = readPacket(mSocket, mPacket);
                if (bytesRead < 1) {
                    if (isRunning()) logError("Socket closed, exiting", null);
                    break;
                }
                mPacketsReceived++;
            } catch (ErrnoException e) {
                if (e.errno == OsConstants.EAGAIN) {
                    // We've read everything there is to read this time around.
                    return true;
                }
                if (e.errno != OsConstants.EINTR) {
                    if (isRunning()) logError("read error: ", e);
                    break;
                }
                continue;
            } catch (Exception e) {
                if (isRunning()) logError("read error: ", e);
                continue;
            }

            try {
                handlePacket(mPacket, bytesRead);
            } catch (Exception e) {
                logError("Unexpected exception: ", e);
                break;
            }
        }

        return false;
    }
}
