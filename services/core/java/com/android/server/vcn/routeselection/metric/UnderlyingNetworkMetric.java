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

import android.annotation.NonNull;
import android.net.Network;
import android.os.Handler;
import android.os.PowerManager;

import com.android.server.vcn.VcnContext;
import com.android.server.vcn.util.VcnWakeLock;

import java.util.Objects;

/**
 * Monitors a single aspect of an underlying network.
 *
 * @hide
 */
public abstract class UnderlyingNetworkMetric {
    private static final String TAG = UnderlyingNetworkMetric.class.getSimpleName();

    /** Denotes that this metric's thresholds has been met by the underlying network */
    public static final int METRIC_STATE_NOT_ACCEPTABLE = 0;

    /** Denotes that this metric's thresholds have NOT been met by the underlying network */
    public static final int METRIC_STATE_ACCEPTABLE = 1;

    /** Denotes that this metric is not available because metric disabled or result pending */
    public static final int METRIC_STATE_NA = Integer.MAX_VALUE;

    private final Object mEvaluationToken = new Object();

    private final VcnContext mVcnContext;
    private final Network mNetwork;
    private final NetworkMetricCallback mCallback;

    private final Handler mHandler;
    private final VcnWakeLock mWakeLock;

    private boolean mIsEnabled = false;
    private int mMetricState = METRIC_STATE_NA;

    protected UnderlyingNetworkMetric(
            @NonNull VcnContext vcnContext,
            @NonNull Network network,
            boolean requiresWakelock,
            @NonNull NetworkMetricCallback listener) {
        mVcnContext = Objects.requireNonNull(vcnContext, "Missing context");
        mNetwork = Objects.requireNonNull(network, "Missing Network");
        mCallback = Objects.requireNonNull(listener, "Missing NetworkMetricCallback");

        mHandler = new Handler(mVcnContext.getLooper());
        mWakeLock =
                requiresWakelock
                        ? new VcnWakeLock(
                                mVcnContext.getContext(), PowerManager.PARTIAL_WAKE_LOCK, TAG)
                        : null;
    }

    /** Retrieves the current metric state. */
    public int getMetricState() {
        return mMetricState;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    /** Enables or disables monitoring of this metric. */
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled || !isApplicable()) {
            return;
        }

        mIsEnabled = enabled;
        mMetricState = METRIC_STATE_NA;
        if (enabled) {
            enableMetric();
        } else {
            disableMetric();
        }
    }

    private void enableMetric() {
        triggerReevaluation();
    }

    private void disableMetric() {
        mHandler.removeCallbacksAndMessages(mEvaluationToken);
    }

    /**
     * Triggers immediate reevaluation of this metric.
     *
     * <p>Metric reevaluation will be performed asynchronously, and a update will be triggered if
     * the state changes.
     */
    public void triggerReevaluation() {
        if (!mIsEnabled) {
            return;
        }

        scheduleEvaluation(0 /* delayMillis */);
    }

    public void setNetworkSelected(boolean isSelected) {
        // Do nothing by default. Subclasses that need this information MUST implement it.
    }

    /**
     * Schedules a subsequent evaluation after the given delay.
     *
     * @param delayMillis the minimum delay before starting the next evaluation. Note that this may
     *     be delayed further if the device goes into standby, or something is blocking the handler
     *     thread.
     */
    protected void scheduleEvaluation(long delayMillis) {
        mHandler.postDelayed(this::doEvaluateMetric, mEvaluationToken, delayMillis);
    }

    /**
     * Helper method to set the state of this metric. Updates the caller if the state changed.
     *
     * @param newState
     */
    protected void setMetricStateAndMaybeNotifyListeners(int newState) {
        if (mMetricState == newState) {
            return;
        }

        mMetricState = newState;
        mCallback.onMetricChanged();
    }

    /** Helper method wrapper around subclass' evaluateMetric function to handle wakelocks */
    private void doEvaluateMetric() {
        try {
            if (mWakeLock != null) {
                mWakeLock.acquire();
            }

            evaluateMetric();
        } finally {
            if (mWakeLock != null) {
                mWakeLock.release();
            }
        }
    }

    /**
     * Perform evaluation of this metric.
     *
     * <p>Subclasses MUST implement this method.
     */
    protected abstract void evaluateMetric();

    protected abstract boolean isApplicable();

    public interface NetworkMetricCallback {
        void onMetricChanged();
    }
}
