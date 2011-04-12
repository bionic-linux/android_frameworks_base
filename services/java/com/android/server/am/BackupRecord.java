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

/** @hide */
final class BackupRecord {

    static final int BACKUP_INCREMENTAL = IApplicationThread.BACKUP_MODE_INCREMENTAL;
    static final int BACKUP_FULL = IApplicationThread.BACKUP_MODE_FULL;
    static final int RESTORE = IApplicationThread.BACKUP_MODE_RESTORE;
    
    final ApplicationInfo appInfo;         // information about BackupAgent's app
    final int backupMode;                  // full backup / incremental / restore
    final ProcessRecord app;                     // where this agent is running or null

    // ----- Implementation -----

    BackupRecord(ApplicationInfo _appInfo, int _backupMode, ProcessRecord app) {
        appInfo = _appInfo;
        backupMode = _backupMode;
        this.app = app;
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
