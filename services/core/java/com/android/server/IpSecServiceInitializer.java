/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server;

import android.content.Context;
import android.util.Log;

/**
 * An initializer for IpSecService.
 */
public final class IpSecServiceInitializer extends SystemService {
    private static final String TAG = IpSecServiceInitializer.class.getSimpleName();
    private final IpSecService mService;

    public IpSecServiceInitializer(Context context) {
        super(context);
        mService = new IpSecService(context);
    }

    @Override
    public void onStart() {
        Log.i(TAG, "Registering " + Context.IPSEC_SERVICE);
        publishBinderService(Context.IPSEC_SERVICE, mService,
                /* allowIsolated= */ false);
    }
}
