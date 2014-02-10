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

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubInfoRecord;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;

public class SIMHelper {
    private static final String TAG = "SIMHelper";
    private static final boolean DEBUG = false;

    private static List<SubInfoRecord> sSimInfos;

    public static final int SUBSCRIPTION_INDEX_DEFAULT = 0;
    public static final int SUBSCRIPTION_INDEX_1 = 1;
    public static final int SUBSCRIPTION_INDEX_2 = 2;
    public static final int SUBSCRIPTION_INDEX_3 = 3;

    private SIMHelper() {
    }

    public static SubInfoRecord getSubInfoById(Context context, long subId) {
        if (sSimInfos == null || sSimInfos.size() == 0) {
            updateSIMInfos(context);
        }

        if (sSimInfos == null) {
            return null;
        }

        for (SubInfoRecord info : sSimInfos) {
            if (info.mSubId == subId) {
                return info;
            }
        }
        return null;
    }

    public static void updateSIMInfos(Context context) {
        sSimInfos = null;
        sSimInfos = SubscriptionManager.getActivatedSubInfoList(context);
    }

    public static int getSIMColorIdBySub(Context context, long subId) {
        SubInfoRecord simInfo = getSubInfoById(context, subId);
        if (simInfo == null) {
            return -1;
        }
        return simInfo.mColor;
    }

    public static int getSimId(long subId) {
        return SubscriptionManager.getSimId(subId);
    }

    public static long getSubId(int simId) {
        long[] subIds = SubscriptionManager.getSubId(simId);
        if (subIds != null) {
            return subIds[0];
        } else {
            return 0;
        }
    }

    public static boolean hasService(ServiceState ss) {
        if (ss != null) {
            // Consider the device to be in service if either voice or data service is available.
            // Some SIM cards are marketed as data-only and do not support voice service, and on
            // these SIM cards, we want to show signal bars for data service as well as the "no
            // service" or "emergency calls only" text that indicates that voice is not available.
            switch (ss.getVoiceRegState()) {
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

    /// default use sim count as subscription count, currently support to 4 subscriptions.
    public static int getNumOfSubscription() {
        return TelephonyManager.getDefault().getPhoneCount();
    }
}
