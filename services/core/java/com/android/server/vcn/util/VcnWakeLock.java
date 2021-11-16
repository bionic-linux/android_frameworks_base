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

package com.android.server.vcn.util;

import android.annotation.NonNull;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;

/**
 * Proxy Implementation of WakeLock, used for testing.
 *
 * @hide
 */
@VisibleForTesting(visibility = Visibility.PRIVATE)
public class VcnWakeLock {
    private final WakeLock mImpl;

    public VcnWakeLock(@NonNull Context context, int flags, @NonNull String tag) {
        final PowerManager powerManager = context.getSystemService(PowerManager.class);
        mImpl = powerManager.newWakeLock(flags, tag);
        mImpl.setReferenceCounted(false /* isReferenceCounted */);
    }

    /**
     * Acquire this WakeLock.
     *
     * <p>Synchronize this action to minimize locking around WakeLock use.
     */
    public synchronized void acquire() {
        mImpl.acquire();
    }

    /**
     * Release this Wakelock.
     *
     * <p>Synchronize this action to minimize locking around WakeLock use.
     */
    public synchronized void release() {
        mImpl.release();
    }
}
