/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.nfc;

import android.nfc.IWlcEventListener;

/**
 * {@hide}
 */
interface IWlcNfcAdapter {
/**
 * Helper to check if this device supports NFC based wireless charging Poller(WLCP) feature
 * Uses Package Manager to retrieve the information on the device i.e.
 *  hasSystemFeature(PackageManager.FEATURE_NFC_BASED_WLCP)
 */
public  static boolean isWlcSupported()

/**
 * Sets state of NFC based WLCP(Wireless Charging Poller) to enable/disable.
 * <p>This API is for the Settings application.
 *
 * <p>If this returns true, then either WLC is already enabled ,
 * or enabled successfully with current invocation.
 * If this returns false, then there is some problem
 * that prevents an attempt to turn on WLC feature.
 * listener needs to be registered during enabling WLC to receive the wlc events.
 *
 * @return True if successful.
 * @throws UnsupportedOperationException if FEATURE_NFC_BASED_WLCP is
 *  unavailable.
 */
@SystemApi
@RequiresPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
public boolean setWlcState(boolean enable, WlcEventListener listener)

/**
 * Checks if WLCP feature is enabled.
 *
 * @return True if WLCP is enabled, false otherwise
 * @throws UnsupportedOperationException if FEATURE_NFC_BASED_WLCP is
 *  unavailable.
 */
 public boolean isWlcEnabled()
}

