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

package com.android.networkstack.tethering.metrics;

import androidx.annotation.Nullable;

import static android.net.TetheringManager.TETHERING_BLUETOOTH;
import static android.net.TetheringManager.TETHERING_ETHERNET;
import static android.net.TetheringManager.TETHERING_INVALID;
import static android.net.TetheringManager.TETHERING_NCM;
import static android.net.TetheringManager.TETHERING_USB;
import static android.net.TetheringManager.TETHERING_WIFI;
import static android.net.TetheringManager.TETHERING_WIFI_P2P;
import static android.net.TetheringManager.TETHER_ERROR_NO_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_UNKNOWN_IFACE ;
import static android.net.TetheringManager.TETHER_ERROR_SERVICE_UNAVAIL;
import static android.net.TetheringManager.TETHER_ERROR_UNSUPPORTED;
import static android.net.TetheringManager.TETHER_ERROR_UNAVAIL_IFACE;
import static android.net.TetheringManager.TETHER_ERROR_INTERNAL_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_TETHER_IFACE_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_UNTETHER_IFACE_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_ENABLE_FORWARDING_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_DISABLE_FORWARDING_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_IFACE_CFG_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_PROVISIONING_FAILED;
import static android.net.TetheringManager.TETHER_ERROR_DHCPSERVER_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_ENTITLEMENT_UNKNOWN;
import static android.net.TetheringManager.TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION;
import static android.net.TetheringManager.TETHER_ERROR_NO_ACCESS_TETHERING_PERMISSION;
import static android.net.TetheringManager.TETHER_ERROR_UNKNOWN_TYPE;


/**
 *
 */
public class TetheringMetrics {
  private static NetworkTetheringReported mStats;
  private static NetworkTetheringReported.Builder mStatsBuilder;

  private static android.stats.connectivity.DownstreamType downstreamTypeToEnum(int ifaceType) {
    switch(ifaceType) {
      case TETHERING_WIFI:
	  	return android.stats.connectivity.DownstreamType.DS_TETHERING_WIFI;
      case TETHERING_WIFI_P2P:
        return android.stats.connectivity.DownstreamType.DS_TETHERING_WIFI_P2P;
      case TETHERING_USB:
        return android.stats.connectivity.DownstreamType.DS_TETHERING_USB;
      case TETHERING_BLUETOOTH:
        return android.stats.connectivity.DownstreamType.DS_TETHERING_BLUETOOTH;
      case TETHERING_NCM:
        return android.stats.connectivity.DownstreamType.DS_TETHERING_NCM;
      default:
      return android.stats.connectivity.DownstreamType.DS_TETHERING_INVALID;
    }
  }

  private static android.stats.connectivity.ErrorCode errorCodeToEnum(int lastError) {
    switch(lastError) {
      case TETHER_ERROR_NO_ERROR:
        return android.stats.connectivity.ErrorCode.EC_NO_ERROR;
      case TETHER_ERROR_UNKNOWN_IFACE:
        return android.stats.connectivity.ErrorCode.EC_UNKNOWN_IFACE;
      case TETHER_ERROR_SERVICE_UNAVAIL:
		return android.stats.connectivity.ErrorCode.EC_SERVICE_UNAVAIL;
      case TETHER_ERROR_UNSUPPORTED:
		return android.stats.connectivity.ErrorCode.EC_UNSUPPORTED;
      case TETHER_ERROR_UNAVAIL_IFACE:
		return android.stats.connectivity.ErrorCode.EC_UNAVAIL_IFACE;
      case TETHER_ERROR_INTERNAL_ERROR:
		return android.stats.connectivity.ErrorCode.EC_INTERNAL_ERROR;
      case TETHER_ERROR_TETHER_IFACE_ERROR:
		return android.stats.connectivity.ErrorCode.EC_TETHER_IFACE_ERROR;
      case TETHER_ERROR_UNTETHER_IFACE_ERROR:
		return android.stats.connectivity.ErrorCode.EC_UNTETHER_IFACE_ERROR;
      case TETHER_ERROR_ENABLE_FORWARDING_ERROR:
		return android.stats.connectivity.ErrorCode.EC_ENABLE_FORWARDING_ERROR;
      case TETHER_ERROR_DISABLE_FORWARDING_ERROR:
		return android.stats.connectivity.ErrorCode.EC_DISABLE_FORWARDING_ERROR;
      case TETHER_ERROR_IFACE_CFG_ERROR:
		return android.stats.connectivity.ErrorCode.EC_IFACE_CFG_ERROR;
      case TETHER_ERROR_PROVISIONING_FAILED:
		return android.stats.connectivity.ErrorCode.EC_PROVISIONING_FAILED;
      case TETHER_ERROR_DHCPSERVER_ERROR:
		return android.stats.connectivity.ErrorCode.EC_DHCPSERVER_ERROR;
      case TETHER_ERROR_ENTITLEMENT_UNKNOWN:
		return android.stats.connectivity.ErrorCode.EC_ENTITLEMENT_UNKNOWN;
      case TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION:
		return android.stats.connectivity.ErrorCode.EC_NO_CHANGE_TETHERING_PERMISSION;
      case TETHER_ERROR_NO_ACCESS_TETHERING_PERMISSION:
		return android.stats.connectivity.ErrorCode.EC_NO_ACCESS_TETHERING_PERMISSION;
      default:
		return android.stats.connectivity.ErrorCode.EC_UNKNOWN_TYPE;
    }
  }

  public TetheringMetrics() {
  	mStatsBuilder = NetworkTetheringReported.newBuilder();
  }

  public static void statsWrite(int ifaceType, int lastError) {
    mStatsBuilder.setDownstreamType(downstreamTypeToEnum(ifaceType));
	mStatsBuilder.setErrorCode(errorCodeToEnum(lastError));
    mStats = mStatsBuilder.build();

    TetheringStatsLog.write(TetheringStatsLog.NETWORK_TETHERING_REPORTED,
        mStats.getErrorCode().getNumber(),
        mStats.getDownstreamType().getNumber(),
        mStats.getUpstreamType().getNumber());
  }
}
