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

import java.io.FileDescriptor;
import java.util.ArrayList;

import android.annotation.NonNull;
import android.annotation.SystemApi;

/**
 * Collection of file descriptors and a raw data stream.
 *
 * @hide
 */
@SystemApi
public final class NativeHandle {
    private ArrayList<FileDescriptor> mFds = new ArrayList<>();
    private int[] mInts;

    public NativeHandle() {
        mInts = new int[0];
    }

    public NativeHandle(@NonNull FileDescriptor descriptor) {
        this();
        mFds.add(descriptor);
    }

    public NativeHandle(@NonNull int[] fds, @NonNull int[] ints) {
        for (int fd : fds) {
            FileDescriptor descriptor = new FileDescriptor();
            descriptor.setInt$(fd);
            mFds.add(descriptor);
        }

        mInts = ints;
    }

    public FileDescriptor getFd() {
        if (mFds.size() > 1 || mInts.length != 0) {
            throw new IllegalStateException(
                    "Accessing File Descriptors (FDs) in handles containing " +
                    "either multiple (>1) FDs or opaque data is disallowed. " +
                    "Such objects are handled exclusively by native code.");
        } else if (mFds.isEmpty()) {
            return new FileDescriptor();
        }

        return mFds.get(0);
    }

    public int[] getFdsAsIntArray() {
        int numFds = mFds.size();
        int[] fds = new int[numFds];

        for (int i = 0; i < numFds; i++) {
            fds[i] = mFds.get(i).getInt$();
        }

        return fds;
    }

    public int[] getInts() {
        return mInts;
    }

    @Override
    public String toString() {
        java.lang.StringBuilder builder = new java.lang.StringBuilder();
        builder.append("NativeHandle {mFds = ");
        builder.append(java.util.Arrays.toString(getFdsAsIntArray()));
        builder.append(", mInts = ");
        builder.append(java.util.Arrays.toString(mInts));
        builder.append("}");
        return builder.toString();
    }
}
