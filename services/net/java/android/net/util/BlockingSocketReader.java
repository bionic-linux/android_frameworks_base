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
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.MessageQueue.OnFileDescriptorEventListener;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
// XXX
import android.util.Log;

import libcore.io.IoBridge;
import libcore.io.IoUtils;

import java.io.FileDescriptor;
import java.io.InterruptedIOException;
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

    private final byte[] mPacket;
    private final HandlerThread mThread;
    private Looper mLooper;
    private Handler mHandler;
    private volatile FileDescriptor mSocket;
    private volatile boolean mRunning;
    private volatile long mPacketsReceived;

    // Make it slightly easier for subclasses to properly close a socket
    // without having to know this incantation.
    public static final void closeSocket(@Nullable FileDescriptor fd) {
        if (fd == null) return;
        try {
            IoBridge.closeAndSignalBlockedThreads(fd);
        } catch (IOException ignored) {}
    }

    protected BlockingSocketReader() {
        this(DEFAULT_RECV_BUF_SIZE);
    }

    protected BlockingSocketReader(int recvbufsize) {
        if (recvbufsize < DEFAULT_RECV_BUF_SIZE) {
            recvbufsize = DEFAULT_RECV_BUF_SIZE;
        }
        mPacket = new byte[recvbufsize];
        mThread = new HandlerThread(BlockingSocketReader.class.getSimpleName());
    }

    public final boolean start() {
        if (mSocket != null) return false;

        try {
            mSocket = createSocket();
        } catch (Exception e) {
            logError("Failed to create socket: ", e);
            return false;
        }

        if (mSocket == null) return false;

        mThread.start();
        mLooper = mThread.getLooper();
        mHandler = mThread.getThreadHandler();
        mHandler.post(() -> { setupListeningSocket(); });
        mRunning = true;
        return true;
    }

    public final void stop() {
        mRunning = false;
        if (mHandler != null) {
            mHandler.post(() -> {
                if (mSocket != null) {
                    mLooper.getQueue().removeOnFileDescriptorEventListener(mSocket);
                    closeSocket(mSocket);
                }
                mSocket = null;
                onExit();
                mLooper.quitSafely();
                mLooper = null;
            });
            mHandler = null;
        }
    }

    public final boolean isRunning() { return mRunning; }

    public final long numPacketsReceived() { return mPacketsReceived; }

    /**
     * Subclasses MUST create the listening socket here, including setting
     * all desired socket options, interface or address/port binding, etc.
     */
    protected abstract FileDescriptor createSocket();

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

    private final void setupListeningSocket() {
        final Looper looper = mHandler.getLooper();
        final MessageQueue queue = looper.getQueue();
        final int eventsOfInterest = EVENT_INPUT | EVENT_ERROR;
        queue.addOnFileDescriptorEventListener(
                mSocket,
                eventsOfInterest,
                new OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        final boolean hasInput = ((events & EVENT_INPUT) != 0);
                        final boolean hasError = ((events & EVENT_ERROR) != 0);
                        if (hasError || (hasInput && !handleInput())) {
                            return 0;  // unregisters this listener
                        }
                        return eventsOfInterest;
                    }
                });
    }

    // Keep trying to read until we get EAGAIN/EWOULDBLOCK.
    private boolean handleInput() {
        while (isRunning()) {
            final int bytesRead;

            try {
                // Nonblocking read.
                IoUtils.setBlocking(mSocket, false);
                // TODO: See if this can be converted to recvfrom.
                bytesRead = Os.read(mSocket, mPacket, 0, mPacket.length);
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
            } catch (IOException ioe) {
                if (isRunning()) logError("read error: ", ioe);
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
