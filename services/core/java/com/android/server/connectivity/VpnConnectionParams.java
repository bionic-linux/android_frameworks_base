/*
 * Copyright (C) 2024 The Android Open Source Project
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

public class VpnConnectionParams {
    private int vpnType;
    private int connIpProtocol;
    private int serverIpProtocol;
    private int encapType;
    private boolean bypassability;
    private boolean validationRequired;
    private int vpnProfileType;
    private int allowedAlogithms;
    private int mtu;
    private boolean localRouteExcluded;
    private boolean metered;
    private boolean proxySetup;
    private boolean alwaysOnVpn;
    private boolean lockdownVpn;
    private boolean preconfiguredDns;
    private boolean preconfiguredRoutes;
    private int vpnOwnerUid;

    public void setVpnType(int vpnType) {
        this.vpnType = vpnType;
    }

    public void setConnIpProtocol(int connIpProtocol) {
        this.connIpProtocol = connIpProtocol;
    }

    public void setServerIpProtocol(int serverIpProtocol) {
        this.serverIpProtocol = serverIpProtocol;
    }

    public void setEncapType(int encapType) {
        this.encapType = encapType;
    }

    public void setBypassability(boolean bypassability) {
        this.bypassability = bypassability;
    }

    public void setValidationRequired(boolean validationRequired) {
        this.validationRequired = validationRequired;
    }

    public void setVpnProfileType(int vpnProfileType) {
        this.vpnProfileType = vpnProfileType;
    }

    public void setAllowedAlogithms(int allowedAlogithms) {
        this.allowedAlogithms = allowedAlogithms;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public void setLocalRouteExcluded(boolean localRouteExcluded) {
        this.localRouteExcluded = localRouteExcluded;
    }

    public void setMetered(boolean metered) {
        this.metered = metered;
    }

    public void setProxySetup(boolean proxySetup) {
        this.proxySetup = proxySetup;
    }

    public void setAlwaysOnVpn(boolean alwaysOnVpn) {
        this.alwaysOnVpn = alwaysOnVpn;
    }

    public void setLockdownVpn(boolean lockdownVpn) {
        this.lockdownVpn = lockdownVpn;
    }

    public void setPreconfiguredDns(boolean preconfiguredDns) {
        this.preconfiguredDns = preconfiguredDns;
    }

    public void setPreconfiguredRoutes(boolean preconfiguredRoutes) {
        this.preconfiguredRoutes = preconfiguredRoutes;
    }

    public void setVpnOwnerUid(int vpnOwnerUid) {
        this.vpnOwnerUid = vpnOwnerUid;
    }

    public int getVpnType() {
        return vpnType;
    }

    public int getConnIpProtocol() {
        return connIpProtocol;
    }

    public int getServerIpProtocol() {
        return serverIpProtocol;
    }

    public int getEncapType() {
        return encapType;
    }

    public boolean isBypassability() {
        return bypassability;
    }

    public boolean isValidationRequired() {
        return validationRequired;
    }

    public int getVpnProfileType() {
        return vpnProfileType;
    }

    public int getAllowedAlogithms() {
        return allowedAlogithms;
    }

    public int getMtu() {
        return mtu;
    }

    public boolean isLocalRouteExcluded() {
        return localRouteExcluded;
    }

    public boolean isMetered() {
        return metered;
    }

    public boolean isProxySetup() {
        return proxySetup;
    }

    public boolean isAlwaysOnVpn() {
        return alwaysOnVpn;
    }

    public boolean isLockdownVpn() {
        return lockdownVpn;
    }

    public boolean isPreconfiguredDns() {
        return preconfiguredDns;
    }

    public boolean isPreconfiguredRoutes() {
        return preconfiguredRoutes;
    }

    public int getVpnOwnerUid() {
        return vpnOwnerUid;
    }
}