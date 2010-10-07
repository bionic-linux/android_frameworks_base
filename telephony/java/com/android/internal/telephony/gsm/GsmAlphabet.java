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

import com.android.internal.telephony.EncodeException;

import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class implements the character set mapping between the GSM SMS 7-bit
 * alphabet specified in TS 23.038 6.2.1 and UTF-16 with regards to the national
 * language properties provided in the constructor
 *
 * {@hide}
 */
public class GsmAlphabet {

    private static final String LOG_TAG = "GSM";

    public static enum GsmLanguage {
        /*
         * Note that it is important that the languages appear in the same
         * order as they appear in 3GPP 23.038 to make ordinal match the
         * language code.
         * We do not want to use a constructor since we want to exploit
         * ordinal/length in code.
         */
        DEFAULT, TURKISH, SPANISH, PORTUGUESE, BENGALI, GUJARATI, HINDI,
        KANNADA, MALAYALAM, ORIYA, PUNJABI, TAMIL, TELUGU, URDU;

        /**
         * Factory method to Returns a GsmLanguage based on the languageCode
         * provided
         *
         * @return a GsmLanguage object based on the languageCode provided
         */
        public static GsmLanguage toGsmLanguage(int languageCode) {
            if (languageCode >= 0 && languageCode < values().length) {
                return values()[languageCode];
            } else {
                return null;
            }
        }

        public int getLanguageCode() {
            return ordinal();
        }
    }

    public static final byte GSM_EXTENDED_ESCAPE = 0x1B;

    // Conversion tables for all supported languages
    private static final Map<GsmLanguage, SparseIntArray> sCharToGsmTables =
        new HashMap<GsmLanguage, SparseIntArray>();

    private static final Map<GsmLanguage, SparseIntArray> sGsmToCharTables =
        new HashMap<GsmLanguage, SparseIntArray>();

    private static final Map<GsmLanguage, SparseIntArray> sCharToGsmExtendedTables =
        new HashMap<GsmLanguage, SparseIntArray>();

    private static final Map<GsmLanguage, SparseIntArray> sGsmExtendedToCharTables =
        new HashMap<GsmLanguage, SparseIntArray>();

    // A cache array to keep the alphabets which uses the default GSM alphabet
    // and an extension table
    private static final GsmAlphabet[] sExtendedDefaultAlphabets =
        new GsmAlphabet[GsmLanguage.values().length];

    // A cache map to keep the alphabets which uses a locking shift alphabet
    private static final int MAX_NBR_CACHED_ALPHABETS = 10;

    private static Map<Pair<GsmLanguage, GsmLanguage>, GsmAlphabet> sAlphabetCache =
        new HashMap<Pair<GsmLanguage, GsmLanguage>, GsmAlphabet>(MAX_NBR_CACHED_ALPHABETS);

    // The default gsm7bit alphabet
    private static final SparseIntArray sGsm7bitAlphabet;

    // We keep the data from getExtensionLanguage to speed up analysis
    private static List<GsmLanguage> sLastCandidateAlphabets;

    private static String sLastAnalyzedText = null;

    // Static initializer to setup static members
    static {
        // Get all available tables
        GsmAlphabetTables.init(sCharToGsmTables, sGsmToCharTables, sCharToGsmExtendedTables,
                sGsmExtendedToCharTables);

        // Setup the default alphabets for fast retreival
        for (GsmLanguage language : GsmLanguage.values()) {
            sExtendedDefaultAlphabets[language.getLanguageCode()] = new GsmAlphabet(
                    GsmLanguage.DEFAULT, language);
        }

        sGsm7bitAlphabet = sCharToGsmTables.get(GsmLanguage.DEFAULT);

        sLastCandidateAlphabets = getNewCandidateAlphabets();
    }

    private final int mGsmSpaceChar;

    private final SparseIntArray mCharToGsmTable;

    private final SparseIntArray mGsmToCharTable;

    private final SparseIntArray mCharToGsmExtendedTable;

    private final SparseIntArray mGsmExtendedToCharTable;

