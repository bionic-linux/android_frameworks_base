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

import android.os.Binder;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.util.Slog;

import java.io.FileDescriptor;
import java.lang.reflect.Field;

final public class DebugService extends Binder {
    private static final String TAG = "DebugService";

    @Override
    public void onShellCommand(FileDescriptor in, FileDescriptor out,
            FileDescriptor err, String[] args, ShellCallback callback,
            ResultReceiver resultReceiver) {
        (new DebugShellCommand(this)).exec(
                this, in, out, err, args, callback, resultReceiver);
    }

    protected boolean changeDebugLog(String module, String tag, int enabled) {

        Class<?> cls;
        try {
            switch (module) {
                case "AM":
                    cls = Class.forName("com.android.server.am.ActivityManagerDebugConfig");
                    break;
                case "PM":
                    cls = Class.forName("com.android.server.pm.PackageManagerService");
                    break;
                case "WM":
                    cls = Class.forName("com.android.server.wm.WindowManagerDebugConfig");
                    break;
                default:
                    cls = Class.forName(module);
            }

            Field f = cls.getDeclaredField(tag);
            f.setAccessible(true);
            f.setBoolean(null, enabled != 0 ? true : false);
            return true;
        } catch (ClassNotFoundException e) {
            Slog.w(TAG, "Failed to change tDebug flag due to unknown Class:" + e);
        } catch (NoSuchFieldException e) {
            Slog.w(TAG, "Failed to change Debug flag due to unknown Field:" + e);
        } catch (IllegalAccessException e) {
            Slog.w(TAG, "Failed to change Debug flag due to Illegal Access: " + e);
        }
        return false;
    }
}
