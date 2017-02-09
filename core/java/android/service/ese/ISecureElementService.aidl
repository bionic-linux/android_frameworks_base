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

package android.service.ese;

/**
 * Interface for communication with the embedded secure element.
 *
 * @hide
 */
interface ISecureElementService {
    /**
     * Returns the number of available Gatekeeper slots. The slots are assigned integer IDs
     * incrementally from 0.
     *
     * @return The number of Gatekeeper slots.
     */
    int gatekeeperGetNumSlots();

    /**
     * Writes a key-value pair into the chosen slot. The slot is updated atomically to avoid being
     * left in an inconsistent state.
     * @param slotId The ID of the slot to write into.
     * @param key The key to write into the slot.
     * @param value The value to write into the slot.
     */
    void gatekeeperWrite(int slotId, in byte[] key, in byte[] value);

    /**
     * Returns the value of in a slot provided the slot has been written to and the provided key
     * matches the key in the slot.
     * @param slotId The ID of the slot to read from.
     * @param key The key stored in the slot.
     * @return The value from the slot or {@code null} if the request was invalid.
     */
    byte[] gatekeeperRead(int slotId, in byte[] key);

    /**
     * Resets a slot by clearning the contents. The slot must be written to before it can be read.
     * The slots is updated atomically to avoid being left in an inconsistent state.
     * @param slotId The ID of the slot to erase.
     */
    void gatekeeperErase(int slotId);

    /**
     * Resets all of the slots. Equivalent to calling {@link #erase(int)} on each slot in turn. Each
     * slot is individually updated atomically but not the operation as a whole.
     */
    void gatekeeperEraseAll();
}
