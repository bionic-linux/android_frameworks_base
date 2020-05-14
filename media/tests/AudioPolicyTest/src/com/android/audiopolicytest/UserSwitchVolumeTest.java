/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.audiopolicytest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.testng.Assert.assertThrows;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.UserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.audiopolicy.AudioProductStrategy;
import android.media.audiopolicy.AudioVolumeGroup;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

public class UserSwitchVolumeTest extends AudioVolumesTestBase {
    private static final String TAG = "UserSwitchVolumeTest";
    private static final int REMOVE_CHECK_INTERVAL_MILLIS = 500; // 0.5 seconds
    private static final int REMOVE_TIMEOUT_MILLIS = 60 * 1000; // 60 seconds
    private static final int SWITCH_USER_TIMEOUT_MILLIS = 40 * 1000; // 40 seconds

    final List<Integer> mPublicStreams = Ints.asList(PUBLIC_STREAM_TYPES);
    private AudioVolumeGroupCallbackHelper mVgCbReceiver;

    private UserManager mUserManager = null;
    private List<Integer> mUsersToRemove;

    private int mStartUser;
    private ActivityManager mAm;
    private IActivityManager mIam;
    private Context mContexteux;

    private final int mDefaultSystemVolume =
            SystemProperties.getInt("ro.config.system_vol_default", -1);
    private final int mDefaultAlarmVolume =
            SystemProperties.getInt("ro.config.alarm_vol_default", -1);
    private final int mDefaultMusicVolume =
            SystemProperties.getInt("ro.config.media_vol_default", -1);
    private final int mDefaultCallVolume =
            SystemProperties.getInt("ro.config.vc_call_vol_default", -1);

    final class UserVolumes {
        Integer mId;
        Map<Integer, Integer> mVolumeGroupVolumes = new HashMap<>();
    }

    Map<Integer, UserVolumes> mUserVolumes = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mContexteux = InstrumentationRegistry.getTargetContext();
        mUserManager = UserManager.get(mContexteux);
        mAm = mContexteux.getSystemService(ActivityManager.class);
        mIam = ActivityManager.getService();
        mUsersToRemove = new ArrayList<>();

        mStartUser = mAm.getCurrentUser();

