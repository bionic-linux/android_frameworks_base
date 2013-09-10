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
 * Provides two double fields packed.
 */
public class Double2 {
    public double x;
    public double y;

    public Double2() {
    }

    public Double2(Double2 data) {
        this.x = data.x;
        this.y = data.y;
    }

    public Double2(double x, double y) {
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
    public static Double2 add(Double2 a, Double2 b) {
        Double2 res = new Double2();
        res.x = a.x + b.x;
        res.y = a.y + b.y;

        return res;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Double2 value) {
        x += value.x;
        y += value.y;
    }

    /**
     * Vector add
     *
     * @param value
     */
    public void add(Double value) {
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
    public static Double2 add(Double2 a, Double b) {
        Double2 res = new Double2();
        res.x = a.x + b.x;
        res.y = a.y + b.x;

        return res;
    }

    /**
     * Vector subtraction
     *
     * @param value
     */
    public void sub(Double2 value) {
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
    public static Double2 sub(Double2 a, Double2 b) {
        Double2 res = new Double2();
        res.x = a.x - b.x;
        res.y = a.y - b.y;

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
    }

    /**
     * Vector subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Double2 sub(Double2 a, Double b) {
        Double2 res = new Double2();
        res.x = a.x - b.x;
        res.y = a.y - b.x;

        return res;
    }

    /**
     * Vector multiplication
     *
     * @param value
     */
    public void mul(Double2 value) {
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
    public static Double2 mul(Double2 a, Double2 b) {
        Double2 res = new Double2();
        res.x = a.x * b.x;
        res.y = a.y * b.y;

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
    }

    /**
     * Vector multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Double2 mul(Double2 a, Double b) {
        Double2 res = new Double2();
        res.x = a.x * b.x;
        res.y = a.y * b.x;

        return res;
    }

    /**
     * Vector division
     *
     * @param value
     */
    public void div(Double2 value) {
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
    public static Double2 div(Double2 a, Double2 b) {
        Double2 res = new Double2();
        res.x = a.x / b.x;
        res.y = a.y / b.y;

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
    }

    /**
     * Vector division
     *
     * @param a
     * @param b
     * @return
     */
    public static Double2 div(Double2 a, Double b) {
        Double2 res = new Double2();
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
    public Double dotProduct(Double2 a) {
        return new Double((x * a.x) + (y * a.y));
    }

    /**
     * Vector dot Product
     *
     * @param a
     * @param b
     * @return
     */
    public static Double dotProduct(Double2 a, Double2 b) {
        return new Double((b.x * a.x) + (b.y * a.y));
    }

    /**
     * Vector add Multiple
     *
     * @param a
     * @param factor
     */
    public void addMultiple(Double2 a, Double factor) {
        x += a.x * factor.x;
        y += a.y * factor.x;
    }

    /**
     * Set vector value by double2
     *
     * @param a
     */
    public void set(Double2 a) {
        this.x = a.x;
        this.y = a.y;
    }

    /**
     * Set vector negate
     */
    public void negate() {
        x = -x;
        y = -y;
    }

    /**
     * Get vector length
     *
     * @return
     */
    public int length() {
        return 2;
    }

    /**
     * Return the element sum of vector
     *
     * @return
     */
    public Double elementSum() {
        return new Double(x + y);
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
        default:
            throw new IndexOutOfBoundsException("Index: i");
        }
    }

    /**
     * Set the vector field value
     *
     * @param x
     * @param y
     */
    public void setValues(Double x, Double y) {
        this.x = x.x;
        this.y = y.x;
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
    }
}
