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
 * Emoji that this device renders non-standard.
 *
 * Third party apps MAY use this list to defer to platform rendering for these non-standard emoji.
 *
 * This is intended to be used only by applications that do custom emoji rendering using tools like
 * {@link android.text.style.ReplacementSpan}.
 *
 * An example of how this should be used:
 *  1. Match emoji for third party custom rendering
 *  2. For each match, check against NonStandardEmoji before adding ReplacementSpan
 *  3. If in NonStandardEmojiSet, do not add a replacement span (defer to platform render).
 *      Otherwise, do custom rendering like normal (custom render).
 */
public class NonStandardEmoji {

    /* Cannot construct */
    private NonStandardEmoji() { }

    /**
     * Emoji that this device renders non-standard.
     *
     * Apps SHOULD attempt to avoid overwriting this non-standard rendering with custom emoji
     * rendering that displays the standard emoji.
     *
     * Apps that display glyphs via custom fonts or other means MAY filter against this list. On
     * match, the application SHOULD use platform text rendering for the matched emoji to display
     * the non-standard rendering.
     *
     * Returned codepoint sequences MUST be a complete emoji codepoint sequence as defined by
     * unicode.
     *
     * Codepoint sequence MUST match the returned sequences fully to match. For example,
     * matching the first half of a ZWJ sequence should not be considered a match.
     *
     * @return set of codepoint sequences representing codepoints that render in a non-standard way
     * on this platform that applications should defer to.
     */
    @NonNull
    public static Set<int[]> getEmojiSet() {
        return Collections.emptySet();
    }
}
