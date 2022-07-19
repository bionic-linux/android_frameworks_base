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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityTrace;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.android.server.wm.WindowManagerInternal;

import java.util.List;

public class ProxyManager {
    private final Object mLock;

    // Used to determine if we should notify AccessibilityManager clients of updates.
    private int mLastState = -1;

    SparseArray<ProxyAccessibilityServiceConnectionImpl> mProxyAccessibilityServiceConnectionList =
            new SparseArray<>();

    ProxyManager(Object lock) {
        mLock = lock;
    }

    public void registerProxy(IAccessibilityServiceClient client, int displayId,
            Context context,
            int id, Handler mainHandler,
            AccessibilitySecurityPolicy securityPolicy,
            AbstractAccessibilityServiceConnection.SystemSupport systemSupport,
            AccessibilityTrace trace,
            WindowManagerInternal windowManagerInternal,
            SystemActionPerformer systemActionPerformer,
            AccessibilityWindowManager awm) throws RemoteException {

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        //
        info.setComponentName(new ComponentName("Exo package", "Exo class"));
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        // Need to make this available as a system API? What can't be changed at runtime?
        info.setCapabilities(AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT
                | AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION);
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;

        ProxyAccessibilityServiceConnectionImpl connection =
                new ProxyAccessibilityServiceConnectionImpl(
                context, info.getComponentName(), info,
                id, mainHandler, mLock, securityPolicy, systemSupport, trace,
                windowManagerInternal, null, awm, displayId);

        // Temporary holder.
        mProxyAccessibilityServiceConnectionList.put(displayId, connection);

        IBinder.DeathRecipient deathRecipient =
                new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                    client.asBinder().unlinkToDeath(this, 0);
                    mProxyAccessibilityServiceConnectionList.remove(displayId);
                    }
                };
        client.asBinder().linkToDeath(deathRecipient, 0);

//        // Exo is not read as part of settings. Need to directly affect mInstalledServices
//        mUserState.mInstalledServices.add(info);

        // This will add to the user state's bound services. This will also add the component name
        // and connection to a map.
        // This will also call onServiceInfoChangedLocked,
        // which checks security policy and notifies app clients of changes
        connection.initializeServiceInterface(client);

        //client.linkT
    }

    public boolean unregisterProxy(int displayId) {
        if (mProxyAccessibilityServiceConnectionList.contains(displayId)) {
            mProxyAccessibilityServiceConnectionList.remove(displayId);
            return true;
        }
        return false;
    }

    public SparseArray<ProxyAccessibilityServiceConnectionImpl> getProxies() {
        return mProxyAccessibilityServiceConnectionList;
    }

    public ProxyAccessibilityServiceConnectionImpl getProxy(int displayId) {
        return mProxyAccessibilityServiceConnectionList.get(displayId);
    }

    // User state will hold the windows, which we can use to send the event to the right user/proxy
    public void sendAccessibilityEvent(AccessibilityEvent event) {
        // send to proxy with display id
        for(int i = 0; i < mProxyAccessibilityServiceConnectionList.size(); i++) {
            int key = mProxyAccessibilityServiceConnectionList.keyAt(i);
            // get the object by the key.
            ProxyAccessibilityServiceConnectionImpl proxy =
                    mProxyAccessibilityServiceConnectionList.get(key);
            proxy.notifyAccessibilityEvent(event);
        }
    }


    boolean canRetrieveInteractiveWindowsLocked() {
        return (mProxyAccessibilityServiceConnectionList.size() > 0) &&
                canRetrieveInteractiveWindows();
    }

    /**
     * If any proxy can retrieve windows, return true
     */
    private boolean canRetrieveInteractiveWindows() {
        boolean observingWindows = false;
        for(int i = 0; i < mProxyAccessibilityServiceConnectionList.size(); i++) {
            int key = mProxyAccessibilityServiceConnectionList.keyAt(i);
            // get the object by the key.
            ProxyAccessibilityServiceConnectionImpl proxy =
                    mProxyAccessibilityServiceConnectionList.get(key);
            if (proxy.mRetrieveInteractiveWindows) {
                observingWindows = true;
                break;
            }
        }
        return observingWindows;
    }

    /**
     * If there is at least one proxy, show that accessibility is enabled.
     * @return
     */
    public int getStateLocked() {
        int clientState = 0;
        final boolean a11yEnabled = mProxyAccessibilityServiceConnectionList.size() > 0;
        if (a11yEnabled) {
            clientState |= AccessibilityManager.STATE_FLAG_ACCESSIBILITY_ENABLED;
        }


        // Touch exploration relies on enabled accessibility.
        if (a11yEnabled && isTouchExplorationEnabledLocked()) {
            clientState |= AccessibilityManager.STATE_FLAG_TOUCH_EXPLORATION_ENABLED;
        }
//        if (mIsTextHighContrastEnabled) {
//            clientState |= AccessibilityManager.STATE_FLAG_HIGH_TEXT_CONTRAST_ENABLED;
//        }
//        if (mIsAudioDescriptionByDefaultRequested) {
//            clientState |=
//                    AccessibilityManager.STATE_FLAG_AUDIO_DESCRIPTION_BY_DEFAULT_ENABLED;
//        }
//
//        clientState |= traceClientState;

        return clientState;
    }

    public int getLastSentStateLocked() {
        return mLastState;
    }

    public void setLastStateLocked(int proxyState) {
        mLastState = proxyState;
    }

    public boolean isTouchExplorationEnabledLocked() {
        boolean requestedTouchExploration = false;
        for(int i = 0; i < mProxyAccessibilityServiceConnectionList.size(); i++) {
            int key = mProxyAccessibilityServiceConnectionList.keyAt(i);
            // get the object by the key.
            ProxyAccessibilityServiceConnectionImpl proxy =
                    mProxyAccessibilityServiceConnectionList.get(key);
            // check capability
            if (proxy.mRequestTouchExplorationMode) {
                requestedTouchExploration = true;
                break;
            }
        }
        return requestedTouchExploration;
    }

    public void appendInstalledAccessibiiltyServices(List<AccessibilityServiceInfo> serviceInfos) {
        for(int i = 0; i < mProxyAccessibilityServiceConnectionList.size(); i++) {
            int key = mProxyAccessibilityServiceConnectionList.keyAt(i);
            // get the object by the key.
            ProxyAccessibilityServiceConnectionImpl proxy =
                    mProxyAccessibilityServiceConnectionList.get(key);
            serviceInfos.add(proxy.mAccessibilityServiceInfo);
        }
    }

    public void appendEnabledAccessibiiltyServices(List<AccessibilityServiceInfo> result) {
        for(int i = 0; i < mProxyAccessibilityServiceConnectionList.size(); i++) {
            int key = mProxyAccessibilityServiceConnectionList.keyAt(i);
            // get the object by the key.
            ProxyAccessibilityServiceConnectionImpl proxy =
                    mProxyAccessibilityServiceConnectionList.get(key);
            result.add(proxy.mAccessibilityServiceInfo);
        }
    }
}
