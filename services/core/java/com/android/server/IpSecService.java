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
import android.net.IpSecManager;
import android.net.IpSecTransform;
import android.os.Binder;
import android.os.Bundle;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private abstract class ManagedResource implements IBinder.DeathRecipient {
        final int pid;
        final int uid;
        private IBinder mBinder;

        ManagedResource(IBinder binder) {
            super();
            mBinder = binder;
            pid = getCallingPid();
            uid = getCallingUid();

            try {
                mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        /**
         * When this record is no longer needed for managing system resources this function should
         * unlink all references held by the record to allow efficient garbage collection.
         */
        public final void release() {
            //Release all the underlying system resources first
            releaseResources();

            if (mBinder != null) {
                mBinder.unlinkToDeath(this, 0);
            }
            mBinder = null;

            //remove this record so that it can be cleaned up
            nullifyRecord();
        }

        /**
         * If the Binder object dies, this function is called to free the system resources that are
         * being managed by this record and to subsequently release this record for garbage
         * collection
         */
        public final void binderDied() {
            release();
        }

        /**
         * Implement this method to release all object references contained in the subclass to allow
         * efficient garbage collection of the record. This should remove any references to the
         * record from all other locations that hold a reference as the record is no longer valid.
         */
        protected abstract void nullifyRecord();

        /**
         * Implement this method to release all system resources that are being protected by this
         * record. Once the resources are released, the record should be invalidated and no longer
         * used by calling releaseRecord()
         */
        protected abstract void releaseResources();
    };

    private final class TransformRecord extends ManagedResource {
        private IpSecConfig mConfig;
        private int mResourceId;

        TransformRecord(IpSecConfig config, int resourceId, IBinder binder) {
            super(binder);
            mConfig = config;
            mResourceId = resourceId;
        }

        public IpSecConfig getConfig() {
            return mConfig;
        }

        @Override
        protected void releaseResources() {
            for (int direction :
                    new int[] {IpSecTransform.DIRECTION_OUT, IpSecTransform.DIRECTION_IN}) {
                try {
                    mNetdService.ipSecDeleteSecurityAssociation(
                            mResourceId,
                            direction,
                            (mConfig.getLocalAddress() != null)
                                    ? mConfig.getLocalAddress().getHostAddress()
                                    : "",
                            (mConfig.getRemoteAddress() != null)
                                    ? mConfig.getRemoteAddress().getHostAddress()
                                    : "",
                            mConfig.getSpi(direction));
                } catch (ServiceSpecificException e) {
                    // FIXME: get the error code and throw is at an IOException from Errno Exception
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }

        @Override
        protected void nullifyRecord() {
            mConfig = null;
            mResourceId = 0;
        }
    }

    private final class SpiRecord implements IBinder.DeathRecipient {
        private final int mDirection;
        private final String mLocalAddress;
        private final String mRemoteAddress;
        private final int mSpi;
        private final IBinder mBinder;
        private final int mResourceId;

        SpiRecord(
                int resourceId,
                int direction,
                String localAddress,
                String remoteAddress,
                int spi,
                IBinder binder) {
            mResourceId = resourceId;
            mDirection = direction;
            mLocalAddress = localAddress;
            mRemoteAddress = remoteAddress;
            mSpi = spi;
            mBinder = binder;
        }

        void unlinkDeathRecipient() {
            if (mBinder != null) {
                mBinder.unlinkToDeath(this, 0);
            }
        }

        protected void releaseResources() {}

        protected void nullifyRecord() {}

        public void binderDied() {
            Log.w(TAG, "IpSecService.SpiRecord binderDied(" + mBinder + ")");
        }
    }

    private final HashMap<Integer, SpiRecord> mSpiRecords = new HashMap<>();
    private final HashMap<Integer, TransformRecord> mTransformRecords = new HashMap<>();

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

    /*
     * Local address must be INADDR_ANY or IN6ADDR_ANY,
     * thus non-zero addr -> Address is Outbound
     */
    // FIXME : This is fundamentally broken and needs to be removed
    int divineDirection(String destinationAddress) {
        try {
            InetAddress testAddr = InetAddress.getByName(destinationAddress);
            byte[] addrBytes = testAddr.getAddress();
            String localAddr = "", remoteAddr = destinationAddress;
            for (byte b : addrBytes) {
                if (b != 0) {
                    return IpSecTransform.DIRECTION_OUT;
                }
            }
        } catch (UnknownHostException e) {};
        return IpSecTransform.DIRECTION_IN;
    }

    @Override
    /** Get a new SPI and maintain the reservation in the system server */
    public Bundle reserveSecurityParameterIndex(
            String destinationAddress, int requestedSpi, IBinder binder) {
        int resourceId = mNextTransformId.getAndIncrement();
        int direction = divineDirection(destinationAddress);

        int spi = IpSecManager.INVALID_SECURITY_PARAMETER_INDEX;
        String remoteAddress, localAddress;
        if (direction == IpSecTransform.DIRECTION_OUT) {
            remoteAddress = destinationAddress;
            localAddress = "";
        } else {
            remoteAddress = "";
            localAddress = "";
        }
        try {
            spi =
                    mNetdService.ipSecAllocateSpi(
                            resourceId, direction, localAddress, remoteAddress, requestedSpi);
        } catch (ServiceSpecificException e) {
            // FIXME: get the error code and throw is at an IOException from Errno Exception
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        synchronized (mSpiRecords) {
            mSpiRecords.put(
                    resourceId,
                    new SpiRecord(resourceId, direction, localAddress, remoteAddress, spi, binder));
        }

        Bundle retBundle = new Bundle(3);
        retBundle.putInt(IpSecManager.SecurityParameterIndex.KEY_STATUS, IpSecManager.Status.OK);
        retBundle.putInt(IpSecManager.SecurityParameterIndex.KEY_RESOURCE_ID, resourceId);
        retBundle.putInt(IpSecManager.SecurityParameterIndex.KEY_SPI, spi);

        return null;
    }

    /** Release a previously allocated SPI that has been registered with the system server */
    @Override
    public void releaseSecurityParameterIndex(int resourceId) {}

    /**
     * Open a socket via the system server and bind it to the specified port (random if port=0).
     * This will return a PFD to the user that represent a bound UDP socket. The system server will
     * cache the socket and a record of its owner so that it can and must be freed when no longer
     * needed.
     */
    @Override
    public Bundle openUdpEncapsulationSocket(int port, IBinder binder) {
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
    public Bundle createTransportModeTransform(IpSecConfig c, IBinder binder) {
        int resourceId = mNextTransformId.getAndIncrement();
        for (int direction :
                new int[] {IpSecTransform.DIRECTION_OUT, IpSecTransform.DIRECTION_IN}) {
            IpSecAlgorithm auth = c.getAuthentication(direction);
            IpSecAlgorithm crypt = c.getEncryption(direction);
            try {
                int result =
                        mNetdService.ipSecAddSecurityAssociation(
                                resourceId,
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
                    Bundle retBundle = new Bundle(2);
                    retBundle.putInt(
                            IpSecTransform.KEY_STATUS, IpSecManager.Status.SPI_UNAVAILABLE);
                    retBundle.putInt(IpSecTransform.KEY_RESOURCE_ID, IpSecTransform.INVALID_SPI);
                    return retBundle;
                }
            } catch (ServiceSpecificException e) {
                // FIXME: get the error code and throw is at an IOException from Errno Exception
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        synchronized (mTransformRecords) {
            mTransformRecords.put(resourceId, new TransformRecord(c, resourceId, binder));
        }

        Bundle retBundle = new Bundle(2);
        retBundle.putInt(IpSecTransform.KEY_STATUS, IpSecManager.Status.OK);
        retBundle.putInt(IpSecTransform.KEY_RESOURCE_ID, resourceId);
        return retBundle;
    }

    /**
     * Delete a transport mode transform that was previously allocated by + registered with the
     * system server. If this is called on an inactive (or non-existent) transform, it will not
     * return an error. It's safe to de-allocate transforms that may have already been deleted for
     * other reasons.
     */
    @Override
    public void deleteTransportModeTransform(int resourceId) {
        TransformRecord record;
        synchronized (mTransformRecords) {
            // We want to non-destructively get so that we can check credentials before removing this
            record = mTransformRecords.get(resourceId);

            if (record == null) {
                throw new IllegalArgumentException(
                        "Transform " + resourceId + " is not available to be deleted");
            }

            if (record.pid != getCallingPid() || record.uid != getCallingUid()) {
                throw new SecurityException("Only the owner of an IpSec Transform may delete it!");
            }

            // remove from the DB because releasing might fail, but it won't ever succeed later
            mTransformRecords.remove(resourceId);
            record.releaseResources();
            record.nullifyRecord();
        }
    }

    /**
     * Apply an active transport mode transform to a socket, which will apply the IPsec security
     * association as a correspondent policy to the provided socket
     */
    @Override
    public void applyTransportModeTransform(ParcelFileDescriptor socket, int resourceId) {
        TransformRecord info;

        synchronized (mTransformRecords) {
            // FIXME: this code should be factored out into a security check + getter
            info = mTransformRecords.get(resourceId);

            if (info == null) {
                throw new IllegalArgumentException("Transform " + resourceId + " is not active");
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
                            resourceId,
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
     * applying a policy that performs no IPsec). Today the resourceId parameter is passed but not
     * used: reserved for future improved input validation.
     */
    @Override
    public void removeTransportModeTransform(ParcelFileDescriptor socket, int resourceId) {}

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        mContext.enforceCallingOrSelfPermission(DUMP, TAG);

        pw.println("IpSecService Log:");
        pw.println("NetdNativeService Connection: " + (isNetdAlive() ? "alive" : "dead"));
        pw.println();
    }
}
