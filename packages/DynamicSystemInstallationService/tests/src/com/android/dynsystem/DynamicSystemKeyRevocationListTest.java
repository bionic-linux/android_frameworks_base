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

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.junit.Assert;
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

    private static final String BLACKLIST_JSON_STRING = "{\"entries\": ["
                + "{\"public_key\": \"00fa2c6637c399afa893fe83d85f3569998707d5\","
                + "\"status\": \"REVOKED\", \"reason\": \"Key revocation test key\"},"
                + "{\"public_key\": \"key2\", \"status\": \"REVOKED\"}]}";

    @Test
    @SmallTest
    public void testFromJsonString() {
        DynamicSystemKeyRevocationList blacklist = null;
        try {
            blacklist = DynamicSystemKeyRevocationList.fromJsonString(BLACKLIST_JSON_STRING);
        } catch (JSONException e) {
            Assert.fail("Unexpected " + e.toString());
        }
        Assert.assertNotNull(blacklist);
        Assert.assertEquals(blacklist.mEntries.size(), 2);
    }

    @Test
    @SmallTest
    public void testFromUrl() throws Exception {
        URLConnection mockConnection = mock(URLConnection.class);
        doReturn(new ByteArrayInputStream(BLACKLIST_JSON_STRING.getBytes()))
                .when(mockConnection).getInputStream();
        URL mockUrl = new URL("http", "foo.bar", 80, "baz",
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) {
                        return mockConnection;
                    }
                });
        URL mockBadUrl = new URL("http", "foo.bar", 80, "baz",
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) throws IOException {
                        throw new IOException();
                    }
                });
        DynamicSystemKeyRevocationList blacklist = null;
        try {
            blacklist = DynamicSystemKeyRevocationList.fromUrl(mockUrl);
        } catch (IOException | JSONException e) {
            Assert.fail("Unexpected " + e.toString());
        }
        Assert.assertNotNull(blacklist);
        Assert.assertEquals(blacklist.mEntries.size(), 2);

        blacklist = null;
        try {
            blacklist = DynamicSystemKeyRevocationList.fromUrl(mockBadUrl);
            // Up should throw, down should be unreachable
            Assert.fail("Reached unreachable part");
        } catch (IOException e) {
            // Do nothing
        } catch (JSONException e) {
            Assert.fail("Unexpected " + e.toString());
        }
        Assert.assertNull(blacklist);
    }

    @Test
    @SmallTest
    public void testIsRevoked() {
        DynamicSystemKeyRevocationList blacklist = new DynamicSystemKeyRevocationList();
        blacklist.mEntries.add(new DynamicSystemKeyRevocationList.RevocationEntry(
                "key1", "REVOKED", "reason for key1"));

        Assert.assertTrue(blacklist.isRevoked("key1"));
        Assert.assertFalse(blacklist.isRevoked("key2"));
        Assert.assertEquals(blacklist.getRevokeReasonForKey("key1"), "reason for key1");
    }
}
