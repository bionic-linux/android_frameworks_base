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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConnectivityBlobStoreTest {
    private static final String DATABASE_FILENAME = "ConnectivityBlobStore.db";
    private ConnectivityBlobStore mConnectivityBlobStore;

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
}
