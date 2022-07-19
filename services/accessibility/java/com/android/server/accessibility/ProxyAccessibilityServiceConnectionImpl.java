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

import static com.android.internal.util.function.pooled.PooledLambda.obtainMessage;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityTrace;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.MagnificationConfig;
import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.graphics.Region;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.Nullable;

import com.android.server.wm.WindowManagerInternal;

import java.util.List;

/**
 * Represents the system connection to an {@link android.view.accessibility.AccessibilityProxy}.
 * This does not handle input-related events.
 */
public class ProxyAccessibilityServiceConnectionImpl extends AbstractAccessibilityServiceConnection {
    private static final String LOG_TAG = "ProxyAccessibilityServiceConnectionImpl";
    private int mDisplayId;

    List<AccessibilityServiceInfo> mInstalledAndEnabledServices;

    ProxyAccessibilityServiceConnectionImpl(
            Context context,
            ComponentName componentName,
            AccessibilityServiceInfo accessibilityServiceInfo, int id,
            Handler mainHandler, Object lock,
            AccessibilitySecurityPolicy securityPolicy,
            SystemSupport systemSupport, AccessibilityTrace trace,
            WindowManagerInternal windowManagerInternal,
            SystemActionPerformer systemActionPerfomer,
            AccessibilityWindowManager awm, int displayId) {
        super(context, componentName, accessibilityServiceInfo, id, mainHandler, lock,
                securityPolicy, systemSupport, trace, windowManagerInternal, null,
                awm);
        mDisplayId = displayId;
    }

    void initializeServiceInterface(IAccessibilityServiceClient serviceInterface)
            throws RemoteException {
        mServiceInterface = serviceInterface;
        mService = serviceInterface.asBinder();
        mServiceInterface.init(this, mId, this.mOverlayWindowTokens.get(mDisplayId));
    }

//    // DeathRecipient
//    @Override
    public void binderDied() {
        // Remove resources
     //   mSystemSupport.onClientChangeLocked(false);
    }

    public void setInstalledAndEnabledServices(List<AccessibilityServiceInfo> infos) {
        // Call a change every time
        mInstalledAndEnabledServices = infos;
        for (AccessibilityServiceInfo info : mInstalledAndEnabledServices) {
            setDynamicallyConfigurableProperties(info);
        }
        mSystemSupport.onClientChangeLocked(true);
    }

    @Override
    public boolean onKeyEvent(KeyEvent keyEvent, int sequenceNumber) {
        return false;
    }

    /**
     * AccessibilityServiceInfo is not in manifests.
     * @param info
     */
    @Override
    public void setServiceInfo(AccessibilityServiceInfo info) {
        final long identity = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                AccessibilityServiceInfo oldInfo = mAccessibilityServiceInfo;
                if (oldInfo != null) {
                    oldInfo.updateDynamicallyConfigurableProperties(null, info);
                    oldInfo.setAccessibilityTool(info.isAccessibilityTool());
                    oldInfo.setCapabilities(info.getCapabilities());
                    oldInfo.setComponentName(info.getComponentName());
                    setDynamicallyConfigurableProperties(info);
                } else {
                    setDynamicallyConfigurableProperties(info);
                }
                mSystemSupport.onClientChangeLocked(true);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override
    protected boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo info) {
        // Don't need to check for earlier APIs.
        return true;
    }

    @Override
    public void disableSelf() {
    }

    @Override
    public void setCacheEnabled(boolean enabled) {
        super.setCacheEnabled(enabled); // why system support?
    }


