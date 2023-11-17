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

package com.android.server.connectivity;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.security.LegacyVpnProfileStore;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VpnBlobStoreTest {
    private static final String TEST_ALIAS = "VPN_TEST_ALIAS";
    private static final String TEST_LEGACY_ALIAS = "VPN_TEST_ALIAS_LEGACY";
    private static final byte[] TEST_BLOB = new byte[] {(byte) 10, (byte) 90, (byte) 45, (byte) 12};

    private Set<String> mAddedAlias = new HashSet<String>();
    private VpnBlobStore mVpnBlobStore;

    private void doPut(String alias, byte[] blob) {
        mAddedAlias.add(alias);
        assertTrue(mVpnBlobStore.put(alias, blob));
    }

    private void doRemove(String alias) {
        mAddedAlias.remove(alias);
        assertTrue(mVpnBlobStore.remove(alias));
    }

    @Before
    public void setUp() throws Exception {
        mVpnBlobStore = new VpnBlobStore(InstrumentationRegistry.getContext());
    }

    @After
    public void tearDown() throws Exception {
        for (String alias : mAddedAlias) {
            mVpnBlobStore.remove(alias);
        }
    }

    @Test
    public void testPutAndGet() throws Exception {
        assumeTrue(mVpnBlobStore.get(TEST_ALIAS) == null);
        doPut(TEST_ALIAS, TEST_BLOB);
        assertArrayEquals(TEST_BLOB, mVpnBlobStore.get(TEST_ALIAS));
    }

    @Test
    public void testGetLegacy() throws Exception {
        assumeTrue(mVpnBlobStore.get(TEST_LEGACY_ALIAS) == null);
        LegacyVpnProfileStore.put(TEST_LEGACY_ALIAS, TEST_BLOB);
        assertArrayEquals(TEST_BLOB, mVpnBlobStore.get(TEST_LEGACY_ALIAS));
        LegacyVpnProfileStore.remove(TEST_LEGACY_ALIAS);
    }

    @Test
    public void testRemove() throws Exception {
        assertFalse(mVpnBlobStore.remove(TEST_ALIAS));

        doPut(TEST_ALIAS, TEST_BLOB);
        assertArrayEquals(TEST_BLOB, mVpnBlobStore.get(TEST_ALIAS));

        doRemove(TEST_ALIAS);
        assertNull(mVpnBlobStore.get(TEST_ALIAS));

        // Removing again returns false
        assertFalse(mVpnBlobStore.remove(TEST_ALIAS));
    }

    @Test
    public void testRemoveLegacy() throws Exception {
        assertFalse(mVpnBlobStore.remove(TEST_LEGACY_ALIAS));

        LegacyVpnProfileStore.put(TEST_LEGACY_ALIAS, TEST_BLOB);
        assertArrayEquals(TEST_BLOB, mVpnBlobStore.get(TEST_LEGACY_ALIAS));

        assertTrue(mVpnBlobStore.remove(TEST_LEGACY_ALIAS));
        assertNull(mVpnBlobStore.get(TEST_LEGACY_ALIAS));

        // Removing again returns false
        assertFalse(mVpnBlobStore.remove(TEST_LEGACY_ALIAS));
    }
}
