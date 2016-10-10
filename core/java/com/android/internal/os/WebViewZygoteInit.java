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

package com.android.internal.os;

import android.app.ApplicationLoaders;
import android.net.LocalSocket;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

/**
 * Startup class for the WebView zygote process.
 *
 * See {@link ZygoteInit} for generic zygote startup documentation.
 *
 * @hide
 */
class WebViewZygoteInit {
    public static final String TAG = "WebViewZygoteInit";

    private static ZygoteServer sServer;

    private static class WebViewZygoteServer extends ZygoteServer {
        @Override
        protected ZygoteConnection createNewConnection(LocalSocket socket, String abiList)
                throws IOException {
            return new WebViewZygoteConnection(socket, abiList);
        }
    }

    private static class WebViewZygoteConnection extends ZygoteConnection {
        WebViewZygoteConnection(LocalSocket socket, String abiList) throws IOException {
            super(socket, abiList);
        }

        @Override
        protected boolean handlePreloadPackage(String packagePath, String libsPath) {
            ClassLoader loader = ApplicationLoaders.getDefault().getClassLoader(packagePath,
                    Build.VERSION.SDK_INT, false, libsPath, null, null);
            // We now need to load the native library into that classloader. Two problems:
            // 1) We don't know the name of the library at this point. It's discovered from a
            //    metadata tag on the package by code in WebViewFactory. The generic preload
            //    interface we've added here doesn't tell us.
            // 2) System.loadLibrary doesn't take a classloader, and the method that does
            //    (Runtime.loadLibrary) is deprecated and targetSDK limited because it's abused by
            //    apps. The real method (Runtime.loadLibrary0) is not public.
            return false;
        }
    }

    public static void main(String argv[]) {
        sServer = new WebViewZygoteServer();

        // Zygote goes into its own process group.
        try {
            Os.setpgid(0, 0);
        } catch (ErrnoException ex) {
            throw new RuntimeException("Failed to setpgid(0,0)", ex);
        }

        try {
            sServer.registerServerSocket("webview_zygote");
            sServer.runSelectLoop(TextUtils.join(",", Build.SUPPORTED_ABIS));
            sServer.closeServerSocket();
        } catch (Zygote.MethodAndArgsCaller caller) {
            caller.run();
        } catch (RuntimeException e) {
            Log.e(TAG, "Fatal exception:", e);
        }

        System.exit(0);
    }
}
