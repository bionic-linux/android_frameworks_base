/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.ParcelUuid;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.ArraySet;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks a set of Networks underpinning a VcnGatewayConnection.
 *
 * <p>A single UnderlyingNetworkTracker is built to serve a SINGLE VCN Gateway Connection, and MUST
 * be torn down with the VcnGatewayConnection in order to ensure underlying networks are allowed to
 * be reaped.
 *
 * @hide
 */
public class UnderlyingNetworkTracker extends Handler {
    @NonNull private static final String TAG = UnderlyingNetworkTracker.class.getSimpleName();

    @NonNull private final Set<NetworkCallback> mCellBringupCallback = new NetworkBringupCallback();
    @NonNull private final NetworkCallback mWifiBringupCallback = new NetworkBringupCallback();
    @NonNull private final NetworkCallback mRouteSelectionCallback = new NetworkSelectionCallback();

    @NonNull private final Set<Integer> mSubIds = new ArraySet<>();

    @NonNull private final VcnContext mVcnContext;
    @NonNull private final ParcelUuid mSubscriptionGroup;
    @NonNull private final UnderlyingNetworkTrackerCallback mCb;
    @NonNull private final Dependencies mDeps;
    @NonNull private final ConnectivityManager mConnectivityManager;
    @NonNull private final SubscriptionManager mSubscriptionManager;

    @Nullable private UnderlyingNetworkRecord mSelectedUnderlyingNetworkRecord;

    public UnderlyingNetworkTracker(
            @NonNull VcnContext vcnContext,
            @NonNull ParcelUuid subscriptionGroup,
            @NonNull UnderlyingNetworkTrackerCallback cb) {
        this(vcnContext, subscriptionGroup, cb, new Dependencies());
    }

    private UnderlyingNetworkTracker(
            @NonNull VcnContext vcnContext,
            @NonNull ParcelUuid subscriptionGroup,
            @NonNull UnderlyingNetworkTrackerCallback cb,
            @NonNull Dependencies deps) {
        super(Objects.requireNonNull(vcnContext, "Missing vcnContext").getLooper());
        mVcnContext = vcnContext;
        mSubscriptionGroup = Objects.requireNonNull(subscriptionGroup, "Missing subscriptionGroup");
        mCb = Objects.requireNonNull(cb, "Missing cb");
        mDeps = Objects.requireNonNull(deps, "Missing deps");
        mConnectivityManager = mVcnContext.getContext().getSystemService(ConnectivityManager.class);
        mSubscriptionManager = mVcnContext.getContext().getSystemService(SubscriptionManager.class);

        updateSubIds();

        registerNetworkRequests();
    }

    private void updateSubIds() {
        mSubIds.clear();

        List<SubscriptionInfo> subInfos = mSubscriptionManager.getSubscriptionsInGroup(mSubscriptionGroup);
        for (SubscriptionInfo subInfo : subInfos) {
            // mSubId.add
        }
    }

