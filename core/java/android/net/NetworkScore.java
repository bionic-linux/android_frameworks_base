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

import android.os.Bundle;

/**
 * A NetworkScore object represents the characteristics of a network that affects how good the
 * network is considered for a particular use.
 * @hide
 */

public class NetworkScore {

    public static final String LEGACY_SCORE = "LEGACY_SCORE";
    final Bundle mExtensions;
    // T getExtension<T>(String key);

    public NetworkScore() {
        mExtensions = new Bundle();
    }

    public NetworkScore(NetworkScore source) {
        mExtensions = new Bundle(source.mExtensions);
    }

    /**
     * Put the value of object inside the bundle by key.
     */
    public void putExtension(String key, Object value) {
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
    public <T> T getExtension(String key) {
        return (T) mExtensions.get(key);
    }

    /**
     * Get the value of int by key.
     */
    public int getIntExtension(String key) {
        return mExtensions.getInt(key);
    }
}
