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
 * Vector version of the basic long type.
 * Provides three long fields packed.
 */
public class Long3 {
    public long x;
    public long y;
    public long z;

    public Long3() {
    }

    public Long3(long i) {
        this.x = this.y = this.z = i;
    }

    public Long3(long x, long y, long z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Long3(Long3 source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Long3 a) {
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
    public static Long3 add(Long3 a, Long3 b) {
        Long3 result = new Long3();
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
    public void add(Long value) {
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
    public static Long3 add(Long3 a, Long b) {
        Long3 result = new Long3();
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
    public void sub(Long3 a) {
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
    public static Long3 sub(Long3 a, Long3 b) {
        Long3 result = new Long3();
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
    public void sub(Long value) {
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
    public static Long3 sub(Long3 a, Long b) {
        Long3 result = new Long3();
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
    public void mul(Long3 a) {
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
    public static Long3 mul(Long3 a, Long3 b) {
        Long3 result = new Long3();
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
    public void mul(Long value) {
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
    public static Long3 mul(Long3 a, Long b) {
        Long3 result = new Long3();
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
    public void div(Long3 a) {
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
    public static Long3 div(Long3 a, Long3 b) {
        Long3 result = new Long3();
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
    public void div(Long value) {
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
    public static Long3 div(Long3 a, Long b) {
        Long3 result = new Long3();
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
    public void mod(Long3 a) {
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
    public static Long3 mod(Long3 a, Long3 b) {
        Long3 result = new Long3();
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
    public void mod(Long value) {
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
    public static Long3 mod(Long3 a, Long b) {
        Long3 result = new Long3();
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
    public long length() {
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
    public Long dotProduct(Long3 a) {
        return new Long((x * a.x) + (y * a.y) + (z * a.z));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Long dotProduct(Long3 a, Long3 b) {
        return new Long((b.x * a.x) + (b.y * a.y) + (b.z * a.z));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Long3 a, Long factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
    }

    /**
     * set vector value by Long3
     *
     * @param a
     */
    public void set(Long3 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
    }

    /**
     * set the vector field value by Long
     *
     * @param a
     * @param b
     * @param c
     */
    public void setValues(Long a, Long b, Long c) {
        this.x = a.x;
        this.y = b.x;
        this.z = c.x;
    }

    /**
     * return the element sum of vector
     *
     * @return
     */
    public Long elementSum() {
        return new Long(x + y + z);
    }

    /**
     * get the vector field value by index
     *
     * @param i
     * @return
     */
    public Long get(int i) {
        switch (i) {
        case 0:
            return new Long(x);
        case 1:
            return new Long(y);
        case 2:
            return new Long(z);
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
    public void set(int i, Long value) {
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
    public void addAt(int i, Long value) {
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
     * copy the vector to Long array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Long[] data, int offset) {
        data[offset] = new Long(x);
        data[offset + 1] = new Long(y);
        data[offset + 2] = new Long(z);
    }
}
