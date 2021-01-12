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
import android.net.TelephonyNetworkSpecifier;
import android.os.Handler;
import android.os.ParcelUuid;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;

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

    @NonNull private final VcnContext mVcnContext;
    @NonNull private final ParcelUuid mSubscriptionGroup;
    @NonNull private final UnderlyingNetworkTrackerCallback mCb;
    @NonNull private final Dependencies mDeps;
    @NonNull private final ConnectivityManager mConnectivityManager;
    @NonNull private final SubscriptionManager mSubscriptionManager;

    @NonNull private final SparseArray<NetworkCallback> mCellBringupCallbacks = new SparseArray<>();
    @NonNull private final NetworkCallback mWifiBringupCallback = new NetworkBringupCallback();
    @NonNull private final NetworkCallback mRouteSelectionCallback = new RouteSelectionCallback();

    @NonNull private final Set<Integer> mSubIds = new ArraySet<>();

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

        registerNetworkRequests();
    }

    private void registerNetworkRequests() {
        // register bringup requests for underlying Networks
        mConnectivityManager.requestBackgroundNetwork(
                getWifiNetworkRequest(), mWifiBringupCallback);
        updateSubIdsAndCellularRequests();

        // register Network-selection request used to decide selected underlying Network
        mConnectivityManager.requestBackgroundNetwork(
                getNetworkRequestBase().build(), mRouteSelectionCallback);
    }

    private NetworkRequest getWifiNetworkRequest() {
        return getNetworkRequestBase().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
    }

    private NetworkRequest getCellNetworkRequestForSubId(int subId) {
        return getNetworkRequestBase()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .setNetworkSpecifier(new TelephonyNetworkSpecifier(subId))
                .build();
    }

    private NetworkRequest.Builder getNetworkRequestBase() {
        return new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)
                .addUnwantedCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED);
    }

    /**
     * Update the current subIds and Cellular bringup requests for this UnderlyingNetworkTracker.
     */
    private void updateSubIdsAndCellularRequests() {
        mVcnContext.ensureRunningOnLooperThread();

        mSubIds.clear();

        // Ensure NetworkRequests filed for all current subIds in mSubscriptionGroup
        List<SubscriptionInfo> subInfos =
                mSubscriptionManager.getSubscriptionsInGroup(mSubscriptionGroup);
        for (SubscriptionInfo subInfo : subInfos) {
            final int subId = subInfo.getSubscriptionId();
            mSubIds.add(subId);

            if (!mCellBringupCallbacks.contains(subId)) {
                final NetworkBringupCallback cb = new NetworkBringupCallback();
                mCellBringupCallbacks.put(subId, cb);

                mConnectivityManager.requestBackgroundNetwork(
                        getCellNetworkRequestForSubId(subId), cb);
            }
        }

        // unregister all NetworkCallbacks for outdated subIds
        for (int i = 0; i < mCellBringupCallbacks.size(); i++) {
            final int subId = mCellBringupCallbacks.keyAt(i);

            if (!mSubIds.contains(subId)) {
                final NetworkCallback cb = mCellBringupCallbacks.removeReturnOld(subId);
                mConnectivityManager.unregisterNetworkCallback(cb);
            }
        }
    }

    /** Tears down this Tracker, and releases all underlying network requests. */
    public void tearDown() {
        mVcnContext.ensureRunningOnLooperThread();

        mConnectivityManager.unregisterNetworkCallback(mWifiBringupCallback);
        mConnectivityManager.unregisterNetworkCallback(mRouteSelectionCallback);

        for (final int subId : mSubIds) {
            final NetworkCallback cb = mCellBringupCallbacks.removeReturnOld(subId);
            mConnectivityManager.unregisterNetworkCallback(cb);
        }
        mSubIds.clear();
    }

    /**
     * NetworkBringupCallback is a NetworkCallback used to keep background, VCN-managed Networks
     * from being reaped by ConnectivityService.
     *
     * <p>NetworkBringupCallback only exists to prevent matching (VCN-managed) Networks from being
     * reaped, so use the default implementation of NetworkCallback.
     */
    @VisibleForTesting
    class NetworkBringupCallback extends NetworkCallback {}

    /**
     * RouteSelectionCallback is a NetworkCallback used to track the currently selected VCN-managed
     * underlying Network from ConnectivityService.
     */
    @VisibleForTesting
    class RouteSelectionCallback extends NetworkCallback {
        @Override
        public void onAvailable(
                @NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull LinkProperties linkProperties,
                boolean isBlocked) {
            mVcnContext.ensureRunningOnLooperThread();

            updateRecordAndNotifyCallback(
                    new UnderlyingNetworkRecord(
                            network, networkCapabilities, linkProperties, isBlocked));
        }

        @Override
        public void onLost(@NonNull Network network) {
            mVcnContext.ensureRunningOnLooperThread();
            if (!mSelectedUnderlyingNetworkRecord.network.equals(network)) {
                return;
            }

            updateRecordAndNotifyCallback(null /* underlyingNetworkRecord */);
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

            if (mSelectedUnderlyingNetworkRecord == null) {
                Slog.wtf(TAG, "Updating UnderlyingNetworkRecord before onAvailable() invoked");
                return;
            }
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

            updateRecordAndNotifyCallback(
                    new UnderlyingNetworkRecord(
                            network, updatedCapabilities, updatedProperties, updatedIsBlocked));
        }

        private void updateRecordAndNotifyCallback(
                @Nullable UnderlyingNetworkRecord underlyingNetworkRecord) {
            // Only forward this update if it represents a change in the underlying Network record
            if (mSelectedUnderlyingNetworkRecord == null
                    || !mSelectedUnderlyingNetworkRecord.equals(underlyingNetworkRecord)) {
                mSelectedUnderlyingNetworkRecord = underlyingNetworkRecord;
                mCb.onSelectedUnderlyingNetworkChanged(underlyingNetworkRecord);
            }
        }
    }

    /** A record of a single underlying network, caching relevant fields. */
    public static class UnderlyingNetworkRecord {
        @NonNull public final Network network;
        @NonNull public final NetworkCapabilities networkCapabilities;
        @NonNull public final LinkProperties linkProperties;
        public final boolean blocked;

        @VisibleForTesting
        UnderlyingNetworkRecord(
                @NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull LinkProperties linkProperties,
                boolean blocked) {
            this.network = network;
            this.networkCapabilities = networkCapabilities;
            this.linkProperties = linkProperties;
            this.blocked = blocked;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UnderlyingNetworkRecord)) return false;
            final UnderlyingNetworkRecord that = (UnderlyingNetworkRecord) o;

            return network.equals(that.network)
                    && networkCapabilities.equals(that.networkCapabilities)
                    && linkProperties.equals(that.linkProperties)
                    && blocked == that.blocked;
        }

        @Override
        public int hashCode() {
            return Objects.hash(network, networkCapabilities, linkProperties, blocked);
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
