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
 * Vector version of the basic short type.
 * Provides two short fields packed.
 */
public class Short2 {
    public short x;
    public short y;

    public Short2() {
    }

    public Short2(short i) {
        this.x = this.y = i;
    }

    public Short2(short x, short y) {
        this.x = x;
        this.y = y;
    }

    public Short2(Short2 source) {
        this.x = source.x;
        this.y = source.y;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Short2 a) {
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
    public static Short2 add(Short2 a, Short2 b) {
        Short2 result = new Short2();
        result.x = (short)(a.x + b.x);
        result.y = (short)(a.y + b.y);

        return result;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Short value) {
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
    public static Short2 add(Short2 a, Short b) {
        Short2 result = new Short2();
        result.x = (short)(a.x + b.x);
        result.y = (short)(a.y + b.x);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Short2 a) {
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
    public static Short2 sub(Short2 a, Short2 b) {
        Short2 result = new Short2();
        result.x = (short)(a.x - b.x);
        result.y = (short)(a.y - b.y);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Short value) {
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
    public static Short2 sub(Short2 a, Short b) {
        Short2 result = new Short2();
        result.x = (short)(a.x - b.x);
        result.y = (short)(a.y - b.x);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Short2 a) {
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
    public static Short2 mul(Short2 a, Short2 b) {
        Short2 result = new Short2();
        result.x = (short)(a.x * b.x);
        result.y = (short)(a.y * b.y);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Short value) {
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
    public static Short2 mul(Short2 a, Short b) {
        Short2 result = new Short2();
        result.x = (short)(a.x * b.x);
        result.y = (short)(a.y * b.x);

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Short2 a) {
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
    public static Short2 div(Short2 a, Short2 b) {
        Short2 result = new Short2();
        result.x = (short)(a.x / b.x);
        result.y = (short)(a.y / b.y);

        return result;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Short value) {
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
    public static Short2 div(Short2 a, Short b) {
        Short2 result = new Short2();
        result.x = (short)(a.x / b.x);
        result.y = (short)(a.y / b.x);

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param a
     */
    public void mod(Short2 a) {
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
    public static Short2 mod(Short2 a, Short2 b) {
        Short2 result = new Short2();
        result.x = (short)(a.x % b.x);
        result.y = (short)(a.y % b.y);

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param value
     */
    public void mod(Short value) {
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
    public static Short2 mod(Short2 a, Short b) {
        Short2 result = new Short2();
        result.x = (short)(a.x % b.x);
        result.y = (short)(a.y % b.x);

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public short length() {
        return 2;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = (short)(-x);
        this.y = (short)(-y);
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Short2 a) {
        return new Int((x * a.x) + (y * a.y));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Short2 a, Short2 b) {
        return new Int((b.x * a.x) + (b.y * a.y));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Short2 a, Short factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
    }

    /**
     * set vector value by Short2
     *
     * @param a
     */
    public void set(Short2 a) {
        this.x = a.x;
        this.y = a.y;
    }

    /**
     * set the vector field value by Short
     *
     * @param a
     * @param b
     */
    public void setValues(Short a, Short b) {
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
    public Short get(int i) {
        switch (i) {
        case 0:
            return new Short(x);
        case 1:
            return new Short(y);
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
    public void set(int i, Short value) {
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
    public void addAt(int i, Short value) {
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
     * copy the vector to Short array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Short[] data, int offset) {
        data[offset] = new Short(x);
        data[offset + 1] = new Short(y);
    }
}
