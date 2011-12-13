/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.server.sip;

import javax.sip.message.Request;

class SipFeature {
    private static final String TAG = "SipFeature";
    private static final boolean DEBUG = true;

    /* NB: This table should reflect the latest SIP-API implementation. */
    private static final String[] methodTab = {
        Request.ACK,
        Request.BYE,
        Request.CANCEL,
        Request.INVITE,
        Request.OPTIONS,
        Request.REGISTER,
        /* Request.INFO, */
        /* Request.MESSAGE, */
        Request.NOTIFY,
        /* Request.PRACK, */
        /* Request.PUBLISH, */
        Request.REFER,
        /* Request.SUBSCRIBE, */
        /* Request.UPDATE, */
    };

    public static boolean isSupportedMethod(String method) {
        boolean supported = false;
        for (int i = 0, n = methodTab.length; i < n; i++) {
            if (method.equalsIgnoreCase(methodTab[i])) {
                supported = true;
                break;
            }
        }
        return supported;
    }

    public static String getAllowedMethods() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = methodTab.length; i < n; i++) {
            sb.append(methodTab[i]);
            if (i < n-1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
