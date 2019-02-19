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

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.net.NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK;
import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_HIGH;
import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_NORMAL;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.dhcp.DhcpServingParamsParcel;
import android.net.dhcp.IDhcpServerCallbacks;
import android.net.ip.IIpClientCallbacks;
import android.net.util.SharedLog;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Service used to communicate with the network stack, which is running in a separate module.
 * @hide
 */
public class NetworkStackClient {
    private static final String TAG = NetworkStackClient.class.getSimpleName();

    private static final int GET_REMOTE_CONNECTOR_TIMEOUT_MS = 10_000;

    // Starting the stack can take 2~3 seconds. 10 times that sounds like a reasonable delay.
    @VisibleForTesting
    static final long START_RETRY_DELAY_MS = 20_000;
    @VisibleForTesting
    static final int MAX_START_RETRIES = 5;

    private static final int MSG_RETRY_START = 1;

    private static NetworkStackClient sInstance;

    @NonNull
    @GuardedBy("mPendingNetStackRequests")
    private final ArrayList<NetworkStackCallback> mPendingNetStackRequests = new ArrayList<>();
    @Nullable
    @GuardedBy("mPendingNetStackRequests")
    private INetworkStackConnector mConnector;

    @GuardedBy("mLog")
    private final SharedLog mLog;
    private final Dependencies mDependencies;

    private final Object mStartRetryLock = new Object();
    // Only set when being started by the system server
    @GuardedBy("mStartRetryLock")
    @Nullable
    private HandlerThread mStartRetryThread;
    @GuardedBy("mStartRetryLock")
    @Nullable
    private StartRetryHandler mStartRetryHandler;
    // Accessed only on the retry handler and in dump()
    private volatile int mStartRetryCount = 0;

    private volatile boolean mNetworkStackStartRequested = false;

    private interface NetworkStackCallback {
        void onNetworkStackConnected(INetworkStackConnector connector);
    }

    /**
     * Dependencies of NetworkStackClient, to be overridden for testing.
     */
    @VisibleForTesting
    public static class Dependencies {
        /**
         * Register the specified service as the network stack service in {@link ServiceManager).
         */
        public void addService(IBinder service) {
            ServiceManager.addService(Context.NETWORK_STACK_SERVICE, service,
                    false /* allowIsolated */, DUMP_FLAG_PRIORITY_HIGH | DUMP_FLAG_PRIORITY_NORMAL);
        }
    }

    private NetworkStackClient() {
        this(new SharedLog(TAG), new Dependencies());
    }

    @VisibleForTesting
    NetworkStackClient(SharedLog log, Dependencies dependencies) {
        mLog = log;
        mDependencies = dependencies;
    }

