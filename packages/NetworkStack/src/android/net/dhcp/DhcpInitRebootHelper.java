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

package android.net.dhcp;

import android.content.Context;
import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DhcpInitRebootHelper {

    private static final String TAG = "DhcpLeaseHistory";

    private static final String WIFI_IFACE_PRFIX = "wlan";
    private static final String DHCP_LEASE_FILE = "/data/misc/wifi/dhcp_lease.conf";

    private static final String LEASE_KEY_IP        = "IP";
    private static final String LEASE_KEY_GATEWAY   = "Gateway";
    private static final String LEASE_KEY_DNS       = "DNS";
    private static final String LEASE_KEY_DOMAIN    = "Domain";
    private static final String LEASE_KEY_SERVER    = "Server";
    private static final String LEASE_KEY_WIFICONF  = "WifiConfigKey";

    private DhcpResults mPastDhcpLease;
    private boolean mIsInitRebootEnabled;

    private final Context mContext;
    private final String mIfaceName;
    private String mReqIp;
    private String mReqGateway;
    private String mReqDns;
    private String mReqDomain;
    private String mSrvIp;
    private String mWifiConfigKey;

    private static HashMap<String, DhcpResults> sDhcpResultMap =
            new HashMap<String, DhcpResults>();

    public DhcpInitRebootHelper(Context context, String iface) {
        mContext = context;
        mIfaceName = iface;
        if (mIfaceName != null && mIfaceName.startsWith(WIFI_IFACE_PRFIX)) {
            mIsInitRebootEnabled = true;
        }
        Log.d(TAG, "mIsInitRebootEnabled =" + mIsInitRebootEnabled);
    }

    public boolean isReady() {
        if (mIsInitRebootEnabled){
            WifiConfiguration wifiCfg = getCurrentWifiConfigWithTimeout();
            if (wifiCfg != null) {
                fetchPastLease();
                checkIpRecovery(wifiCfg);
            }
        }
        Log.d(TAG, "[isReady]past lease after check:\n\t" + mPastDhcpLease);
        return mPastDhcpLease != null ? true : false;
    }

    public DhcpResults getPastDhcpLease () {
        return mPastDhcpLease;
    }

    public void clearPastDhcpLease() {
        mPastDhcpLease = null;
    }

    public void updateLeaseResult(DhcpResults result) {
        if (mIsInitRebootEnabled && result != null) {
            WifiConfiguration wifiCfg = getCurrentWifiConfigWithTimeout();
            if (wifiCfg !=  null) {
                String wifiConfigKey = saveDhcpResult(wifiCfg, result);
                if (wifiConfigKey != null) {
                    writeLeaseToFile(wifiConfigKey, result);
                }
            }
        }
    }

    private String saveDhcpResult(WifiConfiguration wifiCfg, DhcpResults result) {
        // exclude IP recovery for SECURITY_WEP
        if (wifiCfg.allowedKeyManagement.get(KeyMgmt.NONE)
                && wifiCfg.wepTxKeyIndex >= 0
                && wifiCfg.wepTxKeyIndex < wifiCfg.wepKeys.length
                && wifiCfg.wepKeys[wifiCfg.wepTxKeyIndex] != null) {
            Log.d(TAG, "Skip SECURITY_WEP");
            return null;
        }
        String configKey = wifiCfg.configKey();
        Log.d(TAG, "[saveDhcpResult]: record put:" + configKey + " with " + result);
        sDhcpResultMap.put(configKey, result);
        return configKey;
    }

    private void writeLeaseToFile(String wifiConfigKey, DhcpResults result) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(DHCP_LEASE_FILE));
            if (result.ipAddress != null
                    && result.ipAddress.getAddress() != null) {
                writer.write(LEASE_KEY_IP + "=" + result.ipAddress + "\n");
            }
            if (result.gateway != null
                    && result.gateway.getAddress() != null) {
                writer.write(LEASE_KEY_GATEWAY + "=" + result.gateway.getHostAddress() + "\n");
            }
            if (result.dnsServers != null) {
                for (InetAddress dns : result.dnsServers) {
                    writer.write(LEASE_KEY_DNS + "=" + dns.getHostAddress() + "\n");
                    break;
                }
            }
            if (result.domains != null) {
                writer.write(LEASE_KEY_DOMAIN + "=" + result.domains + "\n");
            }
            if (result.serverAddress != null) {
                writer.write(LEASE_KEY_SERVER + "=" + result.serverAddress.getHostAddress() + "\n");
            }
            if (wifiConfigKey != null) {
                writer.write(LEASE_KEY_WIFICONF + "=" + wifiConfigKey + "\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "putLeaseToFile()-01: " + e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "putLeaseToFile()-02: " + e);
            }
        }
    }

    private WifiConfiguration getCurrentWifiConfigWithTimeout() {
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<Object> task = new Callable<Object>() {
            public Object call() {
                return getCurrentWifiConfig();
            }
        };
        Future<Object> future = executor.submit(task);
        try {
            Object result = future.get(3, TimeUnit.SECONDS);
            return (WifiConfiguration) result;
        } catch (Exception ex) {
            Log.e(TAG, "getCurrentWifiConfigWithTimeout:" + ex);
        } finally {
            future.cancel(true);
        }
        return null;
    }

    private WifiConfiguration getCurrentWifiConfig() {
        WifiManager wifiMgr = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr == null) return null;
        WifiInfo info = wifiMgr.getConnectionInfo();
        if (info == null) {
            Log.e(TAG, "[getCurrentWifiConfig]wifi info is nul");
            return null;
        }
        List<WifiConfiguration> networks = wifiMgr.getConfiguredNetworks();
        int length = networks.size();
        for (int i = 0; i < length; i++) {
            if (networks.get(i).networkId == info.getNetworkId()) {
                return networks.get(i);
            }
        }
        return null;
    }

    private void fetchPastLease() {
        if (mPastDhcpLease == null) {
            parseLeaseFromFile();
            if (mReqIp == null || mReqGateway == null || mReqDns == null || mSrvIp == null) {
                Log.e(TAG, "checkPastLease(): past dhcp lease was not valid" +
                        ", request IP = " + mReqIp +
                        ", request Gateway = " + mReqGateway +
                        ", request DNS = " + mReqDns +
                        ", server IP = " + mSrvIp);
            } else {
                DhcpResults savedDhcpLease = new DhcpResults();
                try {
                    savedDhcpLease.ipAddress = new LinkAddress(mReqIp);
                    savedDhcpLease.gateway = InetAddress.getByName(mReqGateway);
                    savedDhcpLease.dnsServers.add((Inet4Address) InetAddress.getByName(mReqDns));
                    savedDhcpLease.domains = mReqDomain;
                    savedDhcpLease.serverAddress = (Inet4Address) InetAddress.getByName(mSrvIp);
                } catch (Exception e) {
                    savedDhcpLease = null;
                    Log.e(TAG, "checkPastLease(): past dhcp lease some IP was not valid, " + e);
                }
                if (savedDhcpLease != null && sDhcpResultMap != null) {
                    if (mWifiConfigKey != null) {
                        Log.d(TAG, "fetchPastLease:" + mWifiConfigKey + ", " + savedDhcpLease);
                        sDhcpResultMap.put(mWifiConfigKey, savedDhcpLease);
                    }
                }
            }
        }
    }

    private void parseLeaseFromFile() {
        File file = new File(DHCP_LEASE_FILE);
        if (file.exists()) {
            BufferedReader reader = null;
            String[] nameValue = null;
            try {
                reader = new BufferedReader(new FileReader(DHCP_LEASE_FILE));
                for (String line = reader.readLine(); line != null; line = reader.readLine()){
                    if (line.startsWith(LEASE_KEY_IP)) {
                        nameValue = line.split("=");
                        mReqIp = (nameValue.length != 2) ? null : nameValue[1];
                    } else if (line.startsWith(LEASE_KEY_GATEWAY)) {
                        nameValue = line.split("=");
                        mReqGateway = (nameValue.length != 2) ? null : nameValue[1];
                    } else if (line.startsWith(LEASE_KEY_DNS)) {
                        nameValue = line.split("=");
                        mReqDns = (nameValue.length != 2) ? null : nameValue[1];
                    } else if (line.startsWith(LEASE_KEY_DOMAIN)) {
                        nameValue = line.split("=");
                        mReqDomain = (nameValue.length != 2) ? null : nameValue[1];
                    } else if (line.startsWith(LEASE_KEY_SERVER)) {
                        nameValue = line.split("=");
                        mSrvIp = (nameValue.length != 2) ? null : nameValue[1];
                    } else if (line.startsWith(LEASE_KEY_WIFICONF)) {
                        nameValue = line.split("=");
                        mWifiConfigKey = (nameValue.length != 2) ? null : nameValue[1];
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "getLeaseFromFile()-01: " + e);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "getLeaseFromFile()-02: " + e);
                }
            }
        } else {
            Log.e(TAG, "parseLeaseFromFile(): file not existed");
        }
    }

    private void checkIpRecovery(WifiConfiguration wifiCfg) {
        String configKey = wifiCfg.configKey();
        DhcpResults record = sDhcpResultMap.get(configKey);
        Log.d(TAG, "checkIpRecovery(" + sDhcpResultMap.size() + ") get DhcpResult Key=["
                + configKey + "], record=[" + record + "]");
        if (record != null) {
            mPastDhcpLease = record;
        }
    }
}
