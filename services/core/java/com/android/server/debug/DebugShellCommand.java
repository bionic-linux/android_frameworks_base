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

package com.android.server.debug;

import android.os.RemoteException;
import android.os.ShellCommand;

import java.io.PrintWriter;

final public class DebugShellCommand extends ShellCommand {
    private static final String TAG = "DebugShellCommand";
    private DebugService mService;

    DebugShellCommand(DebugService service) {
        mService = service;
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd) {
                case "change-log":
                    return runChangeLog(pw);
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
        }
        return -1;
    }

    private int runChangeLog(PrintWriter pw) throws RemoteException {
        final String module = getNextArgRequired();
        final String tag = getNextArgRequired();
        int enabled = 0;

        try {
            enabled = Integer.parseInt(getNextArgRequired());
        } catch (NumberFormatException e) {
            pw.println("Failed to change Debug flag due to invalid argument: " + e);
            return -1;
        }

        boolean result = mService.changeDebugLog(module, tag, enabled);
        if (result) {
            pw.println("Succeeded in changing Debug Flag");
        } else {
            pw.println("Failed to change Debug Flag");
        }
        return 0;
    }

    @Override
    public void onHelp() {
        final PrintWriter pw = getOutPrintWriter();
        pw.println("Debug commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  change-log <Module> <Flag> <Enable>");
        pw.println("    Module : ");
        pw.println("        AM : ActivityManagerDebugConfig class's flags are target");
        pw.println("        WM : WindowManagerDebugConfig class's flags are target");
        pw.println("        PM : PackageManagerService class's flags are target");
        pw.println("        <other> : Input fully qualified name freely instead of above modules");
        pw.println("          In this case, Flag must not be final variable");
        pw.println("    Flag : ");
        pw.println("        Flag name which you want to change");
        pw.println("        (ex) DEBUG_SETTINGS");
        pw.println("    Enable : ");
        pw.println("        0/1 : When you want to enable target log, set 1. Otherwise set 0");
        pw.println("    Example : ");
        pw.println("        adb shell cmd debug change-log AM DEBUG_BROADCAST 1");
    }
}
