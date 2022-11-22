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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
// import java.util.Objects;
// import java.util.stream.Collectors;

/** Defines the audio HAL version. */
public final class AudioHalVersionInfo extends AudioHalVersion {
    /**
     * Indicate the audio HAL is implemented with HIDL (HAL interface definition language). Please
     * refer to <a href="https://source.android.com/docs/core/architecture/hidl/">
     *
     * @see AudioManager#getHalVersion
     */
    public static final int AUDIO_HAL_TYPE_HIDL = Type.HIDL;

    /**
     * Indicate the audio HAL is implemented with AIDL (Android Interface Definition Language).
     * Please refer to <a href="https://source.android.com/docs/core/architecture/aidl/">
     *
     * @see AudioManager#getHalVersion
     */
    public static final int AUDIO_HAL_TYPE_AIDL = Type.AIDL;

    /** List of all supported Audio HAL HIDL versions. */
    public static final @NonNull List<Pair<Integer, Integer>> HIDL_VERSIONS =
            new ArrayList<Pair<Integer, Integer>>() {
                {
                    add(Pair.create(7, 1));
                    add(Pair.create(7, 0));
                    add(Pair.create(6, 0));
                    add(Pair.create(5, 0));
                    add(Pair.create(4, 0));
                    add(Pair.create(2, 0));
                }
            };

    /** List of all supported Audio HAL AIDL versions. */
    public static final @NonNull List<Pair<Integer, Integer>> AIDL_VERSIONS =
            new ArrayList<Pair<Integer, Integer>>() {
                {
                    add(Pair.create(1, 0));
                }
            };

    /** @hide */
    @IntDef(
            flag = false,
            prefix = "AUDIO_HAL_TYPE_",
            value = {AUDIO_HAL_TYPE_HIDL, AUDIO_HAL_TYPE_AIDL})
    public @interface AudioHalType {}

    private boolean mValid = false;

    public boolean isValid() {
        return this.mValid;
    }

    /** @hide */
    private void checkValid() {
        Pair<Integer, Integer> version = new Pair<Integer, Integer>(major, minor);
        if ((type == AUDIO_HAL_TYPE_HIDL && HIDL_VERSIONS.contains(version))
                || (type == AUDIO_HAL_TYPE_AIDL && AIDL_VERSIONS.contains(version))) {
            mValid = true;
        } else {
            mValid = false;
        }

        return;
    }

    public AudioHalVersionInfo() {
        type = AUDIO_HAL_TYPE_HIDL;
        major = 0;
        minor = 0;
        mValid = false;
    }

    public AudioHalVersionInfo(@AudioHalType int halType, int halMajor, int halMinor) {
        type = halType;
        major = halMajor;
        minor = halMinor;
        checkValid();
    }
    /**
     * Compare two HAL versions.
     *
     * @return 0 if the HAL version is the same as the other HAL version. Positive if the HALversion
     *     is newer than the other HAL version. Negative if the HAL version is older than the other
     *     version.
     */
    public int compareTo(@NonNull AudioHalVersionInfo other) {
        if (this.type != other.type) {
            return this.type - other.type;
        }

        return ((this.major - other.major) == 0)
                ? (this.minor - other.minor)
                : (this.major - other.major);
    }

    public static final @NonNull Parcelable.Creator<AudioHalVersionInfo> CREATOR =
            new Parcelable.Creator<AudioHalVersionInfo>() {
                @Override
                public AudioHalVersionInfo createFromParcel(@NonNull Parcel in) {
                    AudioHalVersionInfo version = new AudioHalVersionInfo();
                    version.readFromParcel(in);
                    version.checkValid();
                    return version;
                }

                @Override
                public AudioHalVersionInfo[] newArray(int size) {
                    return new AudioHalVersionInfo[size];
                }
            };
}
