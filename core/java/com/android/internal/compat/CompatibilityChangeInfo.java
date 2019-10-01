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
 * This class is a parcelable version of {@link com.android.server.compat.CompatChange}.
 *
 * @hide
 */
public final class CompatibilityChangeInfo implements Parcelable {
    public final long changeId;
    public final String name;
    public final int enableAfterTargetSdk;
    public final boolean disabled;

    public CompatibilityChangeInfo(
            Long changeId, String name, int enableAfterTargetSdk, boolean disabled) {
        this.changeId = changeId;
        this.name = name;
        this.enableAfterTargetSdk = enableAfterTargetSdk;
        this.disabled = disabled;
    }

    private CompatibilityChangeInfo(Parcel in) {
        changeId = in.readLong();
        name = in.readString();
        enableAfterTargetSdk = in.readInt();
        disabled = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(changeId);
        dest.writeString(name);
        dest.writeInt(enableAfterTargetSdk);
        dest.writeBoolean(disabled);
    }

    public static final Parcelable.Creator<CompatibilityChangeInfo> CREATOR =
            new Parcelable.Creator<CompatibilityChangeInfo>() {

                @Override
                public CompatibilityChangeInfo createFromParcel(Parcel in) {
                    return new CompatibilityChangeInfo(in);
                }

                @Override
                public CompatibilityChangeInfo[] newArray(int size) {
                    return new CompatibilityChangeInfo[size];
                }
            };
}