    private class StartRetryHandler extends Handler {
        StartRetryHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MSG_RETRY_START) return;
            // This is fundamentally race-prone as the connector could arrive at any point after a
            // retry is scheduled. But this retry logic only intends to mitigate the impact of a
            // timeout starting the network stack, which would leave the system in a bad state if
            // the network stack never comes up and no retry is attempted.
            if (mStartRetryCount >= MAX_START_RETRIES) {
                throw new IllegalStateException("Could not start the network stack after "
                        + MAX_START_RETRIES + " retries");
            }
            logWtf("Timeout starting the network stack - retrying", null);
            mStartRetryCount++;
            startInternal((Context) msg.obj);
        }
    }

    /**
     * Get the NetworkStackClient singleton instance.
     */
    public static synchronized NetworkStackClient getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkStackClient();
        }
        return sInstance;
    }

    /**
     * Create a DHCP server according to the specified parameters.
     *
     * <p>The server will be returned asynchronously through the provided callbacks.
     */
    public void makeDhcpServer(final String ifName, final DhcpServingParamsParcel params,
            final IDhcpServerCallbacks cb) {
        requestConnector(connector -> {
            try {
                connector.makeDhcpServer(ifName, params, cb);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        });
    }

    /**
     * Create an IpClient on the specified interface.
     *
     * <p>The IpClient will be returned asynchronously through the provided callbacks.
     */
    public void makeIpClient(String ifName, IIpClientCallbacks cb) {
        requestConnector(connector -> {
            try {
                connector.makeIpClient(ifName, cb);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        });
    }

    /**
     * Create a NetworkMonitor.
     *
     * <p>The INetworkMonitor will be returned asynchronously through the provided callbacks.
     */
    public void makeNetworkMonitor(
            NetworkParcelable network, String name, INetworkMonitorCallbacks cb) {
        requestConnector(connector -> {
            try {
                connector.makeNetworkMonitor(network, name, cb);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        });
    }

    private class NetworkStackConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logi("Network stack service connected");
            registerNetworkStackService(service);

            synchronized (mStartRetryLock) {
                if (mStartRetryHandler != null && mStartRetryThread != null) {
                    mStartRetryHandler.removeMessages(MSG_RETRY_START);
                    mStartRetryThread.quitSafely();
                    mStartRetryHandler = null;
                    mStartRetryThread = null;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logWtf("Lost NetworkStack", null);
            // The system has lost its network stack (probably due to a crash in the
            // network stack process): better crash rather than stay in a bad state where all
            // networking is broken.
            // onServiceDisconnected is not being called on device shutdown, so this method being
            // called always indicates a bad state for the system server.
            throw new IllegalStateException("Lost network stack: crashing");
        }
    };

    private void registerNetworkStackService(@NonNull IBinder service) {
        final INetworkStackConnector connector = INetworkStackConnector.Stub.asInterface(service);

        mDependencies.addService(service);

        final ArrayList<NetworkStackCallback> requests;
        synchronized (mPendingNetStackRequests) {
            requests = new ArrayList<>(mPendingNetStackRequests);
            mPendingNetStackRequests.clear();
            mConnector = connector;
        }

        for (NetworkStackCallback r : requests) {
            r.onNetworkStackConnected(connector);
        }
    }

    /**
     * Start the network stack. Should be called only once on device startup.
     *
     * <p>This method will start the network stack either in the network stack process, or inside
     * the system server on devices that do not support the network stack module. The network stack
     * connector will then be delivered asynchronously to clients that requested it before it was
     * started.
     */
    public void start(@NonNull Context context) {
        final HandlerThread startThread = new HandlerThread(TAG);
        start(context, startThread);
    }

    @VisibleForTesting
    void start(@NonNull Context context, @NonNull HandlerThread handlerThread) {
        synchronized (mStartRetryLock) {
            mStartRetryThread = handlerThread;
            mStartRetryThread.start();
            mStartRetryHandler = new StartRetryHandler(mStartRetryThread.getLooper());
        }
        startInternal(context);
    }

    private void startInternal(@NonNull Context context) {
        log("Starting network stack");
        mNetworkStackStartRequested = true;
        // Try to bind in-process if the library is available
        IBinder connector = null;
        try {
            final Class service = Class.forName(
                    "com.android.server.NetworkStackService",
                    true /* initialize */,
                    context.getClassLoader());
            connector = (IBinder) service.getMethod("makeConnector", Context.class)
                    .invoke(null, context);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not create network stack connector", e);
        } catch (ClassNotFoundException e) {
            // Normal behavior if stack is provided by the app: fall through
        }

        // In-process network stack. Add the service to the service manager here.
        if (connector != null) {
            log("Registering in-process network stack connector");
            registerNetworkStackService(connector);
            return;
        }
        // Start the network stack process. The service will be added to the service manager in
        // NetworkStackConnection.onServiceConnected().
        log("Starting network stack process");
        final Intent intent = new Intent(INetworkStackConnector.class.getName());
        final ComponentName comp = intent.resolveSystemService(context.getPackageManager(), 0);
        intent.setComponent(comp);

        if (comp == null) {
            throw new IllegalStateException("Could not resolve the network stack with " + intent);
        }
        final PackageManager pm = context.getPackageManager();
        int uid = -1;
        try {
            uid = pm.getPackageUidAsUser(comp.getPackageName(), UserHandle.USER_SYSTEM);
        } catch (PackageManager.NameNotFoundException e) {
            logWtf("Network stack package not found", e);
            // Fall through
        }
        if (uid != Process.NETWORK_STACK_UID) {
            throw new SecurityException("Invalid network stack UID: " + uid);
        }

        final int hasPermission =
                pm.checkPermission(PERMISSION_MAINLINE_NETWORK_STACK, comp.getPackageName());
        if (hasPermission != PERMISSION_GRANTED) {
            throw new SecurityException(
                    "Network stack does not have permission " + PERMISSION_MAINLINE_NETWORK_STACK);
        }

        if (!context.bindServiceAsUser(intent, new NetworkStackConnection(),
                Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT, UserHandle.SYSTEM)) {
            throw new IllegalStateException(
                    "Could not bind to network stack in-process, or in app with " + intent);
            // TODO: crash/reboot system server if no network stack after a timeout ?
        }

        synchronized (mStartRetryLock) {
            if (mStartRetryHandler != null) {
                mStartRetryHandler.sendMessageDelayed(
                        mStartRetryHandler.obtainMessage(MSG_RETRY_START, context),
                        START_RETRY_DELAY_MS);
            }
        }

        log("Network stack service start requested");
    }

    /**
     * Log a message in the local log.
     */
    private void log(@NonNull String message) {
        synchronized (mLog) {
            mLog.log(message);
        }
    }

    private void logWtf(@NonNull String message, @Nullable Throwable e) {
        Slog.wtf(TAG, message);
        synchronized (mLog) {
            mLog.e(message, e);
        }
    }

    private void loge(@NonNull String message, @Nullable Throwable e) {
        synchronized (mLog) {
            mLog.e(message, e);
        }
    }

    /**
     * Log a message in the local and system logs.
     */
    private void logi(@NonNull String message) {
        synchronized (mLog) {
            mLog.i(message);
        }
    }

    /**
     * For non-system server clients, get the connector registered by the system server.
     */
    private INetworkStackConnector getRemoteConnector() {
        // Block until the NetworkStack connector is registered in ServiceManager.
        // <p>This is only useful for non-system processes that do not have a way to be notified of
        // registration completion. Adding a callback system would be too heavy weight considering
        // that the connector is registered on boot, so it is unlikely that a client would request
        // it before it is registered.
        // TODO: consider blocking boot on registration and simplify much of the logic in this class
        IBinder connector;
        try {
            final long before = System.currentTimeMillis();
            while ((connector = ServiceManager.getService(Context.NETWORK_STACK_SERVICE)) == null) {
                Thread.sleep(20);
                if (System.currentTimeMillis() - before > GET_REMOTE_CONNECTOR_TIMEOUT_MS) {
                    loge("Timeout waiting for NetworkStack connector", null);
                    return null;
                }
            }
        } catch (InterruptedException e) {
            loge("Error waiting for NetworkStack connector", e);
            return null;
        }

        return INetworkStackConnector.Stub.asInterface(connector);
    }

    private void requestConnector(@NonNull NetworkStackCallback request) {
        // TODO: PID check.
        final int caller = Binder.getCallingUid();
        if (caller != Process.SYSTEM_UID && !UserHandle.isSameApp(caller, Process.BLUETOOTH_UID)) {
            // Don't even attempt to obtain the connector and give a nice error message
            throw new SecurityException(
                    "Only the system server should try to bind to the network stack.");
        }

        if (!mNetworkStackStartRequested) {
            // The network stack is not being started in this process, e.g. this process is not
            // the system server. Get a remote connector registered by the system server.
            final INetworkStackConnector connector = getRemoteConnector();
            synchronized (mPendingNetStackRequests) {
                mConnector = connector;
            }
            request.onNetworkStackConnected(connector);
            return;
        }

        final INetworkStackConnector connector;
        synchronized (mPendingNetStackRequests) {
            connector = mConnector;
            if (connector == null) {
                mPendingNetStackRequests.add(request);
                return;
            }
        }

        request.onNetworkStackConnected(connector);
    }

    /**
     * Dump NetworkStackClient logs to the specified {@link PrintWriter}.
     */
    public void dump(PrintWriter pw) {
        // dump is thread-safe on SharedLog
        mLog.dump(null, pw, null);

        final int requestsQueueLength;
        synchronized (mPendingNetStackRequests) {
            requestsQueueLength = mPendingNetStackRequests.size();
        }

        pw.println();
        pw.println("pendingNetStackRequests length: " + requestsQueueLength);
        pw.println("start retry count: " + mStartRetryCount);
    }
}
