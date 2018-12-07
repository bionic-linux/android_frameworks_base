/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.net.ipmemorystore;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An object representing the answer to a query whether two given L2 networks represent the
 * same L3 network.
 * @hide
 */
public class SameL3NetworkResponse implements Parcelable {
    @IntDef({SAME_NETWORK, DIFFERENT_NETWORK, NETWORK_IS_UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkSameness {}

    /**
     * Both L2 networks represent the same L3 network.
     */
    public static final int SAME_NETWORK = 1;

    /**
     * The two L2 networks represent a different L3 network.
     */
    public static final int DIFFERENT_NETWORK = 2;

    /**
     * At least one of the two L2 networks is unknown, so no decision can be made.
     */
    public static final int NETWORK_IS_UNKNOWN = 3;

    /**
     * The first L2 key specified in the query.
     */
    @NonNull
    public final String l2Key1;

    /**
     * The second L2 key specified in the query.
     */
    @NonNull
    public final String l2Key2;

    /**
     * A confidence value indicating whether the two L2 networks represent the same L3 network.
     *
     * If both L2 networks were known, this value will be between 0.0 and 1.0, with 0.0
     * representing complete confidence that the given L2 networks represent a different
     * L3 network, and 1.0 representing complete confidence that the given L2 networks
     * represent the same L3 network.
     * If at least one of the L2 networks was not known, this value will be outside of the
     * 0.0~1.0 range.
     *
     * Most apps should not be interested in this, and are encouraged to use the collapsing
     * {@link #isSameNetwork()} function below.
     */
    public final float confidence;

    /**
     * @return whether the two L2 networks represent the same L3 network. Either
     *     {@code SAME_NETWORK}, {@code DIFFERENT_NETWORK} or {@code NETWORK_IS_UNKNOWN}.
     */
    @NetworkSameness
    public final int getNetworkSameness() {
        if (confidence > 1.0 || confidence < 0.0) return NETWORK_IS_UNKNOWN;
        return confidence > 0.5 ? SAME_NETWORK : DIFFERENT_NETWORK;
    }

    SameL3NetworkResponse(@NonNull final String l2Key1, @NonNull final String l2Key2,
            final float confidence) {
        this.l2Key1 = l2Key1;
        this.l2Key2 = l2Key2;
        this.confidence = confidence;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeFloat(confidence);
    }

    public static final Creator<SameL3NetworkResponse> CREATOR =
            new Creator<SameL3NetworkResponse>() {
                public SameL3NetworkResponse createFromParcel(@NonNull final Parcel in) {
                    return new SameL3NetworkResponse(in.readString(), in.readString(),
                            in.readFloat());
                }

                @Override
                public SameL3NetworkResponse[] newArray(final int size) {
                    return new SameL3NetworkResponse[size];
                }
            };
}
