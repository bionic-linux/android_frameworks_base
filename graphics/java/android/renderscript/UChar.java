/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.renderscript;

/**
 * Basic UChar type.
 */
public final class UChar {

    private byte x;

    public UChar() {
    }

    public UChar(char value) {
        setValue(fixValue(value));
    }

    private char fixValue(char value) {
        int oraValue = (int) value;
        if (oraValue > 255) {
            oraValue = oraValue % 256;
        }
        return (char) oraValue;
    }

    private void setValue(char value) {
        value = fixValue(value);
        x = (byte) (value & 0xff);
    }

    public char getValue() {
        return (char) (x & 0x00FF);
    }

    public UChar(UChar data) {
        x = data.x;
    }

    /**
     * UChar add
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar add(UChar a, UChar b) {
        char cValue = (char) (a.getValue() + b.getValue());
        UChar res = new UChar(cValue);

        return res;
    }

    /**
     * UChar add
     *
     * @param value
     */
    public void add(UChar value) {
        char cValue = (char) (value.getValue() + getValue());
        setValue(cValue);
    }

    /**
     * UChar subtraction
     *
     * @param value
     */
    public void sub(UChar value) {
        char cValue = (char) (getValue() - value.getValue());
        setValue(cValue);
    }

    /**
     * UChar subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar sub(UChar a, UChar b) {
        char cValue = (char) (a.getValue() - b.getValue());
        UChar res = new UChar(cValue);

        return res;
    }

    /**
     * UChar multiplication
     *
     * @param value
     */
    public void mul(UChar value) {
        char cValue = (char) (getValue() * value.getValue());
        setValue(cValue);
    }

    /**
     * UChar multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar mul(UChar a, UChar b) {
        char cValue = (char) (a.getValue() * b.getValue());
        UChar res = new UChar(cValue);

        return res;
    }

    /**
     * UChar division
     *
     * @param value
     */
    public void div(UChar value) {
        char cValue = (char) (getValue() / value.getValue());
        setValue(cValue);
    }

    /**
     * UChar division
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar div(UChar a, UChar b) {
        char cValue = (char) (a.getValue() / b.getValue());
        UChar res = new UChar(cValue);

        return res;
    }

    /**
     * set UChar value by UChar
     *
     * @param a
     */
    public void set(UChar a) {
        this.x = a.x;
    }

}
