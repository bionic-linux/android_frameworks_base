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

package com.android.server.timezonedetector;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.timezonedetector.ITimeZoneDetectorConfigurationListener;
import android.app.timezonedetector.ITimeZoneDetectorService;
import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import android.app.timezonedetector.TimeZoneDetectorConfiguration;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.timezonedetector.TimeZoneDetectorStrategy.StrategyListener;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The implementation of ITimeZoneDetectorService.aidl.
 */
public final class TimeZoneDetectorService extends ITimeZoneDetectorService.Stub {
    private static final String TAG = "TimeZoneDetectorService";

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
    @NonNull private final Handler mHandler;
    @NonNull private final TimeZoneDetectorStrategy mTimeZoneDetectorStrategy;

    @GuardedBy("mConfigurationListeners")
    @NonNull private final ArrayList<ITimeZoneDetectorConfigurationListener>
            mConfigurationListeners = new ArrayList<>();

    private static TimeZoneDetectorService create(@NonNull Context context) {
        final TimeZoneDetectorStrategy timeZoneDetectorStrategy =
                TimeZoneDetectorStrategyImpl.create(context);

        Handler handler = FgThread.getHandler();
        TimeZoneDetectorService service =
                new TimeZoneDetectorService(context, handler, timeZoneDetectorStrategy);

        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AUTO_TIME_ZONE), true,
                new ContentObserver(handler) {
                    public void onChange(boolean selfChange) {
                        service.handleAutoTimeZoneDetectionChanged();
                    }
                });
        return service;
    }

    @VisibleForTesting
    public TimeZoneDetectorService(@NonNull Context context, @NonNull Handler handler,
            @NonNull TimeZoneDetectorStrategy timeZoneDetectorStrategy) {
        mContext = Objects.requireNonNull(context);
        mHandler = Objects.requireNonNull(handler);
        mTimeZoneDetectorStrategy = Objects.requireNonNull(timeZoneDetectorStrategy);
        mTimeZoneDetectorStrategy.setStrategyListener(new StrategyListener() {
            @Override
            public void onConfigurationChanged(TimeZoneDetectorConfiguration configuration) {
                handleConfigurationChanged(configuration);
            }
        });
    }

    @Override
    @NonNull
    public TimeZoneDetectorConfiguration getConfiguration() {
        enforceManageTimeZoneDetectorConfigurationPermission();
        return mTimeZoneDetectorStrategy.getConfiguration();
    }

    @Override
    public void updateConfiguration(@NonNull TimeZoneDetectorConfiguration configuration) {
        enforceManageTimeZoneDetectorConfigurationPermission();
        Objects.requireNonNull(configuration);

        // This is implemented synchronously for cases where the client wants to wait to know the
        // configuration has actually been updated.
        // TODO: Check that doing it synchronously also ensures that the caller must have permission
        //  to change the settings, e.g. device policy checks? Should this do a DevicePolicyManager
        //  check explicitly?
        mTimeZoneDetectorStrategy.updateConfiguration(configuration);
    }

    @Override
    public void addConfigurationListener(@NonNull ITimeZoneDetectorConfigurationListener listener) {
        enforceManageTimeZoneDetectorConfigurationPermission();
        Objects.requireNonNull(listener);

        final IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                synchronized (mConfigurationListeners) {
                    Slog.i(TAG, "Configuration listener died: " + listener);
                    mConfigurationListeners.remove(listener);
                }
            }
        };

        synchronized (mConfigurationListeners) {
            try {
                // Remove the record of listener if the client process dies.
                listener.asBinder().linkToDeath(deathRecipient, 0 /* flags */);

                // Only add the listener if we can linkToDeath().
                mConfigurationListeners.add(listener);
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to linkToDeath() for listener=" + listener, e);
            }
        }
    }

    void handleConfigurationChanged(@NonNull TimeZoneDetectorConfiguration configuration) {
        Objects.requireNonNull(configuration);

        synchronized (mConfigurationListeners) {
            for (ITimeZoneDetectorConfigurationListener listener : mConfigurationListeners) {
                try {
                    listener.onChange(configuration);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Unable to notify listener="
                            + listener + " of updated configuration=" + configuration, e);
                }
            }
        }
    }

    @Override
    public void suggestManualTimeZone(@NonNull ManualTimeZoneSuggestion timeZoneSuggestion) {
        enforceSuggestManualTimeZonePermission();
        Objects.requireNonNull(timeZoneSuggestion);

        mHandler.post(() -> mTimeZoneDetectorStrategy.suggestManualTimeZone(timeZoneSuggestion));
    }

    @Override
    public void suggestTelephonyTimeZone(@NonNull TelephonyTimeZoneSuggestion timeZoneSuggestion) {
        enforceSuggestTelephonyTimeZonePermission();
        Objects.requireNonNull(timeZoneSuggestion);

        mHandler.post(() -> mTimeZoneDetectorStrategy.suggestTelephonyTimeZone(timeZoneSuggestion));
    }

    @Override
    protected void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter pw,
            @Nullable String[] args) {
        if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) return;

        mTimeZoneDetectorStrategy.dump(pw, args);
    }

    /** Internal method for handling the auto time zone setting being changed. */
    @VisibleForTesting
    public void handleAutoTimeZoneDetectionChanged() {
        mHandler.post(mTimeZoneDetectorStrategy::handleAutoTimeZoneDetectionChanged);
    }

    private void enforceManageTimeZoneDetectorConfigurationPermission() {
        // TODO Switch to a dedicated MANAGE_TIME_AND_ZONE_CONFIGURATION permission.
        mContext.enforceCallingPermission(
                android.Manifest.permission.WRITE_SECURE_SETTINGS,
                "manage time and time zone configuration");
    }

    private void enforceSuggestTelephonyTimeZonePermission() {
        mContext.enforceCallingPermission(
                android.Manifest.permission.SUGGEST_TELEPHONY_TIME_AND_ZONE,
                "suggest telephony time and time zone");
    }

    private void enforceSuggestManualTimeZonePermission() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.SUGGEST_MANUAL_TIME_AND_ZONE,
                "suggest manual time and time zone");
    }
}

