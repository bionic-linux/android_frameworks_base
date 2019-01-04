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

package android.net;

import android.annotation.NonNull;

import com.android.internal.util.BitUtils;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines basic data for DNS protocol based on RFC 1035.
 * Subclasses create the specific format used in DNS packet.
 *
 *
 * @hide
 */
public abstract class DnsPacket {
    public class DnsHeader {
        public final int id;
        public final int flags;
        public final int rcode;
        public final int[] mSectionCount;

        /**
         * Create a new DnsHeader from a positioned ByteBuffer.
         *
         * The ByteBuffer must be in network byte order (which is the default).
         * Reads the passed ByteBuffer from its current position and decodes a DNS header.
         * When this constructor returns, the reading position of the ByteBuffer has been
         * advanced to the end of the DNS header record.
         * This is meant to chain with other methods reading a DNS response in sequence.
         *
         */
        DnsHeader(@NonNull ByteBuffer buf) throws IndexOutOfBoundsException {
            id = BitUtils.uint16(buf.getShort());
            flags = BitUtils.uint16(buf.getShort());
            rcode = flags & 0xF;
            mSectionCount = new int[NUM_SECTIONS];
            for (int i = 0; i < NUM_SECTIONS; ++i) {
                mSectionCount[i] = BitUtils.uint16(buf.getShort());
            }
        }

        /**
         * Get section count by section type.
         */
        public int getSectionCount(int sectionType) {
            return mSectionCount[sectionType];
        }
    }

    public class DnsSection {
        private static final int MAXNAMESIZE = 255;
        private static final int MAXLABELSIZE = 63;
        private static final int MAXLABELCOUNT = 128;
        private static final int NAME_NORMAL = 0;
        private static final int NAME_COMPRESSION = 0xC0;
        private final DecimalFormat byteFormat = new DecimalFormat();

        private static final String TAG = "DnsSection";
        public final String dName;
        public final int nsType;
        public final int nsClass;
        public final long ttl;
        public final byte[] rr;

        /**
         * Create a new DnsSection from a positioned ByteBuffer.
         *
         * The ByteBuffer must be in network byte order (which is the default).
         * Reads the passed ByteBuffer from its current position and decodes a DNS section.
         * When this constructor returns, the reading position of the ByteBuffer has been
         * advanced to the end of the DNS header record.
         * This is meant to chain with other methods reading a DNS response in sequence.
         *
         */
        DnsSection(int sectionType, @NonNull ByteBuffer buf)
                throws ParseException, IndexOutOfBoundsException {
            dName = parseName(buf);
            nsType = BitUtils.uint16(buf.getShort());
            nsClass = BitUtils.uint16(buf.getShort());

            if (sectionType != QDSECTION) {
                ttl = BitUtils.uint32(buf.getInt());
                final int length = BitUtils.uint16(buf.getShort());
                rr = new byte[length];
                buf.get(rr);
            } else {
                ttl = 0;
                rr = null;
            }
        }

        /**
         * Convert label from {@code byte[]} to {@code String}
         *
         * It follows the same converting rule as native layer.
         * (See ns_name.c in libc)
         *
         */
        private String labelToString(@NonNull byte[] label) {
            final StringBuffer sb = new StringBuffer();
            for (int i = 0; i < label.length; ++i) {
                // Control characters and non-ASCII characters.
                int b = BitUtils.uint8(label[i]);
                if (b <= 0x20 || b >= 0x7f) {
                    sb.append('\\');
                    byteFormat.format(b, sb, new FieldPosition(0));
                    //sb.append(byteFormat.format(b));
                } else if (b == '"' || b == '.' || b == ';' || b == '\\'
                        || b == '(' || b == ')' || b == '@' || b == '$') {
                    sb.append('\\');
                    sb.append((char) b);
                } else {
                    sb.append((char) b);
                }
            }
            return sb.toString();
        }

        private String parseName(@NonNull ByteBuffer buf) throws ParseException {
            int len, pos;
            int tmpPos = 0, labelCount = 0;
            final StringBuffer sb = new StringBuffer();

            while (true) {
                len = BitUtils.uint8(buf.get());
                final int mask = len & NAME_COMPRESSION;
                if (mask != NAME_NORMAL && mask != NAME_COMPRESSION) {
                    throw new ParseException("Parse name fail");
                }
                if (mask == NAME_NORMAL) {
                    if (len == 0) {
                        break;
                    }
                    if (len > MAXLABELSIZE) {
                        throw new ParseException("Parse normal name fail");
                    }
                    if (sb.length() > 0) {
                        sb.append(".");
                    }
                    final byte[] label = new byte[len];
                    buf.get(label);
                    sb.append(labelToString(label));
                    if (++labelCount > MAXLABELCOUNT) {
                        throw new ParseException("Parse name fail, too many labels");
                    }
                // Based on RFC 1035 - 4.1.4. Message compression
                } else if (mask == NAME_COMPRESSION) {
                    pos = BitUtils.uint8(buf.get());
                    pos += ((len & ~NAME_COMPRESSION) << 8);

                    if (pos >= buf.position() - 2) {
                        throw new ParseException("Parse compression name fail");
                    }
                    tmpPos = (tmpPos == 0) ? buf.position() : tmpPos;
                    try {
                        buf.position(pos);
                    } catch (IndexOutOfBoundsException e) {
                        throw new ParseException("Parse compression name fail");
                    }
                }
            }
            if (tmpPos != 0) {
                buf.position(tmpPos);
            }
            if (sb.length() > MAXNAMESIZE) {
                throw new ParseException("Parse name fail, name size is too long");
            }
            return sb.toString();
        }
    }

    public static final int QDSECTION = 0;
    public static final int ANSECTION = 1;
    public static final int NSSECTION = 2;
    public static final int ARSECTION = 3;
    private static final int NUM_SECTIONS = ARSECTION + 1;

    private static final String TAG = "DnsPacket";

    protected final DnsHeader mHeader;
    protected final List<DnsSection>[] mSections;

    public static class ParseException extends Exception {
        public ParseException(String msg) {
            super(msg);
        }

        public ParseException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    protected DnsPacket(@NonNull byte[] data) throws ParseException {
        if (null == data) throw new ParseException("Parse header failed, null input data");
        final ByteBuffer buffer;
        try {
            buffer = ByteBuffer.wrap(data);
            mHeader = new DnsHeader(buffer);
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Parse Header fail, bad input data", e);
        }

        mSections = new ArrayList[NUM_SECTIONS];

        for (int i = 0; i < NUM_SECTIONS; ++i) {
            final int count = mHeader.getSectionCount(i);
            if (count > 0) {
                mSections[i] = new ArrayList(count);
            }
            for (int j = 0; j < count; ++j) {
                try {
                    mSections[i].add(new DnsSection(i, buffer));
                } catch (IndexOutOfBoundsException e) {
                    throw new ParseException("Parse section fail", e);
                }
            }
        }
    }
}
