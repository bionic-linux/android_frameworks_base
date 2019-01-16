/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.telephony;

import android.annotation.IntRange;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Contains Carrier-specific (and opaque) Protocol configuration Option
 * Data.  In general this is only passed on to carrier-specific applications
 * for interpretation.
 *
 * @hide
 */
@SystemApi
@TestApi
public final class PcoData implements Parcelable {
    private static final int PCO_ID_LOWER_BOUND = 0xFF00;
    private static final int PCO_ID_UPPER_BOUND = 0xFFFF;
    /** @hide */
    public final int cid;
    /** @hide */
    public final String bearerProto;
    /** @hide */
    public final int pcoId;
    /** @hide */
    public final byte[] contents;

    /**
     * @hide
     */
    public PcoData(int cid, String bearerProto, int pcoId, byte[]contents) {
        this.cid = cid;
        this.bearerProto = bearerProto;
        this.pcoId = pcoId;
        this.contents = contents;
    }

    private PcoData(Parcel in) {
        cid = in.readInt();
        bearerProto = in.readString();
        pcoId = in.readInt();
        contents = in.createByteArray();
    }

    /**
     * @return Context ID for this PCO element
     */
    public int getCid() {
        return cid;
    }

    /**
     * @return One of the PDP_type values in TS 27.007 section 10.1.1. For example,
     * "IP", "IPV6", "IPV4V6"
     */
    public String getBearerProtocol() {
        return bearerProto;
    }

    /**
     * @return The protocol ID for this box. Note that only IDs from 0xFF00 - 0xFFFF
     *         (the operator-specific IDs) are valid return values.
     */
    @IntRange(from = PCO_ID_LOWER_BOUND, to = PCO_ID_UPPER_BOUND)
    public int getPcoId() {
        return pcoId;
    }

    /**
     * @return Data contents of this PCO element.
     */
    public byte[] getContents() {
        return contents;
    }

    /**
     * {@link Parcelable#writeToParcel}
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(cid);
        out.writeString(bearerProto);
        out.writeInt(pcoId);
        out.writeByteArray(contents);
    }

    /**
     * {@link Parcelable#describeContents}
     */
    public int describeContents() {
        return 0;
    }

    /**
     * {@link Parcelable.Creator}
     *
     * @hide
     */
    public static final Parcelable.Creator<PcoData> CREATOR = new Parcelable.Creator() {
        public PcoData createFromParcel(Parcel in) {
            return new PcoData(in);
        }

        public PcoData[] newArray(int size) {
            return new PcoData[size];
        }
    };

    @Override
    public String toString() {
        return "PcoData(" + cid + ", " + bearerProto + ", " + pcoId + ", contents[" +
                contents.length + "])";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PcoData pcoData = (PcoData) o;
        return cid == pcoData.cid
                && pcoId == pcoData.pcoId
                && Objects.equals(bearerProto, pcoData.bearerProto)
                && Arrays.equals(contents, pcoData.contents);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(cid, bearerProto, pcoId);
        result = 31 * result + Arrays.hashCode(contents);
        return result;
    }
}
