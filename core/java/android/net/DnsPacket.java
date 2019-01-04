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

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.android.internal.util.BitUtils;

/**
 * Defines basic data for DNS protocol based on RFC 1035.
 * Subclasses create the specific format used in DNS packet.
 *
 * @hide
 */
public abstract class DnsPacket {
    public class DnsHeader {
        private final int mId;
        private final int mFlags;
        private final int mRcode;
        private final int[] mSectionCount;

        DnsHeader(ByteBuffer buf) throws IndexOutOfBoundsException {
            mId = BitUtils.uint16(buf.getShort());
            mFlags = BitUtils.uint16(buf.getShort());
            mRcode = mFlags & 0xF;
            mSectionCount = new int[NUM_SECTIONS];
            for (int i = 0; i < NUM_SECTIONS; ++i) {
                mSectionCount[i] = BitUtils.uint16(buf.getShort());
            }
        }

        public int getId() {
            return mId;
        }

        public int getFlags() {
            return mFlags;
        }

        public int getRcode() {
            return mRcode;
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

        private static final String TAG = "DnsSection";
        private final String mName;
        private final int mType;
        private final int mClass;
        private final long mTTL;
        private final byte[] mRR;

        DnsSection(int sectionType, ByteBuffer buf)
                throws ParseException, IndexOutOfBoundsException {
            mName = parseName(buf);
            mType = BitUtils.uint16(buf.getShort());
            mClass = BitUtils.uint16(buf.getShort());

            if (sectionType != QDSECTION) {
                mTTL = BitUtils.uint32(buf.getInt());
                int length = BitUtils.uint16(buf.getShort());
                mRR = new byte[length];
                buf.get(mRR, 0, length);
            } else {
                mTTL = 0;
                mRR = null;
            }
        }

        public String getDnsName() {
            return mName;
        }

        public int getDnsType() {
            return mType;
        }

        public int getDnsClass() {
            return mClass;
        }

        public long getDnsTtl() {
            return mTTL;
        }

        public byte[] getRR() {
            return mRR;
        }

        private String labelToString(byte[] label) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < label.length; ++i) {
                // Control characters and non-ASCII characters.
                int b = BitUtils.uint8(label[i]);
                if (b <= 0x20 || b >= 0x7f) {
                    sb.append('\\');
                    sb.append(new DecimalFormat().format(b));
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

        private String parseName(ByteBuffer buf) throws ParseException {
            int len, pos;
            int tmpPos = 0, labelCount = 0;
            StringBuffer sb = new StringBuffer();

            while (true) {
                len = BitUtils.uint8(buf.get());
                int mask = len & NAME_COMPRESSION;
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
                    byte [] label = new byte[len];
                    buf.get(label, 0, len);
                    sb.append(labelToString(label));
                    if (++labelCount > MAXLABELCOUNT) {
                        throw new ParseException("Parse name fail, too many labels");
                    }
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
    private static final int NUM_SECTIONS = ARSECTION +1;

    private static final String TAG = "DnsPacket";
    private static final boolean DBG = false;

    protected final DnsHeader mHeader;
    protected List<DnsSection>[] mSections;
    protected ByteBuffer mBuffer;

    public static class ParseException extends Exception {
        public ParseException(String msg, Object... args) {
            super(String.format(msg, args));
        }
    }

    protected DnsPacket(byte[] data) throws ParseException {
        try {
            mBuffer = ByteBuffer.wrap(data);
            mHeader = new DnsHeader(mBuffer);
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Parse Header fail, bad input data");
        } catch (NullPointerException e) {
            throw new ParseException("Parse Header fail, null input data");
        }

        mSections = new ArrayList[NUM_SECTIONS];

        for (int i = 0; i < NUM_SECTIONS; ++i) {
            int count = mHeader.getSectionCount(i);
            if (count > 0) {
                mSections[i] = new ArrayList(count);
            }
            for (int j = 0; j < count; ++j) {
                try {
                    mSections[i].add(new DnsSection(i, mBuffer));
                } catch (IndexOutOfBoundsException e) {
                    throw new ParseException("Parse section fail", e);
                }
            }
        }
    }
}
