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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.servicestests.apps.zramtestapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.os.ZRAM;

/**
 * Build:
 *     cd ${ANDROID_BUILD_TOP}/ && \
 *         mmma frameworks/base/services/tests/servicestests/
 *
 * Run:
 *     adb install ${OUT}/testcases/ZRAMTestApp/ZRAMTestApp.apk
 *     adb shell am start -n com.android.servicestests.apps.zramtestapp/com.android.servicestests.apps.zramtestapp.TestZRAMActivity
 */
public class TestZRAMActivity extends Activity {
    private static final String TAG = TestZRAMActivity.class.getSimpleName();
    private static final String PATTERN = "The rain in Spain stays mainly on the plain.";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ZRAM zram = new ZRAM();
        int num = -1;
        try {
            num = zram.hotAdd(2L << 30);
            if (num < 0) throw new Exception("fail to hotAdd");
            Log.e(TAG, "Pass hotAdd:" + num);

            if (!zram.open(num)) throw new Exception("fail to open");
            Log.e(TAG, "Pass open");

            byte[] buf = PATTERN.getBytes();
            if (!zram.write(num, buf)) throw new Exception("fail to write");
            Log.e(TAG, "Pass write");

            byte[] buf2 = new byte[PATTERN.length()];
            if (zram.read(num, buf2) != PATTERN.length()) throw new Exception("fail to read");
            Log.e(TAG, "Pass read");

            if (!PATTERN.equals(new String(buf2))) throw new Exception("fail to compare the read pattern");
            Log.e(TAG, "Pass pattern");

            zram.close(num);
            if (!zram.hotRemove(num)) throw new Exception("fail to hotRemove");
            Log.e(TAG, "Pass hotRemove");
            num = -1;
        } catch(Exception e) {
            Log.e(TAG, "Fail:" + e.toString());
        } finally {
            if (num != -1) {
                zram.close(num);
                zram.hotRemove(num);
                Log.e(TAG, "hotRemove:" + num);
                num = -1;
            }
        }
        finish();
    }
}