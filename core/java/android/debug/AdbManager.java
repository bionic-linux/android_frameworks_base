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

package android.debug;

import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;

/**
 * This class allows the control of ADB-related functions. Currently only ADB over USB is
 * supported, and none of the API is public.
 *
 * @hide
 */
@SystemService(Context.ADB_SERVICE)
public class AdbManager {
    private static final String TAG = "AdbManager";

    private final Context mContext;
    private final IAdbManager mService;

    /**
     * {@hide}
     */
    public AdbManager(Context context, IAdbManager service) {
        mContext = context;
        mService = service;
    }

    /**
     * Registers a previously registered ADB transport mechanism.
     */
    public void registerTransport(IAdbTransport transport) {
        try {
            mService.registerTransport(transport);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Unregisters a previously registered ADB transport mechanism.
     */
    public void unregisterTransport(IAdbTransport transport) {
        try {
            mService.unregisterTransport(transport);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }
}
