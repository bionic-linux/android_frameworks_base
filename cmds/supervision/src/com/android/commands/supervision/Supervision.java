/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.commands.supervision;

import android.app.supervision.ISupervisionManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

/** CLI for Supervision Service. */
public final class Supervision {
    private static final String TAG = "Supervision";
    private ISupervisionManager mSupervision;
    private String[] mArgs;
    private int mNextArg;

    /** Main function. */
    public static void main(String[] args) {
        try {
            new Supervision().run(args);
            System.exit(0);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                showUsage();
            }
            Log.e(TAG, "Error", e);
            System.err.println("Error: " + e);
        }
        System.exit(1);
    }

    private void run(String[] args) throws RemoteException {
        if (args.length < 1) {
            throw new IllegalArgumentException();
        }

        mSupervision =
            ISupervisionManager.Stub.asInterface(ServiceManager.getService("supervision"));

        mArgs = args;
        String op = args[0];
        mNextArg = 1;

        if ("set-parent".equals(op)) {
            setParent();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void setParent() throws RemoteException {
        int childId = 0;
        for (String option = nextOption(); option != null; option = nextOption()) {
            switch (option) {
                case "--child":
                    childId = Integer.parseInt(nextArg());
                    break;
            }
        }
        int parentId = Integer.parseInt(nextArg());
        mSupervision.setParentForUser(childId, parentId);
    }

    private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        return mArgs[mNextArg++];
    }

    private String nextOption() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String maybeOption =  mArgs[mNextArg];
        if (maybeOption.startsWith("-")) {
            mNextArg++;
            return maybeOption;
        }
        return null;

    }

    private static void showUsage() {
        System.err.println("usage: supervision set-parent [--child <user-id>] <user-id>");
    }
}
