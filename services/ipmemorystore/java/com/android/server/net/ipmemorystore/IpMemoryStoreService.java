/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.net.ipmemorystore;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.IIpMemoryStore;
import android.net.ipmemorystore.IOnL2KeyResponseListener;
import android.net.ipmemorystore.IOnNetworkAttributesRetrieved;
import android.net.ipmemorystore.IOnPrivateDataRetrievedListener;
import android.net.ipmemorystore.IOnSameNetworkResponseListener;
import android.net.ipmemorystore.IOnStatusListener;
import android.net.ipmemorystore.NetworkAttributes;
import android.net.ipmemorystore.PrivateData;

/**
 * Implementation for the IP memory store.
 * This component offers specialized services for network components to store and retrieve
 * knowledge about networks, and provides intelligence that groups level 2 networks together
 * into level 3 networks.
 * @hide
 */
public class IpMemoryStoreService extends IIpMemoryStore.Stub {
    final Context mContext;

    public IpMemoryStoreService(@NonNull final Context context) {
        mContext = context;
    }

    /**
     * Store network attributes for a given L2 key.
     * If L2Key is null, choose automatically from the attributes ; passing null is equivalent to
     * calling findL2Key with the attributes and storing in the returned value.
     *
     * @param l2Key The L2 key for the L2 network. If this is null, the memory store will
     *              try and find a matching network from the attributes. If it finds one it
     *              will use it, otherwise it will create a new row and associate an
     *              automatically generated L2 key.
     * @param attributes The attributes for this network.
     * @param listener A listener to inform of the completion of this call, or null.
     * @return (through the listener) The L2 key. This is useful if the L2 key was not specified.
     */
    @Override
    public void storeNetworkAttributes(@Nullable final String l2Key,
            @NonNull final NetworkAttributes attributes,
            @NonNull final IOnL2KeyResponseListener listener) {
        // TODO : implement this
    }

    /**
     * Store a binary blob associated with an L2 key and a name.
     *
     * @param l2Key The L2 key for this network.
     * @param clientId The ID of the client.
     * @param name The name of this data.
     * @param data The data to store.
     */
    @Override
    public void storePrivateData(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final PrivateData data,
            @NonNull final IOnStatusListener listener) {
        // TODO : implement this
    }

    /**
     * Returns the best L2 key associated with the attributes.
     *
     * This will find a record that would be in the same group as the passed attributes. This is
     * useful to choose the key for storing a sample or private data when the L2 key is not known.
     * If multiple records are group-close to these attributes, the closest match is returned.
     * If multiple records have the same closeness, the one with the smaller (unicode codepoint
     * order) L2 key is returned.
     * If no record matches these attributes, a new L2 key is automatically generated.
     *
     * @param attributes The attributes of the network to find.
     * @param listener The listener to invoke to return the answer.
     */
    @Override
    public void findL2Key(@NonNull final NetworkAttributes attributes,
            @NonNull final IOnL2KeyResponseListener listener) {
        // TODO : implement this
    }

    /**
     * Returns whether, to the best of the store's ability to tell, the two specified L2 keys point
     * to the same L3 network.
     *
     * @param l2Key1 The key for the first network.
     * @param l2Key2 The key for the second network.
     * @param listener The listener to invoke to give the answer.
     * @return (through the listener) A SameL3NetworkResponse containing the answer and confidence.
     */
    @Override
    public void isSameNetwork(@NonNull final String l2Key1, @NonNull final String l2Key2,
            @NonNull final IOnSameNetworkResponseListener listener) {
        // TODO : implement this
    }

    /**
     * Retrieve the network attributes for a key.
     * If no record is present for this key, this will return null attributes.
     *
     * @param l2Key The key of the network to query.
     * @param listener The listener to invoke to give the answer.
     * @return (through the listener) The network attributes and the L2 key associated with
     *         the query.
     */
    @Override
    public void retrieveNetworkAttributes(@NonNull final String l2Key,
            @NonNull final IOnNetworkAttributesRetrieved listener) {
        // TODO : implement this.
    }

    /**
     * Retrieve previously stored private data.
     * If no data was stored for this L2 key and name this will return null.
     *
     * @param l2Key The L2 key.
     * @param clientId The id of the client that stored this data.
     * @param name The name of the data.
     * @param listener The listener to invoke to give the answer.
     * @return (through the listener) The private data if any or null if none, with the L2 key
     *         and the name of the data associated with the query.
     */
    @Override
    public void retrievePrivateData(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final IOnPrivateDataRetrievedListener listener) {
        // TODO : implement this.
    }
}
