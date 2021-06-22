/* Copyright (C) 2021 The Android Open Source Project
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

package android.media.audiopolicy;

import android.media.audiopolicy.AudioDevicePortGain;

/**
 * AIDL for the AudioService to signal audio volume groups and gains changes
 *
 * {@hide}
 */
oneway interface IAudioVolumeChangeDispatcher {

    /**
     * Called when a volume group has been changed
     * @param group id of the volume group that has changed.
     * @param flags one or more flags to describe the volume change.
     */
    void onAudioVolumeGroupChanged(int group, int flags);

    /**
     * Called when one or more gain on the associated Device Port has been changed and specifies
     * why it has been changed.
     * @param reasons that lead to the device port configuration changes.
     * @param audioDevicePortConfigs list of device port configuration changed.
     */
    void onAudioDevicePortGainsChanged(int reasons, in List<AudioDevicePortGain> audioDevicePortGains);
}
