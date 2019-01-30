/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.android.server.NetworkStackService;

/**
 * service used to communicate with the ip memory store service in network stack,
 * which is running in the same module.
 * @see ipmemorystoreservice
 * @hide
 */
public class NetworkStackIpMemoryStore extends IpMemoryStoreClient {
    private static final String TAG = NetworkStackIpMemoryStore.class.getSimpleName();

    public NetworkStackIpMemoryStore(Context context) {
        super(context);
    }

    @Override
    protected void fetchIpMemoryStore(IIpMemoryStoreCallbacks cb) {
        try {
            cb.onIpMemoryStoreFetched(NetworkStackService.getIpMemoryStore());
        } catch (RemoteException e) {
            // Clients should be in process: this should never happen
            Log.wtf(TAG, "Error sending IpMemoryStore to client", e);
        }
    }
}
