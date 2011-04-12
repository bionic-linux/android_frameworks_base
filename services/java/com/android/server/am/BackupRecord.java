/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.IApplicationThread;
import android.content.pm.ApplicationInfo;

/**
 * @hide
 */
final class BackupRecord {

    static final int BACKUP_INCREMENTAL = IApplicationThread.BACKUP_MODE_INCREMENTAL;
    static final int BACKUP_FULL = IApplicationThread.BACKUP_MODE_FULL;
    static final int RESTORE = IApplicationThread.BACKUP_MODE_RESTORE;

    /**
     * Information about BackupAgent's app.
     */
    final ApplicationInfo appInfo;

    /**
     * Backup mode: incremental, full, or restore.
     */
    final int backupMode;

    /**
     * Where this agent is running or {@code null}.
     */
    final ProcessRecord app;

    /**
     * Creates a new backup record.
     *
     * @param mode Backup mode.
     * @param appInfo Information about BackupAgent's app.
     * @param process Where this agent is running.
     *
     * @throws IllegalArgumentException if {@code backupMode} is not one
     * of {@code BACKUP_INCREMENTAL}, {@code BACKUP_FULL} and {@code RESTORE}.
     */
    BackupRecord(int mode, ApplicationInfo appInfo, ProcessRecord process) {
        if (!modeIsValid(mode)) {
            throw new IllegalArgumentException("Invalid backup mode: " + mode);
        }
        this.backupMode = mode;
        this.appInfo = appInfo;
        // TODO: THERE IS NO REASON TO ALLOW 'null' HERE, BUT..
        // .. review the usage of the constructor and refactor.
        this.app = process;
    }

    private static boolean modeIsValid(final int mode) {
        return mode == BACKUP_INCREMENTAL || mode == BACKUP_FULL || mode == RESTORE;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("BackupRecord{")
                .append(Integer.toHexString(System.identityHashCode(this)))
                .append(' ').append(appInfo.packageName)
                .append(' ').append(appInfo.name)
                .append(' ').append(appInfo.backupAgentName).append('}');
        return sb.toString();
    }

}
