/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * Describes specific properties of a requested network for use in a {@link NetworkRequest}.
 *
 * Applications cannot instantiate this class by themselves, but can obtain instances of
 * subclasses of this class via other APIs.
 */
public abstract class NetworkSpecifier {
    /** @hide */
    public NetworkSpecifier() {}

    /**
     * Returns true if a request with this {@link NetworkSpecifier} is satisfied by a network
     * with the given NetworkSpecifier.
     *
     * @hide
     */
    public abstract boolean satisfiedBy(NetworkSpecifier other);

    /**
     * Optional method which can be overriden by concrete implementations of NetworkSpecifier to
     * check a self-reported UID. A concrete implementation may contain a UID which would be self-
     * reported by the caller (since NetworkSpecifier implementations should be non-mutable). This
     * function is called by ConnectivityService and is passed the actual UID of the caller -
     * allowing the verification of the self-reported UID. In cases of mismatch the implementation
     * should throw a SecurityException.
     *
     * @param requestorUid The UID of the requestor as obtained from its binder.
     *
     * @hide
     */
    public void assertValidFromUid(int requestorUid) {
        // empty
    }

    /**
     * Optional method which can be overriden by concrete implementations of NetworkSpecifier to
     * indicate to the connectivity service that the information contained in the object is
     * sensitive and should not be shared with apps.
     * <p>
     * This method is relevant to NetworkSpecifier objects used by agents - those are shared with
     * apps by default. Some agents may store sensitive matching information in the specifier,
     * e.g. a Wi-Fi SSID (which should not be shared since it may leak location). Other agents
     * use the Network Specifier to share public information with apps - those should not be
     * stripped out.
     * <p>
     * The default is to share the information.
     *
     * @return true to indicate information is sensitive and should be stripped out, false to
     * indicate that the information is not sensitive and should not be stripped out.
     *
     * @hide
     */
    public boolean isSensitive() {
        // TODO (b/122160111): convert default to true.
        return false;
    }
}
