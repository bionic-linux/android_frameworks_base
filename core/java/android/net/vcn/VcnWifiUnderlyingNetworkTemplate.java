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
package android.net.vcn;

import static com.android.internal.annotations.VisibleForTesting.Visibility;
import static com.android.server.vcn.util.PersistableBundleUtils.STRING_DESERIALIZER;
import static com.android.server.vcn.util.PersistableBundleUtils.STRING_SERIALIZER;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.net.NetworkCapabilities;
import android.os.PersistableBundle;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.vcn.util.PersistableBundleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents a configuration for a network template class of underlying Carrier WiFi
 * networks.
 *
 * <p>See {@link VcnUnderlyingNetworkTemplate}
 */
public final class VcnWifiUnderlyingNetworkTemplate extends VcnUnderlyingNetworkTemplate {
    private static final String SSIDS_KEY = "mSsids";
    @Nullable private final Set<String> mSsids;

    private VcnWifiUnderlyingNetworkTemplate(
            int selectedUnderlyingNetworkMatchCriteria,
            int meteredMatchCriteria,
            int minUpstreamBandwidthKbps,
            int minDownstreamBandwidthKbps,
            Set<String> ssids) {
        super(
                NETWORK_PRIORITY_TYPE_WIFI,
                selectedUnderlyingNetworkMatchCriteria,
                meteredMatchCriteria,
                minUpstreamBandwidthKbps,
                minDownstreamBandwidthKbps);
        mSsids = new ArraySet<>(ssids);

        validate();
    }

    /** @hide */
    @Override
    protected void validate() {
        super.validate();
        validateSsids(mSsids);
    }

    private static void validateSsids(Set<String> ssids) {
        Objects.requireNonNull(ssids, "ssids is null");

        for (String ssid : ssids) {
            Objects.requireNonNull(ssid, "found null value ssid");
        }
    }

    /** @hide */
    @NonNull
    @VisibleForTesting(visibility = Visibility.PROTECTED)
    public static VcnWifiUnderlyingNetworkTemplate fromPersistableBundle(
            @NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        final int selectedUnderlyingNetworkMatchCriteria =
                in.getInt(SELECTED_UNDERLYING_NETWORK_MATCH_CRITERIA_KEY);
        final int meteredMatchCriteria = in.getInt(METERED_MATCH_KEY);

        final int minUpstreamBandwidthKbps =
                in.getInt(MIN_UPSTREAM_BANDWIDTH_KBPS_KEY, DEFAULT_MIN_UPSTREAM_BANDWIDTH_KBPS);
        final int minDownstreamBandwidthKbps =
                in.getInt(MIN_DOWNSTREAM_BANDWIDTH_KBPS_KEY, DEFAULT_MIN_DOWNSTREAM_BANDWIDTH_KBPS);

        final PersistableBundle ssidsBundle = in.getPersistableBundle(SSIDS_KEY);
        Objects.requireNonNull(ssidsBundle, "ssidsBundle is null");
        final Set<String> ssids =
                new ArraySet<String>(
                        PersistableBundleUtils.toList(ssidsBundle, STRING_DESERIALIZER));
        return new VcnWifiUnderlyingNetworkTemplate(
                selectedUnderlyingNetworkMatchCriteria,
                meteredMatchCriteria,
                minUpstreamBandwidthKbps,
                minDownstreamBandwidthKbps,
                ssids);
    }

    /** @hide */
    @Override
    @NonNull
    @VisibleForTesting(visibility = Visibility.PROTECTED)
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = super.toPersistableBundle();

