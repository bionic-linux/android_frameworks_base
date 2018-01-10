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
package com.android.server.usb;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbFunction;
import android.hardware.usb.UsbFunctions;
import android.hardware.usb.UsbManager;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.test.filters.SmallTest;
import android.support.test.InstrumentationRegistry;

import com.android.server.FgThread;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tests for UsbHandler state changes.
 */
@RunWith(JUnit4.class)
public class UsbHandlerTest {
    private static final String TAG = UsbHandlerTest.class.getSimpleName();

    @Mock
    private UsbDeviceManager mUsbDeviceManager;
    @Mock
    private UsbDebuggingManager mUsbDebuggingManager;
    @Mock
    private UsbAlsaManager mUsbAlsaManager;
    @Mock
    private UsbSettingsManager mUsbSettingsManager;
    @Mock
    private SharedPreferences mSharedPreferences;
    @Mock
    private SharedPreferences.Editor mEditor;

    private MockUsbHandler mUsbHandler;

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_ENABLE_ADB = 1;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final int MSG_SYSTEM_READY = 3;
    private static final int MSG_BOOT_COMPLETED = 4;
    private static final int MSG_USER_SWITCHED = 5;
    private static final int MSG_UPDATE_USER_RESTRICTIONS = 6;
    private static final int MSG_SET_SCREEN_UNLOCKED_FUNCTIONS = 12;
    private static final int MSG_UPDATE_SCREEN_LOCK = 13;

    private Map<String, String> mMockProperties;
    private Map<String, Integer> mMockGlobalSettings;

    private class MockUsbHandler extends UsbDeviceManager.UsbHandler {
        boolean mIsUsbTransferAllowed;
        Intent mBroadcastedIntent;

        MockUsbHandler(Looper looper, Context context, UsbDeviceManager deviceManager,
                UsbDebuggingManager debuggingManager, UsbAlsaManager alsaManager,
                UsbSettingsManager settingsManager) {
            super(looper, context, deviceManager, debuggingManager, alsaManager, settingsManager);
            mUseUsbNotification = false;
            mIsUsbTransferAllowed = true;
            mCurrentUsbFunctionsReceived = true;
        }

        @Override
        protected void setEnabledFunctions(UsbFunctions functions, boolean force) {
            mCurrentFunctions = functions;
        }

        @Override
        protected void setSystemProperty(String property, String value) {
            mMockProperties.put(property, value);
        }

        @Override
        protected void putGlobalSettings(ContentResolver resolver, String setting, int val) {
            mMockGlobalSettings.put(setting, val);
        }

        @Override
        protected String getSystemProperty(String property, String def) {
            if (mMockProperties.containsKey(property)) {
                return mMockProperties.get(property);
            }
            return def;
        }

        @Override
        protected boolean isUsbTransferAllowed() {
            return mIsUsbTransferAllowed;
        }

        @Override
        protected SharedPreferences getPinnedSharedPrefs(Context context) {
            return mSharedPreferences;
        }

        @Override
        protected void sendStickyBroadcast(Intent intent) {
            mBroadcastedIntent = intent;
        }
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        UsbDeviceManager.sDEBUG = true;
        mMockProperties = new HashMap<>();
        mMockGlobalSettings = new HashMap<>();
        when(mSharedPreferences.edit()).thenReturn(mEditor);

        mUsbHandler = new MockUsbHandler(FgThread.get().getLooper(),
                InstrumentationRegistry.getContext(), mUsbDeviceManager, mUsbDebuggingManager,
                mUsbAlsaManager, mUsbSettingsManager);
    }

    @After
    public void after() {
    }

