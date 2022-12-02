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

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.Range;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.Set;


/**
 * The set of emoji that should be drawn by the system with the default font for device consistency.
 *
 * This is intended to be used only by applications that do custom emoji rendering using tools like
 * {@link android.text.style.ReplacementSpan} or custom emoji fonts.
 *
 * An example of how this should be used:
 *
 * <p>
 *     <ol>
 *         <li>
 *             Match emoji for third party custom rendering
 *         </li>
 *         <li>
 *             For each match, check against NonStandardEmoji before displaying custom glyph
 *         </li>
 *         <li>
 *             If in NonStandardEmojiSet, do not display custom glyph (render with
 *             {@link android.graphics.Typeface.DEFAULT} instead)
 *         </li>
 *         <li>
 *             Otherwise, do custom rendering like normal
 *         </li>
 *     </ol>
 * </p>
 */
public final class EmojiConsistency {
    /* Cannot construct */
    private EmojiConsistency() { }

    @NonNull
    public static final Range<Integer> NO_MATCH = Range.create(-1, -1);

    /**
     * Match ranges containing emoji that should be drawn by the system with the default font for
     * device consistency.
     *
     * Finds the next subsequence that contains an emoji that is in the consistency set.
     *
     * This will always match the longest match in
     * {@link EmojiConsistency#getEmojiConsistencySet()}.
     */
    public interface Matcher {

        /**
         * Find the first emoji in the consistency set after start.
         *
         * Searching will start and continue until the end of the string.
         *
         * On match, returns an [inclusive, exclusive) range containing the matched subsequence that
         * is in the consistency set.
         *
         * On no-match, {@link EmojiConsistency#NO_MATCH} or Range(-1, -1) is returned.
         *
         * @param source CharSequence to search
         * @param start index to start at, is typically 0 or the end of the last match
         * @return [inclusive, exclusive) match or (-1, -1) on no match.
         */
        @NonNull
        Range<Integer> nextEmojiRangeConsistencyMatch(
                @NonNull CharSequence source,
                @IntRange(from = 0) int start
        );
    }

    /**
     * The set of emoji that should be drawn by the system with the default font for device
     * consistency.
     *
     * Apps SHOULD attempt to avoid overwriting system emoji rendering with custom emoji glyphs for
     * these codepoint sequences.
     *
     * Apps that display custom emoji glyphs via matching code MAY filter against this list. On
     * match, the application SHOULD use platform text rendering for the matched emoji to display
     * the non-standard rendering.
     *
     * To search a CodepointSequence for matches aainst this list see
     * {@link emojiConsistencyMatcher}.
     *
     * @return set of codepoint sequences representing codepoints that should be rendered by the
     * system using the default font.
     */
    @NonNull
    public static Set<int[]> getEmojiConsistencySet() {
        return Collections.emptySet();
    }

    /**
     * Matcher to search a CharSequence for emoji that are in {@link getEmojiConsistencySet}.
     *
     * @return new matcher for searching for emoji in the consistency set.
     */
    @NonNull
    public static Matcher emojiConsistencyMatcher() {
        return new ConsistencyMatcher(getEmojiConsistencySet());
    }

    /**
     * Used for testing trie implementation.
     *
     * @hide
     */
    @VisibleForTesting
    @NonNull
    public static Matcher matcher(Set<int[]> emoji) {
        return new ConsistencyMatcher(emoji);
    }

    private static class ConsistencyMatcher implements Matcher {

        private Node mRoot;

        ConsistencyMatcher(Set<int[]> toMatch) {
            mRoot = new Node(toMatch.size());
            for (int[] charSequence : toMatch) {
                insert(charSequence);
            }
        }

        private void insert(int[] charSequence) {
            Node cur = mRoot;
            for (int i = 0; i < charSequence.length; i++) {
                cur = cur.getChild(charSequence[i]);
            }
            cur.setMatch();
        }

        @Override
        public Range<Integer> nextEmojiRangeConsistencyMatch(CharSequence source, int start) {
            int offset = start;
            Node cur = mRoot;
            int matchStart = offset;
            int lastMatch = -1;
            while (offset < source.length()) {
                int codepoint = Character.codePointAt(source, offset);
                offset += Character.charCount(codepoint);
                Node walk = cur.walk(codepoint);
                if (walk == null) {
                    if (lastMatch == -1) {
                        // this is juts a normal reset
                        matchStart = offset;
                        walk = mRoot;
                    } else {
                        // we've found the longest match, return it
                        return Range.create(matchStart, lastMatch);
                    }
                } else if (walk.isMatch()) {
                    // keep walking, next char could also be a match
                    lastMatch = offset;
                }
                cur = walk;
            }
            if (lastMatch != -1) {
                return Range.create(matchStart, lastMatch);
            }
            return NO_MATCH;
        }

        private static class Node {
            final SparseArray<Node> mChildren;
            boolean mIsMatch;

            Node(int size) {
                mChildren = new SparseArray<>(size);
                mIsMatch = false;
            }

            Node(boolean match) {
                mChildren = new SparseArray<>(1);
                mIsMatch = match;
            }

            @NonNull
            public Node getChild(int key) {
                Node result = mChildren.get(key);
                if (result == null) {
                    result = new Node(false);
                    mChildren.put(key, result);
                }
                return result;
            }

            public void setMatch() {
                mIsMatch = true;
            }

            public boolean isMatch() {
                return mIsMatch;
            }

            @Nullable
            public Node walk(int codepoint) {
                return mChildren.get(codepoint);
            }
        }

    }
}
