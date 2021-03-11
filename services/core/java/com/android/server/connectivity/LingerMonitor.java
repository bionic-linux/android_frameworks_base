/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.server.connectivity;

import static android.net.ConnectivityManager.NETID_UNSET;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.MessageUtils;
import com.android.server.connectivity.NetworkNotificationManager.TrackedNetwork;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Class that monitors default network linger events and is used as utility for
 * NetworkNotificationManager to determine whether the user should be notified.
 *
 * TODO: refactor inside NetworkNotificationManager by moving the logic to decide whether a
 * notification should be shown to the corresponding NetworkNotificationManager logic.
 *
 * This class is not thread-safe and all its methods must be called on the
 * NetworkNotificationManager handler thread.
 */
public class LingerMonitor {

    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private static final String TAG = LingerMonitor.class.getSimpleName();

    public static final int DEFAULT_NOTIFICATION_DAILY_LIMIT = 3;
    public static final long DEFAULT_NOTIFICATION_RATE_LIMIT_MILLIS = DateUtils.MINUTE_IN_MILLIS;

    private static final HashMap<String, Integer> TRANSPORT_NAMES = makeTransportToNameMap();
    @VisibleForTesting
    public static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));

    @VisibleForTesting
    public static final int NOTIFY_TYPE_NONE         = 0;
    public static final int NOTIFY_TYPE_NOTIFICATION = 1;
    public static final int NOTIFY_TYPE_TOAST        = 2;

    private static SparseArray<String> sNotifyTypeNames = MessageUtils.findMessageNames(
            new Class[] { LingerMonitor.class }, new String[]{ "NOTIFY_TYPE_" });

    private final Context mContext;
    private final NetworkNotificationManager mNotifier;
    private final int mDailyLimit;
    private final long mRateLimitMillis;

    private long mFirstNotificationMillis;
    private long mLastNotificationMillis;
    private int mNotificationCounter;

    /**
     * Current notifications that NetworkNotificationManager should show. Maps the netId we switched
     * away from to the netId we switched to. */
    private final SparseIntArray mNotifications = new SparseIntArray();

    /** Whether we ever notified that we switched away from a particular network. */
    private final SparseBooleanArray mEverNotified = new SparseBooleanArray();

    public LingerMonitor(Context context, NetworkNotificationManager notifier) {
        this(context, notifier,
                Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.NETWORK_SWITCH_NOTIFICATION_DAILY_LIMIT,
                        LingerMonitor.DEFAULT_NOTIFICATION_DAILY_LIMIT) /* dailyLimit */,
                Settings.Global.getLong(context.getContentResolver(),
                        Settings.Global.NETWORK_SWITCH_NOTIFICATION_RATE_LIMIT_MILLIS,
                        LingerMonitor.DEFAULT_NOTIFICATION_RATE_LIMIT_MILLIS) /* rateLimitMs */);
    }

    public LingerMonitor(Context context, NetworkNotificationManager notifier,
            int dailyLimit, long rateLimitMillis) {
        mContext = context;
        mNotifier = notifier;
        mDailyLimit = dailyLimit;
        mRateLimitMillis = rateLimitMillis;
        // Ensure that (now - mLastNotificationMillis) >= rateLimitMillis at first
        mLastNotificationMillis = -rateLimitMillis;
    }

    private static HashMap<String, Integer> makeTransportToNameMap() {
        SparseArray<String> numberToName = MessageUtils.findMessageNames(
            new Class[] { NetworkCapabilities.class }, new String[]{ "TRANSPORT_" });
        HashMap<String, Integer> nameToNumber = new HashMap<>();
        for (int i = 0; i < numberToName.size(); i++) {
            // MessageUtils will fail to initialize if there are duplicate constant values, so there
            // are no duplicates here.
            nameToNumber.put(numberToName.valueAt(i), numberToName.keyAt(i));
        }
        return nameToNumber;
    }

    private static boolean hasTransport(TrackedNetwork network, int transport) {
        return network.networkCapabilities.hasTransport(transport);
    }

    private int getNotificationSource(Network network) {
        for (int i = 0; i < mNotifications.size(); i++) {
            if (mNotifications.valueAt(i) == network.getNetId()) {
                return mNotifications.keyAt(i);
            }
        }
        return NETID_UNSET;
    }

    private boolean everNotified(TrackedNetwork network) {
        return mEverNotified.get(network.network.getNetId(), false);
    }

    @VisibleForTesting
    public boolean isNotificationEnabled(TrackedNetwork fromNetwork, TrackedNetwork toNetwork) {
        // TODO: Evaluate moving to CarrierConfigManager.
        String[] notifySwitches =
                mContext.getResources().getStringArray(R.array.config_networkNotifySwitches);

        if (VDBG) {
            Log.d(TAG, "Notify on network switches: " + Arrays.toString(notifySwitches));
        }

        for (String notifySwitch : notifySwitches) {
            if (TextUtils.isEmpty(notifySwitch)) continue;
            String[] transports = notifySwitch.split("-", 2);
            if (transports.length != 2) {
                Log.e(TAG, "Invalid network switch notification configuration: " + notifySwitch);
                continue;
            }
            int fromTransport = TRANSPORT_NAMES.get("TRANSPORT_" + transports[0]);
            int toTransport = TRANSPORT_NAMES.get("TRANSPORT_" + transports[1]);
            if (hasTransport(fromNetwork, fromTransport) && hasTransport(toNetwork, toTransport)) {
                return true;
            }
        }

        return false;
    }

    @VisibleForTesting
    protected PendingIntent createNotificationIntent() {
        return PendingIntent.getActivity(
                mContext.createContextAsUser(UserHandle.CURRENT, 0 /* flags */),
                0 /* requestCode */,
                CELLULAR_SETTINGS,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Removes any notification that was put up as a result of switching to nai.
    private void maybeStopNotifying(Network network) {
        int fromNetId = getNotificationSource(network);
        if (fromNetId != NETID_UNSET) {
            mNotifications.delete(fromNetId);
            // Toasts can't be deleted.
        }
    }

    // Notify the user of a network switch using a notification or a toast.
    private void notify(TrackedNetwork fromNetwork, TrackedNetwork toNetwork, boolean forceToast) {
        int notifyType =
                mContext.getResources().getInteger(R.integer.config_networkNotifySwitchType);
        if (notifyType == NOTIFY_TYPE_NOTIFICATION && forceToast) {
            notifyType = NOTIFY_TYPE_TOAST;
        }

        if (VDBG) {
            Log.d(TAG, "Notify type: " + sNotifyTypeNames.get(notifyType, "" + notifyType));
        }

        switch (notifyType) {
            case NOTIFY_TYPE_NONE:
                return;
            case NOTIFY_TYPE_NOTIFICATION:
                mNotifications.put(fromNetwork.network.getNetId(), toNetwork.network.getNetId());
                break;
            case NOTIFY_TYPE_TOAST:
                mNotifier.showToast(fromNetwork, toNetwork);
                break;
            default:
                Log.e(TAG, "Unknown notify type " + notifyType);
                return;
        }

        if (DBG) {
            Log.d(TAG, "Notifying switch from=" + fromNetwork.network
                    + " to=" + toNetwork.network
                    + " type=" + sNotifyTypeNames.get(notifyType, "unknown(" + notifyType + ")"));
        }

        mEverNotified.put(fromNetwork.network.getNetId(), true);
    }

    /**
     * Called by NetworkNotificationManager when the default network is lingered.
     * NetworkNotificationManager should then call {@link #shouldNotify} to determine whether a
     * notification should be shown.
     *
     * Putting up a notification when switching from no network to some network is not supported
     * and as such this method can't be called with a null |fromNai|. It can be called with a
     * null |toNai| if there isn't a default network any more.
     *
     * @param fromNetwork switching from this network
     * @param toNetwork switching to this network
     */
    // The default network changed from fromNai to toNai due to a change in score.
    public void noteLingerDefaultNetwork(
            @NonNull final TrackedNetwork fromNetwork,
            @Nullable final TrackedNetwork toNetwork) {
        if (VDBG) {
            Log.d(TAG, "noteLingerDefaultNetwork from=" + fromNetwork.network
                    + " everValidated=" + fromNetwork.everValidated
                    + " lastValidated=" + fromNetwork.networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    + " to=" + (toNetwork == null ? "null" : toNetwork.network));
        }

        // If we are currently notifying the user because the device switched to fromNai, now that
        // we are switching away from it we should remove the notification. This includes the case
        // where we switch back to toNai because its score improved again (e.g., because it regained
        // Internet access).
        maybeStopNotifying(fromNetwork.network);

        // If the network was simply lost (either because it disconnected or because it stopped
        // being the default with no replacement), then don't show a notification.
        if (null == toNetwork) return;

        // If this network never validated, don't notify. Otherwise, we could do things like:
        //
        // 1. Unvalidated wifi connects.
        // 2. Unvalidated mobile data connects.
        // 3. Cell validates, and we show a notification.
        // or:
        // 1. User connects to wireless printer.
        // 2. User turns on cellular data.
        // 3. We show a notification.
        if (!fromNetwork.everValidated) return;

        // If this network is a captive portal, don't notify. This cannot happen on initial connect
        // to a captive portal, because the everValidated check above will fail. However, it can
        // happen if the captive portal reasserts itself (e.g., because its timeout fires). In that
        // case, as soon as the captive portal reasserts itself, we'll show a sign-in notification.
        // We don't want to overwrite that notification with this one; the user has already been
        // notified, and of the two, the captive portal notification is the more useful one because
        // it allows the user to sign in to the captive portal. In this case, display a toast
        // in addition to the captive portal notification.
        //
        // Note that if the network we switch to is already up when the captive portal reappears,
        // this won't work because NetworkMonitor tells ConnectivityService that the network is
        // unvalidated (causing a switch) before asking it to show the sign in notification. In this
        // case, the toast won't show and we'll only display the sign in notification. This is the
        // best we can do at this time.
        boolean forceToast = fromNetwork.networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);

        // Only show the notification once, in order to avoid irritating the user every time.
        // TODO: should we do this?
        if (everNotified(fromNetwork)) {
            if (VDBG) {
                Log.d(TAG, "Not notifying handover from " + fromNetwork.network
                        + ", already notified");
            }
            return;
        }

        // Only show the notification if we switched away because a network became unvalidated, not
        // because its score changed.
        // TODO: instead of just skipping notification, keep a note of it, and show it if it becomes
        // unvalidated.
        if (fromNetwork.networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return;
        }

        if (!isNotificationEnabled(fromNetwork, toNetwork)) return;

        final long now = SystemClock.elapsedRealtime();
        if (isRateLimited(now) || isAboveDailyLimit(now)) return;

        notify(fromNetwork, toNetwork, forceToast);
    }

    /**
     * Indicates whether a linger notification should be shown for the specified network.
     */
    public boolean shouldNotify(@NonNull Network network) {
        return mNotifications.get(network.getNetId(), -1) != -1;
    }

    public void noteDisconnect(Network network) {
        mNotifications.delete(network.getNetId());
        mEverNotified.delete(network.getNetId());
        maybeStopNotifying(network);
    }

    private boolean isRateLimited(long now) {
        final long millisSinceLast = now - mLastNotificationMillis;
        if (millisSinceLast < mRateLimitMillis) {
            return true;
        }
        mLastNotificationMillis = now;
        return false;
    }

    private boolean isAboveDailyLimit(long now) {
        if (mFirstNotificationMillis == 0) {
            mFirstNotificationMillis = now;
        }
        final long millisSinceFirst = now - mFirstNotificationMillis;
        if (millisSinceFirst > DateUtils.DAY_IN_MILLIS) {
            mNotificationCounter = 0;
            mFirstNotificationMillis = 0;
        }
        if (mNotificationCounter >= mDailyLimit) {
            return true;
        }
        mNotificationCounter++;
        return false;
    }
}
