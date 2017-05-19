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
 * limitations under the License
 */

package android.telephony.mbms;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @hide
 */
public class MbmsUtils {
    private static final String LOG_TAG = "MbmsUtils";

    public static boolean isContainedIn(File parent, File child) {
        try {
            String parentPath = parent.getCanonicalPath();
            String childPath = child.getCanonicalPath();
            return childPath.startsWith(parentPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve canonical paths: " + e);
        }
    }

    public static void waitOnLatchWithTimeout(CountDownLatch l, long timeoutMs) {
        long endTime = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < endTime) {
            try {
                l.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // keep waiting
            }
            if (l.getCount() <= 0) {
                return;
            }
        }
    }
}
