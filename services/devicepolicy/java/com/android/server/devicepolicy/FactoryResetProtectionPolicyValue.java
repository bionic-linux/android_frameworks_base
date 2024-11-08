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

package com.android.server.devicepolicy;

import android.annotation.FlaggedApi;
import android.app.admin.FactoryResetProtectionPolicy;
import android.app.admin.PolicyValue;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/** @hide */
@FlaggedApi(android.app.admin.flags.Flags.FLAG_DEVICE_POLICY_FACTORY_RESET_PROTECTION)
final class FactoryResetProtectionPolicyValue extends PolicyValue<FactoryResetProtectionPolicy> {
    public static final @NonNull Parcelable.Creator<FactoryResetProtectionPolicy> CREATOR =
            FactoryResetProtectionPolicy.CREATOR;

    FactoryResetProtectionPolicyValue(@NonNull FactoryResetProtectionPolicy value) {
        super(value);
    }

    @Override
    public int describeContents() {
        return getValue().describeContents();
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        getValue().writeToParcel(dest, flags);
    }
}

