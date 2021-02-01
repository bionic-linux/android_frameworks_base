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
 * limitations under the License.
 */

package com.android.server.compat;

import android.app.compat.PackageOverride;

import com.android.internal.compat.CompatibilityOverrideConfig;

import java.util.HashMap;
import java.util.Map;

class CompatibilityChangeConfigBuilder {
    private Map<Long, PackageOverride> mOverrides;

    private CompatibilityChangeConfigBuilder() {
        mOverrides = new HashMap<>();
    }

    static CompatibilityChangeConfigBuilder create() {
        return new CompatibilityChangeConfigBuilder();
    }

    CompatibilityChangeConfigBuilder enable(Long id) {
        mOverrides.put(id, new PackageOverride.Builder().addForAllVersions(true).build());
        return this;
    }

    CompatibilityChangeConfigBuilder disable(Long id) {
        mOverrides.put(id, new PackageOverride.Builder().addForAllVersions(false).build());
        return this;
    }

    CompatibilityOverrideConfig build() {
        return new CompatibilityOverrideConfig(mOverrides);
    }
}
