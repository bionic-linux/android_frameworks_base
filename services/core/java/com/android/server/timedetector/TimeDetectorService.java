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

package com.android.server.timedetector;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.timedetector.ITimeDetectorService;
import android.app.timedetector.TimeSignal;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.provider.Settings;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.timedetector.TimeDetectorStrategy.Callback;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;

public final class TimeDetectorService extends ITimeDetectorService.Stub {
    private static final String TAG = "timedetector.TimeDetectorService";

    public static class Lifecycle extends SystemService {

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            TimeDetectorService service = TimeDetectorService.create(getContext());

            // Publish the binder service so it can be accessed from other (appropriately
            // permissioned) processes.
            publishBinderService(Context.TIME_DETECTOR_SERVICE, service);
        }
    }

    private final Context mContext;
    private final Callback mCallback;

    // The handler to use whenever communicating with the strategy.
    private final Handler mHandler;
    private final TimeDetectorStrategy mTimeDetectorStrategy;

    private static TimeDetectorService create(Context context) {
        final TimeDetectorStrategy timeDetector = new SimpleTimeDetectorStrategy();
        final TimeDetectorStrategyCallbackImpl callback =
                new TimeDetectorStrategyCallbackImpl(context);
        timeDetector.initialize(callback);

        // All time detection operations are handled in a single thread for simplicity.
        HandlerThread handlerThread =
                new HandlerThread("TimeDetectorService", Process.THREAD_PRIORITY_DEFAULT);
        handlerThread.start();
        Handler handler = handlerThread.getThreadHandler();

        TimeDetectorService timeDetectorService =
                new TimeDetectorService(context, handler, callback, timeDetector);

        // Wire up event listening.
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AUTO_TIME), true,
                new ContentObserver(new Handler()) {
                    public void onChange(boolean selfChange) {
                        timeDetectorService.handleAutoTimeDetectionToggle();
                    }
                });

        return timeDetectorService;
    }

    @VisibleForTesting
    public TimeDetectorService(@NonNull Context context,
            @NonNull Handler handler,
            @NonNull Callback callback,
            @NonNull TimeDetectorStrategy timeDetectorStrategy) {
        mContext = Objects.requireNonNull(context);
        mCallback = Objects.requireNonNull(callback);
        mHandler = handler;
        mTimeDetectorStrategy = Objects.requireNonNull(timeDetectorStrategy);
    }

    @Override
    public void suggestTime(@NonNull TimeSignal timeSignal) {
        enforceSetTimePermission();

        long callerIdToken = Binder.clearCallingIdentity();
        try {
            mHandler.post(() -> mTimeDetectorStrategy.suggestTime(timeSignal));
        } finally {
            Binder.restoreCallingIdentity(callerIdToken);
        }
    }

    @VisibleForTesting
    public void handleAutoTimeDetectionToggle() {
        final boolean timeDetectionEnabled = mCallback.isTimeDetectionEnabled();
        mHandler.post(
                () -> mTimeDetectorStrategy.handleAutoTimeDetectionToggle(timeDetectionEnabled));
    }

    @Override
    protected void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter pw,
            @Nullable String[] args) {
        if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) return;

        // We guarantee the strategy object is single threaded, so run the dump on the handler and
        // wait for the response. We don't want to set a timeout as then the printwriter could
        // be shared across multiple threads.
        mHandler.runWithScissors(() -> mTimeDetectorStrategy.dump(pw, args), 0);
    }

    private void enforceSetTimePermission() {
        mContext.enforceCallingPermission(android.Manifest.permission.SET_TIME, "set time");
    }
}
