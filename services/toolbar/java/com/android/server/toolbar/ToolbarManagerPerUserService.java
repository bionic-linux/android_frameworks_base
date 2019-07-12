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

package com.android.server.toolbar;

import android.app.ToolbarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.toolbar.IToolbarService;
import android.view.SurfaceControl;

import com.android.internal.infra.AndroidFuture;
import com.android.internal.infra.ServiceConnector;
import com.android.internal.util.Preconditions;
import com.android.server.infra.AbstractPerUserSystemService;

final class ToolbarManagerPerUserService extends
        AbstractPerUserSystemService<ToolbarManagerPerUserService, ToolbarManagerService> {
    private final PackageManager mPackageManager;

    ToolbarManagerPerUserService(ToolbarManagerService master, Object lock, int userId) {
        super(master, lock, userId);
        mPackageManager = master.getContext().getPackageManager();
    }

    void getToolbarSlice(Intent intent, int w, int h, AndroidFuture<SurfaceControl> future) {
        Preconditions.checkNotNull(intent);
        if (!intent.hasCategory(ToolbarManager.CATEGORY_TOOLBAR_SLICE)) {
            throw new IllegalArgumentException(
                    "Incoming intent must have " + ToolbarManager.CATEGORY_TOOLBAR_SLICE);
        }

        ComponentName serviceComponent = resolveToolbarService(intent);
        if (serviceComponent == null) {
            future.complete(null);
            return;
        }
        intent.setComponent(serviceComponent);
        ServiceConnector<IToolbarService> serviceConnector = createServiceConnector(intent);
        serviceConnector.post(service -> service.getToolbarSlice(intent, w, h, future));
    }

    private ServiceConnector<IToolbarService> createServiceConnector(Intent intent) {
        return new ServiceConnector.Impl<>(
                getContext(),
                intent,
                /* bindingFlags= */ 0,
                mUserId,
                IToolbarService.Stub::asInterface);
    }

    private ComponentName resolveToolbarService(Intent intent) {
        ResolveInfo resolveInfo = mPackageManager.resolveServiceAsUser(intent, 0, getUserId());
        if (resolveInfo == null) {
            return null;
        }
        return resolveInfo.serviceInfo.getComponentName();
    }
}
