/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.internal.telephony.gsm;

import com.android.internal.telephony.gsm.GsmAlphabet.GsmLanguage;

import android.util.SparseIntArray;

import java.util.Map;

/**
 * This class contain the tables used for the GSM SMS 7-bit alphabet specified
 * in TS 23.038 6.2.1
 *
 * {@hide}
 */
public class GsmAlphabetTables {
    static final String LOG_TAG = "GSM";

    static void init(Map<GsmLanguage, SparseIntArray> charToGsmTables,
            Map<GsmLanguage, SparseIntArray> gsmToCharTables,
            Map<GsmLanguage, SparseIntArray> charToGsmExtendedTables,
            Map<GsmLanguage, SparseIntArray> gsmExtendedToCharTables) {

        // Create the table for the standard GSM SMS 7-bit alphabet
        SparseIntArray table = new SparseIntArray();
        gsmToCharTables.put(GsmLanguage.DEFAULT, table);

        int i = 0;
        table.put(i++, '@');
        table.put(i++, '\u00a3');
        table.put(i++, '$');
        table.put(i++, '\u00a5');
        table.put(i++, '\u00e8');
        table.put(i++, '\u00e9');
        table.put(i++, '\u00f9');
        table.put(i++, '\u00ec');
        table.put(i++, '\u00f2');
        table.put(i++, '\u00c7');
        table.put(i++, '\n');
        table.put(i++, '\u00d8');
        table.put(i++, '\u00f8');
        table.put(i++, '\r');
        table.put(i++, '\u00c5');
        table.put(i++, '\u00e5');

        table.put(i++, '\u0394');
        table.put(i++, '_');
        table.put(i++, '\u03a6');
        table.put(i++, '\u0393');
        table.put(i++, '\u039b');
        table.put(i++, '\u03a9');
        table.put(i++, '\u03a0');
        table.put(i++, '\u03a8');
        table.put(i++, '\u03a3');
        table.put(i++, '\u0398');
        table.put(i++, '\u039e');
        table.put(i++, '\uffff');
        table.put(i++, '\u00c6');
        table.put(i++, '\u00e6');
        table.put(i++, '\u00df');
        table.put(i++, '\u00c9');

        table.put(i++, ' ');
        table.put(i++, '!');
        table.put(i++, '"');
        table.put(i++, '#');
        table.put(i++, '\u00a4');
        table.put(i++, '%');
        table.put(i++, '&');
        table.put(i++, '\'');
        table.put(i++, '(');
        table.put(i++, ')');
        table.put(i++, '*');
        table.put(i++, '+');
        table.put(i++, ',');
        table.put(i++, '-');
        table.put(i++, '.');
        table.put(i++, '/');

        table.put(i++, '0');
        table.put(i++, '1');
        table.put(i++, '2');
        table.put(i++, '3');
        table.put(i++, '4');
        table.put(i++, '5');
        table.put(i++, '6');
        table.put(i++, '7');
        table.put(i++, '8');
        table.put(i++, '9');
        table.put(i++, ':');
        table.put(i++, ';');
        table.put(i++, '<');
        table.put(i++, '=');
        table.put(i++, '>');
        table.put(i++, '?');

        table.put(i++, '\u00a1');
        table.put(i++, 'A');
        table.put(i++, 'B');
        table.put(i++, 'C');
        table.put(i++, 'D');
        table.put(i++, 'E');
        table.put(i++, 'F');
        table.put(i++, 'G');
        table.put(i++, 'H');
        table.put(i++, 'I');
        table.put(i++, 'J');
        table.put(i++, 'K');
        table.put(i++, 'L');
        table.put(i++, 'M');
        table.put(i++, 'N');
        table.put(i++, 'O');

        table.put(i++, 'P');
        table.put(i++, 'Q');
        table.put(i++, 'R');
        table.put(i++, 'S');
        table.put(i++, 'T');
        table.put(i++, 'U');
        table.put(i++, 'V');
        table.put(i++, 'W');
        table.put(i++, 'X');
        table.put(i++, 'Y');
        table.put(i++, 'Z');
        table.put(i++, '\u00c4');
        table.put(i++, '\u00d6');
        table.put(i++, '\u00d1');
        table.put(i++, '\u00dc');
        table.put(i++, '\u00a7');

        table.put(i++, '\u00bf');
        table.put(i++, 'a');
        table.put(i++, 'b');
        table.put(i++, 'c');
        table.put(i++, 'd');
        table.put(i++, 'e');
        table.put(i++, 'f');
        table.put(i++, 'g');
        table.put(i++, 'h');
        table.put(i++, 'i');
        table.put(i++, 'j');
        table.put(i++, 'k');
        table.put(i++, 'l');
        table.put(i++, 'm');
        table.put(i++, 'n');
        table.put(i++, 'o');

        table.put(i++, 'p');
        table.put(i++, 'q');
        table.put(i++, 'r');
        table.put(i++, 's');
        table.put(i++, 't');
        table.put(i++, 'u');
        table.put(i++, 'v');
        table.put(i++, 'w');
        table.put(i++, 'x');
        table.put(i++, 'y');
        table.put(i++, 'z');
        table.put(i++, '\u00e4');
        table.put(i++, '\u00f6');
        table.put(i++, '\u00f1');
        table.put(i++, '\u00fc');
        table.put(i++, '\u00e0');

        // Create the locking shift table for turkish national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.TURKISH, table);

        table.put(0x04, '\u20ac');
        table.put(0x07, '\u0131');
        table.put(0x0b, '\u011e');
        table.put(0x0c, '\u011f');

        table.put(0x1c, '\u015e');
        table.put(0x1d, '\u015f');

        table.put(0x40, '\u0130');

        table.put(0x60, '\u00e7');

        // Create the locking shift table for spanish national language
        // Not defined - fall back to GSM 7 bit default
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.SPANISH, table);

        // Create the locking shift table for portuguese national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.PORTUGUESE, table);
        table.put(0x04, '\u00ea');
        table.put(0x06, '\u00fa');
        table.put(0x07, '\u00ed');
        table.put(0x08, '\u00f3');
        table.put(0x09, '\u00e7');
        table.put(0x0b, '\u00d4');
        table.put(0x0c, '\u00f4');
        table.put(0x0e, '\u00c1');
        table.put(0x0f, '\u00e1');

        table.put(0x12, '\u00aa');
        table.put(0x13, '\u00c7');
        table.put(0x14, '\u00c0');
        table.put(0x15, '\u22e1');
        table.put(0x16, '\u005e');
        table.put(0x17, '\\');
        table.put(0x18, '\u20ac');
        table.put(0x19, '\u00d3');
        table.put(0x1a, '\u007c');
        table.put(0x1c, '\u00c2');
        table.put(0x1d, '\u00e2');
        table.put(0x1e, '\u00ca');

