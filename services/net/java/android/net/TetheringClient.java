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
import static android.net.TetheringManager.EXTRA_NETWORKSTACK;
import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_HIGH;
import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_NORMAL;

import android.annotation.NonNull;
import android.content.Context;
import android.net.util.SharedLog;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;

import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;

/**
 * Service used to communicate with the tethering service, which is running in a separate module.
 * @hide
 */
public class TetheringClient extends ConnectivityModuleClientBase {
    private static final String TAG = TetheringClient.class.getSimpleName();

    private static TetheringClient sInstance;

    private interface TetheringCallback {
        void onTetheringConnected(ITetheringConnector connector);
    }

    @VisibleForTesting
    protected TetheringClient(@NonNull Dependencies dependencies) {
        super(dependencies, new SharedLog(TAG));
    }

    private TetheringClient() {
        this(new DependenciesImpl());
    }

    private static class DependenciesImpl implements Dependencies {
        @Override
        public void addToServiceManager(@NonNull IBinder service) {
            ServiceManager.addService(Context.TETHERING_SERVICE, service,
                    false /* allowIsolated */, DUMP_FLAG_PRIORITY_HIGH | DUMP_FLAG_PRIORITY_NORMAL);
        }

        @Override
        public void checkCallerUid() { }

        @Override
        public ConnectivityModuleConnector getConnectivityModuleConnector() {
            return ConnectivityModuleConnector.getInstance();
        }
    }

    /**
     * Get the TetheringClient singleton instance.
     */
    public static synchronized TetheringClient getInstance() {
        if (sInstance == null) {
            sInstance = new TetheringClient();
        }
        return sInstance;
    }

    /**
     * Start the tethering service. Should be called only once on device startup.
     *
     * <p>This method will start the tethering service either in the network stack process,
     * or inside the system server on devices that do not support the tethering module.
     */
    public void start(IBinder service) {
        final Bundle bundle = new Bundle();
        bundle.putBinder(EXTRA_NETWORKSTACK, service);
        mDependencies.getConnectivityModuleConnector().startModuleService(
                ITetheringConnector.class.getName(), PERMISSION_MAINLINE_NETWORK_STACK, bundle,
                new ModuleConnection());
        log("Tethering service start requested");
    }

    /**
     * Dump TetheringClient logs to the specified {@link PrintWriter}.
     */
    public void dump(PrintWriter pw) {
        // dump is thread-safe on SharedLog
        mLog.dump(null, pw, null);
    }
}
