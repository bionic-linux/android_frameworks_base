/*
 * Copyright 2023 The Android Open Source Project
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
package com.android.server.tv.tunerresourcemanager;

import android.media.tv.tunerresourcemanager.TunerFrontendRequest;
import android.util.Slog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A frontend resource restriction object to restrict {@link TunerResourceManagerService}
 * to the frontend resource usage that only one frontend resource can be primary used at
 * the same time among the frontend resources under the same exclusive group.
 *
 * @hide
 */
class FrontendExclusiveGroupRestriction implements FrontendResourceRestriction {
    private static final String TAG = "FrontendExclusiveGroupRestriction";

    @Override
    public boolean hasAvailableFrontendResource(
            TunerFrontendRequest request, List<FrontendResource> frontendResources) {
        for (FrontendResource fr : frontendResources) {
            if (fr.getType() == request.frontendType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<FrontendResource> getConflictingFrontendResources(
            TunerFrontendRequest request, List<FrontendResource> frontendResources) {
        List<FrontendResource> conflictingFrontendResources = new ArrayList<>();
        for (FrontendResource fr : frontendResources) {
            if (fr.getType() == request.frontendType) {
                if (!fr.isInUse()) {
                    return Collections.emptyList();
                } else {
                    if (fr.isInPrimaryUse()) {
                        if (!conflictingFrontendResources.contains(fr)) {
                            conflictingFrontendResources.add(fr);
                        }
                    } else {
                        FrontendResource primaryUseFrontendResource =
                                getPrimaryUseFrontendResource(
                                        fr.getExclusiveGroupId(), frontendResources);
                        if (primaryUseFrontendResource != null) {
                            if (!conflictingFrontendResources.contains(
                                    primaryUseFrontendResource)) {
                                conflictingFrontendResources.add(primaryUseFrontendResource);
                            }
                        } else {
                            Slog.e(TAG, "Can't find in-primary-use frontend resource"
                                    + ", frontendType: " + request.frontendType
                                    + ", frontendResources: " + frontendResources);
                        }
                    }
                }
            }
        }
        return conflictingFrontendResources;
    }

    private FrontendResource getPrimaryUseFrontendResource(
            int exclusiveGroupId, List<FrontendResource> frontendResources) {
        for (FrontendResource fr : frontendResources) {
            if (fr.isInPrimaryUse()
                    &&
                    fr.getExclusiveGroupId() == exclusiveGroupId) {
                return fr;
            }
        }
        return null;
    }
}
