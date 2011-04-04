/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.test.AndroidTestCase;
import android.util.Log;

public class SelectionTest extends AndroidTestCase {
    final String angryFaceEmoji = "\uDBB8\uDF20";

    SpannableStringBuilder fourEmojis;

    @Override
    protected void setUp() throws Exception {
        fourEmojis = new SpannableStringBuilder();
        fourEmojis.append(angryFaceEmoji);
        fourEmojis.append(angryFaceEmoji);
        fourEmojis.append(angryFaceEmoji);
        fourEmojis.append(angryFaceEmoji);
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testSelectCenterSurrogatePair() throws Exception {
        // It's expected that the selection is changed to include the full
        // surrogate pairs
        int expectedStart = 2;
        int expectedEnd = 6;

        int start = 3; // Center of the second surrogate pair
        int end = 5; // Center of the third surrogate pair
        Selection.setSelection(fourEmojis, start, end);

        assertSame("Left edge of selection should be moved to the left of the surrogate pair",
                expectedStart, Selection.getSelectionStart(fourEmojis));
        assertSame("Right edge of selection should be moved to the right of the surrogate pair",
                expectedEnd, Selection.getSelectionEnd(fourEmojis));
        testSelection(fourEmojis, angryFaceEmoji + angryFaceEmoji);
    }

    public void testSelectSurrogatePair() throws Exception {
        int start = 2; // Left of the second surrogate pair
        int end = 6; // right of the third surrogate pair

        Selection.setSelection(fourEmojis, start, end);

        assertSame("Left edge of selection should not change", start,
                Selection.getSelectionStart(fourEmojis));
        assertSame("Right edge of selection should not change", end,
                Selection.getSelectionEnd(fourEmojis));
        testSelection(fourEmojis, angryFaceEmoji + angryFaceEmoji);
    }

    public void testSelectAllSurrogatePair() throws Exception {
        int start = 0;
        int end = fourEmojis.length();

        Selection.setSelection(fourEmojis, start, end);

        assertSame("Left edge of selection should not change", start,
                Selection.getSelectionStart(fourEmojis));
        assertSame("Right edge of selection should not change", end,
                Selection.getSelectionEnd(fourEmojis));
        testSelection(fourEmojis, fourEmojis);
    }

    public void testSelectAllInner() throws Exception {
        Selection.setSelection(fourEmojis, 1, fourEmojis.length() - 1);
        testSelection(fourEmojis, fourEmojis);

    }

    public void testCursorOffset() throws Exception {
        int pos = 3;
        int expected = pos - 1; // Left of surrogate pair
        Selection.setSelection(fourEmojis, pos);
        assertSame("Cursor position should have zero length",
                Selection.getSelectionStart(fourEmojis), Selection.getSelectionEnd(fourEmojis));
        assertSame("Cursor position not be in the middle of surrogate pair",
                Selection.getSelectionStart(fourEmojis), expected);
        testSelection(fourEmojis, "");
    }

    private void testSelection(Spannable text, CharSequence expected) throws Exception {
        String selectedText = text.subSequence(Selection.getSelectionStart(text),
                Selection.getSelectionEnd(text)).toString();
        assertEquals("selected text not same as expected", selectedText, expected.toString());
    }
}
