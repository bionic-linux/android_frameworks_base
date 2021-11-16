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

import static com.android.server.vcn.routeselection.NetworkPriorityClassifier.PRIORITY_ANY;
import static com.android.server.vcn.routeselection.metric.UnderlyingNetworkMetric.METRIC_STATE_NA;
import static com.android.server.vcn.routeselection.metric.UnderlyingNetworkMetric.METRIC_STATE_NOT_ACCEPTABLE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.PersistableBundle;

import com.android.server.vcn.TelephonySubscriptionTracker.TelephonySubscriptionSnapshot;
import com.android.server.vcn.VcnContext;

import java.util.Comparator;

/** @hide */
public class UnderlyingNetworkMonitor {
    /** Denotes that all metrics have been validated on the underlying network */
    private static int QUALITY_OK = 0;
    /** Denotes that there remaining metrics have not been validated on the underlying network */
    private static int QUALITY_PENDING = 1;
    /** Denotes that the underlying network has failed an metric validation and is in penalty box */
    private static int QUALITY_IN_PENALTY_BOX = 2;

    @NonNull private final VcnContext mVcnContext;
    @Nullable private PersistableBundle mCarrierConfig;
    @NonNull private final VcnGatewayConnectionConfig mConnectionConfig;
    @NonNull private final UnderlyingNetworkRecord.Builder mUnderlyingNetworkRecordBuilder;
    @NonNull private final UnderlyingNetworkMonitorCallback mCallback;
    @NonNull private final Dependencies mDeps;
    @NonNull private final Handler mHandler;
    @NonNull private final NetworkPriorityClassifier mNetworkPriorityClassifier;

    @NonNull private final Object mPenaltyBoxTimeoutToken = new Object();

    private int mSelectedPotentialPriority = Integer.MAX_VALUE;
    private boolean mIsSelected = false;

    private int mQuality = QUALITY_PENDING;

    // When mQuality is QUALITY_IN_PENALTY_BOX, mPotentialPriority denotes the NetworkPriority
    // assuming network is not validated. Otherwise, mPotentialPriority denotes the NetworkPriority
    // assuming network validated.
    private int mPotentialPriority = PRIORITY_ANY;

    private int mAggregatedLinkMetricState = METRIC_STATE_NA;
    private int mAggregatedActiveProberMetricState = METRIC_STATE_NA;
    private int mAggregatedTrafficFlowMetricState = METRIC_STATE_NA;

    // TODO: Support more than one monitors;
    @NonNull private UnderlyingNetworkLinkMetricAggregator mLinkMetricAggregator;

    public UnderlyingNetworkMonitor(
            @NonNull VcnContext vcnContext,
            @NonNull ParcelUuid subscriptionGroup,
            @NonNull TelephonySubscriptionSnapshot snapshot,
            @Nullable PersistableBundle carrierConfig,
            VcnGatewayConnectionConfig connectionConfig,
            @NonNull Network network,
            UnderlyingNetworkMonitorCallback callback) {
        this(
                vcnContext,
                subscriptionGroup,
                snapshot,
                carrierConfig,
                connectionConfig,
                network,
                callback,
                new Dependencies());
    }

    public UnderlyingNetworkMonitor(
            @NonNull VcnContext vcnContext,
            @NonNull ParcelUuid subscriptionGroup,
            @NonNull TelephonySubscriptionSnapshot snapshot,
            @Nullable PersistableBundle carrierConfig,
            VcnGatewayConnectionConfig connectionConfig,
            @NonNull Network network,
            UnderlyingNetworkMonitorCallback callback,
            @NonNull Dependencies deps) {
        mVcnContext = vcnContext;
        mCarrierConfig = carrierConfig;
        mConnectionConfig = connectionConfig;
        mUnderlyingNetworkRecordBuilder = new UnderlyingNetworkRecord.Builder(network);

        mCallback = callback;
        mDeps = deps;

        mNetworkPriorityClassifier =
                mDeps.newNetworkPriorityClassifier(subscriptionGroup, connectionConfig, snapshot);
        mHandler = new Handler(mVcnContext.getLooper());
    }

    public static class Dependencies {
        public NetworkPriorityClassifier newNetworkPriorityClassifier(
                @NonNull ParcelUuid subscriptionGroup,
                @NonNull VcnGatewayConnectionConfig connectionConfig,
                @NonNull TelephonySubscriptionSnapshot snapshot) {

            return new NetworkPriorityClassifier(subscriptionGroup, connectionConfig, snapshot);
        }
    }

    public static Comparator<UnderlyingNetworkMonitor> getComparator() {
        return (left, right) -> {
            if (left.mQuality == right.mQuality) {
                return Integer.compare(left.mPotentialPriority, right.mPotentialPriority);
            }
            return Integer.compare(left.mQuality, right.mQuality);
        };
    }

