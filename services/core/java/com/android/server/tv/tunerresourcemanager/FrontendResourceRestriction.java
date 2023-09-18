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

import java.util.List;

/**
 * A frontend resource restriction interface used by {@link TunerResourceManagerService}
 * to impose restrictions regarding whether a frontend resource request can be granted or
 * not based on the state of the current available frontend resources.
 *
 * @hide
 */
public interface FrontendResourceRestriction {

    /**
     * Check if there is an available frontend resource or not.
     *
     * @param request the frontend resource request.
     * @param frontendResources the current available frontend resources.
     *
     * @return whether there is an available frontend resource or not.
     */
    boolean hasAvailableFrontendResource(
            TunerFrontendRequest request, List<FrontendResource> frontendResources);

    /**
     * Get conflicting frontend resources that need to be reclaimed to grant the given
     * frontend resource request.
     *
     * @param request the frontend resource request.
     * @param frontendResources the current available frontend resources.
     *
     * @return a list of all conflicting frontend resources for the given frontend resource
     *         request, an empty list if there is no conflict.
     */
    List<FrontendResource> getConflictingFrontendResources(
            TunerFrontendRequest request, List<FrontendResource> frontendResources);
}
