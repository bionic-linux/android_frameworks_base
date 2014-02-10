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
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.util.AsyncChannel;
import com.android.systemui.DemoMode;
import com.android.systemui.R;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NetworkController extends BroadcastReceiver implements DemoMode {
    // debug
    static final String TAG = "StatusBar.NetworkController";
    static final boolean DEBUG = false;
    static final boolean CHATTY = false; // additional diagnostics, but not logspew

    int mSubCount = 0;
    private boolean[] mIsRoaming;
    private int[] mIsRoamingId;

    private static final int FLIGHT_MODE_ICON = R.drawable.stat_sys_signal_flightmode;

    // telephony
    boolean mHspaDataDistinguishable;
    final TelephonyManager mPhone;
    boolean[] mDataConnected;
    IccCardConstants.State[] mSimState;
    int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    int[] mDataNetType;
    int[] mDataState;
    int[] mDataActivity;
    ServiceState[] mServiceState;
    SignalStrength[] mSignalStrength;
    PhoneStateListener[] mPhoneStateListener;
    String[] mNetworkName;
    String mNetworkNameDefault;
    String mNetworkNameSeparator;
    int[] mPhoneSignalIconId;
    int[] mQSPhoneSignalIconId;
    int[] mDataDirectionIconId; // data + data direction on phones
    int[] mDataSignalIconId;
    int[] mDataTypeIconId;
    int[] mQSDataTypeIconId;
    int mAirplaneIconId;
    boolean mDataActive;
    int mLastSignalLevel;
    boolean mShowPhoneRSSIForData = false;
    boolean mShowAtLeastThreeGees = false;
    boolean mAlwaysShowCdmaRssi = false;

    String[] mContentDescriptionPhoneSignal;
    String mContentDescriptionWifi;
    String mContentDescriptionWimax;
    String mContentDescriptionCombinedSignal;
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
    int mLastCombinedSignalIconId = -1;
    int[] mLastDataTypeIconId;
    String mLastCombinedLabel = "";

    private boolean mHasMobileDataFeature;

    boolean mDataAndWifiStacked = false;

    public interface SignalCluster {
        void setWifiIndicators(boolean visible, int strengthIcon,
                String contentDescription);
        void setMobileDataIndicators(int subscription, boolean visible, int activityIcon, int strengthIcon,
                int typeIcon, String contentDescription, String typeContentDescription,
                boolean roaming, int roamingId);
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
        mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        mHspaDataDistinguishable = mContext.getResources().getBoolean(
                R.bool.config_hspa_data_distinguishable);
        mNetworkNameSeparator = mContext.getString(R.string.status_bar_network_name_separator);
        mNetworkNameDefault = mContext.getString(
                com.android.internal.R.string.lockscreen_carrier_default);

        mSubCount = SIMHelper.getNumOfSubscription();
        mIsRoaming = new boolean[mSubCount];
        mIsRoamingId = new int[mSubCount];
        mSignalStrength = new SignalStrength[mSubCount];
        mServiceState = new ServiceState[mSubCount];
        mDataNetType = new int[mSubCount];
        mDataState = new int[mSubCount];
        mDataConnected = new boolean[mSubCount];
        mSimState = new IccCardConstants.State[mSubCount];
        mDataDirectionIconId = new int[mSubCount];
        mDataActivity = new int[mSubCount];
        mContentDescriptionPhoneSignal = new String[mSubCount];
        mDataSignalIconId = new int[mSubCount];
        mContentDescriptionDataType = new String[mSubCount];
        mNetworkName = new String[mSubCount];
        mPhoneSignalIconId = new int[mSubCount];
        mQSPhoneSignalIconId = new int[mSubCount];
        mDataTypeIconId = new int[mSubCount];
        mQSDataTypeIconId = new int[mSubCount];
        mLastPhoneSignalIconId = new int[mSubCount];
        mLastDataDirectionIconId = new int[mSubCount];
        mLastDataTypeIconId = new int[mSubCount];
        mPhoneStateListener = new PhoneStateListener[mSubCount];
        for (int i = 0 ; i < mSubCount ; i++) {
            mDataNetType[i] = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            mDataState[i] = TelephonyManager.DATA_DISCONNECTED;
            mSimState[i] = IccCardConstants.State.READY;
            mDataActivity[i] = TelephonyManager.DATA_ACTIVITY_NONE;
            mNetworkName[i] = mNetworkNameDefault;
            mLastPhoneSignalIconId[i] = -1;
            mLastDataDirectionIconId[i] = -1;
            mLastDataTypeIconId[i] = -1;
            mPhoneStateListener[i] = getPhoneStateListener(i);

            mPhone.listen(mPhoneStateListener[i],
                PhoneStateListener.LISTEN_SERVICE_STATE
              | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
              | PhoneStateListener.LISTEN_CALL_STATE
              | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
              | PhoneStateListener.LISTEN_DATA_ACTIVITY);
        }

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
        filter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        filter.addAction(TelephonyIntents.ACTION_SIMINFO_UPDATED);

        context.registerReceiver(this, filter);

        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode();

        mLastLocale = mContext.getResources().getConfiguration().locale;
    }

    public boolean hasMobileDataFeature() {
        return mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    public boolean isEmergencyOnly() {
        return (mServiceState[SIMHelper.SUBSCRIPTION_INDEX_DEFAULT] != null && mServiceState[SIMHelper.SUBSCRIPTION_INDEX_DEFAULT].isEmergencyOnly());
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

    public void addSignalCluster(SignalCluster cluster) {
        mSignalClusters.add(cluster);
        refreshSignalCluster(cluster);
    }

    public void addNetworkSignalChangedCallback(NetworkSignalChangedCallback cb) {
        mSignalsChangedCallbacks.add(cb);
        notifySignalsChangedCallbacks(cb);
    }

    public void refreshSignalCluster(SignalCluster cluster) {
        if (mDemoMode) return;
        cluster.setWifiIndicators(
                // only show wifi in the cluster if connected or if wifi-only
                mWifiEnabled && (mWifiConnected || !mHasMobileDataFeature),
                mWifiIconId,
                mContentDescriptionWifi);

        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is special
            for (int i = 0; i < mSubCount ; i++) {
                cluster.setMobileDataIndicators(
                    i,
                    true,
                    mDataDirectionIconId[i],
                    mAlwaysShowCdmaRssi ? mPhoneSignalIconId[i] : mWimaxIconId,
                    mDataTypeIconId[i],
                    mContentDescriptionWimax,
                    mContentDescriptionDataType[i],
                    mIsRoaming[i],
                    mIsRoamingId[i]);
            }
        } else {
            // normal mobile data
            for (int i = 0; i < mSubCount ; i++) {
                cluster.setMobileDataIndicators(
                    i,
                    mHasMobileDataFeature,
                    mDataDirectionIconId[i],
                    mShowPhoneRSSIForData ? mPhoneSignalIconId[i] : mDataSignalIconId[i],
                    mDataTypeIconId[i],
                    mContentDescriptionPhoneSignal[i],
                    mContentDescriptionDataType[i],
                    mIsRoaming[i],
                    mIsRoamingId[i]);
            }
        }
        cluster.setIsAirplaneMode(mAirplaneMode, mAirplaneIconId);
    }

    void notifySignalsChangedCallbacks(NetworkSignalChangedCallback cb) {
        final int subscription = SIMHelper.SUBSCRIPTION_INDEX_DEFAULT;
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

        boolean mobileIn = mDataConnected[subscription] 
            && (mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_INOUT
                || mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_IN);
        boolean mobileOut = mDataConnected[subscription] 
            && (mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_INOUT
                || mDataActivity[subscription] == TelephonyManager.DATA_ACTIVITY_OUT);
        if (isEmergencyOnly()) {
            cb.onMobileDataSignalChanged(false, mQSPhoneSignalIconId[subscription],
                    mContentDescriptionPhoneSignal[subscription], mQSDataTypeIconId[subscription], mobileIn, mobileOut,
                    mContentDescriptionDataType[subscription], null);
        } else {
            if (mIsWimaxEnabled && mWimaxConnected) {
                // Wimax is special
                cb.onMobileDataSignalChanged(true, mQSPhoneSignalIconId[subscription],
                        mContentDescriptionPhoneSignal[subscription], mQSDataTypeIconId[subscription], mobileIn, mobileOut,
                        mContentDescriptionDataType[subscription], mNetworkName[subscription]);
            } else {
                // Normal mobile data
                cb.onMobileDataSignalChanged(mHasMobileDataFeature, mQSPhoneSignalIconId[subscription],
                        mContentDescriptionPhoneSignal[subscription], mQSDataTypeIconId[subscription], mobileIn, mobileOut,
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
        if (DEBUG) Log.d(TAG, "onReceive, intent action = " + action);
        if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            updateWifiState(intent);
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            int subscription = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
            updateTelephonySignalStrength();
            updateDataNetType(subscription);
            updateSimState(subscription, intent);
            updateDataIcon(subscription);
            refreshViews(subscription);
        } else if (action.equals(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION)) {
            int subscription = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
            updateNetworkName(subscription,
                        intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                        intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
            refreshViews(subscription);
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                 action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            updateConnectivity(intent);
            refreshViews();
        } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
            refreshLocale();
            refreshViews();
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            refreshLocale();
            updateAirplaneMode();
            refreshViews();
        } else if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            updateWimaxState(intent);
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE)) {
            SIMHelper.updateSIMInfos(context);
            updateDataNetType();
            updateTelephonySignalStrength();
            updateOperatorInfo();
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SIMINFO_UPDATED)) {
            SIMHelper.updateSIMInfos(context);
            updateDataNetType();
            updateTelephonySignalStrength();
            updateOperatorInfo();
            refreshViews();
        }
    }


    // ===== Telephony ==============================================================

    private PhoneStateListener getPhoneStateListener(int subscription) {
        long subscriptionId = SIMHelper.getSubId(subscription);
        PhoneStateListener mPhoneStateListener = new PhoneStateListener(subscriptionId) {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                final int subscription = SIMHelper.getSimId(mSubscription);
                if (DEBUG) {
                    Log.d(TAG, "onSignalStrengthsChanged signalStrength=" + signalStrength +
                        ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
                }
                mSignalStrength[subscription] = signalStrength;
                updateTelephonySignalStrength(subscription);
                refreshViews(subscription);
            }

            @Override
            public void onServiceStateChanged(ServiceState state) {
                final int subscription = SIMHelper.getSimId(mSubscription);
                if (DEBUG) {
                    Log.d(TAG, "onServiceStateChanged voiceState=" + state.getVoiceRegState()
                            + " dataState=" + state.getDataRegState());
                }
                mServiceState[subscription] = state;
                updateTelephonySignalStrength(subscription);
                updateDataNetType();
                updateDataIcon(subscription);
                refreshViews(subscription);
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                final int subscription = SIMHelper.getSimId(mSubscription);
                if (DEBUG) {
                    Log.d(TAG, "onCallStateChanged state=" + state);
                }
                // In cdma, if a voice call is made, RSSI should switch to 1x.
                if (isCdma(subscription)) {
                    updateTelephonySignalStrength(subscription);
                    refreshViews(subscription);
                }
            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                final int subscription = SIMHelper.getSimId(mSubscription);
                if (DEBUG) {
                    Log.d(TAG, "onDataConnectionStateChanged: state=" + state
                            + " type=" + networkType);
                }
                mDataState[subscription] = state;
                mDataNetType[subscription] = networkType;
                updateDataNetType();
                updateDataIcon(subscription);
                refreshViews(subscription);
            }

            @Override
            public void onDataActivity(int direction) {
                final int subscription = SIMHelper.getSimId(mSubscription);
                if (DEBUG) {
                    Log.d(TAG, "onDataActivity: direction=" + direction);
                }
                mDataActivity[subscription] = direction;
                updateDataIcon(subscription);
                refreshViews(subscription);
            }
        };
        return mPhoneStateListener;
    }

    private final void updateSimState(int subscription, Intent intent) {
        IccCardConstants.State tempSimState = null;

        String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
            tempSimState = IccCardConstants.State.ABSENT;
        } else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
            tempSimState = IccCardConstants.State.READY;
        } else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
            final String lockedReason = intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
            if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                tempSimState = IccCardConstants.State.PIN_REQUIRED;
            } else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                tempSimState = IccCardConstants.State.PUK_REQUIRED;
            } else {
                tempSimState = IccCardConstants.State.NETWORK_LOCKED;
            }
        } else {
            tempSimState = IccCardConstants.State.UNKNOWN;
        }

        if (tempSimState != null) {
            mSimState[subscription] = tempSimState;
        }
    }

    private boolean isCdma(int subscription) {
        SignalStrength tempSignalStrength = mSignalStrength[subscription];

        return (tempSignalStrength != null) && !tempSignalStrength.isGsm();
    }

    private boolean hasService(int subscription) {
        return SIMHelper.hasService(mServiceState[subscription]);
    }

    private void updateAirplaneMode() {
        mAirplaneMode = (Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
    }

    private void refreshLocale() {
        mLocale = mContext.getResources().getConfiguration().locale;
    }

    private final void updateTelephonySignalStrength() {
        for (int i=0 ; i < mSubCount ; i++) {
            updateTelephonySignalStrength(i);
        }
    }

    private final void updateTelephonySignalStrength(int subscription) {
        if (!hasService(subscription)) {
            if (CHATTY) Log.d(TAG, "updateTelephonySignalStrength: !hasService()");
            mPhoneSignalIconId[subscription] = R.drawable.stat_sys_sim_signal_null;
            mQSPhoneSignalIconId[subscription] = R.drawable.ic_qs_signal_no_signal;
            mDataSignalIconId[subscription] = R.drawable.stat_sys_sim_signal_null;
        } else {
            if (mSignalStrength[subscription] == null) {
                if (CHATTY) Log.d(TAG, "updateTelephonySignalStrength: mSignalStrength == null");
                mPhoneSignalIconId[subscription] = R.drawable.stat_sys_sim_signal_null;
                mQSPhoneSignalIconId[subscription] = R.drawable.ic_qs_signal_no_signal;
                mDataSignalIconId[subscription] = R.drawable.stat_sys_sim_signal_null;
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
                int simColorId = SIMHelper.getSIMColorIdBySub(mContext, SIMHelper.getSubId(subscription));
                if (simColorId > -1 && simColorId < 4) {
                    iconList = TelephonyIcons.getTelephonySignalStrengthIconList(simColorId);
                    mPhoneSignalIconId[subscription] = iconList[iconLevel];
                }
                if (DEBUG) Log.d(TAG, "updateTelephonySignalStrength(" + subscription + ")   simColorId = " + simColorId);
                mQSPhoneSignalIconId[subscription] =
                        TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH[mInetCondition][iconLevel];
                mContentDescriptionPhoneSignal[subscription] = mContext.getString(
                        AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[iconLevel]);
                mDataSignalIconId[subscription] = mPhoneSignalIconId[subscription];
            }
        }
    }

    private final void updateDataNetType() {
        for (int i = 0; i < mSubCount; i++) {
    	    updateDataNetType(i);
        }
    }

    private final void updateDataNetType(int subscription) {
        int simColorId = SIMHelper.getSIMColorIdBySub(mContext, SIMHelper.getSubId(subscription));
        if (DEBUG) Log.d(TAG, "refreshViews(" + subscription + ")   simColorId = " + simColorId);
        if (simColorId == -1) return;

        if ((isCdma(subscription) && isCdmaEri(subscription)) || mPhone.isNetworkRoaming(subscription)) {
            int tempRoamingId = 0;

            if (simColorId > -1 && simColorId < 4) {
                tempRoamingId = TelephonyIcons.ROAMING[simColorId];
            }
            if (DEBUG) Log.d(TAG, "refreshViews(" + subscription + ")  RoamingresId= " + tempRoamingId + " simColorId = " + simColorId);
            mIsRoaming[subscription] = true;
            mIsRoamingId[subscription] = tempRoamingId;
        } else {
            mIsRoaming[subscription] = false;
            mIsRoamingId[subscription] = 0;
        }

        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is a special 4g network not handled by telephony
            mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_4G, 
                                           simColorId, mIsRoaming[subscription]);
            mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_4G[mInetCondition];
            mContentDescriptionDataType[subscription] = mContext.getString(
                    R.string.accessibility_data_connection_4g);
        } else {
            switch (mDataNetType[subscription]) {
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    if (!mShowAtLeastThreeGees) {
                        mDataTypeIconId[subscription] = 0;
                        mQSDataTypeIconId[subscription] = 0;
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_gprs);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    if (!mShowAtLeastThreeGees) {
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_E, 
                                                       simColorId, mIsRoaming[subscription]);
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_E[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_edge);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_3G, 
                                                   simColorId, mIsRoaming[subscription]);
                    mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_3G[mInetCondition];
                    mContentDescriptionDataType[subscription] = mContext.getString(
                            R.string.accessibility_data_connection_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    if (mHspaDataDistinguishable) {
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_H, 
                                                       simColorId, mIsRoaming[subscription]);
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_H[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_3_5g);
                    } else {
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_3G, 
                                                       simColorId, mIsRoaming[subscription]);
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_3G[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    if (!mShowAtLeastThreeGees) {
                        // display 1xRTT for IS95A/B
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_1X, 
                                                       simColorId, mIsRoaming[subscription]);
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_1X[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_cdma);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    if (!mShowAtLeastThreeGees) {
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_1X, 
                                                       simColorId, mIsRoaming[subscription]);
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_1X[mInetCondition];
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
                    mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_3G, 
                                                   simColorId, mIsRoaming[subscription]);
                    mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_3G[mInetCondition];
                    mContentDescriptionDataType[subscription] = mContext.getString(
                            R.string.accessibility_data_connection_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    boolean show4GforLTE = mContext.getResources().getBoolean(R.bool.config_show4GForLTE);
                    if (show4GforLTE) {
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_4G, 
                                                       simColorId, mIsRoaming[subscription]);
                        mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_4g;
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_4G[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_4g);
                    } else {
                        mDataTypeIconId[subscription] = R.drawable.stat_sys_data_fully_connected_lte;
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_LTE[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_lte);
                    }
                    break;
                default:
                    if (!mShowAtLeastThreeGees) {
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_G, 
                                                       simColorId, mIsRoaming[subscription]);
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_G[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_gprs);
                    } else {
                        mDataTypeIconId[subscription] = TelephonyIcons.getDataTypeId(TelephonyIcons.Type_3G, 
                                                       simColorId, mIsRoaming[subscription]);
                        mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_3G[mInetCondition];
                        mContentDescriptionDataType[subscription] = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
            }
        }

        if (mIsRoaming[subscription]) {
            mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_R[mInetCondition];
        }
    }

    boolean isCdmaEri(int subscription) {
        ServiceState tempServiceState = mServiceState[subscription];

        if (tempServiceState != null) {
            final int iconIndex = tempServiceState.getCdmaEriIconIndex();
            if (iconIndex != EriInfo.ROAMING_INDICATOR_OFF) {
                final int iconMode = tempServiceState.getCdmaEriIconMode();
                if (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                        || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH) {
                    return true;
                }
            }
        }
        return false;
    }

    private final void updateDataIcon() {
        for(int i = 0; i < mSubCount ; i++) {
            updateDataIcon(i);
        }
    }

    private final void updateDataIcon(int subscription) {
        int iconId;
        boolean visible = true;

        if (!isCdma(subscription)) {
            // GSM case, we have to check also the sim state
            if (mSimState[subscription] == IccCardConstants.State.READY ||
                    mSimState[subscription] == IccCardConstants.State.UNKNOWN) {
                if (hasService(subscription) && mDataState[subscription] == TelephonyManager.DATA_CONNECTED) {
                    switch (mDataActivity[subscription]) {
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            iconId = R.drawable.stat_sys_signal_in;
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            iconId = R.drawable.stat_sys_signal_out;
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            iconId = R.drawable.stat_sys_signal_inout;
                            break;
                        default:
                            iconId = 0;
                            break;
                    }
                    mDataDirectionIconId[subscription] = iconId;
                } else {
                    iconId = 0;
                    visible = false;
                }
            } else {
                iconId = 0;
                visible = false; // no SIM? no data
            }
        } else {
            // CDMA case, mDataActivity can be also DATA_ACTIVITY_DORMANT
            if (hasService(subscription) && mDataState[subscription] == TelephonyManager.DATA_CONNECTED) {
                switch (mDataActivity[subscription]) {
                    case TelephonyManager.DATA_ACTIVITY_IN:
                        iconId = R.drawable.stat_sys_signal_in;
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT:
                        iconId = R.drawable.stat_sys_signal_out;
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT:
                        iconId = R.drawable.stat_sys_signal_inout;
                        break;
                    case TelephonyManager.DATA_ACTIVITY_DORMANT:
                    default:
                        iconId = 0;
                        break;
                }
            } else {
                iconId = 0;
                visible = false;
            }
        }

        mDataDirectionIconId[subscription] = iconId;
        mDataConnected[subscription] = visible;
    }

    void updateNetworkName(int subscription, boolean showSpn, String spn, boolean showPlmn, String plmn) {
        if (false) {
            Log.d("CarrierLabel", "updateNetworkName(" + subscription + "), showSpn=" + showSpn + " spn=" + spn
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

        if(mCarrierView[subscription] != null) {
            mCarrierView[subscription].setText(mNetworkName[subscription]);
        }
 
        if (DEBUG) Log.d(TAG, "updateNetworkName(" + subscription + "), mNetworkName=" + mNetworkName[subscription]);
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
                        refreshViews();
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
        updateDataNetType();
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
        updateDataNetType();
        updateWimaxIcons();
        updateDataIcon();
        updateTelephonySignalStrength();
        updateWifiIcons();
    }


    // ===== Update the views =======================================================

    void refreshViews() {
        for (int i = 0 ; i < mSubCount ; i++) {
            refreshViews(i);
        }
    }

    void refreshViews(int subscription) {
        Context context = mContext;

        int combinedSignalIconId = 0;
        String combinedLabel = "";
        String wifiLabel = "";
        String mobileLabel = "";
        int N;
        final boolean emergencyOnly = isEmergencyOnly();

        if (!mHasMobileDataFeature) {
            mDataSignalIconId[subscription] = mPhoneSignalIconId[subscription] = 0;
            mQSPhoneSignalIconId[subscription] = 0;
            mobileLabel = "";
        } else {
            // We want to show the carrier name if in service and either:
            //   - We are connected to mobile data, or
            //   - We are not connected to mobile data, as long as the *reason* packets are not
            //     being routed over that link is that we have better connectivity via wifi.
            // If data is disconnected for some other reason but wifi (or ethernet/bluetooth)
            // is connected, we show nothing.
            // Otherwise (nothing connected) we show "No internet connection".

            if (mDataConnected[subscription]) {
                mobileLabel = mNetworkName[subscription];
            } else if (mConnected || emergencyOnly) {
                if (hasService(subscription) || emergencyOnly) {
                    // The isEmergencyOnly test covers the case of a phone with no SIM
                    mobileLabel = mNetworkName[subscription];
                } else {
                    // Tablets, basically
                    mobileLabel = "";
                }
            } else {
                mobileLabel
                    = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }

            // Now for things that should only be shown when actually using mobile data.
            if (mDataConnected[subscription]) {
                combinedSignalIconId = mDataSignalIconId[subscription];

                combinedLabel = mobileLabel;
                combinedSignalIconId = mDataSignalIconId[subscription]; // set by updateDataIcon()
                mContentDescriptionCombinedSignal = mContentDescriptionDataType[subscription];
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
            combinedSignalIconId = mWifiIconId; // set by updateWifiIcons()
            mContentDescriptionCombinedSignal = mContentDescriptionWifi;
        } else {
            if (mHasMobileDataFeature) {
                wifiLabel = "";
            } else {
                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }
        }

        if (mBluetoothTethered) {
            combinedLabel = mContext.getString(R.string.bluetooth_tethered);
            combinedSignalIconId = mBluetoothTetherIconId;
            mContentDescriptionCombinedSignal = mContext.getString(
                    R.string.accessibility_bluetooth_tether);
        }

        final boolean ethernetConnected = (mConnectedNetworkType == ConnectivityManager.TYPE_ETHERNET);
        if (ethernetConnected) {
            combinedLabel = context.getString(R.string.ethernet_label);
        }

        if (mAirplaneMode &&
                (mServiceState[subscription] == null || (!hasService(subscription) && !mServiceState[subscription].isEmergencyOnly()))) {
            // Only display the flight-mode icon if not in "emergency calls only" mode.

            // look again; your radios are now airplanes
            mContentDescriptionPhoneSignal[subscription] = mContext.getString(
                    R.string.accessibility_airplane_mode);
            mAirplaneIconId = FLIGHT_MODE_ICON;
            mPhoneSignalIconId[subscription] = mDataSignalIconId[subscription] = mDataTypeIconId[subscription] = mQSDataTypeIconId[subscription] = 0;
            mQSPhoneSignalIconId[subscription] = 0;

            // combined values from connected wifi take precedence over airplane mode
            if (mWifiConnected) {
                // Suppress "No internet connection." from mobile if wifi connected.
                mobileLabel = "";
            } else {
                if (mHasMobileDataFeature) {
                    // let the mobile icon show "No internet connection."
                    wifiLabel = "";
                } else {
                    wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
                    combinedLabel = wifiLabel;
                }
                mContentDescriptionCombinedSignal = mContentDescriptionPhoneSignal[subscription];
                combinedSignalIconId = mDataSignalIconId[subscription];
            }
        }
        else if (!mDataConnected[subscription] && !mWifiConnected && !mBluetoothTethered && !mWimaxConnected && !ethernetConnected) {
            // pretty much totally disconnected

            combinedLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            // On devices without mobile radios, we want to show the wifi icon
            combinedSignalIconId =
                mHasMobileDataFeature ? mDataSignalIconId[subscription] : mWifiIconId;
            mContentDescriptionCombinedSignal = mHasMobileDataFeature
                ? mContentDescriptionDataType[subscription] : mContentDescriptionWifi;

            mDataTypeIconId[subscription] = 0;
            mQSDataTypeIconId[subscription] = 0;
            if ((isCdma(subscription) && isCdmaEri(subscription)) || mPhone.isNetworkRoaming(subscription)) {
                int simColorId = SIMHelper.getSIMColorIdBySub(mContext, SIMHelper.getSubId(subscription));
                int tempRoamingId = 0;

                if (simColorId > -1 && simColorId < 4) {
                    tempRoamingId = TelephonyIcons.ROAMING[simColorId];
                }
                if (DEBUG) Log.d(TAG, "refreshViews(" + subscription + ")  RoamingresId= " + tempRoamingId + " simColorId = " + simColorId);
                mIsRoaming[subscription] = true;
                mIsRoamingId[subscription] = tempRoamingId;
                mQSDataTypeIconId[subscription] = TelephonyIcons.QS_DATA_R[mInetCondition];
            } else {
                mIsRoaming[subscription] = false;
                mIsRoamingId[subscription] = 0;
            }
        }

        if (DEBUG) {
            Log.d(TAG, "refreshViews connected={"
                    + (mWifiConnected?" wifi":"")
                    + (mDataConnected[subscription]?" data":"")
                    + " } level="
                    + ((mSignalStrength[subscription] == null)?"??":Integer.toString(mSignalStrength[subscription].getLevel()))
                    + " combinedSignalIconId=0x"
                    + Integer.toHexString(combinedSignalIconId)
                    + "/" + getResourceName(combinedSignalIconId)
                    + " mobileLabel=" + mobileLabel
                    + " wifiLabel=" + wifiLabel
                    + " emergencyOnly=" + emergencyOnly
                    + " combinedLabel=" + combinedLabel
                    + " mAirplaneMode=" + mAirplaneMode
                    + " mDataActivity=" + mDataActivity[subscription]
                    + " mPhoneSignalIconId=0x" + Integer.toHexString(mPhoneSignalIconId[subscription])
                    + " mQSPhoneSignalIconId=0x" + Integer.toHexString(mQSPhoneSignalIconId[subscription])
                    + " mDataDirectionIconId=0x" + Integer.toHexString(mDataDirectionIconId[subscription])
                    + " mDataSignalIconId=0x" + Integer.toHexString(mDataSignalIconId[subscription])
                    + " mDataTypeIconId=0x" + Integer.toHexString(mDataTypeIconId[subscription])
                    + " mQSDataTypeIconId=0x" + Integer.toHexString(mQSDataTypeIconId[subscription])
                    + " mWifiIconId=0x" + Integer.toHexString(mWifiIconId)
                    + " mQSWifiIconId=0x" + Integer.toHexString(mQSWifiIconId)
                    + " mBluetoothTetherIconId=0x" + Integer.toHexString(mBluetoothTetherIconId));
        }

        // update QS
        for (NetworkSignalChangedCallback cb : mSignalsChangedCallbacks) {
            notifySignalsChangedCallbacks(cb);
        }

        if (mLastPhoneSignalIconId[subscription] != mPhoneSignalIconId[subscription]
         || mLastWifiIconId                 != mWifiIconId
         || mLastWimaxIconId                != mWimaxIconId
         || mLastDataTypeIconId[subscription]    != mDataTypeIconId[subscription]
         || mLastAirplaneMode               != mAirplaneMode
         || mLastLocale                     != mLocale)
        {
            // NB: the mLast*s will be updated later
            for (SignalCluster cluster : mSignalClusters) {
                refreshSignalCluster(cluster);
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
        if (mLastCombinedSignalIconId != combinedSignalIconId) {
            mLastCombinedSignalIconId = combinedSignalIconId;
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
        N = mMobileLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mMobileLabelViews.get(i);
            v.setText(mobileLabel);
            if ("".equals(mobileLabel)) {
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
                v.setText(mobileLabel); // comes from the telephony stack
                v.setVisibility(View.VISIBLE);
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");
        pw.println(String.format("  %s network type %d (%s)",
                mConnected?"CONNECTED":"DISCONNECTED",
                mConnectedNetworkType, mConnectedNetworkTypeName));
        pw.println("  - telephony ------");

        for (int i = 0 ; i < mSubCount ; i++) {
            pw.println(String.format("====== subscription: %d ======", i));
            pw.print("  hasVoiceCallingFeature()=");
            pw.println(hasVoiceCallingFeature());
            pw.print("  hasService()=");
            pw.println(hasService(i));
            pw.print("  mHspaDataDistinguishable=");
            pw.println(mHspaDataDistinguishable);
            pw.print("  mDataConnected=");
            pw.println(mDataConnected[i]);
            pw.print("  mSimState=");
            pw.println(mSimState[i]);
            pw.print("  mPhoneState=");
            pw.println(mPhoneState);
            pw.print("  mDataState=");
            pw.println(mDataState[i]);
            pw.print("  mDataActivity=");
            pw.println(mDataActivity[i]);
            pw.print("  mDataNetType=");
            pw.print(mDataNetType[i]);
            pw.print("/");
            pw.println(TelephonyManager.getNetworkTypeName(mDataNetType[i]));
            pw.print("  mServiceState=");
            pw.println(mServiceState[i]);
            pw.print("  mSignalStrength=");
            pw.println(mSignalStrength[i]);
            pw.print("  mLastSignalLevel=");
            pw.println(mLastSignalLevel);
            pw.print("  mNetworkName=");
            pw.println(mNetworkName[i]);
            pw.print("  mNetworkNameDefault=");
            pw.println(mNetworkNameDefault);
            pw.print("  mNetworkNameSeparator=");
            pw.println(mNetworkNameSeparator.replace("\n","\\n"));
            pw.print("  mPhoneSignalIconId=0x");
            pw.print(Integer.toHexString(mPhoneSignalIconId[i]));
            pw.print("/");
            pw.print("  mQSPhoneSignalIconId=0x");
            pw.print(Integer.toHexString(mQSPhoneSignalIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mPhoneSignalIconId[i]));
            pw.print("  mDataDirectionIconId=");
            pw.print(Integer.toHexString(mDataDirectionIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mDataDirectionIconId[i]));
            pw.print("  mDataSignalIconId=");
            pw.print(Integer.toHexString(mDataSignalIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mDataSignalIconId[i]));
            pw.print("  mLastDataDirectionIconId=0x");
            pw.print(Integer.toHexString(mLastDataDirectionIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mLastDataDirectionIconId[i]));
            pw.print("  mDataTypeIconId=");
            pw.print(Integer.toHexString(mDataTypeIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mDataTypeIconId[i]));
            pw.print("  mLastDataTypeIconId=0x");
            pw.print(Integer.toHexString(mLastDataTypeIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mLastDataTypeIconId[i]));
            pw.print("  mQSDataTypeIconId=");
            pw.print(Integer.toHexString(mQSDataTypeIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mQSDataTypeIconId[i]));

            pw.println("  - icons ------");
            pw.print("  mLastPhoneSignalIconId=0x");
            pw.print(Integer.toHexString(mLastPhoneSignalIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mLastPhoneSignalIconId[i]));
        }

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

        pw.print("  mLastWifiIconId=0x");
        pw.print(Integer.toHexString(mLastWifiIconId));
        pw.print("/");
        pw.println(getResourceName(mLastWifiIconId));
        pw.print("  mLastCombinedSignalIconId=0x");
        pw.print(Integer.toHexString(mLastCombinedSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mLastCombinedSignalIconId));
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
            mDemoDataTypeIconId = mDataTypeIconId[0];
            mDemoMobileLevel = mLastSignalLevel;
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            for (SignalCluster cluster : mSignalClusters) {
                refreshSignalCluster(cluster);
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
                    for (int i = 0; i < mSubCount ; i++) {
                        cluster.setMobileDataIndicators(
                                i,
                                show,
                                0,
                                iconId,
                                mDemoDataTypeIconId,
                                "Demo",
                                "Demo",
                                false,
                                0);
                    }
                }
            }
        }
    }

    private TextView[] mCarrierView = new TextView[4];
    private TextView[] mCarrierDivider = new TextView[3];

    public void setCarrierView(LinearLayout mLayout) {
        if (mLayout != null) {
            mCarrierView[SIMHelper.SUBSCRIPTION_INDEX_DEFAULT] = (TextView)mLayout.findViewById(R.id.carrier_label);
            mCarrierView[SIMHelper.SUBSCRIPTION_INDEX_1] = (TextView)mLayout.findViewById(R.id.carrier2);
            mCarrierView[SIMHelper.SUBSCRIPTION_INDEX_2] = (TextView)mLayout.findViewById(R.id.carrier3);
            mCarrierView[SIMHelper.SUBSCRIPTION_INDEX_3] = (TextView)mLayout.findViewById(R.id.carrier4);
            mCarrierDivider[SIMHelper.SUBSCRIPTION_INDEX_DEFAULT] = (TextView)mLayout.findViewById(R.id.carrier_divider);
            mCarrierDivider[SIMHelper.SUBSCRIPTION_INDEX_1] = (TextView)mLayout.findViewById(R.id.carrier_divider2);
            mCarrierDivider[SIMHelper.SUBSCRIPTION_INDEX_2] = (TextView)mLayout.findViewById(R.id.carrier_divider3);
        }
    }

    private boolean isWifiOnlyDevice() {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return  !(cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
    }

    private void updateOperatorInfo() {
        final int mMaxSubscription = SIMHelper.getNumOfSubscription() - 1;

        if (isWifiOnlyDevice()) {
            for (int i = SIMHelper.SUBSCRIPTION_INDEX_DEFAULT; i <= mMaxSubscription; i++) {
                if(mCarrierView[i] != null) {
                    mCarrierView[i].setVisibility(View.GONE);
                }
            }

            for (int i = SIMHelper.SUBSCRIPTION_INDEX_DEFAULT; i <= mMaxSubscription-1; i++) {
                if(mCarrierDivider[i] != null) {
                    mCarrierDivider[i].setVisibility(View.GONE);
                }
            }
        } else {
            int mNumOfSIM = 0;
            TextView mCarrierLeft = null;
            TextView mCarrierRight = null;

            for (int i = SIMHelper.SUBSCRIPTION_INDEX_DEFAULT; i <= mMaxSubscription-1; i++) {
                if (mCarrierDivider[i] != null) {
                    mCarrierDivider[i].setVisibility(View.GONE);
                }
            }

            for (int i = SIMHelper.SUBSCRIPTION_INDEX_DEFAULT; i <= mMaxSubscription; i++) {
                if (true) {
                    if(mCarrierView[i] != null) {
                        mCarrierView[i].setVisibility(View.VISIBLE);
                    }                        
                    mNumOfSIM++;
                    if(mNumOfSIM == 1) {
                        mCarrierLeft = mCarrierView[i];
                    } else if(mNumOfSIM == 2) {
                        mCarrierRight = mCarrierView[i];
                    }
                    if(mNumOfSIM >= 2 && ((i - 1) >= 0) && (mCarrierDivider[i-1] != null)) {
                        mCarrierDivider[i-1].setVisibility(View.VISIBLE);
                        mCarrierDivider[i-1].setText("|");
                    }
                } else {
                    if(mCarrierView[i] != null) {
                        mCarrierView[i].setVisibility(View.GONE);
                    }
                }
                if (mCarrierView[i] != null) {
                    mCarrierView[i].setGravity(Gravity.CENTER);
                }
            }

            if(mNumOfSIM == 2) {
                if (mCarrierLeft != null) {
                    mCarrierLeft.setGravity(Gravity.END);
                }
                if (mCarrierRight != null) {
                    mCarrierRight.setGravity(Gravity.START);
                }
            } else if(mNumOfSIM == 0) {
                if (mCarrierView[SIMHelper.SUBSCRIPTION_INDEX_DEFAULT] != null) {
                    mCarrierView[SIMHelper.SUBSCRIPTION_INDEX_DEFAULT].setVisibility(View.VISIBLE);
                }
                if (DEBUG) Log.d(TAG, "updateOperatorInfo, force the subscription 0 to visible.");
            }
        }
    }
}
