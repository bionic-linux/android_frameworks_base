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

/**
 * This interface represents configurations for a secure encrypted tunnel with a remote endpoint.
 *
 * <p>The specialized tunnel configuration will need to implement this interface. It will need to be
 * an immutable class that contain all connection, authentication and authorization parameters
 * required to establish an encrypted tunnel. APIs that depends on encrypted tunnels (e.g. {@link
 * android.net.vcn.VcnGatewayConnectionConfig}) may require callers to provide a {@link
 * EncryptedTunnelParams} instance.
 */
// TODO: b/180664474 Add "@see android.net.ipsec.ike.IkeTunnelParams" when IkeTunnelParams is
// exposed
public interface EncryptedTunnelParams {}
