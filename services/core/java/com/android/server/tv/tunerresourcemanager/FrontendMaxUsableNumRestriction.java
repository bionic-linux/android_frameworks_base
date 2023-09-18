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
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A frontend resource restriction object to restrict {@link TunerResourceManagerService}
 * to the frontend resource usage that satisfies a maximum usable number limit of frontend
 * resources for each frontend type. The maximum usable number can be configured by
 * {@link setMaxNumberOfFrontends} and {@link getMaxNumberOfFrontends}.
 *
 * @hide
 */
class FrontendMaxUsableNumRestriction implements FrontendResourceRestriction {

    public static final int MAX_NUM_OF_FRONTENDS_NOT_SET = -1;

    // SparseIntArray of the max usable number for each frontend resource type
    private SparseIntArray mFrontendMaxUsableNums = new SparseIntArray();
    // Backups for the max usable number for each frontend resource type
    private SparseIntArray mFrontendMaxUsableNumsBackup = new SparseIntArray();

    @Override
    public boolean hasAvailableFrontendResource(
            TunerFrontendRequest request, List<FrontendResource> frontendResources) {
        return getMaxNumberOfFrontends(request.frontendType) != 0;
    }

    @Override
    public List<FrontendResource> getConflictingFrontendResources(
            TunerFrontendRequest request, List<FrontendResource> frontendResources) {
        if (!isFrontendMaxNumUseReached(request.frontendType, frontendResources)) {
            return Collections.emptyList();
        }
        List<FrontendResource> conflictingFrontendResources = new ArrayList<>();
        for (FrontendResource fr : frontendResources) {
            if (fr.getType() == request.frontendType
                    &&
                    fr.isInPrimaryUse()) {
                conflictingFrontendResources.add(fr);
            }
        }
        return conflictingFrontendResources;
    }

    /**
     * Set the maximum usable frontends number of a given frontend type.
     *
     * @param frontendType the frontendType which the maximum usable number will be set for.
     * @param maxUsableNum the new maximum usable number.
     */
    public void setMaxNumberOfFrontends(int frontendType, int maxUsableNum) {
        mFrontendMaxUsableNums.put(frontendType, maxUsableNum);
    }

    /**
     * Get the maximum usable frontends number of a given frontend type.
     *
     * @param frontendType the frontendType which the maximum usable number will be queried for.
     *
     * @return the maximum usable number of the queried frontend type,
     *         {@link MAX_NUM_OF_FRONTENDS_NOT_SET} if the maximum usable number has not been set.
     */
    public int getMaxNumberOfFrontends(int frontendType) {
        return mFrontendMaxUsableNums.get(frontendType, MAX_NUM_OF_FRONTENDS_NOT_SET);
    }

    public int getStoredMaxNumberOfFrontends(int frontendType) {
        return mFrontendMaxUsableNumsBackup.get(frontendType, MAX_NUM_OF_FRONTENDS_NOT_SET);
    }

    public void storeMaxNumberOfFrontends() {
        replaceFeCounts(mFrontendMaxUsableNums, mFrontendMaxUsableNumsBackup);
    }

    public void clearMaxNumberOfFrontends() {
        replaceFeCounts(null, mFrontendMaxUsableNums);
    }

    public void restoreMaxNumberOfFrontends() {
        replaceFeCounts(mFrontendMaxUsableNumsBackup, mFrontendMaxUsableNums);
    }

    private void replaceFeCounts(SparseIntArray srcCounts, SparseIntArray dstCounts) {
        if (dstCounts != null) {
            dstCounts.clear();
            if (srcCounts != null) {
                for (int i = 0; i < srcCounts.size(); i++) {
                    dstCounts.put(srcCounts.keyAt(i), srcCounts.valueAt(i));
                }
            }
        }
    }

    private boolean isFrontendMaxNumUseReached(
            int frontendType, List<FrontendResource> frontendResources) {
        int maxUsableNum = mFrontendMaxUsableNums.get(frontendType, MAX_NUM_OF_FRONTENDS_NOT_SET);
        if (maxUsableNum == MAX_NUM_OF_FRONTENDS_NOT_SET) {
            return false;
        }
        int usedNum = (int) frontendResources.stream()
                .filter(fr -> fr.isInPrimaryUse() && fr.getType() == frontendType).count();
        return usedNum >= maxUsableNum;
    }
}