        removeExistingUsers();
    }

    @After
    public void tearDown() throws Exception {
        if (mStartUser != mAm.getCurrentUser()) {
            switchUser(mStartUser);
        }
        for (Integer userId : mUsersToRemove) {
            mUserManager.removeUser(userId);
        }
        mAudioManager.unregisterVolumeGroupCallback(mVgCbReceiver);
        super.tearDown();
    }

    private void removeExistingUsers() {
        final List<UserInfo> list = mUserManager.getUsers();
        for (UserInfo user : list) {
            if (user.id != UserHandle.USER_SYSTEM && !user.isPrimary() && user.id != 10) {
                removeUser(user.id);
            }
        }
    }

    private void removeUser(int userId) {
        try {
            Log.i(TAG, "removeUser mUserManager.removeUser " + userId);
            mUserManager.removeUser(userId);
            final long startTime = System.currentTimeMillis();
            final long timeoutInMs = REMOVE_TIMEOUT_MILLIS;
            while (mUserManager.getUserInfo(userId) != null &&
                    System.currentTimeMillis() - startTime < timeoutInMs) {
                TimeUnit.MILLISECONDS.sleep(REMOVE_CHECK_INTERVAL_MILLIS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Ignore
        }
        if (mUserManager.getUserInfo(userId) != null) {
            mUsersToRemove.add(userId);
        }
    }

    private UserInfo createUser(String name, int flags) {
        UserInfo user = mUserManager.createUser(name, flags);
        if (user != null) {
            mUsersToRemove.add(user.id);
        }
        return user;
    }

    private void switchUser(int userId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        registerUserSwitchObserver(latch, null, userId);
        mAm.switchUser(userId);
        assertTrue("Failed to achieve switch to user " + userId, latch.await(30, TimeUnit.SECONDS));
    }

    private void registerBroadcastReceiver(final String action, final CountDownLatch latch,
            final int userId) {
        InstrumentationRegistry.getContext().registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (action.equals(intent.getAction()) && intent.getIntExtra(
                        Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL) == userId) {
                    latch.countDown();
                }
            }
        }, UserHandle.of(userId), new IntentFilter(action), null, null);
    }

    private void registerUserSwitchObserver(final CountDownLatch switchLatch,
                final CountDownLatch bootCompleteLatch, final int userId) throws Exception {
        ActivityManager.getService().registerUserSwitchObserver(
                new UserSwitchObserver() {
                    @Override
                    public void onUserSwitchComplete(int newUserId) throws RemoteException {
                        if (switchLatch != null && userId == newUserId) {
                            switchLatch.countDown();
                        }
                    }

                    @Override
                    public void onLockedBootComplete(int newUserId) {
                        if (bootCompleteLatch != null && userId == newUserId) {
                            bootCompleteLatch.countDown();
                        }
                    }
                }, TAG);
    }

    @LargeTest
    public void testUserSwitch() throws Exception {
        ActivityManager am = mContexteux.getSystemService(ActivityManager.class);
        final int startUser = am.getCurrentUser();

        List<AudioVolumeGroup> audioVolumeGroups = mAudioManager.getAudioVolumeGroups();
        assertTrue(audioVolumeGroups.size() > 0);

        mVgCbReceiver = new AudioVolumeGroupCallbackHelper();
        mAudioManager.registerVolumeGroupCallback(mContexteux.getMainExecutor(), mVgCbReceiver);

        UserVolumes startUserVolumes = new UserVolumes();

        // Validate Audio Volume Groups callback reception
        for (final AudioVolumeGroup avg : audioVolumeGroups) {
            int volumeGroupId = avg.getId();

            final AudioAttributes aa = getVolumeControlAttributes(avg);
            if (aa == null) {
                // Cannot address mute API by attributes
                continue;
            }
            // Set by Attributes
            mVgCbReceiver.setExpectedVolumeGroup(avg.getId());
            final int indexMin = mAudioManager.getMinVolumeIndexForAttributes(aa);
            final int indexMax = mAudioManager.getMaxVolumeIndexForAttributes(aa);
            final int index = resetVolumeIndex(indexMin, indexMax);
            mAudioManager.setVolumeIndexForAttributes(aa, index, 0/*flags*/);

            assertTrue(mVgCbReceiver.waitForExpectedVolumeGroupChanged(
                    AudioVolumeGroupCallbackHelper.ASYNC_TIMEOUT_MS));

            assertEquals(index, mAudioManager.getVolumeIndexForAttributes(aa));
            for (final int stream : avg.getLegacyStreamTypes()) {
                if (stream != AudioSystem.STREAM_DEFAULT && mPublicStreams.contains(stream)) {
                    assertEquals(index, mAudioManager.getStreamVolume(stream));
                }
            }
            startUserVolumes.mVolumeGroupVolumes.put(volumeGroupId, index);
        }
        mUserVolumes.put(startUser, startUserVolumes);

        // Now create and switch to a new user
        UserInfo userTest = createUser("Test", UserInfo.FLAG_EPHEMERAL | UserInfo.FLAG_DEMO);
        assertNotNull(userTest);
        final CountDownLatch latch = new CountDownLatch(1);
        registerBroadcastReceiver(Intent.ACTION_USER_UNLOCKED, latch, userTest.id);
        mAm.switchUser(userTest.id);
        assertTrue("Failed to achieve initial ACTION_USER_UNLOCKED for user " + userTest.id,
                latch.await(30, TimeUnit.SECONDS));

        // Check all volumes went back to default
        UserVolumes userTestVolumes = new UserVolumes();
        // Validate Audio Volume Groups callback reception
        for (final AudioVolumeGroup avg : audioVolumeGroups) {
            int volumeGroupId = avg.getId();

            final AudioAttributes aa = getVolumeControlAttributes(avg);
            if (aa == null) {
                // Cannot address mute API by attributes
                continue;
            }
            final int defaultIndex = mAudioManager.getVolumeIndexForAttributes(aa);
            Log.i(TAG, "getVolumeIndexForAttributes " + defaultIndex + " for " + avg.name());
            for (final int stream : avg.getLegacyStreamTypes()) {
                if (stream != AudioSystem.STREAM_DEFAULT && mPublicStreams.contains(stream)) {
                    assertEquals(defaultIndex, mAudioManager.getStreamVolume(stream));
                }
                final int groupForStream = super.getVolumeGroupForStreamType(stream);
                if (groupForStream == volumeGroupId) {
                    if (stream == AudioSystem.STREAM_MUSIC && mDefaultMusicVolume != -1) {
                        assertEquals(mDefaultMusicVolume, defaultIndex);
                    } else if (stream == AudioSystem.STREAM_SYSTEM && mDefaultSystemVolume != -1) {
                        assertEquals(mDefaultSystemVolume, defaultIndex);
                    } else if (stream == AudioSystem.STREAM_VOICE_CALL
                            && mDefaultCallVolume != -1) {
                        assertEquals(mDefaultCallVolume, defaultIndex);
                    } else if (stream == AudioSystem.STREAM_ALARM && mDefaultAlarmVolume != -1) {
                        assertEquals(mDefaultAlarmVolume, defaultIndex);
                    }
                }
            }
            userTestVolumes.mVolumeGroupVolumes.put(volumeGroupId, defaultIndex);
        }
        mUserVolumes.put(userTest.id, userTestVolumes);


        // Set new volume for new user
        // Validate Audio Volume Groups callback reception
        checkVolumesForUser(userTest.id);

        for (final AudioVolumeGroup avg : audioVolumeGroups) {
            String msg = "Testing group " + avg.name();
            int volumeGroupId = avg.getId();

            final AudioAttributes aa = getVolumeControlAttributes(avg);
            if (aa == null) {
                // Cannot address mute API by attributes
                continue;
            }
            mVgCbReceiver.setExpectedVolumeGroup(avg.getId());
            final int indexMax = mAudioManager.getMaxVolumeIndexForAttributes(aa);
            mAudioManager.setVolumeIndexForAttributes(aa, indexMax, 0/*flags*/);

            assertTrue(mVgCbReceiver.waitForExpectedVolumeGroupChanged(
                    AudioVolumeGroupCallbackHelper.ASYNC_TIMEOUT_MS));

            assertEquals(indexMax, mAudioManager.getVolumeIndexForAttributes(aa));
            for (final int stream : avg.getLegacyStreamTypes()) {
                if (stream != AudioSystem.STREAM_DEFAULT && mPublicStreams.contains(stream)) {
                    assertEquals(indexMax, mAudioManager.getStreamVolume(stream));
                }
            }
            userTestVolumes.mVolumeGroupVolumes.replace(volumeGroupId, indexMax);
        }
        mUserVolumes.replace(userTest.id, userTestVolumes);

        // Switch back to the starting user.
        switchUser(startUser);
        Thread.sleep(5000);
        checkVolumesForUser(startUser);
    }

    private void checkVolumesForUser(int userId) {
        final UserVolumes userVolumes = mUserVolumes.get(userId);
        List<AudioVolumeGroup> audioVolumeGroups = mAudioManager.getAudioVolumeGroups();

        for (final AudioVolumeGroup avg : audioVolumeGroups) {
            int volumeGroupId = avg.getId();

            final AudioAttributes aa = getVolumeControlAttributes(avg);
            if (aa == null) {
                // Cannot address mute API by attributes
                continue;
            }
            final int[] avgStreamTypes = avg.getLegacyStreamTypes();
            String msg = "User " + userId + ", group=" + avg.name() + ", expected "
                    + userVolumes.mVolumeGroupVolumes.get(volumeGroupId)
                    + ", got " + mAudioManager.getVolumeIndexForAttributes(aa);
            assertEquals(msg, (int)userVolumes.mVolumeGroupVolumes.get(volumeGroupId),
                    mAudioManager.getVolumeIndexForAttributes(aa));
            for (final int stream : avgStreamTypes) {
                if (stream != AudioSystem.STREAM_DEFAULT && mPublicStreams.contains(stream)) {
                    assertEquals(msg, (int)userVolumes.mVolumeGroupVolumes.get(volumeGroupId),
                            mAudioManager.getStreamVolume(stream));
                }
            }
        }
    }

    private boolean canUseStreamApi(@NonNull AudioVolumeGroup avg, @NonNull AudioAttributes aa,
            int avgStreamType) {
        final int indexMin = mAudioManager.getMinVolumeIndexForAttributes(aa);

        return avgStreamType != AudioSystem.STREAM_DEFAULT
                && mPublicStreams.contains(avgStreamType)
                && (indexMin == 0) && mAudioManager.isStreamAffectedByMute(avgStreamType);
    }

    private int getStreamVolumeForMute(@NonNull AudioVolumeGroup avg, @NonNull AudioAttributes aa) {
        final int indexMin = mAudioManager.getMinVolumeIndexForAttributes(aa);
        return Arrays.stream(avg.getLegacyStreamTypes())
                .filter(stream -> {
                    return (stream != AudioSystem.STREAM_DEFAULT)
                            && mPublicStreams.contains(stream)
                            && (indexMin == 0)
                            && mAudioManager.isStreamAffectedByMute(stream); })
                .findFirst().orElse(AudioSystem.STREAM_DEFAULT);
    }

    private @Nullable AudioAttributes getVolumeControlAttributes(@NonNull AudioVolumeGroup avg) {
        return avg.getAudioAttributes().stream().filter(aa -> !aa.equals(sDefaultAttributes))
                .findFirst().orElse(null);
    }
}
