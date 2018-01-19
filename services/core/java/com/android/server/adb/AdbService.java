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
 * See the License for the specific language governing permissions an
 * limitations under the License.
 */
package com.android.server.adb;

import android.content.Context;
import android.debug.IAdbManager;
import android.debug.IAdbTransport;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class AdbService extends IAdbManager.Stub {
    public static class Lifecycle extends SystemService {
        private AdbService mAdbService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            mAdbService = new AdbService(getContext());
            publishBinderService(Context.ADB_SERVICE, mAdbService);
        }
    }

    private static final String TAG = "AdbService";

    private final Context mContext;
    private final HashMap<IBinder, IAdbTransport> mTransports = new HashMap<>();

    private boolean mAdbEnabled;

    private AdbService(Context context) {
        mContext = context;
    }

    @Override
    public void registerTransport(IAdbTransport transport) throws RemoteException {
        mTransports.put(transport.asBinder(), transport);
    }

    @Override
    public void unregisterTransport(IAdbTransport transport) throws RemoteException {
        mTransports.remove(transport.asBinder());
    }

    @Override
    public boolean isAdbEnabled() throws RemoteException {
        return mAdbEnabled;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (!DumpUtils.checkDumpPermission(mContext, TAG, writer)) return;

        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        final long ident = Binder.clearCallingIdentity();
        try {
            if (args == null || args.length == 0 || "-a".equals(args[0])) {
                pw.println("ADB Manager State:");
                pw.increaseIndent();
                pw.print("Number of registered transports: ");
                pw.println(mTransports.size());
            } else {
                pw.println("Dump current ADB state");
                pw.println("  No commands available");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
