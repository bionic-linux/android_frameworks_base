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

    private Executor mInlineExecutor;
    private ConnectivityDiagnosticsManager mManager;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
        mService = mock(IConnectivityManager.class);

        mInlineExecutor = x -> x.run();
        mManager = new ConnectivityDiagnosticsManager(mContext, mService);
    }

    private ConnectivityReport createSampleConnectivityReport() {
        final LinkProperties linkProperties = new LinkProperties();
        linkProperties.setInterfaceName(INTERFACE_NAME);

        final NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);

        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(BUNDLE_KEY, BUNDLE_VALUE);

        return new ConnectivityReport(
                new Network(NET_ID), TIMESTAMP, linkProperties, networkCapabilities, bundle);
    }

    private ConnectivityReport createDefaultConnectivityReport() {
        return new ConnectivityReport(
                new Network(0),
                0L,
                new LinkProperties(),
                new NetworkCapabilities(),
                PersistableBundle.EMPTY);
    }

    @Test
    public void testPersistableBundleEquals() {
        assertFalse(
                ConnectivityDiagnosticsManager.persistableBundleEquals(
                        null, PersistableBundle.EMPTY));
        assertFalse(
                ConnectivityDiagnosticsManager.persistableBundleEquals(
                        PersistableBundle.EMPTY, null));
        assertTrue(
                ConnectivityDiagnosticsManager.persistableBundleEquals(
                        PersistableBundle.EMPTY, PersistableBundle.EMPTY));

        final PersistableBundle a = new PersistableBundle();
        a.putString(BUNDLE_KEY, BUNDLE_VALUE);

        final PersistableBundle b = new PersistableBundle();
        b.putString(BUNDLE_KEY, BUNDLE_VALUE);

        final PersistableBundle c = new PersistableBundle();
        c.putString(BUNDLE_KEY, null);

        assertFalse(
                ConnectivityDiagnosticsManager.persistableBundleEquals(PersistableBundle.EMPTY, a));
        assertFalse(
                ConnectivityDiagnosticsManager.persistableBundleEquals(a, PersistableBundle.EMPTY));

        assertTrue(ConnectivityDiagnosticsManager.persistableBundleEquals(a, b));
        assertTrue(ConnectivityDiagnosticsManager.persistableBundleEquals(b, a));

        assertFalse(ConnectivityDiagnosticsManager.persistableBundleEquals(a, c));
        assertFalse(ConnectivityDiagnosticsManager.persistableBundleEquals(c, a));
    }

    @Test
    public void testConnectivityReportEquals() {
        assertEquals(createSampleConnectivityReport(), createSampleConnectivityReport());
        assertEquals(createDefaultConnectivityReport(), createDefaultConnectivityReport());

        final LinkProperties linkProperties = new LinkProperties();
        linkProperties.setInterfaceName(INTERFACE_NAME);

        final NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);

        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(BUNDLE_KEY, BUNDLE_VALUE);

        assertNotEquals(
                createDefaultConnectivityReport(),
                new ConnectivityReport(
                        new Network(NET_ID),
                        0L,
                        new LinkProperties(),
                        new NetworkCapabilities(),
                        PersistableBundle.EMPTY));
        assertNotEquals(
                createDefaultConnectivityReport(),
                new ConnectivityReport(
                        new Network(0),
                        TIMESTAMP,
                        new LinkProperties(),
                        new NetworkCapabilities(),
                        PersistableBundle.EMPTY));
        assertNotEquals(
                createDefaultConnectivityReport(),
                new ConnectivityReport(
                        new Network(0),
                        0L,
                        linkProperties,
                        new NetworkCapabilities(),
                        PersistableBundle.EMPTY));
        assertNotEquals(
                createDefaultConnectivityReport(),
                new ConnectivityReport(
                        new Network(0),
                        TIMESTAMP,
                        new LinkProperties(),
                        networkCapabilities,
                        PersistableBundle.EMPTY));
        assertNotEquals(
                createDefaultConnectivityReport(),
                new ConnectivityReport(
                        new Network(0),
                        TIMESTAMP,
                        new LinkProperties(),
                        new NetworkCapabilities(),
                        bundle));
    }

    @Test
    public void testConnectivityReportParcelUnparcel() {
        assertParcelSane(createSampleConnectivityReport(), 5);
    }

    private DataStallReport createSampleDataStallReport() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(BUNDLE_KEY, BUNDLE_VALUE);
        return new DataStallReport(new Network(NET_ID), TIMESTAMP, DETECTION_METHOD, bundle);
    }

    private DataStallReport createDefaultDataStallReport() {
        return new DataStallReport(new Network(0), 0L, 0, PersistableBundle.EMPTY);
    }

    @Test
    public void testDataStallReportEquals() {
        assertEquals(createSampleDataStallReport(), createSampleDataStallReport());
        assertEquals(createDefaultDataStallReport(), createDefaultDataStallReport());

        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(BUNDLE_KEY, BUNDLE_VALUE);

        assertNotEquals(
                createDefaultDataStallReport(),
                new DataStallReport(new Network(NET_ID), 0L, 0, PersistableBundle.EMPTY));
        assertNotEquals(
                createDefaultDataStallReport(),
                new DataStallReport(new Network(0), TIMESTAMP, 0, PersistableBundle.EMPTY));
        assertNotEquals(
                createDefaultDataStallReport(),
                new DataStallReport(new Network(0), 0L, DETECTION_METHOD, PersistableBundle.EMPTY));
        assertNotEquals(
                createDefaultDataStallReport(), new DataStallReport(new Network(0), 0L, 0, bundle));
    }

    @Test
    public void testDataStallReportParcelUnparcel() {
        assertParcelSane(createSampleDataStallReport(), 4);
    }

    @Test
    public void testConnectivityDiagnosticsCallbackOnConnectivityReport() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectivityDiagnosticsCallback cb =
                new ConnectivityDiagnosticsCallback() {
                    @Override
                    public void onConnectivityReport(ConnectivityReport report) {
                        assertEquals(createSampleConnectivityReport(), report);
                        latch.countDown();
                    }
                };
        cb.setExecutor(mInlineExecutor);

        // The callback will be invoked synchronously since we're using an inline executor. We can
        // immediately check the latch without waiting.
        cb.mBinder.onConnectivityReport(createSampleConnectivityReport());
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testConnectivityDiagnosticsCallbackOnDataStallSuspected() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectivityDiagnosticsCallback cb =
                new ConnectivityDiagnosticsCallback() {
                    @Override
                    public void onDataStallSuspected(DataStallReport report) {
                        assertEquals(createSampleDataStallReport(), report);
                        latch.countDown();
                    }
                };
        cb.setExecutor(mInlineExecutor);

        // The callback will be invoked synchronously since we're using an inline executor. We can
        // immediately check the latch without waiting.
        cb.mBinder.onDataStallSuspected(createSampleDataStallReport());
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testConnectivityDiagnosticsCallbackOnNetworkConnectivityReported()
            throws Exception {
        final Network n = new Network(NET_ID);
        final boolean connectivity = true;
        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectivityDiagnosticsCallback cb =
                new ConnectivityDiagnosticsCallback() {
                    @Override
                    public void onNetworkConnectivityReported(
                            Network network, boolean hasConnectivity) {
                        assertEquals(n, network);
                        assertEquals(connectivity, hasConnectivity);
                        latch.countDown();
                    }
                };
        cb.setExecutor(mInlineExecutor);

        // The callback will be invoked synchronously since we're using an inline executor. We can
        // immediately check the latch without waiting.
        cb.mBinder.onNetworkConnectivityReported(n, connectivity);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testRegisterConnectivityDiagnosticsCallback() throws Exception {
        final NetworkRequest request = new NetworkRequest.Builder().build();
        final ConnectivityDiagnosticsCallback cb = new ConnectivityDiagnosticsCallback() {};
        mManager.registerConnectivityDiagnosticsCallback(request, mInlineExecutor, cb);

        assertNotNull(cb.mExecutor);
        verify(mService).registerConnectivityDiagnosticsCallback(eq(cb.mBinder), eq(request));
    }

    @Test
    public void testUnregisterConnectivityDiagnosticsCallback() throws Exception {
        final ConnectivityDiagnosticsCallback cb = new ConnectivityDiagnosticsCallback() {};
        cb.setExecutor(mInlineExecutor);
        mManager.unregisterConnectivityDiagnosticsCallback(cb);

        assertNull(cb.mExecutor);
        verify(mService).unregisterConnectivityDiagnosticsCallback(eq(cb.mBinder));
    }
}
