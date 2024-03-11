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

import java.util.ArrayList;
import java.util.List;

public class VpnConnection {
    private VpnConnectionParams mVpnConnectionParams;
    private int connectedPeriodSeconds;
    private List<Integer> underlyingNetworkType = new ArrayList<>();
    private int vpnValidatedPeriodSeconds;
    private int validationAttempts;
    private int validationAttemptsSuccess;
    private List<Integer> errorCode = new ArrayList<>();
    private List<RecoveryAction> recoveryAttempts = new ArrayList<>();
    private int recoveryLatency;
    private int ikeAttempts;
    private int ikeAttemptsSuccess;
    private int ikeAttemptsLatencyMilliseconds;
    private int switchAttempts;
    private int switchAttemptsSuccess;
    private int switchAttemptsLatencyMilliseconds;

    public void setVpnConnectionParams(VpnConnectionParams vpnConnectionParams) {
        mVpnConnectionParams = vpnConnectionParams;
    }

    public void setConnectedPeriodSeconds(int connectedPeriodSeconds) {
        this.connectedPeriodSeconds = connectedPeriodSeconds;
    }

    public void addUnderlyingNetworkType(int networkType) {
        underlyingNetworkType.add(networkType);
    }

    public void setVpnValidatedPeriodSeconds(int vpnValidatedPeriodSeconds) {
        this.vpnValidatedPeriodSeconds = vpnValidatedPeriodSeconds;
    }

    public void setValidationAttempts(int validationAttempts) {
        this.validationAttempts = validationAttempts;
    }

    public void setValidationAttemptsSuccess(int validationAttemptsSuccess) {
        this.validationAttemptsSuccess = validationAttemptsSuccess;
    }

    public void addErrorCode(int code) {
        errorCode.add(code);
    }

    public void addRecoveryAttempts(RecoveryAction action) {
        recoveryAttempts.add(action);
    }

    public void setRecoveryLatency(int recoveryLatency) {
        this.recoveryLatency = recoveryLatency;
    }

    public void setIkeAttempts(int ikeAttempts) {
        this.ikeAttempts = ikeAttempts;
    }

    public void setIkeAttemptsSuccess(int ikeAttemptsSuccess) {
        this.ikeAttemptsSuccess = ikeAttemptsSuccess;
    }

    public void setIkeAttemptsLatencyMilliseconds(int ikeAttemptsLatencyMilliseconds) {
        this.ikeAttemptsLatencyMilliseconds = ikeAttemptsLatencyMilliseconds;
    }

    public void setSwitchAttempts(int switchAttempts) {
        this.switchAttempts = switchAttempts;
    }

    public void setSwitchAttemptsSuccess(int switchAttemptsSuccess) {
        this.switchAttemptsSuccess = switchAttemptsSuccess;
    }

    public void setSwitchAttemptsLatencyMilliseconds(
            int switchAttemptsLatencyMilliseconds) {
        this.switchAttemptsLatencyMilliseconds = switchAttemptsLatencyMilliseconds;
    }

    public VpnConnectionParams getmVpnConnectionParams() {
        return mVpnConnectionParams;
    }

    public int getConnectedPeriodSeconds() {
        return connectedPeriodSeconds;
    }

    public List<Integer> getUnderlyingNetworkType() {
        return underlyingNetworkType;
    }

    public int getVpnValidatedPeriodSeconds() {
        return vpnValidatedPeriodSeconds;
    }

    public int getValidationAttempts() {
        return validationAttempts;
    }

    public int getValidationAttemptsSuccess() {
        return validationAttemptsSuccess;
    }

    public List<Integer> getErrorCode() {
        return errorCode;
    }

    public List<RecoveryAction> getRecoveryAttempts() {
        return recoveryAttempts;
    }

    public int getRecoveryLatency() {
        return recoveryLatency;
    }

    public int getIkeAttempts() {
        return ikeAttempts;
    }

    public int getIkeAttemptsSuccess() {
        return ikeAttemptsSuccess;
    }

    public int getIkeAttemptsLatencyMilliseconds() {
        return ikeAttemptsLatencyMilliseconds;
    }

    public int getSwitchAttempts() {
        return switchAttempts;
    }

    public int getSwitchAttemptsSuccess() {
        return switchAttemptsSuccess;
    }

    public int getSwitchAttemptsLatencyMilliseconds() {
        return switchAttemptsLatencyMilliseconds;
    }
}