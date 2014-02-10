/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SIMHelper;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "SignalClusterView";

    NetworkController mNC;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0;
    private int mWifiActivityId = 0;
    private String mWifiDescription;

    private boolean[] mMobileVisible;
    private int[] mMobileStrengthId;
    private int[] mMobileActivityId;
    private int[] mMobileTypeId;
    private String[] mMobileDescription;
    private String[] mMobileTypeDescription;


    private boolean[] mRoaming;
    private int[] mRoamingId;

    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;

    private ViewGroup mWifiGroup;
    private ImageView mWifi;
    private ImageView mWifiActivity;

    private ViewGroup[] mSignalClusterCombo;

    private ViewGroup[] mMobileGroup;
    private ImageView[] mMobileRoam;
    private ImageView[] mMobile;
    private ImageView[] mMobileActivity;
    private ImageView[] mMobileType;
    private View[] mSpacer;
    private ImageView mAirplane;

    private int mSimCount = 0;

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mSimCount = SIMHelper.getNumOfSim();

        mRoaming = new boolean[mSimCount];
        mRoamingId = new int[mSimCount];
        mMobileDescription = new String[mSimCount];
        mMobileTypeDescription = new String[mSimCount];
        mSignalClusterCombo = new ViewGroup[mSimCount];
        mMobileGroup = new ViewGroup[mSimCount];
        mMobileRoam = new ImageView[mSimCount];
        mMobile = new ImageView[mSimCount];
        mMobileActivity = new ImageView[mSimCount];
        mMobileType = new ImageView[mSimCount];
        mSpacer = new View[mSimCount];
        mMobileActivityId = new int[mSimCount];
        mMobileTypeId = new int[mSimCount];
        mMobileStrengthId = new int[mSimCount];
        mMobileVisible = new boolean[mSimCount];
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG) Log.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup                    = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi                         = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity                 = (ImageView) findViewById(R.id.wifi_inout);
        mAirplane                     = (ImageView) findViewById(R.id.airplane);
        for (int i = PhoneConstants.SIM_ID_1 ; i < mSimCount; i++) {
            final int k = i+1;
            if (i == PhoneConstants.SIM_ID_1) {
                // load views for first SIM card
                mMobile[i]                    = (ImageView) findViewById(R.id.mobile_signal);
                mMobileGroup[i]               = (ViewGroup) findViewById(R.id.mobile_combo);
                mMobileActivity[i]            = (ImageView) findViewById(R.id.mobile_inout);
                mMobileType[i]                = (ImageView) findViewById(R.id.mobile_type);
                mMobileRoam[i]                = (ImageView) findViewById(R.id.mobile_roaming);
                mSpacer[i]                    =             findViewById(R.id.spacer);
                mSignalClusterCombo[i]        = (ViewGroup) findViewById(R.id.signal_cluster_combo);
            } else {
                mMobile[i]                    = (ImageView) findViewWithTag("mobile_signal_"+k);
                mMobileGroup[i]               = (ViewGroup) findViewWithTag("mobile_combo_"+k);
                mMobileActivity[i]            = (ImageView) findViewWithTag("mobile_inout_"+k);
                mMobileType[i]                = (ImageView) findViewWithTag("mobile_type_"+k);
                mMobileRoam[i]                = (ImageView) findViewWithTag("mobile_roaming_"+k);
                mSpacer[i]                    =             findViewWithTag("spacer_"+k);
                mSignalClusterCombo[i]        = (ViewGroup) findViewWithTag("signal_cluster_combo_"+k);
            }
        }

        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup            = null;
        mWifi                 = null;
        mWifiActivity         = null;

        for(int i = 0; i < mSimCount ; i++) {
            mMobileGroup[i]          = null;
            mMobile[i]               = null;
            mMobileActivity[i]       = null;
            mMobileType[i]           = null;
            mSpacer[i]               = null;
            mMobileRoam[i]           = null;
        }

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    @Override
    public void setMobileDataIndicators(int slotId, boolean visible, 
            int activityIcon, int strengthIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            boolean roaming, int roamingId) {
        mMobileVisible[slotId] = visible;
        mMobileStrengthId[slotId] = strengthIcon;
        mMobileTypeId[slotId] = typeIcon;
        mMobileActivityId[slotId] = activityIcon;
        mMobileDescription[slotId] = contentDescription;
        mMobileTypeDescription[slotId] = typeContentDescription;
        mRoaming[slotId] = roaming;
        mRoamingId[slotId] = roamingId;
        apply();
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        apply();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup != null && mWifiGroup.getContentDescription() != null) {
            event.getText().add(mWifiGroup.getContentDescription());
        }
        for (int i = 0; i < mSimCount ; i++) {
            if (mMobileVisible[i] && mMobileGroup[i] != null
                && mMobileGroup[i].getContentDescription() != null) {
                event.getText().add(mMobileGroup[i].getContentDescription());
            }
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        apply();
    }

    // Run after each indicator change.
    public void apply() {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifi.setImageResource(mWifiStrengthId);
            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("wifi: %s sig=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId));

        for (int i = 0; i < mSimCount ; i++) {
            if (DEBUG) Log.d(TAG, "apply(), slotId=" + i +", mMobileVisible=" + mMobileVisible[i]);
            if (mMobileVisible[i] && !mIsAirplaneMode) {
            	  mSignalClusterCombo[i].setVisibility(View.VISIBLE);
                if (mRoaming[i]) {
                    mMobileRoam[i].setBackgroundResource(mRoamingId[i]);
                    mMobileRoam[i].setVisibility(View.VISIBLE);
                } else {
                    mMobileRoam[i].setVisibility(View.GONE);
                }

                if (mMobileVisible[i] && !mIsAirplaneMode) {
                    mMobile[i].setImageResource(mMobileStrengthId[i]);
                    mMobileType[i].setImageResource(mMobileTypeId[i]);
                    mMobileType[i].setVisibility(!mWifiVisible ? View.VISIBLE : View.GONE);
                    mMobileGroup[i].setContentDescription(mMobileTypeDescription[i] + " " + mMobileDescription[i]);
                    mMobileActivity[i].setImageResource(mMobileActivityId[i]);
                    mMobileGroup[i].setVisibility(View.VISIBLE);
                } else {
                    mMobileGroup[i].setVisibility(View.GONE);
                }

                if (DEBUG) Log.d(TAG, "apply(), slotId=" + i + ", mRoaming=" + mRoaming[i]
                        + " mMobileActivityId=" + mMobileActivityId[i]
                        + String.format("mobile: %s sig=%d typ=%d ", 
                            (mMobileVisible[i] ? "VISIBLE" : "GONE")
                            , mMobileStrengthId[i], mMobileTypeId[i])
                        + " mIsAirplaneMode is " + mIsAirplaneMode);

                if (mMobileStrengthId[i] == R.drawable.stat_sys_sim_signal_null) {
                    mMobileType[i].setVisibility(View.GONE);
                    mMobileRoam[i].setVisibility(View.GONE);
                }
            } else {
                mSignalClusterCombo[i].setVisibility(View.GONE);
            }
        }

        if (mIsAirplaneMode) {
            mAirplane.setImageResource(mAirplaneIconId);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mWifiVisible) {
            mSpacer[0].setVisibility(View.INVISIBLE);
        } else {
            mSpacer[0].setVisibility(View.GONE);
        }
    }
}

