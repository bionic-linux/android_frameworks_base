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
 * Provides three uchar fields packed.
 */
public final class UChar3 {
    public UChar x;
    public UChar y;
    public UChar z;

    public UChar3() {
    }

    public UChar3(UChar3 data) {
        this.x = data.x;
        this.y = data.y;
        this.z = data.z;
    }

    public UChar3(UChar x, UChar y, UChar z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 add(UChar3 a, UChar3 b) {
        UChar3 res = new UChar3();
        res.x = UChar.add(a.x, b.x);
        res.y = UChar.add(a.y, b.y);
        res.z = UChar.add(a.z, b.z);

        return res;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(UChar3 value) {
        x.add(value.x);
        y.add(value.y);
        z.add(value.z);
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(UChar value) {
        x.add(value);
        y.add(value);
        z.add(value);
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 add(UChar3 a, UChar b) {
        UChar3 res = new UChar3();
        res.x = UChar.add(a.x, b);
        res.y = UChar.add(a.y, b);
        res.z = UChar.add(a.z, b);

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(UChar3 value) {
        x.sub(value.x);
        y.sub(value.y);
        z.sub(value.z);
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 sub(UChar3 a, UChar3 b) {
        UChar3 res = new UChar3();
        res.x = UChar.sub(a.x, b.x);
        res.y = UChar.sub(a.y, b.y);
        res.z = UChar.sub(a.z, b.z);

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
        z.sub(value);
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 sub(UChar3 a, UChar b) {
        UChar3 res = new UChar3();
        res.x = UChar.sub(a.x, b);
        res.y = UChar.sub(a.y, b);
        res.z = UChar.sub(a.z, b);

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(UChar3 value) {
        x.mul(value.x);
        y.mul(value.y);
        z.mul(value.z);
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 mul(UChar3 a, UChar3 b) {
        UChar3 res = new UChar3();
        res.x = UChar.mul(a.x, b.x);
        res.y = UChar.mul(a.y, b.y);
        res.z = UChar.mul(a.z, b.z);

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
        z.mul(value);
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 mul(UChar3 a, UChar b) {
        UChar3 res = new UChar3();
        res.x = UChar.mul(a.x, b);
        res.y = UChar.mul(a.y, b);
        res.z = UChar.mul(a.z, b);

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(UChar3 value) {
        x.div(value.x);
        y.div(value.y);
        z.div(value.z);
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 div(UChar3 a, UChar3 b) {
        UChar3 res = new UChar3();
        res.x = UChar.div(a.x, b.x);
        res.y = UChar.div(a.y, b.y);
        res.z = UChar.div(a.z, b.z);

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
        z.div(value);
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar3 div(UChar3 a, UChar b) {
        UChar3 res = new UChar3();
        res.x = UChar.div(a.x, b);
        res.y = UChar.div(a.y, b);
        res.z = UChar.div(a.z, b);

        return res;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public UChar dotProduct(UChar3 a) {
        UChar mChar = UChar.add(UChar.mul(x, a.x), UChar.mul(y, a.y));
        UChar nChar = UChar.add(UChar.mul(z, a.z), mChar);

        return nChar;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar dotProduct(UChar3 a, UChar3 b) {
        UChar mChar = UChar.add(UChar.mul(b.x, a.x), UChar.mul(b.y, a.y));
        UChar nChar = UChar.add(UChar.mul(b.z, a.z), mChar);

        return nChar;
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(UChar3 a, UChar factor) {
        x.add(UChar.mul(a.x, factor));
        y.add(UChar.mul(a.y, factor));
        z.add(UChar.mul(a.z, factor));
    }

    /**
     * set vector value by UChar3
     *
     * @param a
     */
    public void set(UChar3 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
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
     * return the element sum of vector
     *
     * @return
     */
    public UChar elementSum() {
        UChar aChar = UChar.add(x, y);
        UChar bChar = UChar.add(z, aChar);

        return bChar;
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
        case 2:
            return z;
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
        case 2:
            z = value;
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
        case 2:
            z.add(value);
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
    public void setValues(UChar x, UChar y, UChar z, UChar w) {
        this.x = x;
        this.y = y;
        this.z = z;
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
        data[offset + 2] = z;
    }
}
