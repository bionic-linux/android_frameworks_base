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
 * Basic double type.
 */
public final class Double {

    public double x;

    public Double() {
    }

    public Double(Double data) {
        this.x = data.x;
    }

    public Double(double x) {
        this.x = x;
    }

    /**
     * Double add
     *
     * @param a
     * @param b
     * @return
     */
    public static Double add(Double a, Double b) {
        Double res = new Double();
        res.x = a.x + b.x;

        return res;
    }

    /**
     * Double add
     *
     * @param value
     */
    public void add(Double value) {
        x += value.x;
    }

    /**
     * Double subtraction
     *
     * @param value
     */
    public void sub(Double value) {
        x -= value.x;
    }

    /**
     * Double subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Double sub(Double a, Double b) {
        Double res = new Double();
        res.x = a.x - b.x;

        return res;
    }

    /**
     * Double multiplication
     *
     * @param value
     */
    public void mul(Double value) {
        x *= value.x;
    }

    /**
     * Double multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Double mul(Double a, Double b) {
        Double res = new Double();
        res.x = a.x * b.x;

        return res;
    }

    /**
     * Double division
     *
     * @param value
     */
    public void div(Double value) {
        x /= value.x;
    }

    /**
     * Double division
     *
     * @param a
     * @param b
     * @return
     */
    public static Double div(Double a, Double b) {
        Double res = new Double();
        res.x = a.x / b.x;

        return res;
    }

    /**
     * Set value by Double
     *
     * @param a
     */
    public void set(Double a) {
        this.x = a.x;
    }

    /**
     * Set Double negate
     */
    public void negate() {
        x = -x;
    }

    public static long doubleToRawLongBits(double value) {
      return java.lang.Double.doubleToRawLongBits(value);
    }

    public static double longBitsToDouble(long value) {
      return java.lang.Double.longBitsToDouble(value);
    }
}
