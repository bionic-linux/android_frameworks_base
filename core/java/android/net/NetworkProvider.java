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
import android.annotation.SystemApi;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * Class to listen for NetworkRequests.
 * @hide
 */
@SystemApi
public class NetworkProvider {
    /** @hide used by ConnectivityService */
    public static final int CMD_REQUEST_NETWORK = 1;
    /** @hide used by ConnectivityService */
    public static final int CMD_CANCEL_REQUEST = 2;

    private final Messenger mMessenger;
    private final String mName;
    private int mSerialNumber;

    /**
     * Constructs a new NetworkRequestListener.
     *
     * @param looper the Looper on which to run {@link #onNetworkRequested} and
     *               {@link #onRequestWithdrawn}.
     * @param name the name of the listener, used only for debugging.
     *
     * @hide
     */
    @SystemApi
    public NetworkProvider(@NonNull Looper looper, @NonNull String name) {
        Handler handler = new Handler(looper) {
            @Override
            public void handleMessage(Message m) {
                switch (m.what) {
                    case CMD_REQUEST_NETWORK:
                        onNetworkRequested((NetworkRequest) m.obj, m.arg1, m.arg2);
                        break;
                    case CMD_CANCEL_REQUEST:
                        onRequestWithdrawn((NetworkRequest) m.obj);
                        break;
                    default:
                        Log.e(mName, "Unhandled message: " + m.what);
                }
            }
        };
        mMessenger = new Messenger(handler);
        mName = name;
    }

    public @Nullable Messenger getMessenger() {
        return mMessenger;
    }

    public @NonNull String getName() {
        return mName;
    }

    /** @hide */
    public int getSerialNumber() {
        return mSerialNumber;
    }

    /** @hide */
    public void setSerialNumber(int serialNumber) {
        mSerialNumber = serialNumber;
    }

    /**
     *  Called when a NetworkRequest is received. The request may be a new request or an existing
     *  request with a different score.
     *  @hide
     */
    @SystemApi
    public void onNetworkRequested(@NonNull NetworkRequest request, int score, int serialNumber) {}

    /**
     *  Called when a NetworkRequest is withdrawn.
     *  @hide
     */
    @SystemApi
    public void onRequestWithdrawn(@NonNull NetworkRequest request) {}
}
