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
 * Provides three float fields packed.
 */
public class Float3 {
    public float x;
    public float y;
    public float z;

    public Float3() {
    }

    public Float3(Float3 data) {
        this.x = data.x;
        this.y = data.y;
        this.z = data.z;
    }

    public Float3(float x, float y, float z) {
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
    public static Float3 add(Float3 a, Float3 b) {
        Float3 res = new Float3();
        res.x = a.x + b.x;
        res.y = a.y + b.y;
        res.z = a.z + b.z;

        return res;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Float3 value) {
        x += value.x;
        y += value.y;
        z += value.z;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Float value) {
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
    public static Float3 add(Float3 a, Float b) {
        Float3 res = new Float3();
        res.x = a.x + b.x;
        res.y = a.y + b.x;
        res.z = a.z + b.x;

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Float3 value) {
        x -= value.x;
        y -= value.y;
        z -= value.z;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Float3 sub(Float3 a, Float3 b) {
        Float3 res = new Float3();
        res.x = a.x - b.x;
        res.y = a.y - b.y;
        res.z = a.z - b.z;

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
        z -= value.x;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Float3 sub(Float3 a, Float b) {
        Float3 res = new Float3();
        res.x = a.x - b.x;
        res.y = a.y - b.x;
        res.z = a.z - b.x;

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Float3 value) {
        x *= value.x;
        y *= value.y;
        z *= value.z;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Float3 mul(Float3 a, Float3 b) {
        Float3 res = new Float3();
        res.x = a.x * b.x;
        res.y = a.y * b.y;
        res.z = a.z * b.z;

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
        z *= value.x;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Float3 mul(Float3 a, Float b) {
        Float3 res = new Float3();
        res.x = a.x * b.x;
        res.y = a.y * b.x;
        res.z = a.z * b.x;

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Float3 value) {
        x /= value.x;
        y /= value.y;
        z /= value.z;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Float3 div(Float3 a, Float3 b) {
        Float3 res = new Float3();
        res.x = a.x / b.x;
        res.y = a.y / b.y;
        res.z = a.z / b.z;

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
        z /= value.x;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Float3 div(Float3 a, Float b) {
        Float3 res = new Float3();
        res.x = a.x / b.x;
        res.y = a.y / b.x;
        res.z = a.z / b.x;

        return res;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Float dotProduct(Float3 a) {
        return new Float((x * a.x) + (y * a.y) + (z * a.z));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Float dotProduct(Float3 a, Float3 b) {
        return new Float((b.x * a.x) + (b.y * a.y) + (b.z * a.z));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Float3 a, Float factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
    }

    /**
     * set vector value by float3
     *
     * @param a
     */
    public void set(Float3 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
    }

    /**
     * set vector negate
     */
    public void negate() {
        x = -x;
        y = -y;
        z = -z;
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
    public Float elementSum() {
        return new Float(x + y + z);
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
        case 2:
            return new Float(z);
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
    public void addAt(int i, Float value) {
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
     * set the vector field value
     *
     * @param x
     * @param y
     * @param z
     */
    public void setValues(Float x, Float y, Float z) {
        this.x = x.x;
        this.y = y.x;
        this.z = z.x;
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
        data[offset + 2] = new Float(z);
    }
}
