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

package com.android.unit_tests;

import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.gsm.GsmAlphabet;
import com.android.internal.telephony.gsm.GsmAlphabet.GsmLanguage;
import com.android.internal.util.HexDump;

import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsMessage.SubmitPdu;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;

public class SmsNationalCharactersTest extends AndroidTestCase {

    private static final String gsm7bit_default_short =
        "0123456789";

    private static final String gsm7bit_default_160_chars =
        "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
        + "0123456789012345678901234567890123456789012345678901234567890123456789";

    private static final String ucs2_short =
        "\ubabe123456789";

    private static final String ucs2_70_chars =
        "\ubabe123456789012345678901234567890123456789012345678901234567890123456789";

    private static final String gsm7bit_turkish_short =
        "\u011e\u0130\u015e\u00e7\u011f\u0131\u015f";

    private static final String gsm7bit_spanish_short =
        "\u00e7\u00c1\u00cd\u00d3\u00da\u00e1\u00ed\u00f3\u00fa";

    private static final String gsm7bit_portuguese_short =
        "\u00ea\u00e7\u00d4\u00f4\u00c1\u00e1\u03a6\u0393\u03a9\u03a0\u03a8\u03a3\u0398\u00ca\u00c0"
        + "\u00cd\u00d3\u00da\u00c3\u00d5\u00c2\u00ed\u00f3\u00fa\u00e3\u00f5\u00e2";

    private static final String arbitrary_deliver_pdu_head =
        "00440680214365000080114080545340";

    private final SmsManager smsManager = SmsManager.getDefault();

    @SmallTest
    public void testDivideShortGsm7bitOnly() throws Exception {
        String text = gsm7bit_default_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message divided into more than one part", 1, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideLongGsm7bitOnly() throws Exception {
        String text = gsm7bit_default_160_chars + gsm7bit_default_160_chars;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message not divided into three parts", 3, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideShortUSC2() throws Exception {
        String text = ucs2_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message divided into more than one part", 1, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideLongUSC2() throws Exception {
        String text = ucs2_70_chars + ucs2_70_chars;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message not divided into three parts", 3, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideShortTurkish() throws Exception {
        String text = gsm7bit_turkish_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message divided into more than one part", 1, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideLongTurkish() throws Exception {
        String text = gsm7bit_turkish_short + gsm7bit_default_160_chars + gsm7bit_turkish_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message not divided into two parts", 2, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideShortSpanish() throws Exception {
        String text = gsm7bit_spanish_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message divided into more than one part", 1, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideLongSpanish() throws Exception {
        String text = gsm7bit_spanish_short + gsm7bit_default_160_chars + gsm7bit_spanish_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message not divided into two parts", 2, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideShortPortuguese() throws Exception {
        String text = gsm7bit_portuguese_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message divided into more than one part", 1, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testDivideLongPortuguese() throws Exception {
        String text = gsm7bit_portuguese_short + gsm7bit_default_160_chars
                + gsm7bit_portuguese_short;

        ArrayList<String> parts = smsManager.divideMessage(text);

        assertNotNull("Message divided into no parts: null", parts);
        assertEquals("Message not divided into two parts", 2, parts.size());
        assertTrue("Message corrupted during division", comparePartsToString(parts, text));
    }

    @SmallTest
    public void testEncodeAndDecodeTurkishSingleshift() throws Exception {
        String text = gsm7bit_turkish_short;

        GsmLanguage extensionLanguage = GsmAlphabet.getExtensionLanguage(text);

        assertEquals("Wrong extension table", GsmLanguage.TURKISH, extensionLanguage);

        SmsHeader smsHeader = new SmsHeader();
        smsHeader.singleShiftLanguage = extensionLanguage;

        SubmitPdu pdu = SmsMessage.getSubmitPdu(null, "123456", text, false, smsHeader);
        assertNotNull("getSubmitPdu returned null", pdu);

        String submitPdu = HexDump.toHexString(pdu.encodedMessage);
        assertEquals("Message not correctly coded",
                "4100068121436500001303240101D81C37C9CD7433DE9C37E9CD1C", submitPdu);

        SmsMessage msg = SmsMessage.newFromCDS(submitToDeliver(submitPdu));
        assertNotNull("Could not create message", msg);

        String body = msg.getDisplayMessageBody();
        assertNotNull("Body null", body);
        assertEquals("Pdu not correctly decoded", text, body);
    }

    @SmallTest
    public void testEncodeAndDecodeSpanishSingleshift() throws Exception {
        String text = gsm7bit_spanish_short;

        GsmLanguage extensionLanguage = GsmAlphabet.getExtensionLanguage(text);

        assertEquals("Wrong extension table", GsmLanguage.SPANISH, extensionLanguage);

        SmsHeader smsHeader = new SmsHeader();
        smsHeader.singleShiftLanguage = extensionLanguage;

        SubmitPdu pdu = SmsMessage.getSubmitPdu(null, "123456", text, false, smsHeader);
        assertNotNull("getSubmitPdu returned null", pdu);

        String submitPdu = HexDump.toHexString(pdu.encodedMessage);
        assertEquals("Message not correctly coded",
                "4100068121436500001703240102D82436C14D72F3DC5437E14D7AF3DED401", submitPdu);

        SmsMessage msg = SmsMessage.newFromCDS(submitToDeliver(submitPdu));
        assertNotNull("Could not create message", msg);

        String body = msg.getDisplayMessageBody();
        assertNotNull("Body null", body);
        assertEquals("Pdu not correctly decoded", text, body);
    }

