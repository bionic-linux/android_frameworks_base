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
import static android.net.TetheringManager.TETHER_ERROR_NO_ERROR;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.net.IIntResultListener;
import android.net.ITetheringConnector;
import android.net.ITetheringEventCallback;
import android.net.TetheringRequestParcel;
import android.os.IBinder;
import android.os.ResultReceiver;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class TetheringServiceTest {
    private static final String TEST_IFACE_NAME = "test_wlan0";
    private static final String TEST_CALLER_PKG = "test_pkg";
    @Mock private Tethering mTethering;
    @Mock private ITetheringEventCallback mITetheringEventCallback;
    @Rule public ServiceTestRule mServiceTestRule;
    private Intent mMockServiceIntent;
    private ITetheringConnector mTetheringConnector;

    private class TestTetheringResult extends IIntResultListener.Stub {
        private boolean mHasResult = false;
        @Override
        public void onResult(final int resultCode) {
            mHasResult = true;
        }

        public void assertHasResult() {
            assertTrue(mHasResult);
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MockTetheringService.setTethering(mTethering);
        mServiceTestRule = new ServiceTestRule();
        mMockServiceIntent = new Intent(
                InstrumentationRegistry.getTargetContext(),
                MockTetheringService.class);
        IBinder binder = mServiceTestRule.bindService(mMockServiceIntent);
        mTetheringConnector = ITetheringConnector.Stub.asInterface(binder);
        reset(mTethering);
        when(mTethering.hasTetherableConfiguration()).thenReturn(true);
    }

    @After
    public void tearDwon() throws Exception {
        mServiceTestRule.unbindService();
        reset(mTethering);
    }

    @Test
    public void testTether() throws Exception {
        when(mTethering.tether(TEST_IFACE_NAME)).thenReturn(TETHER_ERROR_NO_ERROR);
        final TestTetheringResult result = new TestTetheringResult();
        mTetheringConnector.tether(TEST_IFACE_NAME, TEST_CALLER_PKG, result);
        verify(mTethering).hasTetherableConfiguration();
        verify(mTethering, times(1)).tether(TEST_IFACE_NAME);
        verifyNoMoreInteractions(mTethering);
        result.assertHasResult();
    }

    @Test
    public void testUntether() throws Exception {
        when(mTethering.untether(TEST_IFACE_NAME)).thenReturn(TETHER_ERROR_NO_ERROR);
        final TestTetheringResult result = new TestTetheringResult();
        mTetheringConnector.untether(TEST_IFACE_NAME, TEST_CALLER_PKG, result);
        verify(mTethering).hasTetherableConfiguration();
        verify(mTethering, times(1)).untether(TEST_IFACE_NAME);
        verifyNoMoreInteractions(mTethering);
        result.assertHasResult();
    }

    @Test
    public void testSetUsbTethering() throws Exception {
        when(mTethering.setUsbTethering(true /* enable */)).thenReturn(TETHER_ERROR_NO_ERROR);
        final TestTetheringResult result = new TestTetheringResult();
        mTetheringConnector.setUsbTethering(true /* enable */, TEST_CALLER_PKG, result);
        verify(mTethering).hasTetherableConfiguration();
        verify(mTethering, times(1)).setUsbTethering(true /* enable */);
        verifyNoMoreInteractions(mTethering);
        result.assertHasResult();
    }

    @Test
    public void testStartTethering() throws Exception {
        final TestTetheringResult result = new TestTetheringResult();
        final TetheringRequestParcel request = new TetheringRequestParcel();
        request.tetheringType = TETHERING_WIFI;
        mTetheringConnector.startTethering(request, TEST_CALLER_PKG, result);
        verify(mTethering).hasTetherableConfiguration();
        verify(mTethering, times(1)).startTethering(eq(request), eq(result));
        verifyNoMoreInteractions(mTethering);
    }

    @Test
    public void testStopTethering() throws Exception {
        final TestTetheringResult result = new TestTetheringResult();
        mTetheringConnector.stopTethering(TETHERING_WIFI, TEST_CALLER_PKG, result);
        verify(mTethering).hasTetherableConfiguration();
        verify(mTethering, times(1)).stopTethering(TETHERING_WIFI);
        verifyNoMoreInteractions(mTethering);
        result.assertHasResult();
    }

    @Test
    public void testRequestLatestTetheringEntitlementResult() throws Exception {
        final ResultReceiver result = new ResultReceiver(null);
        mTetheringConnector.requestLatestTetheringEntitlementResult(TETHERING_WIFI, result,
                true /* showEntitlementUi */, TEST_CALLER_PKG);
        verify(mTethering).hasTetherableConfiguration();
        verify(mTethering, times(1)).requestLatestTetheringEntitlementResult(eq(TETHERING_WIFI),
                eq(result), eq(true) /* showEntitlementUi */);
        verifyNoMoreInteractions(mTethering);
    }

    @Test
    public void testRegisterTetheringEventCallback() throws Exception {
        mTetheringConnector.registerTetheringEventCallback(mITetheringEventCallback,
                TEST_CALLER_PKG);
        verify(mTethering, times(1)).registerTetheringEventCallback(eq(mITetheringEventCallback));
        verifyNoMoreInteractions(mTethering);
    }

    @Test
    public void testUnregisterTetheringEventCallback() throws Exception {
        mTetheringConnector.unregisterTetheringEventCallback(mITetheringEventCallback,
                TEST_CALLER_PKG);
        verify(mTethering, times(1)).unregisterTetheringEventCallback(
                eq(mITetheringEventCallback));
        verifyNoMoreInteractions(mTethering);
    }

    @Test
    public void testStopAllTethering() throws Exception {
        final TestTetheringResult result = new TestTetheringResult();
        mTetheringConnector.stopAllTethering(TEST_CALLER_PKG, result);
        verify(mTethering).hasTetherableConfiguration();
        verify(mTethering, times(1)).untetherAll();
        verifyNoMoreInteractions(mTethering);
        result.assertHasResult();
    }

    @Test
    public void testIsTetheringSupported() throws Exception {
        final TestTetheringResult result = new TestTetheringResult();
        mTetheringConnector.isTetheringSupported(TEST_CALLER_PKG, result);
        verify(mTethering).hasTetherableConfiguration();
        verifyNoMoreInteractions(mTethering);
        result.assertHasResult();
    }
}
