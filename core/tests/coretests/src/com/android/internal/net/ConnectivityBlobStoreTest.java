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

    @Test
    public void testRemove() throws Exception {
        assertNull(mConnectivityBlobStore.get(TEST_NAME));
        assertFalse(mConnectivityBlobStore.remove(TEST_NAME));

        assertTrue(mConnectivityBlobStore.put(TEST_NAME, TEST_BLOB));
        assertArrayEquals(TEST_BLOB, mConnectivityBlobStore.get(TEST_NAME));

        assertTrue(mConnectivityBlobStore.remove(TEST_NAME));
        assertNull(mConnectivityBlobStore.get(TEST_NAME));

        // Removing again returns false
        assertFalse(mConnectivityBlobStore.remove(TEST_NAME));
    }

    @Test
    public void testMultipleNames() throws Exception {
        final String name1 = TEST_NAME + "1";
        final String name2 = TEST_NAME + "2";
        assertNull(mConnectivityBlobStore.get(name1));
        assertNull(mConnectivityBlobStore.get(name2));
        assertFalse(mConnectivityBlobStore.remove(name1));
        assertFalse(mConnectivityBlobStore.remove(name2));

        assertTrue(mConnectivityBlobStore.put(name1, TEST_BLOB));
        assertTrue(mConnectivityBlobStore.put(name2, TEST_BLOB));
        assertArrayEquals(TEST_BLOB, mConnectivityBlobStore.get(name1));
        assertArrayEquals(TEST_BLOB, mConnectivityBlobStore.get(name2));

        // Replace the blob for name1 only.
        final byte[] newBlob = new byte[] {(byte) 16, (byte) 21};
        assertTrue(mConnectivityBlobStore.put(name1, newBlob));
        assertArrayEquals(newBlob, mConnectivityBlobStore.get(name1));

        assertTrue(mConnectivityBlobStore.remove(name1));
        assertNull(mConnectivityBlobStore.get(name1));
        assertArrayEquals(TEST_BLOB, mConnectivityBlobStore.get(name2));

        assertFalse(mConnectivityBlobStore.remove(name1));
    }

    @Test
    public void testList() throws Exception {
        final String[] unsortedNames = new String[] {
                TEST_NAME + "1",
                TEST_NAME + "2",
                TEST_NAME + "0",
                "NON_MATCHING_PREFIX",
                "MATCHING_SUFFIX_" + TEST_NAME
        };
        // Expected to match and discard the prefix and be in increasing sorted order.
        final String[] expected = new String[] {
                "0",
                "1",
                "2"
        };

        for (int i = 0; i < unsortedNames.length; i++) {
            assertTrue(mConnectivityBlobStore.put(unsortedNames[i], TEST_BLOB));
        }
        final String[] actual = mConnectivityBlobStore.list(TEST_NAME /* prefix */);
        assertArrayEquals(expected, actual);
    }
}
