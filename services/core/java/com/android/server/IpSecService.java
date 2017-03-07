/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.server;

import static android.Manifest.permission.DUMP;

import android.content.Context;
import android.net.IIpSecService;
import android.net.INetd;
import android.net.IpSecAlgorithm;
import android.net.IpSecConfig;
import android.net.IpSecTransform;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** @hide */
public class IpSecService extends IIpSecService.Stub implements Watchdog.Monitor {
    private static final String TAG = "IpSecService";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String NETD_TAG = "NetdConnector";
    private static final String NETD_SERVICE_NAME = "netd";

    /** Binder context for this service */
    private final Context mContext;

    /** connector object for communicating with netd */
    private final NativeDaemonConnector mConnector;

    private final Handler mFgHandler;

    private INetd mNetdService;

    private final Thread mThread;
    private CountDownLatch mConnectedSignal = new CountDownLatch(1);

    private AtomicInteger mNextTransformId = new AtomicInteger(0xFADED000);

    private final class TransformInfo implements IBinder.DeathRecipient {
        final int pid;
        final int uid;

        private IpSecConfig mConfig;
        private final IBinder mBinder;
        private final int mTransformId;

        TransformInfo(IpSecConfig config, IBinder binder, int transformId) {
            super();
            mConfig = config;
            mBinder = binder;
            mTransformId = transformId;
            pid = getCallingPid();
            uid = getCallingUid();

            try {
                mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        void unlinkDeathRecipient() {
            if (mBinder != null) {
                mBinder.unlinkToDeath(this, 0);
            }
        }

        public IpSecConfig getConfig() {
            return mConfig;
        }

        public int getTransformId() {
            return mTransformId;
        }

        public void binderDied() {
            Log.w(TAG, "NetworkManagementService.IpecTransform binderDied(" + mBinder + ")");
            mTransformInfo.remove(mTransformId);
            deleteTransformInternal(mConfig, mTransformId);
        }
    };

    private final HashMap<Integer, TransformInfo> mTransformInfo = new HashMap<>();

    /**
     * Constructs a new IpSecService instance
     *
     * @param context Binder context for this service
     */
    private IpSecService(Context context, String socket) {
        mContext = context;

        // make sure this is on the same looper as our NativeDaemonConnector for sync purposes
        mFgHandler = new Handler(FgThread.get().getLooper());

        mConnector =
                new NativeDaemonConnector(
                        new NetdCallbackReceiver(),
                        socket,
                        10,
                        NETD_TAG,
                        160,
                        null /*wakelock*/,
                        FgThread.get().getLooper());
        mThread = new Thread(mConnector, NETD_TAG);

        Watchdog.getInstance().addMonitor(this);
    }

    static IpSecService create(Context context, String socket) throws InterruptedException {
        final IpSecService service = new IpSecService(context, socket);
        final CountDownLatch connectedSignal = service.mConnectedSignal;
        if (DBG) Log.d(TAG, "Creating IpSecService");
        service.mThread.start();
        if (DBG) Log.d(TAG, "Awaiting socket connection");
        connectedSignal.await();
        if (DBG) Log.d(TAG, "Connected");
        service.connectNativeNetdService();
        return service;
    }

    public static IpSecService create(Context context) throws InterruptedException {
        return create(context, NETD_SERVICE_NAME);
    }

    public void systemReady() {
        if (DBG) {
            final long start = System.currentTimeMillis();
            prepareNativeDaemon();
            final long delta = System.currentTimeMillis() - start;
            Log.d(TAG, "Prepared in " + delta + "ms");
            return;
        } else {
            prepareNativeDaemon();
        }
    }

    private void connectNativeNetdService() {
        mNetdService = INetd.Stub.asInterface(ServiceManager.getService(NETD_SERVICE_NAME));

        if (!isNetdAlive()) {
            Log.wtf(TAG, "Can't connect to NativeNetdService " + NETD_SERVICE_NAME);
        }
    }

    /**
     * Prepare native daemon once connected, enabling modules and pushing any existing in-memory
     * rules.
     */
    private void prepareNativeDaemon() {}

    //
    // Netd Callback handling
    //

    private class NetdCallbackReceiver implements INativeDaemonConnectorCallbacks {
        @Override
        public void onDaemonConnected() {
            Log.i(TAG, "onDaemonConnected()");
            // event is dispatched from internal NDC thread, so we prepare the
            // daemon back on main thread.
            if (mConnectedSignal != null) {
                // The system is booting and we're connecting to netd for the first time.
                mConnectedSignal.countDown();
                mConnectedSignal = null;
            } else {
                // We're reconnecting to netd after the socket connection
                // was interrupted (e.g., if it crashed).
                mFgHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                connectNativeNetdService();
                                prepareNativeDaemon();
                            }
                        });
            }
        }

