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

import android.annotation.RequiresPermission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

/**
 * A Simple Surface for Telephony to notify a loosely-coupled debugger of particular issues.
 *
 * DebugEventReporter allows an optional external logging component to receive events detected by
 * the framework and take action. This log surface is designed to provide maximium flexibility
 * to the receiver of these events. Envisioned use cases of this include notifying a vendor
 * component of: an event that necessitates (timely) log collection on non-AOSP components;
 * notifying a vendor component of a rare event that should prompt further action such as a
 * bug report or user intervention for debug purposes.
 *
 * <p>This surface is not intended to enable a diagnostic monitor, nor is it intended to support
 * streaming logs.
 *
 * @hide
 */
public final class DebugEventReporter {
    private static final String TAG = "DebugEventReporter";

    // Require READ_PRIVILEGED_PHONE_STATE to receive these events
    private static final String DEBUG_PERMISSION =
            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE;

    private static Context sContext = null;

    /*
     * Because this is only supporting system packages, once we find a package, it will be the
     * same package until the next system upgrade. Thus, to save time in processing debug events
     * we can cache this info and skip the resolution process after it's done the first time.
     */
    private static String sDebugPackageName = null;

    private DebugEventReporter() {};

    /**
     * If enabled, build and send an intent to a Debug Service for logging.
     *
     * This method sends the {@link TelephonyManager#DEBUG_EVENT DEBUG_EVENT} broadcast, which is
     * system protected. Invoking this method unless you are the system will result in an error.
     */
    public static void sendEvent(UUID eventId, String description) {
        // Don't lock here since we don't want to lock every time; locking is slow, so it's better
        // to drop a few events in early initialization once in a while.
        if (sContext == null) {
            Rlog.w(TAG, "DebugEventReporter not yet initialized, dropping event=" + eventId);
            return;
        }

        // Even if we are initialized, that doesn't mean that a package name has been found.
        // This is normal in many cases, such as when no debug package is installed on the system,
        // so drop these events silently.
        if (sDebugPackageName == null) return;

        Intent dbgIntent = new Intent(TelephonyManager.ACTION_DEBUG_EVENT);
        dbgIntent.putExtra(TelephonyManager.EXTRA_DEBUG_EVENT_ID, new ParcelUuid(eventId));
        if (description != null) {
            dbgIntent.putExtra(TelephonyManager.EXTRA_DEBUG_EVENT_DESCRIPTION, description);
        }
        dbgIntent.setPackage(sDebugPackageName);
        sContext.sendBroadcast(dbgIntent, DEBUG_PERMISSION);
    }

    /**
     * Initialize the DebugEventReporter with the current context.
     *
     * This method must be invoked before any calls to sendEvent() will succeed.
     */
    @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
    public static void initialize(Context context) {
        // Silently disallow null initialization (or reinitialization).
        if (context == null) return;

        // Ensure that this context has sufficient permissions to send debug events.
        context.enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE,
                "This app does not have privileges to send debug events");

        sContext = context;

        // Check to see if there is a valid debug package; if there are multiple, that's a config
        // error, so just take the first one.
        PackageManager pm = sContext.getPackageManager();
        if (pm == null) return;
        List<ResolveInfo> packages = pm.queryBroadcastReceivers(
                new Intent(TelephonyManager.DEBUG_EVENT), PackageManager.MATCH_SYSTEM_ONLY);
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
            break;
        }
        // Initialization may only be performed once.
    }
}
