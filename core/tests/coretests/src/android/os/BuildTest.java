/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.os;

import androidx.test.filters.SmallTest;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Provides test cases for android.os.Build and, in turn, many of the
 * system properties set by the build system.
 */
public class BuildTest extends TestCase {

    private static final String TAG = "BuildTest";

    /**
     * Asserts that a String is non-null and non-empty.  If it is not,
     * an AssertionFailedError is thrown with the given message.
     */
    private static void assertNotEmpty(String message, String string) {
        //Log.i(TAG, "" + message + ": " + string);
        assertNotNull(message, string);
        assertFalse(message, string.equals(""));
    }

    /**
     * Asserts that a String is non-null and non-empty.  If it is not,
     * an AssertionFailedError is thrown.
     */
    private static void assertNotEmpty(String string) {
        assertNotEmpty(null, string);
    }

    /**
     * Asserts that all android.os.Build fields are non-empty and/or in a valid range.
     */
    @SmallTest
    public void testBuildFields() throws Exception {
        assertNotEmpty("ID", Build.ID);
        assertNotEmpty("DISPLAY", Build.DISPLAY);
        assertNotEmpty("PRODUCT", Build.PRODUCT);
        assertNotEmpty("DEVICE", Build.DEVICE);
        assertNotEmpty("BOARD", Build.BOARD);
        assertNotEmpty("BRAND", Build.BRAND);
        assertNotEmpty("MODEL", Build.MODEL);
        assertNotEmpty("VERSION.INCREMENTAL", Build.VERSION.INCREMENTAL);
        assertNotEmpty("VERSION.RELEASE", Build.VERSION.RELEASE_OR_CODENAME);
        assertNotEmpty("TYPE", Build.TYPE);
        Assert.assertNotNull("TAGS", Build.TAGS); // TAGS is allowed to be empty.
        assertNotEmpty("FINGERPRINT", Build.FINGERPRINT);
        Assert.assertTrue("TIME", Build.TIME > 0);
        assertNotEmpty("USER", Build.USER);
        assertNotEmpty("HOST", Build.HOST);
        assertEquals(0, Build.DDR_SIZE);

        // TODO: if any of the android.os.Build fields have additional constraints
        // (e.g., must be a C identifier, must be a valid filename, must not contain any spaces)
        // add tests for them.
    }

    @SmallTest
    public void testGetDdrSizeInBytes() {
        assertEquals(1_000L, Build.getDdrSizeInBytes("1KB"));
        assertEquals(10_000L, Build.getDdrSizeInBytes("10KB"));
        assertEquals(100_000L, Build.getDdrSizeInBytes("100KB"));
        assertEquals(1_000_000L, Build.getDdrSizeInBytes("1000KB"));
        assertEquals(200_000_000L, Build.getDdrSizeInBytes("200MB"));
        assertEquals(1_000_000_000L, Build.getDdrSizeInBytes("1000MB"));
        assertEquals(600_000_000_000L, Build.getDdrSizeInBytes("600GB"));
        assertEquals(999_000_000_000L, Build.getDdrSizeInBytes("999GB"));
        assertEquals(9_000_000_000_000L, Build.getDdrSizeInBytes("9000GB"));
        assertEquals(9_999_000_000_000L, Build.getDdrSizeInBytes("9999GB"));
    }

    @SmallTest
    public void testGetDdrSizeInBytes_invalidArguments() {
        assertEquals(0L, Build.getDdrSizeInBytes(""));
        assertEquals(0L, Build.getDdrSizeInBytes("KB"));
        assertEquals(0L, Build.getDdrSizeInBytes("10K"));
        assertEquals(0L, Build.getDdrSizeInBytes("070MB"));
        assertEquals(0L, Build.getDdrSizeInBytes("0MB"));
        assertEquals(0L, Build.getDdrSizeInBytes("10K"));
        assertEquals(0L, Build.getDdrSizeInBytes("70000KB"));
        assertEquals(0L, Build.getDdrSizeInBytes("Invalid"));
    }
}
