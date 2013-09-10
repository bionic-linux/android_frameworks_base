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
 * Provides three short fields packed.
 */
public class Short3 {
    public short x;
    public short y;
    public short z;

    public Short3() {
    }

    public Short3(short i) {
        this.x = this.y = this.z = i;
    }

    public Short3(short x, short y, short z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Short3(Short3 source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Short3 a) {
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
    public static Short3 add(Short3 a, Short3 b) {
        Short3 result = new Short3();
        result.x = (short)(a.x + b.x);
        result.y = (short)(a.y + b.y);
        result.z = (short)(a.z + b.z);

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
        z += value.x;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Short3 add(Short3 a, Short b) {
        Short3 result = new Short3();
        result.x = (short)(a.x + b.x);
        result.y = (short)(a.y + b.x);
        result.z = (short)(a.z + b.x);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Short3 a) {
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
    public static Short3 sub(Short3 a, Short3 b) {
        Short3 result = new Short3();
        result.x = (short)(a.x - b.x);
        result.y = (short)(a.y - b.y);
        result.z = (short)(a.z - b.z);

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
        z -= value.x;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Short3 sub(Short3 a, Short b) {
        Short3 result = new Short3();
        result.x = (short)(a.x - b.x);
        result.y = (short)(a.y - b.x);
        result.z = (short)(a.z - b.x);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Short3 a) {
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
    public static Short3 mul(Short3 a, Short3 b) {
        Short3 result = new Short3();
        result.x = (short)(a.x * b.x);
        result.y = (short)(a.y * b.y);
        result.z = (short)(a.z * b.z);

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
        z *= value.x;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Short3 mul(Short3 a, Short b) {
        Short3 result = new Short3();
        result.x = (short)(a.x * b.x);
        result.y = (short)(a.y * b.x);
        result.z = (short)(a.z * b.x);

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Short3 a) {
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
    public static Short3 div(Short3 a, Short3 b) {
        Short3 result = new Short3();
        result.x = (short)(a.x / b.x);
        result.y = (short)(a.y / b.y);
        result.z = (short)(a.z / b.z);

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
        z /= value.x;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Short3 div(Short3 a, Short b) {
        Short3 result = new Short3();
        result.x = (short)(a.x / b.x);
        result.y = (short)(a.y / b.x);
        result.z = (short)(a.z / b.x);

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param a
     */
    public void mod(Short3 a) {
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
    public static Short3 mod(Short3 a, Short3 b) {
        Short3 result = new Short3();
        result.x = (short)(a.x % b.x);
        result.y = (short)(a.y % b.y);
        result.z = (short)(a.z % b.z);

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
        z %= value.x;
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Short3 mod(Short3 a, Short b) {
        Short3 result = new Short3();
        result.x = (short)(a.x % b.x);
        result.y = (short)(a.y % b.x);
        result.z = (short)(a.z % b.x);

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public short length() {
        return 3;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = (short)(-x);
        this.y = (short)(-y);
        this.z = (short)(-z);
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Short3 a) {
        return new Int((x * a.x) + (y * a.y) + (z * a.z));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Short3 a, Short3 b) {
        return new Int((b.x * a.x) + (b.y * a.y) + (b.z * a.z));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Short3 a, Short factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
    }

    /**
     * set vector value by Short3
     *
     * @param a
     */
    public void set(Short3 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
    }

    /**
     * set the vector field value by Short
     *
     * @param a
     * @param b
     * @param c
     */
    public void setValues(Short a, Short b, Short c) {
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
    public Short get(int i) {
        switch (i) {
        case 0:
            return new Short(x);
        case 1:
            return new Short(y);
        case 2:
            return new Short(z);
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
    public void addAt(int i, Short value) {
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
     * copy the vector to Short array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Short[] data, int offset) {
        data[offset] = new Short(x);
        data[offset + 1] = new Short(y);
        data[offset + 2] = new Short(z);
    }
}
