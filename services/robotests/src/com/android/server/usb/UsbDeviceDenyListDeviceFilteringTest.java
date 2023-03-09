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

package com.android.server.usb;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link UsbDeviceDenyList}'s device filtering capabilities
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
public class UsbDeviceDenyListDeviceFilteringTest {

    private final String[] mFilterValues;
    private final int[] mValueToTest;
    private final boolean mShouldDeny;

    private UsbDeviceDenyList mDenyList;

    public UsbDeviceDenyListDeviceFilteringTest(
            List<String> filterValues, List<Integer> valueToTest, boolean shouldDeny) {
        this.mFilterValues = filterValues.toArray(value -> new String[filterValues.size()]);
        this.mValueToTest = valueToTest.stream().mapToInt(i -> i).toArray();
        this.mShouldDeny = shouldDeny;
    }

    /**
     * Create the test instances
     */
    @Before
    public void setUp() {
        this.mDenyList = new UsbDeviceDenyList(mFilterValues);
    }

    /**
     * Verify that test values match the expected values
     */
    @Test
    public void testEntries() {
        boolean denied = mDenyList.isDenyListed(
                mValueToTest[0],
                mValueToTest[1],
                mValueToTest[2],
                mValueToTest[3],
                mValueToTest[4]
        );
        assertEquals(mShouldDeny, denied);
    }

    /**
     * Test values
     */
    @ParameterizedRobolectricTestRunner.Parameters(
            name = "USB filter entry: {0} on value: {1} shouldDeny: {2}")
    public static Collection getTestData() {
        Object[][] data = {
                {
                    Arrays.asList("0x1234:0x5678:0xab:0xcd", "0x4321:*"),
                    Arrays.asList(0x1234, 0x5678, 0xab, 0xcd, 0x00),
                    true
                },
                {
                    Arrays.asList("0x1234:0x5678:0xab:0xff", "0x4321:*"),
                    Arrays.asList(0x4321, 0x8765, 0x00, 0x00, 0x00),
                    true
                },
                {
                    Arrays.asList("0x1234:0x5678:0xab:0xff", "0x4321:*"),
                    Arrays.asList(0xfff, 0x8765, 0x00, 0x00, 0x00),
                    false
                },
        };
        return Arrays.asList(data);
    }

}
