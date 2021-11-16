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

package com.android.server.vcn.routeselection.metric;

import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkCapabilities;
import android.net.vcn.VcnManager;
import android.os.PersistableBundle;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.server.vcn.VcnContext;
import com.android.server.vcn.routeselection.UnderlyingNetworkRecord;
import com.android.server.vcn.routeselection.metric.UnderlyingNetworkMetric.NetworkMetricCallback;

/**
 * Monitors WIFI signal strength
 *
 * @hide
 */
public class WifiSignalStrengthLinkMetricMonitor extends UnderlyingNetworkLinkMetric {
    /**
     * Minimum signal strength for a WiFi network to be eligible for switching to
     *
     * <p>A network that satisfies this is eligible to become the selected underlying network with
     * no additional conditions
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final int WIFI_ENTRY_RSSI_THRESHOLD_DEFAULT = -70;

    /**
     * Minimum signal strength to continue using a WiFi network
     *
     * <p>A network that satisfies the conditions may ONLY continue to be used if it is already
     * selected as the underlying network. A WiFi network satisfying this condition, but NOT the
     * prospective-network RSSI threshold CANNOT be switched to.
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final int WIFI_EXIT_RSSI_THRESHOLD_DEFAULT = -74;

    private final PersistableBundle mCarrierConfig;

    private boolean mIsSelectedNetwork = false;

    public WifiSignalStrengthLinkMetricMonitor(
            @NonNull VcnContext vcnContext,
            @NonNull UnderlyingNetworkRecord record,
            @NonNull NetworkMetricCallback listener,
            @Nullable PersistableBundle carrierConfig) {
        super(vcnContext, record, false /* requiresWakelock */, listener);
        mCarrierConfig = carrierConfig;
    }

    @Override
    public void setNetworkSelected(boolean isSelected) {
        mIsSelectedNetwork = isSelected;

        if (isEnabled()) {
            evaluateMetric();
        }
    }

    @Override
    public void evaluateMetric() {
        final NetworkCapabilities caps = mUnderlyingNetworkRecord.networkCapabilities;
        final int threshold =
                mIsSelectedNetwork
                        ? getWifiExitRssiThreshold(mCarrierConfig)
                        : getWifiEntryRssiThreshold(mCarrierConfig);
        if (caps.getSignalStrength() >= threshold) {
            setMetricStateAndMaybeNotifyListeners(METRIC_STATE_ACCEPTABLE);
        } else {
            setMetricStateAndMaybeNotifyListeners(METRIC_STATE_NOT_ACCEPTABLE);
        }
    }

    @Override
    public boolean isApplicable() {
        return mUnderlyingNetworkRecord.networkCapabilities.hasTransport(TRANSPORT_WIFI);
    }

    public static int getWifiEntryRssiThreshold(@Nullable PersistableBundle carrierConfig) {
        if (carrierConfig != null) {
            return carrierConfig.getInt(
                    VcnManager.VCN_NETWORK_SELECTION_WIFI_ENTRY_RSSI_THRESHOLD_KEY,
                    WIFI_ENTRY_RSSI_THRESHOLD_DEFAULT);
        }

        return WIFI_ENTRY_RSSI_THRESHOLD_DEFAULT;
    }

    public static int getWifiExitRssiThreshold(@Nullable PersistableBundle carrierConfig) {
        if (carrierConfig != null) {
            return carrierConfig.getInt(
                    VcnManager.VCN_NETWORK_SELECTION_WIFI_EXIT_RSSI_THRESHOLD_KEY,
                    WIFI_EXIT_RSSI_THRESHOLD_DEFAULT);
        }

        return WIFI_EXIT_RSSI_THRESHOLD_DEFAULT;
    }
}
