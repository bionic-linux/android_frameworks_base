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
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO(bfranz): Write javadoc
 * @hide
 */
public class PackageOverride implements Parcelable {

    @IntDef({
            VALUE_UNDEFINED,
            VALUE_ENABLED,
            VALUE_DISABLED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EvaluatedOverride {
    }

    public static final int VALUE_UNDEFINED = 0;
    public static final int VALUE_ENABLED = 1;
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
     * TODO(bfranz)
     */
    public int evaluate(long versionCode) {
        for (VersionRange range : mRanges) {
            if (range.evaluate(versionCode) != VALUE_UNDEFINED) {
                return range.evaluate(versionCode);
            }
        }
        return VALUE_UNDEFINED;
    }

    /**
     * @hide
     */
    public List<VersionRange> getRangeList() {
        return mRanges;
    }

    /**
     * @hide
     */
    public static class VersionRange implements Parcelable {
        @Nullable
        private final Long mMinVersionCode;
        @Nullable
        private final Long mMaxVersionCode;
        private final boolean mEnabled;

        private VersionRange(
                @Nullable Long minVersionCode,
                @Nullable Long maxVersionCode,
                boolean enabled) {
            this.mMinVersionCode = (minVersionCode != null && minVersionCode != Long.MIN_VALUE)
                    ? minVersionCode : null;
            this.mMaxVersionCode = (maxVersionCode != null && maxVersionCode != Long.MAX_VALUE)
                    ? maxVersionCode : null;
            this.mEnabled = enabled;
        }

        /**
         * TODO(bfranz)
         */
        public int evaluate(long versionCode) {
            if (mMinVersionCode != null && versionCode < mMinVersionCode) {
                return VALUE_UNDEFINED;
            }
            if (mMaxVersionCode != null && versionCode > mMaxVersionCode) {
                return VALUE_UNDEFINED;
            }
            return mEnabled ? VALUE_ENABLED : VALUE_DISABLED;
        }

        public Long getMinVersionCode() {
            return mMinVersionCode;
        }

        public Long getMaxVersionCode() {
            return mMaxVersionCode;
        }

        public boolean getEnabled() {
            return mEnabled;
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
            dest.writeLong(mMinVersionCode != null ? mMinVersionCode : Long.MIN_VALUE);
            dest.writeLong(mMaxVersionCode != null ? mMaxVersionCode : Long.MAX_VALUE);
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
     * TODO(bfranz)
     */
    public static class Builder {
        private List<VersionRange> mRanges = new ArrayList<>();

        /**
         * TODO(bfranz)
         */
        public Builder addForAllVersions(boolean enabled) {
            mRanges.add(new VersionRange(null, null, enabled));
            return this;
        }

        /**
         * TODO(bfranz)
         */
        public Builder addForAllVersionsUntil(long maxVersionCode, boolean enabled) {
            mRanges.add(new VersionRange(null, maxVersionCode, enabled));
            return this;
        }

        /**
         * TODO(bfranz)
         */
        public Builder addForAllVersionsFrom(long minVersionCode, boolean enabled) {
            mRanges.add(new VersionRange(minVersionCode, null, enabled));
            return this;
        }

        /**
         * TODO(bfranz)
         */
        public Builder addForVersionRange(long minVersionCode, long maxVersionCode,
                boolean enabled) {
            mRanges.add(new VersionRange(minVersionCode, maxVersionCode, enabled));
            return this;
        }

        /**
         * TODO(bfranz)
         */
        public PackageOverride build() {
            // TODO(bfranz): Validate non-overlapping
            return new PackageOverride(mRanges);
        }
    };
}
