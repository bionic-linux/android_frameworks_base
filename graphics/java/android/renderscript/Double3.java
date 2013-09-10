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
 * Provides three double fields packed.
 */
public class Double3 {
    public double x;
    public double y;
    public double z;

    public Double3() {
    }

    public Double3(Double3 data) {
        this.x = data.x;
        this.y = data.y;
        this.z = data.z;
    }

    public Double3(double x, double y, double z) {
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
    public static Double3 add(Double3 a, Double3 b) {
        Double3 res = new Double3();
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
    public void add(Double3 value) {
        x += value.x;
        y += value.y;
        z += value.z;
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
    }

    /**
     * Vector add
     *
     * @param a
     * @param b
     * @return
     */
    public static Double3 add(Double3 a, Double b) {
        Double3 res = new Double3();
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
    public void sub(Double3 value) {
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
    public static Double3 sub(Double3 a, Double3 b) {
        Double3 res = new Double3();
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
    public void sub(Double value) {
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
    public static Double3 sub(Double3 a, Double b) {
        Double3 res = new Double3();
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
    public void mul(Double3 value) {
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
    public static Double3 mul(Double3 a, Double3 b) {
        Double3 res = new Double3();
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
    public void mul(Double value) {
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
    public static Double3 mul(Double3 a, Double b) {
        Double3 res = new Double3();
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
    public void div(Double3 value) {
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
    public static Double3 div(Double3 a, Double3 b) {
        Double3 res = new Double3();
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
    public void div(Double value) {
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
    public static Double3 div(Double3 a, Double b) {
        Double3 res = new Double3();
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
    public Double dotProduct(Double3 a) {
        return new Double((x * a.x) + (y * a.y) + (z * a.z));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Double dotProduct(Double3 a, Double3 b) {
        return new Double((b.x * a.x) + (b.y * a.y) + (b.z * a.z));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Double3 a, Double factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
        z += a.z * factor.x;
    }

    /**
     * Set vector value by double3
     *
     * @param a
     */
    public void set(Double3 a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
    }

    /**
     * Set vector negate
     */
    public void negate() {
        x = -x;
        y = -y;
        z = -z;
    }

    /**
     * Get vector length
     *
     * @return
     */
    public int length() {
        return 3;
    }

    /**
     * Return the element sum of vector
     *
     * @return
     */
    public Double elementSum() {
        return new Double(x + y + z);
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
     */
    public void setValues(Double x, Double y, Double z) {
        this.x = x.x;
        this.y = y.x;
        this.z = z.x;
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
    }
}