        table.put(0x24, '\u00b0');

        table.put(0x40, '\u00cd');

        table.put(0x5b, '\u00c3');
        table.put(0x5c, '\u005d');
        table.put(0x5d, '\u00da');

        table.put(0x60, '\u007e');

        table.put(0x7b, '\u00e3');
        table.put(0x7c, '\u00f5');
        table.put(0x7d, '\u0060');

        // Create the locking shift table for bengali national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.BENGALI, table);
        table.put(0x00, '\u0981');
        table.put(0x01, '\u0982');
        table.put(0x02, '\u0983');
        table.put(0x03, '\u0985');
        table.put(0x04, '\u0986');
        table.put(0x05, '\u0987');
        table.put(0x06, '\u0988');
        table.put(0x07, '\u0989');
        table.put(0x08, '\u098a');
        table.put(0x09, '\u098b');
        table.put(0x0b, '\u098c');
        table.put(0x0f, '\u098f');

        table.put(0x10, '\u0990');
        table.put(0x13, '\u0993');
        table.put(0x14, '\u0994');
        table.put(0x15, '\u0995');
        table.put(0x16, '\u0996');
        table.put(0x17, '\u0997');
        table.put(0x18, '\u0998');
        table.put(0x19, '\u0999');
        table.put(0x1a, '\u099a');
        table.put(0x1c, '\u099b');
        table.put(0x1d, '\u099c');
        table.put(0x1e, '\u099d');
        table.put(0x1f, '\u099e');

        table.put(0x22, '\u099f');
        table.put(0x23, '\u09a0');
        table.put(0x24, '\u09a1');
        table.put(0x25, '\u09a2');
        table.put(0x26, '\u09a3');
        table.put(0x27, '\u09a4');
        table.put(0x2a, '\u09a5');
        table.put(0x2b, '\u09a6');
        table.put(0x2d, '\u09a7');
        table.put(0x2f, '\u09a8');

        table.put(0x3d, '\u09aa');
        table.put(0x3e, '\u09ab');

        table.put(0x40, '\u09ac');
        table.put(0x41, '\u09ad');
        table.put(0x42, '\u09ae');
        table.put(0x43, '\u09af');
        table.put(0x44, '\u09b0');
        table.put(0x46, '\u09b2');
        table.put(0x4a, '\u09b6');
        table.put(0x4b, '\u09b7');
        table.put(0x4c, '\u09b8');
        table.put(0x4d, '\u09b9');
        table.put(0x4e, '\u09bc');
        table.put(0x4f, '\u09bd');

        table.put(0x50, '\u09be');
        table.put(0x51, '\u09bf');
        table.put(0x52, '\u09c0');
        table.put(0x53, '\u09c1');
        table.put(0x54, '\u09c2');
        table.put(0x55, '\u09c3');
        table.put(0x56, '\u09c4');
        table.put(0x59, '\u09c7');
        table.put(0x5a, '\u09c8');
        table.put(0x5d, '\u09cb');
        table.put(0x5e, '\u09cc');
        table.put(0x5f, '\u09cd');

        table.put(0x60, '\u09ce');

        table.put(0x7b, '\u09d7');
        table.put(0x7c, '\u09dc');
        table.put(0x7d, '\u09dd');
        table.put(0x7e, '\u09f0');
        table.put(0x7f, '\u09f1');

        // Create the locking shift table for gujariti national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.GUJARATI, table);
        table.put(0x00, '\u0a81');
        table.put(0x01, '\u0a82');
        table.put(0x02, '\u0a83');
        table.put(0x03, '\u0a85');
        table.put(0x04, '\u0a86');
        table.put(0x05, '\u0a87');
        table.put(0x06, '\u0a88');
        table.put(0x07, '\u0a89');
        table.put(0x08, '\u0a8a');
        table.put(0x09, '\u0a8b');
        table.put(0x0b, '\u0a8c');
        table.put(0x0c, '\u0a8d');
        table.put(0x0f, '\u0a8f');

        table.put(0x10, '\u0a90');
        table.put(0x11, '\u0a91');
        table.put(0x13, '\u0a93');
        table.put(0x14, '\u0a94');
        table.put(0x15, '\u0a95');
        table.put(0x16, '\u0a96');
        table.put(0x17, '\u0a97');
        table.put(0x18, '\u0a98');
        table.put(0x19, '\u0a99');
        table.put(0x1a, '\u0a9a');
        table.put(0x1c, '\u0a9b');
        table.put(0x1d, '\u0a9c');
        table.put(0x1e, '\u0a9d');
        table.put(0x1f, '\u0a9e');

        table.put(0x22, '\u0a9f');
        table.put(0x23, '\u0aa0');
        table.put(0x24, '\u0aa1');
        table.put(0x25, '\u0aa2');
        table.put(0x26, '\u0aa3');
        table.put(0x27, '\u0aa4');
        table.put(0x2a, '\u0aa5');
        table.put(0x2b, '\u0aa6');
        table.put(0x2d, '\u0aa7');
        table.put(0x2f, '\u0aa8');

        table.put(0x3d, '\u0aaa');
        table.put(0x3e, '\u0aab');

        table.put(0x40, '\u0aac');
        table.put(0x41, '\u0aad');
        table.put(0x42, '\u0aae');
        table.put(0x43, '\u0aaf');
        table.put(0x44, '\u0ab0');
        table.put(0x46, '\u0ab2');
        table.put(0x47, '\u0ab3');
        table.put(0x49, '\u0ab5');
        table.put(0x4a, '\u0ab6');
        table.put(0x4b, '\u0ab7');
        table.put(0x4c, '\u0ab8');
        table.put(0x4d, '\u0ab9');
        table.put(0x4e, '\u0abc');
        table.put(0x4f, '\u0abd');

        table.put(0x50, '\u0abe');
        table.put(0x51, '\u0abf');
        table.put(0x52, '\u0ac0');
        table.put(0x53, '\u0ac1');
        table.put(0x54, '\u0ac2');
        table.put(0x55, '\u0ac3');
        table.put(0x56, '\u0ac4');
        table.put(0x57, '\u0ac5');
        table.put(0x59, '\u0ac7');
        table.put(0x5a, '\u0ac8');
        table.put(0x5b, '\u0ac9');
        table.put(0x5d, '\u0acb');
        table.put(0x5e, '\u0acc');
        table.put(0x5f, '\u0acd');

        table.put(0x60, '\u0ad0');

        table.put(0x7b, '\u0ae0');
        table.put(0x7c, '\u0ae1');
        table.put(0x7d, '\u0ae2');
        table.put(0x7e, '\u0ae3');
        table.put(0x7f, '\u0af1');

