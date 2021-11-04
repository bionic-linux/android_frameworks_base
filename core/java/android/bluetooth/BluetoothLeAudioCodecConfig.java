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

package android.bluetooth;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the codec configuration for a Bluetooth LE Audio source device.
 *
 * {@see BluetoothLeAudioCodecConfig}
 *
 * {@hide}
 */
public final class BluetoothLeAudioCodecConfig {
    // Add an entry for each source codec here.

    /** @hide */
    @IntDef(prefix = "SOURCE_CODEC_TYPE_", value = {
            SOURCE_CODEC_TYPE_LC3,
            SOURCE_CODEC_TYPE_MAX,
            SOURCE_CODEC_TYPE_INVALID
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SourceCodecType {};

    @UnsupportedAppUsage
    public static final int SOURCE_CODEC_TYPE_LC3 = 0;

    @UnsupportedAppUsage
    public static final int SOURCE_CODEC_TYPE_MAX = 1;

    @UnsupportedAppUsage
    public static final int SOURCE_CODEC_TYPE_INVALID = 1000 * 1000;

    private final @SourceCodecType int mCodecType;

    @UnsupportedAppUsage
    public BluetoothLeAudioCodecConfig(@SourceCodecType int codecType) {
        mCodecType = codecType;
    }

    @Override
    public String toString() {
        return "{codecName:" + getCodecName() + "}";
    }

    /**
     * Gets the codec type.
     * See {@link android.bluetooth.BluetoothLeAudioCodecConfig#SOURCE_CODEC_TYPE_LC3}.
     *
     * @return the codec type
     */
    @UnsupportedAppUsage
    public @SourceCodecType int getCodecType() {
        return mCodecType;
    }

    /**
     * Gets the codec name.
     *
     * @return the codec name
     */
    public @NonNull String getCodecName() {
        switch (mCodecType) {
            case SOURCE_CODEC_TYPE_LC3:
                return "LC3";
            case SOURCE_CODEC_TYPE_INVALID:
                return "INVALID CODEC";
            default:
                break;
        }
        return "UNKNOWN CODEC(" + mCodecType + ")";
    }
}
