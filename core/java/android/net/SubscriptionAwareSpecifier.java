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

package android.net;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.SystemApi;

/**
 * An abstract superclass for NetworkSpecifiers that are subscription-aware
 *
 * <p>This facilitates the matching of non-standard NetworkSpecifiers for Networks that are tied to
 * a subscription.
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public abstract class SubscriptionAwareSpecifier extends NetworkSpecifier {
    /**
     * Return the subscription Id of this SubscriptionAwareSpecifier
     *
     * @return The subscription id.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public abstract int getSubscriptionId();
}
