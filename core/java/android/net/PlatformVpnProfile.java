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

package android.net;

import static com.android.internal.net.VpnProfile.TYPE_IKEV2_IPSEC_PSK;
import static com.android.internal.net.VpnProfile.TYPE_IKEV2_IPSEC_RSA;
import static com.android.internal.net.VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS;

import android.annotation.NonNull;

import com.android.internal.net.VpnProfile;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * PlatformVpnProfile represents a configuration for a platform-based VPN implementation.
 *
 * @see Ikev2VpnProfile
 */
public abstract class PlatformVpnProfile {
    /** @hide */
    PlatformVpnProfile() {}

    /** @hide */
    @NonNull
    public abstract VpnProfile toVpnProfile() throws IOException, GeneralSecurityException;

    /** @hide */
    @NonNull
    public static PlatformVpnProfile fromVpnProfile(@NonNull VpnProfile profile)
            throws IOException, GeneralSecurityException {
        switch (profile.type) {
            case TYPE_IKEV2_IPSEC_USER_PASS: // fallthrough
            case TYPE_IKEV2_IPSEC_PSK: // fallthrough
            case TYPE_IKEV2_IPSEC_RSA:
                return Ikev2VpnProfile.fromVpnProfile(profile);
            default:
                throw new IllegalArgumentException("Unknown VPN Profile type");
        }
    }
}
