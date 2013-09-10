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
 * Provides four byte fields packed.
 */
public final class Char4 {
    public byte x;
    public byte y;
    public byte z;
    public byte w;

    public Char4() {
    }

    public Char4(byte i) {
        this.x = this.y = this.z = this.w = i;
    }

    public Char4(byte x, byte y, byte z, byte w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Char4(Char4 source) {
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
    public void add(Char4 a) {
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
    public static Char4 add(Char4 a, Char4 b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x + b.x);
        result.y = (byte)(a.y + b.y);
        result.z = (byte)(a.z + b.z);
        result.w = (byte)(a.w + b.w);

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
    public static Char4 add(Char4 a, Char b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x + b.x);
        result.y = (byte)(a.y + b.x);
        result.z = (byte)(a.z + b.x);
        result.w = (byte)(a.w + b.x);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Char4 a) {
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
    public static Char4 sub(Char4 a, Char4 b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x - b.x);
        result.y = (byte)(a.y - b.y);
        result.z = (byte)(a.z - b.z);
        result.w = (byte)(a.w - b.w);

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
    public static Char4 sub(Char4 a, Char b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x - b.x);
        result.y = (byte)(a.y - b.x);
        result.z = (byte)(a.z - b.x);
        result.w = (byte)(a.w - b.x);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Char4 a) {
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
    public static Char4 mul(Char4 a, Char4 b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x * b.x);
        result.y = (byte)(a.y * b.y);
        result.z = (byte)(a.z * b.z);
        result.w = (byte)(a.w * b.w);

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
    public static Char4 mul(Char4 a, Char b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x * b.x);
        result.y = (byte)(a.y * b.x);
        result.z = (byte)(a.z * b.x);
        result.w = (byte)(a.w * b.x);

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Char4 a) {
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
    public static Char4 div(Char4 a, Char4 b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x / b.x);
        result.y = (byte)(a.y / b.y);
        result.z = (byte)(a.z / b.z);
        result.w = (byte)(a.w / b.w);

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
    public static Char4 div(Char4 a, Char b) {
        Char4 result = new Char4();
        result.x = (byte)(a.x / b.x);
        result.y = (byte)(a.y / b.x);
        result.z = (byte)(a.z / b.x);
        result.w = (byte)(a.w / b.x);

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public byte length() {
        return 4;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = (byte)(-x);
        this.y = (byte)(-y);
        this.z = (byte)(-z);
        this.w = (byte)(-w);
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Char4 a) {
        return new Int((x * a.x) + (y * a.y) + (z * a.z) + (w * a.w));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Char4 a, Char4 b) {
        return new Int((b.x * a.x) + (b.y * a.y) + (b.z * a.z) + (b.w * a.w));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Char4 a, Char factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
        w += a.w * factor.x;
    }

    /**
     * set vector value by Char4
     *
     * @param a
     */
    public void set(Char4 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.w = a.w;
    }

    /**
     * set the vector field value by Char
     *
     * @param a
     * @param b
     * @param c
     * @param d
     */
    public void setValues(Char a, Char b, Char c, Char d) {
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
    public Char get(int i) {
        switch (i) {
        case 0:
            return new Char(x);
        case 1:
            return new Char(y);
        case 2:
            return new Char(z);
        case 3:
            return new Char(w);
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
    public void addAt(int i, Char value) {
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
     * copy the vector to Char array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Char[] data, int offset) {
        data[offset] = new Char(x);
        data[offset + 1] = new Char(y);
        data[offset + 2] = new Char(z);
        data[offset + 3] = new Char(w);
    }
}
