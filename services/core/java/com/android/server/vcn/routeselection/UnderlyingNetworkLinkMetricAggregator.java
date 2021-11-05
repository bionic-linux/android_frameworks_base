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
import android.annotation.Nullable;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.os.PersistableBundle;

import com.android.server.vcn.VcnContext;
import com.android.server.vcn.routeselection.metric.UnderlyingNetworkLinkMetric;
import com.android.server.vcn.routeselection.metric.WifiSignalStrengthLinkMetricMonitor;

/** @hide */
public class UnderlyingNetworkLinkMetricAggregator
        extends UnderlyingNetworkMetricAggregator<UnderlyingNetworkLinkMetric> {
    public UnderlyingNetworkLinkMetricAggregator(
            @NonNull VcnContext vcnContext,
            @NonNull UnderlyingNetworkRecord record,
            @NonNull VcnGatewayConnectionConfig connectionConfig,
            @Nullable PersistableBundle carrierConfig,
            @NonNull UnderlyingNetworkMetricAggregatorCallback callback) {
        super(vcnContext, record, connectionConfig, callback);
        mMetricMonitors.add(
                new WifiSignalStrengthLinkMetricMonitor(
                        vcnContext,
                        record,
                        () -> {
                            handleMetricChange();
                        },
                        carrierConfig));
    }

    public void setNetworkSelected(boolean isSelected) {
        for (UnderlyingNetworkLinkMetric metric : mMetricMonitors) {
            metric.setNetworkSelected(isSelected);
        }
    }

    public void setUnderlyingNetworkRecord(UnderlyingNetworkRecord record) {
        for (UnderlyingNetworkLinkMetric metric : mMetricMonitors) {
            metric.setUnderlyingNetworkRecord(record);
        }
    }
}
