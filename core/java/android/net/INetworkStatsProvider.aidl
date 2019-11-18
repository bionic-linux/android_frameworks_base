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

import android.net.NetworkStats;
import android.net.INetworkStatsProviderCallback;

/**
 * Interface for NetworkStatsService to query network statistics and set data limits.
 *
 * @hide
 */
oneway interface INetworkStatsProvider {

    // FIXME
    // Returns cumulative statistics for all sessions since boot, on all upstreams.
    // @code {how} is one of the NetworkStats.STATS_PER_* constants. If {@code how} is
    // {@code STATS_PER_IFACE}, the provider should not include any traffic that is already
    // counted by kernel interface counters.
    void requestStatsUpdate(int how);

    // FIXME
    // Sets the interface quota for the specified upstream interface. This is defined as the number
    // of bytes, starting from zero and counting from now, after which data should stop being
    // forwarded to/from the specified upstream. A value of QUOTA_UNLIMITED means there is no limit.
    void setLimit(String iface, long quotaBytes);

    void setAlert(long quotaBytes);
}
