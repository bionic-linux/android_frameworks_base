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

import static android.net.NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.provider.Settings.Global.NETWORK_AVOID_BAD_WIFI;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.NetworkStack;
import android.net.TelephonyNetworkSpecifier;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.messages.nano.SystemMessageProto.SystemMessage;

import com.google.common.base.Objects;

import java.util.Random;

public class NetworkNotificationManager {

    public static enum NotificationType {
        LOST_INTERNET(SystemMessage.NOTE_NETWORK_LOST_INTERNET),
        NETWORK_SWITCH(SystemMessage.NOTE_NETWORK_SWITCH),
        NO_INTERNET(SystemMessage.NOTE_NETWORK_NO_INTERNET),
        PARTIAL_CONNECTIVITY(SystemMessage.NOTE_NETWORK_PARTIAL_CONNECTIVITY),
        SIGN_IN(SystemMessage.NOTE_NETWORK_SIGN_IN),
        PRIVATE_DNS_BROKEN(SystemMessage.NOTE_NETWORK_PRIVATE_DNS_BROKEN);

        public final int eventId;

        NotificationType(int eventId) {
            this.eventId = eventId;
            Holder.sIdToTypeMap.put(eventId, this);
        }

        private static class Holder {
            private static SparseArray<NotificationType> sIdToTypeMap = new SparseArray<>();
        }

        public static NotificationType getFromId(int id) {
            return Holder.sIdToTypeMap.get(id);
        }
    }

    static class TrackedNetwork {
        @Nullable
        LinkProperties linkProperties;
        @Nullable
        NetworkCapabilities networkCapabilities;
        @Nullable
        NotificationType currentNotification;
        @NonNull
        final Network network;
        @NonNull
        final NetworkAgentConfig networkAgentConfig;

        boolean lastValidated = false;
        boolean everValidated = false;
        boolean everCaptivePortalDetected = false;
        boolean validationTimedOut = false;
        boolean hasShownDnsBroken = false;
        boolean acceptUnvalidated;
        boolean acceptPartial;

        TrackedNetwork(Network network, NetworkAgentConfig config) {
            this.network = network;
            networkAgentConfig = config;
            acceptUnvalidated = config.isUnvalidatedConnectivityAcceptable();
            acceptPartial = config.isPartialConnectivityAcceptable();
        }
    }

    private final ArrayMap<Network, TrackedNetwork> mTrackedNetworks = new ArrayMap<>();

    private static final String TAG = NetworkNotificationManager.class.getSimpleName();
    private static final boolean DBG = true;

    // Notification channels used by ConnectivityService mainline module, it should be aligned with
    // SystemNotificationChannels so the channels are the same as the ones used as the system
    // server.
    public static final String NOTIFICATION_CHANNEL_NETWORK_STATUS = "NETWORK_STATUS";
    public static final String NOTIFICATION_CHANNEL_NETWORK_ALERTS = "NETWORK_ALERTS";

    // The context is for the current user (system server)
    private final Context mContext;
    private final HandlerThread mThread;
    private final TelephonyManager mTelephonyManager;
    private final ConnectivityManager mCm;
    private final LingerMonitor mLingerMonitor;
    private final PortalNotificationIntentReceiver mPortalIntentReceiver;
    private final NotificationNetworkCallback mAllNetworksWatcher;
    private final NotificationDefaultNetworkCallback mDefaultNetworkWatcher;
    // The notification manager is created from a context for User.ALL, so notifications
    // will be sent to all users.
    private final NotificationManager mNotificationManager;
    // Tracks the types of notifications managed by this instance, from creation to cancellation.
    private final SparseIntArray mNotificationTypeMap;

    private Handler mHandler;

    public NetworkNotificationManager(@NonNull final Context c) {
        this(c, new HandlerThread(NetworkNotificationManager.class.getSimpleName()));
    }

