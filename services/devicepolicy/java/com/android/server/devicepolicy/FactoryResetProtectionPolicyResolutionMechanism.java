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

import android.app.admin.FactoryResetProtectionPolicy;
import android.app.admin.PolicyValue;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/** @hide */
public final class FactoryResetProtectionPolicyResolutionMechanism extends
        ResolutionMechanism<FactoryResetProtectionPolicy> {
    @Nullable
    @Override
    PolicyValue<FactoryResetProtectionPolicy> resolve(LinkedHashMap<EnforcingAdmin,
            PolicyValue<FactoryResetProtectionPolicy>> adminPolicies) {
        return resolve(adminPolicies.values().stream().toList());
    }

    @Nullable
    @Override
    PolicyValue<FactoryResetProtectionPolicy> resolve(
            List<PolicyValue<FactoryResetProtectionPolicy>> policies) {
        if (policies.isEmpty()) {
            // No one has set a policy.  Null indicates that the default policy applies, meaning
            // FRP is fully and normally enabled.
            return null;
        }

        boolean frpEnabled = true;
        HashSet<String> adminAccounts = new HashSet<>();
        for (var policy : policies) {
            frpEnabled &= policy.getValue().isFactoryResetProtectionEnabled();
            adminAccounts.addAll(policy.getValue().getFactoryResetProtectionAccounts());
        }

        return new FactoryResetProtectionPolicyValue(
                new FactoryResetProtectionPolicy.Builder()
                        .setFactoryResetProtectionEnabled(frpEnabled)
                        .setFactoryResetProtectionAccounts(adminAccounts.stream().toList())
                        .build());
    }

    @Override
    android.app.admin.ResolutionMechanism<FactoryResetProtectionPolicy>
            getParcelableResolutionMechanism() {
        return new android.app.admin.FactoryResetProtectionPolicyResolutionMechanism();
    }

}



