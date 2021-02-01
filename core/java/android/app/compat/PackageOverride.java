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

package android.app.compat;

import android.annotation.IntDef;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * An app compat override applied to a given package and change id pairing.
 *
 * A package override contains a list of version ranges with the desired boolean value of
 * the override for the app in this version range. Ranges can be open ended in either direction.
 * An instance of PackageOverride gets created via {@link Builder} and is immutable once created.
 *
 * @hide
 */
public class PackageOverride implements Parcelable {

    @IntDef({
            VALUE_UNDEFINED,
            VALUE_ENABLED,
            VALUE_DISABLED
    })
    @Retention(RetentionPolicy.SOURCE)
    /** @hide */
    public @interface EvaluatedOverride {
    }

    /**
     * Return value of {@link #evaluate(long)} and {@link #evaluateForAllVersions()} indicating that
     * this PackageOverride does not define the value of the override for the given version.
     * @hide
     */
    public static final int VALUE_UNDEFINED = 0;
    /**
     * Return value of {@link #evaluate(long)} and {@link #evaluateForAllVersions()} indicating that
     * the override evaluates to {@code true} for the given version.
     * @hide
     */
    public static final int VALUE_ENABLED = 1;
    /**
     * Return value of {@link #evaluate(long)} and {@link #evaluateForAllVersions()} indicating that
     * the override evaluates to {@code fakse} for the given version.
     * @hide
     */
    public static final int VALUE_DISABLED = 2;

    private final List<VersionRange> mRanges;

    private PackageOverride(List<VersionRange> ranges) {
        this.mRanges = ranges;
    }

    private PackageOverride(Parcel in) {
        this.mRanges = new ArrayList<>();
        in.readParcelableList(this.mRanges, VersionRange.class.getClassLoader());
    }

    /**
     * Evaluate the override for the given {@code versionCode}. If no override is defined for the
     * specified version code, {@link #VALUE_UNDEFINED} is returned.
     * @hide
     */
    public @EvaluatedOverride int evaluate(long versionCode) {
        for (VersionRange range : mRanges) {
            if (range.evaluate(versionCode) != VALUE_UNDEFINED) {
                return range.evaluate(versionCode);
            }
        }
        return VALUE_UNDEFINED;
    }

    /**
     * Evaluate the override independent of version code, i.e. only return an evaluated value if the
     * same override is defined for all versions, otherwise {@link #VALUE_UNDEFINED} is returned.
     * @hide
     */
    public @EvaluatedOverride int evaluateForAllVersions() {
        if (mRanges.size() == 1) {
            return mRanges.get(0).evaluateForAllVersions();
        }
        return VALUE_UNDEFINED;
    }

    /**
     * Return the raw range list.
     * @hide
     */
    public List<VersionRange> getRangeList() {
        return mRanges;
    }

    @Override
    public String toString() {
        if (mRanges.size() == 1) {
            return mRanges.get(0).toString();
        }
        return mRanges.toString();
    }

    /**
     * Inner class defining a single version range.
     * @hide
     */
    public static class VersionRange implements Parcelable {
        private final long mMinVersionCode;
        private final long mMaxVersionCode;
        private final boolean mEnabled;

        private VersionRange(
                long minVersionCode,
                long maxVersionCode,
                boolean enabled) {
            this.mMinVersionCode = minVersionCode;
            this.mMaxVersionCode = maxVersionCode;
            this.mEnabled = enabled;
        }

        /**
         * Evaluate the override for the given {@code versionCode}. If no override is defined for
         * the specified version code, {@link #VALUE_UNDEFINED} is returned.
         * @hide
         */
        private int evaluate(long versionCode) {
            if (containsVersion(versionCode)) {
                return mEnabled ? VALUE_ENABLED : VALUE_DISABLED;
            }
            return VALUE_UNDEFINED;
        }

        /**
         * Evaluate the override independent of version code, i.e. only return an evaluated value if
         * this range covers all versions, otherwise {@link #VALUE_UNDEFINED} is returned.
         * @hide
         */
        private int evaluateForAllVersions() {
            if (mMinVersionCode == Long.MIN_VALUE && mMaxVersionCode == Long.MAX_VALUE) {
                return mEnabled ? VALUE_ENABLED : VALUE_DISABLED;
            }
            return VALUE_UNDEFINED;
        }

