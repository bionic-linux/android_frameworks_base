/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.NetworkAttributes;
import android.os.RemoteException;
import android.util.Log;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.Inet4Address;
import java.net.UnknownHostException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class IpMemoryStoreTest {
    private static final String TAG = IpMemoryStoreTest.class.getSimpleName();
    private static final String TEST_CLIENT_ID = "testClientId";
    private static final String TEST_DATA_NAME = "testData";

    @Mock
    Context mMockContext;
    @Mock
    NetworkStackClient mNetworkStackClient;
    @Mock
    IIpMemoryStore mMockService;
    IpMemoryStore mStore;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    void startIpMemoryStore() {
        doAnswer(invocation -> {
            ((IIpMemoryStoreCallbacks) invocation.getArgument(0))
                    .onIpMemoryStoreFetched(mMockService);
            return null;
        }).when(mNetworkStackClient).fetchIpMemoryStore(any());
        mStore = new IpMemoryStore(mMockContext) {
            @Override
            protected NetworkStackClient getNetworkStackClient() {
                return mNetworkStackClient;
            }
        };
    }

    @Test
    public void testNetworkAttributes() throws UnknownHostException, RemoteException {
        startIpMemoryStore();

        final NetworkAttributes.Builder na = new NetworkAttributes.Builder();
        na.setAssignedV4Address((Inet4Address) Inet4Address.getByName("1.2.3.4"));
        na.setGroupHint("hint1");
        na.setMtu(219);
        final String l2Key = "fakeKey";
        NetworkAttributes attributes = na.build();

        mStore.storeNetworkAttributes(l2Key, attributes, null);
        verify(mMockService, times(1)).storeNetworkAttributes(any(), any(), any());

        mStore.retrieveNetworkAttributes(l2Key, null);
        verify(mMockService, times(1)).retrieveNetworkAttributes(any(), any());
    }

    @Test
    public void testPrivateData() throws RemoteException {
        startIpMemoryStore();

        final Blob b = new Blob();
        b.data = new byte[] { -3, 6, 8, -9, 12, -128, 0, 89, 112, 91, -34 };
        final String l2Key = "fakeKey";

        mStore.storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);
        verify(mMockService, times(1)).storeBlob(any(), any(), any(), any(), any());

        mStore.retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME + "2", null);
        verify(mMockService, times(1)).retrieveBlob(any(), any(), any(), any());
    }

    @Test
    public void testFindL2Key() throws UnknownHostException, RemoteException {
        startIpMemoryStore();

        final NetworkAttributes.Builder na = new NetworkAttributes.Builder();
        na.setAssignedV4Address((Inet4Address) Inet4Address.getByName("1.2.3.4"));
        na.setGroupHint("hint1");
        na.setMtu(219);
        final String l2Key = "fakeKey";
        NetworkAttributes attributes = na.build();

        mStore.findL2Key(attributes, null);
        verify(mMockService, times(1)).findL2Key(any(), any());
    }

    @Test
    public void testIsSameNetwork() throws UnknownHostException, RemoteException {
        startIpMemoryStore();

        final String l2Key1 = "fakeKey1";
        final String l2Key2 = "fakeKey2";

        mStore.isSameNetwork(l2Key1, l2Key2, null);
        verify(mMockService, times(1)).isSameNetwork(any(), any(), any());
    }

    @Test
    public void testEnqueuedIpMsRequests()
            throws UnknownHostException, InterruptedException, RemoteException {
        doAnswer(invocation -> {
            new Thread(() -> {
                try {
                    Thread.sleep(800);
                    ((IIpMemoryStoreCallbacks) invocation.getArgument(0))
                             .onIpMemoryStoreFetched(mMockService);
                    Log.d(TAG, "ipms service is ready");
                } catch (InterruptedException e) {
                    fail("InterruptedException: " + e);
                } catch (RemoteException e) {
                    fail("RemoteException: " + e);
                }
            }).start();
            return null;
        }).when(mNetworkStackClient).fetchIpMemoryStore(any());
        mStore = new IpMemoryStore(mMockContext) {
            @Override
            protected NetworkStackClient getNetworkStackClient() {
                return mNetworkStackClient;
            }
        };

        final Blob b = new Blob();
        b.data = new byte[] { -3, 6, 8, -9, 12, -128, 0, 89, 112, 91, -34 };
        final NetworkAttributes.Builder na = new NetworkAttributes.Builder();
        na.setAssignedV4Address((Inet4Address) Inet4Address.getByName("1.2.3.4"));
        na.setGroupHint("hint1");
        na.setMtu(219);
        final String l2Key = "fakeKey";
        NetworkAttributes attributes = na.build();

        // enqueue multiple ipms requests
        Log.d(TAG, "enqueuing multiple ipms requests");

        mStore.storeNetworkAttributes(l2Key, attributes, null);
        mStore.retrieveNetworkAttributes(l2Key, null);
        mStore.storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);
        mStore.retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME + "2", null);

        // wait for ipms service get ready
        Thread.sleep(1000);

        Log.d(TAG, "verify the calling order");
        InOrder inOrder = inOrder(mMockService);

        inOrder.verify(mMockService).storeNetworkAttributes(any(), any(), any());
        inOrder.verify(mMockService).retrieveNetworkAttributes(any(), any());
        inOrder.verify(mMockService).storeBlob(any(), any(), any(), any(), any());
        inOrder.verify(mMockService).retrieveBlob(any(), any(), any(), any());
    }

    @Test
    public void testEnqueuedIpMsRequestsWithException()
            throws UnknownHostException, InterruptedException, RemoteException {
        startIpMemoryStore();
        doThrow(RemoteException.class).when(mMockService).retrieveNetworkAttributes(any(), any());

        final Blob b = new Blob();
        b.data = new byte[] { -3, 6, 8, -9, 12, -128, 0, 89, 112, 91, -34 };
        final NetworkAttributes.Builder na = new NetworkAttributes.Builder();
        na.setAssignedV4Address((Inet4Address) Inet4Address.getByName("1.2.3.4"));
        na.setGroupHint("hint1");
        na.setMtu(219);
        final String l2Key = "fakeKey";
        NetworkAttributes attributes = na.build();

        // enqueue multiple ipms requests
        mStore.storeNetworkAttributes(l2Key, attributes, null);
        mStore.retrieveNetworkAttributes(l2Key, null);
        mStore.storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);
        mStore.retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME + "2", null);

        // verify the rest of the queue is still processed in order
        InOrder inOrder = inOrder(mMockService);

        inOrder.verify(mMockService).storeNetworkAttributes(any(), any(), any());
        inOrder.verify(mMockService).storeBlob(any(), any(), any(), any(), any());
        inOrder.verify(mMockService).retrieveBlob(any(), any(), any(), any());

        verify(mMockService, times(1)).storeNetworkAttributes(any(), any(), any());
        verify(mMockService, times(1)).storeBlob(any(), any(), any(), any(), any());
        verify(mMockService, times(1)).retrieveBlob(any(), any(), any(), any());
    }
}
