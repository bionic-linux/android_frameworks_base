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
 * Provides four float fields packed.
 */
public class Float4 {
    public float x;
    public float y;
    public float z;
    public float w;

    public Float4() {
    }

    public Float4(Float4 data) {
        this.x = data.x;
        this.y = data.y;
        this.z = data.z;
        this.w = data.w;
    }

    public Float4(float x, float y, float z, float w) {
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
    public static Float4 add(Float4 a, Float4 b) {
        Float4 res = new Float4();
        res.x = a.x + b.x;
        res.y = a.y + b.y;
        res.z = a.z + b.z;
        res.w = a.w + b.w;

        return res;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Float4 value) {
        x += value.x;
        y += value.y;
        z += value.z;
        w += value.w;
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
        w += value.x;
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Float4 add(Float4 a, Float b) {
        Float4 res = new Float4();
        res.x = a.x + b.x;
        res.y = a.y + b.x;
        res.z = a.z + b.x;
        res.w = a.w + b.x;

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Float4 value) {
        x -= value.x;
        y -= value.y;
        z -= value.z;
        w -= value.w;
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
        w -= value.x;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Float4 sub(Float4 a, Float b) {
        Float4 res = new Float4();
        res.x = a.x - b.x;
        res.y = a.y - b.x;
        res.z = a.z - b.x;
        res.w = a.w - b.x;

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Float4 sub(Float4 a, Float4 b) {
        Float4 res = new Float4();
        res.x = a.x - b.x;
        res.y = a.y - b.y;
        res.z = a.z - b.z;
        res.w = a.w - b.w;

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Float4 value) {
        x *= value.x;
        y *= value.y;
        z *= value.z;
        w *= value.w;
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
        w *= value.x;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Float4 mul(Float4 a, Float4 b) {
        Float4 res = new Float4();
        res.x = a.x * b.x;
        res.y = a.y * b.y;
        res.z = a.z * b.z;
        res.w = a.w * b.w;

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Float4 mul(Float4 a, Float b) {
        Float4 res = new Float4();
        res.x = a.x * b.x;
        res.y = a.y * b.x;
        res.z = a.z * b.x;
        res.w = a.w * b.x;

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Float4 value) {
        x /= value.x;
        y /= value.y;
        z /= value.z;
        w /= value.w;
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
        w /= value.x;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Float4 div(Float4 a, Float b) {
        Float4 res = new Float4();
        res.x = a.x / b.x;
        res.y = a.y / b.x;
        res.z = a.z / b.x;
        res.w = a.w / b.x;

        return res;
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Float4 div(Float4 a, Float4 b) {
        Float4 res = new Float4();
        res.x = a.x / b.x;
        res.y = a.y / b.y;
        res.z = a.z / b.z;
        res.w = a.w / b.w;

        return res;
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @return
     */
    public Float dotProduct(Float4 a) {
        return new Float((x * a.x) + (y * a.y) + (z * a.z) + (w * a.w));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Float dotProduct(Float4 a, Float4 b) {
        return new Float((b.x * a.x) + (b.y * a.y) + (b.z * a.z) + (b.w * a.w));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Float4 a, Float factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
        w += a.w * factor.x;
    }

    /**
     * set vector value by float4
     *
     * @param a
     */
    public void set(Float4 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.w = a.w;
    }

    /**
     * set vector negate
     */
    public void negate() {
        x = -x;
        y = -y;
        z = -z;
        w = -w;
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
    public Float elementSum() {
        return new Float(x + y + z + w);
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
        case 3:
            return new Float(w);
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
        case 3:
            w += value.x;
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
     * @param w
     */
    public void setValues(Float x, Float y, Float z, Float w) {
        this.x = x.x;
        this.y = y.x;
        this.z = z.x;
        this.w = w.x;
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
        data[offset + 3] = new Float(w);
    }
}
