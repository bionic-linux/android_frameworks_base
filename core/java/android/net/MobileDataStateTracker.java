/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.RemoteException;
import android.os.Handler;
import android.os.ServiceManager;

import com.android.internal.net.IpVersion;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import android.net.NetworkInfo.DetailedState;
import android.telephony.TelephonyManager;
import android.util.Slog;
import android.text.TextUtils;

/**
 * Track the state of mobile data connectivity. This is done by
 * receiving broadcast intents from the Phone process whenever
 * the state of data connectivity changes.
 *
 * {@hide}
 */
public class MobileDataStateTracker extends NetworkStateTracker {

    private static final String TAG = "MobileDataStateTracker";
    private static final boolean DBG = true;

    private ITelephony mPhoneService;

    private String mApnType;
    private String mApnTypeToWatchFor;

    //IPV4
    private String mIpv4InterfaceName;
    private Phone.DataState mIpv4MobileDataState;
    private String mIpv4ApnName;

    //IPV6
    private String mIpv6InterfaceName;
    private Phone.DataState mIpv6MobileDataState;
    private String mIpv6ApnName;

    private boolean mEnabled;
    private BroadcastReceiver mStateReceiver;

    /**
     * Create a new MobileDataStateTracker
     * @param context the application context of the caller
     * @param target a message handler for getting callbacks about state changes
     * @param netType the ConnectivityManager network type
     * @param apnType the Phone apnType
     * @param tag the name of this network
     */
    public MobileDataStateTracker(Context context, Handler target, int netType, String tag) {
        super(context, target, netType,
                TelephonyManager.getDefault().getNetworkType(), tag,
                TelephonyManager.getDefault().getNetworkTypeName());
        mApnType = networkTypeToApnType(netType);
        if (TextUtils.equals(mApnType, Phone.APN_TYPE_HIPRI)) {
            mApnTypeToWatchFor = Phone.APN_TYPE_DEFAULT;
        } else {
            mApnTypeToWatchFor = mApnType;
        }

        mPhoneService = null;
        if(netType == ConnectivityManager.TYPE_MOBILE) {
            mEnabled = true;
        } else {
            mEnabled = false;
        }
    }

    /**
     * Begin monitoring mobile data connectivity.
     */
    public void startMonitoring() {

        IntentFilter filter =
                new IntentFilter(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED);
        filter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);

        mIpv4MobileDataState = Phone.DataState.DISCONNECTED;
        mIpv6MobileDataState = Phone.DataState.DISCONNECTED;

        /*
         * TODO: the last broadcasted intent may be for another apn type or ip
         * version. Replace this with a query API.
         */
        mStateReceiver = new MobileDataStateReceiver();
        Intent intent = mContext.registerReceiver(mStateReceiver, filter);
        if (intent != null) {
            mStateReceiver.onReceive(mContext, intent);
        }