        // Create the locking shift table for hindi national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.HINDI, table);
        table.put(0x00, '\u0901');
        table.put(0x01, '\u0902');
        table.put(0x02, '\u0903');
        table.put(0x03, '\u0905');
        table.put(0x04, '\u0906');
        table.put(0x05, '\u0907');
        table.put(0x06, '\u0908');
        table.put(0x07, '\u0909');
        table.put(0x08, '\u090a');
        table.put(0x09, '\u090b');
        table.put(0x0b, '\u090c');
        table.put(0x0c, '\u090c');
        table.put(0x0e, '\u090e');
        table.put(0x0f, '\u090f');

        table.put(0x10, '\u0910');
        table.put(0x11, '\u0911');
        table.put(0x12, '\u0912');
        table.put(0x13, '\u0913');
        table.put(0x14, '\u0914');
        table.put(0x15, '\u0915');
        table.put(0x16, '\u0916');
        table.put(0x17, '\u0917');
        table.put(0x18, '\u0918');
        table.put(0x19, '\u0919');
        table.put(0x1a, '\u091a');
        table.put(0x1c, '\u091b');
        table.put(0x1d, '\u091c');
        table.put(0x1e, '\u091d');
        table.put(0x1f, '\u091e');

        table.put(0x22, '\u091f');
        table.put(0x23, '\u0920');
        table.put(0x24, '\u0921');
        table.put(0x25, '\u0922');
        table.put(0x26, '\u0923');
        table.put(0x27, '\u0924');
        table.put(0x2a, '\u0925');
        table.put(0x2b, '\u0926');
        table.put(0x2d, '\u0927');
        table.put(0x2f, '\u0928');

        table.put(0x3c, '\u0929');
        table.put(0x3d, '\u092a');
        table.put(0x3e, '\u092b');

        table.put(0x40, '\u092c');
        table.put(0x41, '\u092d');
        table.put(0x42, '\u092e');
        table.put(0x43, '\u092f');
        table.put(0x44, '\u0930');
        table.put(0x45, '\u0931');
        table.put(0x46, '\u0932');
        table.put(0x47, '\u0933');
        table.put(0x48, '\u0934');
        table.put(0x49, '\u0935');
        table.put(0x4a, '\u0936');
        table.put(0x4c, '\u0937');
        table.put(0x4d, '\u0938');
        table.put(0x4e, '\u0939');
        table.put(0x4f, '\u093c');
        table.put(0x4f, '\u093d');

        table.put(0x50, '\u093e');
        table.put(0x51, '\u093f');
        table.put(0x52, '\u0940');
        table.put(0x53, '\u0941');
        table.put(0x54, '\u0942');
        table.put(0x55, '\u0943');
        table.put(0x56, '\u0944');
        table.put(0x57, '\u0945');
        table.put(0x58, '\u0946');
        table.put(0x59, '\u0947');
        table.put(0x5a, '\u0948');
        table.put(0x5c, '\u0949');
        table.put(0x5d, '\u094a');
        table.put(0x5e, '\u094b');
        table.put(0x5f, '\u094c');
        table.put(0x5f, '\u094d');

        table.put(0x60, '\u0950');

        table.put(0x7b, '\u0972');
        table.put(0x7c, '\u097b');
        table.put(0x7d, '\u097c');
        table.put(0x7e, '\u097e');
        table.put(0x7f, '\u097f');

        // Create the locking shift table for kannada national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.KANNADA, table);
        table.put(0x01, '\u0c82');
        table.put(0x02, '\u0c83');
        table.put(0x03, '\u0c85');
        table.put(0x04, '\u0c86');
        table.put(0x05, '\u0c87');
        table.put(0x06, '\u0c88');
        table.put(0x07, '\u0c89');
        table.put(0x08, '\u0c8a');
        table.put(0x09, '\u0c8b');
        table.put(0x0b, '\u0c8c');
        table.put(0x0e, '\u0c8e');
        table.put(0x0f, '\u0c8f');

        table.put(0x10, '\u0c90');
        table.put(0x12, '\u0c92');
        table.put(0x13, '\u0c93');
        table.put(0x14, '\u0c94');
        table.put(0x15, '\u0c95');
        table.put(0x16, '\u0c96');
        table.put(0x17, '\u0c97');
        table.put(0x18, '\u0c98');
        table.put(0x19, '\u0c99');
        table.put(0x1a, '\u0c9a');
        table.put(0x1c, '\u0c9b');
        table.put(0x1d, '\u0c9c');
        table.put(0x1e, '\u0c9d');
        table.put(0x1f, '\u0c9e');

        table.put(0x22, '\u0c9f');
        table.put(0x23, '\u0ca0');
        table.put(0x24, '\u0caa');
        table.put(0x25, '\u0ca2');
        table.put(0x26, '\u0ca3');
        table.put(0x27, '\u0ca4');
        table.put(0x2a, '\u0ca5');
        table.put(0x2b, '\u0ca6');
        table.put(0x2d, '\u0ca7');
        table.put(0x2f, '\u0ca8');

        table.put(0x3d, '\u0caa');
        table.put(0x3e, '\u0cab');

        table.put(0x40, '\u0cac');
        table.put(0x41, '\u0cad');
        table.put(0x42, '\u0cae');
        table.put(0x43, '\u0caf');
        table.put(0x44, '\u0cb0');
        table.put(0x45, '\u0cb1');
        table.put(0x46, '\u0cb2');
        table.put(0x47, '\u0cb3');
        table.put(0x49, '\u0cb5');
        table.put(0x4a, '\u0cb6');
        table.put(0x4b, '\u0cb7');
        table.put(0x4c, '\u0cb8');
        table.put(0x4d, '\u0cb9');
        table.put(0x4e, '\u0cbc');
        table.put(0x4f, '\u0cbd');

        table.put(0x50, '\u0cbe');
        table.put(0x51, '\u0cbf');
        table.put(0x52, '\u0cc0');
        table.put(0x53, '\u0cc1');
        table.put(0x54, '\u0cc2');
        table.put(0x55, '\u0cc3');
        table.put(0x56, '\u0cc4');
        table.put(0x58, '\u0cc6');
        table.put(0x59, '\u0cc7');
        table.put(0x5a, '\u0cc8');
        table.put(0x5c, '\u0cca');
        table.put(0x5d, '\u0ccb');
        table.put(0x5e, '\u0ccc');
        table.put(0x5f, '\u0ccd');

        table.put(0x7b, '\u0cd6');
        table.put(0x7c, '\u0ce0');
        table.put(0x7d, '\u0ce1');
        table.put(0x7e, '\u0ce2');
        table.put(0x7f, '\u0ce3');

