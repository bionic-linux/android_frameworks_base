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

import static com.android.server.vcn.routeselection.metric.UnderlyingNetworkMetric.METRIC_STATE_ACCEPTABLE;
import static com.android.server.vcn.routeselection.metric.UnderlyingNetworkMetric.METRIC_STATE_NA;
import static com.android.server.vcn.routeselection.metric.UnderlyingNetworkMetric.METRIC_STATE_NOT_ACCEPTABLE;

import android.annotation.NonNull;
import android.net.vcn.VcnGatewayConnectionConfig;

import com.android.server.vcn.VcnContext;
import com.android.server.vcn.routeselection.metric.UnderlyingNetworkMetric;

import java.util.ArrayList;
import java.util.List;

/** @hide */
public abstract class UnderlyingNetworkMetricAggregator<T extends UnderlyingNetworkMetric> {
    private boolean mIsEnabled = false;
    private int mMetricState = METRIC_STATE_NA;

    private final UnderlyingNetworkMetricAggregatorCallback mCallback;

    protected final List<T> mMetricMonitors = new ArrayList<>();

    public UnderlyingNetworkMetricAggregator(
            @NonNull VcnContext vcnContext,
            @NonNull UnderlyingNetworkRecord record,
            @NonNull VcnGatewayConnectionConfig connectionConfig,
            @NonNull UnderlyingNetworkMetricAggregatorCallback callback) {
        mCallback = callback;
    }

    public interface UnderlyingNetworkMetricAggregatorCallback {
        void onAggregatedMetricChanged();
    }

    // Trigger by monitors
    protected void handleMetricChange() {
        int metricState = METRIC_STATE_ACCEPTABLE;
        for (UnderlyingNetworkMetric metric : mMetricMonitors) {
            // Some monitors might be disabled because it is not applicable. For example
            // WifiSignalStrengthMonitor will be disabled for cell network
            if (metric.isEnabled()) {
                if (metric.getMetricState() == METRIC_STATE_NOT_ACCEPTABLE) {
                    metricState = METRIC_STATE_NOT_ACCEPTABLE;
                    break;
                } else if (metric.getMetricState() == METRIC_STATE_NA) {
                    metricState = METRIC_STATE_NA;
                    break;
                }
            }
        }

        if (mMetricState == metricState) {
            return;
        }

        mCallback.onAggregatedMetricChanged();
    }

    public int getMetricState() {
        return mMetricState;
    }

    public void tearDown() {
        setEnabled(false /* enabled */);
    }

    /** Enables or disables monitoring of this metric. Don't need to notify caller */
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }

        mIsEnabled = enabled;
        mMetricState = METRIC_STATE_NA;

        for (T metric : mMetricMonitors) {
            metric.setEnabled(enabled);
        }
    }
}
