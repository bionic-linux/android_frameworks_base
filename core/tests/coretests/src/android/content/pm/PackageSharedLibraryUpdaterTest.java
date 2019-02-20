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

import java.util.function.Supplier;

/**
 * Helper for classes that test {@link PackageSharedLibraryUpdater}.
 */
abstract class PackageSharedLibraryUpdaterTest {

    /**
     * Check backwards compatibility with a specific flag.
     */
    static void checkBackwardsCompatibility(PackageBuilder before, PackageBuilder after,
            Supplier<PackageSharedLibraryUpdater> updaterSupplier,
            @PackageParser.ParseFlags int flags) {
        PackageParser.Package pkg = before.build();
        updaterSupplier.get().updatePackage(pkg, flags);
        after.check(pkg);
    }

    /**
     * Check backwards compatibility with common flags. In general, apps should be treated the same
     * no matter where they are loaded.
     */
    static void checkBackwardsCompatibility(PackageBuilder before, PackageBuilder after,
            Supplier<PackageSharedLibraryUpdater> updaterSupplier) {
        checkBackwardsCompatibility(before, after, updaterSupplier, 0);
        checkBackwardsCompatibility(before, after, updaterSupplier,
                PackageParser.PARSE_IS_SYSTEM_DIR);
    }
}
