/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.content.pm;

import static android.content.pm.PackageBuilder.builder;
import static android.content.pm.SharedLibraryNames.ANDROID_HIDL_BASE;
import static android.content.pm.SharedLibraryNames.ANDROID_HIDL_MANAGER;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for {@link AndroidHidlUpdater}
 */
@SmallTest
@RunWith(JUnit4.class)
public class AndroidHidlUpdaterTest extends PackageSharedLibraryUpdaterTest {
    @Test
    public void in_usesLibraries() {
        PackageBuilder before = builder()
                .requiredLibraries(ANDROID_HIDL_BASE, ANDROID_HIDL_MANAGER);

        // Dependency is removed, it is not available.
        PackageBuilder after = builder();

        // No change is required because the package explicitly requests the HIDL libraries
        // and is targeted at the current version so does not need backwards compatibility.
        checkBackwardsCompatibility(before, after);
    }

    @Test
    public void in_usesOptionalLibraries() {
        PackageBuilder before = builder()
                .optionalLibraries(ANDROID_HIDL_BASE, ANDROID_HIDL_MANAGER);

        // Dependency is removed, it is not available.
        PackageBuilder after = builder();

        // No change is required because the package explicitly requests the HIDL libraries
        // and is targeted at the current version so does not need backwards compatibility.
        checkBackwardsCompatibility(before, after);
    }

    private void checkBackwardsCompatibility(PackageBuilder before, PackageBuilder after) {
        checkBackwardsCompatibility(before, after, AndroidHidlUpdater::new);
    }
}
