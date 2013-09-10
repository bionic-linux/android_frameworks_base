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

import android.renderscript.UChar;

/**
 * Vector version of the basic uchar type.
 * Provides two uchar fields packed.
 */
public final class UChar2 {
    public UChar x;
    public UChar y;

    public UChar2() {
    }

    public UChar2(UChar2 data) {
        this.x = data.x;
        this.y = data.y;
    }

    public UChar2(UChar x, UChar y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 add(UChar2 a, UChar2 b) {
        UChar2 res = new UChar2();
        res.x = UChar.add(a.x, b.x);
        res.y = UChar.add(a.y, b.y);

        return res;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(UChar2 value) {
        x.add(value.x);
        y.add(value.y);
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(UChar value) {
        x.add(value);
        y.add(value);
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 add(UChar2 a, UChar b) {
        UChar2 res = new UChar2();
        res.x = UChar.add(a.x, b);
        res.y = UChar.add(a.y, b);

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(UChar2 value) {
        x.sub(value.x);
        y.sub(value.y);
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 sub(UChar2 a, UChar2 b) {
        UChar2 res = new UChar2();
        res.x = UChar.sub(a.x, b.x);
        res.y = UChar.sub(a.y, b.y);

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(UChar value) {
        x.sub(value);
        y.sub(value);
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 sub(UChar2 a, UChar b) {
        UChar2 res = new UChar2();
        res.x = UChar.sub(a.x, b);
        res.y = UChar.sub(a.y, b);

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(UChar2 value) {
        x.mul(value.x);
        y.mul(value.y);
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 mul(UChar2 a, UChar2 b) {
        UChar2 res = new UChar2();
        res.x = UChar.mul(a.x, b.x);
        res.y = UChar.mul(a.y, b.y);

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(UChar value) {
        x.mul(value);
        y.mul(value);
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 mul(UChar2 a, UChar b) {
        UChar2 res = new UChar2();
        res.x = UChar.mul(a.x, b);
        res.y = UChar.mul(a.y, b);

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(UChar2 value) {
        x.div(value.x);
        y.div(value.y);
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 div(UChar2 a, UChar2 b) {
        UChar2 res = new UChar2();
        res.x = UChar.div(a.x, b.x);
        res.y = UChar.div(a.y, b.y);

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(UChar value) {
        x.div(value);
        y.div(value);
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar2 div(UChar2 a, UChar b) {
        UChar2 res = new UChar2();
        res.x = UChar.div(a.x, b);
        res.y = UChar.div(a.y, b);

        return res;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public UChar dotProduct(UChar2 a) {
        return UChar.add(UChar.mul(x, a.x), UChar.mul(y, a.y));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar dotProduct(UChar2 a, UChar2 b) {
        return UChar.add(UChar.mul(b.x, a.x), UChar.mul(b.y, a.y));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(UChar2 a, UChar factor) {
        x.add(UChar.mul(a.x, factor));
        y.add(UChar.mul(a.y, factor));
    }

    /**
     * set vector value by UChar2
     *
     * @param a
     */
    public void set(UChar2 a) {
        this.x = a.x;
        this.y = a.y;
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
     * return the element sum of vector
     *
     * @return
     */
    public UChar elementSum() {
        return UChar.add(x, y);
    }

    /**
     * get the vector field value by index
     *
     * @param i
     * @return
     */
    public UChar get(int i) {
        switch (i) {
        case 0:
            return x;
        case 1:
            return y;
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
    public void set(int i, UChar value) {
        switch (i) {
        case 0:
            x = value;
            return;
        case 1:
            y = value;
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
    public void addAt(int i, UChar value) {
        switch (i) {
        case 0:
            x.add(value);
            return;
        case 1:
            y.add(value);
            return;
        default:
            throw new IndexOutOfBoundsException("Index: i");
        }
    }

    /**
     * set the vector field value
     *
     * @param x
     * @param y
     */
    public void setValues(UChar x, UChar y) {
        this.x = x;
        this.y = y;
    }

    /**
     * copy the vector to UChar array
     *
     * @param data
     * @param offset
     */
    public void copyTo(UChar[] data, int offset) {
        data[offset] = x;
        data[offset + 1] = y;
    }
}
