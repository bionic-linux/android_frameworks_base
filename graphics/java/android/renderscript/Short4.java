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
 * Provides four short fields packed.
 */
public class Short4 {
    public short x;
    public short y;
    public short z;
    public short w;

    public Short4() {
    }

    public Short4(short i) {
        this.x = this.y = this.z = this.w = i;
    }

    public Short4(short x, short y, short z, short w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Short4(Short4 source) {
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
    public void add(Short4 a) {
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
    public static Short4 add(Short4 a, Short4 b) {
        Short4 result = new Short4();
        result.x = (short)(a.x + b.x);
        result.y = (short)(a.y + b.y);
        result.z = (short)(a.z + b.z);
        result.w = (short)(a.w + b.w);

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
        w += value.x;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Short4 add(Short4 a, Short b) {
        Short4 result = new Short4();
        result.x = (short)(a.x + b.x);
        result.y = (short)(a.y + b.x);
        result.z = (short)(a.z + b.x);
        result.w = (short)(a.w + b.x);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Short4 a) {
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
    public static Short4 sub(Short4 a, Short4 b) {
        Short4 result = new Short4();
        result.x = (short)(a.x - b.x);
        result.y = (short)(a.y - b.y);
        result.z = (short)(a.z - b.z);
        result.w = (short)(a.w - b.w);

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
        w -= value.x;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Short4 sub(Short4 a, Short b) {
        Short4 result = new Short4();
        result.x = (short)(a.x - b.x);
        result.y = (short)(a.y - b.x);
        result.z = (short)(a.z - b.x);
        result.w = (short)(a.w - b.x);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Short4 a) {
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
    public static Short4 mul(Short4 a, Short4 b) {
        Short4 result = new Short4();
        result.x = (short)(a.x * b.x);
        result.y = (short)(a.y * b.y);
        result.z = (short)(a.z * b.z);
        result.w = (short)(a.w * b.w);

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
        w *= value.x;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Short4 mul(Short4 a, Short b) {
        Short4 result = new Short4();
        result.x = (short)(a.x * b.x);
        result.y = (short)(a.y * b.x);
        result.z = (short)(a.z * b.x);
        result.w = (short)(a.w * b.x);

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Short4 a) {
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
    public static Short4 div(Short4 a, Short4 b) {
        Short4 result = new Short4();
        result.x = (short)(a.x / b.x);
        result.y = (short)(a.y / b.y);
        result.z = (short)(a.z / b.z);
        result.w = (short)(a.w / b.w);

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
        w /= value.x;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Short4 div(Short4 a, Short b) {
        Short4 result = new Short4();
        result.x = (short)(a.x / b.x);
        result.y = (short)(a.y / b.x);
        result.z = (short)(a.z / b.x);
        result.w = (short)(a.w / b.x);

        return result;
    }

    /**
     * Vector Modulo
     *
     * @param a
     */
    public void mod(Short4 a) {
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
    public static Short4 mod(Short4 a, Short4 b) {
        Short4 result = new Short4();
        result.x = (short)(a.x % b.x);
        result.y = (short)(a.y % b.y);
        result.z = (short)(a.z % b.z);
        result.w = (short)(a.w % b.w);

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
        w %= value.x;
    }

    /**
     * Vector Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Short4 mod(Short4 a, Short b) {
        Short4 result = new Short4();
        result.x = (short)(a.x % b.x);
        result.y = (short)(a.y % b.x);
        result.z = (short)(a.z % b.x);
        result.w = (short)(a.w % b.x);

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public short length() {
        return 4;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = (short)(-x);
        this.y = (short)(-y);
        this.z = (short)(-z);
        this.w = (short)(-w);
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Short4 a) {
        return new Int((x * a.x) + (y * a.y) + (z * a.z) + (w * a.w));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Short4 a, Short4 b) {
        return new Int((b.x * a.x) + (b.y * a.y) + (b.z * a.z) + (b.w * a.w));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Short4 a, Short factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
        w += a.w * factor.x;
    }

    /**
     * set vector value by Short4
     *
     * @param a
     */
    public void set(Short4 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.w = a.w;
    }

    /**
     * set the vector field value by Short
     *
     * @param a
     * @param b
     * @param c
     * @param d
     */
    public void setValues(Short a, Short b, Short c, Short d) {
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
    public Short get(int i) {
        switch (i) {
        case 0:
            return new Short(x);
        case 1:
            return new Short(y);
        case 2:
            return new Short(z);
        case 3:
            return new Short(w);
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
        case 3:
            w += value.x;
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
        data[offset + 3] = new Short(w);
    }
}
