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

package android.view.accessibility;

import android.accessibilityservice.AccessibilityGestureEvent;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.accessibilityservice.MagnificationConfig;
import android.annotation.ColorInt;
import android.annotation.SystemApi;
import android.content.Context;
import android.graphics.Region;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.internal.inputmethod.IAccessibilityInputMethodSessionCallback;
import com.android.internal.inputmethod.RemoteAccessibilityInputConnection;

import java.util.ArrayList;
import java.util.List;


/**
 * Allows a privileged app to interact with the windows in the display that this proxy represents.
 * In the accessibility system, a proxy will represent an individual user. Proxying the
 * default display will throw an exception. Only the real user has access to global clients like
 * SystemUI and will be able to receive and send input-related events.
 *
 *
 * @hide
 */
@SystemApi
public abstract class AccessibilityProxy {
    private HandlerThread mRemoteCallbackThread;

    IAccessibilityServiceClient mServiceClient;
    private static final int INVALID = -1;

    private int mConnectionId = INVALID;
    private int mDisplayId = INVALID;

    private AccessibilityServiceInfo mInfo;
    private List<AccessibilityServiceInfo> mInstalledAndEnabledServices = new ArrayList<>();

    private static final String LOG_TAG = "AccessibilityProxy";

    public AccessibilityProxy(int displayId, @NonNull AccessibilityServiceInfo serviceInfo) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            throw new IllegalArgumentException("The default display cannot be proxy-ed.");
        }
        mDisplayId = displayId;
        mInfo = serviceInfo;
        // This is how UiAutomation does it. The AccessibilityProxy constructor could accept
        // a context and a looper.
        // Need a looper to use for system callbacks. The base implementation
        // - IAccessibilityServiceClientWrapper - puts the messages on this thread.
        // Do not use getMainLooper since this will use EXO's process looper.
        mRemoteCallbackThread = new HandlerThread("AccessibilityProxy");
        mRemoteCallbackThread.start();
        // Context is null right now. May need a context for an attribution tag.
        // (but doesn't seem to be necessary). b/190659583. Only relates to errorprone checks.
        mServiceClient = new IAccessibilityServiceClientImpl(null,
                mRemoteCallbackThread.getLooper());
    }

    public int getDisplayId() {
        return mDisplayId;
    }

    /**
     * Callback for {@link android.view.accessibility.AccessibilityEvent}s.
     */
    public abstract void onAccessibilityEvent(@NonNull AccessibilityEvent event);

    public abstract void onProxyConnected();

    /**
     * Callback for interrupting the accessibility feedback.
     */
    public abstract void onInterrupt();

    // A proxy can decide to turn itself off.
    public void disableSelf() {
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance().getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.disableSelf();
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }
    }

    @Nullable
    public AccessibilityNodeInfo findFocus(int focus) {
        return AccessibilityInteractionClient.getInstance().findFocus(mConnectionId,
                AccessibilityWindowInfo.ANY_WINDOW_ID, AccessibilityNodeInfo.ROOT_NODE_ID, focus);
    }

    @Nullable
    public final AccessibilityServiceInfo getServiceInfo() {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance().getConnection(mConnectionId);
        if (connection != null) {
            try {
                return connection.getServiceInfo();
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while getting AccessibilityServiceInfo", re);
                re.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public final void setServiceInfo(@NonNull AccessibilityServiceInfo info) {
        mInfo = info;
        sendServiceInfo();
    }
    @Nullable
    public AccessibilityNodeInfo getRootInActiveWindow() {
        return getRootInActiveWindow(AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID);
    }

    @Nullable
    public AccessibilityNodeInfo getRootInActiveWindow(
            @AccessibilityNodeInfo.PrefetchingStrategy int prefetchingStrategy) {
        return AccessibilityInteractionClient.getInstance().getRootInActiveWindow(
                mConnectionId, prefetchingStrategy);
    }

    /**
     * Sets the {@link AccessibilityServiceInfo} for this service if the latter is
     * properly set and there is an {@link IAccessibilityServiceConnection} to the
     * AccessibilityManagerService.
     */
    private void sendServiceInfo() {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance().getConnection(mConnectionId);
        if (mInfo != null && connection != null) {
            try {
                connection.setServiceInfo(mInfo);
                mInfo = null;
                AccessibilityInteractionClient.getInstance().clearCache(mConnectionId);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", re);
                re.rethrowFromSystemServer();
            }
        }
    }

    @NonNull
    public List<AccessibilityWindowInfo> getWindows() {
        return AccessibilityInteractionClient.getInstance().getWindows(mConnectionId);
    }

    /**
     * Sets the strokeWidth and color of the accessibility focus rectangle.
     * <p>
     * <strong>Note:</strong> This setting persists until this or another active
     * AccessibilityService changes it or the device reboots.
     * </p>
     *
     * @param strokeWidth The stroke width of the rectangle in pixels.
     *                    Setting this value to zero results in no focus rectangle being drawn.
     * @param color The color of the rectangle.
     */
    public void setAccessibilityFocusAppearance(int strokeWidth, @ColorInt int color) {
        IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance().getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.setFocusAppearance(strokeWidth, color);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while setting the strokeWidth and color of the "
                        + "accessibility focus rectangle", re);
                re.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Sets the system settings values that control the scaling factor for animations. The scale
     * controls the animation playback speed for animations that respect these settings. Animations
     * that do not respect the settings values will not be affected by this function. A lower scale
     * value results in a faster speed. A value of <code>0</code> disables animations entirely. When
     * animations are disabled services receive window change events more quickly which can reduce
     * the potential by confusion by reducing the time during which windows are in transition.
     *
     * @see AccessibilityEvent#TYPE_WINDOWS_CHANGED
     * @see AccessibilityEvent#TYPE_WINDOW_STATE_CHANGED
     * @see android.provider.Settings.Global#WINDOW_ANIMATION_SCALE
     * @see android.provider.Settings.Global#TRANSITION_ANIMATION_SCALE
     * @see android.provider.Settings.Global#ANIMATOR_DURATION_SCALE
     * @param scale The scaling factor for all animations.
     */
    public void setAnimationScale(float scale) {
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getInstance().getConnection(mConnectionId);
        if (connection != null) {
            try {
                connection.setAnimationScale(scale);
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }
    }

    /** Sets the cache status.
     *
     * <p>If {@code enabled}, enable the cache and prefetching. Otherwise, disable the cache
     * and prefetching.
     * Note: By default the cache is enabled.
     * @param enabled whether to enable or disable the cache.
     * @return {@code true} if the cache and connection are not null, so the cache status is set.
     */
    public boolean setCacheEnabled(boolean enabled) {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        final IAccessibilityServiceConnection connection =
                AccessibilityInteractionClient.getConnection(mConnectionId);
        if (connection == null) {
            return false;
        }
        try {
            connection.setCacheEnabled(enabled);
            cache.setEnabled(enabled);
            return true;
        } catch (RemoteException re) {
            Log.w(LOG_TAG, "Error while setting status of cache", re);
            re.rethrowFromSystemServer();
        }
        return false;
    }

    /** Invalidates {@code node} and its subtree in the cache.
     * @param node the node to invalidate.
     * @return {@code true} if the subtree rooted at {@code node} was invalidated.
     */
    public boolean clearCachedSubtree(@NonNull AccessibilityNodeInfo node) {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        return cache.clearSubTree(node);
    }

    /** Clears the cache.
     * @return {@code true} if the cache was cleared
     */
    public boolean clearCache() {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        cache.clear();
        return true;
    }

    /** Checks if {@code node} is in the cache.
     * @param node the node to check.
     * @return {@code true} if {@code node} is in the cache.
     */
    public boolean isNodeInCache(@NonNull AccessibilityNodeInfo node) {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        return cache.isNodeInCache(node);
    }

    /** Returns {@code true} if the cache is enabled. */
    public boolean isCacheEnabled() {
        AccessibilityCache cache =
                AccessibilityInteractionClient.getCache(mConnectionId);
        if (cache == null) {
            return false;
        }
        return cache.isEnabled();
    }

    public void setInstalledAndEnabledServices(
            @NonNull List<AccessibilityServiceInfo> installedAndEnabledServices) {
        // Do something here.
        mInstalledAndEnabledServices = installedAndEnabledServices;

    }


    /**
     * An IAccessibilityServiceClient that handles interrupts and accessibility events.
     */
    private class IAccessibilityServiceClientImpl extends
            AccessibilityService.IAccessibilityServiceClientWrapper {

        public IAccessibilityServiceClientImpl(Context context, Looper looper) {
            super(context, looper, new AccessibilityService.Callbacks() {
                @Override
                public void onAccessibilityEvent(AccessibilityEvent event) {
                    AccessibilityProxy.this.onAccessibilityEvent(event);
                }

                @Override
                public void onInterrupt() {
                    AccessibilityProxy.this.onInterrupt();
                }

                @Override
                public void onServiceConnected() {
                    AccessibilityProxy.this.onProxyConnected();
                }

                @Override
                public void init(int connectionId, IBinder windowToken) {
                    mConnectionId = connectionId;
                    // Update default service info.
                    AccessibilityProxy.this.sendServiceInfo();
                }

                @Override
                public boolean onGesture(AccessibilityGestureEvent gestureInfo) {
                    return false;
                }

                @Override
                public boolean onKeyEvent(KeyEvent event) {
                    return false;
                }

                @Override
                public void onMagnificationChanged(int displayId, @NonNull Region region,
                        MagnificationConfig config) {

                }

                @Override
                public void onMotionEvent(MotionEvent event) {
                    // Do nothing.
                }

                @Override
                public void onTouchStateChanged(int displayId, int state) {
                    // Do nothing.
                }

                @Override
                public void onSoftKeyboardShowModeChanged(int showMode) {
                    // Do nothing.
                }

                @Override
                public void onPerformGestureResult(int sequence, boolean completedSuccessfully) {
                    // Do nothing.
                }

                @Override
                public void onFingerprintCapturingGesturesChanged(boolean active) {
                    // Do nothing.
                }

                @Override
                public void onFingerprintGesture(int gesture) {
                    // Do nothing.
                }

                @Override
                public void onAccessibilityButtonClicked(int displayId) {
                    // Do nothing.
                }

                @Override
                public void onAccessibilityButtonAvailabilityChanged(boolean available) {
                    // Do nothing.

                }

                @Override
                public void onSystemActionsChanged() {
                    // Do nothing.
                }

                @Override
                public void createImeSession(IAccessibilityInputMethodSessionCallback callback) {
                    // Do nothing.
                }

                @Override
                public void startInput(@Nullable RemoteAccessibilityInputConnection inputConnection,
                        @NonNull EditorInfo editorInfo, boolean restarting) {
                    // Do nothing.
                }
            });
        }
    }
}
