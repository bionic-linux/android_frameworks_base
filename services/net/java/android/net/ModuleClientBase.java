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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.util.SharedLog;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Service used to communicate with the connectivity module, which is running in separate service.
 * @hide
 */
public class ModuleClientBase {
    private static final int MODULE_TIMEOUT_MS = 10_000;

    @NonNull
    protected final Dependencies mDependencies;

    @GuardedBy("mLog")
    protected final SharedLog mLog;

    @VisibleForTesting
    protected ModuleClientBase(@NonNull Dependencies dependencies, SharedLog log) {
        mDependencies = dependencies;
        mLog = log;
    }

    @VisibleForTesting
    protected interface Dependencies {
        void addToServiceManager(@NonNull IBinder service);
        void checkCallerUid();
        ConnectivityModuleConnector getConnectivityModuleConnector();
    }

    protected class ModuleConnection implements
            ConnectivityModuleConnector.ModuleServiceCallback {
        @Override
        public void onModuleServiceConnected(IBinder service) {
            logi("Module service connected");
            mDependencies.addToServiceManager(service);
            registerModuleService(service);
        }
    }

    protected void registerModuleService(@NonNull IBinder service) { }

    /**
     * Log a message in the local log.
     */
    protected void log(@NonNull String message) {
        synchronized (mLog) {
            mLog.log(message);
        }
    }

    protected void logWtf(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        Slog.wtf(tag, message);
        synchronized (mLog) {
            mLog.e(message, e);
        }
    }

    protected void loge(@NonNull String message, @Nullable Throwable e) {
        synchronized (mLog) {
            mLog.e(message, e);
        }
    }

    /**
     * Log a message in the local and system logs.
     */
    protected void logi(@NonNull String message) {
        synchronized (mLog) {
            mLog.i(message);
        }
    }

    /**
     * For non-system server clients, get the connector registered by the system server.
     */
    protected IBinder getRemoteConnector(String moduleName) {
        // Block until the Module connector is registered in ServiceManager.
        // <p>This is only useful for non-system processes that do not have a way to be notified of
        // registration completion. Adding a callback system would be too heavy weight considering
        // that the connector is registered on boot, so it is unlikely that a client would request
        // it before it is registered.
        // TODO: consider blocking boot on registration and simplify much of the logic in this class
        IBinder connector;
        try {
            final long before = System.currentTimeMillis();
            while ((connector = ServiceManager.getService(moduleName)) == null) {
                Thread.sleep(20);
                if (System.currentTimeMillis() - before > MODULE_TIMEOUT_MS) {
                    loge("Timeout waiting for Module connector", null);
                    return null;
                }
            }
        } catch (InterruptedException e) {
            loge("Error waiting for Module connector", e);
            return null;
        }

        return connector;
    }
}
