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
public abstract class NetworkStatsProviderBase {
    /**
     * Called by {@code NetworkStatsService} when the global polling is needed. Custom
     * implementation of providers SHOULD respond to it by calling
     * {@link NetworkStatsProviderCallback#onStatsUpdated(int, NetworkStats)}.
     * Note that this does not trigger the system to immediately propagate the statistics to reflect
     * the update.
     *
     * @param how one of NetworkStats.STATS_PER_* constants, indicates the type of {@code stats}
     *            that needs to be reported.
     */
    public abstract void requestStatsUpdate(int how);

    /**
     * Called by {@code NetworkStatsService} when sets the interface quota for the specified
     * upstream interface. The custom implementation should block the networking when the
     * {@code quotaBytes} has been reached, and MUST respond to it by calling
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
     * Called by {@code NetworkStatsService} when sets the interface quota for the specified
     * upstream interface. The custom implementation MUST calling
     * {@link NetworkStatsProviderCallback#onAlertReached()} when the {@code quotaBytes} has been
     * reached.
     *
     * @param quotaBytes the quota defined as the number of bytes, starting from zero and counting
     *                   from now. A value of
     *                   {@link android.app.usage.NetworkStatsManager#QUOTA_UNLIMITED} indicates
     *                   there is no limit.
     */
    public abstract void setAlert(long quotaBytes);

}
