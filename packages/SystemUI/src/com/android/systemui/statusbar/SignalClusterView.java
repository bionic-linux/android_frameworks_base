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
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "SignalClusterView";

    NetworkController mNC;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0;
    private boolean mMobileVisible = false;
    private int[] mMobileStrengthId;
    private int[] mMobileTypeId;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileTypeDescription;
    private String[] mMobileDescription;

    ViewGroup mWifiGroup;
    ImageView mWifi, mAirplane;
    ViewGroup[] mMobileGroup;
    ImageView[] mMobile;
    ImageView[] mMobileType;
    View mSpacer;
    private int[] mMobileGroupResourceId = {R.id.mobile_combo, R.id.mobile_combo_sub2,
                                          R.id.mobile_combo_sub3};
    private int[] mMobileResourceId = {R.id.mobile_signal, R.id.mobile_signal_sub2,
                                     R.id.mobile_signal_sub3};
    private int[] mMobileTypeResourceId = {R.id.mobile_type, R.id.mobile_type_sub2,
                                         R.id.mobile_type_sub3};
    private int mNumPhones = TelephonyManager.getDefault().getPhoneCount();

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMobileStrengthId = new int[mNumPhones];
        mMobileDescription = new String[mNumPhones];
        mMobileTypeId = new int[mNumPhones];
        mMobileGroup = new ViewGroup[mNumPhones];
        mMobile = new ImageView[mNumPhones];
        mMobileType = new ImageView[mNumPhones];
        for(int i=0; i < mNumPhones; i++) {
            mMobileStrengthId[i] = 0;
            mMobileTypeId[i] = 0;
        }
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG) Log.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mSpacer         =             findViewById(R.id.spacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        for (int i = 0; i < mNumPhones; i++) {
            mMobileGroup[i]    = (ViewGroup) findViewById(mMobileGroupResourceId[i]);
            mMobile[i]         = (ImageView) findViewById(mMobileResourceId[i]);
            mMobileType[i]     = (ImageView) findViewById(mMobileTypeResourceId[i]);
        }
        apply(TelephonyManager.getDefault().getDefaultSubscription());
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup      = null;
        mWifi           = null;
        mSpacer         = null;
        mAirplane       = null;
        for (int i = 0; i < mNumPhones; i++) {
            mMobileGroup[i]    = null;
            mMobile[i]         = null;
            mMobileType[i]     = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiDescription = contentDescription;

        apply(TelephonyManager.getDefault().getDefaultSubscription());
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int strengthIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            int subscription) {
        mMobileVisible = visible;
        mMobileStrengthId[subscription] = strengthIcon;
        mMobileTypeId[subscription] = typeIcon;
        mMobileDescription[subscription] = contentDescription;
        mMobileTypeDescription = typeContentDescription;

        apply(subscription);
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        apply(TelephonyManager.getDefault().getDefaultSubscription());
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup != null && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        if (mMobileVisible && mMobileGroup[PhoneConstants.DEFAULT_SUBSCRIPTION] != null &&
                 mMobileGroup[PhoneConstants.DEFAULT_SUBSCRIPTION].getContentDescription() != null)
            event.getText().add(mMobileGroup[PhoneConstants.DEFAULT_SUBSCRIPTION].
                    getContentDescription());
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mWifi != null) {
            mWifi.setImageDrawable(null);
        }
        for (int i = 0; i < mNumPhones; i++) {
            if (mMobile[i] != null) {
                mMobile[i].setImageDrawable(null);
            }

            if (mMobileType[i] != null) {
                mMobileType[i].setImageDrawable(null);
            }
        }

        if(mAirplane != null) {
            mAirplane.setImageDrawable(null);
        }

        for (int i = 0; i < mNumPhones; i++) {
            apply(i);
        }
    }

    // Run after each indicator change.
    private void apply(int subscription) {
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

        if (mMobileVisible && !mIsAirplaneMode) {
            mMobile[subscription].setImageResource(mMobileStrengthId[subscription]);
            mMobileType[subscription].setImageResource(mMobileTypeId[subscription]);

            mMobileGroup[subscription].setContentDescription(mMobileTypeDescription + " "
                    + mMobileDescription[subscription]);
            mMobileGroup[subscription].setVisibility(View.VISIBLE);
        } else {
            mMobileGroup[subscription].setVisibility(View.GONE);
        }

        if (mIsAirplaneMode) {
            mAirplane.setImageResource(mAirplaneIconId);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (subscription != 0) {
            if (mMobileVisible && mWifiVisible && mIsAirplaneMode) {
                mSpacer.setVisibility(View.INVISIBLE);
            } else {
                mSpacer.setVisibility(View.GONE);
            }
        }

        if (DEBUG) Log.d(TAG,
                String.format("mobile: %s sig=%d typ=%d",
                    (mMobileVisible ? "VISIBLE" : "GONE"),
                    mMobileStrengthId[subscription], mMobileTypeId[subscription]));

        mMobileType[subscription].setVisibility(
                !mWifiVisible ? View.VISIBLE : View.GONE);
    }
}

