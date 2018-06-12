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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.timedetector.TimeSignal;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.HandlerThread;
import android.support.test.runner.AndroidJUnit4;
import android.util.TimestampedValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.android.server.timedetector.TimeDetectorStrategy.Callback;

import java.io.PrintWriter;

@RunWith(AndroidJUnit4.class)
public class TimeDetectorServiceTest {

    private Context mMockContext;
    private StubbedTimeDetectorStrategy mStubbedTimeDetectorStrategy;
    private HandlerThread mHandlerThread;
    private Callback mMockCallback;

    private TimeDetectorService mTimeDetectorService;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        mMockCallback = mock(Callback.class);
        mStubbedTimeDetectorStrategy = new StubbedTimeDetectorStrategy();
        mHandlerThread = new HandlerThread("TimeDetectorServiceTest");
        mHandlerThread.start();

        mTimeDetectorService = new TimeDetectorService(
                mMockContext, mHandlerThread.getThreadHandler(), mMockCallback,
                mStubbedTimeDetectorStrategy);
    }

    @Test(expected=SecurityException.class)
    public void testStubbedCall_withoutPermission() {
        doThrow(new SecurityException("Mock"))
                .when(mMockContext).enforceCallingPermission(anyString(), any());
        TimeSignal timeSignal = createNitzTimeSignal();

        try {
            mTimeDetectorService.suggestTime(timeSignal);
            waitForHandler(mHandlerThread);
        } finally {
            verify(mMockContext).enforceCallingPermission(
                    eq(android.Manifest.permission.SET_TIME), anyString());
        }
    }

    @Test
    public void testSuggestTime() {
        doNothing().when(mMockContext).enforceCallingPermission(anyString(), any());

        TimeSignal timeSignal = createNitzTimeSignal();
        mTimeDetectorService.suggestTime(timeSignal);
        waitForHandler(mHandlerThread);

        verify(mMockContext)
                .enforceCallingPermission(eq(android.Manifest.permission.SET_TIME), anyString());
        mStubbedTimeDetectorStrategy.verifySuggestTimeCalled(mHandlerThread, timeSignal);
    }

    @Test
    public void testDump() {
        when(mMockContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        mTimeDetectorService.dump(null, null, null);
        waitForHandler(mHandlerThread);

        verify(mMockContext).checkCallingOrSelfPermission(eq(android.Manifest.permission.DUMP));
        mStubbedTimeDetectorStrategy.verifyDumpCalled(mHandlerThread);
    }

    @Test
    public void testAutoTimeDetectionToggle() {
        when(mMockCallback.isTimeDetectionEnabled()).thenReturn(true);

        mTimeDetectorService.handleAutoTimeDetectionToggle();
        waitForHandler(mHandlerThread);

        mStubbedTimeDetectorStrategy.verifyHandleAutoTimeDetectionToggleCalled(
                mHandlerThread, true);

        when(mMockCallback.isTimeDetectionEnabled()).thenReturn(false);

        mTimeDetectorService.handleAutoTimeDetectionToggle();
        waitForHandler(mHandlerThread);

        mStubbedTimeDetectorStrategy.verifyHandleAutoTimeDetectionToggleCalled(
                mHandlerThread, false);
    }

    private static void waitForHandler(HandlerThread handlerThread) {
        // Add an event to the queue. When it completes we know we are done.
        handlerThread.getThreadHandler().runWithScissors(() -> {}, 0);
    }

    private static TimeSignal createNitzTimeSignal() {
        TimestampedValue<Long> timeValue = new TimestampedValue<>(100L, 1_000_000L);
        return new TimeSignal(TimeSignal.SOURCE_ID_NITZ, timeValue);
    }

    private static class StubbedTimeDetectorStrategy implements TimeDetectorStrategy {

        // Call tracking.
        private Thread mLastThread;
        private TimeSignal mLastSuggestedTime;
        private Boolean mLastAutoTimeDetectionToggle;
        private boolean mDumpCalled;

        @Override
        public void initialize(Callback ignored) {
        }

        @Override
        public void suggestTime(TimeSignal timeSignal) {
            resetCallTracking();
            mLastThread = Thread.currentThread();
            mLastSuggestedTime = timeSignal;
        }

        @Override
        public void handleAutoTimeDetectionToggle(boolean enabled) {
            resetCallTracking();
            mLastThread = Thread.currentThread();
            mLastAutoTimeDetectionToggle = enabled;
        }

        @Override
        public void dump(PrintWriter pw, String[] args) {
            resetCallTracking();
            mLastThread = Thread.currentThread();
            mDumpCalled = true;
        }

        void resetCallTracking() {
            mLastThread = null;
            mLastSuggestedTime = null;
            mLastAutoTimeDetectionToggle = null;
            mDumpCalled = false;
        }

        void verifySuggestTimeCalled(Thread expectedThread, TimeSignal expectedSignal) {
            assertSame(expectedThread, mLastThread);
            assertEquals(expectedSignal, mLastSuggestedTime);
        }

        void verifyHandleAutoTimeDetectionToggleCalled(
                Thread expectedThread, boolean expectedEnable) {
            assertSame(expectedThread, mLastThread);
            assertNotNull(mLastAutoTimeDetectionToggle);
            assertEquals(expectedEnable, mLastAutoTimeDetectionToggle);
        }

        void verifyDumpCalled(Thread expectedThread) {
            assertSame(expectedThread, mLastThread);
            assertTrue(mDumpCalled);
        }
    }
}
