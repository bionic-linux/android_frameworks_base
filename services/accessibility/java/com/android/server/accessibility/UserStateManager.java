/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.accessibility;

import static android.accessibilityservice.AccessibilityTrace.FLAGS_PACKAGE_BROADCAST_RECEIVER;
import static android.accessibilityservice.AccessibilityTrace.FLAGS_USER_BROADCAST_RECEIVER;
import static android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.provider.Settings;
import android.safetycenter.SafetyCenterManager;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Handles queries into user state.
 *
 * This class will first return the current user. 
 *
 * Supports concurrent users.
 */
public class UserStateManager {

    public static final String LOG_TAG = "UserStateManager";

    // AccessibilityManagerService lock
    private Object mLock;

    // AccessibilityManagerService context
    private Context mContext;

    private int mCurrentUserId = UserHandle.USER_SYSTEM;

    private AccessibilityUserState.ServiceInfoChangeListener mServiceInfoChangeListener;

    // Distinction somewhere between real and proxy user states
    @VisibleForTesting
    final SparseArray<AccessibilityUserState> mUserStates = new SparseArray<>();

    public UserStateManager(Object a11yManagerLock, Context a11yManagerContext,
            AccessibilityUserState.ServiceInfoChangeListener serviceInfoChangeListener
            ) {
        mLock = a11yManagerLock;
        mContext = a11yManagerContext;
        mServiceInfoChangeListener = serviceInfoChangeListener;
    }


    @NonNull
    AccessibilityUserState getUserStateLocked(int userId) {
        AccessibilityUserState state = mUserStates.get(userId);
        if (state == null) {
            state = new AccessibilityUserState(userId, mContext, mServiceInfoChangeListener);
            mUserStates.put(userId, state);
        }
        return state;
    }

    AccessibilityUserState getUserState(int userId) {
        synchronized (mLock) {
            return getUserStateLocked(userId);
        }
    }

    AccessibilityUserState getCurrentUserStateLocked() {
        return getUserStateLocked(mCurrentUserId);
    }

    AccessibilityUserState getCurrentUserState() {
        synchronized (mLock) {
            return getCurrentUserStateLocked();
        }
    }

    int getCurrentUserIdLocked() {
        return mCurrentUserId;
    }

    int getCurrentUserId() {
        synchronized (mLock) {
            return getCurrentUserIdLocked();
        }
    }

    void setCurrentUserId(int currentUserId) {
        synchronized (mLock) {
            setCurrentUserIdLocked(currentUserId);
        }
    }

    void setCurrentUserIdLocked(int currentUserId) {
        mCurrentUserId = currentUserId;
    }

}
