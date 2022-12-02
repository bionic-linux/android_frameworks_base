/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.text;

import android.annotation.NonNull;

import java.util.Collections;
import java.util.List;


/**
 * A list of codepoints that an OEM would like applications to avoid rendering.
 */
public class CodepointExclusions {

    /* Cannot construct */
    private CodepointExclusions() { }

    /**
     * Codepoints that this device would like to avoid showing.
     *
     * Apps that display codepoints via custom fonts or glyphs may filter against this list to
     * honor the OEM glyph removal.
     *
     * Apps that use system fonts should ignore this list.
     *
     * Codepoint sequence should match the returned string exactly to be excluded.
     *
     * This list does not impact platform text rendering, and is only to be used by non-platform
     * glyph rendering code. OEMs that add codepoints to this list should also remove the codepoints
     * from platform text renedring.
     *
     * @return list of strings representing codepoints to exclude on this platform
     */
    @NonNull
    public static List<String> getExcludedCodpoints() {
        return Collections.emptyList();
    }
}
