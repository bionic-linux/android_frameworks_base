/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConnectivityBlobStoreTest {
    private static final String DATABASE_FILENAME = "ConnectivityBlobStore.db";
    private ConnectivityBlobStore mConnectivityBlobStore;
    private static final String TEST_NAME = "TEST_NAME";
    private static final byte[] TEST_BLOB = new byte[] {(byte) 10, (byte) 90, (byte) 45, (byte) 12};

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getContext();
        final File file = context.getDatabasePath(DATABASE_FILENAME);
        assertFalse(file.exists());
        // Database file is created
        mConnectivityBlobStore = new ConnectivityBlobStore(file);
        assertTrue(file.exists());
    }

    @After
    public void tearDown() throws Exception {
        final Context context = InstrumentationRegistry.getContext();
        assertTrue(context.deleteDatabase(DATABASE_FILENAME));
    }

    @Test
    public void testPutAndGet() throws Exception {
        assertNull(mConnectivityBlobStore.get(TEST_NAME));

        assertTrue(mConnectivityBlobStore.put(TEST_NAME, TEST_BLOB));
        assertArrayEquals(TEST_BLOB, mConnectivityBlobStore.get(TEST_NAME));

        // Test replacement
        final byte[] newBlob = new byte[] {(byte) 15, (byte) 20};
        assertTrue(mConnectivityBlobStore.put(TEST_NAME, newBlob));
        assertArrayEquals(newBlob, mConnectivityBlobStore.get(TEST_NAME));
    }
}
