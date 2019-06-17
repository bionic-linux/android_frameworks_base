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
package android.text.format;

/**
 * Logic to ease migration away from {@link Time} in Android internal code.
 *
 * @hide
 */
public class TimeMigrationUtils {

    private TimeMigrationUtils() {}

    /**
     * A replacement for various users of the {@link Time#format(String)} with the pattern
     * "%Y-%m-%d %H:%M:%S". Note, this method retains the unusual localization behavior originally
     * implemented by Time, which can lead to non-latin numbers being produced if the default
     * locale does not use latin numbers.
     */
    public static String formatMillisAsDateTime(long timeMillis) {
        // Delegate to TimeFormatter so that the unusual localization / threading behavior can be
        // reused.
        return new TimeFormatter().formatMillisAsDateTime(timeMillis);
    }
}
