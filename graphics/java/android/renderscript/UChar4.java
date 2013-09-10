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
 * Provides four uchar fields packed.
 */
public final class UChar4 {
    public UChar x;
    public UChar y;
    public UChar z;
    public UChar w;

    public UChar4() {
    }

    public UChar4(UChar4 data) {
        this.x = data.x;
        this.y = data.y;
        this.z = data.z;
        this.w = data.w;
    }

    public UChar4(UChar x, UChar y, UChar z, UChar w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 add(UChar4 a, UChar4 b) {
        UChar4 res = new UChar4();
        res.x = UChar.add(a.x, b.x);
        res.y = UChar.add(a.y, b.y);
        res.z = UChar.add(a.z, b.z);
        res.w = UChar.add(a.w, b.w);

        return res;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(UChar4 value) {
        x.add(value.x);
        y.add(value.y);
        z.add(value.z);
        w.add(value.w);
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
        w.add(value);
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 add(UChar4 a, UChar b) {
        UChar4 res = new UChar4();
        res.x = UChar.add(a.x, b);
        res.y = UChar.add(a.y, b);
        res.z = UChar.add(a.z, b);
        res.w = UChar.add(a.w, b);

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(UChar4 value) {
        x.sub(value.x);
        y.sub(value.y);
        z.sub(value.z);
        w.sub(value.w);
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 sub(UChar4 a, UChar4 b) {
        UChar4 res = new UChar4();
        res.x = UChar.sub(a.x, b.x);
        res.y = UChar.sub(a.y, b.y);
        res.z = UChar.sub(a.z, b.z);
        res.w = UChar.sub(a.w, b.w);

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
        w.sub(value);
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 sub(UChar4 a, UChar b) {
        UChar4 res = new UChar4();
        res.x = UChar.sub(a.x, b);
        res.y = UChar.sub(a.y, b);
        res.z = UChar.sub(a.z, b);
        res.w = UChar.sub(a.w, b);

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(UChar4 value) {
        x.mul(value.x);
        y.mul(value.y);
        z.mul(value.z);
        w.mul(value.w);
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 mul(UChar4 a, UChar4 b) {
        UChar4 res = new UChar4();
        res.x = UChar.mul(a.x, b.x);
        res.y = UChar.mul(a.y, b.y);
        res.z = UChar.mul(a.z, b.z);
        res.w = UChar.mul(a.w, b.w);

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
        w.mul(value);
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 mul(UChar4 a, UChar b) {
        UChar4 res = new UChar4();
        res.x = UChar.mul(a.x, b);
        res.y = UChar.mul(a.y, b);
        res.z = UChar.mul(a.z, b);
        res.w = UChar.mul(a.w, b);

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(UChar4 value) {
        x.div(value.x);
        y.div(value.y);
        z.div(value.z);
        w.div(value.w);
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 div(UChar4 a, UChar4 b) {
        UChar4 res = new UChar4();
        res.x = UChar.div(a.x, b.x);
        res.y = UChar.div(a.y, b.y);
        res.z = UChar.div(a.z, b.z);
        res.w = UChar.div(a.w, b.w);

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
        w.div(value);
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar4 div(UChar4 a, UChar b) {
        UChar4 res = new UChar4();
        res.x = UChar.div(a.x, b);
        res.y = UChar.div(a.y, b);
        res.z = UChar.div(a.z, b);
        res.w = UChar.div(a.w, b);

        return res;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public UChar dotProduct(UChar4 a) {
        UChar mChar = UChar.add(UChar.mul(x, a.x), UChar.mul(y, a.y));
        UChar nChar = UChar.add(UChar.mul(z, a.z), UChar.mul(w, a.w));

        return UChar.add(mChar, nChar);
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static UChar dotProduct(UChar4 a, UChar4 b) {
        UChar mChar = UChar.add(UChar.mul(b.x, a.x), UChar.mul(b.y, a.y));
        UChar nChar = UChar.add(UChar.mul(b.z, a.z), UChar.mul(b.w, a.w));

        return UChar.add(mChar, nChar);
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(UChar4 a, UChar factor) {
        x.add(UChar.mul(a.x, factor));
        y.add(UChar.mul(a.y, factor));
        z.add(UChar.mul(a.z, factor));
        w.add(UChar.mul(a.w, factor));
    }

    /**
     * set vector value by UChar4
     *
     * @param a
     */
    public void set(UChar4 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.w = a.w;
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
     * return the element sum of vector
     *
     * @return
     */
    public UChar elementSum() {
        UChar aChar = UChar.add(x, y);
        UChar bChar = UChar.add(z, w);

        return UChar.add(aChar, bChar);
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
        case 3:
            return w;
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
        case 3:
            w = value;
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
        case 3:
            w.add(value);
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
        this.w = w;
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
        data[offset + 3] = w;
    }
}