    public void updateCapabilities(@NonNull NetworkCapabilities networkCapabilities) {
        mUnderlyingNetworkRecordBuilder.setNetworkCapabilities(networkCapabilities);
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            constructOrUpdateMetricAggregator(mUnderlyingNetworkRecordBuilder.build());
            reevaluateNetworks();
        }
    }

    public void updateLinkProperties(@NonNull LinkProperties linkProperties) {
        mUnderlyingNetworkRecordBuilder.setLinkProperties(linkProperties);
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            constructOrUpdateMetricAggregator(mUnderlyingNetworkRecordBuilder.build());
            reevaluateNetworks();
        }
    }

    public void updateBlockedStatus(boolean isBlocked) {
        mUnderlyingNetworkRecordBuilder.setIsBlocked(isBlocked);
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            constructOrUpdateMetricAggregator(mUnderlyingNetworkRecordBuilder.build());
            reevaluateNetworks();
        }
    }

    private void constructOrUpdateMetricAggregator(UnderlyingNetworkRecord record) {
        if (mLinkMetricAggregator == null) {
            mLinkMetricAggregator =
                    new UnderlyingNetworkLinkMetricAggregator(
                            mVcnContext,
                            record,
                            mConnectionConfig,
                            mCarrierConfig,
                            () -> {
                                onAggregatedLinkMetricChange();
                            });
            mLinkMetricAggregator.setNetworkSelected(mIsSelected);
            mLinkMetricAggregator.setEnabled(true /* enabled */);
        } else {
            mLinkMetricAggregator.setUnderlyingNetworkRecord(record);
        }
    }

    private void onAggregatedLinkMetricChange() {
        if (mAggregatedLinkMetricState == mLinkMetricAggregator.getMetricState()) {
            return;
        }
        mAggregatedLinkMetricState = mLinkMetricAggregator.getMetricState();
        setQualityAndMaybeNotifyListeners();
    }

    // Fired by any metric change
    private void setQualityAndMaybeNotifyListeners() {
        final int quality;

        if (mAggregatedLinkMetricState == METRIC_STATE_NA
                || mAggregatedActiveProberMetricState == METRIC_STATE_NA
                || mAggregatedTrafficFlowMetricState == METRIC_STATE_NA) {
            quality = QUALITY_PENDING;
        } else if (mAggregatedLinkMetricState == METRIC_STATE_NOT_ACCEPTABLE
                || mAggregatedActiveProberMetricState == METRIC_STATE_NOT_ACCEPTABLE
                || mAggregatedTrafficFlowMetricState == METRIC_STATE_NOT_ACCEPTABLE) {
            quality = QUALITY_IN_PENALTY_BOX;
        } else {
            quality = QUALITY_OK;
        }

        if (mQuality == quality) {
            return;
        }

        mQuality = quality;
        if (quality == QUALITY_IN_PENALTY_BOX) {
            penalize(true /* isPenalized */);
        }
        mCallback.onNetworkPriorityOrQualityChanged();
    }

    private void reevaluateNetworks() {
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            boolean assumeValidated = mQuality != QUALITY_IN_PENALTY_BOX;
            int priority =
                    mNetworkPriorityClassifier.calculatePriorityClass(
                            mUnderlyingNetworkRecordBuilder.build(), assumeValidated);
            if (mPotentialPriority == priority) {
                return;
            }

            mCallback.onNetworkPriorityOrQualityChanged();
        }
    }

    public void updateSubscriptionSnapshot(@NonNull TelephonySubscriptionSnapshot newSnapshot) {
        mNetworkPriorityClassifier.updateSubscriptionSnapshot(newSnapshot);
    }

    public void updateSelectedNetwork(int potentialPriority, Network network) {
        mSelectedPotentialPriority = potentialPriority;
        mIsSelected = network == mUnderlyingNetworkRecordBuilder.getNetwork();
        if (mLinkMetricAggregator != null) {
            mLinkMetricAggregator.setNetworkSelected(mIsSelected);
        }
    }

    private long getPenaltyBoxTimeout() {
        // TODO: Wire this up
        return 1000;
    }

    public void penalize(boolean isPenalized) {
        if (isPenalized) {
            mQuality = QUALITY_IN_PENALTY_BOX;
            reevaluateNetworks();

            mLinkMetricAggregator.setEnabled(false /* enabled */);
            // TODO: Disable all other metrics

            mHandler.postDelayed(
                    () -> {
                        penalize(false /* isPenalized */);
                    },
                    mPenaltyBoxTimeoutToken,
                    getPenaltyBoxTimeout());
        } else {
            mQuality = QUALITY_PENDING;
            reevaluateNetworks();
            mLinkMetricAggregator.setEnabled(true /* enabled */);
        }
    }

    public UnderlyingNetworkRecord getUnderlyingNetworkRecord() {
        return mUnderlyingNetworkRecordBuilder.build();
    }

    public void tearDown() {
        if (mLinkMetricAggregator != null) {
            mLinkMetricAggregator.tearDown();
        }
        mHandler.removeCallbacksAndMessages(mPenaltyBoxTimeoutToken);
    }

    public interface UnderlyingNetworkMonitorCallback {
        public void onNetworkPriorityOrQualityChanged();
    }
}
