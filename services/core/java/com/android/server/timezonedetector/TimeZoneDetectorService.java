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

package com.android.server.timezonedetector;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.timezonedetector.ITimeZoneDetectorService;
import android.app.timezonedetector.PhoneTimeZoneSuggestion;
import android.content.Context;
import android.os.Binder;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * The implementation of ITimeZoneDetectorService.aidl.
 */
public final class TimeZoneDetectorService extends ITimeZoneDetectorService.Stub {
    private static final String TAG = "timezonedetector.TimeZoneDetectorService";

    /**
     * Handles the lifecycle for {@link TimeZoneDetectorService}.
     */
    public static class Lifecycle extends SystemService {

        public Lifecycle(@NonNull Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            TimeZoneDetectorService service = TimeZoneDetectorService.create(getContext());

            // Publish the binder service so it can be accessed from other (appropriately
            // permissioned) processes.
            publishBinderService(Context.TIME_ZONE_DETECTOR_SERVICE, service);
        }
    }

    @NonNull private final Context mContext;

    // The lock used when calling the strategy to ensure thread safety.
    @NonNull private final Object mStrategyLock = new Object();

    @GuardedBy("mStrategyLock")
    @NonNull private final TimeZoneDetectorStrategy mTimeZoneDetectorStrategy;

    private static TimeZoneDetectorService create(@NonNull Context context) {
        final TimeZoneDetectorStrategy timeZoneDetector =
                TimeZoneDetectorStrategy.getInstance(context);
        return new TimeZoneDetectorService(context, timeZoneDetector);
    }

    @VisibleForTesting
    public TimeZoneDetectorService(@NonNull Context context,
            @NonNull TimeZoneDetectorStrategy timeZoneDetectorStrategy) {
        mContext = Objects.requireNonNull(context);
        mTimeZoneDetectorStrategy = Objects.requireNonNull(timeZoneDetectorStrategy);
    }

    @Override
    public void suggestPhoneTimeZone(@NonNull PhoneTimeZoneSuggestion timeZoneSuggestion) {
        enforceSetTimeZonePermission();
        Objects.requireNonNull(timeZoneSuggestion);

        long idToken = Binder.clearCallingIdentity();
        try {
            synchronized (mStrategyLock) {
                mTimeZoneDetectorStrategy.suggestPhoneTimeZone(timeZoneSuggestion);
            }
        } finally {
            Binder.restoreCallingIdentity(idToken);
        }
    }

    @Override
    protected void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter pw,
            @Nullable String[] args) {
        if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) return;

        synchronized (mStrategyLock) {
            mTimeZoneDetectorStrategy.dumpState(pw);
            mTimeZoneDetectorStrategy.dumpLogs(new IndentingPrintWriter(pw, " "));
        }
    }

    private void enforceSetTimeZonePermission() {
        mContext.enforceCallingPermission(
                android.Manifest.permission.SET_TIME_ZONE, "set time zone");
    }
}