    /**
     * This escapes extended characters, and when present indicates that the
     * following character should be looked up in the "extended" table
     *
     * gsmToChar(GSM_EXTENDED_ESCAPE) returns 0xffff
     */

    private GsmAlphabet(GsmLanguage lockingshift, GsmLanguage singleshift) {
        mCharToGsmTable = sCharToGsmTables.get(lockingshift);
        mGsmToCharTable = sGsmToCharTables.get(lockingshift);
        mCharToGsmExtendedTable = sCharToGsmExtendedTables.get(singleshift);
        mGsmExtendedToCharTable = sGsmExtendedToCharTables.get(singleshift);
        mGsmSpaceChar = mCharToGsmTable.get(' ');
    }

    /**
     * Returns an alphabet based on the parameters provided
     *
     * @param alphabet The language used for locking shift
     * @param extension The language used for single shift
     * @return an NationalGsmAlphabet instance
     */
    public static synchronized GsmAlphabet getAlphabet(GsmLanguage alphabet,
            GsmLanguage extension) {
        if (alphabet == GsmLanguage.DEFAULT) {
            // This is a default alphabet with extension table
            return sExtendedDefaultAlphabets[extension.getLanguageCode()];
        } else {
            // This is a locking shift alphabet
            Pair<GsmLanguage, GsmLanguage> languagePair = new Pair<GsmLanguage, GsmLanguage>(
                    alphabet, extension);
            if (sAlphabetCache.containsKey(languagePair)) {
                // This is a cached alphabet, return it
                return sAlphabetCache.get(languagePair);
            } else {
                GsmAlphabet nationalAlphabet = new GsmAlphabet(alphabet, extension);
                sAlphabetCache.put(languagePair, nationalAlphabet);
                return nationalAlphabet;
            }
        }
    }

    /**
     * Calculates what gsm7bit single shift extension table needed to encode a
     * text, using default gsm7bit alphabet for locking shift
     *
     * @param text The text to scan
     * @return the calculated gsm7bit encoding language
     */
    public static synchronized GsmLanguage getExtensionLanguage(CharSequence text) {
        List<GsmLanguage> candidateAlphabets = null;
        CharSequence unanalyzedText = text;
        int unanalyzedTextLength = unanalyzedText.length();

        if (sLastAnalyzedText != null) {
            // This is an optimization that covers the case where a text is
            // growing, i.e. characters are added to the end of a previously
            // analyzed text. In this case we dont have to re-evaluate alphabets
            // that have already been ruled out as candidates for the existing
            // text

            int lastTextLength = sLastAnalyzedText.length();
            if (unanalyzedTextLength > lastTextLength
                    && text.subSequence(0, lastTextLength).equals(sLastAnalyzedText)) {
                // This is the last analyzed text with an addition at the end,
                // we only
                // have to analyze the added text. We can re-use the remaining
                // alphabet
                // candidates from the last run
                unanalyzedText = text.subSequence(lastTextLength, unanalyzedTextLength);
                unanalyzedTextLength = unanalyzedText.length();
                candidateAlphabets = sLastCandidateAlphabets;
            }
        }

        if (candidateAlphabets == null) {
            // We cannot use any information from previous runs
            candidateAlphabets = getNewCandidateAlphabets();
        }

        // Now analyze (parts of the) the text
        for (int i = 0; i < unanalyzedTextLength && candidateAlphabets.size() > 0; i++) {
            int c = unanalyzedText.charAt(i);
            if (sGsm7bitAlphabet.indexOfKey(c) < 0) {
                // The character is not in the gsm7bitAlphabet, loop through all
                // remaining extension tables to see if it is any of them
                Iterator<GsmLanguage> it = candidateAlphabets.iterator();
                while (it.hasNext()) {
                    GsmLanguage language = it.next();
                    if (sCharToGsmExtendedTables.get(language).indexOfKey(c) < 0) {
                        // We found a language that does not contain the
                        // character, remove it from the candidate alphabets
                        it.remove();
                    }
                }
            }
        }

        sLastCandidateAlphabets = candidateAlphabets;
        sLastAnalyzedText = text.toString();

        if (candidateAlphabets.size() > 0) {
            // We have (at least) one alphabet that contains all characters
            return candidateAlphabets.get(0);
        } else {
            // We need unicode for this
            return null;
        }
    }

