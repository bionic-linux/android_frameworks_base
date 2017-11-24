/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.telephony;

public class ApnTypeConstants {
    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br/>
     * APN_TYPE_ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    public static final String APN_TYPE_ALL = "*";
    /** APN type for default data traffic */
    public static final String APN_TYPE_DEFAULT = "default";
    /** APN type for MMS traffic */
    public static final String APN_TYPE_MMS = "mms";
    /** APN type for SUPL assisted GPS */
    public static final String APN_TYPE_SUPL = "supl";
    /** APN type for DUN traffic */
    public static final String APN_TYPE_DUN = "dun";
    /** APN type for HiPri traffic */
    public static final String APN_TYPE_HIPRI = "hipri";
    /** APN type for FOTA */
    public static final String APN_TYPE_FOTA = "fota";
    /** APN type for IMS */
    public static final String APN_TYPE_IMS = "ims";
    /** APN type for CBS */
    public static final String APN_TYPE_CBS = "cbs";
    /** APN type for IA Initial Attach APN */
    public static final String APN_TYPE_IA = "ia";
    /** APN type for Emergency PDN. This is not an IA apn, but is used
     * for access to carrier services in an emergency call situation. */
    public static final String APN_TYPE_EMERGENCY = "emergency";
    /** Array of all APN types */
    public static final String[] APN_TYPES = {APN_TYPE_DEFAULT,
            APN_TYPE_MMS,
            APN_TYPE_SUPL,
            APN_TYPE_DUN,
            APN_TYPE_HIPRI,
            APN_TYPE_FOTA,
            APN_TYPE_IMS,
            APN_TYPE_CBS,
            APN_TYPE_IA,
            APN_TYPE_EMERGENCY
    };
}
