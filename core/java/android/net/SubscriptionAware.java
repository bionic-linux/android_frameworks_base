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
 * An abstract interface for NetworkSpecifiers that are subscription-aware to implement
 *
 * <p>This represents any class that has a subscription ID, but is primarily meant to facilitate the
 * matching of non-public NetworkSpecifiers for Networks that are tied to a subscription.
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public interface SubscriptionAware {
    /**
     * Return the subscription ID tracked by the subscription-aware entity
     *
     * @return The subscription ID if exists, else {@link
     *     SubscriptionManager.INVALID_SUBSCRIPTION_ID}.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    int getSubscriptionId();
}
