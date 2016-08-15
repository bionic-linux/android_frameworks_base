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

import android.net.LocalSocket;
import android.os.Build;
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
        protected ZygoteConnection createNewConnection(LocalSocket socket,
                                                       String abiList) throws IOException {
            return new WebViewZygoteConnection(socket, abiList);
        }
    }

    private static class WebViewZygoteConnection extends ZygoteConnection {
        WebViewZygoteConnection(LocalSocket socket, String abiList) throws IOException {
            super(socket, abiList);
        }

        @Override
        protected boolean handlePreloadPackage(String packagePath, String libsPath) {
            Log.d(TAG, "Preload package ********************************************************");
            Log.d(TAG, "WebView package = " + packagePath);
            return false;
        }
    }

    public static void main(String argv[]) {
        sServer = new WebViewZygoteServer();

        try {
            sServer.registerServerSocket("webview_zygote");
            Log.d(TAG, "Listening for WebView requests");

            sServer.runSelectLoop(TextUtils.join(",", Build.SUPPORTED_ABIS));
            sServer.closeServerSocket();
        } catch (Zygote.MethodAndArgsCaller caller) {
            caller.run();
        } catch (RuntimeException e) {
            Log.e(TAG, "Fatal exception:", e);
        }

        Log.d(TAG, "Exiting webview_zygote server loop");

        System.exit(0);
    }
}
