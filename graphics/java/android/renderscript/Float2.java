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
 * Vector version of the basic float type.
 * Provides two float fields packed.
 */
public  class Float2 {
    public float x;
    public float y;

    public Float2() {
    }

    public Float2(Float2 data) {
        this.x = data.x;
        this.y = data.y;
    }

    public Float2(float x, float y) {
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
    public static Float2 add(Float2 a, Float2 b) {
        Float2 res = new Float2();
        res.x = a.x + b.x;
        res.y = a.y + b.y;

        return res;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Float2 value) {
        x += value.x;
        y += value.y;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Float value) {
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
    public static Float2 add(Float2 a, Float b) {
        Float2 res = new Float2();
        res.x = a.x + b.x;
        res.y = a.y + b.x;

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Float2 value) {
        x -= value.x;
        y -= value.y;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Float2 sub(Float2 a, Float2 b) {
        Float2 res = new Float2();
        res.x = a.x - b.x;
        res.y = a.y - b.y;

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Float value) {
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
    public static Float2 sub(Float2 a, Float b) {
        Float2 res = new Float2();
        res.x = a.x - b.x;
        res.y = a.y - b.x;

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Float2 value) {
        x *= value.x;
        y *= value.y;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Float2 mul(Float2 a, Float2 b) {
        Float2 res = new Float2();
        res.x = a.x * b.x;
        res.y = a.y * b.y;

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Float value) {
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
    public static Float2 mul(Float2 a, Float b) {
        Float2 res = new Float2();
        res.x = a.x * b.x;
        res.y = a.y * b.x;

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Float2 value) {
        x /= value.x;
        y /= value.y;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Float2 div(Float2 a, Float2 b) {
        Float2 res = new Float2();
        res.x = a.x / b.x;
        res.y = a.y / b.y;

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Float value) {
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
    public static Float2 div(Float2 a, Float b) {
        Float2 res = new Float2();
        res.x = a.x / b.x;
        res.y = a.y / b.x;

        return res;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Float dotProduct(Float2 a) {
        return new Float((x * a.x) + (y * a.y));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Float dotProduct(Float2 a, Float2 b) {
        return new Float((b.x * a.x) + (b.y * a.y));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Float2 a, Float factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
    }

    /**
     * set vector value by float2
     *
     * @param a
     */
    public void set(Float2 a) {
        this.x = a.x;
        this.y = a.y;
    }

    /**
     * set vector negate
     */
    public void negate() {
        x = -x;
        y = -y;
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
    public Float elementSum() {
        return new Float(x + y);
    }

    /**
     * get the vector field value by index
     *
     * @param i
     * @return
     */
    public Float get(int i) {
        switch (i) {
        case 0:
            return new Float(x);
        case 1:
            return new Float(y);
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
    public void set(int i, Float value) {
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
    public void addAt(int i, Float value) {
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
     * set the vector field value
     *
     * @param x
     * @param y
     */
    public void setValues(Float x, Float y) {
        this.x = x.x;
        this.y = y.x;
    }

    /**
     * copy the vector to float array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Float[] data, int offset) {
        data[offset] = new Float(x);
        data[offset + 1] = new Float(y);
    }
}
