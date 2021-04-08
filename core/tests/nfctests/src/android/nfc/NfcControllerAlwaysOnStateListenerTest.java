/*
 * Copyright 2021 The Android Open Source Project
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

package android.nfc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.nfc.NfcAdapter.ControllerAlwaysOnStateCallback;
import android.os.RemoteException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Test of {@link NfcControllerAlwaysOnStateListener}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class NfcControllerAlwaysOnStateListenerTest {

    INfcAdapter mNfcAdapter = mock(INfcAdapter.class);

    Answer mRegisterSuccessAnswer = new Answer() {
        public Object answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            INfcControllerAlwaysOnStateCallback cb =
                    (INfcControllerAlwaysOnStateCallback) args[0];
            try {
                cb.onControllerAlwaysOnStateChanged(true);
            } catch (RemoteException e) {
                // Nothing to do
            }
            return new Object();
        }
    };

    Throwable mThrowRemoteException = new RemoteException("RemoteException");

    private static Executor getExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private static void verifyCallbackStateChangedInvoked(
            ControllerAlwaysOnStateCallback callback, int numTimes) {
        verify(callback, times(numTimes)).onStateChanged(anyBoolean());
    }

    @Test
    public void testRegister_RegisterUnregister() throws RemoteException {
        NfcControllerAlwaysOnStateListener mListener =
                new NfcControllerAlwaysOnStateListener(mNfcAdapter);
        ControllerAlwaysOnStateCallback callback1 = mock(ControllerAlwaysOnStateCallback.class);
        ControllerAlwaysOnStateCallback callback2 = mock(ControllerAlwaysOnStateCallback.class);

        // Verify that the state listener registered with the NFC Adapter
        mListener.register(getExecutor(), callback1);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnStateCallback(any());

        // Register a second client and no new call to NFC Adapter
        mListener.register(getExecutor(), callback2);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnStateCallback(any());

        // Unregister first callback
        mListener.unregister(callback1);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnStateCallback(any());
        verify(mNfcAdapter, times(0)).unregisterControllerAlwaysOnStateCallback(any());

        // Unregister second callback and the state listener registered with the NFC Adapter
        mListener.unregister(callback2);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnStateCallback(any());
        verify(mNfcAdapter, times(1)).unregisterControllerAlwaysOnStateCallback(any());
    }

    @Test
    public void testRegister_FirstRegisterFails() throws RemoteException {
        NfcControllerAlwaysOnStateListener mListener =
                new NfcControllerAlwaysOnStateListener(mNfcAdapter);
        ControllerAlwaysOnStateCallback callback1 = mock(ControllerAlwaysOnStateCallback.class);
        ControllerAlwaysOnStateCallback callback2 = mock(ControllerAlwaysOnStateCallback.class);

        // Throw a remote exception whenever first registering
        doThrow(mThrowRemoteException).when(mNfcAdapter).registerControllerAlwaysOnStateCallback(
                any());

        mListener.register(getExecutor(), callback1);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnStateCallback(any());

        // No longer throw an exception, instead succeed
        doNothing().when(mNfcAdapter).registerControllerAlwaysOnStateCallback(any());

        // Register a different callback
        mListener.register(getExecutor(), callback2);
        verify(mNfcAdapter, times(2)).registerControllerAlwaysOnStateCallback(any());

        // Ensure first and second callback were invoked
        mListener.onControllerAlwaysOnStateChanged(true);
        verifyCallbackStateChangedInvoked(callback1, 1);
        verifyCallbackStateChangedInvoked(callback2, 1);
    }

    @Test
    public void testRegister_RegisterSameCallbackTwice() throws RemoteException {
        NfcControllerAlwaysOnStateListener mListener =
                new NfcControllerAlwaysOnStateListener(mNfcAdapter);
        ControllerAlwaysOnStateCallback callback = mock(ControllerAlwaysOnStateCallback.class);

        // Register the same callback Twice
        mListener.register(getExecutor(), callback);
        mListener.register(getExecutor(), callback);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnStateCallback(any());

        // Invoke a state change and ensure the callback is only called once
        mListener.onControllerAlwaysOnStateChanged(true);
        verifyCallbackStateChangedInvoked(callback, 1);
    }

    @Test
    public void testNotify_AllCallbacksNotified() throws RemoteException {

        NfcControllerAlwaysOnStateListener adapterStateListener =
                new NfcControllerAlwaysOnStateListener(mNfcAdapter);
        List<ControllerAlwaysOnStateCallback> callbacks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ControllerAlwaysOnStateCallback callback = mock(ControllerAlwaysOnStateCallback.class);
            adapterStateListener.register(getExecutor(), callback);
            callbacks.add(callback);
        }

        // Invoke a state change and ensure all callbacks are invoked
        adapterStateListener.onControllerAlwaysOnStateChanged(true);
        for (ControllerAlwaysOnStateCallback callback : callbacks) {
            verifyCallbackStateChangedInvoked(callback, 1);
        }
    }

    @Test
    public void testStateChange_CorrectValue() {
        NfcControllerAlwaysOnStateListener adapterStateListener =
                new NfcControllerAlwaysOnStateListener(mNfcAdapter);
        ControllerAlwaysOnStateCallback callback = mock(ControllerAlwaysOnStateCallback.class);
        adapterStateListener.register(getExecutor(), callback);
        runStateChangeValue(true, true);
        runStateChangeValue(false, false);

    }

    private void runStateChangeValue(boolean isEnabledIn, boolean isEnabledOut) {
        NfcControllerAlwaysOnStateListener adapterStateListener =
                new NfcControllerAlwaysOnStateListener(mNfcAdapter);
        ControllerAlwaysOnStateCallback callback = mock(ControllerAlwaysOnStateCallback.class);
        adapterStateListener.register(getExecutor(), callback);
        adapterStateListener.onControllerAlwaysOnStateChanged(isEnabledIn);
        verify(callback, times(1)).onStateChanged(isEnabledOut);
    }
}
