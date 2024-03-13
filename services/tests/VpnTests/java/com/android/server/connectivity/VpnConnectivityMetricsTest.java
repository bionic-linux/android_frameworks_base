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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkAgent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.android.server.connectivity.VpnConnectivityMetrics.VpnMetricCollector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VpnConnectivityMetricsTest {
    private static final int TEST_USER_ID = 1;

    private VpnConnectivityMetrics mVpnConnectivityMetrics;

    @Mock private VpnConnectivityMetrics.Dependencies mDependencies;
    @Mock private NetworkAgent mNetworkAgent;
    @Mock private Context mContext;
    @Mock private ConnectivityManager mConnectivityManager;

    private void setElapsedRealtimeMs(long time) {
        doReturn(time).when(mDependencies).getElapsedRealtime();
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(Context.CONNECTIVITY_SERVICE).when(mContext)
                .getSystemServiceName(ConnectivityManager.class);
        doReturn(mConnectivityManager).when(mContext)
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        setElapsedRealtimeMs(0);
        mVpnConnectivityMetrics = new VpnConnectivityMetrics(mContext, mDependencies);
    }

    @Test
    public void testEmptyMetrics() {
        mVpnConnectivityMetrics.newCollector(TEST_USER_ID);
        final List<VpnConnection> metrics = mVpnConnectivityMetrics.pullMetrics();
        assertTrue(metrics.isEmpty());
    }

    @Test
    public void testConnectedPeriod() {
        final long timeConnectedMs = 1000;
        final long timeDisconnectedMs = 5124;
        final int expectedConnectedTimeSec = 4;
        final VpnMetricCollector vpnMetricCollector =
                mVpnConnectivityMetrics.newCollector(TEST_USER_ID);
        vpnMetricCollector.onAppStarted();

        setElapsedRealtimeMs(timeConnectedMs);
        vpnMetricCollector.onVpnConnected(mNetworkAgent);
        setElapsedRealtimeMs(timeDisconnectedMs);
        vpnMetricCollector.onVpnDisconnected(mNetworkAgent);

        final List<VpnConnection> metrics = mVpnConnectivityMetrics.pullMetrics();
        assertEquals(1, metrics.size());
        final int actualConnectedTimeSec = metrics.get(0).getConnectedPeriodSeconds();
        assertEquals(actualConnectedTimeSec, expectedConnectedTimeSec);
    }

    @Test
    public void testConnectedPeriod_PullBeforeDisconnect() {
        final long timeConnectedMs = 5234;
        final long timePullMs = 50124;
        final int expectedConnectedTimeSec = 44;
        final VpnMetricCollector vpnMetricCollector =
                mVpnConnectivityMetrics.newCollector(TEST_USER_ID);
        vpnMetricCollector.onAppStarted();

        setElapsedRealtimeMs(timeConnectedMs);
        vpnMetricCollector.onVpnConnected(mNetworkAgent);
        setElapsedRealtimeMs(timePullMs);
        final List<VpnConnection> metrics = mVpnConnectivityMetrics.pullMetrics();

        assertEquals(1, metrics.size());
        final int actualConnectedTimeSec = metrics.get(0).getConnectedPeriodSeconds();
        assertEquals(actualConnectedTimeSec, expectedConnectedTimeSec);
    }

    @Test
    public void testValidatedPeriod() {
        final long timeConnectedMs = 5000;
        final long timeValidationPassMs = 10021;
        final long timePullMs = 50124;
        final int expectedValidatedTimeSec = 40;
        final int expectedValidationAttempts = 2;
        final int expectedValidationSuccess = 1;
        final VpnMetricCollector vpnMetricCollector =
                mVpnConnectivityMetrics.newCollector(TEST_USER_ID);
        vpnMetricCollector.onAppStarted();

        setElapsedRealtimeMs(timeConnectedMs);
        vpnMetricCollector.onVpnConnected(mNetworkAgent);
        // first failure
        vpnMetricCollector.onValidationStatus(
                mNetworkAgent, NetworkAgent.VALIDATION_STATUS_NOT_VALID);
        setElapsedRealtimeMs(timeValidationPassMs);
        vpnMetricCollector.onValidationStatus(mNetworkAgent, NetworkAgent.VALIDATION_STATUS_VALID);
        setElapsedRealtimeMs(timePullMs);
        final List<VpnConnection> metrics = mVpnConnectivityMetrics.pullMetrics();

        assertEquals(1, metrics.size());
        final int actualValidatedTimeSec = metrics.get(0).getVpnValidatedPeriodSeconds();
        assertEquals(expectedValidatedTimeSec, actualValidatedTimeSec);
        assertEquals(expectedValidationAttempts, metrics.get(0).getValidationAttempts());
        assertEquals(expectedValidationSuccess, metrics.get(0).getValidationAttemptsSuccess());
    }

}
