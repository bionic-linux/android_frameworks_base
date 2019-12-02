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

import static android.net.NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK;
import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_HIGH;
import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_NORMAL;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.dhcp.DhcpServingParamsParcel;
import android.net.dhcp.IDhcpServerCallbacks;
import android.net.ip.IIpClientCallbacks;
import android.net.util.SharedLog;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Service used to communicate with the network stack, which is running in a separate module.
 * @hide
 */
public class NetworkStackClient extends ConnectivityModuleClientBase {
    private static final String TAG = NetworkStackClient.class.getSimpleName();

    private static NetworkStackClient sInstance;

    @NonNull
    @GuardedBy("mPendingNetStackRequests")
    private final ArrayList<NetworkStackCallback> mPendingNetStackRequests = new ArrayList<>();
    @Nullable
    @GuardedBy("mPendingNetStackRequests")
    private INetworkStackConnector mConnector;

    private volatile boolean mWasSystemServerInitialized = false;

    private interface NetworkStackCallback {
        void onNetworkStackConnected(INetworkStackConnector connector);
    }

    @VisibleForTesting
    protected NetworkStackClient(@NonNull Dependencies dependencies) {
        super(dependencies, new SharedLog(TAG));
    }

    private NetworkStackClient() {
        this(new DependenciesImpl());
    }

    private static class DependenciesImpl implements Dependencies {
        @Override
        public void addToServiceManager(@NonNull IBinder service) {
            ServiceManager.addService(Context.NETWORK_STACK_SERVICE, service,
                    false /* allowIsolated */, DUMP_FLAG_PRIORITY_HIGH | DUMP_FLAG_PRIORITY_NORMAL);
        }

        @Override
        public void checkCallerUid() {
            final int caller = Binder.getCallingUid();
            // This is a client lib so "caller" is the current UID in most cases. The check is done
            // here in the caller's process just to provide a nicer error message to clients; more
            // generic checks are also done in NetworkStackService.
            // See PermissionUtil in NetworkStack for the actual check on the service side - the
            // checks here should be kept in sync with PermissionUtil.
            if (caller != Process.SYSTEM_UID
                    && caller != Process.NETWORK_STACK_UID
                    && UserHandle.getAppId(caller) != Process.BLUETOOTH_UID) {
                throw new SecurityException(
                        "Only the system server should try to bind to the network stack.");
            }
        }

        @Override
        public ConnectivityModuleConnector getConnectivityModuleConnector() {
            return ConnectivityModuleConnector.getInstance();
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
    public void makeNetworkMonitor(Network network, String name, INetworkMonitorCallbacks cb) {
        requestConnector(connector -> {
            try {
                connector.makeNetworkMonitor(network, name, cb);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        });
    }

    /**
     * Get an instance of the IpMemoryStore.
     *
     * <p>The IpMemoryStore will be returned asynchronously through the provided callbacks.
     */
    public void fetchIpMemoryStore(IIpMemoryStoreCallbacks cb) {
        requestConnector(connector -> {
            try {
                connector.fetchIpMemoryStore(cb);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        });
    }

    @Override
    protected void registerModuleService(@NonNull IBinder service) {
        final INetworkStackConnector connector = INetworkStackConnector.Stub.asInterface(service);
        log("Network stack service registered");

        try {
            TetheringClient.getInstance().start(service);
        } catch (Throwable e) {
            logWtf(TAG, "BOOT FAILTURE start Tethering ", e);
        }

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
     * Initialize the network stack. Should be called only once on device startup, before any
     * client attempts to use the network stack.
     */
    public void init() {
        log("Network stack init");
        mWasSystemServerInitialized = true;
    }

    /**
     * Start the network stack. Should be called only once on device startup.
     *
     * <p>This method will start the network stack either in the network stack process, or inside
     * the system server on devices that do not support the network stack module. The network stack
     * connector will then be delivered asynchronously to clients that requested it before it was
     * started.
     */
    public void start() {
        mDependencies.getConnectivityModuleConnector().startModuleService(
                INetworkStackConnector.class.getName(), PERMISSION_MAINLINE_NETWORK_STACK,
                new ModuleConnection());
        log("Network stack service start requested");
    }

    private void requestConnector(@NonNull NetworkStackCallback request) {
        mDependencies.checkCallerUid();

        if (!mWasSystemServerInitialized) {
            // The network stack is not being started in this process, e.g. this process is not
            // the system server. Get a remote connector registered by the system server.
            final IBinder serviceBinder = getRemoteConnector(Context.NETWORK_STACK_SERVICE);
            synchronized (mPendingNetStackRequests) {
                if (serviceBinder == null) {
                    // Add the request to pending list, in case client request before
                    // NetworkStackService is registered and getRemoteConnector is timeout.
                    mPendingNetStackRequests.add(request);
                    return;
                }
                mConnector = INetworkStackConnector.Stub.asInterface(serviceBinder);
            }
            request.onNetworkStackConnected(mConnector);
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
     * TetheringClient logs would also be dumped at the same time.
     */
    public void dump(PrintWriter pw) {
        // dump is thread-safe on SharedLog
        mLog.dump(null, pw, null);
        // dump connectivity module connector logs.
        ConnectivityModuleConnector.getInstance().dump(pw);

        final int requestsQueueLength;
        synchronized (mPendingNetStackRequests) {
            requestsQueueLength = mPendingNetStackRequests.size();
        }

        pw.println();
        pw.println("pendingNetStackRequests length: " + requestsQueueLength);

        pw.println();
        TetheringClient.getInstance().dump(pw);
    }
}
