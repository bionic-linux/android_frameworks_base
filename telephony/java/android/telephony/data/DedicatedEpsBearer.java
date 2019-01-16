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
package android.telephony.data;

import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PcoData;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Data class for information about EPS dedicated bearers associated with a data call.
 * @hide
 */
@SystemApi
@TestApi
public final class DedicatedEpsBearer implements Parcelable {
    private final int mBearerId;
    private final byte[] mQosData;
    private final byte[] mTft;
    private final List<PcoData> mPcoElements;

    /** @hide */
    public DedicatedEpsBearer(int bearerId, byte[] qosData, byte[] tft,
            List<PcoData> pcoElements) {
        mBearerId = bearerId;
        mQosData = qosData;
        mTft = tft;
        mPcoElements = pcoElements;
    }

    protected DedicatedEpsBearer(Parcel in) {
        mBearerId = in.readInt();
        mQosData = in.readBlob();
        mTft = in.readBlob();
        mPcoElements = in.createTypedArrayList(PcoData.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mBearerId);
        dest.writeBlob(mQosData);
        dest.writeBlob(mTft);
        dest.writeTypedList(mPcoElements);
    }

    public static final Creator<DedicatedEpsBearer> CREATOR = new Creator<DedicatedEpsBearer>() {
        @Override
        public DedicatedEpsBearer createFromParcel(Parcel in) {
            return new DedicatedEpsBearer(in);
        }

        @Override
        public DedicatedEpsBearer[] newArray(int size) {
            return new DedicatedEpsBearer[size];
        }
    };

    /**
     * @return The bearer ID, as defined in 3GPP 24.301 9.3.2
     */
    public int getBearerId() {
        return mBearerId;
    }

    /**
     * @return The quality-of-service description, as defined in 3GPP 24.008 10.5.6.5
     */
    public byte[] getQosData() {
        return mQosData;
    }

    /**
     * @return The traffic flow template info, as defined in 3GPP 24.008 10.5.6.12
     */
    public byte[] getTft() {
        return mTft;
    }

    /**
     * @return A list of carrier-specific PCO (Protocol Configuration Options) elements associated
     *         with this dedicated bearer.
     */
    public List<PcoData> getPcoElements() {
        return mPcoElements;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DedicatedEpsBearer that = (DedicatedEpsBearer) o;
        return mBearerId == that.mBearerId
                && Arrays.equals(mQosData, that.mQosData)
                && Arrays.equals(mTft, that.mTft)
                && Objects.equals(mPcoElements, that.mPcoElements);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mBearerId, mPcoElements);
        result = 31 * result + Arrays.hashCode(mQosData);
        result = 31 * result + Arrays.hashCode(mTft);
        return result;
    }
}
