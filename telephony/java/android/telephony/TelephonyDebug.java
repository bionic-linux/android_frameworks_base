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

package android.telephony;

import android.annotation.SystemApi;
import android.content.Intent;
import android.os.ParcelUuid;

/** @hide */
@SystemApi
public final class TelephonyDebug {

    private static final boolean DBG = false; // STOPSHIP if true

    /**
     * Intent sent when an error occurs that debug tools should log and possibly take further
     * action such as capturing vendor-specific logs.
     */
    public static final String DEBUG_EVENT = "android.telephony.debug.action.DEBUG_EVENT";

    /**
     * An arbitrary ParcelUuid which should be consistent for each occurrence of the same event.
     *
     * This field must be included in all events.
     */
    public static final String EXTRA_EVENT_ID = "android.telephony.debug.extra.EVENT_ID";

    /**
     * A freeform string description of the event.
     *
     * This field is optional for all events.
     */
    public static final String EXTRA_EVENT_DESCRIPTION =
            "android.telephony.debug.extra.EVENT_DESCRIPTION";

    /**
     * A integer bitmap of requested action to be taken by the receiver of this event.
     *
     * This field should provide a hint as to what type of information should be gathered based
     * on the occurrence of this event.
     *
     * <p>This field is optional for all events.
     */
    public static final String EXTRA_REQUEST = "android.telephony.debug.extra.REQUEST";

    /** Build an intent to send to a TelephonyDebugService */
    public static Intent buildDebugIntent(
            ParcelUuid eventId, String description) {
        if (eventId == null) throw new IllegalArgumentException("Missing event ID");

        Intent dbgIntent = new Intent(DEBUG_EVENT);
        if (description != null) dbgIntent.putExtra(EXTRA_EVENT_DESCRIPTION, description);
        return dbgIntent;
    }

    /** If enabled, build and send an intent to a TelephonyDebugService */
    public static void maybeSendDebugEvent(
            ParcelUuid eventId, String description, int requestAction) {
        if (android.os.Build.IS_USER && !DBG) return;

        Intent i = buildDebugIntent(eventId, description);
        // TODO: send the event
    }
}
