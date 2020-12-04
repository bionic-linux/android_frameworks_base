/**
 * Copyright 2020 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Class that stores information specific to QOS session.
 *
 * @hide
 */
public final class QosSession implements Parcelable {

    private final int mQosSessionId;

    @NonNull
    private final Qos mQos;

    @NonNull
    private final List<QosFilter> mQosFilterList;

    public QosSession(final int qosSessionId, @NonNull final Qos qos,
            @NonNull final List<QosFilter> qosFilterList) {
        this.mQosSessionId = qosSessionId;
        this.mQos = qos;
        this.mQosFilterList = qosFilterList;
    }

    private QosSession(final Parcel source) {
        mQosSessionId = source.readInt();
        mQos = source.readParcelable(Qos.class.getClassLoader());
        mQosFilterList = new ArrayList<>();
        source.readList(mQosFilterList, QosFilter.class.getClassLoader());
    }

    public int getQosSessionId() {
        return mQosSessionId;
    }

    public Qos getQos() {
        return mQos;
    }

    public List<QosFilter> getQosFilterList() {
        return mQosFilterList;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(mQosSessionId);
        if (mQos.getType() == Qos.QOS_TYPE_EPS) {
            dest.writeParcelable((EpsQos) mQos, flags);
        } else {
            dest.writeParcelable((NrQos) mQos, flags);
        }
        dest.writeList(mQosFilterList);
    }

    public static @NonNull QosSession create(
            @NonNull final android.hardware.radio.V1_6.QosSession qosSession) {
        final List<QosFilter> qosFilters = new ArrayList<>();

        if (qosSession.qosFilters != null) {
            for (final android.hardware.radio.V1_6.QosFilter filter : qosSession.qosFilters) {
                qosFilters.add(QosFilter.create(filter));
            }
        }

        return new QosSession(
                        qosSession.qosSessionId,
                        Qos.create(qosSession.qos),
                        qosFilters);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "QosSession {"
                + " mQosSessionId=" + mQosSessionId
                + " mQos=" + mQos
                + " mQosFilterList=" + mQosFilterList + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mQosSessionId, mQos, mQosFilterList);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (!(o instanceof QosSession)) {
            return false;
        }

        final QosSession other = (QosSession) o;
        return this.mQosSessionId == other.mQosSessionId
                && this.mQos.equals(other.mQos)
                && this.mQosFilterList.size() == other.mQosFilterList.size()
                && this.mQosFilterList.containsAll(other.mQosFilterList);
    }

    public static final @NonNull Parcelable.Creator<QosSession> CREATOR =
            new Parcelable.Creator<QosSession>() {
                @Override
                public QosSession createFromParcel(final Parcel source) {
                    return new QosSession(source);
                }

                @Override
                public QosSession[] newArray(final int size) {
                    return new QosSession[size];
                }
            };
}