        public long getMinVersionCode() {
            return mMinVersionCode;
        }

        public long getMaxVersionCode() {
            return mMaxVersionCode;
        }

        public boolean getEnabled() {
            return mEnabled;
        }

        private boolean containsVersion(long versionCode) {
            return versionCode >= mMinVersionCode && versionCode <= mMaxVersionCode;
        }

        private boolean isOverlapping(VersionRange range) {
            return range.containsVersion(mMinVersionCode)
                    || range.containsVersion(mMaxVersionCode)
                    || containsVersion(range.getMaxVersionCode())
                    || containsVersion(range.getMinVersionCode());
        }

        @Override
        public String toString() {
            if (mMinVersionCode == Long.MIN_VALUE && mMaxVersionCode == Long.MAX_VALUE) {
                return Boolean.toString(mEnabled);
            }
            return String.format("[%d,%d,%b]", mMinVersionCode, mMaxVersionCode, mEnabled);
        }

        private VersionRange(Parcel in) {
            this(in.readLong(), in.readLong(), in.readBoolean());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mMinVersionCode);
            dest.writeLong(mMaxVersionCode);
            dest.writeBoolean(mEnabled);
        }

        public static final Creator<VersionRange> CREATOR =
                new Creator<VersionRange>() {

                    @Override
                    public VersionRange createFromParcel(Parcel in) {
                        return new VersionRange(in);
                    }

                    @Override
                    public VersionRange[] newArray(int size) {
                        return new VersionRange[size];
                    }
                };
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelableList(mRanges, 0);
    }

    public static final Creator<PackageOverride> CREATOR =
            new Creator<PackageOverride>() {

                @Override
                public PackageOverride createFromParcel(Parcel in) {
                    return new PackageOverride(in);
                }

                @Override
                public PackageOverride[] newArray(int size) {
                    return new PackageOverride[size];
                }
            };

    /**
     * Builder to construct a PackageOverride.
     *
     * Version ranges added, must be non-overlapping.
     */
    public static class Builder {
        private List<VersionRange> mRanges = new ArrayList<>();

        /**
         * Add an override for all versions.
         */
        public Builder addForAllVersions(boolean enabled) {
            mRanges.add(new VersionRange(Long.MIN_VALUE, Long.MAX_VALUE, enabled));
            return this;
        }

        /**
         * Add an override for all versions until (including) {@code maxVersionCode}.
         */
        public Builder addForAllVersionsUntil(long maxVersionCode, boolean enabled) {
            mRanges.add(new VersionRange(Long.MIN_VALUE, maxVersionCode, enabled));
            return this;
        }

        /**
         * Add an override for all versions from (including) {@code minVersionCode}.
         */
        public Builder addForAllVersionsFrom(long minVersionCode, boolean enabled) {
            mRanges.add(new VersionRange(minVersionCode, Long.MAX_VALUE, enabled));
            return this;
        }

        /**
         * Add an override for all versions from (including) {@code minVersionCode} until
         * (including) {@code maxVersionCode}.
         */
        public Builder addForVersionRange(long minVersionCode, long maxVersionCode,
                boolean enabled) {
            mRanges.add(new VersionRange(minVersionCode, maxVersionCode, enabled));
            return this;
        }

        /**
         * Build the {@link PackageOverride}.
         *
         * @throws IllegalArgumentException if the set of version ranges is overlapping.
         */
        public PackageOverride build() {
            enforceNonOverlapping();
            return new PackageOverride(mRanges);
        }

        private void enforceNonOverlapping() {
            for (int i = 0; i < mRanges.size(); i++) {
                for (int j = i + 1; j < mRanges.size(); j++) {
                    if (mRanges.get(i).isOverlapping(mRanges.get(j))) {
                        throw new IllegalArgumentException(
                                "Version ranges must be non-overlapping");
                    }
                }
            }
        }
    };
}
