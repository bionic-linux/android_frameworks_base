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
 * Provides three byte fields packed.
 */
public final class Char3 {
    public byte x;
    public byte y;
    public byte z;

    public Char3() {
    }

    public Char3(byte i) {
        this.x = this.y = this.z = i;
    }

    public Char3(byte x, byte y, byte z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Char3(Char3 source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
    }

    /**
     * Vector add
     *
     * @param a
     */
    public void add(Char3 a) {
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
    public static Char3 add(Char3 a, Char3 b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x + b.x);
        result.y = (byte)(a.y + b.y);
        result.z = (byte)(a.z + b.z);

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
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Char3 add(Char3 a, Char b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x + b.x);
        result.y = (byte)(a.y + b.x);
        result.z = (byte)(a.z + b.x);

        return result;
    }

    /**
     * Vector subtraction
     *
     * @param a
     */
    public void sub(Char3 a) {
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
    public static Char3 sub(Char3 a, Char3 b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x - b.x);
        result.y = (byte)(a.y - b.y);
        result.z = (byte)(a.z - b.z);

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
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Char3 sub(Char3 a, Char b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x - b.x);
        result.y = (byte)(a.y - b.x);
        result.z = (byte)(a.z - b.x);

        return result;
    }

    /**
     * Vector multiplication
     *
     * @param a
     */
    public void mul(Char3 a) {
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
    public static Char3 mul(Char3 a, Char3 b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x * b.x);
        result.y = (byte)(a.y * b.y);
        result.z = (byte)(a.z * b.z);

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
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Char3 mul(Char3 a, Char b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x * b.x);
        result.y = (byte)(a.y * b.x);
        result.z = (byte)(a.z * b.x);

        return result;
    }

    /**
     * Vector division
     *
     * @param a
     */
    public void div(Char3 a) {
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
    public static Char3 div(Char3 a, Char3 b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x / b.x);
        result.y = (byte)(a.y / b.y);
        result.z = (byte)(a.z / b.z);

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
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Char3 div(Char3 a, Char b) {
        Char3 result = new Char3();
        result.x = (byte)(a.x / b.x);
        result.y = (byte)(a.y / b.x);
        result.z = (byte)(a.z / b.x);

        return result;
    }

    /**
     * get vector length
     *
     * @return
     */
    public byte length() {
        return 3;
    }

    /**
     * set vector negate
     */
    public void negate() {
        this.x = (byte)(-x);
        this.y = (byte)(-y);
        this.z = (byte)(-z);
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Int dotProduct(Char3 a) {
        return new Int((x * a.x) + (y * a.y) + (z * a.z));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Int dotProduct(Char3 a, Char3 b) {
        return new Int((b.x * a.x) + (b.y * a.y) + (b.z * a.z));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Char3 a, Char factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
    }

    /**
     * set vector value by Char3
     *
     * @param a
     */
    public void set(Char3 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
    }

    /**
     * set the vector field value by Char
     *
     * @param a
     * @param b
     * @param c
     */
    public void setValues(Char a, Char b, Char c) {
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
    public Char get(int i) {
        switch (i) {
        case 0:
            return new Char(x);
        case 1:
            return new Char(y);
        case 2:
            return new Char(z);
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
    }
}
