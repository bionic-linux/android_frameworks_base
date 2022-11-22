/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.media;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.List;

/**
 * Defines the audio HAL version.
 *
 * @hide
 */
@TestApi
public final class AudioHalVersionInfo implements Parcelable, Comparable<AudioHalVersionInfo> {
    /**
     * Indicate the audio HAL is implemented with HIDL (HAL interface definition language).
     *
     * @see <a href="https://source.android.com/docs/core/architecture/hidl/">HIDL</a>
     *     <p>The value of AUDIO_HAL_TYPE_HIDL should match the value of {@link
     *     AudioHalVersion#Type.HIDL}.
     */
    public static final int AUDIO_HAL_TYPE_HIDL = 0;

    /**
     * Indicate the audio HAL is implemented with AIDL (Android Interface Definition Language).
     *
     * @see <a href="https://source.android.com/docs/core/architecture/aidl/">AIDL</a>
     *     <p>The value of AUDIO_HAL_TYPE_HIDL should match the value of {@link
     *     AudioHalVersion#Type.AIDL}.
     */
    public static final int AUDIO_HAL_TYPE_AIDL = 1;

    /** @hide */
    @IntDef(
            flag = false,
            prefix = "AUDIO_HAL_TYPE_",
            value = {AUDIO_HAL_TYPE_HIDL, AUDIO_HAL_TYPE_AIDL})
    public @interface AudioHalType {}

    /**
     * List of all valid Audio HAL versions. This list need to be in sync with sAudioHALVersions
     * defined in frameworks/av/media/libaudiohal/FactoryHal.cpp.
     */
    public static final @NonNull List<List<Integer>> VERSIONS =
            List.of(
                    List.of(AUDIO_HAL_TYPE_AIDL, 1, 0), /* AIDL1.0 */
                    List.of(AUDIO_HAL_TYPE_HIDL, 7, 1), /* HIDL7.1 */
                    List.of(AUDIO_HAL_TYPE_HIDL, 7, 0), /* HIDL7.0 */
                    List.of(AUDIO_HAL_TYPE_HIDL, 6, 0), /* HIDL6.0 */
                    List.of(AUDIO_HAL_TYPE_HIDL, 5, 0), /* HIDL5.0 */
                    List.of(AUDIO_HAL_TYPE_HIDL, 4, 0), /* HIDL4.0 */
                    List.of(AUDIO_HAL_TYPE_HIDL, 2, 0), /* HIDL2.0 */
                    List.of(AUDIO_HAL_TYPE_HIDL, 1, 0) /* HIDL1.0 */);