    @SmallTest
    public void testEncodeAndDecodePortugueseSingleshift() throws Exception {
        String text = gsm7bit_portuguese_short;

        GsmLanguage extensionLanguage = GsmAlphabet.getExtensionLanguage(text);

        assertEquals("Wrong extension table", GsmLanguage.PORTUGUESE, extensionLanguage);

        SmsHeader smsHeader = new SmsHeader();
        smsHeader.singleShiftLanguage = extensionLanguage;

        SubmitPdu pdu = SmsMessage.getSubmitPdu(null, "123456", text, false, smsHeader);
        assertNotNull("getSubmitPdu returned null", pdu);

        String submitPdu = HexDump.toHexString(pdu.encodedMessage);
        assertEquals(
                "Message not correctly coded",
                "4100068121436500003403240103D8143689CD62C3D838360FC9A462B960329BCF26B8496E9E9BEA66"
                + "BBE16EC29BF4E6BDA96FF61BFEE60F",
                submitPdu);

        SmsMessage msg = SmsMessage.newFromCDS(submitToDeliver(submitPdu));
        assertNotNull("Could not create message", msg);

        String body = msg.getDisplayMessageBody();
        assertNotNull("Body null", body);
        assertEquals("Pdu not correctly decoded", text, body);
    }

    @SmallTest
    public void testEncodeAndDecodeSingleshift() throws Exception {

        for (GsmLanguage language : GsmLanguage.values()) {

            if (language != GsmLanguage.DEFAULT) {
                GsmAlphabet alphabet = GsmAlphabet.getAlphabet(GsmLanguage.DEFAULT, language);

                String text = getAllExtendedCharacters(alphabet);

                GsmLanguage extensionLanguage = GsmAlphabet.getExtensionLanguage(text);

                assertEquals("Wrong extension table (language: " + language + ")", language,
                        extensionLanguage);

                SmsHeader smsHeader = new SmsHeader();
                smsHeader.singleShiftLanguage = extensionLanguage;

                SubmitPdu submitPdu = SmsMessage.getSubmitPdu(null, "123456", text, false,
                        smsHeader);
                assertNotNull("getSubmitPdu returned null (language: " + language + ")", submitPdu);

                SmsMessage msg = SmsMessage.newFromCDS(submitToDeliver(HexDump
                        .toHexString(submitPdu.encodedMessage)));
                assertNotNull("Could not create message (language: " + language + ")", msg);

                String body = msg.getDisplayMessageBody();
                assertNotNull("Body null (language: " + language + ")", body);
                assertEquals("Pdu not correctly decoded (language: " + language + ")", text, body);
            }
        }
    }

    @SmallTest
    public void testDecodeLockingShift() throws Exception {

        for (GsmLanguage language : GsmLanguage.values()) {

            GsmAlphabet alphabet = GsmAlphabet.getAlphabet(language, GsmLanguage.DEFAULT);

            String text = getAllDefaultCharacters(alphabet);
            byte[] header = {
                    0x25, 0x01, (byte)language.getLanguageCode()
            };

            byte[] userdata = alphabet.stringToGsm7BitPackedWithHeader(text, header);
            assertNotNull("Could not create userdata (language: " + language + ")", userdata);

            String deliverPdu = arbitrary_deliver_pdu_head + HexDump.toHexString(userdata);

            SmsMessage msg = SmsMessage.newFromCDS(deliverPdu);
            assertNotNull("Could not create message from: " + deliverPdu + " (language: "
                    + language + ")", msg);

            String body = msg.getDisplayMessageBody();
            assertNotNull("Could not get body from message, pdu: " + deliverPdu + " (language: "
                    + language + ")", body);
            assertEquals("Pdu not correctly decoded" + " (language: " + language + ")", text, body);
        }
    }

    private static boolean comparePartsToString(ArrayList<String> parts, String text) {
        if (parts != null && text != null) {
            StringBuilder sb = new StringBuilder();
            for (String str : parts) {
                sb.append(str);
            }
            if (sb.toString().equals(text)) {
                return true;
            }
        }
        return false;
    }

    private static String submitToDeliver(String submitPdu) {
        return arbitrary_deliver_pdu_head + submitPdu.substring(18);
    }

    private static String getAllDefaultCharacters(GsmAlphabet alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 0x80; i++) {
            if (i != GsmAlphabet.GSM_EXTENDED_ESCAPE) {
                sb.append(alphabet.gsmToChar(i));
            }
        }
        return sb.toString();
    }

    private String getAllExtendedCharacters(GsmAlphabet alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 0x80; i++) {
            if (i != 0x0a && i != 0x0d && i != GsmAlphabet.GSM_EXTENDED_ESCAPE) {
                char c = alphabet.gsmExtendedToChar(i);
                if (c != alphabet.gsmToChar(i)) {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

}
