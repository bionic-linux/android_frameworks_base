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

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import static com.android.server.VcnManagementService.LOCAL_LOG;

import android.annotation.NonNull;
import android.net.NetworkCapabilities;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.os.ParcelUuid;
import android.telephony.SubscriptionManager;
import android.util.Slog;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.server.vcn.TelephonySubscriptionTracker.TelephonySubscriptionSnapshot;

import java.util.Set;

/** @hide */
public class NetworkPriorityClassifier {
    @NonNull private static final String TAG = NetworkPriorityClassifier.class.getSimpleName();

    /** Priority for any cellular network for which the subscription is listed as opportunistic */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final int PRIORITY_OPPORTUNISTIC_CELLULAR = 0;

    /** Priority for any WiFi network which is in use, and satisfies the in-use RSSI threshold */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final int PRIORITY_WIFI = 1;

    /** Priority for any standard macro cellular network */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final int PRIORITY_MACRO_CELLULAR = 3;

    /** Priority for any other networks (including unvalidated, etc) */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    static final int PRIORITY_ANY = Integer.MAX_VALUE;

    private static final SparseArray<String> PRIORITY_TO_STRING_MAP = new SparseArray<>();

    static {
        PRIORITY_TO_STRING_MAP.put(
                PRIORITY_OPPORTUNISTIC_CELLULAR, "PRIORITY_OPPORTUNISTIC_CELLULAR");
        PRIORITY_TO_STRING_MAP.put(PRIORITY_WIFI, "PRIORITY_WIFI");
        PRIORITY_TO_STRING_MAP.put(PRIORITY_MACRO_CELLULAR, "PRIORITY_MACRO_CELLULAR");
        PRIORITY_TO_STRING_MAP.put(PRIORITY_ANY, "PRIORITY_ANY");
    }

    @NonNull private final ParcelUuid mSubscriptionGroup;
    @NonNull private final VcnGatewayConnectionConfig mConnectionConfig;
    @NonNull private TelephonySubscriptionSnapshot mLastSnapshot;

    public NetworkPriorityClassifier(
            @NonNull ParcelUuid subscriptionGroup,
            @NonNull VcnGatewayConnectionConfig connectionConfig,
            @NonNull TelephonySubscriptionSnapshot snapshot) {
        mSubscriptionGroup = subscriptionGroup;
        mConnectionConfig = connectionConfig;
        mLastSnapshot = snapshot;
    }

    public void updateSubscriptionSnapshot(@NonNull TelephonySubscriptionSnapshot newSnapshot) {
        mLastSnapshot = newSnapshot;
    }

    /**
     * Gives networks a priority class, based on the following priorities:
     *
     * <ol>
     *   <li>Opportunistic cellular
     *   <li>Carrier WiFi
     *   <li>Macro cellular
     *   <li>Any others
     * </ol>
     */
    // TODO: Gve priorty index based on the NetworkPriority list in mConnectionConfig
    public int calculatePriorityClass(UnderlyingNetworkRecord networkRecord, boolean isValidated) {
        final NetworkCapabilities caps = networkRecord.networkCapabilities;

        // mRouteSelectionNetworkRequest requires a network be both VALIDATED and NOT_SUSPENDED

        if (networkRecord.isBlocked) {
            logWtf("Network blocked for System Server: " + networkRecord.network);
            return PRIORITY_ANY;
        }

        if (caps.hasTransport(TRANSPORT_CELLULAR)
                && isOpportunistic(mLastSnapshot, caps.getSubscriptionIds())) {
            // If this carrier is the active data provider, ensure that opportunistic is only
            // ever prioritized if it is also the active data subscription. This ensures that
            // if an opportunistic subscription is still in the process of being switched to,
            // or switched away from, the VCN does not attempt to continue using it against the
            // decision made at the telephony layer. Failure to do so may result in the modem
            // switching back and forth.
            //
            // Allow the following two cases:
            // 1. Active subId is NOT in the group that this VCN is supporting
            // 2. This opportunistic subscription is for the active subId
            if (!mLastSnapshot
                            .getAllSubIdsInGroup(mSubscriptionGroup)
                            .contains(SubscriptionManager.getActiveDataSubscriptionId())
                    || caps.getSubscriptionIds()
                            .contains(SubscriptionManager.getActiveDataSubscriptionId())) {
                return PRIORITY_OPPORTUNISTIC_CELLULAR;
            }
        }

        if (caps.hasTransport(TRANSPORT_WIFI) && isValidated) {
            return PRIORITY_WIFI;
        }

        // Disallow opportunistic subscriptions from matching PRIORITY_MACRO_CELLULAR, as might
        // be the case when Default Data SubId (CBRS) != Active Data SubId (MACRO), as might be
        // the case if the Default Data SubId does not support certain services (eg voice
        // calling)
        if (caps.hasTransport(TRANSPORT_CELLULAR)
                && !isOpportunistic(mLastSnapshot, caps.getSubscriptionIds())) {
            return PRIORITY_MACRO_CELLULAR;
        }

        return PRIORITY_ANY;
    }

    public static boolean isOpportunistic(
            @NonNull TelephonySubscriptionSnapshot snapshot, Set<Integer> subIds) {
        if (snapshot == null) {
            logWtf("Got null snapshot");
            return false;
        }

        for (int subId : subIds) {
            if (snapshot.isOpportunistic(subId)) {
                return true;
            }
        }

        return false;
    }

    public static String priorityClassToString(int priorityClass) {
        return PRIORITY_TO_STRING_MAP.get(priorityClass, "unknown");
    }

    private static void logWtf(String msg) {
        Slog.wtf(TAG, msg);
        LOCAL_LOG.log(TAG + " WTF: " + msg);
    }
}
