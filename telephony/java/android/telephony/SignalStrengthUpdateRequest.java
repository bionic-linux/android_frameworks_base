/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Request used to register {@link SignalThresholdInfo} to be notified when the signal strength
 * breach the thresholds.
 */
public final class SignalStrengthUpdateRequest implements Parcelable {

    /**
     * The minimum number of thresholds allowed in the request for each SignalThresholdInfo.
     *
     * @see {@link SignalThresholdInfo#getThresholds()}
     */
    public static final int MINIMUM_NUMBER_OF_THRESHOLDS = 1;

    /**
     * The maximum number of thresholds allowed in the request for each SignalThresholdInfo.
     *
     * @see {@link SignalThresholdInfo#getThresholds()}
     */
    public static final int MAXIMUM_NUMBER_OF_THRESHOLDS = 4;
    public static final @NonNull Parcelable.Creator<SignalStrengthUpdateRequest> CREATOR =
            new Parcelable.Creator<SignalStrengthUpdateRequest>() {

                @Override
                public SignalStrengthUpdateRequest createFromParcel(Parcel source) {
                    return new SignalStrengthUpdateRequest(source);
                }

                @Override
                public SignalStrengthUpdateRequest[] newArray(int size) {
                    return new SignalStrengthUpdateRequest[size];
                }
            };
    /**
     * Collection of SignalMeasurementType and corresponding thresholds for the request.
     */
    private Collection<SignalThresholdInfo> mSignalThresholdInfos;
    /**
     * Whether the system thresholds should be honored when screen is off.
     *
     * System signal thresholds are loaded from carrier config items and mainly are used for UI
     * displaying. By default, they are ignored when screen is off. When setting the value to true,
     * modem will continue reporting signal strength changes over the system signal thresholds even
     * screen is off.
     *
     * This should only set to true by the system caller.
     *
     * @hide
     */
    private boolean mHonorSystemThresholdsWhenScreenOff;

    /** @hide */
    public SignalStrengthUpdateRequest(
            @NonNull Collection<SignalThresholdInfo> signalThresholdInfos,
            boolean honorSystemThresholdsWhenScreenOff) {
        if (!isValid(signalThresholdInfos)) {
            throw new IllegalArgumentException("Invalid collection of SignalThresholdInfo");
        }
        mSignalThresholdInfos = new ArrayList<SignalThresholdInfo>(signalThresholdInfos.size());
        mSignalThresholdInfos.addAll(signalThresholdInfos);
        mHonorSystemThresholdsWhenScreenOff = honorSystemThresholdsWhenScreenOff;
    }

    /**
     * Construct request from a collection of {@link SignalThresholdInfo}.
     *
     * Note that the collection should not be empty size. And in each SignalThresholdInfo, the
     * SignalMeasurementType should be unique and the length of thresholds should between
     * {@link #MINIMUM_NUMBER_OF_THRESHOLDS} and {@link #MAXIMUM_NUMBER_OF_THRESHOLDS}. An
     * IllegalArgumentException will throw otherwise.
     *
     * @param signalThresholdInfos a collection of SignalThresholdInfo which specifies the
     *                             SignalMeasurementType and corresponding thresholds.
     */
    public SignalStrengthUpdateRequest(
            @NonNull Collection<SignalThresholdInfo> signalThresholdInfos) {
        this(signalThresholdInfos, false);
    }

    private SignalStrengthUpdateRequest(Parcel in) {
        int size = in.readInt();
        mSignalThresholdInfos = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mSignalThresholdInfos.add(
                    in.readParcelable(SignalThresholdInfo.class.getClassLoader()));
        }
        mHonorSystemThresholdsWhenScreenOff = in.readBoolean();
    }

    /**
     * @return the collection of {@link SignalThresholdInfo} of the request.
     */
    @NonNull
    public Collection<SignalThresholdInfo> getSignalThresholdInfos() {
        return mSignalThresholdInfos;
    }

    /**
     * @return if honor system thresholds even when screen is off.
     * @hide
     */
    public boolean getHonorSystemThresholdWhenScreenOff() {
        return mHonorSystemThresholdsWhenScreenOff;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mSignalThresholdInfos.size());
        for (SignalThresholdInfo info : mSignalThresholdInfos) {
            dest.writeParcelable(info, flags);
        }
        dest.writeBoolean(mHonorSystemThresholdsWhenScreenOff);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;

        if (!(other instanceof SignalStrengthUpdateRequest)) {
            return false;
        }

        SignalStrengthUpdateRequest request = (SignalStrengthUpdateRequest) other;
        return Arrays.equals(request.getSignalThresholdInfos().toArray(),
                mSignalThresholdInfos.toArray())
                && request.getHonorSystemThresholdWhenScreenOff()
                == mHonorSystemThresholdsWhenScreenOff;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSignalThresholdInfos, mHonorSystemThresholdsWhenScreenOff);
    }

    @Override
    public String toString() {
        return new StringBuilder("SignalStrengthUpdateRequest{")
                .append("mSignalThresholdInfos=")
                .append(Arrays.toString(mSignalThresholdInfos.toArray()))
                .append("mHonorSystemThresholdsWhenScreenOff=")
                .append(mHonorSystemThresholdsWhenScreenOff)
                .append("}").toString();
    }

    private boolean isValid(Collection<SignalThresholdInfo> infos) {
        if (infos == null || infos.size() == 0) {
            return false;
        }

        Set<Integer> signalMeasurementTypes = new HashSet<>(infos.size());
        for (SignalThresholdInfo info : infos) {
            if (signalMeasurementTypes.contains(info.getSignalMeasurement())) {
                return false;
            }
            signalMeasurementTypes.add(info.getSignalMeasurement());

            int[] thresholds = info.getThresholds();
            if (ArrayUtils.isEmpty(thresholds)
                    || thresholds.length > MAXIMUM_NUMBER_OF_THRESHOLDS) {
                return false;
            }
        }

        return true;
    }
}
