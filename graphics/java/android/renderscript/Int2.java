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
 * Vector version of the basic int type.
 * Provides two int fields packed.
 */
public class Int2 {
    public int x;
    public int y;

    public Int2() {
    }

    public Int2(int i) {
        this.x = this.y = i;
    }

    public Int2(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Int2(Int2 source) {
        this.x = source.x;
        this.y = source.y;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Int2 a) {
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
    public static Int2 add(Int2 a, Int2 b) {
        Int2 result = new Int2();
        result.x = a.x + b.x;
        result.y = a.y + b.y;

        return result;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Int value) {
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
    public static Int2 add(Int2 a, Int b) {
        Int2 result = new Int2();
        result.x = a.x + b.x;
        result.y = a.y + b.x;

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Int2 a) {
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
    public static Int2 sub(Int2 a, Int2 b) {
        Int2 result = new Int2();
        result.x = a.x - b.x;
        result.y = a.y - b.y;

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Int value) {
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
    public static Int2 sub(Int2 a, Int b) {
        Int2 result = new Int2();
        result.x = a.x - b.x;
        result.y = a.y - b.x;

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Int2 a) {
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
    public static Int2 mul(Int2 a, Int2 b) {
        Int2 result = new Int2();
        result.x = a.x * b.x;
        result.y = a.y * b.y;

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Int value) {
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
    public static Int2 mul(Int2 a, Int b) {
        Int2 result = new Int2();
        result.x = a.x * b.x;
        result.y = a.y * b.x;

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Int2 a) {
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
    public static Int2 div(Int2 a, Int2 b) {
        Int2 result = new Int2();
        result.x = a.x / b.x;
        result.y = a.y / b.y;

        return result;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Int value) {
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
    public static Int2 div(Int2 a, Int b) {
        Int2 result = new Int2();
        result.x = a.x / b.x;
        result.y = a.y / b.x;

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param a
     */
    public void mod(Int2 a) {
        this.x %= a.x;
        this.y %= a.y;
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Int2 mod(Int2 a, Int2 b) {
        Int2 result = new Int2();
        result.x = a.x % b.x;
        result.y = a.y % b.y;

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param value
     */
    public void mod(Int value) {
        x %= value.x;
        y %= value.x;
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Int2 mod(Int2 a, Int b) {
        Int2 result = new Int2();
        result.x = a.x % b.x;
        result.y = a.y % b.x;

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public int length() {
        return 2;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = -x;
        this.y = -y;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Int2 a) {
        return new Int((x * a.x) + (y * a.y));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Int2 a, Int2 b) {
        return new Int((b.x * a.x) + (b.y * a.y));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Int2 a, Int factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
    }

    /**
     * set vector value by Int2
     *
     * @param a
     */
    public void set(Int2 a) {
        this.x = a.x;
        this.y = a.y;
    }

    /**
     * set the vector field value by Int
     *
     * @param a
     * @param b
     */
    public void setValues(Int a, Int b) {
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
    public Int get(int i) {
        switch (i) {
        case 0:
            return new Int(x);
        case 1:
            return new Int(y);
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
    public void set(int i, Int value) {
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
    public void addAt(int i, Int value) {
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
     * copy the vector to Int array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Int[] data, int offset) {
        data[offset] = new Int(x);
        data[offset + 1] = new Int(y);
    }
}
