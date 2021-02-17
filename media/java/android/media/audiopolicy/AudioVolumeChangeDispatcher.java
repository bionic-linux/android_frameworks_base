/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.media.audiopolicy;

import android.annotation.NonNull;
import android.content.Context;
import android.media.AudioManager.VolumeGroupCallback;
import android.media.IAudioService;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @hide
 * Internal dispatcher class for volume and gain change handling.
 */
public class AudioVolumeChangeDispatcher  {
    private static final String TAG = "AudioVolumeChangeDispatcher";

    private Executor mExecutor;
    private VolumeGroupCallback mAudioVolumeCallback;

    private final IAudioVolumeChangeDispatcher mAudioVolumeChangeDispatcher =
            new IAudioVolumeChangeDispatcher.Stub() {
        @Override
        public void onAudioVolumeGroupChanged(int group, int flags) {
            Log.v(TAG, "onAudioVolumeGroupChanged group=" + group);

            Executor executor;
            VolumeGroupCallback callback;

            synchronized (AudioVolumeChangeDispatcher.this) {
                executor = mExecutor;
                callback = mAudioVolumeCallback;
            }
            if ((executor == null) || (callback == null)) {
                return;
            }
            executor.execute(() -> callback.onAudioVolumeGroupChanged(group, flags));
        }

        @Override
        public void onAudioDevicePortGainsChanged(
                int reasons, List<AudioDevicePortGain> audioDevicePortGains) {
            Log.d(TAG, "onAudioDevicePortGainsChanged reasons=" + reasons
                    + ", gains=" + audioDevicePortGains.toString());
            Executor executor;
            VolumeGroupCallback callback;

            synchronized (AudioVolumeChangeDispatcher.this) {
                executor = mExecutor;
                callback = mAudioVolumeCallback;
            }
            if ((executor == null) || (callback == null)) {
                return;
            }
            executor.execute(() -> callback.onAudioDevicePortGainsChanged(
                    reasons, audioDevicePortGains));
        }
    };

    /**
     * @hide
    */
    public synchronized void registerAudioVolumeCallback(@NonNull Executor executor,
            @NonNull VolumeGroupCallback callback) {
        if (mAudioVolumeCallback != null) {
            throw new IllegalStateException(
                "registerAudioVolumeCallback called with already registered callabck");
        }
        final IAudioService service = getService();
        try {
            service.registerAudioVolumeCallback(mAudioVolumeChangeDispatcher);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        mExecutor = executor;
        mAudioVolumeCallback = callback;
        Log.d(TAG, "registerAudioVolumeCallback exit");
    }

    /**
     * @hide
     * Unregister an audio gain change listener.
     * @param callback the {@link VolumeGroupCallback} to unregister
     */
     public synchronized void unregisterAudioVolumeCallback(@NonNull VolumeGroupCallback callback) {
         if (mAudioVolumeCallback != null) {
             final IAudioService service = getService();
             try {
                 service.unregisterAudioVolumeCallback(mAudioVolumeChangeDispatcher);
             } catch (RemoteException e) {
                 throw e.rethrowFromSystemServer();
             }
         }
         mExecutor = null;
         mAudioVolumeCallback = null;
     }

     private static IAudioService sService;

     private static IAudioService getService()
     {
         if (sService != null) {
             return sService;
         }
         IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
         sService = IAudioService.Stub.asInterface(b);
         return sService;
     }
}
