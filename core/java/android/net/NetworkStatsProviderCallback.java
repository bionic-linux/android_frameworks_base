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
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.os.RemoteException;

/**
 * A callback class that allows caller reports events to the system.
 * @hide
 */
@SystemApi
@SuppressLint("CallbackMethodName")
public class NetworkStatsProviderCallback {
    private @NonNull final INetworkStatsProviderCallback mBinder;

    /** @hide */
    public NetworkStatsProviderCallback(@NonNull INetworkStatsProviderCallback binder) {
        mBinder = binder;
    }

    /**
     * Notifies that there is an updated network statistics that needs to be combined into system
     * since last {@link #onStatsUpdated(int, NetworkStats, NetworkStats)}. Expected to be triggered
     * within 1 minute after the {@link NetworkStatsProviderBase#requestStatsUpdate(int)} has been
     * called, or whenever the provider thinks there is some statistics needs to be reported.
     * Note that this does not trigger the system to immediately propagate the statistics to reflect
     * the update. To do so, calls {@link NetworkStatsProviderCallback#onAlertReached()} when
     * needed.
     *
     * @param token the identifier used by the system.
     *              See {@link NetworkStatsProviderBase#requestStatsUpdate(int)}.
     * @param ifaceStats the {@link NetworkStats} per interface to be reported. The provider should
     *                   not include any traffic that is already counted by kernel interface
     *                   counters.
     * @param uidStats the same as above, but counts {@link NetworkStats} per uid.
     */
    public void onStatsUpdated(int token, @NonNull NetworkStats ifaceStats,
            @NonNull NetworkStats uidStats) {
        try {
            mBinder.onStatsUpdated(token, ifaceStats, uidStats);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Notifies system that the quota set by {@code setAlert} is reached.
     */
    public void onAlertReached() {
        try {
            mBinder.onAlertReached();
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Notifies system that the quota set by {@code setLimit} is reached.
     */
    public void onLimitReached() {
        try {
            mBinder.onLimitReached();
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Unregister the provider and the referencing callback.
     */
    public void unregister() {
        try {
            mBinder.unregister();
        } catch (RemoteException e) {
            // Ignore error.
        }
    }
}
