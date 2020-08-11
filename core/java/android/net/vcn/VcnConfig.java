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
package android.net.vcn;

import static com.android.internal.annotations.VisibleForTesting.Visibility;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.vcn.util.PersistableBundleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents a configuration for a Virtual Carrier Network.
 *
 * <p>Each {@link VcnGatewayConnectionConfig} instance added represents a connection that will be
 * brought up on demand based on app-requested {@link Network}s.
 *
 * @hide
 */
public final class VcnConfig implements Parcelable {
    @NonNull private static final String TAG = VcnConfig.class.getSimpleName();

    private static final String TUNNEL_CONFIGS_KEY = "mTunnelConfigs";
    @NonNull private final Set<VcnGatewayConnectionConfig> mTunnelConfigs;

    private VcnConfig(@NonNull Set<VcnGatewayConnectionConfig> tunnelConfigs) {
        mTunnelConfigs = Collections.unmodifiableSet(tunnelConfigs);

        validate();
    }

    /**
     * Deserializes a VcnConfig from a PersistableBundle.
     *
     * @hide
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public VcnConfig(@NonNull PersistableBundle in) {
        final PersistableBundle tunnelConfigsBundle = in.getPersistableBundle(TUNNEL_CONFIGS_KEY);
        mTunnelConfigs =
                new ArraySet<>(
                        PersistableBundleUtils.toList(
                                tunnelConfigsBundle, VcnGatewayConnectionConfig::new));

        validate();
    }

    private void validate() {
        Preconditions.checkCollectionNotEmpty(mTunnelConfigs, "tunnelConfigs");
    }

    /**
     * Retrieves the set of configured tunnels.
     *
     * @hide
     */
    @NonNull
    public Set<VcnGatewayConnectionConfig> getTunnelConfigs() {
        return Collections.unmodifiableSet(mTunnelConfigs);
    }

    /**
     * Serializes this object to a PersistableBundle.
     *
     * @hide
     */
    @NonNull
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = new PersistableBundle();

        final PersistableBundle tunnelConfigsBundle =
                PersistableBundleUtils.fromList(
                        new ArrayList<>(mTunnelConfigs),
                        VcnGatewayConnectionConfig::toPersistableBundle);
        result.putPersistableBundle(TUNNEL_CONFIGS_KEY, tunnelConfigsBundle);

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTunnelConfigs);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof VcnConfig)) {
            return false;
        }

        final VcnConfig rhs = (VcnConfig) other;
        return mTunnelConfigs.equals(rhs.mTunnelConfigs);
    }

    // Parcelable methods

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(toPersistableBundle(), flags);
    }

    @NonNull
    public static final Parcelable.Creator<VcnConfig> CREATOR =
            new Parcelable.Creator<VcnConfig>() {
                @NonNull
                public VcnConfig createFromParcel(Parcel in) {
                    return new VcnConfig((PersistableBundle) in.readParcelable(null));
                }

                @NonNull
                public VcnConfig[] newArray(int size) {
                    return new VcnConfig[size];
                }
            };

    /** This class is used to incrementally build {@link VcnConfig} objects. */
    public static class Builder {
        @NonNull private final Set<VcnGatewayConnectionConfig> mTunnelConfigs = new ArraySet<>();

        /**
         * Adds a {@link VcnGatewayConnectionConfig} with the configuration for an individual
         * tunnel.
         *
         * @param tunnelConfig the configuration for an individual tunnel.
         * @return this {@link Builder} instance, for chaining.
         */
        @NonNull
        public Builder addTunnelConfig(@NonNull VcnGatewayConnectionConfig tunnelConfig) {
            Objects.requireNonNull(tunnelConfig, "tunnelConfig was null");

            mTunnelConfigs.add(tunnelConfig);
            return this;
        }

        /**
         * Builds and validates the VcnConfig.
         *
         * @return an immutable VcnConfig instance.
         */
        @NonNull
        public VcnConfig build() {
            return new VcnConfig(mTunnelConfigs);
        }
    }
}