        @Override
        public boolean onCheckHoldWakeLock(int code) {
            return false;
        }

        @Override
        public boolean onEvent(int code, String raw, String[] cooked) {
            String errorMessage = String.format("Invalid event from daemon (%s)", raw);
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void monitor() {
        if (mConnector != null) {
            mConnector.monitor();
        }
    }

    private static void enforceSystemUid() {
        final int uid = Binder.getCallingUid();
        if (uid != Process.SYSTEM_UID) {
            throw new SecurityException("Only available to AID_SYSTEM");
        }
    }

    boolean isNetdAlive() {
        if (mNetdService == null) {
            return false;
        }

        try {
            return mNetdService.isAlive();
        } catch (RemoteException re) {
            return false;
        }
    }

    @Override
    /** Get a new SPI and maintain the reservation in the system server */
    public int reserveSecurityParameterIndex(
            String destinationAddress, int requestedSpi, IBinder binder) {
        return 0xDEADBEEF;
    }

    /** Release a previously allocated SPI that has been registered with the system server */
    @Override
    public void releaseSecurityParameterIndex(int spi) {}

    /**
     * Open a socket via the system server and bind it to the specified port (random if port=0).
     * This will return a PFD to the user that represent a bound UDP socket. The system server will
     * cache the socket and a record of its owner so that it can and must be freed when no longer
     * needed.
     */
    @Override
    public ParcelFileDescriptor openUdpEncapsulationSocket(int port, IBinder binder) {
        return null;
    }

    /** close a socket that has been been allocated by and registered with the system server */
    @Override
    public void closeUdpEncapsulationSocket(ParcelFileDescriptor socket) {}

    /**
     * Create a transport mode transform, which represent two security associations (one in each
     * direction) in the kernel. The transform will be cached by the system server and must be freed
     * when no longer needed. It is possible to free one, deleting the SA from underneath sockets
     * that are using it, which will result in all of those sockets becoming unable to send or
     * receive data.
     */
    @Override
    public int createTransportModeTransform(IpSecConfig c, IBinder binder) {
        int transformId = mNextTransformId.getAndIncrement();
        for (int direction :
                new int[] {IpSecTransform.DIRECTION_OUT, IpSecTransform.DIRECTION_IN}) {
            IpSecAlgorithm auth = c.getAuthentication(direction);
            IpSecAlgorithm crypt = c.getEncryption(direction);
            try {
                int result =
                        mNetdService.ipSecAddSecurityAssociation(
                                transformId,
                                c.getMode(),
                                direction,
                                (c.getLocalAddress() != null)
                                        ? c.getLocalAddress().getHostAddress()
                                        : "",
                                (c.getRemoteAddress() != null)
                                        ? c.getRemoteAddress().getHostAddress()
                                        : "",
                                (c.getNetwork() != null) ? c.getNetwork().getNetworkHandle() : 0,
                                c.getSpi(direction),
                                (auth != null) ? auth.getName() : "",
                                (auth != null) ? auth.getKey() : null,
                                (auth != null) ? auth.getTruncationLengthBits() : 0,
                                (crypt != null) ? crypt.getName() : "",
                                (crypt != null) ? crypt.getKey() : null,
                                (crypt != null) ? crypt.getTruncationLengthBits() : 0,
                                c.getEncapType(),
                                c.getEncapLocalPort(),
                                c.getEncapRemotePort());
                if (result != c.getSpi(direction)) {
                    return IpSecTransform.INVALID_TRANSFORM_ID;
                }
            } catch (ServiceSpecificException e) {
                // FIXME: get the error code and throw is at an IOException from Errno Exception
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        synchronized (mTransformInfo) {
            mTransformInfo.put(transformId, new TransformInfo(c, binder, transformId));
        }

        return transformId;
    }

    private void deleteTransformInternal(IpSecConfig c, int transformId) {
        for (int direction :
                new int[] {IpSecTransform.DIRECTION_OUT, IpSecTransform.DIRECTION_IN}) {
            try {
                mNetdService.ipSecDeleteSecurityAssociation(
                        transformId,
                        direction,
                        (c.getLocalAddress() != null) ? c.getLocalAddress().getHostAddress() : "",
                        (c.getRemoteAddress() != null) ? c.getRemoteAddress().getHostAddress() : "",
                        c.getSpi(direction));
            } catch (ServiceSpecificException e) {
                // FIXME: get the error code and throw is at an IOException from Errno Exception
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Delete a transport mode transform that was previously allocated by + registered with the
     * system server. If this is called on an inactive (or non-existent) transform, it will not
     * return an error. It's safe to de-allocate transforms that may have already been deleted for
     * other reasons.
     */
    @Override
    public void deleteTransportModeTransform(int transformId) {
        TransformInfo info;
        synchronized (mTransformInfo) {
            // We want to non-destructively get so that we can check credentials before removing this
            info = mTransformInfo.get(transformId);

            if (info == null) {
                throw new IllegalArgumentException(
                        "Transform " + transformId + " is not available to be deleted");
            }

            if (info.pid != getCallingPid() || info.uid != getCallingUid()) {
                throw new SecurityException("Only the owner of an IpSec Transform may delete it!");
            }

            deleteTransformInternal(info.getConfig(), info.getTransformId());
            // don't remove it from the data structure until we've successfully deleted it
            mTransformInfo.remove(transformId);
        }
    }

    /**
     * Apply an active transport mode transform to a socket, which will apply the IPsec security
     * association as a correspondent policy to the provided socket
     */
    @Override
    public void applyTransportModeTransform(ParcelFileDescriptor socket, int transformId) {
        TransformInfo info;

        synchronized (mTransformInfo) {
            // FIXME: this code should be factored out into a security check + getter
            info = mTransformInfo.get(transformId);

            if (info == null) {
                throw new IllegalArgumentException("Transform " + transformId + " is not active");
            }

            if (info.pid != getCallingPid() || info.uid != getCallingUid()) {
                throw new SecurityException("Only the owner of an IpSec Transform may apply it!");
            }

            IpSecConfig c = info.getConfig();
            try {
                for (int direction :
                        new int[] {IpSecTransform.DIRECTION_OUT, IpSecTransform.DIRECTION_IN}) {
                    mNetdService.ipSecApplyTransportModeTransform(
                            socket.getFileDescriptor(),
                            info.getTransformId(),
                            direction,
                            (c.getLocalAddress() != null)
                                    ? c.getLocalAddress().getHostAddress()
                                    : "",
                            (c.getRemoteAddress() != null)
                                    ? c.getRemoteAddress().getHostAddress()
                                    : "",
                            c.getSpi(direction));
                }
            } catch (ServiceSpecificException e) {
                // FIXME: get the error code and throw is at an IOException from Errno Exception
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }
    /**
     * Remove a transport mode transform from a socket, applying the default (empty) policy. This
     * will ensure that NO IPsec policy is applied to the socket (would be the equivalent of
     * applying a policy that performs no IPsec). Today the transformId parameter is passed but not
     * used: reserved for future improved input validation.
     */
    @Override
    public void removeTransportModeTransform(ParcelFileDescriptor socket, int transformId) {}

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        mContext.enforceCallingOrSelfPermission(DUMP, TAG);

        pw.println("IpSecService Log:");
        pw.println("NetdNativeService Connection: " + (isNetdAlive() ? "alive" : "dead"));
        pw.println();
    }
}
