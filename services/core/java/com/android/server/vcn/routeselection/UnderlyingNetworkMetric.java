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

    /** Denotes that this metric is not applicable in the current network state. */
    public static final int METRIC_STATE_IGNORED = Integer.MAX_VALUE;

    private final Object mEvaluationToken = new Object();

    private final VcnContext mVcnContext;
    private final Network mNetwork;
    private final int mDefaultState;
    private final NetworkMetricCallback mCallback;

    private final Handler mHandler;
    private final VcnWakeLock mWakeLock;

    private int mMetricState;

    protected UnderlyingNetworkMetric(
            @NonNull VcnContext vcnContext,
            @NonNull Network network,
            int defaultState,
            boolean requiresWakelock,
            @NonNull NetworkMetricCallback listener) {
        mVcnContext = Objects.requireNonNull(vcnContext, "Missing context");
        mNetwork = Objects.requireNonNull(network, "Missing Network");
        mDefaultState = defaultState;
        mCallback = Objects.requireNonNull(listener, "Missing NetworkMetricCallback");

        mHandler = new Handler(mVcnContext.getLooper());
        mWakeLock =
                requiresWakelock
                        ? new VcnWakeLock(
                                mVcnContext.getContext(), PowerManager.PARTIAL_WAKE_LOCK, TAG)
                        : null;

        mMetricState = mDefaultState;
    }

    /** Retrieves the current metric state. */
    public int getMetricState() {
        return mMetricState;
    }

    /**
     * Enables or disables monitoring of this metric.
     *
     * <p>Metrics start in the enabled state. Changes to metric state as a result of this call will
     * NOT trigger callbacks.
     *
     * <p>Upon enabling, the metric state will be reset to the default for the metric type, and a
     * reevaluation will be triggered immediately.
     *
     * <p>Upon disabling, the metric state will be set to METRIC_STATE_IGNORED, and all subsequent
     * evaluation runs will be cancelled.
     *
     * <p>This method is idempotent; calling it multiple times will only trigger state change
     * notifications, revalidation or cancellation upon the first call.
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            enableMetric();
        } else {
            disableMetric();
        }
    }

    private void enableMetric() {
        // Prevent triggering of reevaluation if already enabled.
        if (mMetricState != METRIC_STATE_IGNORED) {
            return;
        }

        // No notification necessary; the UnderlyingNetworkMonitor is supposed to update it's
        // internal state after updating the enabled state of all metrics
        mMetricState = mDefaultState;
        triggerReevaluation();
    }

    private void disableMetric() {
        // No notification necessary; the UnderlyingNetworkMonitor is supposed to update it's
        // internal state after updating the enabled state of all metrics
        mMetricState = METRIC_STATE_IGNORED;
        mHandler.removeCallbacksAndMessages(mEvaluationToken);
    }

    /**
     * Triggers immediate reevaluation of this metric.
     *
     * <p>Metric reevaluation will be performed asynchronously, and a update will be triggered if
     * the state changes.
     */
    public void triggerReevaluation() {
        if (mMetricState == METRIC_STATE_IGNORED) {
            return;
        }

        scheduleEvaluation(0 /* delayMillis */);
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
        if (mMetricState == METRIC_STATE_IGNORED) {
            return;
        }

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

    public interface NetworkMetricCallback {
        void onMetricChanged();
    }
}
