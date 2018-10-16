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
import android.os.ZRAM;
import android.util.Log;

public class TestZRAMActivity extends Activity {
    private static final String TAG = TestJobActivity.class.getSimpleName();
    private static final String PACKAGE_NAME = "com.android.servicestests.apps.zramtestapp";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ZRAM zram = new ZRAM();
        finish();
    }
}