    /** AudioHalVersionInfo object of all valid Audio HAL versions. */
    public static final @NonNull AudioHalVersionInfo AIDL_1_0 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_AIDL, 1 /* major */, 0 /* minor */);

    public static final @NonNull AudioHalVersionInfo HIDL_7_1 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_HIDL, 7 /* major */, 1 /* minor */);
    public static final @NonNull AudioHalVersionInfo HIDL_7_0 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_HIDL, 7 /* major */, 0 /* minor */);
    public static final @NonNull AudioHalVersionInfo HIDL_6_0 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_HIDL, 6 /* major */, 0 /* minor */);
    public static final @NonNull AudioHalVersionInfo HIDL_5_0 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_HIDL, 5 /* major */, 0 /* minor */);
    public static final @NonNull AudioHalVersionInfo HIDL_4_0 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_HIDL, 4 /* major */, 0 /* minor */);
    public static final @NonNull AudioHalVersionInfo HIDL_2_0 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_HIDL, 2 /* major */, 0 /* minor */);
    public static final @NonNull AudioHalVersionInfo HIDL_1_0 =
            new AudioHalVersionInfo(AUDIO_HAL_TYPE_HIDL, 1 /* major */, 0 /* minor */);

    /** Represent an invalid type/major/minor */
    public static final int INVALID = -1;

    private static final int TUPLE_SIZE = 3;
    private static final int TUPLE_INDEX_TYPE = 0;
    private static final int TUPLE_INDEX_MAJOR = 1;
    private static final int TUPLE_INDEX_MINOR = 2;

    private static final String TAG = "AudioHalVersionInfo";
    private boolean mValid = false;
    private AudioHalVersion mHalVersion = new AudioHalVersion();

    public boolean isValid() {
        return mValid;
    }

    public @AudioHalType int getHalType() {
        return mHalVersion.type;
    }

    public int getMajorVersion() {
        return mHalVersion.major;
    }

    public int getMinorVersion() {
        return mHalVersion.minor;
    }

    /** Get Hal type from an item of VERSIONS */
    public static @AudioHalType int getHalType(@NonNull List<Integer> tuple) {
        if (tuple.size() != TUPLE_SIZE) {
            return INVALID;
        }
        return tuple.get(TUPLE_INDEX_TYPE);
    }
    /** Get Hal major version from an item of VERSIONS */
    public static int getMajorVersion(@NonNull List<Integer> tuple) {
        if (tuple.size() != TUPLE_SIZE) {
            return INVALID;
        }
        return tuple.get(TUPLE_INDEX_MAJOR);
    }
    /** Get Hal minor version from an item of VERSIONS */
    public static int getMinorVersion(@NonNull List<Integer> tuple) {
        if (tuple.size() != TUPLE_SIZE) {
            return INVALID;
        }
        return tuple.get(TUPLE_INDEX_MINOR);
    }

    /** String representative of AudioHalType */
    public static @NonNull String toString(@AudioHalType int type) {
        if (type == AUDIO_HAL_TYPE_HIDL) {
            return "HIDL";
        } else if (type == AUDIO_HAL_TYPE_AIDL) {
            return "AIDL";
        } else {
            return "INVALID";
        }
    }

    /** String representative of AudioHalType, major and minor */
    public static @NonNull String toString(@AudioHalType int type, int major, int minor) {
        return toString(type) + ":" + Integer.toString(major) + "." + Integer.toString(minor);
    }

    /** String representative of this (AudioHalVersionInfo) object */
    @Override
    public String toString() {
        return toString(mHalVersion.type)
                + ":"
                + Integer.toString(mHalVersion.major)
                + "."
                + Integer.toString(mHalVersion.minor);
    }

    public AudioHalVersionInfo(@AudioHalType int type, int major, int minor) {
        mHalVersion.type = type;
        mHalVersion.major = major;
        mHalVersion.minor = minor;

        if (VERSIONS.contains(List.of(type, major, minor))) {
            mValid = true;
        } else {
            Log.w(TAG, "invalid version: " + toString(type) + major + "." + minor);
            mValid = false;
        }
    }

    /**
     * Compare two HAL versions.
     *
     * <p>The greater type represents newer version (for example, AIDL version always newer than
     * HIDL). Within same HAL type, greater major version represents newer version HAL. Within same
     * HAL type and major version, greater minor version represents newer version HAL.
     *
     * @return 0 if the HAL version is the same as the other HAL version. Positive if the HALversion
     *     is newer than the other HAL version. Negative if the HAL version is older than the other
     *     version.
     */
    @Override
    public int compareTo(@NonNull AudioHalVersionInfo other) {
        if (this.mHalVersion.type != other.mHalVersion.type) {
            return this.mHalVersion.type - other.mHalVersion.type;
        }

        return ((this.mHalVersion.major - other.mHalVersion.major) == 0)
                ? (this.mHalVersion.minor - other.mHalVersion.minor)
                : (this.mHalVersion.major - other.mHalVersion.major);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull android.os.Parcel out, int flag) {
        out.writeBoolean(mValid);
        out.writeInt(mHalVersion.type);
        out.writeInt(mHalVersion.major);
        out.writeInt(mHalVersion.minor);
    }

    private AudioHalVersionInfo(Parcel in) {
        mValid = in.readBoolean();
        mHalVersion.type = in.readInt();
        mHalVersion.major = in.readInt();
        mHalVersion.minor = in.readInt();
    }

    public static final @NonNull Parcelable.Creator<AudioHalVersionInfo> CREATOR =
            new Parcelable.Creator<AudioHalVersionInfo>() {
                @Override
                public AudioHalVersionInfo createFromParcel(@NonNull Parcel in) {
                    return new AudioHalVersionInfo(in);
                }

                @Override
                public AudioHalVersionInfo[] newArray(int size) {
                    return new AudioHalVersionInfo[size];
                }
            };
}
