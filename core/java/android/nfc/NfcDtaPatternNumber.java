/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.nfc;

/**
 * TODO: Add more contents
 * Based on NFC Forum Device Test Application Application Specification
 * Version 2.3.01
 */
public class NfcDtaPatternNumber {

    /**
     * Error value if PatternNumber is invalid
     */
    public static final int PN_ERR = -2;

    /**
     * Default config without setting PatternNumber
     */
    public static final int PN_NONE = -1;

    /**
     * PatternNumber
     */
    public static final int PN_0000 = 0;

    /**
     * PatternNumber
     */
    public static final int PN_0001 = 1;

    /**
     * PatternNumber
     */
    public static final int PN_0002 = 2;

    /**
     * PatternNumber
     */
    public static final int PN_0003 = 3;

    /**
     * PatternNumber
     */
    public static final int PN_0004 = 4;

    /**
     * PatternNumber
     */
    public static final int PN_0005 = 5;

    /**
     * PatternNumber
     */
    public static final int PN_0006 = 6;

    /**
     * PatternNumber
     */
    public static final int PN_0007 = 7;

    /**
     * PatternNumber
     */
    public static final int PN_0008 = 8;

    /**
     * PatternNumber
     */
    public static final int PN_0009 = 9;

    /**
     * PatternNumber
     */
    public static final int PN_000A = 10;

    /**
     * PatternNumber
     */
    public static final int PN_000B = 11;

    /**
     * PatternNumber
     */
    public static final int PN_1200 = 4608;

    /**
     * PatternNumber
     */
    public static final int PN_1201 = 4609;

    /**
     * PatternNumber
     */
    public static final int PN_1240 = 4672;

    /**
     * PatternNumber
     */
    public static final int PN_1241 = 4673;

    /**
     * PatternNumber
     */
    public static final int PN_1280 = 4736;

    /**
     * PatternNumber
     */
    public static final int PN_1281 = 4737;

    private final int mValue;


    /**
     * Default Constructor
     */
    public NfcDtaPatternNumber() {
        this.mValue = PN_NONE;
    }

    /**
     * Constructor
     */
    public NfcDtaPatternNumber(int pValue) {
        switch (pValue) {
            case PN_NONE:
            case PN_0000:
            case PN_0001:
            case PN_0002:
            case PN_0003:
            case PN_0004:
            case PN_0005:
            case PN_0006:
            case PN_0007:
            case PN_0008:
            case PN_0009:
            case PN_000A:
            case PN_000B:
            case PN_1200:
            case PN_1201:
            case PN_1240:
            case PN_1241:
            case PN_1280:
                this.mValue = pValue;
                break;
            default:
                this.mValue = PN_ERR;
        }
    }

    /**
     * Get pattern number value
     */
    public int getValue() {
        return this.mValue;
    }
}
