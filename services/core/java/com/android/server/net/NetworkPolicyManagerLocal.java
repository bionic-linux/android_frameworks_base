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

package com.android.server.net;

import android.annotation.SystemApi;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.RestrictBackgroundStatus;

/**
 * Interface for in-process calls into
 * {@link android.content.Context#NETWORK_POLICY_SERVICE NetworkPolicyManager system service}.
 *
 * @hide
 */
/** @SystemApi(client = SystemApi.Client.SYSTEM_SERVER) */
public interface NetworkPolicyManagerLocal {
    /**
     * Determines if an UID is subject to metered network restrictions while running in background.
     *
     * @param uid The UID whose status needs to be checked.
     * @return {@link ConnectivityManager#RESTRICT_BACKGROUND_STATUS_DISABLED},
     *         {@link ConnectivityManager##RESTRICT_BACKGROUND_STATUS_ENABLED},
     *         or {@link ConnectivityManager##RESTRICT_BACKGROUND_STATUS_WHITELISTED} to denote
     *         the current status of the UID.
     */
    @RestrictBackgroundStatus int getRestrictBackgroundStatus(int uid);
}
