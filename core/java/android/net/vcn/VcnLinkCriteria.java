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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.PersistableBundle;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * A abstract superclass for configuring the minimum link characteristics.
 *
 * <p>Subclasses allow for configuration of parameters that provide an understanding of the
 * performance, reliability or quality of the underlying link in realtime.
 *
 * @see VcnWifiUnderlyingNetworkTemplate.Builder#setLinkCriterion(Set<VcnLinkCriteria>)
 * @see VcnCellUnderlyingNetworkTemplate.Builder#setLinkCriterion(Set<VcnLinkCriteria>)
 */
public abstract class VcnLinkCriteria {
    /** @hide */
    static final int LINK_CRITERIA_TYPE_ESTIMATED_BANDWIDTH = 1;

    private static final String LINK_CRITERIA_TYPE_KEY = "mLinkCriteriaType";
    private final int mLinkCriteriaType;

    private VcnLinkCriteria(int linkCriteriaType) {
        mLinkCriteriaType = linkCriteriaType;
    }

    /** @hide */
    @NonNull
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public static VcnLinkCriteria fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        final int linkCriteriaType = in.getInt(LINK_CRITERIA_TYPE_KEY);
        switch (linkCriteriaType) {
            case LINK_CRITERIA_TYPE_ESTIMATED_BANDWIDTH:
                return new EstimatedBandwidthCriteria(in);
            default:
                throw new IllegalArgumentException("Invalid linkCriteriaType:" + linkCriteriaType);
        }
    }

    /** @hide */
    @NonNull
    PersistableBundle toPersistableBundle() {
        final PersistableBundle result = new PersistableBundle();

        result.putInt(LINK_CRITERIA_TYPE_KEY, mLinkCriteriaType);

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLinkCriteriaType);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof VcnLinkCriteria)) {
            return false;
        }

        final VcnLinkCriteria rhs = (VcnLinkCriteria) other;
        return mLinkCriteriaType == rhs.mLinkCriteriaType;
    }

    /**
     * A configuration class for the minimum required bandwidth required.
     *
     * <p>Estimated bandwidth of a network is provided by the transport layer, and reported in
     * {@link NetworkCapabilities}. The provided estimates will be used without modification.
     *
     * <p>A network will only be consdered as matching this template if BOTH reported the upstream
     * and downstream bandwidth estimates are greater than the minimums configured.
     */
    public static class EstimatedBandwidthCriteria extends VcnLinkCriteria {
        private static final String MIN_UPSTREAM_BANDWIDTH_KBPS_KEY = "minUpstreamBandwidthKbps";

        /**
         * The minimum allowed upstream bandwidth, in Kbps.
         *
         * @see {@link NetworkCapabilities.getLinkUpstreamBandwidthKbps()}
         */
        public final int minUpstreamBandwidthKbps;

        private static final String MIN_DOWNSTREAM_BANDWIDTH_KBPS_KEY =
                "minDownstreamBandwidthKbps";

        /**
         * The minimum allowed downstream bandwidth, in Kbps.
         *
         * @see {@link NetworkCapabilities.getLinkDownstreamBandwidthKbps()}
         */
        public final int minDownstreamBandwidthKbps;

        /**
         * Creates an instance with the configured minium upstream and downstream bandwidth, in
         * Kbps.
         */
        public EstimatedBandwidthCriteria(
                int minUpstreamBandwidthKbps, int minDownstreamBandwidthKbps) {
            super(LINK_CRITERIA_TYPE_ESTIMATED_BANDWIDTH);

            this.minUpstreamBandwidthKbps = minUpstreamBandwidthKbps;
            this.minDownstreamBandwidthKbps = minDownstreamBandwidthKbps;
        }

        /** @hide */
        public EstimatedBandwidthCriteria(@NonNull PersistableBundle in) {
            super(LINK_CRITERIA_TYPE_ESTIMATED_BANDWIDTH);

            this.minUpstreamBandwidthKbps = in.getInt(MIN_UPSTREAM_BANDWIDTH_KBPS_KEY);
            this.minDownstreamBandwidthKbps = in.getInt(MIN_DOWNSTREAM_BANDWIDTH_KBPS_KEY);
        }

        /** @hide */
        @Override
        @NonNull
        @VisibleForTesting(visibility = Visibility.PROTECTED)
        public PersistableBundle toPersistableBundle() {
            final PersistableBundle result = super.toPersistableBundle();

            result.putInt(MIN_UPSTREAM_BANDWIDTH_KBPS_KEY, minUpstreamBandwidthKbps);
            result.putInt(MIN_DOWNSTREAM_BANDWIDTH_KBPS_KEY, minDownstreamBandwidthKbps);

            return result;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(), minUpstreamBandwidthKbps, minDownstreamBandwidthKbps);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (!super.equals(other)) {
                return false;
            }

            if (!(other instanceof EstimatedBandwidthCriteria)) {
                return false;
            }

            final EstimatedBandwidthCriteria rhs = (EstimatedBandwidthCriteria) other;
            return minUpstreamBandwidthKbps == rhs.minUpstreamBandwidthKbps
                    && minDownstreamBandwidthKbps == rhs.minDownstreamBandwidthKbps;
        }
    }
}
