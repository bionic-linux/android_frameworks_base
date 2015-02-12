/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.security;

import java.io.IOException;

/**
 * Signals that an I/O operation could not be performed because it employed insecure network
 * traffic (e.g., cleartext HTTP).
 *
 * @hide
 */
public class InsecureNetworkTrafficException extends IOException {
    /**
     * Constructs a new {@code InsecureNetworkTrafficException} with without a detail message or a
     * cause.
     */
    public InsecureNetworkTrafficException() {
    }

    /**
     * Constructs a new {@code InsecureNetworkTrafficException} with the provided detail message.
     */
    public InsecureNetworkTrafficException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code InsecureNetworkTrafficException} with the provided detail message and
     * cause.
     */
    public InsecureNetworkTrafficException(String message, Throwable cause) {
        super(message, cause);
    }
}
