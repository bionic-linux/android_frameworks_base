/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.os.IBinder;

/**
 * An interface for NetworkTimeUpdateService implementations. Eventually part or all of this service
 * will be subsumed into {@link com.android.server.timedetector.TimeDetectorService}. In the
 * meantime this interface allows Android to use either the old or new implementation.
 */
public interface NetworkTimeUpdateService extends IBinder {

    /** Initialize the receivers and initiate the first NTP request */
<<<<<<< HEAD
    void systemRunning();
=======
    public void systemRunning() {
        registerForTelephonyIntents();
        registerForAlarms();

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new MyHandler(thread.getLooper());
        mNetworkTimeUpdateCallback = new NetworkTimeUpdateCallback();
        mCM.registerDefaultNetworkCallback(mNetworkTimeUpdateCallback, mHandler);

        mSettingsObserver = new SettingsObserver(mHandler, EVENT_AUTO_TIME_CHANGED);
        mSettingsObserver.observe(mContext);
    }

    private void registerForTelephonyIntents() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_NETWORK_SET_TIME);
        mContext.registerReceiver(mNitzReceiver, intentFilter);
    }

    private void registerForAlarms() {
        mContext.registerReceiver(
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mHandler.obtainMessage(EVENT_POLL_NETWORK_TIME).sendToTarget();
                }
            }, new IntentFilter(ACTION_POLL));
    }

    private void onPollNetworkTime(int event) {
        // If Automatic time is not set, don't bother. Similarly, if we don't
        // have any default network, don't bother.
        if (mDefaultNetwork == null) return;
        mWakeLock.acquire();
        try {
            onPollNetworkTimeUnderWakeLock(event);
        } finally {
            mWakeLock.release();
        }
    }

    private void onPollNetworkTimeUnderWakeLock(int event) {
        // Force an NTP fix when outdated
        if (mTime.getCacheAge() >= mPollingIntervalMs) {
            if (DBG) Log.d(TAG, "Stale NTP fix; forcing refresh");
            mTime.forceSync();
        }

        if (mTime.getCacheAge() < mPollingIntervalMs) {
            // Obtained fresh fix; schedule next normal update
            resetAlarm(mPollingIntervalMs);
            if (isAutomaticTimeRequested()) {
                updateSystemClock(event);
            }

        } else {
            // No fresh fix; schedule retry
            mTryAgainCounter++;
            if (mTryAgainTimesMax < 0 || mTryAgainCounter <= mTryAgainTimesMax) {
                resetAlarm(mPollingIntervalShorterMs);
            } else {
                // Try much later
                mTryAgainCounter = 0;
                resetAlarm(mPollingIntervalMs);
            }
        }
    }

    private long getNitzAge() {
        if (mNitzTimeSetTime == NOT_SET) {
            return Long.MAX_VALUE;
        } else {
            return SystemClock.elapsedRealtime() - mNitzTimeSetTime;
        }
    }

    /**
     * Consider updating system clock based on current NTP fix, if requested by
     * user, significant enough delta, and we don't have a recent NITZ.
     */
    private void updateSystemClock(int event) {
        final boolean forceUpdate = (event == EVENT_AUTO_TIME_CHANGED);
        if (!forceUpdate) {
            if (getNitzAge() < mPollingIntervalMs) {
                if (DBG) Log.d(TAG, "Ignoring NTP update due to recent NITZ");
                return;
            }

            final long skew = Math.abs(mTime.currentTimeMillis() - System.currentTimeMillis());
            if (skew < mTimeErrorThresholdMs) {
                if (DBG) Log.d(TAG, "Ignoring NTP update due to low skew");
                return;
            }
        }

        SystemClock.setCurrentTimeMillis(mTime.currentTimeMillis());
    }

    /**
     * Cancel old alarm and starts a new one for the specified interval.
     *
     * @param interval when to trigger the alarm, starting from now.
     */
    private void resetAlarm(long interval) {
        mAlarmManager.cancel(mPendingPollIntent);
        long now = SystemClock.elapsedRealtime();
        long next = now + interval;
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, next, mPendingPollIntent);
    }

    /**
     * Checks if the user prefers to automatically set the time.
     */
    private boolean isAutomaticTimeRequested() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.AUTO_TIME, 0) != 0;
    }

    /** Receiver for Nitz time events */
    private BroadcastReceiver mNitzReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) Log.d(TAG, "Received " + action);
            if (TelephonyIntents.ACTION_NETWORK_SET_TIME.equals(action)) {
                mNitzTimeSetTime = SystemClock.elapsedRealtime();
            }
        }
    };

    /** Handler to do the network accesses on */
    private class MyHandler extends Handler {

        public MyHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_AUTO_TIME_CHANGED:
                case EVENT_POLL_NETWORK_TIME:
                case EVENT_NETWORK_CHANGED:
                    onPollNetworkTime(msg.what);
                    break;
            }
        }
    }

    private class NetworkTimeUpdateCallback extends NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.d(TAG, String.format("New default network %s; checking time.", network));
            mDefaultNetwork = network;
            // Running on mHandler so invoke directly.
            onPollNetworkTime(EVENT_NETWORK_CHANGED);
        }

        @Override
        public void onLost(Network network) {
            if (network.equals(mDefaultNetwork)) mDefaultNetwork = null;
        }
    }

    /** Observer to watch for changes to the AUTO_TIME setting */
    private static class SettingsObserver extends ContentObserver {

        private int mMsg;
        private Handler mHandler;

        SettingsObserver(Handler handler, int msg) {
            super(handler);
            mHandler = handler;
            mMsg = msg;
        }

        void observe(Context context) {
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.AUTO_TIME),
                    false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            mHandler.obtainMessage(mMsg).sendToTarget();
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) return;
        pw.print("PollingIntervalMs: ");
        TimeUtils.formatDuration(mPollingIntervalMs, pw);
        pw.print("\nPollingIntervalShorterMs: ");
        TimeUtils.formatDuration(mPollingIntervalShorterMs, pw);
        pw.println("\nTryAgainTimesMax: " + mTryAgainTimesMax);
        pw.print("TimeErrorThresholdMs: ");
        TimeUtils.formatDuration(mTimeErrorThresholdMs, pw);
        pw.println("\nTryAgainCounter: " + mTryAgainCounter);
        pw.println("NTP cache age: " + mTime.getCacheAge());
        pw.println("NTP cache certainty: " + mTime.getCacheCertainty());
        pw.println();
    }
>>>>>>> 185e7c3... NtpTrustedTime:Abstract forceSync and forceRefresh
}
