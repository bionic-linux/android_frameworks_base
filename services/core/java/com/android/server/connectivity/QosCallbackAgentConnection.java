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

import static android.net.NetworkAgent.QOS_SESSION_AVAILABLE_ATTRIBUTES_KEY;
import static android.net.NetworkAgent.QOS_SESSION_AVAILABLE_SESSION_KEY;
import static android.net.QosCallbackException.EX_TYPE_FILTER_NONE;

import android.annotation.NonNull;
import android.net.IQosCallback;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.QosFilter;
import android.net.QosSession;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.telephony.data.EpsBearerQosSessionAttributes;
import android.util.Slog;

import com.android.internal.util.AsyncChannel;

import java.util.Objects;

/**
 * Wraps callback related information and sends messages between network agent and the application.
 * <p/>
 * This is a satellite class of {@link com.android.server.ConnectivityService} and not meant
 * to be used in other contexts.
 *
 * @hide
 */
class QosCallbackAgentConnection implements IBinder.DeathRecipient {
    private static final String TAG = QosCallbackAgentConnection.class.getSimpleName();
    private static final boolean DBG = false;

    private final int mAgentCallbackId;
    @NonNull private final QosCallbackTracker mQosCallbackTracker;
    @NonNull private final IQosCallback mCallback;
    @NonNull private final IBinder mBinder;
    @NonNull private final QosFilter mFilter;
    @NonNull private final AsyncChannel mNetworkAgentAsyncChannel;

    private final int mUid;

    /**
     * Gets the uid
     * @return uid
     */
    int getUid() {
        return mUid;
    }

    /**
     * Gets the binder
     * @return binder
     */
    @NonNull
    IBinder getBinder() {
        return mBinder;
    }

    /**
     * Gets the callback id
     *
     * @return callback id
     */
    int getAgentCallbackId() {
        return mAgentCallbackId;
    }

    /**
     * Gets the network tied to the callback of this connection
     *
     * @return network
     */
    @NonNull
    Network getNetwork() {
        return mFilter.getNetwork();
    }

    QosCallbackAgentConnection(@NonNull final QosCallbackTracker qosCallbackTracker,
            final int agentCallbackId,
            @NonNull final IQosCallback callback,
            @NonNull final QosFilter filter,
            final int uid,
            @NonNull final AsyncChannel networkAgentAsyncChannel) {
        Objects.requireNonNull(qosCallbackTracker, "qosCallbackTracker must be non-null");
        Objects.requireNonNull(callback, "callback must be non-null");
        Objects.requireNonNull(filter, "filter must be non-null");
        Objects.requireNonNull(networkAgentAsyncChannel,
                "networkAgentAsyncChannel must be non-null");

        mQosCallbackTracker = qosCallbackTracker;
        mAgentCallbackId = agentCallbackId;
        mCallback = callback;
        mFilter = filter;
        mUid = uid;
        mBinder = mCallback.asBinder();
        mNetworkAgentAsyncChannel = networkAgentAsyncChannel;
    }

    @Override
    public void binderDied() {
        logw("binderDied: binder died with callback id: " + mAgentCallbackId);
        mQosCallbackTracker.unregisterCallback(mCallback);
    }

    void unlinkToDeathRecipient() {
        mBinder.unlinkToDeath(this, 0);
    }

    // Returns false if the NetworkAgent was never notified.
    boolean sendCmdRegisterCallback() {
        final int exceptionType = mFilter.validate();
        if (exceptionType != EX_TYPE_FILTER_NONE) {
            try {
                if (DBG) log("sendCmdRegisterCallback: filter validation failed");
                mCallback.onError(exceptionType);
            } catch (final RemoteException e) {
                loge("sendCmdRegisterCallback:", e);
            }
            return false;
        }

        final Message msg = Message.obtain();
        msg.what = NetworkAgent.CMD_REGISTER_QOS_CALLBACK;
        msg.arg1 = mAgentCallbackId;
        msg.obj = mFilter;

        try {
            mBinder.linkToDeath(this, 0);
        } catch (final RemoteException e) {
            loge("failed linking to death recipient", e);
            return false;
        }
        mNetworkAgentAsyncChannel.sendMessage(msg);
        return true;
    }

    void sendCmdUnregisterCallback() {
        final Message msg = Message.obtain();
        msg.what = NetworkAgent.CMD_UNREGISTER_QOS_CALLBACK;
        msg.arg1 = mAgentCallbackId;
        if (DBG) log("sendCmdUnregisterCallback: filter validation failed");
        mNetworkAgentAsyncChannel.sendMessage(msg);
    }

    void sendEventQosSessionAvailable(@NonNull final Bundle bundle) {
        final QosSession session = bundle.getParcelable(QOS_SESSION_AVAILABLE_SESSION_KEY);
        final Parcelable parcelableAttributes =
                bundle.getParcelable(QOS_SESSION_AVAILABLE_ATTRIBUTES_KEY);
        if (session == null || parcelableAttributes == null) {
            logwtf("sendEventQosSessionAvailable: missing session or attributes");
        } else if (parcelableAttributes instanceof EpsBearerQosSessionAttributes) {
            try {
                if (DBG) log("sendEventQosSessionAvailable: sending...");
                mCallback.onQosEpsBearerSessionAvailable(
                        session, (EpsBearerQosSessionAttributes) parcelableAttributes);
            } catch (final RemoteException e) {
                loge("sendEventQosSessionAvailable: remote exception", e);
            }
        } else {
            logwtf("sendEventQosSessionAvailable: do not recognize attributes "
                    + parcelableAttributes);
        }
    }

    void sendEventQosSessionLost(@NonNull final QosSession session) {
        try {
            if (DBG) log("sendEventQosSessionLost: sending...");
            mCallback.onQosSessionLost(session);
        } catch (final RemoteException e) {
            loge("sendEventQosSessionLost: remote exception", e);
        }
    }

    void sendEventQosSessionError(final int type) {
        try {
            if (DBG) log("sendEventQosSessionError: sending...");
            mCallback.onError(type);
        } catch (final RemoteException e) {
            loge("sendEventQosSessionError: remote exception", e);
        }
    }

    private static void log(@NonNull final String msg) {
        Slog.d(TAG, msg);
    }

    private static void logw(@NonNull final String msg) {
        Slog.w(TAG, msg);
    }

    private static void loge(@NonNull final String msg, final Throwable t) {
        Slog.e(TAG, msg, t);
    }

    private static void logwtf(@NonNull final String msg) {
        Slog.wtf(TAG, msg);
    }
}
