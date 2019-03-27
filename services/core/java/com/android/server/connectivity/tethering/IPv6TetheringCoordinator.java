/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.net.ConnectivityManager;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkState;
import android.net.RouteInfo;
import android.net.ip.IpServer;
import android.net.util.SharedLog;
import android.util.Log;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;


/**
 * IPv6 tethering is rather different from IPv4 owing to the absence of NAT.
 * This coordinator is responsible for evaluating the dedicated prefixes
 * assigned to the device and deciding how to divvy them up among downstream
 * interfaces.
 *
 * @hide
 */
public class IPv6TetheringCoordinator {
    private static final String TAG = IPv6TetheringCoordinator.class.getSimpleName();
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    private final SharedLog mLog;
    // NOTE: mActiveDownstreams is a list and not a hash data structure because
    // we keep active downstreams in arrival order.  This is done so /64s can
    // be parceled out on a "first come, first served" basis and a /64 used by
    // a downstream that is no longer active can be redistributed to any next
    // waiting active downstream (again, in arrival order).
    private final ArrayList<IpServer> mActiveDownstreams;
    private NetworkState mUpstreamNetworkState;

    public IPv6TetheringCoordinator(SharedLog log) {
        mLog = log.forSubComponent(TAG);
        mActiveDownstreams = new ArrayList<>();
    }

    /** Add active downstream to ipv6 tethering candidate list. */
    public void addActiveDownstream(IpServer downstream) {
        if (!mActiveDownstreams.contains(downstream)) {
            // Adding a new downstream appends it to the list. Adding a
            // downstream a second time without first removing it has no effect.
            mActiveDownstreams.add(downstream);
            updateIPv6TetheringInterfaces();
        }
    }

    /** Remove downstream from ipv6 tethering candidate list. */
    public void removeActiveDownstream(IpServer downstream) {
        stopIPv6TetheringOn(downstream);
        if (mActiveDownstreams.remove(downstream)) {
            updateIPv6TetheringInterfaces();
        }
    }

    public void updateUpstreamNetworkState(NetworkState ns) {
        if (VDBG) {
            Log.d(TAG, "updateUpstreamNetworkState: " + toDebugString(ns));
        }
        if (TetheringInterfaceUtils.getIPv6Interface(ns) == null) {
            stopIPv6TetheringOnAllInterfaces();
            setUpstreamNetworkState(null);
            return;
        }

        if (mUpstreamNetworkState != null &&
            !ns.network.equals(mUpstreamNetworkState.network)) {
            stopIPv6TetheringOnAllInterfaces();
        }

        setUpstreamNetworkState(ns);
        updateIPv6TetheringInterfaces();
    }

    private void stopIPv6TetheringOnAllInterfaces() {
        for (IpServer ipServer : mActiveDownstreams) {
            stopIPv6TetheringOn(ipServer);
        }
    }

    private void setUpstreamNetworkState(NetworkState ns) {
        if (ns == null) {
            mUpstreamNetworkState = null;
        } else {
            // Make a deep copy of the parts we need.
            mUpstreamNetworkState = new NetworkState(
                    null,
                    new LinkProperties(ns.linkProperties),
                    new NetworkCapabilities(ns.networkCapabilities),
                    new Network(ns.network),
                    null,
                    null);
        }

        mLog.log("setUpstreamNetworkState: " + toDebugString(mUpstreamNetworkState));
    }

    private void updateIPv6TetheringInterfaces() {
        for (IpServer ipServer : mActiveDownstreams) {
            final LinkProperties lp = getInterfaceIPv6LinkProperties(ipServer);
            ipServer.sendMessage(IpServer.CMD_IPV6_TETHER_UPDATE, 0, 0, lp);
            break;
        }
    }

    private LinkProperties getInterfaceIPv6LinkProperties(IpServer ipServer) {
        if (ipServer.interfaceType() == ConnectivityManager.TETHERING_BLUETOOTH) {
            // TODO: Figure out IPv6 support on PAN interfaces.
            return null;
        }

        if (!mActiveDownstreams.contains(ipServer)) return null;

        if (mUpstreamNetworkState == null || mUpstreamNetworkState.linkProperties == null) {
            return null;
        }

        // NOTE: Here, in future, we would have policies to decide how to divvy
        // up the available dedicated prefixes among downstream interfaces.
        // At this time we have no such mechanism--we only support tethering
        // IPv6 toward the oldest (first requested) active downstream.
        final IpServer currentActive = mActiveDownstreams.get(0);
        if (currentActive != null && currentActive == ipServer) {
            final LinkProperties lp = getIPv6OnlyLinkProperties(
                    mUpstreamNetworkState.linkProperties);
            if (lp.hasIpv6DefaultRoute() && lp.hasGlobalIpv6Address()) {
                return lp;
            }
        }

        return null;
    }

    private static LinkProperties getIPv6OnlyLinkProperties(LinkProperties lp) {
        final LinkProperties v6only = new LinkProperties();
        if (lp == null) {
            return v6only;
        }

        // NOTE: At this time we don't copy over any information about any
        // stacked links. No current stacked link configuration has IPv6.

        v6only.setInterfaceName(lp.getInterfaceName());

        v6only.setMtu(lp.getMtu());

        for (LinkAddress linkAddr : lp.getLinkAddresses()) {
            if (linkAddr.isGlobalPreferred() && linkAddr.getPrefixLength() == 64) {
                v6only.addLinkAddress(linkAddr);
            }
        }

        for (RouteInfo routeInfo : lp.getRoutes()) {
            final IpPrefix destination = routeInfo.getDestination();
            if ((destination.getAddress() instanceof Inet6Address) &&
                (destination.getPrefixLength() <= 64)) {
                v6only.addRoute(routeInfo);
            }
        }

        for (InetAddress dnsServer : lp.getDnsServers()) {
            if (isIPv6GlobalAddress(dnsServer)) {
                // For now we include ULAs.
                v6only.addDnsServer(dnsServer);
            }
        }

        v6only.setDomains(lp.getDomains());

        return v6only;
    }

    // TODO: Delete this and switch to LinkAddress#isGlobalPreferred once we
    // announce our own IPv6 address as DNS server.
    private static boolean isIPv6GlobalAddress(InetAddress ip) {
        return (ip instanceof Inet6Address) &&
               !ip.isAnyLocalAddress() &&
               !ip.isLoopbackAddress() &&
               !ip.isLinkLocalAddress() &&
               !ip.isSiteLocalAddress() &&
               !ip.isMulticastAddress();
    }

    private static String toDebugString(NetworkState ns) {
        if (ns == null) {
            return "NetworkState{null}";
        }
        return String.format("NetworkState{%s, %s, %s}",
                ns.network,
                ns.networkCapabilities,
                ns.linkProperties);
    }

    private static void stopIPv6TetheringOn(IpServer ipServer) {
        ipServer.sendMessage(IpServer.CMD_IPV6_TETHER_UPDATE, 0, 0, null);
    }
}
