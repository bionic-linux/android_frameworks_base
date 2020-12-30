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

package android.net;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Snapshot of network state.
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public final class NetworkStateSnapshot implements Parcelable {
    /** The {@link LinkProperties} of the network associated to this snapshot. */
    @NonNull
    public final LinkProperties linkProperties;

    /** The {@link NetworkCapabilities} of the network associated to this snapshot. */
    @NonNull
    public final NetworkCapabilities networkCapabilities;

    /** The network associated to this snapshot. */
    @NonNull
    public final Network network;

    /**
     * The Subscriber Id of the network associated to this snapshot. See
     * {@link android.telephony.TelephonyManager#getSubscriberId()}.
     */
    @Nullable
    public final String subscriberId;

    /**
     * The legacy type of the network associated to this snapshot. See
     * {@code ConnectivityManager#TYPE_*}.
     */
    public final int legacyType;

    public NetworkStateSnapshot(@NonNull LinkProperties linkProperties,
            @NonNull NetworkCapabilities networkCapabilities, @NonNull Network network,
            @Nullable String subscriberId, int legacyType) {
        this.linkProperties = Objects.requireNonNull(linkProperties);
        this.networkCapabilities = Objects.requireNonNull(networkCapabilities);
        this.network = Objects.requireNonNull(network);
        this.subscriberId = subscriberId;
        this.legacyType = legacyType;
    }

    /** @hide */
    public NetworkStateSnapshot(@NonNull Parcel in) {
        linkProperties = in.readParcelable(null);
        networkCapabilities = in.readParcelable(null);
        network = in.readParcelable(null);
        subscriberId = in.readString();
        legacyType = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeParcelable(linkProperties, flags);
        out.writeParcelable(networkCapabilities, flags);
        out.writeParcelable(network, flags);
        out.writeString(subscriberId);
        out.writeInt(legacyType);
    }

    @NonNull
    public static final Creator<NetworkStateSnapshot> CREATOR =
            new Creator<NetworkStateSnapshot>() {
        @NonNull
        @Override
        public NetworkStateSnapshot createFromParcel(@NonNull Parcel in) {
            return new NetworkStateSnapshot(in);
        }

        @NonNull
        @Override
        public NetworkStateSnapshot[] newArray(int size) {
            return new NetworkStateSnapshot[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkStateSnapshot)) return false;
        NetworkStateSnapshot that = (NetworkStateSnapshot) o;
        return legacyType == that.legacyType
                && Objects.equals(linkProperties, that.linkProperties)
                && Objects.equals(networkCapabilities, that.networkCapabilities)
                && Objects.equals(network, that.network)
                && Objects.equals(subscriberId, that.subscriberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkProperties, networkCapabilities, network, subscriberId, legacyType);
    }
}
