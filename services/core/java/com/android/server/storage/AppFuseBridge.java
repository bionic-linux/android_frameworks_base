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

package com.android.server.storage;

import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.FuseUnavailableMountException;
import com.android.internal.util.Preconditions;
import com.android.server.NativeDaemonConnectorException;
import libcore.io.IoUtils;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runnable that delegates FUSE command from the kernel to application.
 * run() blocks until all opened files on the FUSE mount point are closed. So this should be run in
 * a separated thread.
 */
public class AppFuseBridge implements Runnable {
    public static final String TAG = "AppFuseBridge";

    /**
     * The path AppFuse is mounted to.
     * The first number is UID who is mounting the FUSE.
     * THe second number is mount ID.
     * The path must be sync with vold.
     */
    private static final String APPFUSE_MOUNT_NAME_TEMPLATE = "/mnt/appfuse/%d_%d";

    @GuardedBy("mScopes")
    private final SparseArray<MountScope> mScopes = new SparseArray<>();
    private final AtomicInteger mMountIdBeingAdded = new AtomicInteger(-1);

    @GuardedBy("this")
    private long mNativeLoop;

    public AppFuseBridge() {
        mNativeLoop = native_new();
    }

    public ParcelFileDescriptor addBridge(MountScope mountScope)
            throws FuseUnavailableMountException, NativeDaemonConnectorException {
        try {
            // Make sure the native loop isn't destroyed while we are adding a bridge
            synchronized (this) {
                if (mNativeLoop == 0) {
                    throw new FuseUnavailableMountException(mountScope.mountId);
                }
                synchronized (mScopes) {
                    Preconditions.checkArgument(mScopes.indexOfKey(mountScope.mountId) < 0);
                }
                mMountIdBeingAdded.set(mountScope.mountId);
                final int fd = native_add_bridge(
                        mNativeLoop, mountScope.mountId, mountScope.open().detachFd());
                if (fd == -1) {
                    throw new FuseUnavailableMountException(mountScope.mountId);
                }
                final ParcelFileDescriptor result = ParcelFileDescriptor.adoptFd(fd);
                synchronized (mScopes) {
                    mScopes.put(mountScope.mountId, mountScope);
                }
                mountScope = null;
                mMountIdBeingAdded.set(-1);
                return result;
            }
        } finally {
            IoUtils.closeQuietly(mountScope);
        }
    }

    @Override
    public void run() {
        native_start_loop(mNativeLoop);
        synchronized (this) {
            native_delete(mNativeLoop);
            mNativeLoop = 0;
        }
    }

    public ParcelFileDescriptor openFile(int mountId, int fileId, int mode)
            throws FuseUnavailableMountException, InterruptedException {
        final MountScope scope;
        synchronized (mScopes) {
            scope = mScopes.get(mountId);
            if (scope == null) {
                throw new FuseUnavailableMountException(mountId);
            }
        }
        if (!scope.waitForMount()) {
            throw new FuseUnavailableMountException(mountId);
        }
        try {
            int flags = FileUtils.translateModePfdToPosix(mode);
            return scope.openFile(mountId, fileId, flags);
        } catch (NativeDaemonConnectorException error) {
            throw new FuseUnavailableMountException(mountId);
        }
    }

    // Called by com_android_server_storage_AppFuse.cpp while holding FuseBridgeLoop.mutex_
    private void onMount(int mountId) {
        synchronized (mScopes) {
            final MountScope scope = mScopes.get(mountId);
            if (scope != null) {
                scope.setMountResultLocked(true);
            }
        }
    }

    // Called by com_android_server_storage_AppFuse.cpp while holding FuseBridgeLoop.mutex_
    private void onClosed(int mountId) {
        // With the current synchronization between the java and native code,
        // here we need to wait for add operation to finish to avoid dead lock
        while(mMountIdBeingAdded.get() == mountId) {
            SystemClock.sleep(10);
        }
        synchronized (mScopes) {
            final MountScope scope = mScopes.get(mountId);
            if (scope != null) {
                scope.setMountResultLocked(false);
                IoUtils.closeQuietly(scope);
                mScopes.remove(mountId);
            }
        }
    }

    public static abstract class MountScope implements AutoCloseable {
        public final int uid;
        public final int mountId;
        private final CountDownLatch mMounted = new CountDownLatch(1);
        private boolean mMountResult = false;

        public MountScope(int uid, int mountId) {
            this.uid = uid;
            this.mountId = mountId;
        }

        @GuardedBy("AppFuseBridge.mScopes")
        void setMountResultLocked(boolean result) {
            if (mMounted.getCount() == 0) {
                return;
            }
            mMountResult = result;
            mMounted.countDown();
        }

        boolean waitForMount() throws InterruptedException {
            mMounted.await();
            return mMountResult;
        }

        public abstract ParcelFileDescriptor open() throws NativeDaemonConnectorException;
        public abstract ParcelFileDescriptor openFile(int mountId, int fileId, int flags)
                throws NativeDaemonConnectorException;
    }

    private native long native_new();
    private native void native_delete(long loop);
    private native void native_start_loop(long loop);
    private native int native_add_bridge(long loop, int mountId, int deviceId);
}
