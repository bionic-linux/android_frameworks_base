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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.IOnBlobRetrievedListener;
import android.net.ipmemorystore.IOnL2KeyResponseListener;
import android.net.ipmemorystore.IOnNetworkAttributesRetrieved;
import android.net.ipmemorystore.IOnSameNetworkResponseListener;
import android.net.ipmemorystore.IOnStatusListener;
import android.net.ipmemorystore.NetworkAttributes;
import android.os.RemoteException;
import android.util.Log;

import com.android.server.NetworkStackService;

/**
 * service used to communicate with the ip memory store service in network stack,
 * which is running in the same module.
 * @see com.android.server.connectivity.ipmemorystore.IpMemoryStoreService
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
            if (NetworkStackService.getIpMemoryStore() == null) {
                Log.wtf(TAG, "Error getting IpMemoryStore Service instance");
                return;
            }
            cb.onIpMemoryStoreFetched(NetworkStackService.getIpMemoryStore());
        } catch (RemoteException e) {
            // Clients should be in process: this should never happen
            Log.wtf(TAG, "Error sending IpMemoryStore to client", e);
        }
    }

    @Override
    public void storeNetworkAttributes(@NonNull final String l2Key,
            @NonNull final NetworkAttributes attributes,
            @Nullable final IOnStatusListener listener) {
        try {
            super.storeNetworkAttributes(l2Key, attributes, listener);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error storing network attributes", e);
        }
    }

    @Override
    public void storeBlob(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final Blob data,
            @Nullable final IOnStatusListener listener) {
        try {
            super.storeBlob(l2Key, clientId, name, data, listener);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error storing blob", e);
        }
    }

    @Override
    public void findL2Key(@NonNull final NetworkAttributes attributes,
            @NonNull final IOnL2KeyResponseListener listener) {
        try {
            super.findL2Key(attributes, listener);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error finding l2 key", e);
        }
    }

    @Override
    public void isSameNetwork(@NonNull final String l2Key1, @NonNull final String l2Key2,
            @NonNull final IOnSameNetworkResponseListener listener) {
        try {
            super.isSameNetwork(l2Key1, l2Key2, listener);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error checking if it's the same network", e);
        }
    }

    @Override
    public void retrieveNetworkAttributes(@NonNull final String l2Key,
            @NonNull final IOnNetworkAttributesRetrieved listener) {
        try {
            super.retrieveNetworkAttributes(l2Key, listener);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error retrieving network attributes", e);
        }
    }

    @Override
    public void retrieveBlob(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final IOnBlobRetrievedListener listener) {
        try {
            super.retrieveBlob(l2Key, clientId, name, listener);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error retrieving blob", e);
        }
    }
}
