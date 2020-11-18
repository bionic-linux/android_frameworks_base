/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.net;

import android.os.HandlerThread;
import android.os.Looper;

/**
 * TODO: migrate clients away from this thread
 * @hide
 */
public final class ConnectivityThreadInternal extends HandlerThread {

    // A class implementing the lazy holder idiom: the unique static instance
    // of ConnectivityThread is instantiated in a thread-safe way (guaranteed by
    // the language specs) the first time that Singleton is referenced in get()
    // or getInstanceLooper().
    private static class Singleton {
        private static final ConnectivityThreadInternal INSTANCE = createInstance();
    }

    private ConnectivityThreadInternal() {
        super("ConnectivityThread");
    }

    private static ConnectivityThreadInternal createInstance() {
        ConnectivityThreadInternal t = new ConnectivityThreadInternal();
        t.start();
        return t;
    }

    public static ConnectivityThreadInternal get() {
        return ConnectivityThreadInternal.Singleton.INSTANCE;
    }

    public static Looper getInstanceLooper() {
        return ConnectivityThreadInternal.Singleton.INSTANCE.getLooper();
    }
}

