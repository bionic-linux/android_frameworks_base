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

/**
* service used to communicate with the ip memory store service in network stack,
* which is running in a separate module.
* @hide
*/
public class IpMemoryStore extends IpMemoryStoreClient {
    public IpMemoryStore(Context context) {
        super(context);
    }

    protected void fetchIpMemoryStore(final IIpMemoryStoreCallbacks cb) {
        NetworkStackClient.getInstance().fetchIpMemoryStore(cb);
    }

    @Override
    public void storeNetworkAttributes(@NonNull final String l2Key,
            @NonNull final NetworkAttributes attributes,
            @Nullable final IOnStatusListener listener) {
        try {
            super.storeNetworkAttributes(l2Key, attributes, listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void storeBlob(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final Blob data,
            @Nullable final IOnStatusListener listener) {
        try {
            super.storeBlob(l2Key, clientId, name, data, listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void findL2Key(@NonNull final NetworkAttributes attributes,
            @NonNull final IOnL2KeyResponseListener listener) {
        try {
            super.findL2Key(attributes, listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void isSameNetwork(@NonNull final String l2Key1, @NonNull final String l2Key2,
            @NonNull final IOnSameNetworkResponseListener listener) {
        try {
            super.isSameNetwork(l2Key1, l2Key2, listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void retrieveNetworkAttributes(@NonNull final String l2Key,
            @NonNull final IOnNetworkAttributesRetrieved listener) {
        try {
            super.retrieveNetworkAttributes(l2Key, listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void retrieveBlob(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final IOnBlobRetrievedListener listener) {
        try {
            super.retrieveBlob(l2Key, clientId, name, listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
