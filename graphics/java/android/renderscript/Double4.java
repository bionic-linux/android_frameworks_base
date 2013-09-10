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
 * Vector version of the basic double type.
 * Provides four double fields packed.
 */
public class Double4 {
    public double x;
    public double y;
    public double z;
    public double w;

    public Double4() {
    }

    public Double4(Double4 data) {
        this.x = data.x;
        this.y = data.y;
        this.z = data.z;
        this.w = data.w;
    }

    public Double4(double x, double y, double z, double w) {
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
    public static Double4 add(Double4 a, Double4 b) {
        Double4 res = new Double4();
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
    public void add(Double4 value) {
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
    public void add(Double value) {
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
    public static Double4 add(Double4 a, Double b) {
        Double4 res = new Double4();
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
    public void sub(Double4 value) {
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
    public void sub(Double value) {
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
    public static Double4 sub(Double4 a, Double b) {
        Double4 res = new Double4();
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
    public static Double4 sub(Double4 a, Double4 b) {
        Double4 res = new Double4();
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
    public void mul(Double4 value) {
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
    public void mul(Double value) {
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
    public static Double4 mul(Double4 a, Double4 b) {
        Double4 res = new Double4();
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
    public static Double4 mul(Double4 a, Double b) {
        Double4 res = new Double4();
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
    public void div(Double4 value) {
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
    public void div(Double value) {
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
    public static Double4 div(Double4 a, Double b) {
        Double4 res = new Double4();
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
    public static Double4 div(Double4 a, Double4 b) {
        Double4 res = new Double4();
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
    public Double dotProduct(Double4 a) {
        return new Double((x * a.x) + (y * a.y) + (z * a.z) + (w * a.w));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Double dotProduct(Double4 a, Double4 b) {
        return new Double((b.x * a.x) + (b.y * a.y) + (b.z * a.z) + (b.w * a.w));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Double4 a, Double factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
        w += a.w * factor.x;
    }

    /**
     * Set vector value by double4
     *
     * @param a
     */
    public void set(Double4 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.w = a.w;
    }

    /**
     * Set vector negate
     */
    public void negate() {
        x = -x;
        y = -y;
        z = -z;
        w = -w;
    }

    /**
     * Get vector length
     *
     * @return
     */
    public int length() {
        return 4;
    }

    /**
     * Return the element sum of vector
     *
     * @return
     */
    public Double elementSum() {
        return new Double(x + y + z + w);
    }

    /**
     * Get the vector field value by index
     *
     * @param i
     * @return
     */
    public Double get(int i) {
        switch (i) {
        case 0:
            return new Double(x);
        case 1:
            return new Double(y);
        case 2:
            return new Double(z);
        case 3:
            return new Double(w);
        default:
            throw new IndexOutOfBoundsException("Index: i");
        }
    }

    /**
     * Set the vector field value by index
     *
     * @param i
     * @param value
     */
    public void set(int i, Double value) {
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
     * Add the vector field value by index
     *
     * @param i
     * @param value
     */
    public void addAt(int i, Double value) {
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
     * Set the vector field value
     *
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public void setValues(Double x, Double y, Double z, Double w) {
        this.x = x.x;
        this.y = y.x;
        this.z = z.x;
        this.w = w.x;
    }

    /**
     * Copy the vector to double array
     *
     * @param data
     * @param offset
     */
    public void copyTo(Double[] data, int offset) {
        data[offset] = new Double(x);
        data[offset + 1] = new Double(y);
        data[offset + 2] = new Double(z);
        data[offset + 3] = new Double(w);
    }
}
