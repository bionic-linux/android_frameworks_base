/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.annotation.TestApi;
import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
/**
 * Constants and utilities for client code communicating with the network stack service.
 * @hide
 */
@SystemApi
public class NetworkStack {
    /**
     * Permission granted only to the NetworkStack APK, defined in NetworkStackStub with signature
     * protection level.
     * @hide
     */
    @SystemApi
    public static final String PERMISSION_MAINLINE_NETWORK_STACK =
            "android.permission.MAINLINE_NETWORK_STACK";

    @Nullable
    private static volatile IBinder sMockService;

    /**
     * Get an {@link IBinder} representing the NetworkStack stable AIDL Interface, if registered.
     * @hide
     */
    @Nullable
    @SystemApi
    public static IBinder getService() {
        final IBinder mockService = sMockService;
        if (mockService != null) return mockService;
        return ServiceManager.getService(Context.NETWORK_STACK_SERVICE);
    }

    /**
     * Set a mock service for testing, to be returned by future calls to {@link #getService()}.
     *
     * <p>Passing a {@code null} {@code mockService} resets {@link #getService()} to normal
     * behavior.
     * @hide
     */
    @TestApi
    public static void setServiceForTest(@Nullable IBinder mockService) {
        sMockService = mockService;
    }

    private NetworkStack() {}

}
