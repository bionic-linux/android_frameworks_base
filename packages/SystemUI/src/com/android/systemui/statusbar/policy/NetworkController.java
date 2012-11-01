/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxManagerConstants;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.util.AsyncChannel;
import com.android.systemui.DemoMode;
import com.android.systemui.R;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NetworkController extends BroadcastReceiver implements DemoMode {
    // debug
    static final String TAG = "StatusBar.NetworkController";
    static final boolean DEBUG = false;
    static final boolean CHATTY = false; // additional diagnostics, but not logspew

    private static final int FLIGHT_MODE_ICON = R.drawable.stat_sys_signal_flightmode;

    // telephony
    boolean mHspaDataDistinguishable;
    TelephonyManager mPhone;
    boolean[] mDataConnected;
    IccCardConstants.State[] mSimState;
    int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    int mDataState = TelephonyManager.DATA_DISCONNECTED;
    int[] mDataActivity;
    ServiceState[] mServiceState;
    SignalStrength[] mSignalStrength;
    PhoneStateListener[] mPhoneStateListener;
    int[] mDataIconList = TelephonyIcons.DATA_G[0];
    String[] mNetworkName;
    String mNetworkNameDefault;
    String mNetworkNameSeparator;
    int[] mPhoneSignalIconId;
    int mQSPhoneSignalIconId;
    int[] mDataDirectionIconId; // data + data direction on phones
    int[] mDataSignalIconId;
    int[] mDataTypeIconId;
    int [] mCombinedSignalIconId;
    int mQSDataTypeIconId;
    int mAirplaneIconId;
    boolean mDataActive;
    int mLastSignalLevel;
    boolean mShowPhoneRSSIForData = false;
    boolean mShowAtLeastThreeGees = false;
    boolean mAlwaysShowCdmaRssi = false;
    String[] mCarrierTextSub;
    String[] mMobileLabel;

    String[] mContentDescriptionPhoneSignal;
    String mContentDescriptionWifi;
    String mContentDescriptionWimax;
    String[] mContentDescriptionCombinedSignal;
    String[] mContentDescriptionDataType;

    // wifi
    final WifiManager mWifiManager;
    AsyncChannel mWifiChannel;
    boolean mWifiEnabled, mWifiConnected;
    int mWifiRssi, mWifiLevel;
    String mWifiSsid;
    int mWifiIconId = 0;
    int mQSWifiIconId = 0;
    int mWifiActivity = WifiManager.DATA_ACTIVITY_NONE;

    // bluetooth
    private boolean mBluetoothTethered = false;
    private int mBluetoothTetherIconId =
        com.android.internal.R.drawable.stat_sys_tether_bluetooth;

    //wimax
    private boolean mWimaxSupported = false;
    private boolean mIsWimaxEnabled = false;
    private boolean mWimaxConnected = false;
    private boolean mWimaxIdle = false;
    private int mWimaxIconId = 0;
    private int mWimaxSignal = 0;
    private int mWimaxState = 0;
    private int mWimaxExtraState = 0;

    // data connectivity (regardless of state, can we access the internet?)
    // state of inet connection - 0 not connected, 100 connected
    private boolean mConnected = false;
    private int mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
    private String mConnectedNetworkTypeName;
    private int mInetCondition = 0;
    private static final int INET_CONDITION_THRESHOLD = 50;

    private boolean mAirplaneMode = false;
    private boolean mLastAirplaneMode = true;

    private Locale mLocale = null;
    private Locale mLastLocale = null;

    // our ui
    Context mContext;
    ArrayList<TextView> mCombinedLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mMobileLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mWifiLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mEmergencyLabelViews = new ArrayList<TextView>();
    ArrayList<SignalCluster> mSignalClusters = new ArrayList<SignalCluster>();
    ArrayList<NetworkSignalChangedCallback> mSignalsChangedCallbacks =
            new ArrayList<NetworkSignalChangedCallback>();

    int[] mLastPhoneSignalIconId;
    int[] mLastDataDirectionIconId;
    int mLastWifiIconId = -1;
    int mLastWimaxIconId = -1;
    int[] mLastCombinedSignalIconId;
    int[] mLastDataTypeIconId;
    String mLastCombinedLabel = "";

    private int mDefaultSubscription;
    boolean[] mShowSpn;
    boolean[] mShowPlmn;
    String[] mSpn;
    String[] mPlmn;

    private boolean mHasMobileDataFeature;

    boolean mDataAndWifiStacked = false;

    public interface SignalCluster {
        void setWifiIndicators(boolean visible, int strengthIcon,
                String contentDescription);
        void setMobileDataIndicators(boolean visible, int strengthIcon,
                int typeIcon, String contentDescription, String typeContentDescription,
                int subscription);
        void setIsAirplaneMode(boolean is, int airplaneIcon);
    }

    public interface NetworkSignalChangedCallback {
        void onWifiSignalChanged(boolean enabled, int wifiSignalIconId,
                boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description);
        void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
                String mobileSignalContentDescriptionId, int dataTypeIconId,
                boolean activityIn, boolean activityOut,
                String dataTypeContentDescriptionId, String description);
        void onAirplaneModeChanged(boolean enabled);
    }

    /**
     * Construct this controller object and register for updates.
     */
    public NetworkController(Context context) {
        mContext = context;
        final Resources res = context.getResources();

        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mHasMobileDataFeature = cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);

        mShowPhoneRSSIForData = res.getBoolean(R.bool.config_showPhoneRSSIForData);
        mShowAtLeastThreeGees = res.getBoolean(R.bool.config_showMin3G);
        mAlwaysShowCdmaRssi = res.getBoolean(
                com.android.internal.R.bool.config_alwaysUseCdmaRssi);

        // set up the default wifi icon, used when no radios have ever appeared
        updateWifiIcons();
        updateWimaxIcons();

        // telephony
        registerPhoneStateListener(context);
        mHspaDataDistinguishable = mContext.getResources().getBoolean(
                R.bool.config_hspa_data_distinguishable);
        mNetworkNameSeparator = mContext.getString(R.string.status_bar_network_name_separator);
        mNetworkNameDefault = mContext.getString(
                com.android.internal.R.string.lockscreen_carrier_default);
        
        // wifi
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Handler handler = new WifiHandler();
        mWifiChannel = new AsyncChannel();
        Messenger wifiMessenger = mWifiManager.getWifiServiceMessenger();
        if (wifiMessenger != null) {
            mWifiChannel.connect(mContext, handler, wifiMessenger);
        }

        // broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mWimaxSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if(mWimaxSupported) {
            filter.addAction(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION);
        }
        context.registerReceiver(this, filter);

        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode();

        mLastLocale = mContext.getResources().getConfiguration().locale;
        init();
    }

    void init() {
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        mDefaultSubscription = TelephonyManager.getDefault().getDefaultSubscription();
        mSignalStrength = new SignalStrength[numPhones];
        mServiceState = new ServiceState[numPhones];
        mSimState = new IccCardConstants.State[numPhones];
        mPhoneSignalIconId = new int[numPhones];
        mDataTypeIconId = new int[numPhones];
        mContentDescriptionPhoneSignal = new String[numPhones];
        mLastPhoneSignalIconId = new int[numPhones];
        mNetworkName = new String[numPhones];
        mLastDataTypeIconId = new int[numPhones];
        mDataConnected = new boolean[numPhones];
        mDataSignalIconId = new int[numPhones];
        mDataDirectionIconId = new int[numPhones];
        mLastDataDirectionIconId = new int[numPhones];
        mLastCombinedSignalIconId = new int[numPhones];
        mCombinedSignalIconId = new int[numPhones];
        mDataActivity = new int[numPhones];
        mContentDescriptionCombinedSignal = new String[numPhones];
        mContentDescriptionDataType = new String[numPhones];
        mCarrierTextSub = new String[numPhones];
        mShowSpn = new boolean[numPhones];
        mShowPlmn = new boolean[numPhones];
        mSpn = new String[numPhones];
        mPlmn = new String[numPhones];
        mMobileLabel = new String[numPhones];

        for (int i=0; i < numPhones; i++) {
            mSignalStrength[i] = new SignalStrength();
            mServiceState[i] = new ServiceState();
            mSimState[i] = IccCardConstants.State.READY;
            // phone_signal
            mPhoneSignalIconId[i] = R.drawable.stat_sys_signal_null;
            mLastPhoneSignalIconId[i] = -1;
            mLastDataTypeIconId[i] = -1;
            mDataConnected[i] = false;
            mLastDataDirectionIconId[i] = -1;
            mLastCombinedSignalIconId[i] = -1;
            mCombinedSignalIconId[i] = 0;
            mDataActivity[i] = TelephonyManager.DATA_ACTIVITY_NONE;
            mNetworkName[i] = mNetworkNameDefault;
            mMobileLabel[i] = "";
        }
    }

    public boolean hasMobileDataFeature() {
        return mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    public boolean isEmergencyOnly(int subscription) {
        return (mServiceState != null && mServiceState[subscription].isEmergencyOnly());
    }

    public void addCombinedLabelView(TextView v) {
        mCombinedLabelViews.add(v);
    }

    public void addMobileLabelView(TextView v) {
        mMobileLabelViews.add(v);
    }

    public void addWifiLabelView(TextView v) {
        mWifiLabelViews.add(v);
    }

    public void addEmergencyLabelView(TextView v) {
        mEmergencyLabelViews.add(v);
    }

    public void addSignalCluster(SignalCluster cluster, int subscription) {
        mSignalClusters.add(cluster);
        refreshSignalCluster(cluster, subscription);
    }

    public void addNetworkSignalChangedCallback(NetworkSignalChangedCallback cb) {
        mSignalsChangedCallbacks.add(cb);
        notifySignalsChangedCallbacks(cb, TelephonyManager.getDefault().getDefaultSubscription());
    }

    void registerPhoneStateListener(Context context) {
        // telephony
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneStateListener = new PhoneStateListener[numPhones];
        for (int i=0; i < numPhones; i++) {
            mPhoneStateListener[i] = getPhoneStateListener(i);
            mPhone.listen(mPhoneStateListener[i],
                              PhoneStateListener.LISTEN_SERVICE_STATE
                            | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                            | PhoneStateListener.LISTEN_CALL_STATE
                            | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                            | PhoneStateListener.LISTEN_DATA_ACTIVITY);
        }
    }

    public void refreshSignalCluster(SignalCluster cluster, int subscription) {
        if (mDemoMode) return;
        cluster.setWifiIndicators(
                // only show wifi in the cluster if connected or if wifi-only
                mWifiEnabled && (mWifiConnected || !mHasMobileDataFeature),
                mWifiIconId,
                mContentDescriptionWifi);

        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is special
            cluster.setMobileDataIndicators(
                    true,
                    mAlwaysShowCdmaRssi ? mPhoneSignalIconId[subscription] : mWimaxIconId,
                    mDataTypeIconId[subscription],
                    mContentDescriptionWimax,
                    mContentDescriptionDataType[subscription], subscription);
        } else {
            // normal mobile data
            cluster.setMobileDataIndicators(
                    mHasMobileDataFeature,
                    mShowPhoneRSSIForData ? mPhoneSignalIconId[subscription]
                        : mDataSignalIconId[subscription],
                    mDataTypeIconId[subscription],
                    mContentDescriptionPhoneSignal[subscription],
                    mContentDescriptionDataType[subscription], subscription);
        }
        cluster.setIsAirplaneMode(mAirplaneMode, mAirplaneIconId);
    }

    void notifySignalsChangedCallbacks(NetworkSignalChangedCallback cb, int subscription) {
        // only show wifi in the cluster if connected or if wifi-only
        boolean wifiEnabled = mWifiEnabled && (mWifiConnected || !mHasMobileDataFeature);
        String wifiDesc = wifiEnabled ?
                mWifiSsid : null;
        boolean wifiIn = wifiEnabled && mWifiSsid != null
                && (mWifiActivity == WifiManager.DATA_ACTIVITY_INOUT
                || mWifiActivity == WifiManager.DATA_ACTIVITY_IN);
        boolean wifiOut = wifiEnabled && mWifiSsid != null
                && (mWifiActivity == WifiManager.DATA_ACTIVITY_INOUT
                || mWifiActivity == WifiManager.DATA_ACTIVITY_OUT);
        cb.onWifiSignalChanged(wifiEnabled, mQSWifiIconId, wifiIn, wifiOut,
                mContentDescriptionWifi, wifiDesc);

        boolean mobileIn = mDataConnected[subscription] &&
                (mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_INOUT
                || mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_IN);
        boolean mobileOut = mDataConnected[subscription] &&
                 (mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_INOUT
                || mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_OUT);
        if (isEmergencyOnly(subscription)) {
            cb.onMobileDataSignalChanged(false, mQSPhoneSignalIconId,
                    mContentDescriptionPhoneSignal[subscription], mQSDataTypeIconId, mobileIn, mobileOut,
                    mContentDescriptionDataType[subscription], null);
        } else {
            if (mIsWimaxEnabled && mWimaxConnected) {
                // Wimax is special
                cb.onMobileDataSignalChanged(true, mQSPhoneSignalIconId,
                        mContentDescriptionPhoneSignal[subscription], mQSDataTypeIconId, mobileIn, mobileOut,
                        mContentDescriptionDataType[subscription], mNetworkName[subscription]);
            } else {
                // Normal mobile data
                cb.onMobileDataSignalChanged(mHasMobileDataFeature, mQSPhoneSignalIconId,
                        mContentDescriptionPhoneSignal[subscription], mQSDataTypeIconId, mobileIn, mobileOut,
                        mContentDescriptionDataType[subscription], mNetworkName[subscription]);
            }
        }
        cb.onAirplaneModeChanged(mAirplaneMode);
    }

    public void setStackedMode(boolean stacked) {
        mDataAndWifiStacked = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            updateWifiState(intent);
            refreshViews(TelephonyManager.getDefault().getDefaultSubscription());
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            updateSimState(intent);
            for (int sub = 0; sub < TelephonyManager.getDefault().getPhoneCount(); sub++) {
                updateDataIcon(sub);
                refreshViews(sub);
            }
        } else if (action.equals(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION)) {
            final int subscription = intent.getIntExtra(MSimConstants.SUBSCRIPTION_KEY, 0);
            Log.d(TAG, "Received SPN update on sub :" + subscription);
            mShowSpn[subscription] = intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false);
            mSpn[subscription] = intent.getStringExtra(TelephonyIntents.EXTRA_SPN);
            mShowPlmn[subscription] = intent.getBooleanExtra(
                    TelephonyIntents.EXTRA_SHOW_PLMN, false);
            mPlmn[subscription] = intent.getStringExtra(TelephonyIntents.EXTRA_PLMN);

            updateNetworkName(mShowSpn[subscription], mSpn[subscription], mShowPlmn[subscription],
                    mPlmn[subscription], subscription);
            updateCarrierText(subscription);
            refreshViews(subscription);
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                 action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            updateConnectivity(intent);
            refreshViews(TelephonyManager.getDefault().getDefaultSubscription());
        } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
            refreshViews(TelephonyManager.getDefault().getDefaultSubscription());
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            refreshLocale();
            updateAirplaneMode();
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                updateCarrierText(i);
            }
            refreshViews(TelephonyManager.getDefault().getDefaultSubscription());
        } else if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            updateWimaxState(intent);
            refreshViews(TelephonyManager.getDefault().getDefaultSubscription());
        }
    }

    private void updateCarrierText(int sub) {
        int textResId = 0;
        if (mAirplaneMode) {
            textResId = com.android.internal.R.string.lockscreen_airplane_mode_on;
        } else {
            if (DEBUG) {
                Log.d(TAG, "updateCarrierText for sub:" + sub + " simState =" + mSimState[sub]);
            }

            switch (mSimState[sub]) {
                case ABSENT:
                case UNKNOWN:
                case NOT_READY:
                    textResId = com.android.internal.R.string.lockscreen_missing_sim_message_short;
                    break;
                case PIN_REQUIRED:
                    textResId = com.android.internal.R.string.lockscreen_sim_locked_message;
                    break;
                case PUK_REQUIRED:
                    textResId = com.android.internal.R.string.lockscreen_sim_puk_locked_message;
                    break;
                case READY:
                    // If the state is ready, set the text as network name.
                    mCarrierTextSub[sub] = mNetworkName[sub];
                    break;
                case PERM_DISABLED:
                    textResId = com.android.internal.
                            R.string.lockscreen_permanent_disabled_sim_message_short;
                    break;
                default:
                    textResId = com.android.internal.R.string.lockscreen_missing_sim_message_short;
                    break;
            }
        }

        if (textResId != 0) {
            mCarrierTextSub[sub] = mContext.getString(textResId);
        }
    }

    // ===== Telephony ==============================================================

    private PhoneStateListener getPhoneStateListener(int subscription) {
        PhoneStateListener mPhoneStateListener = new PhoneStateListener(subscription) {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                if (DEBUG) {
                    Log.d(TAG, "onSignalStrengthsChanged received on subscription :"
                        + mSubscription + "signalStrength=" + signalStrength +
                        ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
                }
                mSignalStrength[mSubscription] = signalStrength;
                updateTelephonySignalStrength(mSubscription);
                refreshViews(mSubscription);
            }

            @Override
            public void onServiceStateChanged(ServiceState state) {
                if (DEBUG) {
                    Log.d(TAG, "onServiceStateChanged received on subscription :"
                        + mSubscription + "state=" + state.getState());
                }
                mServiceState[mSubscription] = state;
                updateTelephonySignalStrength(mSubscription);
                updateDataNetType(mSubscription);
                updateDataIcon(mSubscription);
                updateNetworkName(mShowSpn[mSubscription], mSpn[mSubscription],
                                mShowPlmn[mSubscription], mPlmn[mSubscription], mSubscription);
                updateCarrierText(mSubscription);

                refreshViews(mSubscription);
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (DEBUG) {
                    Log.d(TAG, "onCallStateChanged received on subscription :"
                    + mSubscription + "state=" + state);
                }
                // In cdma, if a voice call is made, RSSI should switch to 1x.
                if (isCdma(mSubscription)) {
                    updateTelephonySignalStrength(mSubscription);
                    refreshViews(mSubscription);
                }
            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                if (DEBUG) {
                    Log.d(TAG, "onDataConnectionStateChanged received on subscription :"
                    + mSubscription + "state=" + state + " type=" + networkType);
                }

                // DSDS case: Data is active only on DDS. Ignore the Data Connection
                // State changed notifications of the other NON-DDS.
                if (mSubscription ==
                        TelephonyManager.getDefault().getPreferredDataSubscription()) {
                    mDataState = state;
                    mDataNetType = networkType;
                }
                updateDataNetType(mSubscription);
                updateDataIcon(mSubscription);
                refreshViews(mSubscription);
            }

            @Override
            public void onDataActivity(int direction) {
                if (DEBUG) {
                    Log.d(TAG, "onDataActivity received on subscription :"
                        + mSubscription + "direction=" + direction);
                }
                mDataActivity[mSubscription] = direction;
                updateDataIcon(mSubscription);
                refreshViews(mSubscription);
            }
        };
        return mPhoneStateListener;
    }

    private final void updateSimState(Intent intent) {
        IccCardConstants.State simState;
        String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        // Obtain the subscription info from intent.
        int sub = intent.getIntExtra(MSimConstants.SUBSCRIPTION_KEY, 0);
        Log.d(TAG, "updateSimState for subscription :" + sub);
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
            simState = IccCardConstants.State.ABSENT;
        }
        else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
            simState = IccCardConstants.State.READY;
        }
        else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
            final String lockedReason =
                    intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
            if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                simState = IccCardConstants.State.PIN_REQUIRED;
            }
            else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                simState = IccCardConstants.State.PUK_REQUIRED;
            }
            else {
                simState = IccCardConstants.State.NETWORK_LOCKED;
            }
        } else {
            simState = IccCardConstants.State.UNKNOWN;
        }
        // Update the sim state and carrier text.
        if (simState != IccCardConstants.State.UNKNOWN && simState != mSimState[sub]) {
            mSimState[sub] = simState;
            updateCarrierText(sub);
            Log.d(TAG, "updateSimState simState =" + mSimState[sub]);
        }
        updateDataIcon(sub);
    }

    private boolean isCdma(int subscription) {
        return (mSignalStrength[subscription] != null) && !mSignalStrength[subscription].isGsm();
    }

    private boolean hasService(int subscription) {
        ServiceState ss = mServiceState[subscription];
        if (ss != null) {
            // Consider the device to be in service if either voice or data service is available.
            // Some SIM cards are marketed as data-only and do not support voice service, and on
            // these SIM cards, we want to show signal bars for data service as well as the "no
            // service" or "emergency calls only" text that indicates that voice is not available.
            switch(ss.getVoiceRegState()) {
                case ServiceState.STATE_POWER_OFF:
                    return false;
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    return ss.getDataRegState() == ServiceState.STATE_IN_SERVICE;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private void updateAirplaneMode() {
        mAirplaneMode = (Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
    }

    private void refreshLocale() {
        mLocale = mContext.getResources().getConfiguration().locale;
    }

    private final void updateTelephonySignalStrength(int subscription) {
        Log.d(TAG, "updateTelephonySignalStrength: subscription =" + subscription);
        if (!hasService(subscription)) {
            if (CHATTY) Log.d(TAG, "updateTelephonySignalStrength: !hasService()");
            mPhoneSignalIconId[subscription] = R.drawable.stat_sys_signal_null;
            mQSPhoneSignalIconId = R.drawable.ic_qs_signal_no_signal;
            mDataSignalIconId[subscription] = R.drawable.stat_sys_signal_null;
        } else {
            if (mSignalStrength[subscription] == null) {
                if (CHATTY) Log.d(TAG, "updateTelephonySignalStrength: mSignalStrength == null");
                mPhoneSignalIconId[subscription] = R.drawable.stat_sys_signal_null;
                mQSPhoneSignalIconId = R.drawable.ic_qs_signal_no_signal;
                mDataSignalIconId[subscription] = R.drawable.stat_sys_signal_null;
                mContentDescriptionPhoneSignal[subscription] = mContext.getString(
                        AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0]);
            } else {
                int iconLevel;
                int[] iconList;
                if (isCdma(subscription) && mAlwaysShowCdmaRssi) {
                    mLastSignalLevel = iconLevel = mSignalStrength[subscription].getCdmaLevel();
                    if(DEBUG) Log.d(TAG, "mAlwaysShowCdmaRssi=" + mAlwaysShowCdmaRssi
                            + " set to cdmaLevel=" + mSignalStrength[subscription].getCdmaLevel()
                            + " instead of level=" + mSignalStrength[subscription].getLevel());
                } else {
                    mLastSignalLevel = iconLevel = mSignalStrength[subscription].getLevel();
                }

                if (isCdma(subscription)) {
                    if (isCdmaEri(subscription)) {
                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[mInetCondition];
                    } else {
                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[mInetCondition];
                    }
                } else {
                    // Though mPhone is a Manager, this call is not an IPC
                    if (mPhone.isNetworkRoaming()) {
                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[mInetCondition];
                    } else {
                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[mInetCondition];
                    }
                }
                mPhoneSignalIconId[subscription] = iconList[iconLevel];
                mQSPhoneSignalIconId =
                        TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH[mInetCondition][iconLevel];
                mContentDescriptionPhoneSignal[subscription] = mContext.getString(
                        AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[iconLevel]);
                mDataSignalIconId[subscription] = TelephonyIcons.DATA_SIGNAL_STRENGTH[mInetCondition][iconLevel];
            }
        }
    }

    private final void updateDataNetType(int subscription) {
        // DSDS case: Data is active only on DDS. Clear the icon for NON-DDS
        int dataSub = TelephonyManager.getDefault().getPreferredDataSubscription();
        if (subscription != dataSub) {
            Log.d(TAG,"updateDataNetType: SUB" + subscription
                    + " is not DDS(=SUB" + dataSub + ")!");
            mDataTypeIconId[subscription] = 0;
        } else {
            if (mIsWimaxEnabled && mWimaxConnected) {
                // wimax is a special 4g network not handled by telephony
                mDataIconList = TelephonyIcons.DATA_4G[mInetCondition];
                mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_4g;
                mQSDataTypeIconId = TelephonyIcons.QS_DATA_4G[mInetCondition];
                mContentDescriptionDataType[subscription] = mContext.getString(
                        R.string.accessibility_data_connection_4g);
            } else {
                Log.d(TAG,"updateDataNetType sub = " + subscription
                        + " mDataNetType = " + mDataNetType);
                switch (mDataNetType) {
                    case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                        if (DEBUG) {
                            Log.e(TAG, "updateDataNetType NETWORK_TYPE_UNKNOWN");
                        }
                        if (!mShowAtLeastThreeGees) {
                            mDataIconList = TelephonyIcons.DATA_G[mInetCondition];
                            mDataTypeIconId[subscription] = 0;
                            mQSDataTypeIconId = 0;
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_gprs);
                            break;
                        } else {
                            // fall through
                        }
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        if (!mShowAtLeastThreeGees) {
                            mDataIconList = TelephonyIcons.DATA_E[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_e;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_E[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_edge);
                            break;
                        } else {
                            // fall through
                        }
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                        mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_3g;
                        mQSDataTypeIconId = TelephonyIcons.QS_DATA_3G[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        if (mHspaDataDistinguishable) {
                            mDataIconList = TelephonyIcons.DATA_H[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_h;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_H[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_3_5g);
                        } else {
                            mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_3g;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_3G[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_3g);
                        }
                        break;
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                        if (!mShowAtLeastThreeGees) {
                            // display 1xRTT for IS95A/B
                            mDataIconList = TelephonyIcons.DATA_1X[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_1x;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_1X[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_cdma);
                            break;
                        } else {
                            // fall through
                        }
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                        if (!mShowAtLeastThreeGees) {
                            mDataIconList = TelephonyIcons.DATA_1X[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_1x;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_1X[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_cdma);
                            break;
                        } else {
                            // fall through
                        }
                    case TelephonyManager.NETWORK_TYPE_EVDO_0: //fall through
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                        mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                        mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_3g;
                        mQSDataTypeIconId = TelephonyIcons.QS_DATA_3G[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        boolean show4GforLTE = mContext.getResources().getBoolean(R.bool.config_show4GForLTE);
                        if (show4GforLTE) {
                            mDataIconList = TelephonyIcons.DATA_4G[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_4g;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_4G[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_4g);
                        } else {
                            mDataIconList = TelephonyIcons.DATA_LTE[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_lte;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_LTE[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_lte);
                        }
                        break;
                    default:
                        if (!mShowAtLeastThreeGees) {
                            mDataIconList = TelephonyIcons.DATA_G[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_g;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_G[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_gprs);
                        } else {
                            mDataIconList = TelephonyIcons.DATA_3G[mInetCondition];
                            mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_3g;
                            mQSDataTypeIconId = TelephonyIcons.QS_DATA_3G[mInetCondition];
                            mContentDescriptionDataType[subscription] = mContext.getString(
                                    R.string.accessibility_data_connection_3g);
                        }
                        break;
                }
            }
        }

        if (isCdma(subscription)) {
            if (isCdmaEri(subscription)) {
                mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_roam;
                mQSDataTypeIconId = TelephonyIcons.QS_DATA_R[mInetCondition];
            }
        } else if (mPhone.isNetworkRoaming(subscription)) {
                mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_roam;
                mQSDataTypeIconId = TelephonyIcons.QS_DATA_R[mInetCondition];
        }
    }

    boolean isCdmaEri(int subscription) {
        if (mServiceState[subscription] != null) {
            final int iconIndex = mServiceState[subscription].getCdmaEriIconIndex();
            if (iconIndex != EriInfo.ROAMING_INDICATOR_OFF) {
                final int iconMode = mServiceState[subscription].getCdmaEriIconMode();
                if (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                        || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH) {
                    return true;
                }
            }
        }
        return false;
    }

    private final void updateDataIcon(int subscription) {
        Log.d(TAG,"updateDataIcon subscription =" + subscription);
        int iconId = 0;
        boolean visible = true;

        int dataSub = TelephonyManager.getDefault().getPreferredDataSubscription();
        Log.d(TAG,"updateDataIcon dataSub =" + dataSub);
        // DSDS case: Data is active only on DDS. Clear the icon for NON-DDS
        if (subscription != dataSub) {
            mDataConnected[subscription] = false;
            Log.d(TAG,"updateDataIconi: SUB" + subscription
                     + " is not DDS.  Clear the mDataConnected Flag and return");
            return;
        }

        Log.d(TAG,"updateDataIcon  when SimState =" + mSimState[subscription]);
        if (mDataNetType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            // If data network type is unknown do not display data icon
            visible = false;
        } else if (!isCdma(subscription)) {
             Log.d(TAG,"updateDataIcon  when gsm mSimState =" + mSimState[subscription]);
            // GSM case, we have to check also the sim state
            if (mSimState[subscription] == IccCardConstants.State.READY ||
                    mSimState[subscription] == IccCardConstants.State.UNKNOWN) {
                if (hasService(subscription) && mDataState == TelephonyManager.DATA_CONNECTED) {
                    switch (mDataActivity[subscription]) {
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            iconId = mDataIconList[1];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            iconId = mDataIconList[2];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            iconId = mDataIconList[3];
                            break;
                        default:
                            iconId = mDataIconList[0];
                            break;
                    }
                    mDataDirectionIconId[subscription] = iconId;
                } else {
                    iconId = 0;
                    visible = false;
                }
            } else {
                Log.d(TAG,"updateDataIcon when no sim");
                iconId = R.drawable.stat_sys_no_sim;
                visible = false; // no SIM? no data
            }
        } else {
            // CDMA case, mDataActivity can be also DATA_ACTIVITY_DORMANT
            if (hasService(subscription) && mDataState == TelephonyManager.DATA_CONNECTED) {
                switch (mDataActivity[subscription]) {
                    case TelephonyManager.DATA_ACTIVITY_IN:
                        iconId = mDataIconList[1];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT:
                        iconId = mDataIconList[2];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT:
                        iconId = mDataIconList[3];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_DORMANT:
                    default:
                        iconId = mDataIconList[0];
                        break;
                }
            } else {
                iconId = 0;
                visible = false;
            }
        }

        mDataDirectionIconId[subscription] = iconId;
        mDataConnected[subscription] = visible;
        Log.d(TAG,"updateDataIcon when mDataConnected =" + mDataConnected[subscription]);
    }

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn,
            int subscription) {
        if (DEBUG) {
            Log.d(TAG, "updateNetworkName showSpn=" + showSpn + " spn=" + spn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
            str.append(plmn);
            something = true;
        }
        if (showSpn && spn != null) {
            if (something) {
                str.append(mNetworkNameSeparator);
            }
            str.append(spn);
            something = true;
        }
        if (something) {
            mNetworkName[subscription] = str.toString();
        } else {
            mNetworkName[subscription] = mNetworkNameDefault;
        }
        Log.d(TAG, "mNetworkName[subscription] " + mNetworkName[subscription]
                                                      + "subscription " + subscription);
    }

    // ===== Wifi ===================================================================

    class WifiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        mWifiChannel.sendMessage(Message.obtain(this,
                                AsyncChannel.CMD_CHANNEL_FULL_CONNECTION));
                    } else {
                        Log.e(TAG, "Failed to connect to wifi");
                    }
                    break;
                case WifiManager.DATA_ACTIVITY_NOTIFICATION:
                    if (msg.arg1 != mWifiActivity) {
                        mWifiActivity = msg.arg1;
                        refreshViews(TelephonyManager.getDefault().
                                getPreferredDataSubscription());
                    }
                    break;
                default:
                    //Ignore
                    break;
            }
        }
    }

    private void updateWifiState(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            mWifiEnabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;

        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            final NetworkInfo networkInfo = (NetworkInfo)
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean wasConnected = mWifiConnected;
            mWifiConnected = networkInfo != null && networkInfo.isConnected();
            // If we just connected, grab the inintial signal strength and ssid
            if (mWifiConnected && !wasConnected) {
                // try getting it out of the intent first
                WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                if (info == null) {
                    info = mWifiManager.getConnectionInfo();
                }
                if (info != null) {
                    mWifiSsid = huntForSsid(info);
                } else {
                    mWifiSsid = null;
                }
            } else if (!mWifiConnected) {
                mWifiSsid = null;
            }
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            mWifiRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            mWifiLevel = WifiManager.calculateSignalLevel(
                    mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
        }

        updateWifiIcons();
    }

    private void updateWifiIcons() {
        if (mWifiConnected) {
            mWifiIconId = WifiIcons.WIFI_SIGNAL_STRENGTH[mInetCondition][mWifiLevel];
            mQSWifiIconId = WifiIcons.QS_WIFI_SIGNAL_STRENGTH[mInetCondition][mWifiLevel];
            mContentDescriptionWifi = mContext.getString(
                    AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH[mWifiLevel]);
        } else {
            if (mDataAndWifiStacked) {
                mWifiIconId = 0;
                mQSWifiIconId = 0;
            } else {
                mWifiIconId = mWifiEnabled ? R.drawable.stat_sys_wifi_signal_null : 0;
                mQSWifiIconId = mWifiEnabled ? R.drawable.ic_qs_wifi_no_network : 0;
            }
            mContentDescriptionWifi = mContext.getString(R.string.accessibility_no_wifi);
        }
    }

    private String huntForSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        // OK, it's not in the connectionInfo; we have to go hunting for it
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration net : networks) {
            if (net.networkId == info.getNetworkId()) {
                return net.SSID;
            }
        }
        return null;
    }


    // ===== Wimax ===================================================================
    private final void updateWimaxState(Intent intent) {
        final String action = intent.getAction();
        boolean wasConnected = mWimaxConnected;
        if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION)) {
            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.EXTRA_4G_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mIsWimaxEnabled = (wimaxStatus ==
                    WimaxManagerConstants.NET_4G_STATE_ENABLED);
        } else if (action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION)) {
            mWimaxSignal = intent.getIntExtra(WimaxManagerConstants.EXTRA_NEW_SIGNAL_LEVEL, 0);
        } else if (action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            mWimaxState = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxExtraState = intent.getIntExtra(
                    WimaxManagerConstants.EXTRA_WIMAX_STATE_DETAIL,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxConnected = (mWimaxState ==
                    WimaxManagerConstants.WIMAX_STATE_CONNECTED);
            mWimaxIdle = (mWimaxExtraState == WimaxManagerConstants.WIMAX_IDLE);
        }
        updateDataNetType(TelephonyManager.getDefault().getDefaultSubscription());
        updateWimaxIcons();
    }

    private void updateWimaxIcons() {
        if (mIsWimaxEnabled) {
            if (mWimaxConnected) {
                if (mWimaxIdle)
                    mWimaxIconId = WimaxIcons.WIMAX_IDLE;
                else
                    mWimaxIconId = WimaxIcons.WIMAX_SIGNAL_STRENGTH[mInetCondition][mWimaxSignal];
                mContentDescriptionWimax = mContext.getString(
                        AccessibilityContentDescriptions.WIMAX_CONNECTION_STRENGTH[mWimaxSignal]);
            } else {
                mWimaxIconId = WimaxIcons.WIMAX_DISCONNECTED;
                mContentDescriptionWimax = mContext.getString(R.string.accessibility_no_wimax);
            }
        } else {
            mWimaxIconId = 0;
        }
    }

    // ===== Full or limited Internet connectivity ==================================

    private void updateConnectivity(Intent intent) {
        if (CHATTY) {
            Log.d(TAG, "updateConnectivity: intent=" + intent);
        }

        final ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = connManager.getActiveNetworkInfo();

        // Are we connected at all, by any interface?
        mConnected = info != null && info.isConnected();
        if (mConnected) {
            mConnectedNetworkType = info.getType();
            mConnectedNetworkTypeName = info.getTypeName();
        } else {
            mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
            mConnectedNetworkTypeName = null;
        }

        int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);

        if (CHATTY) {
            Log.d(TAG, "updateConnectivity: networkInfo=" + info);
            Log.d(TAG, "updateConnectivity: connectionStatus=" + connectionStatus);
        }

        mInetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);

        if (info != null && info.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
            mBluetoothTethered = info.isConnected();
        } else {
            mBluetoothTethered = false;
        }

        // We want to update all the icons, all at once, for any condition change
        updateWimaxIcons();
        for (int sub = 0; sub < TelephonyManager.getDefault().getPhoneCount(); sub++) {
            updateDataNetType(sub);
            updateDataIcon(sub);
            updateTelephonySignalStrength(sub);
        }
        updateWifiIcons();
    }


    // ===== Update the views =======================================================

    void refreshViews(int subscription) {
        Context context = mContext;

        int combinedSignalIconId = 0;
        String combinedLabel = "";
        String wifiLabel = "";
        mMobileLabel[subscription] = "";
        int N;
        final boolean emergencyOnly = isEmergencyOnly(subscription);

        if (!mHasMobileDataFeature) {
            mDataSignalIconId[subscription] = mPhoneSignalIconId[subscription] = 0;
            mQSPhoneSignalIconId = 0;
            mMobileLabel[subscription] = "";
        } else {
            // We want to show the carrier name if in service and either:
            //   - We are connected to mobile data, or
            //   - We are not connected to mobile data, as long as the *reason* packets are not
            //     being routed over that link is that we have better connectivity via wifi.
            // If data is disconnected for some other reason but wifi (or ethernet/bluetooth)
            // is connected, we show nothing.
            // Otherwise (nothing connected) we show "No internet connection".

            if (mDataConnected[subscription]) {
                mMobileLabel[subscription] = mNetworkName[subscription];
            } else if (mConnected || emergencyOnly) {
                if (hasService(subscription) || emergencyOnly) {
                    // The isEmergencyOnly test covers the case of a phone with no SIM
                    mMobileLabel[subscription] = mNetworkName[subscription];
                } else {
                    // Tablets, basically
                    mMobileLabel[subscription] = "";
                }
            } else {
                mMobileLabel[subscription]
                    = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }

            // Now for things that should only be shown when actually using mobile data.
            if (mDataConnected[subscription]) {
                mCombinedSignalIconId[subscription] = mDataSignalIconId[subscription];

                combinedLabel = mMobileLabel[subscription];
                mContentDescriptionCombinedSignal[subscription] =
                        mContentDescriptionDataType[subscription];
            }
        }

        if (mWifiConnected) {
            if (mWifiSsid == null) {
                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_wifi_nossid);
            } else {
                wifiLabel = mWifiSsid;
                if (DEBUG) {
                    wifiLabel += "xxxxXXXXxxxxXXXX";
                }
            }

            combinedLabel = wifiLabel;
            mCombinedSignalIconId[subscription] = mWifiIconId; // set by updateWifiIcons()
            mContentDescriptionCombinedSignal[subscription] = mContentDescriptionWifi;
        } else {
            if (mHasMobileDataFeature) {
                wifiLabel = "";
            } else {
                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }
        }

        if (mBluetoothTethered) {
            combinedLabel = mContext.getString(R.string.bluetooth_tethered);
            mCombinedSignalIconId[subscription] = mBluetoothTetherIconId;
            mContentDescriptionCombinedSignal[subscription] = mContext.getString(
                    R.string.accessibility_bluetooth_tether);
        }

        final boolean ethernetConnected = (mConnectedNetworkType == ConnectivityManager.TYPE_ETHERNET);
        if (ethernetConnected) {
            combinedLabel = context.getString(R.string.ethernet_label);
        }

        if (mAirplaneMode &&
                (mServiceState[subscription] == null || (!hasService(subscription)
                    && !mServiceState[subscription].isEmergencyOnly()))) {
            // Only display the flight-mode icon if not in "emergency calls only" mode.

            // look again; your radios are now airplanes
            mContentDescriptionPhoneSignal[subscription] = mContext.getString(
                    R.string.accessibility_airplane_mode);
            mAirplaneIconId = FLIGHT_MODE_ICON;
            mPhoneSignalIconId[subscription] = mDataSignalIconId[subscription]
                    = mDataTypeIconId[subscription] = mQSDataTypeIconId = 0;
            mQSPhoneSignalIconId = 0;

            // combined values from connected wifi take precedence over airplane mode
            if (mWifiConnected) {
                // Suppress "No internet connection." from mobile if wifi connected.
                mMobileLabel[subscription] = "";
            } else {
                if (mHasMobileDataFeature) {
                    // let the mobile icon show "No internet connection."
                    wifiLabel = "";
                } else {
                    wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
                    combinedLabel = wifiLabel;
                }
                mContentDescriptionCombinedSignal[subscription] = mContentDescriptionPhoneSignal[subscription];
                mCombinedSignalIconId[subscription] = mDataSignalIconId[subscription];
            }
        }
        else if (!mDataConnected[subscription] && !mWifiConnected && !mBluetoothTethered &&
                !mWimaxConnected && !ethernetConnected) {
            // pretty much totally disconnected

            combinedLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            // On devices without mobile radios, we want to show the wifi icon
            mCombinedSignalIconId[subscription] =
                mHasMobileDataFeature ? mDataSignalIconId[subscription] : mWifiIconId;
            mContentDescriptionCombinedSignal[subscription] = mHasMobileDataFeature
                ? mContentDescriptionDataType[subscription] : mContentDescriptionWifi;
        }
        if (!mDataConnected[subscription]) {
            Log.d(TAG, "refreshViews: Data not connected!! Set no data type icon / Roaming for"
                    + " subscription: " + subscription);

            mDataTypeIconId[subscription] = 0;
            mQSDataTypeIconId = 0;
            if (isCdma(subscription)) {
                if (isCdmaEri(subscription)) {
                    mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_roam;
                    mQSDataTypeIconId = TelephonyIcons.QS_DATA_R[mInetCondition];
                }
            } else if (mPhone.isNetworkRoaming(subscription)) {
                mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_roam;
                mQSDataTypeIconId = TelephonyIcons.QS_DATA_R[mInetCondition];
            }
        }

        if (DEBUG) {
            Log.d(TAG, "refreshViews connected={"
                    + (mWifiConnected?" wifi":"")
                    + (mDataConnected[subscription]?" data":"")
                    + " } level="
                    + ((mSignalStrength[subscription] == null)?"??":Integer.toString
                            (mSignalStrength[subscription].getLevel()))
                    + " combinedSignalIconId=0x"
                    + Integer.toHexString(mCombinedSignalIconId[subscription])
                    + "/" + getResourceName(combinedSignalIconId)
                    + " mobileLabel=" + mMobileLabel[subscription]
                    + " wifiLabel=" + wifiLabel
                    + " emergencyOnly=" + emergencyOnly
                    + " combinedLabel=" + combinedLabel
                    + " mAirplaneMode=" + mAirplaneMode
                    + " mDataActivity=" + mDataActivity[subscription]
                    + " mPhoneSignalIconId=0x" + Integer.toHexString(mPhoneSignalIconId[subscription])
                    + " mQSPhoneSignalIconId=0x" + Integer.toHexString(mQSPhoneSignalIconId)
                    + " mDataDirectionIconId=0x" + Integer.toHexString(mDataDirectionIconId[subscription])
                    + " mDataSignalIconId=0x" + Integer.toHexString(mDataSignalIconId[subscription])
                    + " mDataTypeIconId=0x" + Integer.toHexString(mDataTypeIconId[subscription])
                    + " mQSDataTypeIconId=0x" + Integer.toHexString(mQSDataTypeIconId)
                    + " mWifiIconId=0x" + Integer.toHexString(mWifiIconId)
                    + " mQSWifiIconId=0x" + Integer.toHexString(mQSWifiIconId)
                    + " mBluetoothTetherIconId=0x" + Integer.toHexString(mBluetoothTetherIconId));
        }

        // update QS
        for (NetworkSignalChangedCallback cb : mSignalsChangedCallbacks) {
            notifySignalsChangedCallbacks(cb, subscription);
        }

        if (mLastPhoneSignalIconId[subscription]          != mPhoneSignalIconId[subscription]
         || mLastWifiIconId                 != mWifiIconId
         || mLastWimaxIconId                != mWimaxIconId
         || mLastDataTypeIconId[subscription] != mDataTypeIconId[subscription]
         || mLastAirplaneMode               != mAirplaneMode
         || mLastLocale                     != mLocale)
        {
            // NB: the mLast*s will be updated later
            for (SignalCluster cluster : mSignalClusters) {
                refreshSignalCluster(cluster, subscription);
            }
        }

        if (mLastAirplaneMode != mAirplaneMode) {
            mLastAirplaneMode = mAirplaneMode;
        }

        if (mLastLocale != mLocale) {
            mLastLocale = mLocale;
        }

        // the phone icon on phones
        if (mLastPhoneSignalIconId[subscription] != mPhoneSignalIconId[subscription]) {
            mLastPhoneSignalIconId[subscription] = mPhoneSignalIconId[subscription];
        }

        // the data icon on phones
        if (mLastDataDirectionIconId[subscription] != mDataDirectionIconId[subscription]) {
            mLastDataDirectionIconId[subscription] = mDataDirectionIconId[subscription];
        }

        // the wifi icon on phones
        if (mLastWifiIconId != mWifiIconId) {
            mLastWifiIconId = mWifiIconId;
        }

        // the wimax icon on phones
        if (mLastWimaxIconId != mWimaxIconId) {
            mLastWimaxIconId = mWimaxIconId;
        }
        // the combined data signal icon
        if (mLastCombinedSignalIconId[subscription] != mCombinedSignalIconId[subscription]) {
            mLastCombinedSignalIconId[subscription] = mCombinedSignalIconId[subscription];
        }

        // the data network type overlay
        if (mLastDataTypeIconId[subscription] != mDataTypeIconId[subscription]) {
            mLastDataTypeIconId[subscription] = mDataTypeIconId[subscription];
        }

        // the combinedLabel in the notification panel
        if (!mLastCombinedLabel.equals(combinedLabel)) {
            mLastCombinedLabel = combinedLabel;
            N = mCombinedLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mCombinedLabelViews.get(i);
                v.setText(combinedLabel);
            }
        }

        // wifi label
        N = mWifiLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mWifiLabelViews.get(i);
            v.setText(wifiLabel);
            if ("".equals(wifiLabel)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }

        // mobile label
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        String carrierName = "";
        for (int i = 0; i < numPhones; i++) {
            carrierName = mMobileLabel[i];
            if (i != numPhones-1) {
                carrierName = carrierName + mNetworkNameSeparator;
            }
        }

        N = mMobileLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mMobileLabelViews.get(i);
            v.setText(carrierName);
            if ("".equals(carrierName)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }

        // e-call label
        N = mEmergencyLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mEmergencyLabelViews.get(i);
            if (!emergencyOnly) {
                v.setVisibility(View.GONE);
            } else {
                v.setText(mMobileLabel[subscription]); // comes from the telephony stack
                v.setVisibility(View.VISIBLE);
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args, int subscription) {
        pw.println("NetworkController state:");
        pw.println(String.format("  %s network type %d (%s)",
                mConnected?"CONNECTED":"DISCONNECTED",
                mConnectedNetworkType, mConnectedNetworkTypeName));
        pw.println("  - telephony ------");
        pw.print("  hasVoiceCallingFeature()=");
        pw.println(hasVoiceCallingFeature());
        pw.print("  hasService()=");
        pw.println(hasService(subscription));
        pw.print("  mHspaDataDistinguishable=");
        pw.println(mHspaDataDistinguishable);
        pw.print("  mDataConnected=");
        pw.println(mDataConnected[subscription]);
        pw.print("  mSimState=");
        pw.println(mSimState[subscription]);
        pw.print("  mPhoneState=");
        pw.println(mPhoneState);
        pw.print("  mDataState=");
        pw.println(mDataState);
        pw.print("  mDataActivity=");
        pw.println(mDataActivity[subscription]);
        pw.print("  mDataNetType=");
        pw.print(mDataNetType);
        pw.print("/");
        pw.println(TelephonyManager.getNetworkTypeName(mDataNetType));
        pw.print("  mServiceState=");
        pw.println(mServiceState[subscription]);
        pw.print("  mSignalStrength=");
        pw.println(mSignalStrength[subscription]);
        pw.print("  mLastSignalLevel=");
        pw.println(mLastSignalLevel);
        pw.print("  mNetworkName=");
        pw.println(mNetworkName[subscription]);
        pw.print("  mNetworkNameDefault=");
        pw.println(mNetworkNameDefault);
        pw.print("  mNetworkNameSeparator=");
        pw.println(mNetworkNameSeparator.replace("\n","\\n"));
        pw.print("  mPhoneSignalIconId=0x");
        pw.print(Integer.toHexString(mPhoneSignalIconId[subscription]));
        pw.print("/");
        pw.print("  mQSPhoneSignalIconId=0x");
        pw.print(Integer.toHexString(mQSPhoneSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mPhoneSignalIconId[subscription]));
        pw.print("  mDataDirectionIconId=");
        pw.print(Integer.toHexString(mDataDirectionIconId[subscription]));
        pw.print("/");
        pw.println(getResourceName(mDataDirectionIconId[subscription]));
        pw.print("  mDataSignalIconId=");
        pw.print(Integer.toHexString(mDataSignalIconId[subscription]));
        pw.print("/");
        pw.println(getResourceName(mDataSignalIconId[subscription]));
        pw.print("  mDataTypeIconId=");
        pw.print(Integer.toHexString(mDataTypeIconId[subscription]));
        pw.print("/");
        pw.println(getResourceName(mDataTypeIconId[subscription]));
        pw.print("  mQSDataTypeIconId=");
        pw.print(Integer.toHexString(mQSDataTypeIconId));
        pw.print("/");
        pw.println(getResourceName(mQSDataTypeIconId));

        pw.println("  - wifi ------");
        pw.print("  mWifiEnabled=");
        pw.println(mWifiEnabled);
        pw.print("  mWifiConnected=");
        pw.println(mWifiConnected);
        pw.print("  mWifiRssi=");
        pw.println(mWifiRssi);
        pw.print("  mWifiLevel=");
        pw.println(mWifiLevel);
        pw.print("  mWifiSsid=");
        pw.println(mWifiSsid);
        pw.println(String.format("  mWifiIconId=0x%08x/%s",
                    mWifiIconId, getResourceName(mWifiIconId)));
        pw.println(String.format("  mQSWifiIconId=0x%08x/%s",
                    mQSWifiIconId, getResourceName(mQSWifiIconId)));
        pw.print("  mWifiActivity=");
        pw.println(mWifiActivity);

        if (mWimaxSupported) {
            pw.println("  - wimax ------");
            pw.print("  mIsWimaxEnabled="); pw.println(mIsWimaxEnabled);
            pw.print("  mWimaxConnected="); pw.println(mWimaxConnected);
            pw.print("  mWimaxIdle="); pw.println(mWimaxIdle);
            pw.println(String.format("  mWimaxIconId=0x%08x/%s",
                        mWimaxIconId, getResourceName(mWimaxIconId)));
            pw.println(String.format("  mWimaxSignal=%d", mWimaxSignal));
            pw.println(String.format("  mWimaxState=%d", mWimaxState));
            pw.println(String.format("  mWimaxExtraState=%d", mWimaxExtraState));
        }

        pw.println("  - Bluetooth ----");
        pw.print("  mBtReverseTethered=");
        pw.println(mBluetoothTethered);

        pw.println("  - connectivity ------");
        pw.print("  mInetCondition=");
        pw.println(mInetCondition);

        pw.println("  - icons ------");
        pw.print("  mLastPhoneSignalIconId=0x");
        pw.print(Integer.toHexString(mLastPhoneSignalIconId[subscription]));
        pw.print("/");
        pw.println(getResourceName(mLastPhoneSignalIconId[subscription]));
        pw.print("  mLastDataDirectionIconId=0x");
        pw.print(Integer.toHexString(mLastDataDirectionIconId[subscription]));
        pw.print("/");
        pw.println(getResourceName(mLastDataDirectionIconId[subscription]));
        pw.print("  mLastWifiIconId=0x");
        pw.print(Integer.toHexString(mLastWifiIconId));
        pw.print("/");
        pw.println(getResourceName(mLastWifiIconId));
        pw.print("  mLastCombinedSignalIconId=0x");
        pw.print(Integer.toHexString(mLastCombinedSignalIconId[subscription]));
        pw.print("/");
        pw.println(getResourceName(mLastCombinedSignalIconId[subscription]));
        pw.print("  mLastDataTypeIconId=0x");
        pw.print(Integer.toHexString(mLastDataTypeIconId[subscription]));
        pw.print("/");
        pw.println(getResourceName(mLastDataTypeIconId[subscription]));
        pw.print("  mLastCombinedLabel=");
        pw.print(mLastCombinedLabel);
        pw.println("");
    }

    private String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private boolean mDemoMode;
    private int mDemoInetCondition;
    private int mDemoWifiLevel;
    private int mDemoDataTypeIconId;
    private int mDemoMobileLevel;

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            mDemoMode = true;
            mDemoWifiLevel = mWifiLevel;
            mDemoInetCondition = mInetCondition;
            mDemoDataTypeIconId =
                    mDataTypeIconId[TelephonyManager.getDefault().getDefaultSubscription()];
            mDemoMobileLevel = mLastSignalLevel;
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            for (SignalCluster cluster : mSignalClusters) {
                refreshSignalCluster(cluster,
                        TelephonyManager.getDefault().getDefaultSubscription());
            }
        } else if (mDemoMode && command.equals(COMMAND_NETWORK)) {
            String airplane = args.getString("airplane");
            if (airplane != null) {
                boolean show = airplane.equals("show");
                for (SignalCluster cluster : mSignalClusters) {
                    cluster.setIsAirplaneMode(show, FLIGHT_MODE_ICON);
                }
            }
            String fully = args.getString("fully");
            if (fully != null) {
                mDemoInetCondition = Boolean.parseBoolean(fully) ? 1 : 0;
            }
            String wifi = args.getString("wifi");
            if (wifi != null) {
                boolean show = wifi.equals("show");
                String level = args.getString("level");
                if (level != null) {
                    mDemoWifiLevel = level.equals("null") ? -1
                            : Math.min(Integer.parseInt(level), WifiIcons.WIFI_LEVEL_COUNT - 1);
                }
                int iconId = mDemoWifiLevel < 0 ? R.drawable.stat_sys_wifi_signal_null
                        : WifiIcons.WIFI_SIGNAL_STRENGTH[mDemoInetCondition][mDemoWifiLevel];
                for (SignalCluster cluster : mSignalClusters) {
                    cluster.setWifiIndicators(
                            show,
                            iconId,
                            "Demo");
                }
            }
            String mobile = args.getString("mobile");
            if (mobile != null) {
                boolean show = mobile.equals("show");
                String datatype = args.getString("datatype");
                if (datatype != null) {
                    mDemoDataTypeIconId =
                            datatype.equals("1x") ? R.drawable.stat_sys_data_fully_connected_1x :
                            datatype.equals("3g") ? R.drawable.stat_sys_data_fully_connected_3g :
                            datatype.equals("4g") ? R.drawable.stat_sys_data_fully_connected_4g :
                            datatype.equals("e") ? R.drawable.stat_sys_data_fully_connected_e :
                            datatype.equals("g") ? R.drawable.stat_sys_data_fully_connected_g :
                            datatype.equals("h") ? R.drawable.stat_sys_data_fully_connected_h :
                            datatype.equals("lte") ? R.drawable.stat_sys_data_fully_connected_lte :
                            datatype.equals("roam")
                                    ? R.drawable.stat_sys_data_fully_connected_roam :
                            0;
                }
                int[][] icons = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH;
                String level = args.getString("level");
                if (level != null) {
                    mDemoMobileLevel = level.equals("null") ? -1
                            : Math.min(Integer.parseInt(level), icons[0].length - 1);
                }
                int iconId = mDemoMobileLevel < 0 ? R.drawable.stat_sys_signal_null :
                        icons[mDemoInetCondition][mDemoMobileLevel];
                for (SignalCluster cluster : mSignalClusters) {
                    cluster.setMobileDataIndicators(
                            show,
                            iconId,
                            mDemoDataTypeIconId,
                            "Demo",
                            "Demo", TelephonyManager.getDefault().getDefaultSubscription());
                }
            }
        }
    }
}
