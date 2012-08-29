/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony;

public class HacIntent {
    private static final String TAG = "HacIntent";

    /** Event for HAC mode change */

    /**
     * Broadcast intent action indicating that the HAC has either been
     * enabled or disabled. An intent extra provides this state as a
     * boolean, where {@code true} means enabled.
     *
     * @see #HAC_ENABLED
     */
    public static final String HAC_ENABLED_CHANGE_ACTION =
            "com.android.telephony.intent.action.HAC_ENABLED_CHANGE";

    /**
     * The lookup key for a boolean that indicates whether HAC mode is enabled
     * or disabled. {@code true} means HAC mode is enabled. Retrieve it with
     * {@link android.content.Intent#getBooleanExtra(String,boolean)}.
     */
    public static final String HAC_ENABLED = "hacEnabled";
}