        final PersistableBundle ssidsBundle =
                PersistableBundleUtils.fromList(new ArrayList<>(mSsids), STRING_SERIALIZER);
        result.putPersistableBundle(SSIDS_KEY, ssidsBundle);

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mSsids);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (!(other instanceof VcnWifiUnderlyingNetworkTemplate)) {
            return false;
        }

        final VcnWifiUnderlyingNetworkTemplate rhs = (VcnWifiUnderlyingNetworkTemplate) other;
        return mSsids.equals(rhs.mSsids);
    }

    /** @hide */
    @Override
    void dumpTransportSpecificFields(IndentingPrintWriter pw) {
        pw.println("mSsids: " + mSsids);
    }

    /**
     * Retrieve the matching SSIDs, or an empty set if any SSID is acceptable.
     *
     * @see Builder#setSsids(Set)
     */
    @NonNull
    public Set<String> getSsids() {
        return Collections.unmodifiableSet(mSsids);
    }

    /** This class is used to incrementally build VcnWifiUnderlyingNetworkTemplate objects. */
    public static final class Builder {
        private int mSelectedUnderlyingNetworkMatchCriteria = MATCH_ANY;
        private int mMeteredMatchCriteria = MATCH_ANY;
        @NonNull private final Set<String> mSsids = new ArraySet<>();

        private int mMinUpstreamBandwidthKbps = DEFAULT_MIN_UPSTREAM_BANDWIDTH_KBPS;
        private int mMinDownstreamBandwidthKbps = DEFAULT_MIN_DOWNSTREAM_BANDWIDTH_KBPS;

        /** Construct a Builder object. */
        public Builder() {}

        /**
         * Set the criteria for matching against the VCN's selected underlying network.
         *
         * @param matchCriteria the matching criteria for matching aginst the VCN's selected
         *     underlying network. If set to {@link #REQUIRED}, this template will ONLY match a
         *     VCN's selected underlying network. If set to {@link #FORBIDDEN}, this template will
         *     ONLY match network that are NOT the VCN's selected underlying network. Defaults to
         *     {@link #MATCH_ANY}.
         */
        // The matching getter is defined in the super class. Please see {@link
        // VcnUnderlyingNetworkTemplate#getSelectedUnderlyingNetwork()}
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setSelectedUnderlyingNetwork(@MatchCriteria int matchCriteria) {
            validateMatchCriteria(matchCriteria, "setSelectedUnderlyingNetwork");

            mSelectedUnderlyingNetworkMatchCriteria = matchCriteria;
            return this;
        }

        /**
         * Set the matching criteria for metered networks.
         *
         * @param matchCriteria the matching criteria for metered networks. Defaults to {@link
         *     #MATCH_ANY}.
         * @see NetworkCapabilities#NET_CAPABILITY_NOT_METERED
         */
        // The matching getter is defined in the super class. Please see {@link
        // VcnUnderlyingNetworkTemplate#getMetered()}
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setMetered(@MatchCriteria int matchCriteria) {
            validateMatchCriteria(matchCriteria, "setMetered");

            mMeteredMatchCriteria = matchCriteria;
            return this;
        }

        /**
         * Set the SSIDs with which a network can match this priority rule.
         *
         * @param ssids the matching SSIDs. Network with one of the matching SSIDs can match this
         *     priority rule. Defaults to an empty set, allowing ANY SSID.
         */
        @NonNull
        public Builder setSsids(@NonNull Set<String> ssids) {
            validateSsids(ssids);

            mSsids.clear();
            mSsids.addAll(ssids);
            return this;
        }

        /**
         * Set the minimum acceptable upstream bandwidth allowed by this template.
         *
         * <p>Estimated bandwidth of a network is provided by the transport layer, and reported in
         * {@link NetworkCapabilities}. The provided estimates will be used without modification.
         *
         * @param minUpstreamBandwidthKbps the minimum accepted upstream bandwidth, or {@code 0} to
         *     disable this requirement. Defaults to {@code 0}
         * @return this {@link Builder} instance, for chaining
         */
        @NonNull
        public Builder setMinUpstreamBandwidthKbps(int minUpstreamBandwidthKbps) {
            Preconditions.checkArgument(
                    minUpstreamBandwidthKbps >= 0,
                    "Invalid minUpstreamBandwidthKbps, must be >= 0");
            mMinUpstreamBandwidthKbps = minUpstreamBandwidthKbps;
            return this;
        }

        /**
         * Set the minimum acceptable downstream bandwidth allowed by this template.
         *
         * <p>Estimated bandwidth of a network is provided by the transport layer, and reported in
         * {@link NetworkCapabilities}. The provided estimates will be used without modification.
         *
         * @param minDownstreamBandwidthKbps the minimum accepted downstream bandwidth, or {@code 0}
         *     to disable this requirement. Defaults to {@code 0}
         * @return this {@link Builder} instance, for chaining
         */
        @NonNull
        public Builder setMinDownstreamBandwidthKbps(int minDownstreamBandwidthKbps) {
            Preconditions.checkArgument(
                    minDownstreamBandwidthKbps >= 0,
                    "Invalid minDownstreamBandwidthKbps, must be >= 0");
            mMinDownstreamBandwidthKbps = minDownstreamBandwidthKbps;
            return this;
        }

        /** Build the VcnWifiUnderlyingNetworkTemplate. */
        @NonNull
        public VcnWifiUnderlyingNetworkTemplate build() {
            return new VcnWifiUnderlyingNetworkTemplate(
                    mSelectedUnderlyingNetworkMatchCriteria,
                    mMeteredMatchCriteria,
                    mMinUpstreamBandwidthKbps,
                    mMinDownstreamBandwidthKbps,
                    mSsids);
        }
    }
}
