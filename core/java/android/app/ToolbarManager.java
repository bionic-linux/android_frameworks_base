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

package android.app;

import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.view.SurfaceControl;

import com.android.internal.infra.AndroidFuture;

/** @hide */
public class ToolbarManager {
    private final Context mContext;
    private final IToolbarManager mService;

    @SdkConstant(SdkConstant.SdkConstantType.INTENT_CATEGORY
    )
    public static final String CATEGORY_TOOLBAR_SLICE = "android.intent.category.TOOLBAR_SLICE";


    public ToolbarManager(Context context, IToolbarManager service) {
        mContext = context;
        mService = service;
    }

    public AndroidFuture<SurfaceControl> getToolbarSlice(Intent intent, int w, int h) {
        AndroidFuture<SurfaceControl> future = new AndroidFuture<>();
        try {
            mService.getToolbarSlice(intent, w, h, future);
        } catch (RemoteException ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    public interface Callback<T> {
        void onSuccess(@Nullable T o);

        void onFailure(Throwable throwable);
    }
}
