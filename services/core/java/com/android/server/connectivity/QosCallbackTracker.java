/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.connectivity;

import android.annotation.NonNull;
import android.net.ConnectivityManager;
import android.net.IQosCallback;
import android.net.QosCallbackException;
import android.net.QosFilter;
import android.net.QosSession;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ServiceSpecificException;
import android.util.Slog;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Tracks qos callbacks and handles the communication between the network agent and application.
 *
 * Any method prefixed by handle must be called from the ConnectivityService handler thread.
 *
 * @hide
 */
public class QosCallbackTracker {
    static final String TAG = QosCallbackTracker.class.getSimpleName();
    static final boolean DBG = true;

    private final Handler mConnectivityServiceHandler;

    /**
     * Each agent gets a unique callback id that is used to proxy messages back to the original
     * callback.
     */
    private int mAgentCallbackIdCounter = 1;

    @NonNull private final QosCallbackAgentConnectionCollection mCallbacks =
            new QosCallbackAgentConnectionCollection();

    private QosFilterValidator mValidator;

    @NonNull private final Consumer<Integer> mEnforceRequestCountLimit;
    @NonNull private final Consumer<Integer> mDecrementNetworkRequestPerUidCount;

    public QosFilterValidator getValidator() {
        return mValidator;
    }

    public QosCallbackTracker(@NonNull final Handler handler,
            @NonNull final QosFilterValidator validator,
            @NonNull final Consumer<Integer> enforceRequestCountLimit,
            @NonNull final Consumer<Integer> decrementNetworkRequestPerUidCount) {
        mConnectivityServiceHandler = handler;
        mValidator = validator;
        mEnforceRequestCountLimit = enforceRequestCountLimit;
        mDecrementNetworkRequestPerUidCount = decrementNetworkRequestPerUidCount;
    }

    /**
     * Registers the callback with the tracker
     * @param networkAgentInfo the info of the corresponding network agent
     * @param callback the callback to register
     * @param filter the filter being registered alongside the callback
     */
    public void registerCallback(@NonNull final NetworkAgentInfo networkAgentInfo,
            @NonNull final IQosCallback callback, @NonNull final QosFilter filter) {
        int uid = Binder.getCallingUid();
        mConnectivityServiceHandler.post(() ->
                handleRegisterCallback(networkAgentInfo, callback, filter, uid)
        );
    }

    private void handleRegisterCallback(@NonNull final NetworkAgentInfo networkAgentInfo,
            @NonNull final IQosCallback callback, @NonNull final QosFilter filter, int uid) {

        IBinder binder = callback.asBinder();
        if (mCallbacks.get(binder) != null) {
            logwtf("handleRegisterCallback: Callback should never be registered twice");
            return;
        }

        int newCallbackId = mAgentCallbackIdCounter++;
        QosCallbackAgentConnection ac = new QosCallbackAgentConnection(this, newCallbackId,
                networkAgentInfo, callback, filter, uid);

        // Checks the request count limit.
        try {
            mEnforceRequestCountLimit.accept(uid);
        }  catch (ServiceSpecificException ex) {
            if (ex.errorCode == ConnectivityManager.Errors.TOO_MANY_REQUESTS) {
                ac.sendEventQosSessionError(
                        QosCallbackException.EX_TYPE_TOO_MANY_REQUESTS, "");
            } else {
                logwtf("handleRegisterCallback: unknown ServiceSpecificException exception");
            }
            return;
        }

        // Only add to the callback maps if the NetworkAgent successfully registered it
        if (ac.sendCmdRegisterCallback()) {
            mCallbacks.put(ac);

            if (DBG) log("handleRegisterCallback: added callback " + newCallbackId);
        } else {
            if (DBG) log("handleRegisterCallback: error sending register callback");

            // There was an issue with registering with the agent and so we need to decrement
            // the uid counter.
            mDecrementNetworkRequestPerUidCount.accept(ac.getUid());
        }
    }

    /**
     * Unregisters callback
     * @param callback callback to unregister
     */
    public void unregisterCallback(@NonNull final IQosCallback callback) {
        mConnectivityServiceHandler.post(() -> handleUnregisterCallback(callback.asBinder(), true));
    }

    private void handleUnregisterCallback(@NonNull final IBinder binder,
            boolean sendToNetworkAgent) {
        QosCallbackAgentConnection agentConnection = mCallbacks.get(binder);
        if (agentConnection != null) {
            if (DBG) {
                log("handleUnregisterCallback: unregister "
                        + agentConnection.getAgentCallbackId());
            }

            mDecrementNetworkRequestPerUidCount.accept(agentConnection.getUid());
            mCallbacks.remove(agentConnection);

            if (sendToNetworkAgent) {
                agentConnection.sendCmdUnregisterCallback();
            }
            agentConnection.unlinkToDeathRecipient();
        } else {
            logw("handleUnregisterCallback: agentConnection is null");
        }
    }

    /**
     * Called when the NetworkAgent sends the qos session available event
     *
     * Note: Must be called on the connectivity service handler thread
     *
     * @param msg wraps the qos session attributes
     */
    public void handleEventQosSessionAvailable(@NonNull final Message msg) {
        QosCallbackAgentConnection ac = getConnection(msg, "handleEventQosSessionAvailable: ");
        if (ac != null) {
            ac.sendEventQosSessionAvailable((Bundle) msg.obj);
        }
    }

    /**
     * Called when the NetworkAgent sends the qos session lost event
     *
     * Note: Must be called on the connectivity service handler thread
     *
     * @param msg wraps the qos session attributes
     */
    public void handleEventQosSessionLost(@NonNull final Message msg) {
        QosCallbackAgentConnection ac = getConnection(msg, "handleEventQosSessionLost: ");
        if (ac != null) {
            ac.sendEventQosSessionLost((QosSession) msg.obj);
        }
    }

    /**
     * Called when the NetworkAgent sends the qos session on error event
     *
     * Note: Must be called on the connectivity service handler thread
     *
     * @param msg wraps the qos session attributes
     */
    public void handleEventQosSessionError(@NonNull final Message msg) {
        QosCallbackAgentConnection ac = getConnection(msg, "handleEventQosSessionError: ");
        if (ac != null) {
            ac.sendEventQosSessionError(msg.arg2, "");
            handleUnregisterCallback(ac.getBinder(), false);
        }
    }

    /**
     * Unregisters all callbacks associated to this network agent
     *
     * @param networkAgentInfo
     */
    public void handleNetworkReleased(@NonNull final NetworkAgentInfo networkAgentInfo) {
        Set<QosCallbackAgentConnection> connections = mCallbacks.remove(networkAgentInfo);
        if (connections != null) {
            for (QosCallbackAgentConnection agentConnection : connections) {
                agentConnection.sendEventQosSessionError(
                        QosCallbackException.EX_TYPE_FILTER_NETWORK_RELEASED, "");

                // Call unregister workflow w\o sending anything to agent since it is disconnected.
                handleUnregisterCallback(agentConnection.getBinder(), false);
            }
        }
    }

    private QosCallbackAgentConnection getConnection(@NonNull Message msg,
            @NonNull String logPrefix) {
        QosCallbackAgentConnection ac = mCallbacks.get(msg.arg1);
        if (ac == null) {
            loge(logPrefix + ": " + " missing callback id");
        }
        return ac;
    }

    private static void log(String msg) {
        Slog.d(TAG, msg);
    }

    private static void logw(String msg) {
        Slog.w(TAG, msg);
    }

    private static void loge(String msg) {
        Slog.e(TAG, msg);
    }

    private static void logwtf(String msg) {
        Slog.wtf(TAG, msg);
    }
}
