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
 * Basic float type.
 */
public class Float {

    public float x;

    public Float() {
    }

    public Float(Float data) {
        this.x = data.x;
    }

    public Float(float x) {
        this.x = x;
    }

    /**
     * Float add
     *
     * @param a
     * @param b
     * @return
     */
    public static Float add(Float a, Float b) {
        Float res = new Float();
        res.x = a.x + b.x;

        return res;
    }

    /**
     * Float add
     *
     * @param value
     */
    public void add(Float value) {
        x += value.x;
    }

    /**
     * Float subtraction
     *
     * @param value
     */
    public void sub(Float value) {
        x -= value.x;
    }

    /**
     * Float subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Float sub(Float a, Float b) {
        Float res = new Float();
        res.x = a.x - b.x;

        return res;
    }

    /**
     * Float multiplication
     *
     * @param value
     */
    public void mul(Float value) {
        x *= value.x;
    }

    /**
     * Float multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Float mul(Float a, Float b) {
        Float res = new Float();
        res.x = a.x * b.x;

        return res;
    }

    /**
     * Float division
     *
     * @param dt
     */
    public void div(Float dt) {
        x /= dt.x;
    }

    /**
     * Float division
     *
     * @param a
     * @param b
     * @return
     */
    public static Float div(Float a, Float b) {
        Float res = new Float();
        res.x = a.x / b.x;

        return res;
    }

    /**
     * set value by Float
     *
     * @param a
     */
    public void set(Float a) {
        this.x = a.x;
    }

    /**
     * set Float negate
     */
    public void negate() {
        x = -x;
    }

    public static int floatToRawIntBits(float value) {
      return java.lang.Float.floatToRawIntBits(value);
    }

    public static float intBitsToFloat(int bits) {
      return java.lang.Float.intBitsToFloat(bits);
    }

}
