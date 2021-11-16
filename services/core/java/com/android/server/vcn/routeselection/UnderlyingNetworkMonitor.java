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
import static com.android.server.vcn.routeselection.NetworkPriorityClassifier.PriorityCalculationConfig;

import android.annotation.NonNull;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.util.Comparator;

/** @hide */
public class UnderlyingNetworkMonitor {
    @NonNull private final UnderlyingNetworkRecord.Builder mUnderlyingNetworkRecordBuilder;
    @NonNull private final NetworkPriorityClassifier mNetworkPriorityClassifier;
    @NonNull private final UnderlyingNetworkMonitorCallback mCallback;

    private int mPriority = PRIORITY_ANY;

    public UnderlyingNetworkMonitor(
            @NonNull Network network, UnderlyingNetworkMonitorCallback callback) {
        mUnderlyingNetworkRecordBuilder = new UnderlyingNetworkRecord.Builder(network);
        mNetworkPriorityClassifier = new NetworkPriorityClassifier();
        mCallback = callback;
    }

    public static Comparator<UnderlyingNetworkMonitor> getComparator() {
        return (left, right) -> {
            return Integer.compare(left.mPriority, right.mPriority);
        };
    }

    public void updateCapabilities(
            @NonNull NetworkCapabilities networkCapabilities,
            @NonNull PriorityCalculationConfig priorityCalculationConfig) {
        mUnderlyingNetworkRecordBuilder.setNetworkCapabilities(networkCapabilities);
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            reevaluateNetworks(priorityCalculationConfig);
        }
    }

    public void updateLinkProperties(
            @NonNull LinkProperties linkProperties,
            @NonNull PriorityCalculationConfig priorityCalculationConfig) {
        mUnderlyingNetworkRecordBuilder.setLinkProperties(linkProperties);
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            reevaluateNetworks(priorityCalculationConfig);
        }
    }

    public void updateBlockedStatus(
            boolean isBlocked, @NonNull PriorityCalculationConfig priorityCalculationConfig) {
        mUnderlyingNetworkRecordBuilder.setIsBlocked(isBlocked);
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            reevaluateNetworks(priorityCalculationConfig);
        }
    }

    private void reevaluateNetworks(@NonNull PriorityCalculationConfig priorityCalculationConfig) {
        if (mUnderlyingNetworkRecordBuilder.isValid()) {
            mPriority =
                    mNetworkPriorityClassifier.calculatePriorityClass(
                            mUnderlyingNetworkRecordBuilder.build(), priorityCalculationConfig);
            mCallback.onPriorityChanged();
        }
    }

    public UnderlyingNetworkRecord getUnderlyingNetworkRecord() {
        return mUnderlyingNetworkRecordBuilder.build();
    }

    public void tearDown() {}

    public interface UnderlyingNetworkMonitorCallback {
        public void onPriorityChanged();
    }
}