    @VisibleForTesting
    public NetworkNotificationManager(@NonNull final Context c, @NonNull HandlerThread thread) {
        mContext = c;
        mTelephonyManager = c.getSystemService(TelephonyManager.class);
        mCm = c.getSystemService(ConnectivityManager.class);
        mAllNetworksWatcher = new NotificationNetworkCallback();
        mDefaultNetworkWatcher = new NotificationDefaultNetworkCallback();
        mLingerMonitor = new LingerMonitor(c, this);
        mPortalIntentReceiver = new PortalNotificationIntentReceiver();
        mThread = thread;
        mNotificationManager =
                (NotificationManager) c.createContextAsUser(UserHandle.ALL, 0 /* flags */)
                        .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationTypeMap = new SparseIntArray();
    }

    public void start() {
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mPortalIntentReceiver.register();
        // TODO: check each notification to adjust the request
        mCm.registerNetworkCallback(new NetworkRequest.Builder().build(),
                new NotificationNetworkCallback(), mHandler);
        mCm.registerDefaultNetworkCallback(new NotificationDefaultNetworkCallback(), mHandler);
    }

    private class NotificationNetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onCapabilitiesChanged(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities) {
            final TrackedNetwork trackedNetwork = mTrackedNetworks.get(network);
            if (trackedNetwork == null) return;
            trackedNetwork.networkCapabilities = networkCapabilities;