        // Create the locking shift table for malayalam national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.MALAYALAM, table);
        table.put(0x01, '\u0d02');
        table.put(0x02, '\u0d03');
        table.put(0x03, '\u0d05');
        table.put(0x04, '\u0d06');
        table.put(0x05, '\u0d07');
        table.put(0x06, '\u0d08');
        table.put(0x07, '\u0d09');
        table.put(0x08, '\u0d0a');
        table.put(0x09, '\u0d0b');
        table.put(0x0b, '\u0d0c');
        table.put(0x0e, '\u0d0e');
        table.put(0x0f, '\u0c0f');

        table.put(0x10, '\u0d10');
        table.put(0x12, '\u0d12');
        table.put(0x13, '\u0d13');
        table.put(0x14, '\u0d14');
        table.put(0x15, '\u0d15');
        table.put(0x16, '\u0d16');
        table.put(0x17, '\u0d17');
        table.put(0x18, '\u0d18');
        table.put(0x19, '\u0d19');
        table.put(0x1a, '\u0d1a');
        table.put(0x1c, '\u0d1b');
        table.put(0x1d, '\u0d1c');
        table.put(0x1e, '\u0d1d');
        table.put(0x1f, '\u0d1e');

        table.put(0x22, '\u0d1f');
        table.put(0x23, '\u0d20');
        table.put(0x24, '\u0d21');
        table.put(0x25, '\u0d22');
        table.put(0x26, '\u0d23');
        table.put(0x27, '\u0d24');
        table.put(0x2a, '\u0d25');
        table.put(0x2b, '\u0d26');
        table.put(0x2d, '\u0d27');
        table.put(0x2f, '\u0d28');

        table.put(0x3d, '\u0d2a');
        table.put(0x3e, '\u0d2b');

        table.put(0x40, '\u0d2c');
        table.put(0x41, '\u0d2d');
        table.put(0x42, '\u0d2e');
        table.put(0x43, '\u0d2f');
        table.put(0x44, '\u0d30');
        table.put(0x45, '\u0d31');
        table.put(0x46, '\u0d32');
        table.put(0x47, '\u0d33');
        table.put(0x48, '\u0d34');
        table.put(0x49, '\u0d35');
        table.put(0x4a, '\u0d36');
        table.put(0x4b, '\u0d37');
        table.put(0x4c, '\u0d38');
        table.put(0x4d, '\u0d39');
        table.put(0x4f, '\u0d3d');

        table.put(0x50, '\u0d3e');
        table.put(0x51, '\u0d3f');
        table.put(0x52, '\u0d40');
        table.put(0x53, '\u0d41');
        table.put(0x54, '\u0d42');
        table.put(0x55, '\u0d43');
        table.put(0x56, '\u0d44');
        table.put(0x58, '\u0d46');
        table.put(0x59, '\u0d47');
        table.put(0x5a, '\u0d48');
        table.put(0x5c, '\u0d4a');
        table.put(0x5d, '\u0d4b');
        table.put(0x5e, '\u0d4c');
        table.put(0x5f, '\u0d4d');

        table.put(0x60, '\u0d57');

        table.put(0x7b, '\u0d60');
        table.put(0x7c, '\u0d61');
        table.put(0x7d, '\u0d62');
        table.put(0x7e, '\u0d63');
        table.put(0x7f, '\u0d79');

        // Create the locking shift table for oriya national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.ORIYA, table);
        table.put(0x00, '\u0b01');
        table.put(0x01, '\u0b02');
        table.put(0x02, '\u0b03');
        table.put(0x03, '\u0b05');
        table.put(0x04, '\u0b06');
        table.put(0x05, '\u0b07');
        table.put(0x06, '\u0b08');
        table.put(0x07, '\u0b09');
        table.put(0x08, '\u0b0a');
        table.put(0x09, '\u0b0b');
        table.put(0x0b, '\u0b0c');
        table.put(0x0f, '\u0b0f');

        table.put(0x00, '\u0b10');
        table.put(0x13, '\u0b13');
        table.put(0x14, '\u0b14');
        table.put(0x15, '\u0b15');
        table.put(0x16, '\u0b16');
        table.put(0x17, '\u0b17');
        table.put(0x18, '\u0b18');
        table.put(0x19, '\u0b19');
        table.put(0x1a, '\u0b1a');
        table.put(0x1c, '\u0b1b');
        table.put(0x1d, '\u0b1c');
        table.put(0x1e, '\u0b1d');
        table.put(0x1f, '\u0b1e');

        table.put(0x22, '\u0b1f');
        table.put(0x23, '\u0b20');
        table.put(0x24, '\u0b21');
        table.put(0x25, '\u0b22');
        table.put(0x26, '\u0b23');
        table.put(0x27, '\u0b24');
        table.put(0x2a, '\u0b25');
        table.put(0x2b, '\u0b26');
        table.put(0x2d, '\u0b27');
        table.put(0x2f, '\u0b28');

        table.put(0x3d, '\u0b2a');
        table.put(0x3e, '\u0b2b');

        table.put(0x40, '\u0b2c');
        table.put(0x41, '\u0b2d');
        table.put(0x42, '\u0b2e');
        table.put(0x43, '\u0b2f');
        table.put(0x44, '\u0b30');
        table.put(0x46, '\u0b32');
        table.put(0x47, '\u0b33');
        table.put(0x49, '\u0b35');
        table.put(0x4a, '\u0b36');
        table.put(0x4b, '\u0b37');
        table.put(0x4c, '\u0b38');
        table.put(0x4d, '\u0b39');
        table.put(0x4f, '\u0b3d');

        table.put(0x50, '\u0b3e');
        table.put(0x51, '\u0b3f');
        table.put(0x52, '\u0b40');
        table.put(0x53, '\u0b41');
        table.put(0x54, '\u0b42');
        table.put(0x55, '\u0b43');
        table.put(0x56, '\u0b44');
        table.put(0x59, '\u0b47');
        table.put(0x5a, '\u0b48');
        table.put(0x5d, '\u0b4b');
        table.put(0x5e, '\u0b4c');
        table.put(0x5f, '\u0b4d');

        table.put(0x60, '\u0b56');

        table.put(0x7b, '\u0b57');
        table.put(0x7c, '\u0b60');
        table.put(0x7d, '\u0b61');
        table.put(0x7e, '\u0b62');
        table.put(0x7f, '\u0b63');

        // Create the locking shift table for punjabi national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.PUNJABI, table);
        table.put(0x00, '\u0a01');
        table.put(0x01, '\u0a02');
        table.put(0x02, '\u0a03');
        table.put(0x03, '\u0a05');
        table.put(0x04, '\u0a06');
        table.put(0x05, '\u0a07');
        table.put(0x06, '\u0a08');
        table.put(0x07, '\u0a09');
        table.put(0x08, '\u0a0a');
        table.put(0x0f, '\u0a0f');

