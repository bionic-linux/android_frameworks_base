/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.server.am;

public final class BinderProcsInfo {
    private int mPid;            // Start pid for the binder info file scan.
    private int mStatus;         // Status from the binder info files scan. 0 = ok otherwise
                                 // the first error in the scan, see status_t in utils/Errors.h.
    private String[] mTrLines;   // Lines with outgoing, incoming or pending binder transaction from
                                 // scanned binder info files.
    private int[] mScannedPids;  // The pids having their binder info files scanned.

    static native void nGetBinderTransactionInfo(int pid, BinderProcsInfo bpi);

    private BinderProcsInfo(int pid) {
        mPid = pid;
    }

    /**
     * Get binder transaction information from Android binderfs filesystem.
     *
     * Starting with given pid, binder info files for all to-pids in outgoing transactions are
     * scanned recursively.
     * From the scanned binder info files, all lines with binder transactions are collected.
     * All the pids that had their binder files scanned are also collected.
     *
     * @param pid Starting pid of binder transaction info files to scan..
     * @return An object containing appropriate binder transaction information, se description above
     */
    public static BinderProcsInfo getBinderTransactionInfo(int pid) {
        BinderProcsInfo bpi = new BinderProcsInfo(pid);
        nGetBinderTransactionInfo(pid, bpi);
        return bpi;
    }

    /**
     * @return An array with pids in outgoing transaction chains, see description for
     * getBinderTransactionInfo().
     */
    public int[] getPids() {
        return mScannedPids;
    }

    /**
     * Append binder transaction information from the scan done in this object to the given dropbox
     * report.
     */
    public void toDropbox(StringBuilder report) {
        if (((mTrLines != null) && (mTrLines.length > 0)) || (mStatus != 0)) {
            report.append("----- binder transactions from pid ")
                    .append(mPid)
                    .append(" -----\n");
            if (mTrLines != null) {
                for (int i = 0; i < mTrLines.length; i++) {
                    report.append(mTrLines[i])
                            .append('\n');
                }
            }
            if (mStatus != 0) {
                report.append("Error (")
                        .append(mStatus)
                        .append(") when reading the binder information\n");
            }
            report.append("----- end binder transactions ")
                    .append(mPid)
                    .append(" -----\n\n");
        }
    }
}
