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

package android.net.util;

import static com.android.internal.util.BitUtils.packBits;

import android.net.NetworkCapabilities;
import android.net.shared.NetworkCapabilitiesPartial;

/**
 * Collection of utility methods to create partial views of framework network classes.
 *
 * <p>This is useful to let the framework share some internal data structures with the NetworkStack,
 * without exposing fields of the original structures as system or public API.
 * @hide
 */
public final class PartialViewUtil {

    /**
     * Create a {@link NetworkCapabilitiesPartial} from {@link NetworkCapabilities}.
     */
    public static NetworkCapabilitiesPartial toPartial(NetworkCapabilities nc) {
        return new NetworkCapabilitiesPartial.Builder()
                .setCapabilities(packBits(nc.getCapabilities()))
                .setUnwantedCapabilities(packBits(nc.getUnwantedCapabilities()))
                .setTransportTypes(packBits(nc.getTransportTypes()))
                .setLinkUpBandwidthKbps(nc.getLinkUpstreamBandwidthKbps())
                .setLinkDownBandwidthKbps(nc.getLinkDownstreamBandwidthKbps())
                .setSsid(nc.getSSID())
                .build();
    }

    private PartialViewUtil() {}
}
