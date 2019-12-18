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

package android.net;

import static android.net.ConnectivityDiagnosticsManager.ConnectivityDiagnosticsCallback;
import static android.net.ConnectivityDiagnosticsManager.ConnectivityReport;
import static android.net.ConnectivityDiagnosticsManager.DataStallReport;

import static com.android.testutils.ParcelUtilsKt.assertParcelSane;
import static com.android.testutils.ParcelUtilsKt.assertParcelingIsLossless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.PersistableBundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class ConnectivityDiagnosticsManagerTest {
    private static final int NET_ID = 1;
    private static final int DETECTION_METHOD = 2;
    private static final long TIMESTAMP = 10L;
    private static final long TIMEOUT_MILLIS = 200L;
    private static final String INTERFACE_NAME = "interface";
    private static final String BUNDLE_KEY = "key";
    private static final String BUNDLE_VALUE = "value";

    @Mock private Context mContext;
    @Mock private IConnectivityManager mService;

    private Executor mExecutor;
    private ConnectivityDiagnosticsManager mManager;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
        mService = mock(IConnectivityManager.class);

        mExecutor = Executors.newSingleThreadExecutor();
        mManager = new ConnectivityDiagnosticsManager(mContext, mService);
    }

    private ConnectivityReport getSampleConnectivityReport() {
        final LinkProperties linkProperties = new LinkProperties();
        linkProperties.setInterfaceName(INTERFACE_NAME);

        final NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);

        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(BUNDLE_KEY, BUNDLE_VALUE);

        return new ConnectivityReport(
                new Network(NET_ID), TIMESTAMP, linkProperties, networkCapabilities, bundle);
    }

    @Test
    public void testPersistableBundleEquals() {
        assertFalse(ConnectivityDiagnosticsManager.equals(null, PersistableBundle.EMPTY));
        assertTrue(
                ConnectivityDiagnosticsManager.equals(
                        PersistableBundle.EMPTY, PersistableBundle.EMPTY));

        final PersistableBundle a = new PersistableBundle();
        a.putString(BUNDLE_KEY, BUNDLE_VALUE);

        final PersistableBundle b = new PersistableBundle();
        b.putString(BUNDLE_KEY, BUNDLE_VALUE);

        final PersistableBundle c = new PersistableBundle();
        c.putString(BUNDLE_KEY, null);

        assertFalse(ConnectivityDiagnosticsManager.equals(PersistableBundle.EMPTY, a));
        assertTrue(ConnectivityDiagnosticsManager.equals(a, b));
        assertFalse(ConnectivityDiagnosticsManager.equals(a, c));
    }

    @Test
    public void testConnectivityReportEquals() {
        assertEquals(getSampleConnectivityReport(), getSampleConnectivityReport());
        assertNotEquals(
                getSampleConnectivityReport(),
                new ConnectivityReport(
                        new Network(0),
                        0L,
                        new LinkProperties(),
                        new NetworkCapabilities(),
                        new PersistableBundle()));
    }

    @Test
    public void testConnectivityReportParcelUnparcel() {
        assertParcelingIsLossless(getSampleConnectivityReport());

        assertParcelSane(getSampleConnectivityReport(), 5);
    }

    private DataStallReport getSampleDataStallReport() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(BUNDLE_KEY, BUNDLE_VALUE);
        return new DataStallReport(new Network(NET_ID), TIMESTAMP, DETECTION_METHOD, bundle);
    }

    @Test
    public void testDataStallReportEquals() {
        assertEquals(getSampleDataStallReport(), getSampleDataStallReport());
        assertNotEquals(
                getSampleDataStallReport(),
                new DataStallReport(new Network(0), 0L, 0, new PersistableBundle()));
    }

    @Test
    public void testDataStallReportParcelUnparcel() {
        assertParcelingIsLossless(getSampleDataStallReport());

        assertParcelSane(getSampleDataStallReport(), 4);
    }

    @Test
    public void testConnectivityDiagnosticsCallbackOnConnectivityReport() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectivityDiagnosticsCallback cb =
                new ConnectivityDiagnosticsCallback() {
                    @Override
                    public void onConnectivityReport(ConnectivityReport report) {
                        latch.countDown();
                    }
                };
        cb.setExecutor(mExecutor);

        cb.mBinder.onConnectivityReport(getSampleConnectivityReport());
        latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testConnectivityDiagnosticsCallbackOnDataStallSuspected() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectivityDiagnosticsCallback cb =
                new ConnectivityDiagnosticsCallback() {
                    @Override
                    public void onDataStallSuspected(DataStallReport report) {
                        latch.countDown();
                    }
                };
        cb.setExecutor(mExecutor);

        cb.mBinder.onDataStallSuspected(getSampleDataStallReport());
        latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testConnectivityDiagnosticsCallbackOnNetworkConnectivityReported()
            throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectivityDiagnosticsCallback cb =
                new ConnectivityDiagnosticsCallback() {
                    @Override
                    public void onNetworkConnectivityReported(
                            Network network, boolean hasConnectivity) {
                        latch.countDown();
                    }
                };
        cb.setExecutor(mExecutor);

        cb.mBinder.onNetworkConnectivityReported(new Network(NET_ID), true);
        latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testRegisterConnectivityDiagnosticsCallback() throws Exception {
        final NetworkRequest request = new NetworkRequest.Builder().build();
        final ConnectivityDiagnosticsCallback cb = new ConnectivityDiagnosticsCallback() {};
        mManager.registerConnectivityDiagnosticsCallback(request, mExecutor, cb);

        assertNotNull(cb.mExecutor);
        verify(mService).registerConnectivityDiagnosticsCallback(eq(cb.mBinder), eq(request));
    }

    @Test
    public void testUnregisterConnectivityDiagnosticsCallback() throws Exception {
        final ConnectivityDiagnosticsCallback cb = new ConnectivityDiagnosticsCallback() {};
        cb.setExecutor(mExecutor);
        mManager.unregisterConnectivityDiagnosticsCallback(cb);

        assertNull(cb.mExecutor);
        verify(mService).unregisterConnectivityDiagnosticsCallback(eq(cb.mBinder));
    }
}
