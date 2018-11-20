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

package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.service.runtimeinfo.DebugEntryProto;
import android.service.runtimeinfo.RuntimeInfoServiceDumpProto;
import android.util.proto.ProtoOutputStream;

import libcore.timezone.TimeZoneDataFiles;
import libcore.util.CoreLibrary;

import com.android.internal.util.DumpUtils;
import com.android.timezone.distro.DistroException;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.FileUtils;
import com.android.timezone.distro.TimeZoneDistro;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This service exists only as a "dumpsys" target which reports
 * information about the status of the runtime and related libraries.
 */
public class RuntimeInfoService extends Binder {

    private static final String TAG = "RuntimeInfoService";

    private final Context mContext;

    public RuntimeInfoService(Context context) {
        mContext = context;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpAndUsageStatsPermission(mContext, TAG, pw)) return;

        boolean protoFormat = hasOption(args, "--proto");
        ProtoOutputStream proto = null;

        CoreLibrary.DebugInfo coreLibraryDebugInfo = CoreLibrary.getDebugInfo();

        // Add /data tz data set using the DistroVersion class (which libcore cannot use).
        // This update mechanism will be removed after the time zone APEX is launched so this
        // untidiness will disappear with it.
        {
            String debugKeyPrefix = "libcore.timezone.data_";
            String versionFileName = TimeZoneDataFiles.getDataTimeZoneFile(
                    TimeZoneDistro.DISTRO_VERSION_FILE_NAME);
            addDistroVersionDebugInfo(versionFileName, debugKeyPrefix, coreLibraryDebugInfo);
        }

        if (protoFormat) {
            proto = new ProtoOutputStream(fd);
            pw = null;

            reportTimeZoneInfoProto(coreLibraryDebugInfo, proto);
        } else {
            reportTimeZoneInfo(coreLibraryDebugInfo, pw);
        }

        if (protoFormat) {
            proto.flush();
        }
    }

    private boolean hasOption(String[] args, String arg) {
        for (String opt : args) {
            if (arg.equals(opt)) {
                return true;
            }
        }
        return false;
    }

    // If you change this method, make sure to modify the Proto version of this method as well.
    private void reportTimeZoneInfo(CoreLibrary.DebugInfo coreLibraryDebugInfo,
            PrintWriter pw) {
        pw.println("Core Library Debug Info: ");
        for (CoreLibrary.DebugEntry debugEntry : coreLibraryDebugInfo.getDebugEntries()) {
            pw.print("Key: \"");
            pw.print(debugEntry.getKey());
            pw.println("\"");
            pw.print("Value: \"");
            pw.print(debugEntry.getStringValue());
            pw.println("\"");
        }
    }

    private void reportTimeZoneInfoProto(
            CoreLibrary.DebugInfo coreLibraryDebugInfo, ProtoOutputStream proto) {
        for (CoreLibrary.DebugEntry debugEntry : coreLibraryDebugInfo.getDebugEntries()) {
            long entryToken = proto.start(RuntimeInfoServiceDumpProto.DEBUG_ENTRY);
            proto.write(DebugEntryProto.KEY, debugEntry.getKey());
            proto.write(DebugEntryProto.STRING_VALUE, debugEntry.getStringValue());
            proto.end(entryToken);
        }
    }

    private static void addDistroVersionDebugInfo(String distroVersionFileName,
            String debugKeyPrefix, CoreLibrary.DebugInfo debugInfo) {
        File file = new File(distroVersionFileName);
        String statusKey = debugKeyPrefix + "status";
        if (file.exists()) {
            try {
                byte[] versionBytes =
                        FileUtils.readBytes(file, DistroVersion.DISTRO_VERSION_FILE_LENGTH);
                DistroVersion distroVersion = DistroVersion.fromBytes(versionBytes);
                String formatVersionString = distroVersion.formatMajorVersion + "."
                        + distroVersion.formatMinorVersion;
                debugInfo.addStringEntry(statusKey, "OK")
                        .addStringEntry(debugKeyPrefix + "formatVersion", formatVersionString)
                        .addStringEntry(debugKeyPrefix + "rulesVersion",
                                distroVersion.rulesVersion)
                        .addStringEntry(debugKeyPrefix + "revision",
                                distroVersion.revision);
            } catch (IOException | DistroException e) {
                debugInfo.addStringEntry(statusKey, "ERROR");
                debugInfo.addStringEntry(debugKeyPrefix + "error", e.getMessage());
                System.logE("Error reading " + file, e);
            }
        } else {
            debugInfo.addStringEntry(statusKey, "NOT_FOUND");
        }
    }

}
