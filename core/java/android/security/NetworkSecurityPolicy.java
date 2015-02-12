/**
 * Copyright (c) 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security;

/**
 * Network security policy.
 *
 * @hide
 */
public class NetworkSecurityPolicy {

  private static final NetworkSecurityPolicy INSTANCE = new NetworkSecurityPolicy();

  /**
   * {@code true} if insecure network traffic is permitted for this application, {@code false}
   * otherwise.
   */
  private boolean mInsecureTrafficPermitted = true;

  private NetworkSecurityPolicy() {}

  /**
   * Gets the current policy.
   */
  public static NetworkSecurityPolicy get() {
    return INSTANCE;
  }

  /**
   * Checks whether insecure network traffic (e.g., cleartext HTTP) is permitted for this
   * application.
   *
   * <p> When insecure network traffic is not permitted, the platform and third-party network
   * stacks (e.g,. HTTP stacks) are encouraged to block attempts from this application to use
   * insecure network traffic, for example, by throwing a
   * {@link java.lang.SecurityException SecurityException}.
   *
   * @return {@code true} if insecure network traffic is permitted, {@code false} otherwise.
   */
  public boolean isInsecureTrafficPermitted() {
    synchronized (this) {
      return mInsecureTrafficPermitted;
    }
  }

  /**
   * Sets whether insecure network traffic (e.g., cleartext HTTP) is permitted for this
   * application.
   *
   * <p>This method is method is meant to be used by the platform early on in the application's
   * initialization to set the policy.
   *
   * @hide
   */
  public void setInsecureTrafficPermitted(boolean permitted) {
    synchronized (this) {
      mInsecureTrafficPermitted = permitted;
    }
  }
}
