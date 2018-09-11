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

package android.telephony;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

/**
 * Defines the threshold value of the signal strength.
 * @hide
 */
public class SignalThresholdInfo implements Parcelable {

    /** Received Signal Strength Indication */
    public static final int SIGNAL_RSSI = 1;
    /** Received Signal Code Power */
    public static final int SIGNAL_RSCP = 2;
    /** Reference Signal Received Power */
    public static final int SIGNAL_RSRP = 3;
    /** Reference Signal Received Quality */
    public static final int SIGNAL_RSRQ = 4;
    /** Reference Signal Signal to Noise Ratio */
    public static final int SIGNAL_RSSNR = 5;

    /** @hide */
    @IntDef(prefix = { "SIGNAL_" }, value = {
            SIGNAL_RSSI,
            SIGNAL_RSCP,
            SIGNAL_RSRP,
            SIGNAL_RSRQ,
            SIGNAL_RSSNR
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SignalMeasurementType {}

    @SignalMeasurementType
    private int mSignalMeasurement;

    private int[] mThresholds = null;

    /**
     * Constructor
     *
     * @param signalMeasurement Signal Measurement Type
     * @param thresholds threshold value
     */
    public SignalThresholdInfo(@SignalMeasurementType int signalMeasurement,
            @NonNull int [] thresholds) {
        mSignalMeasurement = signalMeasurement;
        mThresholds = thresholds == null ? null : thresholds.clone();
    }

    public @SignalMeasurementType int getSignalMeasurement() {
        return mSignalMeasurement;
    }

    public int[] getThresholds() {
        return mThresholds == null ? null : mThresholds.clone();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mSignalMeasurement);
        out.writeIntArray(mThresholds);
    }

    private SignalThresholdInfo(Parcel in) {
        mSignalMeasurement = in.readInt();
        mThresholds = in.createIntArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof SignalThresholdInfo)) {
            return false;
        }

        SignalThresholdInfo other = (SignalThresholdInfo) o;
        return mSignalMeasurement == other.mSignalMeasurement
                && Arrays.equals(mThresholds, other.mThresholds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSignalMeasurement, mThresholds);
    }

    public static final @NonNull Parcelable.Creator<SignalThresholdInfo> CREATOR =
            new Parcelable.Creator<SignalThresholdInfo>() {
                @Override
                public SignalThresholdInfo createFromParcel(Parcel in) {
                    return new SignalThresholdInfo(in);
                }

                @Override
                public SignalThresholdInfo[] newArray(int size) {
                    return new SignalThresholdInfo[size];
                }
            };

    @Override
    public String toString() {
        return new StringBuilder("SignalThresholdInfo{")
                .append("mSignalMeasurement=").append(mSignalMeasurement)
                .append("mThresholds=").append(Arrays.toString(mThresholds))
                .append("}").toString();
    }
}
