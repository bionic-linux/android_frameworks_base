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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.NetworkAttributes;
import android.net.ipmemorystore.NetworkAttributesParcelable;
import android.os.RemoteException;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
    private static final String TEST_OTHER_DATA_NAME = TEST_DATA_NAME + "Other";
    private static final byte[] TEST_BLOB_DATA = new byte[] { -3, 6, 8, -9, 12,
            -128, 0, 89, 112, 91, -34 };
    private static final NetworkAttributes TEST_NETWORK_ATTRIBUTES = buildTestNetworkAttributes(
            "1.2.3.4", "hint", 219);

    @Mock
    Context mMockContext;
    @Mock
    NetworkStackClient mNetworkStackClient;
    @Mock
    IIpMemoryStore mMockService;
    IpMemoryStore mStore;

    @Captor
    ArgumentCaptor<IIpMemoryStoreCallbacks> mCbCaptor;
    @Captor
    ArgumentCaptor<NetworkAttributesParcelable> mNapCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void startIpMemoryStore() {
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

    private static NetworkAttributes buildTestNetworkAttributes(String address,
            String hint, int mtu) {
        final NetworkAttributes.Builder na = new NetworkAttributes.Builder();
        na.setGroupHint(hint);
        na.setMtu(mtu);
        try {
            na.setAssignedV4Address((Inet4Address) Inet4Address.getByName(address));
        } catch (UnknownHostException e) {
            fail("UnknownHostException: " + e);
        }
        return na.build();
    }

    @Test
    public void testNetworkAttributes()
            throws UnknownHostException, RemoteException, Exception {
        startIpMemoryStore();
        final String l2Key = "fakeKey";

        mStore.storeNetworkAttributes(l2Key, TEST_NETWORK_ATTRIBUTES, null);
        verify(mMockService, times(1)).storeNetworkAttributes(eq(l2Key),
                mNapCaptor.capture(), any());
        assertEquals(TEST_NETWORK_ATTRIBUTES, new NetworkAttributes(mNapCaptor.getValue()));

        mStore.retrieveNetworkAttributes(l2Key, null);
        verify(mMockService, times(1)).retrieveNetworkAttributes(eq(l2Key), any());
    }

    @Test
    public void testPrivateData() throws RemoteException {
        startIpMemoryStore();
        final Blob b = new Blob();
        b.data = TEST_BLOB_DATA;
        final String l2Key = "fakeKey";

        mStore.storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);
        verify(mMockService, times(1)).storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);

        mStore.retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_OTHER_DATA_NAME, null);
        verify(mMockService, times(1)).retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_OTHER_DATA_NAME,
                null);
    }

    @Test
    public void testFindL2Key()
            throws UnknownHostException, RemoteException, Exception {
        startIpMemoryStore();
        final String l2Key = "fakeKey";

        mStore.findL2Key(TEST_NETWORK_ATTRIBUTES, null);
        verify(mMockService, times(1)).findL2Key(mNapCaptor.capture(), any());
        assertEquals(TEST_NETWORK_ATTRIBUTES, new NetworkAttributes(mNapCaptor.getValue()));
    }

    @Test
    public void testIsSameNetwork() throws UnknownHostException, RemoteException {
        startIpMemoryStore();
        final String l2Key1 = "fakeKey1";
        final String l2Key2 = "fakeKey2";

        mStore.isSameNetwork(l2Key1, l2Key2, null);
        verify(mMockService, times(1)).isSameNetwork(l2Key1, l2Key2, null);
    }

    @Test
    public void testEnqueuedIpMsRequests()
            throws UnknownHostException, InterruptedException, RemoteException, Exception {
        doNothing().when(mNetworkStackClient).fetchIpMemoryStore(mCbCaptor.capture());
        mStore = new IpMemoryStore(mMockContext) {
            @Override
            protected NetworkStackClient getNetworkStackClient() {
                return mNetworkStackClient;
            }
        };

        final Blob b = new Blob();
        b.data = TEST_BLOB_DATA;
        final String l2Key = "fakeKey";

        // enqueue multiple ipms requests
        mStore.storeNetworkAttributes(l2Key, TEST_NETWORK_ATTRIBUTES, null);
        mStore.retrieveNetworkAttributes(l2Key, null);
        mStore.storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);
        mStore.retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_OTHER_DATA_NAME, null);

        // get ipms service ready
        mCbCaptor.getValue().onIpMemoryStoreFetched(mMockService);

        InOrder inOrder = inOrder(mMockService);

        inOrder.verify(mMockService).storeNetworkAttributes(eq(l2Key), mNapCaptor.capture(), any());
        inOrder.verify(mMockService).retrieveNetworkAttributes(l2Key, null);
        inOrder.verify(mMockService).storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME,
                b, null);
        inOrder.verify(mMockService).retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_OTHER_DATA_NAME,
                null);
        assertEquals(TEST_NETWORK_ATTRIBUTES, new NetworkAttributes(mNapCaptor.getValue()));
    }

    @Test
    public void testEnqueuedIpMsRequestsWithException()
            throws UnknownHostException, InterruptedException, RemoteException, Exception {
        startIpMemoryStore();
        doThrow(RemoteException.class).when(mMockService).retrieveNetworkAttributes(any(), any());

        final Blob b = new Blob();
        b.data = TEST_BLOB_DATA;
        final String l2Key = "fakeKey";

        // enqueue multiple ipms requests
        mStore.storeNetworkAttributes(l2Key, TEST_NETWORK_ATTRIBUTES, null);
        mStore.retrieveNetworkAttributes(l2Key, null);
        mStore.storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);
        mStore.retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_OTHER_DATA_NAME, null);

        // verify the rest of the queue is still processed in order
        InOrder inOrder = inOrder(mMockService);

        inOrder.verify(mMockService).storeNetworkAttributes(eq(l2Key), mNapCaptor.capture(), any());
        inOrder.verify(mMockService).storeBlob(l2Key, TEST_CLIENT_ID, TEST_DATA_NAME, b, null);
        inOrder.verify(mMockService).retrieveBlob(l2Key, TEST_CLIENT_ID, TEST_OTHER_DATA_NAME,
                null);
        assertEquals(TEST_NETWORK_ATTRIBUTES, new NetworkAttributes(mNapCaptor.getValue()));
    }
}