        table.put(0x10, '\u0a10');
        table.put(0x13, '\u0a13');
        table.put(0x14, '\u0a14');
        table.put(0x15, '\u0a15');
        table.put(0x16, '\u0a16');
        table.put(0x17, '\u0a17');
        table.put(0x18, '\u0a18');
        table.put(0x19, '\u0a19');
        table.put(0x1a, '\u0a1a');
        table.put(0x1c, '\u0a1b');
        table.put(0x1d, '\u0a1c');
        table.put(0x1e, '\u0a1d');
        table.put(0x1f, '\u0a1e');

        table.put(0x22, '\u0a1f');
        table.put(0x23, '\u0a20');
        table.put(0x24, '\u0a21');
        table.put(0x25, '\u0a22');
        table.put(0x26, '\u0a23');
        table.put(0x27, '\u0a24');
        table.put(0x2a, '\u0a25');
        table.put(0x2b, '\u0a26');
        table.put(0x2d, '\u0a27');
        table.put(0x2f, '\u0a28');

        table.put(0x3d, '\u0a2a');
        table.put(0x3e, '\u0a2b');

        table.put(0x40, '\u0a2c');
        table.put(0x41, '\u0a2d');
        table.put(0x42, '\u0a2e');
        table.put(0x43, '\u0a2f');
        table.put(0x44, '\u0a30');
        table.put(0x46, '\u0a32');
        table.put(0x47, '\u0a33');
        table.put(0x49, '\u0a35');
        table.put(0x4a, '\u0a36');
        table.put(0x4c, '\u0a38');
        table.put(0x4d, '\u0a39');
        table.put(0x4e, '\u0a3c');

        table.put(0x50, '\u0a3e');
        table.put(0x51, '\u0a3f');
        table.put(0x52, '\u0a40');
        table.put(0x53, '\u0a41');
        table.put(0x54, '\u0a42');
        table.put(0x59, '\u0a47');
        table.put(0x5a, '\u0a48');
        table.put(0x5d, '\u0a4b');
        table.put(0x5e, '\u0a4c');
        table.put(0x5f, '\u0a4d');

        table.put(0x60, '\u0a51');

        table.put(0x7b, '\u0a70');
        table.put(0x7c, '\u0a71');
        table.put(0x7d, '\u0a72');
        table.put(0x7e, '\u0a73');
        table.put(0x7f, '\u0a74');

        // Create the locking shift table for tamil national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.TAMIL, table);
        table.put(0x01, '\u0b82');
        table.put(0x02, '\u0b83');
        table.put(0x03, '\u0b85');
        table.put(0x04, '\u0b86');
        table.put(0x05, '\u0b87');
        table.put(0x06, '\u0b88');
        table.put(0x07, '\u0b89');
        table.put(0x08, '\u0b8a');
        table.put(0x0e, '\u0b8e');
        table.put(0x0f, '\u0b8f');

        table.put(0x10, '\u0b90');
        table.put(0x12, '\u0b92');
        table.put(0x13, '\u0b93');
        table.put(0x14, '\u0b94');
        table.put(0x15, '\u0b95');
        table.put(0x19, '\u0b99');
        table.put(0x1a, '\u0b9a');
        table.put(0x1d, '\u0b9c');
        table.put(0x1f, '\u0b9e');

        table.put(0x22, '\u0b9f');
        table.put(0x26, '\u0ba3');
        table.put(0x27, '\u0ba4');
        table.put(0x2f, '\u0ba8');

        table.put(0x3c, '\u0ba9');
        table.put(0x3d, '\u0baa');

        table.put(0x42, '\u0bae');
        table.put(0x43, '\u0baf');
        table.put(0x44, '\u0bb0');
        table.put(0x45, '\u0bb1');
        table.put(0x46, '\u0bb2');
        table.put(0x47, '\u0bb3');
        table.put(0x48, '\u0bb4');
        table.put(0x49, '\u0bb5');
        table.put(0x4a, '\u0bb6');
        table.put(0x4b, '\u0bb7');
        table.put(0x4c, '\u0bb8');
        table.put(0x4d, '\u0bb9');

        table.put(0x50, '\u0bbe');
        table.put(0x51, '\u0bbf');
        table.put(0x52, '\u0bc0');
        table.put(0x53, '\u0bc1');
        table.put(0x54, '\u0bc2');
        table.put(0x58, '\u0bc6');
        table.put(0x59, '\u0bc7');
        table.put(0x5a, '\u0bc8');
        table.put(0x5c, '\u0bca');
        table.put(0x5d, '\u0bcb');
        table.put(0x5e, '\u0bcc');
        table.put(0x5f, '\u0bcd');

        table.put(0x60, '\u0bd0');

        table.put(0x7b, '\u0bd7');
        table.put(0x7c, '\u0bf0');
        table.put(0x7d, '\u0bf1');
        table.put(0x7e, '\u0bf2');
        table.put(0x7f, '\u0bf9');

        // Create the locking shift table for telugu national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.TELUGU, table);
        table.put(0x00, '\u0c01');
        table.put(0x01, '\u0c02');
        table.put(0x02, '\u0c03');
        table.put(0x03, '\u0c05');
        table.put(0x04, '\u0c06');
        table.put(0x05, '\u0c07');
        table.put(0x06, '\u0c08');
        table.put(0x07, '\u0c09');
        table.put(0x08, '\u0c0a');
        table.put(0x09, '\u0c0b');
        table.put(0x0b, '\u0c0c');
        table.put(0x0e, '\u0c0e');
        table.put(0x0f, '\u0c0f');

        table.put(0x10, '\u0c10');
        table.put(0x12, '\u0c12');
        table.put(0x13, '\u0c13');
        table.put(0x14, '\u0c14');
        table.put(0x15, '\u0c15');
        table.put(0x16, '\u0c16');
        table.put(0x17, '\u0c17');
        table.put(0x18, '\u0c18');
        table.put(0x19, '\u0c19');
        table.put(0x1a, '\u0c1a');
        table.put(0x1c, '\u0c1b');
        table.put(0x1d, '\u0c1c');
        table.put(0x1e, '\u0c1d');
        table.put(0x1f, '\u0c1e');

        table.put(0x22, '\u0c1f');
        table.put(0x23, '\u0c20');
        table.put(0x24, '\u0c21');
        table.put(0x25, '\u0c22');
        table.put(0x26, '\u0c23');
        table.put(0x27, '\u0c24');
        table.put(0x2a, '\u0c25');
        table.put(0x2b, '\u0c26');
        table.put(0x2d, '\u0c27');
        table.put(0x2f, '\u0c28');

        table.put(0x3d, '\u0c2a');
        table.put(0x3e, '\u0c2b');

