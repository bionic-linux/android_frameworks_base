/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionManager;
import android.telephony.SubInfoRecord;
import android.util.Log;
import android.widget.TextView;

public class KeyguardUtils {
    private static final String TAG = "KeyguardUtils";
    private static final boolean DEBUG = KeyguardViewMediator.DEBUG;
    private KeyguardUpdateMonitor mUpdateMonitor;

    public KeyguardUtils(Context context) {
        mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    public String getOptrNameBySubIdx(Context context, int subIdx) {
        if (subIdx >= 0) {
            long subId = mUpdateMonitor.getSubIdBySubIdx(subIdx);
            if (subId > 0) {
                if (DEBUG) Log.d(TAG, "getOptrNameBySubIdx, subIdx=" + subIdx + " subId=" + subId);
                SubInfoRecord info = SubscriptionManager.getSubInfoUsingSubId(context, subId);
                if (null == info) {
                   if (DEBUG) Log.d(TAG, "getOptrNameBySubIdx, return null");
                   return null;
                } else {
                   if (DEBUG) Log.d(TAG, "getOptrNameBySubIdx mDisplayName=" + info.mDisplayName);
                   return info.mDisplayName; 
                }
            }
        }
        throw new IndexOutOfBoundsException();
    }

    public void setOptrBackgroundBySubIdx(TextView v, int subIdx, Context context, String optrname) {
        Drawable bgDrawable = getOptrDrawableBySubIdx(context, subIdx);
        v.setBackground(bgDrawable);

        int simCardNamePadding = context.getResources().
                            getDimensionPixelSize(R.dimen.sim_card_name_padding);
        v.setPadding(simCardNamePadding, 0, simCardNamePadding, 0);

        if (null == optrname) {
            v.setText(R.string.kg_detecting_simcard);
        } else {
            v.setText(optrname);
        }
    }

    public Drawable getOptrDrawableBySubIdx(Context context, int subIdx) {
        long subId = mUpdateMonitor.getSubIdBySubIdx(subIdx);
        Drawable bgDrawable = null;
        if (subId > 0) {
            if (DEBUG) Log.d(TAG, "getOptrDrawableBySubIdx, subId=" + subId);
            SubInfoRecord info = SubscriptionManager.getSubInfoUsingSubId(context, subId); 
            if (null == info) {
                if (DEBUG) Log.d(TAG, "getOptrDrawableBySubIdx, return null");
            } else {
                if (info.mSimIconRes[0] > 0) {
                    bgDrawable = context.getResources().getDrawable(info.mSimIconRes[0]);//SubscriptionManager.RES_TYPE_BACKGROUND_DARK]);
                }
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
        return bgDrawable;
    }

}
