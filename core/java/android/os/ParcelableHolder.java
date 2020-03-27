/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * Parcelable containing the other Parcelable object.
 */
public final class ParcelableHolder implements Parcelable {
    private ParcelableParcel mParcelableParcel;
    @NonNull
    public static final Parcelable.Creator<ParcelableHolder> CREATOR =
            new Parcelable.Creator<ParcelableHolder>() {
                @NonNull
                @Override
                public ParcelableHolder createFromParcel(@NonNull Parcel parcel) {
                    ParcelableHolder parcelable = new ParcelableHolder();
                    parcelable.readFromParcel(parcel);
                    return parcelable;
                }

                @NonNull
                @Override
                public ParcelableHolder[] newArray(int size) {
                    return new ParcelableHolder[size];
                }
            };

    /**
     * Write a parcelable into ParcelableHolder, the previous parcelable will be removed.
     */
    public void setParcelable(@Nullable Parcelable p) {
        if (mParcelableParcel == null) {
            mParcelableParcel = new ParcelableParcel(getClass().getClassLoader());
        }
        Parcel parcel = mParcelableParcel.getParcel();
        parcel.setDataSize(0);
        parcel.writeTypedObject(p, 0);
    }

    /**
     * Returns the parcelable that was written by setParcelable(Parcelable).
     */
    @Nullable
    public <T extends Parcelable> T getParcelable(@NonNull Parcelable.Creator<T> c) {
        if (mParcelableParcel == null) {
            return null;
        }
        return mParcelableParcel.getParcel().readTypedObject(c);
    }

    /**
     * Read ParcelableHolder from a parcel.
     */
    public void readFromParcel(@NonNull Parcel p) {
        mParcelableParcel = p.readTypedObject(ParcelableParcel.CREATOR);
    }

    @Override
    public void writeToParcel(@NonNull Parcel p, int flags) {
        p.writeTypedObject(mParcelableParcel, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
