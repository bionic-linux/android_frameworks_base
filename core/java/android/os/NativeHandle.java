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
 * Collection of file descriptors and an opaque data stream.
 *
 * @hide
 */
@SystemApi
public final class NativeHandle {
    private ArrayList<FileDescriptor> mFds = new ArrayList<>();
    private int[] mInts;

    /**
     * Constructs a {@link NativeHandle} object containing
     * zero file descriptors and an empty data stream.
     */
    public NativeHandle() {
        mInts = new int[0];
    }

    /**
     * Constructs a {@link NativeHandle} object containing the given
     * {@link FileDescriptor} object and an empty data stream. Also
     * assumes ownership of this file descriptor.
     */
    public NativeHandle(@NonNull FileDescriptor descriptor) {
        mInts = new int[0];
        mFds.add(descriptor);
    }

    /**
     * Convenience method for instantiating a {@link NativeHandle} from JNI. The
     * constructed object does not assume ownership of the file descriptors or
     * the data-stream; as a result, these can be safely mutated.
     *
     * @hide
     */
    private NativeHandle(@NonNull int[] fds, @NonNull int[] ints) {
        for (int fd : fds) {
            FileDescriptor descriptor = new FileDescriptor();
            descriptor.setInt$(fd);
            mFds.add(descriptor);
        }

        mInts = ints.clone();
    }

    /**
     * Returns whether this {@link NativeHandle}
     * object contains a single file descriptor.
     *
     * @return a boolean value
     */
    public boolean hasSingleFileDescriptor() {
        return mFds.size() == 1;
    }

    /**
     * Returns the underlying file descriptor.
     *
     * @return a {@link FileDescriptor} object
     * @throws IllegalStateException if this object contains either zero or
     *         more than one file descriptor, or a non-empty data stream.
     */
    public FileDescriptor getFileDescriptor() {
        if (mFds.size() != 1 || mInts.length != 0) {
            throw new IllegalStateException(
                    "Accessing a File Descriptor (FD) in handles containing " +
                    "either zero or more than one FDs, or a non-empty data " +
                    "stream is disallowed. For such objects, the FDs must " +
                    "be opaque to Java clients.");
        }

        return mFds.get(0);
    }

    /**
     * Convenience method for fetching this object's file descriptors from JNI.
     * @return a mutable copy of the underlying file descriptors (as an int[])
     *
     * @hide
     */
    private int[] getFdsAsIntArray() {
        int numFds = mFds.size();
        int[] fds = new int[numFds];

        for (int i = 0; i < numFds; i++) {
            fds[i] = mFds.get(i).getInt$();
        }

        return fds;
    }

    /**
     * Convenience method for fetching this object's data stream from JNI.
     * @return the opaque data stream. Note: This object retains ownership
     *         of the data, so it must not be mutated.
     *
     * @hide
     */
    private int[] getInts() {
        return mInts;
    }
}