    private static List<GsmLanguage> getNewCandidateAlphabets() {
        List<GsmLanguage> list = new LinkedList<GsmLanguage>();
        for (GsmLanguage language : GsmLanguage.values()) {
            list.add(language);
        }
        return list;
    }

    /**
     * char to GSM alphabet char
     *
     * @param c the character to convert
     * @return Returns ' ' in GSM alphabet if there's no possible match
     *         Returns GSM_EXTENDED_ESCAPE if this character is in the extended
     *         table. In this case, you must call charToGsmExtended() for the
     *         value that should follow GSM_EXTENDED_ESCAPE in the GSM alphabet
     *         string
     */
    public int charToGsm(char c) {
        try {
            return charToGsm(c, false);
        } catch (EncodeException ex) {
            // this should never happen
            return mGsmSpaceChar;
        }
    }

    /**
     * char to GSM alphabet char
     *
     * @param throwException If true, throws EncodeException on invalid char. If
     *        false, returns GSM alphabet ' ' char.
     * @return Returns GSM_EXTENDED_ESCAPE if this character is in the extended
     *         table. In this case, you must call charToGsmExtended() for the
     *         value that should follow GSM_EXTENDED_ESCAPE in the GSM alphabet
     *         string
     */

    public int charToGsm(char c, boolean throwException) throws EncodeException {
        int ret;

        ret = mCharToGsmTable.get(c, -1);

        if (ret == -1) {
            ret = mCharToGsmExtendedTable.get(c, -1);

            if (ret == -1) {
                if (throwException) {
                    throw new EncodeException(c);
                } else {
                    return mGsmSpaceChar;
                }
            } else {
                return GSM_EXTENDED_ESCAPE;
            }
        }

        return ret;

    }

    /**
     * char to extended GSM alphabet char
     * Extended chars should be escaped with GSM_EXTENDED_ESCAPE
     *
     * @param c the character to convert
     * @return Returns ' ' in GSM alphabet if there's no possible match
     *
     */
    public int charToGsmExtended(char c) {
        int ret;

        ret = mCharToGsmExtendedTable.get(c, -1);

        if (ret == -1) {
            return mGsmSpaceChar;
        }

        return ret;
    }

    /**
     * Converts a character in the GSM alphabet into a char
     *
     * @param gsmChar the character to convert
     * @return if GSM_EXTENDED_ESCAPE is passed, 0xffff is returned. In this
     *         case, the following character in the stream should be decoded
     *         with gsmExtendedToChar(). If an unmappable value is passed
     *         (one greater than 127), ' ' is returned
     */

    public char gsmToChar(int gsmChar) {
        return (char)mGsmToCharTable.get(gsmChar, ' ');
    }

    /**
     * Converts a character in the extended GSM alphabet into a char
     *
     * @param gsmChar the character to convert
     * @return if GSM_EXTENDED_ESCAPE is passed, ' ' is returned since no second
     *         extension page has yet been defined (see Note 1 in Table 6.2.1.1
     *         of TS 23.038 v7.00)
     */

    public char gsmExtendedToChar(int gsmChar) {
        int ret;

        ret = mGsmExtendedToCharTable.get(gsmChar, -1);

        if (ret == -1) {
            // A miss in the extension table should fall back to the default
            // table
            return gsmToChar(gsmChar);
        }
        return (char)ret;
    }

