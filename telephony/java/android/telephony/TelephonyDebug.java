/*jjjjjjjjjjjjjjj
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

    private static Context sContext = null;

    /* Flag to allow lazy initialization of the debug package info. */
    private static boolean sIsInitialized = false;

    /* Lock to ensure lazy initialization doesn't happen concurrently. */
    private static final Object sLock = new Object();

    /*
     * Because this is only supporting system packages, once we find a package, it will be the
     * same package until the next system upgrade. Thus, to save time in processing debug events
     * we can cache this info and skip the resolution process after it's done the first time.
     */
    private static String sDebugPackageName = null;

    /** If enabled, build and send an intent to a Debug Service for logging */
    public static void sendEvent(UUID eventId, String description) {
        if (android.os.Build.IS_USER && !DBG) return;

        // Don't lock here since we don't want to lock every time; locking is slow.
        if (!sIsInitialized || sContext == null) {
            lazyInitialize();
        }

        if (sDebugPackageName == null) return;

        Intent i = buildDebugIntent(eventId, description);
        i.setPackage(sDebugPackageName);
        sContext.startActivity(i);
    }

    /** Build an intent to send to a Debug Service for logging */
    private static Intent buildDebugIntent(UUID eventId, String description) {
        if (eventId == null) throw new IllegalArgumentException("Missing event ID");

        Intent dbgIntent = new Intent(DEBUG_EVENT);
        dbgIntent.putExtra(EXTRA_EVENT_ID, new ParcelUuid(eventId));
        if (description != null) dbgIntent.putExtra(EXTRA_EVENT_DESCRIPTION, description);
        return dbgIntent;
    }

    /** @hide */
    public static void setContext(Context context) {
        synchronized (sLock) {
            sContext = context;
        }
    }

    private static void lazyInitialize() {
        // Lock here so that we achieve synchronization on the initialize() method.
        synchronized (sLock) {
            if (sIsInitialized || sContext == null) return;
            PackageManager pm = sContext.getPackageManager();
            if (pm == null) return;
            List<ResolveInfo> packages = pm.queryBroadcastReceivers(
                    new Intent(DEBUG_EVENT), PackageManager.MATCH_SYSTEM_ONLY);
            if (packages == null) return;
            for (ResolveInfo r : packages) {
                if (r.activityInfo == null || pm.checkPermission(
                            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
                            r.activityInfo.packageName)
                        != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                sDebugPackageName = r.activityInfo.packageName;
            }
            sIsInitialized = true;
        }
    }

}
