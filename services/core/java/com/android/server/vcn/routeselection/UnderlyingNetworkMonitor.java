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

package com.android.server.vcn;

import static com.android.server.VcnManagementService.LOCAL_LOG;
import static com.android.server.VcnManagementService.VDBG;
import static com.android.server.vcn.routeselection.UnderlyingNetworkMetric.METRIC_STATE_ACCEPTABLE;
import static com.android.server.vcn.routeselection.UnderlyingNetworkMetric.METRIC_STATE_NOT_ACCEPTABLE;

import android.annotation.NonNull;
import android.net.Network;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.os.Handler;
import android.util.Slog;

import com.android.server.vcn.routeselection.UnderlyingNetworkActiveProberMetric;
import com.android.server.vcn.routeselection.UnderlyingNetworkLinkMetric;
import com.android.server.vcn.routeselection.UnderlyingNetworkMetric;
import com.android.server.vcn.routeselection.UnderlyingNetworkTrafficFlowMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Monitors a single network underlying a VcnGatewayConnection
 *
 * <p>A single UnderlyingNetworkTracker is built to monitor a SINGLE underlying network, and MUST be
 * bound to the lifecycle of an underlying network (ie, created upon onAvailable(), and torn down
 * upon onLost(), or teardown of the UnderlyingNetworkController).
 *
 * @hide
 */
public class UnderlyingNetworkMonitor {
    private static final String TAG = UnderlyingNetworkMonitor.class.getSimpleName();

    /**
     * Denotes that the underlying network that is being monitored is currently active.
     *
     * <p>While active, all metrics should be actively monitored. If any metric fails in this state,
     * the network will be put into the penalty box.
     */
    public static final int NETWORK_STATE_ACTIVE = 1;

    /**
     * Denotes that the underlying network is being re-evaluated to supersede the current active
     * network.
     *
     * <p>While in the prospective state, link metrics and active probers should be monitored.
     *
     * <p>A network monitor may only stay in this state for a specified duration; if the network
     * does not validate within a timeout, it will be put into the penalty box.
     */
    public static final int NETWORK_STATE_PROSPECTIVE = 2;

    public final Object mProspectiveTimeoutToken = new Object();

    /**
     * Denotes that the underlying network is being monitored in the background.
     *
     * <p>While in the background, only link metrics are monitored.
     */
    public static final int NETWORK_STATE_BACKGROUND = 3;

    /**
     * Denotes that the underlying network was previously marked as failed, and is barred from
     * attempting to revalidate for a timeout.
     *
     * <p>While in the penalty box, no metrics are monitored.
     */
    public static final int NETWORK_STATE_IN_PENALTY_BOX = 4;

    public final Object mPenaltyBoxTimeoutToken = new Object();

    private final VcnContext mVcnContext;
    private final VcnGatewayConnectionConfig mConnectionConfig;
    private final Network mNetwork;

    private final List<UnderlyingNetworkMetric> mMetrics = new ArrayList<>();

    private final Handler mHandler;

    private int mNetworkState;

    public UnderlyingNetworkMonitor(
            @NonNull VcnContext vcnContext,
            @NonNull VcnGatewayConnectionConfig connectionConfig,
            @NonNull Network network) {
        mVcnContext = Objects.requireNonNull(vcnContext, "Missing VcnContext");
        mConnectionConfig = Objects.requireNonNull(connectionConfig, "Missing ConnectionConfig");
        mNetwork = Objects.requireNonNull(network, "Missing Network");

        mHandler = new Handler(mVcnContext.getLooper());

        mNetworkState = NETWORK_STATE_BACKGROUND;

        // TODO: Create metrics
    }

    public int getPriorityClass() {
        // TODO: Wire this up
        return -1;
    }

    /**
     * Updates the state of the Underlying Network
     *
     * <p>Enables/disables metrics and schedules timers as needed
     */
    public void setNetworkState(int newState) {
        if (newState == mNetworkState) {
            return;
        }

        boolean linkMetricsEnabled = false;
        boolean trafficFlowMetricsEnabled = false;
        boolean activeProbingEnabled = false;

        switch (newState) {
            case NETWORK_STATE_ACTIVE:
                trafficFlowMetricsEnabled = true;
                // fallthrough
            case NETWORK_STATE_PROSPECTIVE:
                activeProbingEnabled = true;
                // fallthrough
            case NETWORK_STATE_BACKGROUND:
                linkMetricsEnabled = true;
                // fallthrough
            case NETWORK_STATE_IN_PENALTY_BOX:
                break;
            default:
                logWtf("Unknown network state: " + newState);
                return;
        }

        for (UnderlyingNetworkMetric metric : mMetrics) {
            if (metric instanceof UnderlyingNetworkLinkMetric) {
                metric.setEnabled(linkMetricsEnabled);
            } else if (metric instanceof UnderlyingNetworkTrafficFlowMetric) {
                metric.setEnabled(trafficFlowMetricsEnabled);
            } else if (metric instanceof UnderlyingNetworkActiveProberMetric) {
                metric.setEnabled(activeProbingEnabled);
            } else {
                logWtf("Unknown metric type: " + metric.getClass().getCanonicalName());
            }
        }

        // Remove all pending timers. If we've changed states, the old status is moot.
        mHandler.removeCallbacksAndMessages(mPenaltyBoxTimeoutToken);
        mHandler.removeCallbacksAndMessages(mProspectiveTimeoutToken);

        if (newState == NETWORK_STATE_IN_PENALTY_BOX) {
            mHandler.postDelayed(
                    () -> {
                        setNetworkState(NETWORK_STATE_BACKGROUND);
                    },
                    mPenaltyBoxTimeoutToken,
                    getPenaltyBoxTimeout());
        } else if (newState == NETWORK_STATE_PROSPECTIVE) {
            mHandler.postDelayed(
                    () -> {
                        setNetworkState(NETWORK_STATE_IN_PENALTY_BOX);
                    },
                    mProspectiveTimeoutToken,
                    getProspectiveTimeout());
        }

        mNetworkState = newState;
    }

