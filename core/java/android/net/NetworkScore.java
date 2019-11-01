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
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Object representing the quality of a network as perceived by the user.
 *
 * A NetworkScore object represents the characteristics of a network that affects how good the
 * network is considered for a particular use.
 * @hide
 */
public class NetworkScore implements Parcelable {

    // It's the key of bundle which is used to get the legacy network score of NetworkAgentInfo.
    public static final String LEGACY_SCORE = "LEGACY_SCORE";
    private final Bundle mExtensions;

    public NetworkScore() {
        mExtensions = new Bundle();
    }

    public NetworkScore(NetworkScore source) {
        mExtensions = new Bundle(source.mExtensions);
    }

    /**
     * Put the value of object inside the bundle by key.
     */
    public void putExtension(String key, Parcelable value) {
        mExtensions.putObject(key, value);
    }

    /**
     * Put the value of int inside the bundle by key.
     */
    public void putExtension(String key, int value) {
        mExtensions.putInt(key, value);
    }

    /**
     * Get the value of non primitive type by key.
     */
    public <T extends Parcelable> T getExtension(String key) {
        return mExtensions.getParcelable(key);
    }

    /**
     * Get the value of int by key.
     */
    public int getIntExtension(String key) {
        return mExtensions.getInt(key);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        synchronized (this) {
            dest.writeBundle(mExtensions);
        }
    }

    public static final @NonNull Creator<NetworkScore> CREATOR = new Creator<NetworkScore>() {
        @Override
        public NetworkScore createFromParcel(Parcel in) {
            return new NetworkScore(in);
        }

        @Override
        public NetworkScore[] newArray(int size) {
            return new NetworkScore[size];
        }
    };

    private NetworkScore(Parcel in) {
        mExtensions = in.readBundle();
    }
}
