/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.connectivity;

import android.net.NetworkAgent;
import android.os.SystemClock;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for logging VPN connectivity health event
 * @hide
 */
public class VpnConnectivityMetrics {
    private List<VpnConnection> mVpnConnectionList = new ArrayList<>();
    private Map<Integer, VpnMetricCollector> mVpnMetricCollectorMap = new HashMap<>();
    private final Dependencies mDependencies;

    public VpnConnectivityMetrics() {
        this(new Dependencies());
    }

    @VisibleForTesting
    public VpnConnectivityMetrics(Dependencies dependencies) {
        mDependencies = dependencies;
    }

    @VisibleForTesting
    public static class Dependencies {
        public long getElapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }
    }

    public class VpnMetricCollector {
        private final int mUserId;
        private long mVpnConnectionPeriodMs = 0;
        // Each user should have only 1 VPN network agent connected but in the case where the VPN is
        // restarted, a new network agent will be connected before the old network is disconnected.
        private Map<NetworkAgent, Long> mVpnConnectedTimestampMs = new HashMap<>();

        VpnMetricCollector(int userId) {
            mUserId = userId;
        }

        private VpnConnectionParams mVpnConnectionParams = null;

        private void updateTimestamps(long timeNow) {
            for (Map.Entry<NetworkAgent, Long> entry : mVpnConnectedTimestampMs.entrySet()) {
                mVpnConnectionPeriodMs += timeNow - entry.getValue();
                entry.setValue(timeNow);
            }
        }

        /** Build VpnConnection proto and add it to the stored list. */
        private void buildAndAppendVpnConnectionMetric() {
            if (mVpnConnectionParams == null) {
                return;
            }
            updateTimestamps(mDependencies.getElapsedRealtime());
            final VpnConnection vpnConnection = new VpnConnection();
            vpnConnection.setVpnConnectionParams(mVpnConnectionParams);
            vpnConnection.setConnectedPeriodSeconds((int) (mVpnConnectionPeriodMs / 1000));

            mVpnConnectionList.add(vpnConnection);
        }

        /** Inform the VpnMetricCollector that an app starts a Vpn. */
        public void onAppStarted() {
            mVpnConnectionParams = new VpnConnectionParams();
            // TODO: Fill the values of VpnConnectionParams.
        }

        /** Inform the VpnMetricCollector that a Vpn network is connected. */
        public void onVpnConnected(NetworkAgent networkAgent) {
            if (mVpnConnectedTimestampMs.containsKey(networkAgent)) {
                Log.wtf(getTag(), "onVpnConnected called on an already connected NetworkAgent: "
                        + networkAgent);
            }
            mVpnConnectedTimestampMs.put(networkAgent, mDependencies.getElapsedRealtime());
        }

        /** Inform the VpnMetricCollector that a Vpn network is disconnected. */
        public void onVpnDisconnected(NetworkAgent networkAgent) {
            if (!mVpnConnectedTimestampMs.containsKey(networkAgent)) {
                Log.wtf(getTag(), "onVpnDisconnected called on an unknown NetworkAgent: "
                        + networkAgent);
                return;
            }
            mVpnConnectionPeriodMs +=
                    mDependencies.getElapsedRealtime() - mVpnConnectedTimestampMs.get(networkAgent);
            mVpnConnectedTimestampMs.remove(networkAgent);
        }

        private String getTag() {
            return VpnMetricCollector.class.getSimpleName() + "/" + mUserId;
        }
    }

    /** Create a new VpnMetricCollector for the given user, to be used in Vpn */
    public VpnMetricCollector newCollector(int userId) {
        final VpnMetricCollector newVpnMetricCollector = new VpnMetricCollector(userId);
        mVpnMetricCollectorMap.put(userId, newVpnMetricCollector);
        return newVpnMetricCollector;
    }

    /** Build and get the currently stored metrics */
    @VisibleForTesting
    public List<VpnConnection> pullMetrics() {
        for (VpnMetricCollector vpnMetricCollector : mVpnMetricCollectorMap.values()) {
            vpnMetricCollector.buildAndAppendVpnConnectionMetric();
        }

        return mVpnConnectionList;
    }
}
