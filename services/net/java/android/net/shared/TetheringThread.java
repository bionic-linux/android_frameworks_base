/*
 * Copyright (C) 2019 The Android Open Source Project
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
package android.net.shared;

import android.os.Handler;


/**
 * This is a temporary class to obtain the sytem serve thread for tethering module development.
 * This class would be deprecated after tethering module is moved out of system server.
 */
public final class TetheringThread {
    private static Handler sTetherHandler;

    public static Handler get() {
        synchronized(TetheringThread.class) {
            return sTetherHandler;
        }
    }

    public static void set(Handler handler) {
        synchronized(TetheringThread.class) {
            sTetherHandler = handler;
        }
    }
}
