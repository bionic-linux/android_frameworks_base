/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Metadata about a network to be used by the system or privileged applications.
 * @hide
 */
@SystemApi
@TestApi
public final class NetworkMetadata implements Parcelable {

    // Metadata obtained from NetworkAgents
    @Nullable
    private final Uri mCaptivePortalApiUrl;

    // Metadata obtained from NetworkMonitor
    @Nullable
    private final CaptivePortalData mCaptivePortalData;

    /**
     * A builder to create new {@link NetworkMetadata}.
     */
    public static class Builder {
        private Uri mCaptivePortalApiUrl;
        private CaptivePortalData mCaptivePortalData;

        public Builder() {}
        public Builder(@NonNull NetworkMetadata metadata) {
            mCaptivePortalApiUrl = metadata.mCaptivePortalApiUrl;
            mCaptivePortalData = metadata.mCaptivePortalData;
        }

        /**
         * Set the URL where the network {@link CaptivePortalData} can be fetched.
         */
        @NonNull
        public Builder setCaptivePortalApiUrl(@Nullable Uri captivePortalApiUrl) {
            mCaptivePortalApiUrl = captivePortalApiUrl;
            return this;
        }

        /**
         * Set the {@link CaptivePortalData} that was obtained from the network.
         */
        @NonNull
        public Builder setCaptivePortalData(@Nullable CaptivePortalData captivePortalData) {
            mCaptivePortalData = captivePortalData;
            return this;
        }

        /**
         * Create a new {@link NetworkMetadata}.
         */
        @NonNull
        public NetworkMetadata build() {
            return new NetworkMetadata(mCaptivePortalApiUrl, mCaptivePortalData);
        }
    }

    private NetworkMetadata(Uri captivePortalApiUrl, CaptivePortalData captivePortalData) {
        mCaptivePortalApiUrl = captivePortalApiUrl;
        mCaptivePortalData = captivePortalData;
    }

    private NetworkMetadata(Parcel source) {
        this(source.readParcelable(Uri.class.getClassLoader()),
                source.readParcelable(CaptivePortalData.class.getClassLoader()));
    }

    /**
     * Return a new {@link NetworkMetadata} where fields sourced from {@link NetworkAgent} data are
     * updated with values held by the argument.
     * @hide
     */
    public NetworkMetadata mergeNetworkAgentMetadata(@Nullable NetworkMetadata metadata) {
        if (metadata == null) return this;
        return new NetworkMetadata.Builder(this)
                .setCaptivePortalApiUrl(metadata.mCaptivePortalApiUrl)
                .build();
    }

    /**
     * Return a new {@link NetworkMetadata} where fields sourced from NetworkMonitor data are
     * updated with values held by the argument.
     * @hide
     */
    public NetworkMetadata mergeNetworkMonitorMetadata(@Nullable NetworkMetadata metadata) {
        if (metadata == null) return this;
        return new NetworkMetadata.Builder(this)
                .setCaptivePortalData(metadata.getCaptivePortalData())
                .build();
    }

    @Nullable
    public Uri getCaptivePortalApiUrl() {
        return mCaptivePortalApiUrl;
    }

    @Nullable
    public CaptivePortalData getCaptivePortalData() {
        return mCaptivePortalData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mCaptivePortalApiUrl, 0);
        dest.writeParcelable(mCaptivePortalData, 0);
    }

    @NonNull
    public static final Creator<NetworkMetadata> CREATOR = new Creator<NetworkMetadata>() {
        @Override
        public NetworkMetadata createFromParcel(Parcel source) {
            return new NetworkMetadata(source);
        }

        @Override
        public NetworkMetadata[] newArray(int size) {
            return new NetworkMetadata[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NetworkMetadata)) return false;
        final NetworkMetadata other = (NetworkMetadata) o;
        return Objects.equals(mCaptivePortalApiUrl, other.mCaptivePortalApiUrl)
                && Objects.equals(mCaptivePortalData, other.mCaptivePortalData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCaptivePortalApiUrl, mCaptivePortalData);
    }

    @Override
    public String toString() {
        return "NetworkMetadata {"
                + "portalApiUrl: " + mCaptivePortalApiUrl
                + ", captivePortalData: " + mCaptivePortalData
                + "}";
    }
}
