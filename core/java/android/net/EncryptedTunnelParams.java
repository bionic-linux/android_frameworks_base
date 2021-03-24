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
package android.net;

import android.net.ipsec.ike.IkeTunnelParams;

/**
 * This interface represents configurations for a secure encrypted tunnel with a remote endpoint.
 *
 * <p>An instance of {@link EncryptedTunnelParams} will contain all connection, authentication and
 * authorization parameters required to establish an encrypted tunnel. APIs that depends on
 * encrypted tunnels may require callers to provide such instances.
 *
 * <p>The specialized tunnel configuration will need to implements this interface. See, for example,
 * the {@link IkeTunnelParams} in android.net.ipsec.ike.
 */
public interface EncryptedTunnelParams {}