    private long getPenaltyBoxTimeout() {
        // TODO: Wire this up
        return 1000;
    }

    private long getProspectiveTimeout() {
        // TODO: Wire this up
        return 1000;
    }

    private void handleOnMetricChanged() {
        if (mNetworkState == NETWORK_STATE_IN_PENALTY_BOX) {
            return; // No point re-evaluating.
        }

        int overallMetricState = METRIC_STATE_ACCEPTABLE;
        for (UnderlyingNetworkMetric metric : mMetrics) {
            // Finds the lowest common state. Ignored metrics use Integer.MAX_VALUE, and do not
            // affect this logic.
            overallMetricState = Math.min(overallMetricState, metric.getMetricState());
        }

        switch (overallMetricState) {
            case METRIC_STATE_ACCEPTABLE:
                // TODO: If new priority class <= current active network class, force to background.

                if (mNetworkState == NETWORK_STATE_ACTIVE
                        || mNetworkState == NETWORK_STATE_PROSPECTIVE) {
                    // TODO: trigger callback to reselect networks
                } else if (mNetworkState == NETWORK_STATE_BACKGROUND) {
                    setNetworkState(NETWORK_STATE_PROSPECTIVE);
                }
                break;
            case METRIC_STATE_NOT_ACCEPTABLE:
                // Only penalize network if active; prospective networks may be waiting for the
                // initial
                // validation run to complete, and background networks may be considered
                // unacceptable
                // due to rapidly changing signals (eg. bandwidth constraints)
                if (mNetworkState == NETWORK_STATE_ACTIVE) {
                    setNetworkState(NETWORK_STATE_IN_PENALTY_BOX);
                }
                break;
            default:
                logWtf("Unknown or unexpected metric state: " + overallMetricState);
        }
    }

    private class UnderlyingNetworkMetricListener
            implements UnderlyingNetworkMetric.NetworkMetricCallback {
        @Override
        public void onMetricChanged() {
            handleOnMetricChanged();
        }
    }

    /**
     * Callback interface for the UnderlyingNetworkMonitor to notify that the monitor state changed.
     */
    public static interface NetworkMonitorCallback {
        void onMonitorStateChanged();
    }

    private String getLogPrefix() {
        return "[" + mConnectionConfig.getGatewayConnectionName() + "-" + mNetwork + "] ";
    }

    private void logVdbg(String msg) {
        if (VDBG) {
            Slog.v(TAG, getLogPrefix() + msg);
        }
    }

    private void logDbg(String msg) {
        Slog.d(TAG, getLogPrefix() + msg);
    }

    private void logDbg(String msg, Throwable tr) {
        Slog.d(TAG, getLogPrefix() + msg, tr);
    }

    private void logWarn(String msg) {
        Slog.w(TAG, getLogPrefix() + msg);
        LOCAL_LOG.log(getLogPrefix() + "WARN: " + msg);
    }

    private void logWarn(String msg, Throwable tr) {
        Slog.w(TAG, getLogPrefix() + msg, tr);
        LOCAL_LOG.log(getLogPrefix() + "WARN: " + msg + tr);
    }

    private void logErr(String msg) {
        Slog.e(TAG, getLogPrefix() + msg);
        LOCAL_LOG.log(getLogPrefix() + "ERR: " + msg);
    }

    private void logErr(String msg, Throwable tr) {
        Slog.e(TAG, getLogPrefix() + msg, tr);
        LOCAL_LOG.log(getLogPrefix() + "ERR: " + msg + tr);
    }

    private void logWtf(String msg) {
        Slog.wtf(TAG, getLogPrefix() + msg);
        LOCAL_LOG.log(getLogPrefix() + "WTF: " + msg);
    }

    private void logWtf(String msg, Throwable tr) {
        Slog.wtf(TAG, getLogPrefix() + msg, tr);
        LOCAL_LOG.log(getLogPrefix() + "WTF: " + msg + tr);
    }
}
