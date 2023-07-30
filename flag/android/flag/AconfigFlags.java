/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.flag;

import android.annotation.NonNull;
import android.annotation.SystemApi;

/**
 * Class to abstract how aconfig generated code gets its flag values.
 *
 * Aconfig generates java libraries, which are linked in to other java modules, including
 * framework-minus-apex.jar. This class sits below those generated classes in the dependency
 * graph, and allows DeviceConfig to plug in an implementation without there being a build-time
 * dependeny on DeviceConfig.
 *
 * This class mirrors the DeviceConfig API.
 *
 * @hide
 */
@SystemApi
public abstract class AconfigFlags {
    private static AconfigFlagProvider sProvider;

    private AconfigFlags() {
    }

    /**
     * @hide
     */
    public static void setProvider(AconfigFlagProvider p) {
        sProvider = p;
    }

    /**
     * Get a boolean flag.
     *
     * @param ns The flag namespace. Usually provided by the .aconfig file.
     * @param name The flag name. Usually provided by the .aconfig file.
     * @param def The default value of the flag. Usually provided by the release configuration
     *          at build time via aconfig.
     */
    public static boolean getBoolean(@NonNull String ns, @NonNull String name, boolean def) {
        if (sProvider == null) {
            throw new RuntimeException("Flags accessed before DeviceConfig initialization");
        }
        return sProvider.getBoolean(ns, name, def);
    }
}

