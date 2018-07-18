/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.connectivity.tethering;

import static android.net.ConnectivityManager.EXTRA_ADD_TETHER_TYPE;
import static android.net.ConnectivityManager.EXTRA_PROVISION_CALLBACK;
import static android.net.ConnectivityManager.EXTRA_REM_TETHER_TYPE;
import static android.net.ConnectivityManager.EXTRA_RUN_PROVISION;
import static android.net.ConnectivityManager.EXTRA_SET_ALARM;
import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_INVALID;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.ConnectivityManager.TETHERING_WIFI;
import static android.net.ConnectivityManager.TETHER_ERROR_NO_ERROR;
import static android.net.ConnectivityManager.TETHER_ERROR_PROVISION_FAILED;
import static android.net.ConnectivityManager.TETHER_ERROR_PROVISION_NOT_REQUIRED;

import static com.android.internal.R.string.config_wifi_tether_enable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.util.SharedLog;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.util.SparseIntArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.StateMachine;
import com.android.server.connectivity.MockableSystemProperties;

import java.io.PrintWriter;


/**
 * This class encapsulates entitlement/provisioning mechanics
 *
 * @hide
 */
public class EntitlementManager {
    private static final String TAG = EntitlementManager.class.getSimpleName();

    protected static final String DISABLE_PROVISIONING_SYSPROP_KEY = "net.tethering.noprovisioning";

    // {@link ComponentName} of the Service used to run tether provisioning.
    private static final ComponentName TETHER_SERVICE = ComponentName.unflattenFromString(Resources
            .getSystem().getString(config_wifi_tether_enable));

    private final Context mContext;
    private final MockableSystemProperties mSystemProperties;
    private final SharedLog mLog;
    private final StateMachine mTarget;
    private final int mWhat;
    //key: TETHERING_TYPE, value: PROVISION_RESULT
    private final SparseIntArray mMobilePermittedMap;
    private TetheringConfiguration mConfig;
    private boolean mCurrentPermitted = true;

    public EntitlementManager(Context ctx, StateMachine target,
            SharedLog log, int what, MockableSystemProperties systemProperties) {
        mContext = ctx;
        mLog = log;
        mMobilePermittedMap = new SparseIntArray();
        mSystemProperties = systemProperties;
        mTarget = target;
        mWhat = what;
    }

    /**
     * Passing a new TetheringConfiguration instance each time when
     * Tethering.java's updateConfiguration() is called.
     */
    public void updateConfiguration(TetheringConfiguration ctx) {
        mConfig = ctx;
    }

    /**
     * add the mapping between provisioning result and tethering type
     * notify UpstreamNetworkMonitor if mobile permission is change
     * @param type Tethering type
     * @param resultcode Provisioning result
     */
    public void addDownStreamMapping(int type, int resultcode) {
        mMobilePermittedMap.put(type, resultcode);

        boolean prePermitted = mCurrentPermitted;
        mCurrentPermitted = ((mMobilePermittedMap.indexOfValue(TETHER_ERROR_NO_ERROR) > -1)
                || (mMobilePermittedMap.indexOfValue(TETHER_ERROR_PROVISION_NOT_REQUIRED) > -1));
        if (mCurrentPermitted != prePermitted) {
            mLog.log("Entitlement permitted change: " + mCurrentPermitted);
            notifyPermittedChange();
        }
    }

    /**
     * remove the mapping for input tethering type
     * @param type Tethering type
     */
    public void removeDownStreamMapping(int type) {
        if (isTetherProvisioningRequired()) {
            cancelTetherProvisioningRechecks(type);
        }
        mMobilePermittedMap.delete(type);
    }

    /**
     * check if mobile upstream is permitted
     */
    public boolean isMobileUpstreamPermitted() {
        return mCurrentPermitted;
    }

    /**
     * Check if the device requires a provisioning check in order to enable tethering.
     *
     * @return a boolean - {@code true} indicating tether provisioning is required by the carrier.
     */
    @VisibleForTesting
    public boolean isTetherProvisioningRequired() {
        if (mSystemProperties.getBoolean(DISABLE_PROVISIONING_SYSPROP_KEY, false)
                || mConfig.provisioningApp.length == 0) {
            return false;
        }
        if (carrierConfigAffirmsEntitlementCheckNotRequired()) {
            return false;
        }
        return (mConfig.provisioningApp.length == 2);
    }

