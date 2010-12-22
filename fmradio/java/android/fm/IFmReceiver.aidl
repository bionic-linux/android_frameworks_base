/*
 * Copyright (C) ST-Ericsson SA 2010
 * Copyright (C) 2010 The Android Open Source Project
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
 *
 * Author: Markus Grape (markus.grape@stericsson.com) for ST-Ericsson
 */

package android.fm;

import android.fm.FmBand;
import android.fm.IOnStateChangedListener;
import android.fm.IOnStartedListener;
import android.fm.IOnErrorListener;
import android.fm.IOnScanListener;
import android.fm.IOnForcedPauseListener;
import android.fm.IOnForcedResetListener;
import android.fm.IOnRDSDataFoundListener;
import android.fm.IOnSignalStrengthListener;
import android.fm.IOnStereoListener;
import android.fm.IOnExtraCommandListener;
import android.fm.IOnAutomaticSwitchListener;

/**
 * {@hide}
 */
interface IFmReceiver
{
    void start(in FmBand band);
    void startAsync(in FmBand band);
    void reset();
    void pause();
    void resume();
    int getState();
    int getFrequency();
    void setFrequency(int frequency);
    void scanUp();
    void scanDown();
    void startFullScan();
    void stopScan();
    boolean isRDSDataSupported();
    boolean isTunedToValidChannel();
    boolean isTunerLooping();
    void setThreshold(int threshold);
    int getThreshold();
    int getSignalStrength();
    boolean isPlayingInStereo();
    void setForceMono(boolean forceMono);
    void setAutomaticAFSwitching(boolean automatic);
    void setAutomaticTASwitching(boolean automatic);
    boolean sendExtraCommand(String command, in String[] extras);
    void addOnStateChangedListener(in IOnStateChangedListener listener);
    void removeOnStateChangedListener(in IOnStateChangedListener listener);
    void addOnStartedListener(in IOnStartedListener listener);
    void removeOnStartedListener(in IOnStartedListener listener);
    void addOnErrorListener(in IOnErrorListener listener);
    void removeOnErrorListener(in IOnErrorListener listener);
    void addOnScanListener(in IOnScanListener listener);
    void removeOnScanListener(in IOnScanListener listener);
    void addOnForcedPauseListener(in IOnForcedPauseListener listener);
    void removeOnForcedPauseListener(in IOnForcedPauseListener listener);
    void addOnForcedResetListener(in IOnForcedResetListener listener);
    void removeOnForcedResetListener(in IOnForcedResetListener listener);
    void addOnRDSDataFoundListener(in IOnRDSDataFoundListener listener);
    void removeOnRDSDataFoundListener(in IOnRDSDataFoundListener listener);
    void addOnSignalStrengthChangedListener(in IOnSignalStrengthListener listener);
    void removeOnSignalStrengthChangedListener(in IOnSignalStrengthListener listener);
    void addOnPlayingInStereoListener(in IOnStereoListener listener);
    void removeOnPlayingInStereoListener(in IOnStereoListener listener);
    void addOnExtraCommandListener(in IOnExtraCommandListener listener);
    void removeOnExtraCommandListener(in IOnExtraCommandListener listener);
    void addOnAutomaticSwitchListener(in IOnAutomaticSwitchListener listener);
    void removeOnAutomaticSwitchListener(in IOnAutomaticSwitchListener listener);
}