            updateNotifications(trackedNetwork);
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull Network network,
                @NonNull LinkProperties linkProperties) {
            final TrackedNetwork trackedNetwork = mTrackedNetworks.get(network);
            if (trackedNetwork == null) return;
            trackedNetwork.linkProperties = linkProperties;

            updateNotifications(trackedNetwork);
        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            // onLost for the default network is sent before onLosing in the network callback, so
            // this should now be the last default network
            if (!network.equals(mDefaultNetworkWatcher.mLastDefaultNetwork)) return;

            final TrackedNetwork lastDefault = mTrackedNetworks.get(network);
            if (lastDefault == null || lastDefault.networkCapabilities == null
                    || lastDefault.linkProperties == null) {
                return;
            }

            final TrackedNetwork newDefault = mDefaultNetworkWatcher.mDefaultNetwork == null
                    ? null
                    : mTrackedNetworks.get(mDefaultNetworkWatcher.mDefaultNetwork);
            if (newDefault != null && (newDefault.linkProperties == null
                    || newDefault.networkCapabilities == null)) {
                return;
            }

            mLingerMonitor.noteLingerDefaultNetwork(lastDefault, newDefault);
            updateNotifications(lastDefault);
        }
    }

    private class NotificationDefaultNetworkCallback extends ConnectivityManager.NetworkCallback {
        @Nullable
        private Network mLastDefaultNetwork;
        @Nullable
        private Network mDefaultNetwork;

        @Override
        public void onAvailable(@NonNull Network network) {
            mDefaultNetwork = network;
        }

        @Override
        public void onLost(@NonNull Network network) {
            // On network switch, onLost is called before onAvailable, which is called before
            // onLosing for the lingered network
            mLastDefaultNetwork = mDefaultNetwork;
            mDefaultNetwork = null;
        }
    }

    // TODO: make module API
    public void notifyNetworkConnected(@NonNull Network network,
            @NonNull NetworkAgentConfig config) {
        final TrackedNetwork oldConfig = mTrackedNetworks.put(network, new TrackedNetwork(network,
                config));
        if (oldConfig != null) {
            Log.wtf(TAG, "Network connected twice: " + network);
        }
    }

    // TODO: make module API
    public void notifyNetworkDisconnected(@NonNull Network network) {
        mLingerMonitor.noteDisconnect(network);
        if (mTrackedNetworks.remove(network) != null) {
            clearNotification(network.getNetId());
        }
    }

    // TODO: make module API
    public void notifyValidationTimedOut(@NonNull Network network) {
        final TrackedNetwork trackedNetwork = mTrackedNetworks.get(network);
        if (trackedNetwork == null) return;
        trackedNetwork.validationTimedOut = true;

        updateNotifications(trackedNetwork);
    }

    // TODO: make module API
    public void notifyUnvalidatedConnectivityAcceptable(@NonNull Network network,
            boolean acceptable) {
        final TrackedNetwork trackedNetwork = mTrackedNetworks.get(network);
        if (trackedNetwork == null) return;
        trackedNetwork.acceptUnvalidated = acceptable;

        updateNotifications(trackedNetwork);
    }

    public void notifyPartialConnectivityAcceptable(@NonNull Network network, boolean acceptable) {
        final TrackedNetwork trackedNetwork = mTrackedNetworks.get(network);
        if (trackedNetwork == null) return;
        trackedNetwork.acceptPartial = acceptable;

        updateNotifications(trackedNetwork);
    }

    private void updateNotifications(@NonNull TrackedNetwork tn) {
        if (tn.linkProperties == null || tn.networkCapabilities == null) return;

        final boolean validated = tn.networkCapabilities.hasCapability(NET_CAPABILITY_VALIDATED);
        final boolean wasValidated = tn.lastValidated;
        tn.lastValidated = validated;
        if (validated) {
            tn.everValidated = true;
            tn.validationTimedOut = false;
        }
        final boolean portal = tn.networkCapabilities.hasCapability(NET_CAPABILITY_CAPTIVE_PORTAL);
        tn.everCaptivePortalDetected |= portal;

        final NotificationType nextNotification;
        if (tn.networkAgentConfig.isProvisioningNotificationEnabled()
                && tn.networkCapabilities.hasCapability(NET_CAPABILITY_CAPTIVE_PORTAL)) {
            nextNotification = NotificationType.SIGN_IN;
        } else if (tn.networkCapabilities.hasCapability(NET_CAPABILITY_PARTIAL_CONNECTIVITY)
                && shouldPromptUnvalidated(tn)) {
            nextNotification = NotificationType.PARTIAL_CONNECTIVITY;
        } else if (!tn.hasShownDnsBroken && tn.networkCapabilities.isPrivateDnsBroken()) {
            nextNotification = NotificationType.PRIVATE_DNS_BROKEN;
            tn.hasShownDnsBroken = true;
        } else if (tn.validationTimedOut && shouldPromptUnvalidated(tn)) {
            nextNotification = NotificationType.NO_INTERNET;
        } else if (mLingerMonitor.shouldNotify(tn.network)) {
            nextNotification = NotificationType.NETWORK_SWITCH;
        } else if (wasValidated && !validated && tn.networkCapabilities.hasTransport(TRANSPORT_WIFI)
                && shouldNotifyWifiUnvalidated()) {
            nextNotification = NotificationType.LOST_INTERNET;
        } else {
            nextNotification = null;
        }

        if (Objects.equal(nextNotification, tn.currentNotification)) return;
        if (nextNotification == null) {
            clearNotification(tn.network.getNetId());
            tn.currentNotification = null;
            return;
        }

        final TrackedNetwork switchToNetwork = nextNotification == NotificationType.NETWORK_SWITCH
                && mDefaultNetworkWatcher.mDefaultNetwork != null
                ? mTrackedNetworks.get(mDefaultNetworkWatcher.mDefaultNetwork) : null;
        final PendingIntent intent = getIntent(nextNotification, tn);
        showNotification(tn.network.getNetId(), nextNotification, tn, switchToNetwork,
                intent, shouldNotifyHighPriority(nextNotification, tn));
        tn.currentNotification = nextNotification;
    }

    private boolean shouldNotifyHighPriority(@NonNull NotificationType type,
            @NonNull TrackedNetwork network) {
        switch (type) {
            case PARTIAL_CONNECTIVITY:
            case SIGN_IN:
                return network.networkAgentConfig.isExplicitlySelected();
            case NO_INTERNET:
            case PRIVATE_DNS_BROKEN:
            case LOST_INTERNET:
            case NETWORK_SWITCH:
            default:
                return true;
        }
    }

    private PendingIntent getIntent(@NonNull NotificationType type,
            @NonNull TrackedNetwork network) {
        if (type == NotificationType.SIGN_IN) {
            return mPortalIntentReceiver.getPendingIntent(network.network);
        } else if (type == NotificationType.NETWORK_SWITCH) {
            return mLingerMonitor.createNotificationIntent();
        }

        final String action;
        switch (type) {
            case NO_INTERNET:
                action = ConnectivityManager.ACTION_PROMPT_UNVALIDATED;
                break;
            case PRIVATE_DNS_BROKEN:
                action = Settings.ACTION_WIRELESS_SETTINGS;
                break;
            case LOST_INTERNET:
                action = ConnectivityManager.ACTION_PROMPT_LOST_VALIDATION;
                break;
            case PARTIAL_CONNECTIVITY:
                action = ConnectivityManager.ACTION_PROMPT_PARTIAL_CONNECTIVITY;
                break;
            default:
                return null;
        }

        final Intent intent = new Intent(action);
        if (type != NotificationType.PRIVATE_DNS_BROKEN) {
            intent.setData(Uri.fromParts("netId", Integer.toString(network.network.getNetId()),
                    null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Some OEMs have their own Settings package. Thus, need to get the current using
            // Settings package name instead of just use default name "com.android.settings".
            final String settingsPkgName = getSettingsPackageName(mContext.getPackageManager());
            intent.setClassName(settingsPkgName,
                    settingsPkgName + ".wifi.WifiNoInternetDialog");
        }

        return PendingIntent.getActivity(
                mContext.createContextAsUser(UserHandle.CURRENT, 0 /* flags */),
                0 /* requestCode */,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // TODO: This method is copied from TetheringNotificationUpdater. Should have a utility class to
    // unify the method.
    private static @NonNull String getSettingsPackageName(@NonNull final PackageManager pm) {
        final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
        final ComponentName settingsComponent = settingsIntent.resolveActivity(pm);
        return settingsComponent != null
                ? settingsComponent.getPackageName() : "com.android.settings";
    }

    private class PortalNotificationIntentReceiver extends BroadcastReceiver {
        private static final String ACTION = "com.android.connectivity.notification.portalapp";

        public void register() {
            mContext.registerReceiver(this, new IntentFilter(ACTION),
                    NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK, mHandler);
        }

        public PendingIntent getPendingIntent(@NonNull Network network) {
            final Intent intent = new Intent(ACTION);
            intent.setClass(mContext, PortalNotificationIntentReceiver.class);
            intent.setIdentifier(String.valueOf(network.getNetworkHandle()));
            intent.putExtra(ConnectivityManager.EXTRA_NETWORK, network);
            return PendingIntent.getBroadcast(mContext, 0, intent, 0);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(ACTION)) return;
            final Network network = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK);
            if (network == null) return;

            final TrackedNetwork trackedNetwork = mTrackedNetworks.get(network);
            if (trackedNetwork == null
                    || trackedNetwork.currentNotification != NotificationType.SIGN_IN) {
                return;
            }
            mCm.startCaptivePortalApp(network);
        }
    }

    /**
     * Whether to display a notification when wifi becomes unvalidated.
     */
    private boolean shouldNotifyWifiUnvalidated() {
        return !mCm.shouldAvoidBadWifi() && Settings.Global.getString(
                mContext.getContentResolver(), NETWORK_AVOID_BAD_WIFI) == null;
    }

    private static boolean shouldPromptUnvalidated(@NonNull TrackedNetwork tn) {
        // Don't prompt if the network is validated, and don't prompt on captive portals
        // because we're already prompting the user to sign in.
        if (tn.everValidated || tn.everCaptivePortalDetected) {
            return false;
        }

        // If a network has partial connectivity, always prompt unless the user has already accepted
        // partial connectivity and selected don't ask again. This ensures that if the device
        // automatically connects to a network that has partial Internet access, the user will
        // always be able to use it, either because they've already chosen "don't ask again" or
        // because we have prompt them.
        if (tn.networkCapabilities.hasCapability(NET_CAPABILITY_PARTIAL_CONNECTIVITY)
                && !tn.networkAgentConfig.acceptPartialConnectivity) {
            return true;
        }

        // If a network has no Internet access, only prompt if the network was explicitly selected
        // and if the user has not already told us to use the network regardless of whether it
        // validated or not.
        if (tn.networkAgentConfig.explicitlySelected
                && !tn.networkAgentConfig.acceptUnvalidated) {
            return true;
        }

        return false;
    }

    @VisibleForTesting
    protected static int approximateTransportType(TrackedNetwork network) {
        return network.networkCapabilities.hasTransport(TRANSPORT_VPN)
                ? TRANSPORT_VPN : getFirstTransportType(network);
    }

    // TODO: deal more gracefully with multi-transport networks.
    private static int getFirstTransportType(TrackedNetwork network) {
        // TODO: The range is wrong, the safer and correct way is to change the range from
        // MIN_TRANSPORT to MAX_TRANSPORT.
        for (int i = 0; i < 64; i++) {
            if (network.networkCapabilities.hasTransport(i)) return i;
        }
        return -1;
    }

    private static String getTransportName(final int transportType) {
        Resources r = Resources.getSystem();
        String[] networkTypes = r.getStringArray(R.array.network_switch_type_name);
        try {
            return networkTypes[transportType];
        } catch (IndexOutOfBoundsException e) {
            return r.getString(R.string.network_switch_type_name_unknown);
        }
    }

    private static int getIcon(int transportType) {
        return (transportType == TRANSPORT_WIFI)
                ? R.drawable.stat_notify_wifi_in_range :  // TODO: Distinguish ! from ?.
                R.drawable.stat_notify_rssi_in_range;
    }

    /**
     * Show or hide network provisioning notifications.
     *
     * We use notifications for two purposes: to notify that a network requires sign in
     * (NotificationType.SIGN_IN), or to notify that a network does not have Internet access
     * (NotificationType.NO_INTERNET). We display at most one notification per ID, so on a
     * particular network we can display the notification type that was most recently requested.
     * So for example if a captive portal fails to reply within a few seconds of connecting, we
     * might first display NO_INTERNET, and then when the captive portal check completes, display
     * SIGN_IN.
     *
     * @param id an identifier that uniquely identifies this notification.  This must match
     *         between show and hide calls.  We use the NetID value but for legacy callers
     *         we concatenate the range of types with the range of NetIDs.
     * @param notifyType the type of the notification.
     * @param network the network with which the notification is associated. For a SIGN_IN,
     *         NO_INTERNET, or LOST_INTERNET notification, this is the network we're connecting to.
     *         For a NETWORK_SWITCH notification it's the network that we switched from. When this
     *         network disconnects the notification is removed.
     * @param switchToNetwork for a NETWORK_SWITCH notification, the network we are switching to.
     *         Null in all other cases. Only used to determine the text of the notification.
     */
    public void showNotification(int id, NotificationType notifyType, TrackedNetwork network,
            TrackedNetwork switchToNetwork, PendingIntent intent, boolean highPriority) {
        final String tag = tagFor(id);
        final int eventId = notifyType.eventId;
        final int transportType;
        final String name;
        if (network != null) {
            transportType = approximateTransportType(network);
            if (network.linkProperties != null && network.linkProperties.getCaptivePortalData() != null
                    && !TextUtils.isEmpty(network.linkProperties.getCaptivePortalData()
                    .getVenueFriendlyName())) {
                name = network.linkProperties.getCaptivePortalData().getVenueFriendlyName();
            } else {
                final NetworkInfo info = mContext.getSystemService(ConnectivityManager.class)
                        .getNetworkInfo(network.network);
                name = info == null || TextUtils.isEmpty(info.getExtraInfo())
                        ? WifiInfo.sanitizeSsid(network.networkCapabilities.getSsid())
                        : info.getExtraInfo();
            }
            // Only notify for Internet-capable networks.
            if (!network.networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET)) return;
        } else {
            // Legacy notifications.
            transportType = TRANSPORT_CELLULAR;
            name = "";
        }

        // Clear any previous notification with lower priority, otherwise return. http://b/63676954.
        // A new SIGN_IN notification with a new intent should override any existing one.
        final int previousEventId = mNotificationTypeMap.get(id);
        final NotificationType previousNotifyType = NotificationType.getFromId(previousEventId);
        if (priority(previousNotifyType) > priority(notifyType)) {
            Log.d(TAG, String.format(
                    "ignoring notification %s for network %s with existing notification %s",
                    notifyType, id, previousNotifyType));
            return;
        }
        clearNotification(id);

        if (DBG) {
            Log.d(TAG, String.format(
                    "showNotification tag=%s event=%s transport=%s name=%s highPriority=%s",
                    tag, nameOf(eventId), getTransportName(transportType), name, highPriority));
        }

        Resources r = mContext.getResources();
        final CharSequence title;
        final CharSequence details;
        int icon = getIcon(transportType);
        if (notifyType == NotificationType.NO_INTERNET && transportType == TRANSPORT_WIFI) {
            title = r.getString(R.string.wifi_no_internet, name);
            details = r.getString(R.string.wifi_no_internet_detailed);
        } else if (notifyType == NotificationType.PRIVATE_DNS_BROKEN) {
            if (transportType == TRANSPORT_CELLULAR) {
                title = r.getString(R.string.mobile_no_internet);
            } else if (transportType == TRANSPORT_WIFI) {
                title = r.getString(R.string.wifi_no_internet, name);
            } else {
                title = r.getString(R.string.other_networks_no_internet);
            }
            details = r.getString(R.string.private_dns_broken_detailed);
        } else if (notifyType == NotificationType.PARTIAL_CONNECTIVITY
                && transportType == TRANSPORT_WIFI) {
            title = r.getString(R.string.network_partial_connectivity, name);
            details = r.getString(R.string.network_partial_connectivity_detailed);
        } else if (notifyType == NotificationType.LOST_INTERNET &&
                transportType == TRANSPORT_WIFI) {
            title = r.getString(R.string.wifi_no_internet, name);
            details = r.getString(R.string.wifi_no_internet_detailed);
        } else if (notifyType == NotificationType.SIGN_IN) {
            switch (transportType) {
                case TRANSPORT_WIFI:
                    title = r.getString(R.string.wifi_available_sign_in, 0);
                    details = r.getString(R.string.network_available_sign_in_detailed, name);
                    break;
                case TRANSPORT_CELLULAR:
                    title = r.getString(R.string.network_available_sign_in, 0);
                    // TODO: Change this to pull from NetworkInfo once a printable
                    // name has been added to it
                    NetworkSpecifier specifier = network.networkCapabilities.getNetworkSpecifier();
                    int subId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
                    if (specifier instanceof TelephonyNetworkSpecifier) {
                        subId = ((TelephonyNetworkSpecifier) specifier).getSubscriptionId();
                    }

                    details = mTelephonyManager.createForSubscriptionId(subId)
                            .getNetworkOperatorName();
                    break;
                default:
                    title = r.getString(R.string.network_available_sign_in, 0);
                    details = r.getString(R.string.network_available_sign_in_detailed, name);
                    break;
            }
        } else if (notifyType == NotificationType.NETWORK_SWITCH) {
            String fromTransport = getTransportName(transportType);
            String toTransport = getTransportName(approximateTransportType(switchToNetwork));
            title = r.getString(R.string.network_switch_metered, toTransport);
            details = r.getString(R.string.network_switch_metered_detail, toTransport,
                    fromTransport);
        } else if (notifyType == NotificationType.NO_INTERNET
                    || notifyType == NotificationType.PARTIAL_CONNECTIVITY) {
            // NO_INTERNET and PARTIAL_CONNECTIVITY notification for non-WiFi networks
            // are sent, but they are not implemented yet.
            return;
        } else {
            Log.wtf(TAG, "Unknown notification type " + notifyType + " on network transport "
                    + getTransportName(transportType));
            return;
        }
        // When replacing an existing notification for a given network, don't alert, just silently
        // update the existing notification. Note that setOnlyAlertOnce() will only work for the
        // same id, and the id used here is the NotificationType which is different in every type of
        // notification. This is required because the notification metrics only track the ID but not
        // the tag.
        final boolean hasPreviousNotification = previousNotifyType != null;
        final String channelId = (highPriority && !hasPreviousNotification)
                ? NOTIFICATION_CHANNEL_NETWORK_ALERTS : NOTIFICATION_CHANNEL_NETWORK_STATUS;
        Notification.Builder builder = new Notification.Builder(mContext, channelId)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(notifyType == NotificationType.NETWORK_SWITCH)
                .setSmallIcon(icon)
                .setAutoCancel(true)
                .setTicker(title)
                .setColor(mContext.getColor(
                        com.android.internal.R.color.system_notification_accent_color))
                .setContentTitle(title)
                .setContentIntent(intent)
                .setLocalOnly(true)
                .setOnlyAlertOnce(true);

        if (notifyType == NotificationType.NETWORK_SWITCH) {
            builder.setStyle(new Notification.BigTextStyle().bigText(details));
        } else {
            builder.setContentText(details);
        }

        if (notifyType == NotificationType.SIGN_IN) {
            builder.extend(new Notification.TvExtender().setChannelId(channelId));
        }

        Notification notification = builder.build();

        mNotificationTypeMap.put(id, eventId);
        try {
            mNotificationManager.notify(tag, eventId, notification);
        } catch (NullPointerException npe) {
            Log.d(TAG, "setNotificationVisible: visible notificationManager error", npe);
        }
    }

    /**
     * Clear the notification with the given id, only if it matches the given type.
     */
    public void clearNotification(int id, NotificationType notifyType) {
        final int previousEventId = mNotificationTypeMap.get(id);
        final NotificationType previousNotifyType = NotificationType.getFromId(previousEventId);
        if (notifyType != previousNotifyType) {
            return;
        }
        clearNotification(id);
    }

    public void clearNotification(int id) {
        if (mNotificationTypeMap.indexOfKey(id) < 0) {
            return;
        }
        final String tag = tagFor(id);
        final int eventId = mNotificationTypeMap.get(id);
        if (DBG) {
            Log.d(TAG, String.format("clearing notification tag=%s event=%s", tag,
                   nameOf(eventId)));
        }
        try {
            mNotificationManager.cancel(tag, eventId);
        } catch (NullPointerException npe) {
            Log.d(TAG, String.format(
                    "failed to clear notification tag=%s event=%s", tag, nameOf(eventId)), npe);
        }
        mNotificationTypeMap.delete(id);
    }

    /**
     * Legacy provisioning notifications coming directly from DcTracker.
     */
    public void setProvNotificationVisible(boolean visible, int id, String action) {
        if (visible) {
            // For legacy purposes, action is sent as the action + the phone ID from DcTracker.
            // Split the string here and send the phone ID as an extra instead.
            String[] splitAction = action.split(":");
            Intent intent = new Intent(splitAction[0]);
            try {
                intent.putExtra("provision.phone.id", Integer.parseInt(splitAction[1]));
            } catch (NumberFormatException ignored) { }
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    mContext, 0 /* requestCode */, intent, PendingIntent.FLAG_IMMUTABLE);
            showNotification(id, NotificationType.SIGN_IN, null, null, pendingIntent, false);
        } else {
            clearNotification(id);
        }
    }

    public void showToast(TrackedNetwork fromNetwork, TrackedNetwork toNetwork) {
        String fromTransport = getTransportName(approximateTransportType(fromNetwork));
        String toTransport = getTransportName(approximateTransportType(toNetwork));
        String text = mContext.getResources().getString(
                R.string.network_switch_metered_toast, fromTransport, toTransport);
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
    }

    @VisibleForTesting
    static String tagFor(int id) {
        return String.format("ConnectivityNotification:%d", id);
    }

    @VisibleForTesting
    static String nameOf(int eventId) {
        NotificationType t = NotificationType.getFromId(eventId);
        return (t != null) ? t.name() : "UNKNOWN";
    }

    /**
     * A notification with a higher number will take priority over a notification with a lower
     * number.
     */
    private static int priority(NotificationType t) {
        if (t == null) {
            return 0;
        }
        switch (t) {
            case SIGN_IN:
                return 6;
            case PARTIAL_CONNECTIVITY:
                return 5;
            case PRIVATE_DNS_BROKEN:
                return 4;
            case NO_INTERNET:
                return 3;
            case NETWORK_SWITCH:
                return 2;
            case LOST_INTERNET:
                return 1;
            default:
                return 0;
        }
    }
}