    /**
     * re-check tethering provisioning when user change SIM card
     */
    public void reevaluateSimCardProvisioning() {
        //TODO: refine provisioning check to isTetherProvisioningRequired() ??
        if (!mConfig.hasMobileHotspotProvisionApp()
                || carrierConfigAffirmsEntitlementCheckNotRequired()) {
            for (int i = 0; i < mMobilePermittedMap.size(); i++) {
                int type = mMobilePermittedMap.keyAt(i);
                addDownStreamMapping(type, TETHER_ERROR_PROVISION_NOT_REQUIRED);
            }
            return;
        }
        for (int i = 0; i < mMobilePermittedMap.size(); i++) {
            int type = mMobilePermittedMap.keyAt(i);
            ResultReceiver build = buildReceiver(type);
            runUiTetherProvisioning(type, build);
        }
    }

    // The logic here is aimed solely at confirming that a CarrierConfig exists
    // and affirms that entitlement checks are not required.
    //
    // TODO: find a better way to express this, or alter the checking process
    // entirely so that this is more intuitive.
    private boolean carrierConfigAffirmsEntitlementCheckNotRequired() {
        // Check carrier config for entitlement checks
        final CarrierConfigManager configManager = (CarrierConfigManager) mContext
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager == null) return false;

        final PersistableBundle carrierConfig = configManager.getConfig();
        if (carrierConfig == null) return false;

        // A CarrierConfigManager was found and it has a config.
        final boolean isEntitlementCheckRequired = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_REQUIRE_ENTITLEMENT_CHECKS_BOOL);
        return !isEntitlementCheckRequired;
    }

    /**
     * schedule tethering provisioning re-check
     * @param type Tethering type
     */
    public void scheduleProvisioningRechecks(int type) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ADD_TETHER_TYPE, type);
        intent.putExtra(EXTRA_SET_ALARM, true);
        intent.setComponent(TETHER_SERVICE);
        final long ident = Binder.clearCallingIdentity();
        try {
            mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /**
     * run no UI tethering provisioning check
     * @param type Tethering type
     * @param receiver callback of tethering provisioning
     */
    public void runSilentTetherProvisioning(int type, ResultReceiver receiver) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ADD_TETHER_TYPE, type);
        intent.putExtra(EXTRA_RUN_PROVISION, true);
        intent.putExtra(EXTRA_PROVISION_CALLBACK, receiver);
        intent.setComponent(TETHER_SERVICE);
        final long ident = Binder.clearCallingIdentity();
        try {
            mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    //TODO: consider the right place to remove provision status
    private void cancelTetherProvisioningRechecks(int type) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_REM_TETHER_TYPE, type);
        intent.setComponent(TETHER_SERVICE);
        final long ident = Binder.clearCallingIdentity();
        try {
            mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /**
     * There are UI shown when running this tethering provisioning check method
     * @param type Tethering type
     * @param receiver callback of tethering provisioning
     */
    private void runUiTetherProvisioning(int type, ResultReceiver receiver) {
        Intent intent = new Intent(Settings.ACTION_TETHER_PROVISIONING);
        intent.putExtra(EXTRA_ADD_TETHER_TYPE, type);
        intent.putExtra(EXTRA_PROVISION_CALLBACK, receiver);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final long ident = Binder.clearCallingIdentity();
        try {
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private ResultReceiver buildReceiver(int type) {
        ResultReceiver rr = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                addDownStreamMapping(type, resultCode);
            }
        };

        Parcel parcel = Parcel.obtain();
        rr.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
    }

    private void notifyPermittedChange() {
        mTarget.sendMessage(mWhat);
    }

    /**
     * dump the log of EntitlementManager
     * @param pw {@link PrintWriter} is used to print formatted
     */
    public void dump(PrintWriter pw) {
        for (int i = 0; i < mMobilePermittedMap.size(); i++) {
            pw.print("Type: ");
            pw.print(typeString(mMobilePermittedMap.keyAt(i)));
            pw.print(", Value: ");
            pw.println(valueString(mMobilePermittedMap.valueAt(i)));
        }
    }

    private static String typeString(int type) {
        switch (type) {
            case TETHERING_BLUETOOTH: return "TETHERING_BLUETOOTH";
            case TETHERING_INVALID: return "TETHERING_INVALID";
            case TETHERING_USB: return "TETHERING_USB";
            case TETHERING_WIFI: return "TETHERING_WIFI";
            default:
                return String.format("UNKNOWN (%s)", type);
        }
    }

    private static String valueString(int value) {
        switch (value) {
            case TETHER_ERROR_NO_ERROR: return "TETHER_ERROR_NO_ERROR";
            case TETHER_ERROR_PROVISION_FAILED: return "TETHER_ERROR_PROVISION_FAILED";
            case TETHER_ERROR_PROVISION_NOT_REQUIRED: return "TETHER_ERROR_PROVISION_NOT_REQUIRED";
            default:
                return String.format("UNKNOWN (%s)", value);
        }
    }
}
