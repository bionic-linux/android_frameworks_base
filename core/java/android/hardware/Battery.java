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

package android.hardware;

/**
 * The Battery class is a representation of a single battery on a device.
 */
public abstract class Battery {

    /**
     * @hide
     */
    public Battery() {

    }

    /**
     * Get the scope of the battery.
     * @return the scope of the battery.
     */
    public abstract String getScope();

    /**
     * Get the type of the battery.
     * @return the type of the battery.
     */
    public abstract String getType();

    /**
     * Get the capacity of the battery.
     * @return the capacity of the battery.
     */
    public abstract int getCapacity();

    /**
     * Get the status of the battery.
     * @return the status of the battery.
     */
    public abstract String getStatus();
}
