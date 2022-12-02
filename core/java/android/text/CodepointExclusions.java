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
import java.util.Set;


/**
 * Codepoints that this device would like applications to avoid rendering.
 */
public class CodepointExclusions {

    /* Cannot construct */
    private CodepointExclusions() { }

    /**
     * Codepoints that this device would like to avoid showing.
     *
     * Codepoint sequences in this list MAY be filtered by platform text display. However, this
     * requirement is optional.
     *
     * Apps that display glyphs via custom fonts or other means MAY filter against this list.
     *
     * Apps that do not draw their own glyphs use system fonts SHOULD ignore this list.
     *
     * Codepoint sequence MUST match the returned sequences fully to be filtered.
     *
     * @return list of strings representing codepoints to exclude on this platform
     */
    @NonNull
    public static Set<int[]> getExcludedCodepoints() {
        return Collections.emptySet();
    }
}
