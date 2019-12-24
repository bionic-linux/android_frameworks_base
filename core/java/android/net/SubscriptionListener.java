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

package android.net;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.net.NetworkPolicyManager.SubscriptionListenerProxy;
import android.telephony.SubscriptionPlan;

/**
 * Base class for Network policy listener.
 * @hide
 */
// This is used when registering/unregistering network policy listener,
// and the naming is consistent from NetworkPolicyManager to NetworkPolicyManagerService
// for many releases. Thus, if renamed as Callback, it would be inconsistent.
@SuppressLint("ListenerInterface")
@SystemApi
public class SubscriptionListener {

    private SubscriptionListenerProxy mListener;

    /**
     * Notify of a new override about a given subscription.
     *
     * @param subId The subscriber this override applies to.
     * @param overrideMask The override mask.
     * @param overrideValue The override value.
     */
    public void onSubscriptionOverride(int subId, int overrideMask, int overrideValue) {}

    /**
     * Nofify of subscription plans change about a given subscription.
     *
     * @param subId The subscriber id that got subscription plans change.
     * @param plans The list of subscription plan.
     */
    public void onSubscriptionPlansChanged(int subId, @NonNull SubscriptionPlan[] plans) {}

    /** @hide */
    public void setListener(SubscriptionListenerProxy listener) {
        mListener = listener;
    }

    /** @hide */
    public SubscriptionListenerProxy getListener() {
        return mListener;
    }
}
