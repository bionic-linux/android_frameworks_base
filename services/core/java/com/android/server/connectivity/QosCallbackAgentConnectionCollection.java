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
import android.os.IBinder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of {@link QosCallbackAgentConnection}s.
 *
 * Not thread-safe.
 *
 * @hide
 */
class QosCallbackAgentConnectionCollection {

    @NonNull
    private final Map<Integer, QosCallbackAgentConnection>
            mCallbackIdsToConnections =
            new HashMap<>();
    @NonNull private final Map<IBinder, QosCallbackAgentConnection>
            mCallbacksToConnections = new HashMap<>();
    @NonNull private final Map<NetworkAgentInfo,
            Set<QosCallbackAgentConnection>>
            mNetworkAgentsToConnections = new HashMap<>();

    QosCallbackAgentConnectionCollection() {
    }

    void put(@NonNull final QosCallbackAgentConnection connection) {
        mCallbackIdsToConnections.put(connection.getAgentCallbackId(), connection);
        mCallbacksToConnections.put(connection.getBinder(), connection);
        final Set<QosCallbackAgentConnection> subSet =
                mNetworkAgentsToConnections.computeIfAbsent(connection.getNetworkAgentInfo(), (k) ->
                        new HashSet<>()
                );
        subSet.add(connection);
    }

    QosCallbackAgentConnection get(final int callbackId) {
        return mCallbackIdsToConnections.get(callbackId);
    }

    QosCallbackAgentConnection get(@NonNull final IBinder binder) {
        return mCallbacksToConnections.get(binder);
    }

    @NonNull
    QosCallbackAgentConnection[] get(@NonNull final NetworkAgentInfo networkAgentInfo) {
        final Set<QosCallbackAgentConnection> connections =
                mNetworkAgentsToConnections.get(networkAgentInfo);
        if (connections != null) {
            return connections.toArray(new QosCallbackAgentConnection[0]);
        } else {
            return new QosCallbackAgentConnection[0];
        }
    }

    void remove(@NonNull final QosCallbackAgentConnection connection) {
        mCallbacksToConnections.remove(connection.getBinder());
        mCallbackIdsToConnections.remove(connection.getAgentCallbackId());
        final Set<QosCallbackAgentConnection> subSet =
                mNetworkAgentsToConnections.get(connection.getNetworkAgentInfo());
        if (subSet != null) {
            subSet.remove(connection);
            if (subSet.size() == 0) {
                mNetworkAgentsToConnections.remove(connection.getNetworkAgentInfo());
            }
        }
    }
}
