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

import android.annotation.NonNull;
import android.net.IQosCallback;
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

import java.util.Optional;

/**
 * Wraps callback related information and sends messages between network agent and the application.
 *
 * @hide
 */
class QosCallbackAgentConnection implements IBinder.DeathRecipient {
    static final String TAG = QosCallbackTracker.TAG;
    static final boolean DBG = QosCallbackTracker.DBG;

    private final int mAgentCallbackId;
    @NonNull private final QosCallbackTracker mQosCallbackTracker;
    @NonNull private final NetworkAgentInfo mNetworkAgentInfo;
    @NonNull private final IQosCallback mCallback;
    @NonNull private final IBinder mBinder;
    @NonNull private final QosFilter mFilter;

    private boolean mBinderDied;
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
    IBinder getBinder() {
        return mBinder;
    }

    /**
     * Gets the callback id
     * @return callback id
     */
    Integer getAgentCallbackId() {
        return mAgentCallbackId;
    }

    /**
     * Gets the network agent info
     * @return network agent info
     */
    NetworkAgentInfo getNetworkAgentInfo() {
        return mNetworkAgentInfo;
    }

    QosCallbackAgentConnection(@NonNull final QosCallbackTracker qosCallbackTracker,
            final int agentCallbackId,
            @NonNull final NetworkAgentInfo networkAgentInfo,
            @NonNull final IQosCallback callback,
            @NonNull final QosFilter filter,
            final int uid) {
        mQosCallbackTracker = qosCallbackTracker;
        mAgentCallbackId = agentCallbackId;
        mNetworkAgentInfo = networkAgentInfo;
        mCallback = callback;
        mFilter = filter;
        mUid = uid;
        mBinder = mCallback.asBinder();
        mBinderDied = false;
    }

    @Override
    public void binderDied() {
        if (!mBinderDied) {
            logw("binderDied: binder died with callback id: " + mAgentCallbackId);
            // The IQosCallback binder died, we need to unregister with the network agent.
            mQosCallbackTracker.unregisterCallback(mCallback);
            mBinderDied = true;
        }
    }

    private boolean isBinderAlive() {
        if (mBinderDied) {
            return false;
        } else if (!mBinder.isBinderAlive()) {
            binderDied();
            return false;
        } else {
            return true;
        }
    }

    void unlinkToDeathRecipient() {
        mBinder.unlinkToDeath(this, 0);
    }


    // Returns false if the NetworkAgent was never notified.
    boolean sendCmdRegisterCallback() {
        final Optional<Integer> exceptionType = mFilter.validate(
                mNetworkAgentInfo.networkCapabilities);
        if (!exceptionType.isPresent()) {
            final Message msg = new Message();
            msg.what = NetworkAgent.CMD_REGISTER_QOS_CALLBACK;
            msg.arg1 = mAgentCallbackId;

            // Call the utility method on the network agent that is aware of how to write each
            // filter type
            NetworkAgent.writeQosFilter(msg, mFilter);

            try {
                mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                loge("failed linking to death recipient", e);
                return false;
            }
            mNetworkAgentInfo.asyncChannel.sendMessage(msg);
            return true;
        } else {
            try {
                if (mBinder.isBinderAlive()) {
                    if (DBG) log("sendCmdRegisterCallback: filter validation failed");
                    this.mCallback.onError(exceptionType.get(), "");
                }
            } catch (RemoteException e) {
                loge("sendCmdRegisterCallback:", e);
            }
            return false;
        }
    }

    void sendCmdUnregisterCallback() {
        final Message msg = new Message();
        msg.what = NetworkAgent.CMD_UNREGISTER_QOS_CALLBACK;
        msg.arg1 = mAgentCallbackId;
        if (DBG) log("sendCmdUnregisterCallback: filter validation failed");
        mNetworkAgentInfo.asyncChannel.sendMessage(msg);
    }

    void sendEventQosSessionAvailable(@NonNull Bundle bundle) {
        final QosSession session = bundle.getParcelable(QOS_SESSION_AVAILABLE_SESSION_KEY);
        final Parcelable parcelableAttributes =
                (Parcelable) bundle.getParcelable(QOS_SESSION_AVAILABLE_ATTRIBUTES_KEY);
        if (session == null || parcelableAttributes == null) {
            logwtf("sendEventQosSessionAvailable: missing session or attributes");
        } else if (parcelableAttributes instanceof EpsBearerQosSessionAttributes) {
            try {
                if (isBinderAlive()) {
                    if (DBG) log("sendEventQosSessionAvailable: sending...");
                    mCallback.onQosEpsBearerSessionAvailable(
                            session, (EpsBearerQosSessionAttributes) parcelableAttributes);
                }
            } catch (RemoteException e) {
                loge("sendEventQosSessionAvailable: remote exception", e);
            }
        } else {
            logwtf("sendEventQosSessionAvailable: do not recognize attributes "
                    + parcelableAttributes);
        }
    }

    void sendEventQosSessionLost(@NonNull QosSession session) {
        try {
            if (isBinderAlive()) {
                if (DBG) log("sendEventQosSessionLost: sending...");
                mCallback.onQosSessionLost(session);
            }
        } catch (RemoteException e) {
            loge("sendEventQosSessionLost: remote exception", e);
        }
    }

    void sendEventQosSessionError(int type) {
        try {
            if (isBinderAlive()) {
                if (DBG) log("sendEventQosSessionError: sending...");
                mCallback.onError(type, "");
            }
        } catch (RemoteException e) {
            loge("sendEventQosSessionError: remote exception", e);
        }
    }

    private static void log(String msg) {
        Slog.d(TAG, msg);
    }

    private static void logw(String msg) {
        Slog.w(TAG, msg);
    }

    private static void loge(String msg, Throwable t) {
        Slog.e(TAG, msg, t);
    }

    private static void logwtf(String msg) {
        Slog.wtf(TAG, msg);
    }
}
