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


package com.android.server.connectivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.IQosCallback;
import android.net.QosFilter;
import android.os.IBinder;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class QosCallbackAgentCollectionTest {

    int mCallbackId;

    @Mock QosCallbackTracker mTracker;
    QosCallbackAgentConnectionCollection mConnections;

    @Mock NetworkAgentInfo mNetworkAgentInfoA;
    @Mock NetworkAgentInfo mNetworkAgentInfoB;

    QosCallbackAgentConnection mConnectionA1;
    QosCallbackAgentConnection mConnectionA2;
    QosCallbackAgentConnection mConnectionB1;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mCallbackId = 1;
        mConnections =
                new QosCallbackAgentConnectionCollection();

        //Add two mConnections with network agent info A
        mConnectionA1 = createAgentConnection(mNetworkAgentInfoA);
        mConnectionA2 = createAgentConnection(mNetworkAgentInfoA);

        //Add one connection with network agent info A
        mConnectionB1 = createAgentConnection(mNetworkAgentInfoB);
    }

    @Test
    public void testGets() {
        assertEquals(mConnectionA2, mConnections.get(mConnectionA2.getBinder()));
        assertEquals(mConnectionA2, mConnections.get(mConnectionA2.getAgentCallbackId()));
        assertEquals(2, mConnections.get(mNetworkAgentInfoA).length);
        assertEquals(1, mConnections.get(mNetworkAgentInfoB).length);

        Supplier<Stream<QosCallbackAgentConnection>> streamA =
                () -> Arrays.stream(mConnections.get(mNetworkAgentInfoA));
        assertTrue(streamA.get().anyMatch(ac -> ac.equals(mConnectionA1)));
        assertTrue(streamA.get().anyMatch(ac -> ac.equals(mConnectionA2)));
    }

    @Test
    public void testSimpleRemoves() {
        mConnections.remove(mConnectionA1);
        assertNull(mConnections.get(mConnectionA1.getBinder()));
        assertNull(mConnections.get(mConnectionA1.getAgentCallbackId()));
        assertEquals(1, mConnections.get(mNetworkAgentInfoA).length);
        assertEquals(1, mConnections.get(mNetworkAgentInfoB).length);

        mConnections.remove(mConnectionA2);
        assertEquals(0, mConnections.get(mNetworkAgentInfoA).length);
    }

    private QosCallbackAgentConnection
            createAgentConnection(NetworkAgentInfo networkAgentInfo) {
        IBinder binder = mock(IBinder.class);
        QosFilter filter = mock(QosFilter.class);
        IQosCallback callback = mock(IQosCallback.class);
        QosCallbackValidator validator = mock(QosCallbackValidator.class);
        int uid = 1001;
        when(callback.asBinder()).thenReturn(binder);
        QosCallbackAgentConnection ac =
                new QosCallbackAgentConnection(mTracker, mCallbackId++, networkAgentInfo, callback,
                filter, validator, uid);
        mConnections.put(ac);
        return ac;
    }
}
