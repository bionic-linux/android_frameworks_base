package com.android.server.pm;

import static android.app.BackgroundInstallControlManager.Callback.FLAGGED_PACKAGE_NAME_KEY;
import static org.junit.Assert.assertEquals;

import android.os.Bundle;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.verify;
/**
 * Tests for {@link BackgroundInstallControlCallbackHelperTest}
 */
@Presubmit
public class BackgroundInstallControlCallbackHelperTest {

    @Mock
    private IRemoteCallback mBicCallback;
    @Captor
    private ArgumentCaptor<Bundle> mBundleCaptor;
    private BackgroundInstallControlCallbackHelper mCallbackHelper;
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mCallbackHelper = new BackgroundInstallControlCallbackHelper();
    }
    @Test
    public void registerBackgroundInstallControlCallback_registers_successfully() {
        mCallbackHelper.registerBackgroundInstallControlCallback(mBicCallback);

        assertEquals(1, mCallbackHelper.mCallbacks.getRegisteredCallbackCount());
        assertEquals(mBicCallback, mCallbackHelper.mCallbacks.getRegisteredCallbackItem(0));
    }

    @Test
    public void unregisterBackgroundInstallControlCallback_unregisters_successfully() {
        mCallbackHelper.mCallbacks.register(mBicCallback);

        mCallbackHelper.unregisterBackgroundInstallControlCallback(mBicCallback);

        assertEquals(0, mCallbackHelper.mCallbacks.getRegisteredCallbackCount());
    }

    @Test
    public void notifyAllCallbacks_broadcastsToCallbacks() throws RemoteException {
        String testPackageName = "testname";
        mCallbackHelper.registerBackgroundInstallControlCallback(mBicCallback);

        mCallbackHelper.notifyAllCallbacks(testPackageName);

        verify(mBicCallback).sendResult(mBundleCaptor.capture());
        Bundle receivedBundle = mBundleCaptor.getValue();
        assertEquals(testPackageName, receivedBundle.getString(FLAGGED_PACKAGE_NAME_KEY));
    }
}
