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
import android.annotation.Nullable;
import android.net.IQosCallback;
import android.net.Network;
import android.net.QosCallbackException;
import android.net.QosFilter;
import android.net.QosSession;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Slog;

import com.android.internal.util.AsyncChannel;
import com.android.internal.util.CollectionUtils;
import com.android.server.ConnectivityService;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks qos callbacks and handles the communication between the network agent and application.
 * <p/>
 * Any method prefixed by handle must be called from the
 * {@link com.android.server.ConnectivityService} handler thread.
 *
 * @hide
 */
public class QosCallbackTracker {
    private static final String TAG = QosCallbackTracker.class.getSimpleName();
    private static final boolean DBG = true;

    @NonNull
    private final Handler mConnectivityServiceHandler;

    @NonNull
    private final ConnectivityService.PerUidCounter mNetworkRequestCounter;

    /**
     * Each agent gets a unique callback id that is used to proxy messages back to the original
     * callback.
     * <p/>
     * Note: The fact that this is initialized to 0 is to ensure that the thread running
     * {@link #handleRegisterCallback(IQosCallback, QosFilter, int, AsyncChannel)} sees the
     * initialized value. This would not necessarily be the case if the value was initialized to
     * the non-default value.
     * <p/>
     * Note: The term previous does not apply to the first callback id that is assigned.
     */
    private int mPreviousAgentCallbackId = 0;

    @NonNull
    private final List<QosCallbackAgentConnection> mConnections = new ArrayList<>();

    /**
     *
     * @param connectivityServiceHandler must be the same handler used with
     *                {@link com.android.server.ConnectivityService}
     * @param networkRequestCounter keeps track of the number of open requests under a given
     *                              uid
     */
    public QosCallbackTracker(@NonNull final Handler connectivityServiceHandler,
            final ConnectivityService.PerUidCounter networkRequestCounter) {
        mConnectivityServiceHandler = connectivityServiceHandler;
        mNetworkRequestCounter = networkRequestCounter;
    }

    /**
     * Registers the callback with the tracker
     *
     * @param callback the callback to register
     * @param filter the filter being registered alongside the callback
     */
    public void registerCallback(@NonNull final IQosCallback callback,
            @NonNull final QosFilter filter, @NonNull final AsyncChannel networkAgentAsyncChannel) {
        final int uid = Binder.getCallingUid();

        // Enforce that the number of requests under this uid has exceeded the allowed number
        mNetworkRequestCounter.incrementCountOrThrow(uid);

        mConnectivityServiceHandler.post(
                () -> handleRegisterCallback(callback, filter, uid, networkAgentAsyncChannel));
    }

    private void handleRegisterCallback(@NonNull final IQosCallback callback,
            @NonNull final QosFilter filter, final int uid,
            @NonNull final AsyncChannel networkAgentAsyncChannel) {
        final QosCallbackAgentConnection ac =
                handleRegisterCallbackInternal(callback, filter, uid, networkAgentAsyncChannel);
        if (ac != null) {
            if (DBG) log("handleRegisterCallback: added callback " + ac.getAgentCallbackId());
            mConnections.add(ac);
        } else {
            mNetworkRequestCounter.decrementCount(uid);
        }
    }

    private QosCallbackAgentConnection handleRegisterCallbackInternal(
            @NonNull final IQosCallback callback,
            @NonNull final QosFilter filter, final int uid,
            @NonNull final AsyncChannel networkAgentAsyncChannel) {
        final IBinder binder = callback.asBinder();
        if (CollectionUtils.any(mConnections, c -> c.getBinder().equals(binder))) {
            // A duplicate registration would have only made this far due to a programming error.
            logwtf("handleRegisterCallback: Callbacks can only be register once.");
            return null;
        }

        mPreviousAgentCallbackId = mPreviousAgentCallbackId + 1;
        final int newCallbackId = mPreviousAgentCallbackId;

        final QosCallbackAgentConnection ac =
                new QosCallbackAgentConnection(this, newCallbackId, callback,
                        filter, uid, networkAgentAsyncChannel);

        final int exceptionType = filter.validate();
        if (exceptionType != QosCallbackException.EX_TYPE_FILTER_NONE) {
            ac.sendEventQosSessionError(exceptionType);
            return null;
        }

        // Only add to the callback maps if the NetworkAgent successfully registered it
        if (!ac.sendCmdRegisterCallback()) {
            // There was an issue when registering the agent
            if (DBG) log("handleRegisterCallback: error sending register callback");
            mNetworkRequestCounter.decrementCount(uid);
            return null;
        }
        return ac;
    }

