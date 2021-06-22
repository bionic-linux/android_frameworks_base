/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.media.audiopolicy;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * @hide
 */
@SystemApi
public final class AudioDevicePortGain implements Parcelable {
    private int mId;
    private int mIndex;

    /**
     * @param id of the audio device port
     * @param index of the audio device port gain
     */
    private AudioDevicePortGain(int id, int index) {
        mId = id;
        mIndex = index;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeInt(mIndex);
    }

    @NonNull
    public static final Parcelable.Creator<AudioDevicePortGain> CREATOR =
            new Parcelable.Creator<AudioDevicePortGain>() {
                @Override
                public AudioDevicePortGain createFromParcel(@NonNull Parcel in) {
                    int id = in.readInt();
                    int index = in.readInt();
                    return new AudioDevicePortGain(id, index);
                }

                @Override
                public @NonNull AudioDevicePortGain[] newArray(int size) {
                    return new AudioDevicePortGain[size];
                }
            };

    @NonNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("\n Id: ");
        s.append(Integer.toString(mId));
        s.append(" Index: ");
        s.append(Integer.toString(mIndex));
        return s.toString();
    }
}
