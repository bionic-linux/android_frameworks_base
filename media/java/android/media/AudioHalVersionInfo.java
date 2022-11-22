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
import android.annotation.Nullable;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import java.util.List;
// import com.google.common.collect.ImmutableList;;

/**
 * Defines the audio HAL version.
 *
 * @hide
 */
@TestApi
public final class AudioHalVersionInfo implements Parcelable, Comparable<AudioHalVersionInfo> {
    /**
     * Indicate the audio HAL is implemented with HIDL (HAL interface definition language). Please
     * refer to <a href="https://source.android.com/docs/core/architecture/hidl/">
     *
     * <p>The value of AUDIO_HAL_TYPE_HIDL should match the value of @see AudioHalVersion.Type.HIDL.
     */
    public static final int AUDIO_HAL_TYPE_HIDL = 0;

    /**
     * Indicate the audio HAL is implemented with AIDL (Android Interface Definition Language).
     * Please refer to <a href="https://source.android.com/docs/core/architecture/aidl/">
     *
     * <p>The value of AUDIO_HAL_TYPE_AIDL should match the value of @see AudioHalVersion.Type.AIDL.
     */
    public static final int AUDIO_HAL_TYPE_AIDL = 1;

    /** List of all supported Audio HAL HIDL versions. */
    public static final @NonNull List<Pair<Integer, Integer>> HIDL_VERSIONS =
            List.of(
                    Pair.create(7, 1),
                    Pair.create(7, 0),
                    Pair.create(6, 0),
                    Pair.create(5, 0),
                    Pair.create(4, 0),
                    Pair.create(2, 0));

    /** List of all supported Audio HAL AIDL versions. */
    public static final @NonNull List<Pair<Integer, Integer>> AIDL_VERSIONS =
            List.of(Pair.create(1, 0));

    /** @hide */
    @IntDef(
            flag = false,
            prefix = "AUDIO_HAL_TYPE_",
            value = {AUDIO_HAL_TYPE_HIDL, AUDIO_HAL_TYPE_AIDL})
    public @interface AudioHalType {}

    private boolean mValid = false;
    private AudioHalVersion mHalVersion;
/*
    private @Nullable String typeToString(@AudioHalType int type) {
        if (type == AUDIO_HAL_TYPE_HIDL) {
            return "HIDL";
        } else if (type == AUDIO_HAL_TYPE_AIDL) {
            return "AIDL";
        } else {
            return null;
        }
    }
 */
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

    @Override
    public @Nullable String toString() {
        return mValid ? mHalVersion.toString() : null;
    }

    public AudioHalVersionInfo(@AudioHalType int type, int major, int minor) {
        mHalVersion.type = type;
        mHalVersion.major = major;
        mHalVersion.minor = minor;

        Pair<Integer, Integer> version =
                new Pair<Integer, Integer>(mHalVersion.major, mHalVersion.minor);
        if ((type == AUDIO_HAL_TYPE_HIDL && HIDL_VERSIONS.contains(version))
                || (type == AUDIO_HAL_TYPE_AIDL && AIDL_VERSIONS.contains(version))) {
            mValid = true;
        } else {
            mValid = false;
        }
    }

    /**
     * Compare two HAL versions.
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
    public void writeToParcel(@NonNull android.os.Parcel in, int flag) {
        int startPos = in.dataPosition();
        in.writeBoolean(mValid);
        in.writeParcelable(mHalVersion, flag);
        int endPos = in.dataPosition();
        in.setDataPosition(startPos);
        in.writeInt(endPos - startPos);
        in.setDataPosition(endPos);
    }

    private AudioHalVersionInfo(Parcel in) {
        int startPos = in.dataPosition();
        int inSize = in.readInt();
        try {
            if (in.dataPosition() - startPos >= inSize) {
                return;
            }
            mHalVersion = in.readParcelable(null);
            if (in.dataPosition() - startPos >= inSize) {
                return;
            }
            mValid = in.readBoolean();
        } finally {
            if (startPos > (Integer.MAX_VALUE - inSize)) {
                throw new android.os.BadParcelableException("Overflow in the size of parcelable");
            }
            in.setDataPosition(startPos + inSize);
        }
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