    // Related to service: ServiceConnection
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Do nothing
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Do nothing
    }



    // Ignore fingerprints. FingerprintGestureClient
    @Override
    public boolean isCapturingFingerprintGestures() {
        return false;
    }

    @Override
    public void onFingerprintGestureDetectionActiveChanged(boolean active) {}

    @Override
    public void onFingerprintGesture(int gesture) {}

    // IAccessibilityServiceConnection methods that are stubbed
    @Override
    public boolean performGlobalAction(int action) {
        return false;
    }

    @Override
    public void setOnKeyEventResult(boolean handled, int sequence) {
        // calls into system support
    }

    @Override
    protected boolean hasRightsToCurrentUserLocked() {
        return true;
    }

    @Override
    public @NonNull
    List<AccessibilityNodeInfo.AccessibilityAction> getSystemActions() {
        return null;
    }

    // Magnification not needed.
    @Nullable
    @Override
    public MagnificationConfig getMagnificationConfig(int displayId) {
        return null;
    }

    @Override
    public float getMagnificationScale(int displayId) {
        return -1;
    }

    @Override
    public float getMagnificationCenterX(int displayId) {
        return -1;
    }

    @Override
    public float getMagnificationCenterY(int displayId) {
        return -1;
    }

    @Override
    public Region getMagnificationRegion(int displayId) {
        return null;
    }

    @Override
    public Region getCurrentMagnificationRegion(int displayId) {
        return null;
    }

    @Override
    public boolean resetMagnification(int displayId, boolean animate) {
        return false;
    }

    @Override
    public boolean resetCurrentMagnification(int displayId, boolean animate) {
        return false;
    }

    @Override
    public boolean setMagnificationConfig(int displayId,
            @androidx.annotation.NonNull MagnificationConfig config, boolean animate) {
        return false;
    }

    @Override
    public void setMagnificationCallbackEnabled(int displayId, boolean enabled) {}

    // This is only in AbstractA11yServiceConnection
    @Override
    public boolean isMagnificationCallbackEnabled(int displayId) {
        return false;
    }

    @Override
    public boolean setSoftKeyboardShowMode(int showMode) {
        return false;
    }

    @Override
    public int getSoftKeyboardShowMode() {
        return 0;
    }

    @Override
    public void setSoftKeyboardCallbackEnabled(boolean enabled) {

    }
    @Override
    public boolean switchToInputMethod(String imeId) {
        return false;
    }
    @Override
    public int setInputMethodEnabled(String imeId, boolean enabled) {
        return 0;
    }

    @Override
    public boolean isAccessibilityButtonAvailable() {
        return false;
    }

    @Override
    public void sendGesture(int sequence, ParceledListSlice gestureSteps) {
        // Do nothing;
    }

    @Override
    public void dispatchGesture(int sequence, ParceledListSlice gestureSteps, int displayId) {
    }

    @Override
    public boolean isFingerprintGestureDetectionAvailable() {
        return false;
    }


    @Override
    public int getWindowIdForLeashToken(@androidx.annotation.NonNull IBinder token) {
        return -1;

    }
    @Override
    public void takeScreenshot(int displayId, RemoteCallback callback) {
    }

    @Override
    public void setGestureDetectionPassthroughRegion(int displayId, Region region) {
    }

    @Override
    public void setTouchExplorationPassthroughRegion(int displayId, Region region) {
    }

    @Override
    public void setServiceDetectsGesturesEnabled(int displayId, boolean mode) {
        super.setServiceDetectsGesturesEnabled(displayId, mode);
    }

    // Don't need touch events, since input is proxied.
    @Override
    public void requestTouchExploration(int displayId) {
    }

    @Override
    public void requestDragging(int displayId, int pointerId) {
    }

    @Override
    public void requestDelegating(int displayId) {
    }

    @Override
    public void onDoubleTap(int displayId) {
    }

    @Override
    public void onDoubleTapAndHold(int displayId) {
    }

    @Override
    public void setAnimationScale(float scale) {
        super.setAnimationScale(scale);
    }

    // AccessibilityServiceConnection methods that are overridden.
//    @Override
//    public void bindLocked() {
//    }
//
//    @Override
//    public void unbindLocked() {
//    }
//
//    // canRetrieveInteractiveWindowsLocked()
//    // getServiceInfo
//
//    @Override
//    public boolean isAccessibilityButtonAvailableLocked(AccessibilityUserState userState) {
//        return false;
//    }
//
//    @Override
//    public void notifyMotionEvent(MotionEvent event) {
//    }
//
//    @Override
//    public void notifyTouchState(int displayId, int state) {
//    }
//
//    @Override
//    public boolean requestImeApis() {
//        return false;
//    }
}
