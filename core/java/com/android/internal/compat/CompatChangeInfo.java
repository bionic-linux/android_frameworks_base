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

package com.android.internal.compat;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class is a parcelable version of {@link com.android.server.compat.config.Change}.
 * @hide
 */
public final class CompatChangeInfo implements Parcelable {
    public final long changeId;
    public final String name;
    public final int targetSdkThreshold;
    public final boolean defaultValue;

    public CompatChangeInfo(
            Long changeId, String name, int targetSdkThreshold, boolean defaultValue) {
        this.changeId = changeId;
        this.name = name;
        this.targetSdkThreshold = targetSdkThreshold;
        this.defaultValue = defaultValue;
    }

    private CompatChangeInfo(Parcel in) {
        changeId = in.readLong();
        name = in.readString();
        targetSdkThreshold = in.readInt();
        defaultValue = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(changeId);
        dest.writeString(name);
        dest.writeInt(targetSdkThreshold);
        dest.writeBoolean(defaultValue);
    }
    public static final Parcelable.Creator<CompatChangeInfo> CREATOR =
            new Parcelable.Creator<CompatChangeInfo>() {

                @Override
                public CompatChangeInfo createFromParcel(Parcel in) {
                    return new CompatChangeInfo(in);
                }

                @Override
                public CompatChangeInfo[] newArray(int size) {
                    return new CompatChangeInfo[size];
                }
            };
}
