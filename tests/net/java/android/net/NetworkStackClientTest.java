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

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.util.SharedLog;
import android.os.HandlerThread;
import android.os.Process;
import android.os.UserHandle;
import android.os.test.TestLooper;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for {@link NetworkStackClient}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class NetworkStackClientTest {
    private static final String TEST_PACKAGE_NAME = "com.android.testnetworkstack";
    private static final String TEST_SERVICE_CLASS_NAME =
            "com.android.testnetworkstack.TestNetworkStackService";

    @Mock private Context mContext;
    @Mock private SharedLog mLog;
    @Mock private NetworkStackClient.Dependencies mDependencies;
    @Mock private HandlerThread mHandlerThread;
    @Mock private PackageManager mPackageManager;
    @Mock private INetworkStackConnector.Stub mConnector;
    @Captor private ArgumentCaptor<Intent> mResolveIntentCaptor;
    @Captor private ArgumentCaptor<Intent> mBindIntentCaptor;

    private TestLooper mLooper;
    private NetworkStackClient mClient;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mLooper = new TestLooper();
        mClient = spy(new NetworkStackClient(mLog, mDependencies));

        doNothing().when(mDependencies).addService(any());

        when(mHandlerThread.getLooper()).thenReturn(mLooper.getLooper());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        final ResolveInfo mockServiceInfo = makeServiceInfo(
                TEST_PACKAGE_NAME, TEST_SERVICE_CLASS_NAME, FLAG_SYSTEM);

        when(mPackageManager.queryIntentServices(mResolveIntentCaptor.capture(), anyInt()))
                .thenReturn(Collections.singletonList(mockServiceInfo));
        when(mPackageManager.getPackageUidAsUser(TEST_PACKAGE_NAME, UserHandle.USER_SYSTEM))
                .thenReturn(Process.NETWORK_STACK_UID);
        when(mPackageManager.checkPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
                TEST_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_GRANTED);
    }

    private static ResolveInfo makeServiceInfo(String pkg, String clazz, int flags) {
        final ResolveInfo serviceInfo = new ResolveInfo();
        serviceInfo.serviceInfo = new ServiceInfo();
        serviceInfo.serviceInfo.name = clazz;
        serviceInfo.serviceInfo.applicationInfo = new ApplicationInfo();
        serviceInfo.serviceInfo.applicationInfo.packageName = pkg;
        serviceInfo.serviceInfo.applicationInfo.flags = flags;
        return serviceInfo;
    }

    @Test
    public void testStart() {
        doStartTest(0);
    }

    @Test
    public void testStart_OtherNonSystemPackages() {
        // Some other non-system package implements the network stack service intent: they should be
        // ignored.
        when(mPackageManager.queryIntentServices(mResolveIntentCaptor.capture(), anyInt()))
                .thenReturn(Arrays.asList(
                        makeServiceInfo("com.example.package", "com.example.Class", 0),
                        makeServiceInfo(TEST_PACKAGE_NAME, TEST_SERVICE_CLASS_NAME, FLAG_SYSTEM)));
        doStartTest(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testStart_OtherSystemPackages() {
        // Some other non-system package implements the network stack service intent: start should
        // crash.
        when(mPackageManager.queryIntentServices(mResolveIntentCaptor.capture(), anyInt()))
                .thenReturn(Arrays.asList(
                        makeServiceInfo("com.example.package", "com.example.Class", FLAG_SYSTEM),
                        makeServiceInfo(TEST_PACKAGE_NAME, TEST_SERVICE_CLASS_NAME, FLAG_SYSTEM)));
        doStartTest(0);
    }

    @Test(expected = SecurityException.class)
    public void testStart_NoPermission() {
        when(mPackageManager.checkPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
                TEST_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_DENIED);
        doStartTest(0);
    }

    @Test(expected = SecurityException.class)
    public void testStart_WrongUid() throws Exception {
        when(mPackageManager.getPackageUidAsUser(TEST_PACKAGE_NAME, UserHandle.USER_SYSTEM))
                .thenReturn(Process.NETWORK_STACK_UID + 1);
        doStartTest(0);
    }

    @Test
    public void testRestart() {
        doStartTest(NetworkStackClient.MAX_START_RETRIES);
    }

    @Test(expected = IllegalStateException.class)
    public void testRestart_TooManyRetries() {
        doStartTest(NetworkStackClient.MAX_START_RETRIES + 1);
    }

    private void doStartTest(int restartCount) {
        final ArgumentCaptor<ServiceConnection> serviceConnectionCaptor =
                ArgumentCaptor.forClass(ServiceConnection.class);
        when(mContext.bindServiceAsUser(
                mBindIntentCaptor.capture(), serviceConnectionCaptor.capture(), anyInt(), any()))
                .thenReturn(true);
        mClient.start(mContext, mHandlerThread);

        for (int i = 1; i <= restartCount; i++) {
            // No callback to onServiceConnected after timeout: should restart
            mLooper.moveTimeForward(NetworkStackClient.START_RETRY_DELAY_MS + 1);
            mLooper.dispatchAll();
            // Called i + 1 times: first invocation and number of retries
            verify(mContext, times(i + 1)).bindServiceAsUser(any(), any(), anyInt(), any());

            final Intent bindIntent = mBindIntentCaptor.getValue();
            assertNotNull(bindIntent.getComponent());
            assertEquals(TEST_PACKAGE_NAME, bindIntent.getComponent().getPackageName());
            assertEquals(TEST_SERVICE_CLASS_NAME, bindIntent.getComponent().getClassName());
        }

        // Network stack start succeeds: onServiceConnected is called
        serviceConnectionCaptor.getValue().onServiceConnected(null, mConnector);
        verify(mDependencies, times(1)).addService(mConnector);

        mLooper.moveTimeForward(NetworkStackClient.START_RETRY_DELAY_MS + 1);
        mLooper.dispatchAll();
        // Still restartCount + 1 invocation (was not called again)
        verify(mContext, times(restartCount + 1)).bindServiceAsUser(any(), any(), anyInt(), any());
    }
}
