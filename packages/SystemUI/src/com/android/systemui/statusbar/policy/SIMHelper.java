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
import android.telephony.SimInfoManager;
import android.telephony.SimInfoManager.SimInfoRecord;
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
    private static List<SimInfoManager.SimInfoRecord> sSimInfos;
    private static boolean[] simInserted;

    private SIMHelper() {
    }

    public static List<SimInfoManager.SimInfoRecord> getSIMInfoList(Context context) {
        if (sSimInfos == null || sSimInfos.size() == 0) {
            sSimInfos = getSortedSIMInfoList(context);
        }
        return sSimInfos;
    }

    /**
     * Get the SIM info of the assigned SIM id.
     *
     * @param context
     * @param simId
     * @return The SIM info, or null if it doesn't exist.
     */
    public static SimInfoManager.SimInfoRecord getSIMInfo(Context context, long simId) {
        if (sSimInfos == null || sSimInfos.size() == 0) {
            getSIMInfoList(context);
        }
        for (SimInfoManager.SimInfoRecord info : sSimInfos) {
            if (info.mSimInfoIdx == simId) {
                return info;
            }
        }
        return null;
    }

    /**
     * Get the SIM info of the assigned SLOT id.
     *
     * @param context
     * @param slotId
     * @return The SIM info, or null if it doesn't exist.
     */
    public static SimInfoManager.SimInfoRecord getSIMInfoBySlot(Context context, int slotId) {
        if(!isSimInserted(slotId)) {
            return null;
        }
        if (sSimInfos == null || sSimInfos.size() == 0) {
            getSIMInfoList(context);
        }
        if (sSimInfos == null) {
            return null;
        }

        for (SimInfoManager.SimInfoRecord info : sSimInfos) {
            if (info.mSimId == slotId) {
                return info;
            }
        }
        return null;
    }

    private static List<SimInfoManager.SimInfoRecord> getSortedSIMInfoList(Context context) {
        List<SimInfoManager.SimInfoRecord> simInfoList = SimInfoManager.getInsertedSimInfoList(context);
        Collections.sort(simInfoList, new Comparator<SimInfoManager.SimInfoRecord>() {
            @Override
            public int compare(SimInfoManager.SimInfoRecord a, SimInfoManager.SimInfoRecord b) {
                if(a.mSimId < b.mSimId) {
                    return -1;
                } else if (a.mSimId > b.mSimId) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return simInfoList;
    }

    public static void updateSIMInfos(Context context) {
        sSimInfos = null;
        sSimInfos = getSortedSIMInfoList(context);
    }

    public static long getSIMIdBySlot(Context context, int slotId) {
        SimInfoManager.SimInfoRecord simInfo = getSIMInfoBySlot(context, slotId);
        if (simInfo == null) {
            return 0;
        }
        return simInfo.mSimInfoIdx;
    }

    public static int getSIMColorIdBySlot(Context context, int slotId) {
        SimInfoManager.SimInfoRecord simInfo = getSIMInfoBySlot(context, slotId);
        if (simInfo == null) {
            return -1;
        }
        return simInfo.mColor;
    }

    public static boolean isSimInserted(int slotId) {
        if(simInserted == null) {
            updateSimInsertedStatus();
        }
        if (simInserted != null) {
            if(slotId <= simInserted.length -1) {
                if (DEBUG) Log.d(TAG, "isSimInserted(" + slotId + "), SimInserted=" + simInserted[slotId]);
                return simInserted[slotId];
            } else {
                if (DEBUG) Log.d(TAG, "isSimInserted(" + slotId + "), indexOutOfBound, arraysize=" + simInserted.length);
                return false; // default return false
            }
        } else {
            if (DEBUG) Log.d(TAG, "isSimInserted, simInserted is null");
            return false;
        }
    }

    public static void updateSimInsertedStatus() {
        final TelephonyManager mTelephony = TelephonyManager.getDefault();
        if (mTelephony != null) {
            int mSimCount = mTelephony.getSimCount();
            if (mSimCount > 0) {
                if(simInserted == null) {
                    simInserted = new boolean[mSimCount];
                }
                for (int i = 0 ; i < mSimCount ; i++) {
                    simInserted[i] = mTelephony.hasIccCard(i);
                    if (DEBUG) Log.d(TAG, "updateSimInsertedStatus, simInserted(" + i + ") = " + simInserted[i]);
                }
            }
        } else {
            if (DEBUG) Log.d(TAG, "updateSimInsertedStatus, phone is null");
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

    public static int getNumOfSim() {
        int simCount = SystemProperties.getInt(TelephonyProperties.PROPERTY_SIM_COUNT, 1);
        return ((simCount > 4) ? 4 : simCount);
    }
}
