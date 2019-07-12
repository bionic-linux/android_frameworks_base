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
 * limitations under the License
 */

package android.service.toolbar;

import android.annotation.MainThread;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowlessViewRoot;

import com.android.internal.infra.AndroidFuture;

/**
 * A service that provides slices to a toolbar.
 * TODO: Make this a public/system api, so that any apps can provide slices.
 *
 * @hide
 */
public abstract class ToolbarService extends Service {
    private static final String TAG = "ToolbarService";

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private IBinder mServiceImpl = new IToolbarService.Stub() {
        @Override
        public void getToolbarSlice(Intent intent, int w, int h, AndroidFuture future) {
            mHandler.post(() -> onGetToolbarSlice(intent, w, h, new Callback<View>() {
                @Override
                public void onSuccess(View slice) {
                    mHandler.post(() -> {
                        SurfaceControl surfaceControl = addSliceToSurfaceControl(slice, w, h);
                        future.complete(surfaceControl);
                    });
                }

                @Override
                public void onFailure(Throwable error) {
                    mHandler.post(() -> future.completeExceptionally(error));
                }
            }));
        }

        private SurfaceControl addSliceToSurfaceControl(View slice, int w, int h) {
            SurfaceControl surfaceControl =
                    new SurfaceControl.Builder()
                            .setName("toolbar-slice")
                            .build();
            WindowlessViewRoot windowlessViewRoot = new WindowlessViewRoot(
                    ToolbarService.this,
                    ToolbarService.this.getDisplay(),
                    surfaceControl);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    w,
                    h,
                    WindowManager.LayoutParams.TYPE_APPLICATION,
                    0,
                    PixelFormat.TRANSLUCENT);
            windowlessViewRoot.addView(slice, layoutParams);
            new SurfaceControl.Transaction().setVisibility(surfaceControl, true).apply();
            return surfaceControl;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceImpl;
    }

    @MainThread
    public abstract void onGetToolbarSlice(Intent intent, int w, int h, Callback<View> callback);

    public interface Callback<T> {
        void onSuccess(T result);

        void onFailure(Throwable error);
    }

}
