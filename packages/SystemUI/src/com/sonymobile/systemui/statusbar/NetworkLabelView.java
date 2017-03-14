/*
 * Copyright (C) 2017 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.sonymobile.systemui.statusbar;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants.State;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

import java.util.List;

public class NetworkLabelView extends TextView implements DemoMode, DarkReceiver {

    private static final String TAG = NetworkLabelView.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    /**
     * A key that is used to retrieve the value of the checkbox
     * in Settings application that allows a user to add or remove
     * the operator name in statusbar.
     */
    private static final String SHOW_OPERATOR_NAME = "show_operator_name";

    private boolean mAttached;
    private ContentObserver mShowOperatorNameObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) Log.d(TAG, "onChange(" + selfChange + ")");
            if (shouldShowOperatorName()) {
                setVisibility(View.VISIBLE);
                updateText();
            } else {
                setVisibility(View.GONE);
            }
        }
    };
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onRefreshCarrierInfo() {
            if (DEBUG) Log.d(TAG, "onRefreshCarrierInfo()");
            updateText();
        }
    };

    public NetworkLabelView(Context context) {
        this(context, null);
    }

    public NetworkLabelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkLabelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) Log.d(TAG, "onAttachedToWindow() mAttached=" + mAttached);
        if (!mAttached) {
            mAttached = true;
            Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
            // If the phone is configured to show the operator name
            // then register the observer.
            if (isShowOperatorNameAvailable()) {
                mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
                mKeyguardUpdateMonitor.registerCallback(mCallback);
                mShowOperatorNameObserver.onChange(false); // setup
                getContext().getContentResolver()
                        .registerContentObserver(Settings.System.getUriFor(SHOW_OPERATOR_NAME),
                                true, mShowOperatorNameObserver);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) Log.d(TAG, "onDetachedFromWindow() mAttached=" + mAttached);
        if (mAttached) {
            mAttached = false;
            Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
            if (mKeyguardUpdateMonitor != null) mKeyguardUpdateMonitor.removeCallback(mCallback);
            getContext().getContentResolver().unregisterContentObserver(mShowOperatorNameObserver);
        }
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        setTextColor(DarkIconDispatcher.getTint(area, this, tint));
    }

    private boolean mDemoMode;

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            mDemoMode = true;
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            updateText();
        } else if (mDemoMode && command.equals(COMMAND_OPERATOR_NAME)) {
            String operatorName = args.getString("operator");
            if (operatorName != null) {
                setText(operatorName);
            }
        }
    }

    private void updateText() {
        if (mDemoMode || getVisibility() == View.GONE) return;
        CharSequence displayText = "";
        List<SubscriptionInfo> subs = mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        final int N = subs.size();
        if (DEBUG) Log.d(TAG, "updateText(): " + N);
        for (int i = 0; i < N; i++) {
            int subId = subs.get(i).getSubscriptionId();
            State simState = mKeyguardUpdateMonitor.getSimState(subId);
            CharSequence carrierName = subs.get(i).getCarrierName();
            if (DEBUG) {
                Log.d(TAG, "subId=" + subId + ", simState=" + simState + ", carrierName=" +
                        carrierName);
            }
            if (simState != State.READY) carrierName = "";
            displayText = carrierName;
        }

        if (displayText.equals(getContext().getString(
                com.android.internal.R.string.lockscreen_carrier_default))
                || displayText.equals(getContext().getString(
                com.android.internal.R.string.emergency_calls_only))) {
            displayText = "";
        }
        if (DEBUG) Log.d(TAG, "displayText=" + displayText);
        setText(displayText);
    }

    private boolean shouldShowOperatorName() {
        boolean showOperatorName = 0 != Settings.System
                .getInt(getContext().getContentResolver(), SHOW_OPERATOR_NAME, 1);
        if (DEBUG) Log.d(TAG, "shouldShowOperatorName() showOperatorName=" + showOperatorName);
        DeviceProvisionedController provisionedController = Dependency
                .get(DeviceProvisionedController.class);
        if (!provisionedController.isDeviceProvisioned()) {
            // When the device is not provisioned SHOW_OPERATOR_NAME is not
            // available in the database, so showOperatorName variable is not valid.
            // In this case, use the default value (true)
            if (DEBUG) Log.d(TAG, "shouldShowOperatorName() Not provisioned");
            showOperatorName = true;
        }
        return showOperatorName;
    }

    private boolean isShowOperatorNameAvailable() {
        boolean isAvailable = getContext().getResources().
                getBoolean(R.bool.config_showOperatorNameInStatusBar);
        if (DEBUG) Log.d(TAG, "config_showOperatorNameInStatusBar=" + isAvailable);
        if (isAvailable) {
            final TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.
                    TELEPHONY_SERVICE);
            if (tm != null) {
                // Dualsim phones and phones without simcard should not show
                // operatorname in statusbar.
                final boolean isMultiSim = tm.getPhoneCount() > 1;
                if (DEBUG) {
                    Log.d(TAG, "isMultiSim=" + isMultiSim + ", isSmsCapable=" + tm.isSmsCapable());
                }
                if (isMultiSim || !tm.isSmsCapable()) {
                    isAvailable = false;
                }
            } else {
                isAvailable = false;
            }
        }
        return isAvailable;
    }
}
