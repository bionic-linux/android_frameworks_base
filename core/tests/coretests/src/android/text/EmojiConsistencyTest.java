/*
 * Copyright 2018 The Android Open Source Project
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

import static junit.framework.Assert.assertEquals;

import android.util.Range;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiConsistencyTest {

    @Test
    public void exclusionList_isEmpty() {
        assertEquals(EmojiConsistency.getEmojiConsistencySet(), Collections.emptySet());
    }

    @Test
    public void matcherMatches() {
        EmojiConsistency.Matcher subject = EmojiConsistency.matcher(
                Set.of(new int[] { 1 }, new int[] { 2 })
        );

        String str = new String(new int[] { 0, 1, 0 }, 0, 3);

        Range<Integer> match = subject.nextEmojiRangeConsistencyMatch(str, 0);

        assertEquals(Range.create(1, 2), match);

        match = subject.nextEmojiRangeConsistencyMatch(str, 2);

        assertEquals(Range.create(-1, -1), match);
    }

    @Test
    public void matcherMatchesLongest() {
        EmojiConsistency.Matcher subject = EmojiConsistency.matcher(
                Set.of(new int[] { 1, 2, 3 }, new int[] { 1, 2 })
        );

        String str = new String(new int[] { 0, 1, 2, 3 }, 0, 3);
        Range<Integer> match = subject.nextEmojiRangeConsistencyMatch(str, 0);
        assertEquals(Range.create(1, 4), match);

        str = new String(new int[] { 0, 1, 2, 4 }, 0, 3);
        match = subject.nextEmojiRangeConsistencyMatch(str, 0);
        assertEquals(Range.create(1, 3), match);
    }

    @Test
    public void matcherMatchesRepeat() {
        EmojiConsistency.Matcher subject = EmojiConsistency.matcher(
                Set.of(new int[] { 1 })
        );

        String str = new String(new int[] { 0, 1, 1, 2, 1, 1 }, 0, 3);
        Range<Integer> match = subject.nextEmojiRangeConsistencyMatch(str, 0); // match 1;
        assertEquals(Range.create(1, 2), match);
        match = subject.nextEmojiRangeConsistencyMatch(str, 2); // match 1, 2
        assertEquals(Range.create(2, 3), match);
        match = subject.nextEmojiRangeConsistencyMatch(str, 4); // match 1
        assertEquals(Range.create(4, 5), match);
        match = subject.nextEmojiRangeConsistencyMatch(str, 5); // match 1
        assertEquals(Range.create(5, 6), match);
    }
}