    /**
     * Converts a String into a byte array containing the 7-bit packed GSM
     * Alphabet representation of the string. If a header is provided, this is
     * included in the returned byte array and padded to a septet boundary.
     *
     * Unencodable chars are encoded as spaces.
     *
     * Byte 0 in the returned byte array is the count of septets used, including
     * the header and header padding. The returned byte array is the minimum
     * size required to store the packed septets. The returned array cannot
     * contain more than 255 septets.
     *
     * @param data The text string to encode.
     * @param header Optional header (including length byte) that precedes the
     *        encoded data, padded to septet boundary.
     * @return Byte array containing header and encoded data.
     */
    public byte[] stringToGsm7BitPackedWithHeader(String data, byte[] header)
            throws EncodeException {
        if (header == null || header.length == 0) {
            return stringToGsm7BitPacked(data);
        }

        int headerBits = (header.length + 1) * 8;
        int headerSeptets = (headerBits + 6) / 7;

        byte[] ret = stringToGsm7BitPacked(data, headerSeptets, true);

        // Paste in the header
        ret[1] = (byte)header.length;
        System.arraycopy(header, 0, ret, 2, header.length);
        return ret;
    }

    /**
     * Converts a String into a byte array containing the 7-bit packed GSM
     * Alphabet representation of the string.
     *
     * Unencodable chars are encoded as spaces.
     *
     * Byte 0 in the returned byte array is the count of septets used The
     * returned byte array is the minimum size required to store the packed
     * septets. The returned array cannot contain more than 255 septets.
     *
     * @param data the data string to encode
     * @throws EncodeException if string is too large to encode
     */
    public byte[] stringToGsm7BitPacked(String data) throws EncodeException {
        return stringToGsm7BitPacked(data, 0, true);
    }

    /**
     * Converts a String into a byte array containing the 7-bit packed GSM
     * Alphabet representation of the string.
     *
     * @param data the text to convert to septets
     * @param startingSeptetOffset the number of padding septets to put before
     *        the character data at the beginning of the array
     * @param throwException If true, throws EncodeException on invalid char. If
     *        false, replaces unencodable char with GSM alphabet space char.
     * @return Byte 0 in the returned byte array is the count of septets used.
     *         The returned byte array is the minimum size required to store the
     *         packed septets. The returned array cannot contain more than 255
     *         septets.
     * @throws EncodeException if string is too large to encode
     */
    public byte[] stringToGsm7BitPacked(String data, int startingSeptetOffset,
            boolean throwException) throws EncodeException {
        int dataLen = data.length();
        int septetCount = countGsmSeptets(data, throwException) + startingSeptetOffset;
        if (septetCount > 255) {
            throw new EncodeException("Payload cannot exceed 255 septets");
        }
        int byteCount = ((septetCount * 7) + 7) / 8;
        byte[] ret = new byte[byteCount + 1]; // Include space for one byte length prefix.
        for (int i = 0, septets = startingSeptetOffset, bitOffset = startingSeptetOffset * 7;
                i < dataLen && septets < septetCount; i++, bitOffset += 7) {
            char c = data.charAt(i);
            int v = charToGsm(c, throwException);
            if (v == GSM_EXTENDED_ESCAPE) {
                v = charToGsmExtended(c); // Lookup the extended char.
                packSmsChar(ret, bitOffset, GSM_EXTENDED_ESCAPE);
                bitOffset += 7;
                septets++;
            }
            packSmsChar(ret, bitOffset, v);
            septets++;
        }
        ret[0] = (byte)(septetCount); // Validated by check above.

        return ret;
    }

    /**
     * Pack a 7-bit char into its appropriate place in a byte array
     *
     * @param bitOffset the bit offset that the septet should be packed at
     *        (septet index * 7)
     */
    private void packSmsChar(byte[] packedChars, int bitOffset, int value) {
        int byteOffset = bitOffset / 8;
        int shift = bitOffset % 8;

        packedChars[++byteOffset] |= value << shift;

        if (shift > 1) {
            packedChars[++byteOffset] = (byte)(value >> (8 - shift));
        }
    }

