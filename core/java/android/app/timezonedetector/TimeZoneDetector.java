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

package android.app.timezonedetector;

import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.content.Context;

/**
 * The interface through which system components can send signals to the TimeZoneDetectorService.
 *
 * @hide
 */
@SystemService(Context.TIME_ZONE_DETECTOR_SERVICE)
public interface TimeZoneDetector {

    /**
     * A shared utility method to create a {@link ManualTimeZoneSuggestion}.
     *
     * @hide
     */
    static ManualTimeZoneSuggestion createManualTimeZoneSuggestion(String tzId, String debugInfo) {
        ManualTimeZoneSuggestion suggestion = new ManualTimeZoneSuggestion(tzId);
        suggestion.addDebugInfo(debugInfo);
        return suggestion;
    }

    /**
     * Returns the complete, current time zone detection configuration.
     */
    @RequiresPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
    @NonNull TimeZoneDetectorConfiguration getConfiguration();

    /**
     * Updates the time zone detection configuration. If configuration is not complete then only the
     * specified properties will be updated.
     */
    @RequiresPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
    void updateConfiguration(@NonNull TimeZoneDetectorConfiguration configuration);

    /**
     * Registers a listener that will be informed when the configuration changes. The complete
     * configuration is passed to the listener.
     */
    @RequiresPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
    void addConfigurationListener(@NonNull ITimeZoneDetectorConfigurationListener listener);

    /**
     * Suggests the current time zone, determined using telephony signals, to the detector. The
     * detector may ignore the signal based on system settings, whether better information is
     * available, and so on.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.SUGGEST_TELEPHONY_TIME_AND_ZONE)
    void suggestTelephonyTimeZone(@NonNull TelephonyTimeZoneSuggestion timeZoneSuggestion);

    /**
     * Suggests the current time zone, determined for the user's manually information, to the
     * detector. The detector may ignore the signal based on system settings.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.SUGGEST_MANUAL_TIME_AND_ZONE)
    void suggestManualTimeZone(@NonNull ManualTimeZoneSuggestion timeZoneSuggestion);
}
