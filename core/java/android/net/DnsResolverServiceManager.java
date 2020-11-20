/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;

import java.util.Objects;

/**
 * Provides a way to obtain the DnsResolver binder objects.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class DnsResolverServiceManager {
    /**
     * Name to retrieve a {@link android.net.IDnsResolver} IBinder.
     */
    private static final String DNS_RESOLVER_SERVICE = "dnsresolver";
    @Nullable
    private static volatile IBinder sMockService;

    private DnsResolverServiceManager() {}

    /**
     * Get an {@link IBinder} representing the DnsResolver stable AIDL interface or null if the
     * service is not registered.
     */
    @Nullable
    @RequiresPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK)
    public static IBinder getService(@NonNull final Context context) {
        Objects.requireNonNull(context);
        context.enforceCallingOrSelfPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
                "DnsResolverServiceManager");

        final IBinder mockService = sMockService;
        if (mockService != null) return mockService;
        return ServiceManager.getService(DNS_RESOLVER_SERVICE);
    }

    /**
     * Set a mock service for testing, to be returned by future calls to {@link #getService()}.
     *
     * <p>Passing a {@code null} {@code mockService} resets {@link #getService()} to normal
     * behavior.
     * @hide
     */
    public static void setServiceForTest(@Nullable IBinder mockService) {
        sMockService = mockService;
    }
}

