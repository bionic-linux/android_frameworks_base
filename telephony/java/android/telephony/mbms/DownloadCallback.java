/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.telephony.mbms;

/**
 * A optional listener class used by download clients to track progress.
 * @hide
 */
public class DownloadCallback extends IDownloadCallback.Stub {
    /**
     * Gives process callbacks for a given DownloadRequest.
     * request indicates which download is being referenced.
     * fileInfo gives information about the file being downloaded.  Note that
     *   the request may result in many files being downloaded and the client
     *   may not have been able to get a list of them in advance.
     * currentDownloadSize is the current amount downloaded.
     * fullDownloadSize is the total number of bytes that make up the downloaded content.  This
     *   may be different from the decoded final size, but is useful in gauging download progress.
     * currentDecodedSize is the number of bytes that have been decoded.
     * fullDecodedSize is the total number of bytes that make up the final decoded content.
     */
    public void progress(DownloadRequest request, FileInfo fileInfo,
            int currentDownloadSize, int fullDownloadSize,
            int currentDecodedSize, int fullDecodedSize) {
    }
}