    /**
     * Unregisters callback
     * @param callback callback to unregister
     */
    public void unregisterCallback(@NonNull final IQosCallback callback) {
        mConnectivityServiceHandler.post(() -> handleUnregisterCallback(callback.asBinder(), true));
    }

    private void handleUnregisterCallback(@NonNull final IBinder binder,
            final boolean sendToNetworkAgent) {
        final QosCallbackAgentConnection agentConnection =
                CollectionUtils.find(mConnections, c -> c.getBinder().equals(binder));
        if (agentConnection == null) {
            logw("handleUnregisterCallback: agentConnection is null");
            return;
        }

        if (DBG) {
            log("handleUnregisterCallback: unregister "
                    + agentConnection.getAgentCallbackId());
        }

        mNetworkRequestCounter.decrementCount(agentConnection.getUid());
        mConnections.remove(agentConnection);

        if (sendToNetworkAgent) {
            agentConnection.sendCmdUnregisterCallback();
        }
        agentConnection.unlinkToDeathRecipient();
    }

    /**
     * Called when the NetworkAgent sends the qos session available event
     *
     * Note: Must be called on the connectivity service handler thread
     *
     * @param msg wraps the qos session attributes
     */
    public void handleEventQosSessionAvailable(@NonNull final Message msg) {
        final QosCallbackAgentConnection ac =
                getConnection(msg, "handleEventQosSessionAvailable: ");
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
        final QosCallbackAgentConnection ac =
                getConnection(msg, "handleEventQosSessionLost: ");
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
        final QosCallbackAgentConnection ac =
                getConnection(msg, "handleEventQosSessionError: ");
        if (ac != null) {
            ac.sendEventQosSessionError(msg.arg2);
            handleUnregisterCallback(ac.getBinder(), false);
        }
    }

    /**
     * Unregisters all callbacks associated to this network agent
     *
     * @param network the network that was released
     */
    public void handleNetworkReleased(@Nullable final Network network) {
        final List<QosCallbackAgentConnection> connections =
                CollectionUtils.filter(mConnections, ac -> ac.getNetwork().equals(network));

        for (final QosCallbackAgentConnection agentConnection : connections) {
            agentConnection.sendEventQosSessionError(
                    QosCallbackException.EX_TYPE_FILTER_NETWORK_RELEASED);

            // Call unregister workflow w\o sending anything to agent since it is disconnected.
            handleUnregisterCallback(agentConnection.getBinder(), false);
        }
    }

    @Nullable
    private QosCallbackAgentConnection getConnection(@NonNull final Message msg,
            @NonNull final String logPrefix) {
        final QosCallbackAgentConnection ac =
                CollectionUtils.find(mConnections, c -> c.getAgentCallbackId() == msg.arg1);
        if (ac == null) {
            loge(logPrefix + ": " + msg.arg1 + " missing callback id");
        }
        return ac;
    }

    private static void log(final String msg) {
        Slog.d(TAG, msg);
    }

    private static void logw(final String msg) {
        Slog.w(TAG, msg);
    }

    private static void loge(final String msg) {
        Slog.e(TAG, msg);
    }

    private static void logwtf(final String msg) {
        Slog.wtf(TAG, msg);
    }
}