        table.put(0x40, '\u0c2c');
        table.put(0x41, '\u0c2d');
        table.put(0x42, '\u0c2e');
        table.put(0x43, '\u0c2f');
        table.put(0x44, '\u0c30');
        table.put(0x45, '\u0d31');
        table.put(0x46, '\u0c32');
        table.put(0x47, '\u0c33');
        table.put(0x49, '\u0c35');
        table.put(0x4a, '\u0c36');
        table.put(0x4b, '\u0c37');
        table.put(0x4c, '\u0c38');
        table.put(0x4d, '\u0c39');
        table.put(0x4f, '\u0c3d');

        table.put(0x50, '\u0c3e');
        table.put(0x51, '\u0c3f');
        table.put(0x52, '\u0c40');
        table.put(0x53, '\u0c41');
        table.put(0x54, '\u0c42');
        table.put(0x55, '\u0c43');
        table.put(0x56, '\u0c44');
        table.put(0x58, '\u0c46');
        table.put(0x59, '\u0c47');
        table.put(0x5a, '\u0c48');
        table.put(0x5c, '\u0c4a');
        table.put(0x5d, '\u0c4b');
        table.put(0x5e, '\u0c4c');
        table.put(0x5f, '\u0c4d');

        table.put(0x60, '\u0c55');

        table.put(0x7b, '\u0c56');
        table.put(0x7c, '\u0c60');
        table.put(0x7d, '\u0c61');
        table.put(0x7e, '\u0c62');
        table.put(0x7f, '\u0c63');

        // Create the locking shift table for urdu national language
        table = copyTable(gsmToCharTables.get(GsmLanguage.DEFAULT));
        gsmToCharTables.put(GsmLanguage.URDU, table);
        table.put(0x00, '\u0627');
        table.put(0x01, '\u0622');
        table.put(0x02, '\u0628');
        table.put(0x03, '\u067b');
        table.put(0x04, '\u0680');
        table.put(0x05, '\u067e');
        table.put(0x06, '\u06a6');
        table.put(0x07, '\u062a');
        table.put(0x08, '\u06c2');
        table.put(0x09, '\u067f');
        table.put(0x0b, '\u0679');
        table.put(0x0c, '\u067d');
        table.put(0x0e, '\u067a');
        table.put(0x0f, '\u067c');

        table.put(0x10, '\u062b');
        table.put(0x11, '\u062c');
        table.put(0x12, '\u0681');
        table.put(0x13, '\u0684');
        table.put(0x14, '\u0683');
        table.put(0x15, '\u0685');
        table.put(0x16, '\u0686');
        table.put(0x17, '\u0687');
        table.put(0x18, '\u062d');
        table.put(0x19, '\u062e');
        table.put(0x1a, '\u062f');
        table.put(0x1c, '\u068c');
        table.put(0x1d, '\u0688');
        table.put(0x1e, '\u0689');
        table.put(0x1f, '\u068a');

        table.put(0x22, '\u068f');
        table.put(0x23, '\u068d');
        table.put(0x24, '\u0630');
        table.put(0x25, '\u0631');
        table.put(0x26, '\u0691');
        table.put(0x27, '\u0693');
        table.put(0x2a, '\u0699');
        table.put(0x2b, '\u0632');
        table.put(0x2d, '\u0696');
        table.put(0x2f, '\u0698');

        table.put(0x3c, '\u069a');
        table.put(0x3d, '\u0633');
        table.put(0x3e, '\u0634');

        table.put(0x40, '\u0635');
        table.put(0x41, '\u0636');
        table.put(0x42, '\u0637');
        table.put(0x43, '\u0638');
        table.put(0x44, '\u0639');
        table.put(0x45, '\u0641');
        table.put(0x46, '\u0642');
        table.put(0x47, '\u06a9');
        table.put(0x48, '\u06aa');
        table.put(0x49, '\u06ab');
        table.put(0x4a, '\u06af');
        table.put(0x4b, '\u06b3');
        table.put(0x4c, '\u06b1');
        table.put(0x4d, '\u0644');
        table.put(0x4e, '\u0645');
        table.put(0x4f, '\u0646');

        table.put(0x50, '\u06ba');
        table.put(0x51, '\u06bb');
        table.put(0x52, '\u06bc');
        table.put(0x53, '\u0648');
        table.put(0x54, '\u06c4');
        table.put(0x55, '\u06d5');
        table.put(0x56, '\u06c1');
        table.put(0x57, '\u06be');
        table.put(0x58, '\u0621');
        table.put(0x59, '\u06cc');
        table.put(0x5a, '\u06d0');
        table.put(0x5b, '\u06d2');
        table.put(0x5c, '\u064d');
        table.put(0x5d, '\u0650');
        table.put(0x5e, '\u064f');
        table.put(0x5f, '\u0657');

        table.put(0x60, '\u0654');

        table.put(0x7b, '\u0655');
        table.put(0x7c, '\u0651');
        table.put(0x7d, '\u0653');
        table.put(0x7e, '\u0656');
        table.put(0x7f, '\u0670');

        // Create the table for the extended GSM SMS 7-bit alphabet
        table = new SparseIntArray();
        gsmExtendedToCharTables.put(GsmLanguage.DEFAULT, table);
        table.put(10, '\f');
        table.put(20, '^');
        table.put(40, '{');
        table.put(41, '}');
        table.put(47, '\\');
        table.put(60, '[');
        table.put(61, '~');
        table.put(62, ']');
        table.put(64, '|');
        table.put(101, '\u20ac');

        // Create the single shift table for the turkish national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.TURKISH, table);
        table.put(0x47, '\u011e');
        table.put(0x49, '\u0130');
        table.put(0x53, '\u015e');
        table.put(0x63, '\u00e7');
        table.put(0x67, '\u011f');
        table.put(0x69, '\u0131');
        table.put(0x73, '\u015f');

        // Create the single shift table for the spanish national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.SPANISH, table);
        table.put(0x09, '\u00e7');
        table.put(0x41, '\u00c1');
        table.put(0x49, '\u00cd');
        table.put(0x4f, '\u00d3');
        table.put(0x55, '\u00da');
        table.put(0x61, '\u00e1');
        table.put(0x69, '\u00ed');
        table.put(0x6f, '\u00f3');
        table.put(0x75, '\u00fa');

        // Create the single shift table for the portuguese national language
        // table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.PORTUGUESE, table);
        table.put(0x05, '\u00ea');
        table.put(0x09, '\u00e7');
        table.put(0x0b, '\u00d4');
        table.put(0x0c, '\u00f4');
        table.put(0x0e, '\u00c1');
        table.put(0x0f, '\u00e1');

