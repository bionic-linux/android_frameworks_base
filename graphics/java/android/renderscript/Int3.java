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
 * Provides three int fields packed.
 */
public class Int3 {
    public int x;
    public int y;
    public int z;

    public Int3() {
    }

    public Int3(int i) {
        this.x = this.y = this.z = i;
    }

    public Int3(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Int3(Int3 source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Int3 a) {
        this.x += a.x;
        this.y += a.y;
        this.z += a.z;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 add(Int3 a, Int3 b) {
        Int3 result = new Int3();
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        result.z = a.z + b.z;

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
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 add(Int3 a, Int b) {
        Int3 result = new Int3();
        result.x = a.x + b.x;
        result.y = a.y + b.x;
        result.z = a.z + b.x;

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Int3 a) {
        this.x -= a.x;
        this.y -= a.y;
        this.z -= a.z;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 sub(Int3 a, Int3 b) {
        Int3 result = new Int3();
        result.x = a.x - b.x;
        result.y = a.y - b.y;
        result.z = a.z - b.z;

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
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 sub(Int3 a, Int b) {
        Int3 result = new Int3();
        result.x = a.x - b.x;
        result.y = a.y - b.x;
        result.z = a.z - b.x;

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Int3 a) {
        this.x *= a.x;
        this.y *= a.y;
        this.z *= a.z;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 mul(Int3 a, Int3 b) {
        Int3 result = new Int3();
        result.x = a.x * b.x;
        result.y = a.y * b.y;
        result.z = a.z * b.z;

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
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 mul(Int3 a, Int b) {
        Int3 result = new Int3();
        result.x = a.x * b.x;
        result.y = a.y * b.x;
        result.z = a.z * b.x;

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Int3 a) {
        this.x /= a.x;
        this.y /= a.y;
        this.z /= a.z;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 div(Int3 a, Int3 b) {
        Int3 result = new Int3();
        result.x = a.x / b.x;
        result.y = a.y / b.y;
        result.z = a.z / b.z;

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
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 div(Int3 a, Int b) {
        Int3 result = new Int3();
        result.x = a.x / b.x;
        result.y = a.y / b.x;
        result.z = a.z / b.x;

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param a
     */
    public void mod(Int3 a) {
        this.x %= a.x;
        this.y %= a.y;
        this.z %= a.z;
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 mod(Int3 a, Int3 b) {
        Int3 result = new Int3();
        result.x = a.x % b.x;
        result.y = a.y % b.y;
        result.z = a.z % b.z;

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
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Int3 mod(Int3 a, Int b) {
        Int3 result = new Int3();
        result.x = a.x % b.x;
        result.y = a.y % b.x;
        result.z = a.z % b.x;

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public int length() {
        return 3;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = -x;
        this.y = -y;
        this.z = -z;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Int3 a) {
        return new Int((x * a.x) + (y * a.y) + (z * a.z));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Int3 a, Int3 b) {
        return new Int((b.x * a.x) + (b.y * a.y) + (b.z * a.z));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Int3 a, Int factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
    }

    /**
     * set vector value by Int3
     *
     * @param a
     */
    public void set(Int3 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
    }

    /**
     * set the vector field value by Int
     *
     * @param a
     * @param b
     * @param c
     */
    public void setValues(Int a, Int b, Int c) {
        this.x = a.x;
        this.y = b.x;
        this.z = c.x;
    }

    /**
     * return the element sum of vector
     *
     * @return
     */
    public Int elementSum() {
        return new Int(x + y + z);
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
    }
}
