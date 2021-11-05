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
import android.net.Network;

import com.android.server.vcn.VcnContext;

/**
 * Monitors traffic flow of an underlying network.
 *
 * <p>This method implicitly relies on traffic flow in order to function; if the network is not the
 * selected underlying network, this will likely not provide any useful data, and should be
 * disabled.
 *
 * @hide
 */
public abstract class UnderlyingNetworkTrafficFlowMetric extends UnderlyingNetworkMetric {
    private static final String TAG = UnderlyingNetworkTrafficFlowMetric.class.getSimpleName();

    protected UnderlyingNetworkTrafficFlowMetric(
            @NonNull VcnContext vcnContext,
            @NonNull Network network,
            int defaultState,
            boolean requiresWakelock,
            @NonNull NetworkMetricCallback listener) {
        super(vcnContext, network, defaultState, requiresWakelock, listener);
    }
}
