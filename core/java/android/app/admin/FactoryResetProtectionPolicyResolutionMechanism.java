/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.app.admin;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/** @hide */
public final class FactoryResetProtectionPolicyResolutionMechanism extends
        ResolutionMechanism<FactoryResetProtectionPolicy> {
    /** @hide */
    @NonNull
    public static final Parcelable.Creator<FactoryResetProtectionPolicyResolutionMechanism>
            CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public FactoryResetProtectionPolicyResolutionMechanism createFromParcel(
                        Parcel source) {
                    return new FactoryResetProtectionPolicyResolutionMechanism();
                }

                @Override
                public FactoryResetProtectionPolicyResolutionMechanism[] newArray(int size) {
                    return new FactoryResetProtectionPolicyResolutionMechanism[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {}
}
