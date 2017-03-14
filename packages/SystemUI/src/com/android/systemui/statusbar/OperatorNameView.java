/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.statusbar;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants.State;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.WirelessUtils;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;

import java.util.List;

public class OperatorNameView extends TextView implements DemoMode, DarkReceiver {

    private static final String KEY_SHOW_OPERATOR_NAME = "show_operator_name";

    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private CurrentUserTracker mUserTracker;
    private int mUser;

    private final ContentObserver mSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            update();
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                update();
            }
        }
    };
    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onRefreshCarrierInfo() {
            updateText();
        }
    };

    public OperatorNameView(Context context) {
        this(context, null);
    }

    public OperatorNameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OperatorNameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mUserTracker = new CurrentUserTracker(mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                mUser = newUserId;
                getContext().getContentResolver().unregisterContentObserver(mSettingObserver);
                getContext().getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(KEY_SHOW_OPERATOR_NAME), false, mSettingObserver,
                        newUserId);
                update();
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        mKeyguardUpdateMonitor.registerCallback(mCallback);
        mUser = ActivityManager.getCurrentUser();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(KEY_SHOW_OPERATOR_NAME), false, mSettingObserver, mUser);
        getContext().registerReceiver(mReceiver,
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
        update();
        mUserTracker.startTracking();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mKeyguardUpdateMonitor.removeCallback(mCallback);
        getContext().getContentResolver().unregisterContentObserver(mSettingObserver);
        getContext().unregisterReceiver(mReceiver);
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
        mUserTracker.stopTracking();
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
            update();
        } else if (mDemoMode && command.equals(COMMAND_OPERATOR)) {
            setText(args.getString("name"));
        }
    }

    private void update() {
        boolean showOperatorName = Settings.System.getIntForUser(getContext().getContentResolver(),
                KEY_SHOW_OPERATOR_NAME, 1, mUser) != 0;
        setVisibility(showOperatorName ? VISIBLE : GONE);

        boolean hasMobile = ConnectivityManager.from(mContext)
                .isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
        boolean airplaneMode = WirelessUtils.isAirplaneModeOn(mContext);
        if (!hasMobile || airplaneMode) {
            setText(null);
            setVisibility(GONE);
            return;
        }

        if (!mDemoMode) {
            updateText();
        }
    }

    private void updateText() {
        CharSequence displayText = null;
        List<SubscriptionInfo> subs = mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        final int N = subs.size();
        for (int i = 0; i < N; i++) {
            int subId = subs.get(i).getSubscriptionId();
            State simState = mKeyguardUpdateMonitor.getSimState(subId);
            CharSequence carrierName = subs.get(i).getCarrierName();
            if (!TextUtils.isEmpty(carrierName) && simState == State.READY) {
                ServiceState ss = mKeyguardUpdateMonitor.getServiceState(subId);
                if (ss != null && ss.getState() == ServiceState.STATE_IN_SERVICE) {
                    displayText = carrierName;
                    break;
                }
            }
        }

        setText(displayText);
    }
}
