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
 * Utilities for obtaining the DnsResolver stable AIDL Interface.
 * @hide
 */
@SystemApi
public final class DnsResolverService {
    /**
     * Name to retrieve a {@link android.net.IDnsResolver} IBinder.
     */
    private static final String DNS_RESOLVER_SERVICE = "dnsresolver";

    private DnsResolverService() {}

    /**
     * Get an {@link IBinder} representing the DnsResolver stable AIDL Interface, if registered.
     * @hide
     */
    @Nullable
    @SystemApi
    public static IBinder getService() {
        return ServiceManager.getService(DNS_RESOLVER_SERVICE);
    }
}
