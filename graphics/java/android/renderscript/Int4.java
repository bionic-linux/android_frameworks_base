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
 * Provides four int fields packed.
 */
public class Int4 {
    public int x;
    public int y;
    public int z;
    public int w;

    public Int4() {
    }

    public Int4(int i) {
        this.x = this.y = this.z = this.w = i;
    }

    public Int4(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Int4(Int4 source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        this.w = source.w;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Int4 a) {
        this.x += a.x;
        this.y += a.y;
        this.z += a.z;
        this.w += a.w;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 add(Int4 a, Int4 b) {
        Int4 result = new Int4();
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        result.z = a.z + b.z;
        result.w = a.w + b.w;

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
        z += value.x;
        w += value.x;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 add(Int4 a, Int b) {
        Int4 result = new Int4();
        result.x = a.x + b.x;
        result.y = a.y + b.x;
        result.z = a.z + b.x;
        result.w = a.w + b.x;

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Int4 a) {
        this.x -= a.x;
        this.y -= a.y;
        this.z -= a.z;
        this.w -= a.w;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 sub(Int4 a, Int4 b) {
        Int4 result = new Int4();
        result.x = a.x - b.x;
        result.y = a.y - b.y;
        result.z = a.z - b.z;
        result.w = a.w - b.w;

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
        z -= value.x;
        w -= value.x;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 sub(Int4 a, Int b) {
        Int4 result = new Int4();
        result.x = a.x - b.x;
        result.y = a.y - b.x;
        result.z = a.z - b.x;
        result.w = a.w - b.x;

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Int4 a) {
        this.x *= a.x;
        this.y *= a.y;
        this.z *= a.z;
        this.w *= a.w;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 mul(Int4 a, Int4 b) {
        Int4 result = new Int4();
        result.x = a.x * b.x;
        result.y = a.y * b.y;
        result.z = a.z * b.z;
        result.w = a.w * b.w;

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
        z *= value.x;
        w *= value.x;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 mul(Int4 a, Int b) {
        Int4 result = new Int4();
        result.x = a.x * b.x;
        result.y = a.y * b.x;
        result.z = a.z * b.x;
        result.w = a.w * b.x;

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Int4 a) {
        this.x /= a.x;
        this.y /= a.y;
        this.z /= a.z;
        this.w /= a.w;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 div(Int4 a, Int4 b) {
        Int4 result = new Int4();
        result.x = a.x / b.x;
        result.y = a.y / b.y;
        result.z = a.z / b.z;
        result.w = a.w / b.w;

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
        z /= value.x;
        w /= value.x;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 div(Int4 a, Int b) {
        Int4 result = new Int4();
        result.x = a.x / b.x;
        result.y = a.y / b.x;
        result.z = a.z / b.x;
        result.w = a.w / b.x;

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param a
     */
    public void mod(Int4 a) {
        this.x %= a.x;
        this.y %= a.y;
        this.z %= a.z;
        this.w %= a.w;
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 mod(Int4 a, Int4 b) {
        Int4 result = new Int4();
        result.x = a.x % b.x;
        result.y = a.y % b.y;
        result.z = a.z % b.z;
        result.w = a.w % b.w;

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
        z %= value.x;
        w %= value.x;
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Int4 mod(Int4 a, Int b) {
        Int4 result = new Int4();
        result.x = a.x % b.x;
        result.y = a.y % b.x;
        result.z = a.z % b.x;
        result.w = a.w % b.x;

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public int length() {
        return 4;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = -x;
        this.y = -y;
        this.z = -z;
        this.w = -w;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Int4 a) {
        return new Int((x * a.x) + (y * a.y) + (z * a.z) + (w * a.w));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Int4 a, Int4 b) {
        return new Int((b.x * a.x) + (b.y * a.y) + (b.z * a.z) + (b.w * a.w));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Int4 a, Int factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
        w += a.w * factor.x;
    }

    /**
     * set vector value by Int4
     *
     * @param a
     */
    public void set(Int4 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.w = a.w;
    }

    /**
     * set the vector field value by Int
     *
     * @param a
     * @param b
     * @param c
     * @param d
     */
    public void setValues(Int a, Int b, Int c, Int d) {
        this.x = a.x;
        this.y = b.x;
        this.z = c.x;
        this.w = d.x;
    }

    /**
     * return the element sum of vector
     *
     * @return
     */
    public Int elementSum() {
        return new Int(x + y + z + w);
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
        case 2:
            return new Int(z);
        case 3:
            return new Int(w);
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
        case 2:
            z = value.x;
            return;
        case 3:
            w = value.x;
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
        case 2:
            z += value.x;
            return;
        case 3:
            w += value.x;
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
        data[offset + 2] = new Int(z);
        data[offset + 3] = new Int(w);
    }
}
