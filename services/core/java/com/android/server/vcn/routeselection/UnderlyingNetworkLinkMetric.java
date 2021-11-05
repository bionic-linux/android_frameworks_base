/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.vcn.routeselection;

import android.annotation.NonNull;

import com.android.server.vcn.UnderlyingNetworkTracker.UnderlyingNetworkRecord;
import com.android.server.vcn.VcnContext;

import java.util.Objects;

/**
 * Monitors observable link qualities of an underlying network.
 *
 * @hide
 */
public abstract class UnderlyingNetworkLinkMetric extends UnderlyingNetworkMetric {
    private static final String TAG = UnderlyingNetworkLinkMetric.class.getSimpleName();

    protected UnderlyingNetworkRecord mUnderlyingNetworkRecord;

    protected UnderlyingNetworkLinkMetric(
            @NonNull VcnContext vcnContext,
            @NonNull UnderlyingNetworkRecord record,
            int defaultState,
            boolean requiresWakelock,
            @NonNull NetworkMetricCallback listener) {
        super(
                vcnContext,
                Objects.requireNonNull(record, "Missing record").network,
                defaultState,
                requiresWakelock,
                listener);

        // Non-null enforced in super constructor call above
        mUnderlyingNetworkRecord = record;
    }

    public void setUnderlyingNetworkRecord(@NonNull UnderlyingNetworkRecord record) {
        mUnderlyingNetworkRecord = Objects.requireNonNull(record, "Missing record");

        triggerReevaluation();
    }
}
