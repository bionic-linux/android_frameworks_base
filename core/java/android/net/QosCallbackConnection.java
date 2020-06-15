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

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.telephony.data.EpsBearerQosSessionAttributes;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Sends messages from {@link com.android.server.ConnectivityService} to the registered
 * {@link QosCallback}.
 *
 * @hide
 */
public class QosCallbackConnection extends android.net.IQosCallback.Stub {

    @NonNull private final ConnectivityManager mConnectivityManager;
    @NonNull private final QosCallback mCallback;
    @NonNull private final Executor mExecutor;
    private boolean mStopReceivingMessages = false;

    @VisibleForTesting
    @Nullable
    public QosCallback getCallback() {
        return mCallback;
    }

    /**
     * The constructor for the connection
     *
     * @param connectivityManager the mgr that created this connection
     * @param callback the callback to send messages back to
     */
    public QosCallbackConnection(@NonNull final ConnectivityManager connectivityManager,
            @NonNull final QosCallback callback,
            @NonNull final Executor executor) {
        mConnectivityManager = Objects.requireNonNull(connectivityManager,
                "connectivityManager must be non-null");
        mCallback = Objects.requireNonNull(callback, "callback must be non-null");
        mExecutor = Objects.requireNonNull(executor, "executor must be non-null");
    }

    /**
     * Called when either the {@link EpsBearerQosSessionAttributes} has changed or on the first time
     * the attributes have become available.
     *
     * @param session the session that is now available
     * @param attributes the corresponding attributes of session
     */
    @Override
    public void onQosEpsBearerSessionAvailable(@NonNull final QosSession session,
            @NonNull final EpsBearerQosSessionAttributes attributes) {

        mExecutor.execute(() -> {
            if (canReceiveMessages()) {
                mCallback.onQosSessionAvailable(session, attributes);
            }
        });
    }

    /**
     * Called when the session is lost.
     *
     * @param session the session that was lost
     */
    @Override
    public void onQosSessionLost(@NonNull final QosSession session) {
        mExecutor.execute(() -> {
            if (canReceiveMessages()) {
                mCallback.onQosSessionLost(session);
            }
        });
    }

    /**
     * Called when there is an error on the registered callback.
     *
     *  @param errorType the type of error
     */
    @Override
    public void onError(@QosCallbackException.ExceptionType final int errorType) {
        mExecutor.execute(() -> {
            if (canReceiveMessages()) {
                // Messages no longer need to be received since there was an error.
                stopReceivingMessages();
                mConnectivityManager.unregisterQosCallback(mCallback);
                mCallback.onError(QosCallbackException.createException(errorType));
            }
        });
    }

    /**
     * Whether this connection can still receive messages.
     * <p/>
     * Note: If the executor is using a thread pool, messages may still end up being relayed to the
     * callback after the time in which {@link #stopReceivingMessages()} was called.
     */
    public boolean canReceiveMessages() {
        return !mStopReceivingMessages;
    }

    /**
     * The callback no longer receives messages.
     * <p/>
     * Note: If the executor is using a thread pool, messages may still end up being relayed to the
     * callback after the time in which this is called.
     */
    public void stopReceivingMessages() {
        mStopReceivingMessages = true;
    }
}
