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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

/** @hide */
@SystemApi
public final class TelephonyDebug {
    private static final String TAG = "TelephonyDebug";

    private static final boolean DBG = false; // STOPSHIP if true

    // Require READ_PRIVILEGED_PHONE_STATE to receive these events
    private static final String DEBUG_PERMISSION =
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE;

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
     * This field is optional for all events and as a guideline should not exceed 80 characters.
     */
    public static final String EXTRA_EVENT_DESCRIPTION =
            "android.telephony.debug.extra.EVENT_DESCRIPTION";

    private static Context sContext = null;

    /*
     * Because this is only supporting system packages, once we find a package, it will be the
     * same package until the next system upgrade. Thus, to save time in processing debug events
     * we can cache this info and skip the resolution process after it's done the first time.
     */
    private static String sDebugPackageName = null;

    private TelephonyDebug() {};

    /**
     * If enabled, build and send an intent to a Debug Service for logging
     *
     * @hide
     */
    public static void sendEvent(UUID eventId, String description) {
        if (android.os.Build.IS_USER && !DBG) return;

        // Don't lock here since we don't want to lock every time; locking is slow, so it's better
        // to drop a few events in early initialization once in a while.
        if (sContext == null) {
            Rlog.w(TAG, "TelephonyDebug not yet initialized, dropping event=" + eventId);
            return;
        }

        // Even if we are initialized, that doesn't mean that a package name has been found.
        // This is normal in many cases, so drop these events silently.
        if (sDebugPackageName == null) return;

        Intent dbgIntent = new Intent(DEBUG_EVENT);
        dbgIntent.putExtra(EXTRA_EVENT_ID, new ParcelUuid(eventId));
        if (description != null) dbgIntent.putExtra(EXTRA_EVENT_DESCRIPTION, description);
        dbgIntent.setPackage(sDebugPackageName);
        sContext.sendBroadcast(dbgIntent, DEBUG_PERMISSION);
    }

    /** @hide */
    public static void initialize(Context context) {
        // Silently disallow null initialization (or reinitialization).
        if (context == null) return;
        sContext = context;

        // Check to see if there is a valid debug package; if there are multiple, that's a config
        // error, so just take the first one.
        PackageManager pm = sContext.getPackageManager();
        if (pm == null) return;
        List<ResolveInfo> packages = pm.queryBroadcastReceivers(
                new Intent(DEBUG_EVENT), PackageManager.MATCH_SYSTEM_ONLY);
        if (packages == null || packages.isEmpty()) return;
        for (ResolveInfo r : packages) {
            if (r.activityInfo == null
                    || pm.checkPermission(DEBUG_PERMISSION, r.activityInfo.packageName)
                    != PackageManager.PERMISSION_GRANTED) {
                Rlog.w(TAG,
                        "Found package without proper permissions or no activity"
                                + r.activityInfo.packageName);
                continue;
            }
            Rlog.d(TAG, "Found a valid package " + r.activityInfo.packageName);
            sDebugPackageName = r.activityInfo.packageName;
        }
        // Initialization may only be performed once.
    }
}
