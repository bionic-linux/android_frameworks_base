/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.internal.os;


/** @hide */
public final class FilePrefetch {
    private FilePrefetch() {}

    /**
     * Prefetch a portion of a file.
     *
     * @param path the absolute pathname of the file to be prefetched.
     * @param offset the byte offset (which will be aligned 0 mod 4k) of the start of the chunk.
     * @param length the byte length of the chunk to be prefetched.
     */
    native static void nativePrefetchFileChunk(String path, int offset, int length);

    public static void prefetchFileChunk(String path, int offset, int length) {
        nativePrefetchFileChunk(path, offset, length);
    }
}