    /**
     * Convert a GSM alphabet 7 bit packed string (SMS string) into a
     * {@link java.lang.String}.
     *
     * See TS 23.038 6.1.2.1 for SMS Character Packing
     *
     * @param pdu the raw data from the pdu
     * @param offset the byte offset of
     * @param lengthSeptets string length in septets, not bytes
     * @return string representation or null on decoding exception
     */
    public String gsm7BitPackedToString(byte[] pdu, int offset, int lengthSeptets) {
        return gsm7BitPackedToString(pdu, offset, lengthSeptets, 0);
    }

    /**
     * Convert a GSM alphabet 7 bit packed string (SMS string) into a
     * {@link java.lang.String}.
     *
     * See TS 23.038 6.1.2.1 for SMS Character Packing
     *
     * @param pdu the raw data from the pdu
     * @param offset the byte offset
     * @param lengthSeptets string length in septets, not bytes
     * @param numPaddingBits the number of padding bits before the start of the
     *        string in the first byte
     * @return string representation or null on decoding exception
     */
    public String gsm7BitPackedToString(byte[] pdu, int offset, int lengthSeptets,
            int numPaddingBits) {
        StringBuilder ret = new StringBuilder(lengthSeptets);
        boolean prevCharWasEscape;

        try {
            prevCharWasEscape = false;

            for (int i = 0; i < lengthSeptets; i++) {
                int bitOffset = (7 * i) + numPaddingBits;

                int byteOffset = bitOffset / 8;
                int shift = bitOffset % 8;
                int gsmVal;

                gsmVal = (0x7f & (pdu[offset + byteOffset] >> shift));

                // if it crosses a byte boundary
                if (shift > 1) {
                    // set MSB bits to 0
                    gsmVal &= 0x7f >> (shift - 1);

                    gsmVal |= 0x7f & (pdu[offset + byteOffset + 1] << (8 - shift));
                }

                if (prevCharWasEscape) {
                    ret.append(gsmExtendedToChar(gsmVal));
                    prevCharWasEscape = false;
                } else if (gsmVal == GSM_EXTENDED_ESCAPE) {
                    prevCharWasEscape = true;
                } else {
                    ret.append(gsmToChar(gsmVal));
                }
            }
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Error GSM 7 bit packed: ", ex);
            return null;
        }

        return ret.toString();
    }

    /**
     * Convert a GSM alphabet string that's stored in 8-bit unpacked format (as
     * it often appears in SIM records) into a string
     *
     * Field may be padded with trailing 0xff's. The decode stops at the first
     * 0xff encountered.
     */
    public String gsm8BitUnpackedToString(byte[] data, int offset, int length) {
        boolean prevWasEscape;
        StringBuilder ret = new StringBuilder(length);

        prevWasEscape = false;
        for (int i = offset; i < offset + length; i++) {
            // Never underestimate the pain that can be caused
            // by signed bytes
            int c = data[i] & 0xff;

            if (c == 0xff) {
                break;
            } else if (c == GSM_EXTENDED_ESCAPE) {
                if (prevWasEscape) {
                    // Two escape chars in a row
                    // We treat this as a space
                    // See Note 1 in Table 6.2.1.1 of TS 23.038 v7.00
                    ret.append(' ');
                    prevWasEscape = false;
                } else {
                    prevWasEscape = true;
                }
            } else {
                if (prevWasEscape) {
                    ret.append((char)mGsmExtendedToCharTable.get(c, ' '));
                } else {
                    ret.append((char)mGsmToCharTable.get(c, ' '));
                }
                prevWasEscape = false;
            }
        }

        return ret.toString();
    }

    /**
     * Convert a string into an 8-bit unpacked GSM alphabet byte array
     */
    public byte[] stringToGsm8BitPacked(String s) {
        byte[] ret;

        int septets = 0;

        septets = countGsmSeptets(s);

        // Enough for all the septets and the length byte prefix
        ret = new byte[septets];

        stringToGsm8BitUnpackedField(s, ret, 0, ret.length);

        return ret;
    }

    /**
     * Write a String into a GSM 8-bit unpacked field <code>dest</code> of size
     * <code>length</code> at given <code>offset</code>. Field is padded with
     * 0xff's, string is truncated if necessary.
     *
     * @param length size of field
     * @param offset offset into field
     * @param dest field
     */

