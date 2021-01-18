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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Object representing the quality of a network as perceived by the user.
 *
 * A NetworkScore object represents the characteristics of a network that affects how good the
 * network is considered for a particular use.
 * @hide
 */
// TODO : @SystemApi when the implementation is complete
public final class NetworkScore implements Parcelable {
    // This will be removed soon. Do *NOT* depend on it for any new code that is not part of
    // a migration.
    private final int mLegacyInt;

    // Agent-managed policies
    // TODO : add them here, starting from 1

    // Bitmask of all the policies applied to this score.
    private final long mPolicy;

    /** @hide */
    NetworkScore(final int legacyInt, final long policy) {
        mLegacyInt = legacyInt;
        mPolicy = policy;
    }

    private NetworkScore(@NonNull final Parcel in) {
        mLegacyInt = in.readInt();
        mPolicy = in.readLong();
    }

    public int getLegacyInt() {
        return mLegacyInt;
    }

    /**
     * @return whether this score has a particular policy.
     */
    @VisibleForTesting
    public boolean hasPolicy(final int policy) {
        return 0 != (mPolicy & (1L << policy));
    }

    @Override
    public String toString() {
        return "Score(" + mLegacyInt + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NetworkScore that = (NetworkScore) o;

        if (mLegacyInt != that.mLegacyInt) return false;
        return mPolicy == that.mPolicy;
    }

    @Override
    public int hashCode() {
        // Policy bits are at the top and the bottom, so by multiplying the legacy int by
        // 256 this is a perfect hash until policy bits get up to bit 8 or down to
        // bit 32 - 6 = 24 (6 because legacyInt max value is 101 which fits in 6 bits).
        // By the time there are this many policy bits, the legacy int will be gone.
        return 256 * mLegacyInt + (int) (mPolicy ^ (mPolicy >>> 32));
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(mLegacyInt);
        dest.writeLong(mPolicy);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull public static final Creator<NetworkScore> CREATOR = new Creator<>() {
        @Override
        @NonNull
        public NetworkScore createFromParcel(@NonNull final Parcel in) {
            return new NetworkScore(in);
        }

        @Override
        @NonNull
        public NetworkScore[] newArray(int size) {
            return new NetworkScore[size];
        }
    };

    /**
     * A builder for NetworkScore.
     */
    public static final class Builder {
        private static final long POLICY_NONE = 0L;
        private static final int INVALID_LEGACY_INT = Integer.MIN_VALUE;
        private int mLegacyInt = INVALID_LEGACY_INT;

        /**
         * Sets the legacy int for this score.
         *
         * Do not rely on this. It will be gone by the time S is released.
         *
         * @param score the legacy int
         * @return this
         */
        @NonNull
        public Builder setLegacyInt(final int score) {
            mLegacyInt = score;
            return this;
        }

        /**
         * Builds this NetworkScore.
         * @return The built NetworkScore object.
         */
        @NonNull
        public NetworkScore build() {
            return new NetworkScore(mLegacyInt, POLICY_NONE);
        }
    }
}
