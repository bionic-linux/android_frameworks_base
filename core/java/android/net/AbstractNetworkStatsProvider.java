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
import android.annotation.SystemApi;

/**
 * A base class that allows external modules to implement a custom network statistics provider.
 * @hide
 */
@SystemApi
public abstract class AbstractNetworkStatsProvider {
    /**
     * Called by {@code NetworkStatsService} when the global polling is needed. Custom
     * implementation of providers MUST respond to it by calling
     * {@link NetworkStatsProviderCallback#onStatsUpdated(int, NetworkStats, NetworkStats)} within 1
     * minute, a late response might cause the system failed to record the given statistics, and
     * results in data lost.
     *
     * @param token a positive number that used by the system to associate with
     *              {@link NetworkStats}. Note that custom implementation of providers MUST report
     *              locally stored token with current {@link NetworkStats} via
     *              {@link NetworkStatsProviderCallback#onStatsUpdated} before updating the given
     *              token to local variable.
     */
    public abstract void requestStatsUpdate(int token);

    /**
     * Called by {@code NetworkStatsService} when sets the interface quota for the specified
     * upstream interface. If called, the custom implementation should block all egress packets on
     * the {@code iface} that associated with the provider, when the {@code quotaBytes} has been
     * reached, and MUST respond to it by calling
     * {@link NetworkStatsProviderCallback#onLimitReached()}.
     *
     * @param iface the interface requires the operation.
     * @param quotaBytes the quota defined as the number of bytes, starting from zero and counting
     *                   from now. A value of
     *                   {@link android.app.usage.NetworkStatsManager#QUOTA_UNLIMITED} indicates
     *                   there is no limit.
     */
    public abstract void setLimit(@NonNull String iface, long quotaBytes);

    /**
     * Called by {@code NetworkStatsService} when sets the alert bytes. The custom implementation
     * MUST call {@link NetworkStatsProviderCallback#onAlertReached()} when the {@code quotaBytes}
     * has been reached. Unlike {@link #setLimit(String, long)}, the custom implementation does not
     * need to block all egress packets when the {@code quotaBytes} has been reached.
     *
     * @param quotaBytes the quota defined as the number of bytes, starting from zero and counting
     *                   from now. A value of
     *                   {@link android.app.usage.NetworkStatsManager#QUOTA_UNLIMITED} indicates
     *                   there is no alert.
     */
    public abstract void setAlert(long quotaBytes);
}