    @Test
    @SmallTest
    public void testSetFunctionsMtp() {
        Message msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.MTP);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));
    }

    @Test
    @SmallTest
    public void testSetFunctionsPtp() {
        Message msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.PTP);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.PTP));
    }

    @Test
    @SmallTest
    public void testSetFunctionsMidi() {
        Message msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.MIDI);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MIDI));
    }

    @Test
    @SmallTest
    public void testSetFunctionsRndis() {
        Message msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.RNDIS);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.RNDIS));
    }

    @Test
    @SmallTest
    public void testEnableAdb() {
        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_ENABLE_ADB);
        msg.arg1 = 1;
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().empty());
        assertThat(mUsbHandler.mAdbEnabled);
        assertThat(mMockProperties.get(UsbDeviceManager.UsbHandler.USB_PERSISTENT_CONFIG_PROPERTY))
                .isEqualTo(UsbManager.USB_FUNCTION_ADB);
        verify(mUsbDebuggingManager).setAdbEnabled(true);

        msg = Message.obtain(mUsbHandler, MSG_UPDATE_STATE);
        msg.arg1 = 1;
        msg.arg2 = 1;
        mUsbHandler.handleMessage(msg);

        assertThat(mUsbHandler.mBroadcastedIntent.getBooleanExtra(UsbManager.USB_CONNECTED, false));
        assertThat(mUsbHandler.mBroadcastedIntent
                .getBooleanExtra(UsbManager.USB_CONFIGURED, false));
        assertThat(mUsbHandler.mBroadcastedIntent
                .getBooleanExtra(UsbManager.USB_FUNCTION_ADB, false));
    }

    @Test
    @SmallTest
    public void testDisableAdb() {
        mMockProperties.put(UsbDeviceManager.UsbHandler.USB_PERSISTENT_CONFIG_PROPERTY,
                UsbManager.USB_FUNCTION_ADB);
        mUsbHandler = new MockUsbHandler(FgThread.get().getLooper(),
                InstrumentationRegistry.getContext(), mUsbDeviceManager, mUsbDebuggingManager,
                mUsbAlsaManager, mUsbSettingsManager);

        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_ENABLE_ADB);
        msg.arg1 = 0;
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().empty());
        assertThat(!mUsbHandler.mAdbEnabled);
        assertThat(mMockProperties.get(UsbDeviceManager.UsbHandler.USB_PERSISTENT_CONFIG_PROPERTY))
                .isEqualTo("");
        verify(mUsbDebuggingManager).setAdbEnabled(false);
    }

    @Test
    @SmallTest
    public void testBootCompletedCharging() {
        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().empty());
    }

    @Test
    @SmallTest
    public void testBootCompletedAdbEnabled() {
        mMockProperties.put(UsbDeviceManager.UsbHandler.USB_PERSISTENT_CONFIG_PROPERTY, "adb");
        mUsbHandler = new MockUsbHandler(FgThread.get().getLooper(),
                InstrumentationRegistry.getContext(), mUsbDeviceManager, mUsbDebuggingManager,
                mUsbAlsaManager, mUsbSettingsManager);

        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().empty());
        assertThat(mMockGlobalSettings.get(Settings.Global.ADB_ENABLED)).isEqualTo(1);
        assertThat(mUsbHandler.mAdbEnabled);
        verify(mUsbDebuggingManager).setAdbEnabled(true);
    }

    @Test
    @SmallTest
    public void testUserSwitchedDisablesMtp() {
        Message msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.MTP);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));

        msg = Message.obtain(mUsbHandler, MSG_USER_SWITCHED);
        msg.arg1 = UserHandle.getCallingUserId() + 1;
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().empty());
    }

    @Test
    @SmallTest
    public void testChangedRestrictionsDisablesMtp() {
        Message msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.MTP);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));

        mUsbHandler.mIsUsbTransferAllowed = false;
        msg = Message.obtain(mUsbHandler, MSG_UPDATE_USER_RESTRICTIONS);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().empty());
    }

    @Test
    @SmallTest
    public void testDisconnectResetsCharging() {
        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);

        msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.MTP);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));

        msg = Message.obtain(mUsbHandler, MSG_UPDATE_STATE);
        msg.arg1 = 0;
        msg.arg2 = 0;
        mUsbHandler.handleMessage(msg);

        assertThat(mUsbHandler.getEnabledFunctions().empty());
    }

    @Test
    @SmallTest
    public void testConfiguredSendsBroadcast() {
        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);

        msg = Message.obtain(mUsbHandler, MSG_SET_CURRENT_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.MTP);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));

        msg = Message.obtain(mUsbHandler, MSG_UPDATE_STATE);
        msg.arg1 = 1;
        msg.arg2 = 1;
        mUsbHandler.handleMessage(msg);

        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));
        assertThat(mUsbHandler.mBroadcastedIntent.getBooleanExtra(UsbManager.USB_CONNECTED, false));
        assertThat(mUsbHandler.mBroadcastedIntent
                .getBooleanExtra(UsbManager.USB_CONFIGURED, false));
        assertThat(mUsbHandler.mBroadcastedIntent
                .getBooleanExtra(UsbManager.USB_FUNCTION_MTP, false));
    }

    @Test
    @SmallTest
    public void testSetScreenUnlockedFunctions() {
        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_UPDATE_SCREEN_LOCK);
        msg.arg1 = 0;
        mUsbHandler.handleMessage(msg);

        msg = Message.obtain(mUsbHandler, MSG_SET_SCREEN_UNLOCKED_FUNCTIONS);
        msg.obj = new UsbFunctions(UsbFunction.MTP);
        mUsbHandler.handleMessage(msg);
        assertThat(mUsbHandler.getScreenUnlockedFunctions().contains(UsbFunction.MTP));
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));
        verify(mEditor).putString(String.format(Locale.ENGLISH,
                UsbDeviceManager.UNLOCKED_CONFIG_PREF, mUsbHandler.mCurrentUser),
                UsbManager.USB_FUNCTION_MTP);
    }

    @Test
    @SmallTest
    public void testUnlockScreen() {
        when(mSharedPreferences.getString(String.format(Locale.ENGLISH,
                UsbDeviceManager.UNLOCKED_CONFIG_PREF, mUsbHandler.mCurrentUser), ""))
                .thenReturn(UsbManager.USB_FUNCTION_MTP);
        Message msg = Message.obtain(mUsbHandler, MSG_BOOT_COMPLETED);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_SYSTEM_READY);
        mUsbHandler.handleMessage(msg);
        msg = Message.obtain(mUsbHandler, MSG_UPDATE_SCREEN_LOCK);
        msg.arg1 = 0;
        mUsbHandler.handleMessage(msg);

        assertThat(mUsbHandler.getScreenUnlockedFunctions().contains(UsbFunction.MTP));
        assertThat(mUsbHandler.getEnabledFunctions().contains(UsbFunction.MTP));
    }
}