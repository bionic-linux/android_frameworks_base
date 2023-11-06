package com.android.server.pm;

import static android.app.BackgroundInstallControlManager.Callback.FLAGGED_PACAKGE_USER_ID_KEY;
import static android.app.BackgroundInstallControlManager.Callback.FLAGGED_PACKAGE_NAME_KEY;

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.IRemoteCallback;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

public class BackgroundInstallControlCallbackHelper {
    private static final String TAG = "BackgroundInstallControlCallbackHelper";
    @NonNull
    private final Object mLock = new Object();

    @NonNull
    @GuardedBy("mLock")
    @VisibleForTesting
    final RemoteCallbackList<IRemoteCallback> mCallbacks = new RemoteCallbackList<>();

    public void registerBackgroundInstallControlCallback(IRemoteCallback callback) {
        synchronized (mLock) {
            mCallbacks.register(callback, null);
        }
    }

    public void unregisterBackgroundInstallControlCallback(IRemoteCallback callback) {
        synchronized (mLock) {
            mCallbacks.unregister(callback);
        }
    }

    public void notifyAllCallbacks(String packageName) {
        synchronized (mCallbacks) {
            mCallbacks.broadcast(callback -> {
                Bundle extras = new Bundle();
                extras.putCharSequence(FLAGGED_PACKAGE_NAME_KEY, packageName);
                try {
                    callback.sendResult(extras);
                } catch (RemoteException e) {
                    Slog.e(TAG, "error detected: " + e.getLocalizedMessage(), e);
                }
            });
        }
    }
}