        table.put(0x12, '\u03a6');
        table.put(0x13, '\u0393');
        table.put(0x15, '\u03a9');
        table.put(0x16, '\u03a0');
        table.put(0x17, '\u03a8');
        table.put(0x18, '\u03a3');
        table.put(0x19, '\u0398');
        table.put(0x1f, '\u00ca');

        table.put(0x41, '\u00c0');
        table.put(0x49, '\u00cd');
        table.put(0x4f, '\u00d3');

        table.put(0x55, '\u00da');
        table.put(0x5b, '\u00c3');
        table.put(0x5c, '\u00d5');
        table.put(0x5c, '\u00d5');

        table.put(0x61, '\u00c2');
        table.put(0x69, '\u00ed');
        table.put(0x6f, '\u00f3');

        table.put(0x75, '\u00fa');
        table.put(0x7b, '\u00e3');
        table.put(0x7c, '\u00f5');
        table.put(0x7f, '\u00e2');

        // Create the single shift table for the bengali national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.BENGALI, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u09e6');
        table.put(0x1a, '\u09e7');
        table.put(0x1c, '\u09e8');
        table.put(0x1d, '\u09e9');
        table.put(0x1e, '\u09ea');
        table.put(0x1f, '\u09eb');

        table.put(0x20, '\u09ec');
        table.put(0x21, '\u09ed');
        table.put(0x22, '\u09ee');
        table.put(0x23, '\u09ef');
        table.put(0x24, '\u09df');
        table.put(0x25, '\u09e0');
        table.put(0x26, '\u09e1');
        table.put(0x27, '\u09e2');
        table.put(0x2a, '\u09e3');
        table.put(0x2b, '\u09f2');
        table.put(0x2c, '\u09f3');
        table.put(0x2d, '\u09f4');
        table.put(0x2e, '\u09f5');

        table.put(0x30, '\u09f6');
        table.put(0x31, '\u09f7');
        table.put(0x32, '\u09f8');
        table.put(0x33, '\u09f9');
        table.put(0x34, '\u09fa');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the gujarati national language
        // table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.GUJARATI, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0964');
        table.put(0x1a, '\u0965');
        table.put(0x1c, '\u0ae6');
        table.put(0x1d, '\u0ae7');
        table.put(0x1e, '\u0ae8');
        table.put(0x1f, '\u0ae9');

        table.put(0x20, '\u0aea');
        table.put(0x21, '\u0aeb');
        table.put(0x22, '\u0aec');
        table.put(0x23, '\u0aed');
        table.put(0x24, '\u0aee');
        table.put(0x25, '\u0aef');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the hindi national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.HINDI, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0964');
        table.put(0x1a, '\u0965');
        table.put(0x1c, '\u0966');
        table.put(0x1d, '\u0967');
        table.put(0x1e, '\u0968');
        table.put(0x1f, '\u0969');

        table.put(0x20, '\u096a');
        table.put(0x21, '\u096b');
        table.put(0x22, '\u096c');
        table.put(0x23, '\u096d');
        table.put(0x24, '\u096e');
        table.put(0x25, '\u096f');
        table.put(0x26, '\u0951');
        table.put(0x27, '\u0952');
        table.put(0x2a, '\u0953');
        table.put(0x2b, '\u0954');
        table.put(0x2c, '\u0958');
        table.put(0x2d, '\u0959');
        table.put(0x2e, '\u095a');

        table.put(0x30, '\u095b');
        table.put(0x31, '\u095c');
        table.put(0x32, '\u095d');
        table.put(0x33, '\u095e');
        table.put(0x34, '\u095f');
        table.put(0x35, '\u0960');
        table.put(0x36, '\u0961');
        table.put(0x37, '\u0962');
        table.put(0x38, '\u0963');
        table.put(0x39, '\u0970');
        table.put(0x3a, '\u0971');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the kannada national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.KANNADA, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0964');
        table.put(0x1a, '\u0965');
        table.put(0x1c, '\u0ce6');
        table.put(0x1d, '\u0ce7');
        table.put(0x1e, '\u0ce8');
        table.put(0x1f, '\u0ce9');

        table.put(0x20, '\u0cea');
        table.put(0x21, '\u0ceb');
        table.put(0x22, '\u0cec');
        table.put(0x23, '\u0ced');
        table.put(0x24, '\u0cee');
        table.put(0x25, '\u0cef');
        table.put(0x26, '\u0cde');
        table.put(0x27, '\u0cf1');
        table.put(0x2a, '\u0cf2');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the malayalam national language
        // table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.MALAYALAM, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0964');
        table.put(0x1a, '\u0965');
        table.put(0x1c, '\u0d66');
        table.put(0x1d, '\u0d67');
        table.put(0x1e, '\u0d68');
        table.put(0x1f, '\u0d69');

        table.put(0x20, '\u0d6a');
        table.put(0x21, '\u0d6b');
        table.put(0x22, '\u0d6c');
        table.put(0x23, '\u0d6d');
        table.put(0x24, '\u0d6e');
        table.put(0x25, '\u0d6f');
        table.put(0x26, '\u0d70');
        table.put(0x27, '\u0d71');
        table.put(0x2a, '\u0d72');
        table.put(0x2b, '\u0d73');
        table.put(0x2c, '\u0d74');
        table.put(0x2d, '\u0d75');
        table.put(0x2e, '\u0d7a');

        table.put(0x30, '\u0d7b');
        table.put(0x31, '\u0d7c');
        table.put(0x32, '\u0d7d');
        table.put(0x33, '\u0d7e');
        table.put(0x34, '\u0d7f');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the oriya national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.ORIYA, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0964');
        table.put(0x1a, '\u0965');
        table.put(0x1c, '\u0b66');
        table.put(0x1d, '\u0b67');
        table.put(0x1e, '\u0b68');
        table.put(0x1f, '\u0b69');

        table.put(0x20, '\u0b6a');
        table.put(0x21, '\u0b6b');
        table.put(0x22, '\u0b6c');
        table.put(0x23, '\u0b6d');
        table.put(0x24, '\u0b6e');
        table.put(0x25, '\u0b6f');
        table.put(0x26, '\u0b5c');
        table.put(0x27, '\u0b5d');
        table.put(0x2a, '\u0b5f');
        table.put(0x2b, '\u0b70');
        table.put(0x2c, '\u0b71');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the punjabi national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.PUNJABI, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0964');
        table.put(0x1a, '\u0965');
        table.put(0x1c, '\u0a66');
        table.put(0x1d, '\u0a67');
        table.put(0x1e, '\u0a68');
        table.put(0x1f, '\u0a69');

