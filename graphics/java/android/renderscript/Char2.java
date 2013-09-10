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
 * Vector version of the basic Char type.
 * Provides two byte fields packed.
 */
public final class Char2 {
    public byte x;
    public byte y;

    public Char2() {
    }

    public Char2(byte i) {
        this.x = this.y = i;
    }

    public Char2(byte x, byte y) {
        this.x = x;
        this.y = y;
    }

    public Char2(Char2 source) {
        this.x = source.x;
        this.y = source.y;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Char2 a) {
        this.x += a.x;
        this.y += a.y;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 add(Char2 a, Char2 b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x + b.x);
        result.y = (byte)(a.y + b.y);

        return result;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Char value) {
        x += value.x;
        y += value.x;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 add(Char2 a, Char b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x + b.x);
        result.y = (byte)(a.y + b.x);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Char2 a) {
        this.x -= a.x;
        this.y -= a.y;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 sub(Char2 a, Char2 b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x - b.x);
        result.y = (byte)(a.y - b.y);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Char value) {
        x -= value.x;
        y -= value.x;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 sub(Char2 a, Char b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x - b.x);
        result.y = (byte)(a.y - b.x);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Char2 a) {
        this.x *= a.x;
        this.y *= a.y;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 mul(Char2 a, Char2 b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x * b.x);
        result.y = (byte)(a.y * b.y);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Char value) {
        x *= value.x;
        y *= value.x;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 mul(Char2 a, Char b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x * b.x);
        result.y = (byte)(a.y * b.x);

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Char2 a) {
        this.x /= a.x;
        this.y /= a.y;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 div(Char2 a, Char2 b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x / b.x);
        result.y = (byte)(a.y / b.y);

        return result;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Char value) {
        x /= value.x;
        y /= value.x;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Char2 div(Char2 a, Char b) {
        Char2 result = new Char2();
        result.x = (byte)(a.x / b.x);
        result.y = (byte)(a.y / b.x);

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public byte length() {
        return 2;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = (byte)(-x);
        this.y = (byte)(-y);
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Char2 a) {
        return new Int((x * a.x) + (y * a.y));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Char2 a, Char2 b) {
        return new Int((b.x * a.x) + (b.y * a.y));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Char2 a, Char factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
    }

    /**
     * set vector value by Char2
     *
     * @param a
     */
    public void set(Char2 a) {
        this.x = a.x;
        this.y = a.y;
    }

    /**
     * set the vector field value by Char
     *
     * @param a
     * @param b
     */
    public void setValues(Char a, Char b) {
        this.x = a.x;
        this.y = b.x;
    }

    /**
     * return the element sum of vector
     *
     * @return
     */
    public Int elementSum() {
        return new Int(x + y);
    }

    /**
     * get the vector field value by index
     *
     * @param i
     * @return
     */
    public Char get(int i) {
        switch (i) {
        case 0:
            return new Char(x);
        case 1:
            return new Char(y);
        default:
            throw new IndexOutOfBoundsException("Index: i");
        }
    }

    /**
     * set the vector field value by index
     *
     * @param i
     * @param value
     */
    public void set(int i, Char value) {
        switch (i) {
        case 0:
            x = value.x;
            return;
        case 1:
            y = value.x;
            return;
        default:
            throw new IndexOutOfBoundsException("Index: i");
        }
    }

    /**
     * add the vector field value by index
     *
     * @param i
     * @param value
     */
    public void addAt(int i, Char value) {
        switch (i) {
        case 0:
            x += value.x;
            return;
        case 1:
            y += value.x;
            return;
        default:
            throw new IndexOutOfBoundsException("Index: i");
        }
    }

    /**
     * copy the vector to Char array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Char[] data, int offset) {
        data[offset] = new Char(x);
        data[offset + 1] = new Char(y);
    }
}