    private void registerNetworkRequests() {
        // register bringup requests for underlying Networks
        mConnectivityManager.requestBackgroundNetwork(
                getRequestForTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                mCellBringupCallback);
        mConnectivityManager.requestBackgroundNetwork(
                getRequestForTransport(NetworkCapabilities.TRANSPORT_WIFI), mWifiBringupCallback);

        // register Network-selection request used to decide selected underlying Network
        mConnectivityManager.requestBackgroundNetwork(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)
                        .addUnwantedCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)
                        .build(),
                mRouteSelectionCallback);
    }

    @VisibleForTesting
    static NetworkRequest getRequestForTransport(int transportType) {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)
                .addUnwantedCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)
                .addTransport(transportType)
                .build();
    }

    /**
     * NetworkBringupCallback is a NetworkCallback used to keep background, VCN-managed Networks
     * from being reaped by ConnectivityService.
     *
     * <p>NetworkBringupCallback only exists to prevent matching (VCN-managed) Networks from being
     * reaped, so use the default implementation of NetworkCallback.
     */
    private class NetworkBringupCallback extends NetworkCallback {}

    /**
     * NetworkSelectionCallback is a NetworkCallback used to track the currently selected
     * VCN-managed underlying Network from ConnectivityService.
     */
    private class NetworkSelectionCallback extends NetworkCallback {
        @Override
        public void onAvailable(
                @NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull LinkProperties linkProperties,
                boolean isBlocked) {
            mVcnContext.ensureRunningOnLooperThread();
            mSelectedUnderlyingNetworkRecord =
                    new UnderlyingNetworkRecord(
                            network, networkCapabilities, linkProperties, isBlocked);
            mCb.onSelectedUnderlyingNetworkChanged(mSelectedUnderlyingNetworkRecord);
        }

        @Override
        public void onLost(@NonNull Network network) {
            mVcnContext.ensureRunningOnLooperThread();
            if (!mSelectedUnderlyingNetworkRecord.network.equals(network)) {
                return;
            }

            mSelectedUnderlyingNetworkRecord = null;
            mCb.onSelectedUnderlyingNetworkChanged(mSelectedUnderlyingNetworkRecord);
        }

        @Override
        public void onCapabilitiesChanged(
                @NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            updateRecord(network, networkCapabilities, null, null);
        }

        @Override
        public void onNetworkSuspended(@NonNull Network network) {
            onCapabilitiesChanged(network, getCapsWithSuspended(network, true /* isSuspended */));
        }

        @Override
        public void onNetworkResumed(@NonNull Network network) {
            onCapabilitiesChanged(network, getCapsWithSuspended(network, false /* isSuspended */));
        }

        @Override
        public void onLinkPropertiesChanged(
                @NonNull Network network, @NonNull LinkProperties linkProperties) {
            updateRecord(network, null, linkProperties, null);
        }

        @Override
        public void onBlockedStatusChanged(@NonNull Network network, boolean isBlocked) {
            updateRecord(network, null, null, isBlocked);
        }

        private NetworkCapabilities getCapsWithSuspended(
                @NonNull Network network, boolean isSuspended) {
            mVcnContext.ensureRunningOnLooperThread();

            final NetworkCapabilities newCaps =
                    new NetworkCapabilities(mSelectedUnderlyingNetworkRecord.networkCapabilities);
            if (isSuspended) {
                newCaps.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
            } else {
                newCaps.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
            }
            return newCaps;
        }

        private void updateRecord(
                @NonNull Network network,
                @Nullable NetworkCapabilities networkCapabilities,
                @Nullable LinkProperties linkProperties,
                @Nullable Boolean isBlocked) {
            mVcnContext.ensureRunningOnLooperThread();

            if (!mSelectedUnderlyingNetworkRecord.network.equals(network)) {
                Slog.wtf(TAG, "Received Network update for non-selected Network: " + network);
                return;
            }

            NetworkCapabilities updatedCapabilities =
                    (networkCapabilities != null)
                            ? networkCapabilities
                            : mSelectedUnderlyingNetworkRecord.networkCapabilities;
            LinkProperties updatedProperties =
                    (linkProperties != null)
                            ? linkProperties
                            : mSelectedUnderlyingNetworkRecord.linkProperties;
            boolean updatedIsBlocked =
                    (isBlocked != null) ? isBlocked : mSelectedUnderlyingNetworkRecord.blocked;

            mSelectedUnderlyingNetworkRecord =
                    new UnderlyingNetworkRecord(
                            network, updatedCapabilities, updatedProperties, updatedIsBlocked);
            mCb.onSelectedUnderlyingNetworkChanged(mSelectedUnderlyingNetworkRecord);
        }
    }

    /** Tears down this Tracker, and releases all underlying network requests. */
    public void teardown() {
        mVcnContext.ensureRunningOnLooperThread();

        mConnectivityManager.unregisterNetworkCallback(mCellBringupCallback);
        mConnectivityManager.unregisterNetworkCallback(mWifiBringupCallback);
        mConnectivityManager.unregisterNetworkCallback(mRouteSelectionCallback);
    }

    /** A record of a single underlying network, caching relevant fields. */
    public static class UnderlyingNetworkRecord {
        @NonNull public final Network network;
        @NonNull public final NetworkCapabilities networkCapabilities;
        @NonNull public final LinkProperties linkProperties;
        public final boolean blocked;

        private UnderlyingNetworkRecord(
                @NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull LinkProperties linkProperties,
                boolean blocked) {
            this.network = network;
            this.networkCapabilities = networkCapabilities;
            this.linkProperties = linkProperties;
            this.blocked = blocked;
        }
    }

    /** Callbacks for being notified of the changes in, or to the selected underlying network. */
    public interface UnderlyingNetworkTrackerCallback {
        /**
         * Fired when a new underlying network is selected, or properties have changed.
         *
         * <p>This callback does NOT signal a mobility event.
         *
         * @param underlying The details of the new underlying network
         */
        void onSelectedUnderlyingNetworkChanged(@Nullable UnderlyingNetworkRecord underlying);
    }

    private static class Dependencies {}
}
