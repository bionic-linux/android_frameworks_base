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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Negative test cases for {@link UsbIdFilterEntryParser}
 */
@RunWith(RobolectricTestRunner.class)
public class UsbFilterEntryParserInvalidValuesTest {

    private static final int ASTERISK = -1;
    private UsbIdFilterEntryParser mParser;

    /**
     * Create the test instances
     */
    @Before
    public void setUp() {
        mParser = new UsbIdFilterEntryParser(ASTERISK);
    }

    /**
     * Test invalidNumericValue verifies that non numeric string parsing fails
     */
    @Test(expected = NumberFormatException.class)
    public void invalidNumericValue() {
        mParser.parseFilterEntry("12ef");
    }

    /**
     * Test invalidNumericValue verifies that vendorId parsing fails for
     * the value that exceeds 2 bytes
     */
    @Test(expected = NumberFormatException.class)
    public void invalidVidValue() {
        mParser.parseFilterEntry("0xfffff");
    }

    /**
     * Test invalidPidValue verifies that productId parsing fails for
     * the value that exceeds 2 bytes
     */
    @Test(expected = NumberFormatException.class)
    public void invalidPidValue() {
        mParser.parseFilterEntry("0x1234:0xfffff");
    }

    /**
     * Test invalidClassValue verifies that device class parsing fails for
     * the value that exceeds 1 byte
     */
    @Test(expected = NumberFormatException.class)
    public void invalidClassValue() {
        mParser.parseFilterEntry("0x1234:0x5678:0xfff");
    }

    /**
     * Test invalidSubClassValue verifies that device subclass parsing fails for
     * the value that exceeds 1 byte
     */
    @Test(expected = NumberFormatException.class)
    public void invalidSubClassValue() {
        mParser.parseFilterEntry("0x1234:0x5678:0xab:0xfff");
    }

    /**
     * Test invalidProtocolValue verifies that protocol parsing fails for
     * the value that exceeds 1 byte
     */
    @Test(expected = NumberFormatException.class)
    public void invalidProtocolValue() {
        mParser.parseFilterEntry("0x1234:0x5678:0xab:0xcd:0xfff");
    }

    /**
     * Test outOfBoundsEntriesValue verifies that parsing fails for
     * strings that contain more than 5 entries separated by ':'
     */
    @Test(expected = IllegalArgumentException.class)
    public void outOfBoundsEntriesValue() {
        mParser.parseFilterEntry("0x1234:0x5678:0xab:0xcd:0xef:0x0e");
    }

    /**
     * Test invalidAllColons verifies that parsing fails for
     * strings that only consist of colons
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidAllColons() {
        mParser.parseFilterEntry(":::::");
    }

    /**
     * Test invalidEmptyEntries verifies that parsing fails for
     * strings that miss entries for some of the segments
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidEmptyEntries() {
        mParser.parseFilterEntry("0x1234:0x5678:::");
    }

    /**
     * Test invalidAllColons verifies that parsing fails for empty
     * strings
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidValueEmptyString() {
        mParser.parseFilterEntry("");
    }
}
