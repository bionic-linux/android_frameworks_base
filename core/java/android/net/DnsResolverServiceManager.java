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

import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.IBinder;
import android.os.ServiceManager;

/**
 * Provides a way to obtain the DnsResolver binder objects.
 *
 * <p>Only the connectivity mainline module will be able to access this class.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
public final class DnsResolverServiceManager {
    /**
     * Name to retrieve a {@link android.net.IDnsResolver} IBinder.
     */
    private static final String DNS_RESOLVER_SERVICE = "dnsresolver";
    @Nullable
    private static volatile IBinder sMockService;

    private DnsResolverServiceManager() {}

    /**
     * Get an {@link IBinder} representing the DnsResolver stable AIDL interface, if registered.
     * @hide
     */
    @Nullable
    @SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
    public static IBinder getService() {
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
