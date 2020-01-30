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

package com.android.server.connectivity.tethering;

import static android.net.TetheringManager.TETHERING_WIFI;

import android.net.MacAddress;
import android.net.TetheredClient;
import android.net.TetheredClient.AddressInfo;
import android.net.ip.IpServer;
import android.net.wifi.WifiClient;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracker for clients connected to downstreams.
 *
 * <p>This class is not thread safe, it is intended to be used only from the tethering handler
 * thread.
 */
public class ConnectedClientsTracker {
    private final Clock mClock;
    /**
     * List of client mac addresses for which there is a valid IP address, but the client has
     * disconnected at layer 2. The client may come back later and start using the address again,
     * but it is currently disconnected, so will not show up in the list of connected clients.
     *
     * For some clients, Tethering has no information on whether L2 is connected / disconnected;
     * for example a client could be behind a bridge, and some link types do not provide
     * notifications on connect/disconnect even for directly connected clients. In that case the
     * lease is never considered disconnected.
     */
    @NonNull
    private final Set<MacAddress> mDisconnectedClientsWithLease = new HashSet<>();

    @NonNull
    private List<WifiClient> mLastWifiClients = Collections.emptyList();
    @NonNull
    private List<TetheredClient> mLastTetheredClients = Collections.emptyList();

    @VisibleForTesting
    static class Clock {
        public long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }
    }

    public ConnectedClientsTracker() {
        this(new Clock());
    }

    @VisibleForTesting
    ConnectedClientsTracker(Clock clock) {
        mClock = clock;
    }

    /**
     * Update the tracker with new connected clients.
     * @param ipServers The IpServers used to assign addresses to clients.
     * @param wifiClients The list of L2-connected WiFi clients. Null for no change since last
     *                    update.
     * @return The new list of tethered clients.
     */
    public List<TetheredClient> updateConnectedClients(
            Iterable<IpServer> ipServers, @Nullable List<WifiClient> wifiClients) {
        final long now = mClock.elapsedRealtime();

        // Build the list of non-expired leases from all IpServers
        final List<TetheredClient> validLeases = new ArrayList<>();
        for (IpServer server : ipServers) {
            for (TetheredClient client : server.getAllLeases()) {
                final TetheredClient remainingAddrs = getRemainingAddresses(client, now);
                if (remainingAddrs == null) continue;
                validLeases.add(remainingAddrs);
            }
        }

        final Set<MacAddress> currentWifiClients;
        final Set<MacAddress> lostWifiClients;
        if (wifiClients != null) {
            currentWifiClients = getClientMacs(wifiClients);
            lostWifiClients = getClientMacs(mLastWifiClients);
            lostWifiClients.removeAll(currentWifiClients);

            mLastWifiClients = wifiClients;
        } else {
            currentWifiClients = getClientMacs(mLastWifiClients);
            lostWifiClients = Collections.emptySet();
        }

        final Set<MacAddress> leasesMacs = getLeasesMacs(validLeases);
        // A client is no longer "disconnected with a lease" if it's connected, or has no lease
        mDisconnectedClientsWithLease.removeIf(c ->
                currentWifiClients.contains(c) || (!leasesMacs.contains(c)));


        // Build the list of tethered clients to report, grouped by mac address
        final Map<MacAddress, TetheredClient> clientsMap = new HashMap<>();
        for (TetheredClient leaseInfo : validLeases) {
            if (mDisconnectedClientsWithLease.contains(leaseInfo.getMacAddress())) {
                // Skip this lease as it belongs to a disconnected client
                continue;
            } else if (lostWifiClients.contains(leaseInfo.getMacAddress())) {
                // The owner of this lease just disconnected
                mDisconnectedClientsWithLease.add(leaseInfo.getMacAddress());
                continue;
            }

            final TetheredClient aggregateClient = clientsMap.getOrDefault(
                    leaseInfo.getMacAddress(), leaseInfo);
            if (aggregateClient != leaseInfo) {
                // Only add the address info; this assumes that the tethering type is the same when
                // the mac address is the same. If a client is connected through different tethering
                // types with the same mac address, connected clients callbacks will report all of
                // its addresses under only one of these tethering types. This keeps the API simple
                // considering that such a scenario would really be a rare edge case.
                clientsMap.put(leaseInfo.getMacAddress(), aggregateClient.addAddresses(leaseInfo));
            }
            clientsMap.put(leaseInfo.getMacAddress(), leaseInfo);
        }

        // TODO: add IPv6 addresses from netlink

        // Add connected WiFi clients that do not have any known address
        for (MacAddress client : currentWifiClients) {
            if (clientsMap.containsKey(client)) continue;
            clientsMap.put(client, new TetheredClient(
                    client, Collections.emptyList() /* addresses */, TETHERING_WIFI));
        }

        mLastTetheredClients = Collections.unmodifiableList(new ArrayList<>(clientsMap.values()));
        return mLastTetheredClients;
    }

    /**
     * Get the last list of tethered clients, as calculated in {@link #updateConnectedClients}.
     *
     * <p>The returned list is immutable.
     */
    @NonNull
    public List<TetheredClient> getLastTetheredClients() {
        return mLastTetheredClients;
    }

    private static boolean hasExpiredAddress(List<AddressInfo> addresses, long now) {
        for (AddressInfo info : addresses) {
            if (info.getExpirationTime() <= now) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static TetheredClient getRemainingAddresses(TetheredClient client, long now) {
        final List<AddressInfo> addresses = client.getAddresses();
        if (addresses.size() == 0) return null;
        if (!hasExpiredAddress(addresses, now)) return client;

        final ArrayList<AddressInfo> newAddrs = new ArrayList<>(addresses.size() - 1);
        for (AddressInfo info : addresses) {
            if (info.getExpirationTime() > now) {
                newAddrs.add(info);
            }
        }

        if (newAddrs.size() == 0) {
            return null;
        }
        return new TetheredClient(client.getMacAddress(), newAddrs, client.getTetheringType());
    }

    private static Set<MacAddress> getClientMacs(@NonNull List<WifiClient> clients) {
        final Set<MacAddress> macs = new HashSet<>(clients.size());
        for (WifiClient c : clients) {
            macs.add(c.getMacAddress());
        }
        return macs;
    }

    private static Set<MacAddress> getLeasesMacs(@NonNull List<TetheredClient> leases) {
        final Set<MacAddress> macs = new HashSet<>(leases.size());
        for (TetheredClient c : leases) {
            macs.add(c.getMacAddress());
        }
        return macs;
    }

}
