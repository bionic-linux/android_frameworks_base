/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.annotation.SystemApi;

/**
 * Collection of file descriptors and a raw data stream.
 *
 * @hide
 */
@SystemApi
public final class NativeHandle {
    private static final String TAG = "NativeHandle";

    private int[] mFds = new int[0];
    private int[] mInts = new int[0];

    public NativeHandle() {}

    public NativeHandle(@NonNull int[] fds, @NonNull int[] ints) {
        mFds = fds;
        mInts = ints;
    }

    public int[] getFds() {
        return mFds;
    }

    public int[] getInts() {
        return mInts;
    }

    @Override
    public String toString() {
        java.lang.StringBuilder builder = new java.lang.StringBuilder();
        builder.append("NativeHandle {mFds = ");
        builder.append(java.util.Arrays.toString(mFds));
        builder.append(", mInts = ");
        builder.append(java.util.Arrays.toString(mInts));
        builder.append("}");
        return builder.toString();
    }
}
