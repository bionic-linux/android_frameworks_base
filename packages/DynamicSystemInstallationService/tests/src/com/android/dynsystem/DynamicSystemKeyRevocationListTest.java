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

package com.android.dynsystem;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import android.content.Context;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A test for DynamicSystemKeyRevocationList.java
 */
@RunWith(AndroidJUnit4.class)
public class DynamicSystemKeyRevocationListTest {

    private static final String TAG = "DynamicSystemKeyRevocationListTest";

    private static Context sContext;

    private static String sBlacklistJsonString;

    @BeforeClass
    public static void setUpClass() throws Exception {
        sContext = InstrumentationRegistry.getInstrumentation().getContext();
        sBlacklistJsonString =
                sContext.getString(com.android.dynsystem.tests.R.string.blacklist_json_string);
    }

    @Test
    @SmallTest
    public void testFromJsonString() throws JSONException {
        DynamicSystemKeyRevocationList blacklist;
        blacklist = DynamicSystemKeyRevocationList.fromJsonString(sBlacklistJsonString);
        Assert.assertNotNull(blacklist);
        Assert.assertEquals(blacklist.mEntries.size(), 2);
        blacklist = DynamicSystemKeyRevocationList.fromJsonString("{}");
        Assert.assertNotNull(blacklist);
        Assert.assertEquals(blacklist.mEntries.size(), 0);
    }

    @Test
    @SmallTest
    public void testFromUrl() throws IOException, JSONException {
        URLConnection mockConnection = mock(URLConnection.class);
        doReturn(new ByteArrayInputStream(sBlacklistJsonString.getBytes()))
                .when(mockConnection).getInputStream();
        URL mockUrl = new URL(
                "http",     // protocol
                "foo.bar",  // host
                80,         // port
                "baz",      // file
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) {
                        return mockConnection;
                    }
                });
        URL mockBadUrl = new URL(
                "http",     // protocol
                "foo.bar",  // host
                80,         // port
                "baz",      // file
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) throws IOException {
                        throw new IOException();
                    }
                });

        DynamicSystemKeyRevocationList blacklist = DynamicSystemKeyRevocationList.fromUrl(mockUrl);
        Assert.assertNotNull(blacklist);
        Assert.assertEquals(blacklist.mEntries.size(), 2);

        blacklist = null;
        try {
            blacklist = DynamicSystemKeyRevocationList.fromUrl(mockBadUrl);
            // Up should throw, down should be unreachable
            Assert.fail("Expected IOException not thrown");
        } catch (IOException e) {
            // This is expected, do nothing
        }
        Assert.assertNull(blacklist);
    }

    @Test
    @SmallTest
    public void testIsRevoked() {
        DynamicSystemKeyRevocationList blacklist = new DynamicSystemKeyRevocationList();
        blacklist.mEntries.add(new DynamicSystemKeyRevocationList.RevocationEntry(
                "key1", "REVOKED", "reason for key1"));

        DynamicSystemKeyRevocationList.RevocationEntry entry =
                blacklist.getRevocationEntryForKey("key1");
        Assert.assertNotNull(entry);
        Assert.assertEquals(entry.mReason, "reason for key1");

        entry = blacklist.getRevocationEntryForKey("key2");
        Assert.assertNull(entry);

        Assert.assertTrue(blacklist.isRevoked("key1"));
        Assert.assertFalse(blacklist.isRevoked("key2"));
    }
}
