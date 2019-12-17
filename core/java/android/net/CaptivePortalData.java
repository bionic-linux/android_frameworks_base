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
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Metadata sent by captive portals, see https://www.ietf.org/id/draft-ietf-capport-api-03.txt.
 * @hide
 */
@SystemApi
public final class CaptivePortalData implements Parcelable {
    private final long mRefreshTime;
    @Nullable
    private final Uri mUserPortalUrl;
    @Nullable
    private final Uri mVenueInfoUrl;
    private final long mBytesRemaining;
    private final long mSecondsRemaining;
    private final boolean mCaptive;

    private CaptivePortalData(long refreshTime, Uri userPortalUrl, Uri venueInfoUrl,
            long bytesRemaining,
            long secondsRemaining, boolean captive) {
        mRefreshTime = refreshTime;
        mUserPortalUrl = userPortalUrl;
        mVenueInfoUrl = venueInfoUrl;
        mBytesRemaining = bytesRemaining;
        mSecondsRemaining = secondsRemaining;
        mCaptive = captive;
    }

    private CaptivePortalData(Parcel p) {
        this(p.readLong(), p.readParcelable(Uri.class.getClassLoader()),
                p.readParcelable(Uri.class.getClassLoader()), p.readLong(), p.readLong(),
                p.readBoolean());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mRefreshTime);
        dest.writeParcelable(mUserPortalUrl, 0);
        dest.writeParcelable(mVenueInfoUrl, 0);
        dest.writeLong(mBytesRemaining);
        dest.writeLong(mSecondsRemaining);
        dest.writeBoolean(mCaptive);
    }

    /**
     * A builder to create new {@link CaptivePortalData}.
     */
    public static class Builder {
        private long mRefreshTime;
        private Uri mUserPortalUrl;
        private Uri mVenueInfoUrl;
        private long mBytesRemaining = -1;
        private long mSecondsRemaining = -1;
        private boolean mCaptive;

        /**
         * Create an empty builder.
         */
        public Builder() {}

        /**
         * Create a builder copying all data from existing {@link CaptivePortalData}.
         */
        public Builder(@Nullable CaptivePortalData data) {
            if (data == null) return;
            setRefreshTime(data.mRefreshTime)
                    .setUserPortalUrl(data.mUserPortalUrl)
                    .setVenueInfoUrl(data.mVenueInfoUrl)
                    .setBytesRemaining(data.mBytesRemaining)
                    .setSecondsRemaining(data.mSecondsRemaining)
                    .setCaptive(data.mCaptive);
        }

        /**
         * Set the time at which data was last refreshed, as per {@link System#currentTimeMillis()}.
         */
        @NonNull
        public Builder setRefreshTime(long refreshTime) {
            mRefreshTime = refreshTime;
            return this;
        }

        /**
         * Set the URL to be used for users to login to the portal, if captive.
         */
        @NonNull
        public Builder setUserPortalUrl(@Nullable Uri userPortalUrl) {
            mUserPortalUrl = userPortalUrl;
            return this;
        }

        /**
         * Set the URL that can be used by users to view information about the network venue.
         */
        @NonNull
        public Builder setVenueInfoUrl(@Nullable Uri venueInfoUrl) {
            mVenueInfoUrl = venueInfoUrl;
            return this;
        }

        /**
         * Set the number of bytes remaining on the network before the portal closes.
         */
        @NonNull
        public Builder setBytesRemaining(long bytesRemaining) {
            mBytesRemaining = bytesRemaining;
            return this;
        }

        /**
         * Set the number of seconds remaining on the network before the portal closes.
         */
        @NonNull
        public Builder setSecondsRemaining(long secondsRemaining) {
            mSecondsRemaining = secondsRemaining;
            return this;
        }

        /**
         * Set whether the network is captive (portal closed).
         */
        @NonNull
        public Builder setCaptive(boolean captive) {
            mCaptive = captive;
            return this;
        }

        /**
         * Create a new {@link CaptivePortalData}.
         */
        @NonNull
        public CaptivePortalData build() {
            return new CaptivePortalData(mRefreshTime,
                    mUserPortalUrl, mVenueInfoUrl, mBytesRemaining, mSecondsRemaining, mCaptive);
        }
    }

    public long getRefreshTime() {
        return mRefreshTime;
    }

    @Nullable
    public Uri getUserPortalUrl() {
        return mUserPortalUrl;
    }

    @Nullable
    public Uri getVenueInfoUrl() {
        return mVenueInfoUrl;
    }

    @Nullable
    public long getBytesRemaining() {
        return mBytesRemaining;
    }

    @Nullable
    public long getSecondsRemaining() {
        return mSecondsRemaining;
    }

    public boolean isCaptive() {
        return mCaptive;
    }

    @NonNull
    public static final Creator<CaptivePortalData> CREATOR = new Creator<CaptivePortalData>() {
        @Override
        public CaptivePortalData createFromParcel(Parcel source) {
            return new CaptivePortalData(source);
        }

        @Override
        public CaptivePortalData[] newArray(int size) {
            return new CaptivePortalData[size];
        }
    };

    @Override
    public int hashCode() {
        return Objects.hash(mRefreshTime, mUserPortalUrl, mVenueInfoUrl, mBytesRemaining,
                mSecondsRemaining, mCaptive);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CaptivePortalData)) return false;
        final CaptivePortalData other = (CaptivePortalData) obj;
        return mRefreshTime == other.mRefreshTime
                && Objects.equals(mUserPortalUrl, other.mUserPortalUrl)
                && Objects.equals(mVenueInfoUrl, other.mVenueInfoUrl)
                && mBytesRemaining == other.mBytesRemaining
                && mSecondsRemaining == other.mSecondsRemaining
                && mCaptive == other.mCaptive;
    }

    @Override
    public String toString() {
        return "CaptivePortalData {"
                + "refreshTime: " + mRefreshTime
                + ", userPortalUrl: " + mUserPortalUrl
                + ", venueInfoUrl: " + mVenueInfoUrl
                + ", bytesRemaining: " + mBytesRemaining
                + ", secondsRemaining: " + mSecondsRemaining
                + ", captive: " + mCaptive
                + "}";
    }
}