        logv("initial state. v4=" + mIpv4MobileDataState + ", v6=" + mIpv4MobileDataState);
    }

    private Phone.DataState getApnTypeState(Intent intent) {
        String str = intent.getStringExtra(Phone.DATA_APN_TYPE_STATE_KEY);
        if (str != null) {
            String apnTypeList =
                    intent.getStringExtra(Phone.DATA_APN_TYPES_KEY);
            if (isApnTypeIncluded(apnTypeList)) {
                return Enum.valueOf(Phone.DataState.class, str);
            }
        }
        return Phone.DataState.DISCONNECTED;
    }

    private IpVersion getIpVersionFromIntent(Intent intent) {
        String ipVersion =  intent.getStringExtra(Phone.DATA_IPVERSION_KEY);
        if (ipVersion != null) {
            return Enum.valueOf(IpVersion.class, ipVersion);
        }
        return IpVersion.INET;
    }

    private boolean isApnTypeIncluded(String typeList) {
        /* comma seperated list - split and check */
        if (typeList == null)
            return false;

        String[] list = typeList.split(",");
        for(int i=0; i< list.length; i++) {
            if (TextUtils.equals(list[i], mApnTypeToWatchFor) ||
                TextUtils.equals(list[i], Phone.APN_TYPE_ALL)) {
                return true;
            }
        }
        return false;
    }

    private class MobileDataStateReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            synchronized(this) {
                if (intent.getAction().equals(TelephonyIntents.
                        ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {

                    String apnTypeList = intent.getStringExtra(Phone.DATA_APN_TYPES_KEY);
                    boolean unavailable = intent.getBooleanExtra(Phone.NETWORK_UNAVAILABLE_KEY,
                            false);

                    // set this regardless of everything else. It's
                    // all the same radio/network underneath
                    mNetworkInfo.setIsAvailable(!unavailable);

                    if (isApnTypeIncluded(apnTypeList) == false)
                        return; //not the apnType we are looking for

                    /*
                     * fetch rest of the information from intent, and update
                     */
                    Phone.DataState state = getApnTypeState(intent);
                    IpVersion ipv = getIpVersionFromIntent(intent);
                    String reason = intent.getStringExtra(Phone.STATE_CHANGE_REASON_KEY);
                    String apnName = intent.getStringExtra(Phone.DATA_APN_KEY);
                    String interfaceName = intent.getStringExtra(Phone.DATA_IFACE_NAME_KEY);

                    logi("any dc state change intent received for " + mApnTypeToWatchFor + "/"
                            + ipv + " : enabled = " + mEnabled + ", new state = " + state
                            + ", apn = " + apnName + ", interface = " + interfaceName
                            + ", reason = " + reason);

                    /*
                     * Cache the information in the intent received - even if we
                     * are disabled. needDetailedStateUpdate will be set to true
                     * if state has really changed, and if connectivity service
                     * needs to be notified.
                     */

                    boolean needDetailedStateUpdate =
                        updateMobileDataState(ipv, state, interfaceName, apnName);

                    if (mEnabled == false || needDetailedStateUpdate == false) {
                        /*
                         * no need to notify connectivity service if we are disabled,
                         * or if nothing has changed.
                         */
                        return;
                    }

                    /*
                     * We keep separate states for v4 and v6 in mobile data
                     * state tracker, but mNetworkinfo and connectivity service
                     * needs just one state. So we say CONNECTED if either v4 or
                     * v6 is connected.
                     */
                    if (mIpv4MobileDataState == Phone.DataState.CONNECTED
                            || mIpv6MobileDataState == Phone.DataState.CONNECTED) {
                        state = Phone.DataState.CONNECTED;
                    }

                    String extraInfo = "ipv4 apn name = " + mIpv4ApnName + "," +
                            "ipv6 apn name = " + mIpv6ApnName;

                    if (needDetailedStateUpdate) {
                        switch (state) {
                            case DISCONNECTED:
                                if(isTeardownRequested()) {
                                    mEnabled = false;
                                    setTeardownRequested(false);
                                }
                                setDetailedState(DetailedState.DISCONNECTED, reason, extraInfo);
                                break;
                            case CONNECTING:
                                setDetailedState(DetailedState.CONNECTING, reason, extraInfo);
                                break;
                            case SUSPENDED:
                                setDetailedState(DetailedState.SUSPENDED, reason, extraInfo);
                                break;
                            case CONNECTED:
                                setDetailedState(DetailedState.CONNECTED, reason, extraInfo);
                                break;
                        }
                    }
                } else if (intent.getAction().
                        equals(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED)) {
                    mEnabled = false;
                    String reason = intent.getStringExtra(Phone.FAILURE_REASON_KEY);
                    String apnName = intent.getStringExtra(Phone.DATA_APN_KEY);
                    logi("Received " + intent.getAction() + " broadcast" +
                            reason == null ? "" : "(" + reason + ")");
                    setDetailedState(DetailedState.FAILED, reason, apnName);
                }
                TelephonyManager tm = TelephonyManager.getDefault();
                setRoamingStatus(tm.isNetworkRoaming());
                setSubtype(tm.getNetworkType(), tm.getNetworkTypeName());
            }
        }
    }

    private boolean updateMobileDataState(IpVersion ipv, Phone.DataState newState,
            String newInterfaceName, String newApn) {

        Phone.DataState oldState = ipv == IpVersion.INET ? mIpv4MobileDataState
                : mIpv6MobileDataState;

        if (oldState == newState) {
            /* nothing has changed for this IP family, return. */
            return false;
        }

        if (ipv == IpVersion.INET6) {
            logv("ipv6 state changed :" + mIpv6MobileDataState + " >> " + newState);
            mIpv6MobileDataState = newState;
            if (newState == Phone.DataState.CONNECTED) {
                mIpv6ApnName = newApn;
                mIpv6InterfaceName = newInterfaceName;
                logv("setting ipv6 interface : " + mIpv6InterfaceName);
            } else if (newState == Phone.DataState.DISCONNECTED && mIpv6InterfaceName != null) {
                NetworkUtils.resetConnections(mIpv6InterfaceName);
                mIpv6InterfaceName = null;
            }
        } else if (ipv == IpVersion.INET) {
            logv("ipv4 state changed :" + mIpv4MobileDataState + " >> " + newState);
            mIpv4MobileDataState = newState;
            if (newState == Phone.DataState.CONNECTED) {
                mIpv4ApnName = newApn;
                mIpv4InterfaceName = newInterfaceName;
                logv("setting ipv4 interface : " + mIpv4InterfaceName);
            } else if (newState == Phone.DataState.DISCONNECTED && mIpv4InterfaceName != null) {
                NetworkUtils.resetConnections(mIpv4InterfaceName);
                mIpv4InterfaceName = null;
            }
        }
        return true;
    }

    private void getPhoneService(boolean forceRefresh) {
        if ((mPhoneService == null) || forceRefresh) {
            mPhoneService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    }

    /**
     * Report whether data connectivity is possible.
     */
    public boolean isAvailable() {
        getPhoneService(false);

        /*
         * If the phone process has crashed in the past, we'll get a
         * RemoteException and need to re-reference the service.
         */
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) break;

            try {
                return mPhoneService.isDataConnectivityPossible();
            } catch (RemoteException e) {
                // First-time failed, get the phone service again
                if (retry == 0) getPhoneService(true);
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * The mobile data network subtype indicates what generation network technology is in effect,
     * e.g., GPRS, EDGE, UMTS, etc.
     */
    public int getNetworkSubtype() {
        return TelephonyManager.getDefault().getNetworkType();
    }

    /**
     * Return the system properties name associated with the tcp buffer sizes
     * for this network.
     */
    public String getTcpBufferSizesPropName() {
        String networkTypeStr = "unknown";
        TelephonyManager tm = new TelephonyManager(mContext);
        //TODO We have to edit the parameter for getNetworkType regarding CDMA
        switch(tm.getNetworkType()) {
        case TelephonyManager.NETWORK_TYPE_GPRS:
            networkTypeStr = "gprs";
            break;
        case TelephonyManager.NETWORK_TYPE_EDGE:
            networkTypeStr = "edge";
            break;
        case TelephonyManager.NETWORK_TYPE_UMTS:
            networkTypeStr = "umts";
            break;
        case TelephonyManager.NETWORK_TYPE_HSDPA:
            networkTypeStr = "hsdpa";
            break;
        case TelephonyManager.NETWORK_TYPE_HSUPA:
            networkTypeStr = "hsupa";
            break;
        case TelephonyManager.NETWORK_TYPE_HSPA:
            networkTypeStr = "hspa";
            break;
        case TelephonyManager.NETWORK_TYPE_CDMA:
            networkTypeStr = "cdma";
            break;
        case TelephonyManager.NETWORK_TYPE_1xRTT:
            networkTypeStr = "1xrtt";
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_0:
            networkTypeStr = "evdo";
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
            networkTypeStr = "evdo";
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
            networkTypeStr = "evdo";
            break;
        }
        return "net.tcp.buffersize." + networkTypeStr;
    }

    /**
     * Tear down mobile data connectivity, i.e., disable the ability to create
     * mobile data connections.
     */
    @Override
    public boolean teardown() {
        // since we won't get a notification currently (TODO - per APN notifications)
        // we won't get a disconnect message until all APN's on the current connection's
        // APN list are disabled.  That means privateRoutes for DNS and such will remain on -
        // not a problem since that's all shared with whatever other APN is still on, but
        // ugly.
        setTeardownRequested(true);
        return (setEnableApn(mApnType, false) != Phone.APN_REQUEST_FAILED);
    }

    /**
     * Re-enable mobile data connectivity after a {@link #teardown()}.
     */
    public boolean reconnect() {
        setTeardownRequested(false);
        switch (setEnableApn(mApnType, true)) {
            case Phone.APN_ALREADY_ACTIVE:
                /*
                 * APN is already active, we are not going to get any more intents
                 * from data connection tracker, so we need to rebroadcast the
                 * intent with the last ipv4 and ipv6 states we cached.
                 */
                mEnabled = true;

                logv("dct reports apn already active. " + this);

                /*
                 * fake an intent to so that mStateReceiver.onReceive() function
                 * notifies connectivity service of a connection state changed.
                 * We set current state as connecting and call onReceive with
                 * the actual state we cached.
                 */

                Intent intent = new Intent(TelephonyIntents.
                        ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
                intent.putExtra(Phone.STATE_KEY, Phone.DataState.CONNECTED.toString());
                intent.putExtra(Phone.STATE_CHANGE_REASON_KEY, Phone.REASON_APN_CHANGED);
                intent.putExtra(Phone.DATA_APN_TYPES_KEY, mApnTypeToWatchFor);
                intent.putExtra(Phone.NETWORK_UNAVAILABLE_KEY, false);

                /* Once for IPV4 */
                intent.putExtra(Phone.DATA_APN_KEY, mIpv4ApnName);
                intent.putExtra(Phone.DATA_APN_TYPE_STATE_KEY, mIpv4MobileDataState.toString());
                intent.putExtra(Phone.DATA_IPVERSION_KEY, IpVersion.INET.toString());
                intent.putExtra(Phone.DATA_IFACE_NAME_KEY, mIpv4InterfaceName);
                mIpv4MobileDataState = Phone.DataState.CONNECTING;
                if (mStateReceiver != null) mStateReceiver.onReceive(mContext, intent);

                /* Once for IPV6 */
                intent.putExtra(Phone.DATA_APN_KEY, mIpv6ApnName);
                intent.putExtra(Phone.DATA_APN_TYPE_STATE_KEY, mIpv6MobileDataState.toString());
                intent.putExtra(Phone.DATA_IPVERSION_KEY, IpVersion.INET6.toString());
                intent.putExtra(Phone.DATA_IFACE_NAME_KEY, mIpv6InterfaceName);
                mIpv6MobileDataState = Phone.DataState.CONNECTING;
                if (mStateReceiver != null) mStateReceiver.onReceive(mContext, intent);

                break;
            case Phone.APN_REQUEST_STARTED:
                mEnabled = true;
                // no need to do anything - we're already due some status update intents
                break;
            case Phone.APN_REQUEST_FAILED:
                if (mPhoneService == null && mApnType == Phone.APN_TYPE_DEFAULT) {
                    // on startup we may try to talk to the phone before it's ready
                    // since the phone will come up enabled, go with that.
                    // TODO - this also comes up on telephony crash: if we think mobile data is
                    // off and the telephony stuff crashes and has to restart it will come up
                    // enabled (making a data connection).  We will then be out of sync.
                    // A possible solution is a broadcast when telephony restarts.
                    mEnabled = true;
                    return false;
                }
                // else fall through
            case Phone.APN_TYPE_NOT_AVAILABLE:
                // Default is always available, but may be off due to
                // AirplaneMode or E-Call or whatever..
                if (mApnType != Phone.APN_TYPE_DEFAULT) {
                    mEnabled = false;
                }
                break;
            default:
                Slog.e(TAG, "Error in reconnect - unexpected response.");
                mEnabled = false;
                break;
        }
        return mEnabled;
    }

    /**
     * Turn on or off the mobile radio. No connectivity will be possible while the
     * radio is off. The operation is a no-op if the radio is already in the desired state.
     * @param turnOn {@code true} if the radio should be turned on, {@code false} if
     */
    public boolean setRadio(boolean turnOn) {
        getPhoneService(false);
        /*
         * If the phone process has crashed in the past, we'll get a
         * RemoteException and need to re-reference the service.
         */
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) {
                Slog.w(TAG,
                    "Ignoring mobile radio request because could not acquire PhoneService");
                break;
            }

            try {
                return mPhoneService.setRadio(turnOn);
            } catch (RemoteException e) {
                if (retry == 0) getPhoneService(true);
            }
        }

        Slog.w(TAG, "Could not set radio power to " + (turnOn ? "on" : "off"));
        return false;
    }

    /**
     * Tells the phone sub-system that the caller wants to
     * begin using the named feature. The only supported features at
     * this time are {@code Phone.FEATURE_ENABLE_MMS}, which allows an application
     * to specify that it wants to send and/or receive MMS data, and
     * {@code Phone.FEATURE_ENABLE_SUPL}, which is used for Assisted GPS.
     * @param feature the name of the feature to be used
     * @param callingPid the process ID of the process that is issuing this request
     * @param callingUid the user ID of the process that is issuing this request
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is feature-specific.
     * specific, except that the value {@code -1}
     * always indicates failure. For {@code Phone.FEATURE_ENABLE_MMS},
     * the other possible return values are
     * <ul>
     * <li>{@code Phone.APN_ALREADY_ACTIVE}</li>
     * <li>{@code Phone.APN_REQUEST_STARTED}</li>
     * <li>{@code Phone.APN_TYPE_NOT_AVAILABLE}</li>
     * <li>{@code Phone.APN_REQUEST_FAILED}</li>
     * </ul>
     */
    public int startUsingNetworkFeature(String feature, int callingPid, int callingUid) {
        return -1;
    }

    /**
     * Tells the phone sub-system that the caller is finished
     * using the named feature. The only supported feature at
     * this time is {@code Phone.FEATURE_ENABLE_MMS}, which allows an application
     * to specify that it wants to send and/or receive MMS data.
     * @param feature the name of the feature that is no longer needed
     * @param callingPid the process ID of the process that is issuing this request
     * @param callingUid the user ID of the process that is issuing this request
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is feature-specific, except that
     * the value {@code -1} always indicates failure.
     */
    public int stopUsingNetworkFeature(String feature, int callingPid, int callingUid) {
        return -1;
    }

    /**
     * Ensure that a network route exists to deliver traffic to the specified
     * host via the mobile data network.
     * @param hostAddress the IP address of the host to which the route is desired.
     * @return {@code true} on success, {@code false} on failure
     */
    @Override
    public boolean requestRouteToHost(InetAddress hostAddress) {

        /*
         * Use v4 interface to route to v4 host, and v6 interface to route to v6
         * host.
         */
        String interfaceName = null;
        if (hostAddress instanceof Inet4Address) {
            interfaceName = mIpv4InterfaceName;
        } else if (hostAddress instanceof Inet6Address) {
            interfaceName = mIpv6InterfaceName;
        }

        logv("Requested host route to " + hostAddress.getHostAddress() +
                " for " + mApnType + "(" + interfaceName + ")");

        if (interfaceName != null) {
            return NetworkUtils.addHostRoute(interfaceName, hostAddress);
        } else {
            return false;
        }
    }

    /**
     * Return the IP addresses of the DNS servers available for the current
     * network interface.
     * @return a list of DNS addresses, with no holes.
     */
    @Override
    public String[] getNameServers() {
        //null interfaces are fine - take care of by getNameServerList()
        String[] dnsPropNames = new String[] {
                /* static list - emulator etc.. */
                "net.eth0.dns1",
                "net.eth0.dns2",
                "net.eth0.dns3",
                "net.eth0.dns4",
                "net.gprs.dns1",
                "net.gprs.dns2",
                "net.ppp0.dns1",
                "net.ppp0.dns2",
                /* dynamic */
                "net." + mIpv4InterfaceName + ".dns1",
                "net." + mIpv4InterfaceName + ".dns2",
                "net." + mIpv6InterfaceName + ".dns1",
                "net." + mIpv6InterfaceName + ".dns2"
            };
        return getNameServerList(dnsPropNames);
    }

    private boolean mIpv4PrivateDnsRouteSet = false;

    /*
     * (non-Javadoc)
     * @see android.net.NetworkStateTracker#addPrivateDnsRoutes()
     * This function just takes care of adding dns routes to V4 dns servers. Assumes
     * that somebody else (kernel??) handles setting routes to V6 dns servers.
     */
    @Override
    public void addPrivateDnsRoutes() {
        if (DBG) {
            logv("addPrivateDnsRoutes. " + this);
            logv("  mIpv4InterfaceName = " + mIpv4InterfaceName +
                    ", mIpv4PrivateDnsRouteSet = "+ mIpv4PrivateDnsRouteSet);
        }

        if (mIpv4InterfaceName != null && !mIpv4PrivateDnsRouteSet) {
            for (String addrString : getNameServers()) {
                    try {
                        InetAddress inetAddress = InetAddress.getByName(addrString);
                        if (inetAddress instanceof Inet4Address) {
                            logv("  adding ipv4 dns " + addrString + " through "+mIpv4InterfaceName);
                            NetworkUtils.addHostRoute(mIpv4InterfaceName, inetAddress);
                        }
                    } catch (UnknownHostException e) {
                        logw(" DNS address " + addrString + " : Exception " + e);
                    }
            }
            mIpv4PrivateDnsRouteSet = true;
        }
    }

    @Override
    public void removePrivateDnsRoutes() {
        // TODO - we should do this explicitly but the NetUtils api doesnt
        // support this yet - must remove all.  No worse than before
        if (mIpv4InterfaceName != null && mIpv4PrivateDnsRouteSet) {
            logv("remove ipv4 dns routes for " + mNetworkInfo.getTypeName() +
                    " (" + mIpv4InterfaceName + ")");
            NetworkUtils.removeHostRoutes(mIpv4InterfaceName);
            mIpv4PrivateDnsRouteSet = false;
        }
    }

    /**
     * Record the detailed state of a network, and if it is a
     * change from the previous state, send a notification to
     * any listeners.
     * @param state the new @{code DetailedState}
     * @param reason a {@code String} indicating a reason for the state change,
     * if one was supplied. May be {@code null}.
     * @param extraInfo optional {@code String} providing extra information about the state change
     */
    @Override
    public void setDetailedState(NetworkInfo.DetailedState state, String reason, String extraInfo) {

        logv("setDetailed state :" + mNetworkInfo.getDetailedState() + " >>> " + state);

        /*
         * We shouldn't do old state != new state here. if we are IPV4 CONNECTED and now
         * become IPV6 CONNECTED or vice versa, we NEED to notify
         * ConnectivtyService so that it can trigger adding/removing private dns routes etc.
         */

        boolean wasConnecting = (mNetworkInfo.getState() == NetworkInfo.State.CONNECTING);
        String lastReason = mNetworkInfo.getReason();
        /*
         * If a reason was supplied when the CONNECTING state was entered, and
         * no reason was supplied for entering the CONNECTED state, then retain
         * the reason that was supplied when going to CONNECTING.
         */
        if (wasConnecting && state == NetworkInfo.DetailedState.CONNECTED && reason == null
                && lastReason != null)
            reason = lastReason;
        mNetworkInfo.setDetailedState(state, reason, extraInfo);
        Message msg = mTarget.obtainMessage(NetworkStateTracker.EVENT_STATE_CHANGED, mNetworkInfo);
        msg.sendToTarget();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Mobile data state: IPV4=");
        sb.append(mIpv4MobileDataState);
        sb.append(", IPV6=");
        sb.append(mIpv6MobileDataState);
        return sb.toString();
    }

   /**
     * Internal method supporting the ENABLE_MMS feature.
     * @param apnType the type of APN to be enabled or disabled (e.g., mms)
     * @param enable {@code true} to enable the specified APN type,
     * {@code false} to disable it.
     * @return an integer value representing the outcome of the request.
     */
    private int setEnableApn(String apnType, boolean enable) {
        getPhoneService(false);
        /*
         * If the phone process has crashed in the past, we'll get a
         * RemoteException and need to re-reference the service.
         */
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) {
                Slog.w(TAG,
                    "Ignoring feature request because could not acquire PhoneService");
                break;
            }

            try {
                if (enable) {
                    return mPhoneService.enableApnType(apnType);
                } else {
                    return mPhoneService.disableApnType(apnType);
                }
            } catch (RemoteException e) {
                if (retry == 0) getPhoneService(true);
            }
        }

        Slog.w(TAG, "Could not " + (enable ? "enable" : "disable")
                + " APN type \"" + apnType + "\"");
        return Phone.APN_REQUEST_FAILED;
    }

    public static String networkTypeToApnType(int netType) {
        switch(netType) {
            case ConnectivityManager.TYPE_MOBILE:
                return Phone.APN_TYPE_DEFAULT;  // TODO - use just one of these
            case ConnectivityManager.TYPE_MOBILE_MMS:
                return Phone.APN_TYPE_MMS;
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                return Phone.APN_TYPE_SUPL;
            case ConnectivityManager.TYPE_MOBILE_DUN:
                return Phone.APN_TYPE_DUN;
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
                return Phone.APN_TYPE_HIPRI;
            default:
                Slog.e(TAG, "Error mapping networkType " + netType + " to apnType.");
                return null;
        }
    }

    void logw(String string) {
        Slog.w(TAG, "[" + mApnType + "] " + string);
    }

    void logv(String string) {
        if (DBG)
            Slog.v(TAG, "[" + mApnType + "] " + string);
    }

    void logi(String string) {
        if (DBG)
            Slog.i(TAG, "[" + mApnType + "] " + string);
    }
}