    public void stringToGsm8BitUnpackedField(String s, byte dest[], int offset, int length) {
        int outByteIndex = offset;

        // Septets are stored in byte-aligned octets
        for (int i = 0, sz = s.length(); i < sz && (outByteIndex - offset) < length; i++) {
            char c = s.charAt(i);

            int v = charToGsm(c);

            if (v == GSM_EXTENDED_ESCAPE) {
                // make sure we can fit an escaped char
                if (!(outByteIndex + 1 - offset < length)) {
                    break;
                }

                dest[outByteIndex++] = GSM_EXTENDED_ESCAPE;

                v = charToGsmExtended(c);
            }

            dest[outByteIndex++] = (byte)v;
        }

        // pad with 0xff's
        while ((outByteIndex - offset) < length) {
            dest[outByteIndex++] = (byte)0xff;
        }
    }

    /**
     * Returns the count of 7-bit GSM alphabet characters needed to represent
     * this character. Counts unencodable char as 1 septet.
     *
     * @param c the character to count
     * @return count of 7-bit GSM alphabet characters needed to represent
     *         this character
     */
    public int countGsmSeptets(char c) {
        try {
            return countGsmSeptets(c, false);
        } catch (EncodeException ex) {
            // This should never happen.
            return 0;
        }
    }

    /**
     * Returns the count of 7-bit GSM alphabet characters needed to represent
     * this character
     *
     * @param c the character to count
     * @param throwsException if true, throws EncodeException if unencodable
     *        char. Otherwise, counts invalid char as 1 septet.
     * @return count of 7-bit GSM alphabet characters needed to represent
     *         this character
     */
    public int countGsmSeptets(char c, boolean throwsException) throws EncodeException {
        if (mCharToGsmTable.get(c, -1) != -1) {
            return 1;
        }

        if (mCharToGsmExtendedTable.get(c, -1) != -1) {
            return 2;
        }

        if (throwsException) {
            throw new EncodeException(c);
        } else {
            // count as a space char
            return 1;
        }
    }

    /**
     * Returns the count of 7-bit GSM alphabet characters needed to represent
     * this string. Counts unencodable char as 1 septet.
     *
     * @param s the character sequence to count
     * @return count of 7-bit GSM alphabet characters needed to represent
     *         this character sequence
     */
    public int countGsmSeptets(CharSequence s) {
        try {
            return countGsmSeptets(s, false);
        } catch (EncodeException ex) {
            // this should never happen
            return 0;
        }
    }

    /**
     * Returns the count of 7-bit GSM alphabet characters needed to represent
     * this string.
     *
     * @param s the character sequence to count
     * @param throwsException if true, throws EncodeException if unencodable
     *        char. Otherwise, counts invalid char as 1 septet.
     * @return count of 7-bit GSM alphabet characters needed to represent
     *         this character sequence
     */
    public int countGsmSeptets(CharSequence s, boolean throwsException) throws EncodeException {
        int charIndex = 0;
        int sz = s.length();
        int count = 0;

        while (charIndex < sz) {
            count += countGsmSeptets(s.charAt(charIndex), throwsException);
            charIndex++;
        }

        return count;
    }

    /**
     * Returns the index into <code>s</code> of the first character after
     * <code>limit</code> septets have been reached, starting at index
     * <code>start</code>. This is used when dividing messages into units within
     * the SMS message size limit.
     *
     * @param s source string
     * @param start index of where to start counting septets
     * @param limit maximum septets to include, e.g.
     *        <code>MAX_USER_DATA_SEPTETS</code>
     * @return index of first character that won't fit, or the length of the
     *         entire string if everything fits
     */
    public int findGsmSeptetLimitIndex(String s, int start, int limit) {
        int accumulator = 0;
        int size = s.length();

        for (int i = start; i < size; i++) {
            accumulator += countGsmSeptets(s.charAt(i));
            if (accumulator > limit) {
                return i;
            }
        }
        return size;
    }
}
