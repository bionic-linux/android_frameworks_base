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
 * Provides two long fields packed.
 */
public class Long2 {
    public long x;
    public long y;

    public Long2() {
    }

    public Long2(long i) {
        this.x = this.y = i;
    }

    public Long2(long x, long y) {
        this.x = x;
        this.y = y;
    }

    public Long2(Long2 source) {
        this.x = source.x;
        this.y = source.y;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Long2 a) {
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
    public static Long2 add(Long2 a, Long2 b) {
        Long2 result = new Long2();
        result.x = a.x + b.x;
        result.y = a.y + b.y;

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
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Long2 add(Long2 a, Long b) {
        Long2 result = new Long2();
        result.x = a.x + b.x;
        result.y = a.y + b.x;

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Long2 a) {
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
    public static Long2 sub(Long2 a, Long2 b) {
        Long2 result = new Long2();
        result.x = a.x - b.x;
        result.y = a.y - b.y;

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
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Long2 sub(Long2 a, Long b) {
        Long2 result = new Long2();
        result.x = a.x - b.x;
        result.y = a.y - b.x;

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Long2 a) {
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
    public static Long2 mul(Long2 a, Long2 b) {
        Long2 result = new Long2();
        result.x = a.x * b.x;
        result.y = a.y * b.y;

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
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Long2 mul(Long2 a, Long b) {
        Long2 result = new Long2();
        result.x = a.x * b.x;
        result.y = a.y * b.x;

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Long2 a) {
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
    public static Long2 div(Long2 a, Long2 b) {
        Long2 result = new Long2();
        result.x = a.x / b.x;
        result.y = a.y / b.y;

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
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Long2 div(Long2 a, Long b) {
        Long2 result = new Long2();
        result.x = a.x / b.x;
        result.y = a.y / b.x;

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param a
     */
    public void mod(Long2 a) {
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
    public static Long2 mod(Long2 a, Long2 b) {
        Long2 result = new Long2();
        result.x = a.x % b.x;
        result.y = a.y % b.y;

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
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Long2 mod(Long2 a, Long b) {
        Long2 result = new Long2();
        result.x = a.x % b.x;
        result.y = a.y % b.x;

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public long length() {
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
    public Long dotProduct(Long2 a) {
        return new Long((x * a.x) + (y * a.y));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Long dotProduct(Long2 a, Long2 b) {
        return new Long((b.x * a.x) + (b.y * a.y));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Long2 a, Long factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
    }

    /**
     * set vector value by Long2
     *
     * @param a
     */
    public void set(Long2 a) {
        this.x = a.x;
        this.y = a.y;
    }

    /**
     * set the vector field value by Long
     *
     * @param a
     * @param b
     */
    public void setValues(Long a, Long b) {
        this.x = a.x;
        this.y = b.x;
    }

    /**
     * return the element sum of vector
     *
     * @return
     */
    public Long elementSum() {
        return new Long(x + y);
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
    }
}
