/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.server.audio;

import android.media.AudioSystem;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.view.IDisplayFoldListener;
/**
 * Class to handle device fold events for AudioService, and forward device fold status
 * to the audio HALs through AudioSystem.
 */
class FoldHelper {

    private static final String TAG = "AudioService.FoldHelper";

    private static AudioFoldListener sFoldListener;

    static void init() {
        sFoldListener = new AudioFoldListener();
        enable();
    }

    static void enable() {
        try {
            WindowManagerGlobal.getWindowManagerService()
                    .registerDisplayFoldListener(sFoldListener);
        } catch (Exception e) {
            Log.e(TAG, "registerDisplayFoldListener error " + e);
        }
    }

    static void disable() {
        try {
            WindowManagerGlobal.getWindowManagerService()
                    .unregisterDisplayFoldListener(sFoldListener);
        } catch (Exception e) {
            Log.e(TAG, "unregisterDisplayFoldListener error " + e);
        }
    }

    final static class AudioFoldListener extends
                 IDisplayFoldListener.Stub {
        @Override
        public void onDisplayFoldChanged(int displayId, boolean folded) {
             Log.v(TAG, "publishing device fold=" + folded);
             AudioSystem.setParameters("fold=" + folded);
        }
    }
}