        table.put(0x20, '\u0a6a');
        table.put(0x21, '\u0a6b');
        table.put(0x22, '\u0a6c');
        table.put(0x23, '\u0a6d');
        table.put(0x24, '\u0a6e');
        table.put(0x25, '\u0a6f');
        table.put(0x26, '\u0a59');
        table.put(0x27, '\u0a5a');
        table.put(0x2a, '\u0a5b');
        table.put(0x2b, '\u0a5c');
        table.put(0x2c, '\u0a5e');
        table.put(0x2d, '\u0a75');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the tamil national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.TAMIL, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0964');
        table.put(0x1a, '\u0965');
        table.put(0x1c, '\u0be6');
        table.put(0x1d, '\u0be7');
        table.put(0x1e, '\u0be8');
        table.put(0x1f, '\u0be9');

        table.put(0x20, '\u0bea');
        table.put(0x21, '\u0beb');
        table.put(0x22, '\u0bec');
        table.put(0x23, '\u0bed');
        table.put(0x24, '\u0bee');
        table.put(0x25, '\u0bef');
        table.put(0x26, '\u0bf3');
        table.put(0x27, '\u0bf4');
        table.put(0x2a, '\u0bf5');
        table.put(0x2b, '\u0bf6');
        table.put(0x2c, '\u0bf7');
        table.put(0x2d, '\u0bf8');
        table.put(0x2e, '\u0bfa');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the telugu national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.TELUGU, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x1c, '\u0c66');
        table.put(0x1d, '\u0c67');
        table.put(0x1e, '\u0c68');
        table.put(0x1f, '\u0c69');

        table.put(0x20, '\u0c6a');
        table.put(0x21, '\u0c6b');
        table.put(0x22, '\u06cc');
        table.put(0x23, '\u06cd');
        table.put(0x24, '\u0c6e');
        table.put(0x25, '\u0c6f');
        table.put(0x26, '\u0c58');
        table.put(0x27, '\u0c59');
        table.put(0x2a, '\u0c78');
        table.put(0x2b, '\u0c79');
        table.put(0x2c, '\u0c7a');
        table.put(0x2d, '\u0c7b');
        table.put(0x2e, '\u0c7c');

        table.put(0x30, '\u0c7d');
        table.put(0x31, '\u0c7e');
        table.put(0x32, '\u0c7f');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the single shift table for the urdu national language table
        table = copyTable(gsmExtendedToCharTables.get(GsmLanguage.DEFAULT));
        gsmExtendedToCharTables.put(GsmLanguage.URDU, table);
        table.put(0x00, '\u0040');
        table.put(0x01, '\u00a3');
        table.put(0x02, '\u0024');
        table.put(0x03, '\u00a5');
        table.put(0x04, '\u00bf');
        table.put(0x05, '\u0022');
        table.put(0x06, '\u00a4');
        table.put(0x07, '\u0025');
        table.put(0x08, '\u0026');
        table.put(0x09, '\'');
        table.put(0x0b, '\u002a');
        table.put(0x0c, '\u002b');
        table.put(0x0e, '\u002d');
        table.put(0x0f, '\u002f');

        table.put(0x10, '\u003c');
        table.put(0x11, '\u003d');
        table.put(0x12, '\u003e');
        table.put(0x13, '\u00a1');
        table.put(0x15, '\u00a1');
        table.put(0x16, '\u005f');
        table.put(0x17, '\u0023');
        table.put(0x18, '\u002a');
        table.put(0x19, '\u0600');
        table.put(0x1a, '\u0601');
        table.put(0x1c, '\u06f0');
        table.put(0x1d, '\u06f1');
        table.put(0x1e, '\u06f2');
        table.put(0x1f, '\u06f3');

        table.put(0x20, '\u06f4');
        table.put(0x21, '\u06f5');
        table.put(0x22, '\u06f6');
        table.put(0x23, '\u06f7');
        table.put(0x24, '\u06f8');
        table.put(0x25, '\u06f9');
        table.put(0x26, '\u060c');
        table.put(0x27, '\u060d');
        table.put(0x2a, '\u060e');
        table.put(0x2b, '\u060f');
        table.put(0x2c, '\u0610');
        table.put(0x2d, '\u0611');
        table.put(0x2e, '\u0612');

        table.put(0x30, '\u0613');
        table.put(0x31, '\u0614');
        table.put(0x32, '\u061b');
        table.put(0x33, '\u061f');
        table.put(0x34, '\u0640');
        table.put(0x35, '\u0652');
        table.put(0x36, '\u0658');
        table.put(0x37, '\u066b');
        table.put(0x38, '\u066c');
        table.put(0x39, '\u0672');
        table.put(0x3a, '\u0673');
        table.put(0x3b, '\u06cd');
        table.put(0x3f, '\u06d4');

        table.put(0x41, '\u0041');
        table.put(0x42, '\u0042');
        table.put(0x43, '\u0043');
        table.put(0x44, '\u0044');
        table.put(0x45, '\u0045');
        table.put(0x46, '\u0046');
        table.put(0x47, '\u0047');
        table.put(0x48, '\u0048');
        table.put(0x49, '\u0049');
        table.put(0x4a, '\u004a');
        table.put(0x4b, '\u004b');
        table.put(0x4c, '\u004c');
        table.put(0x4d, '\u004d');
        table.put(0x4e, '\u004e');
        table.put(0x4f, '\u004f');

        table.put(0x50, '\u0050');
        table.put(0x51, '\u0051');
        table.put(0x52, '\u0052');
        table.put(0x53, '\u0053');
        table.put(0x54, '\u0054');
        table.put(0x55, '\u0055');
        table.put(0x56, '\u0056');
        table.put(0x57, '\u0057');
        table.put(0x58, '\u0058');
        table.put(0x59, '\u0059');
        table.put(0x5a, '\u005a');

        // Create the reverse lookup table for gsm characters
        for (GsmLanguage language : GsmLanguage.values()) {
            table = gsmToCharTables.get(language);
            SparseIntArray revTable = new SparseIntArray();
            charToGsmTables.put(language, revTable);
            int size = table.size();
            for (i = 0; i < size; i++) {
                revTable.put(table.valueAt(i), table.keyAt(i));
            }
        }

        // Create the reverse lookup tables for extended gsm characters
        for (GsmLanguage language : GsmLanguage.values()) {
            table = gsmExtendedToCharTables.get(language);
            SparseIntArray revTable = new SparseIntArray();
            charToGsmExtendedTables.put(language, revTable);
            int size = table.size();
            for (i = 0; i < size; i++) {
                revTable.put(table.valueAt(i), table.keyAt(i));
            }
        }
    }

    private static SparseIntArray copyTable(SparseIntArray src) {
        SparseIntArray dest = new SparseIntArray();
        int size = src.size();
        for (int i = 0; i < size; i++) {
            dest.put(src.keyAt(i), src.valueAt(i));
        }
        return dest;
    }
}
