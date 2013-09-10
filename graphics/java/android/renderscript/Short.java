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
 * Basic short type.
 */
public final class Short {
    public short x;

    public Short() {
    }

    public Short(Short value) {
        this.x = value.x;
    }

    public Short(short value) {
        this.x = value;
    }

    /**
     * Short add
     *
     * @param value
     */
    public void add(Short value) {
        this.x += value.x;
    }

    /**
     * Short add
     *
     * @param a
     * @param b
     * @return
     */
    public static Short add(Short a, Short b) {
        Short result = new Short();
        result.x = (short)(a.x + b.x);

        return result;
    }

    /**
     * Short subtraction
     *
     * @param value
     */
    public void sub(Short value) {
        this.x -= value.x;
    }

    /**
     * Short subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Short sub(Short a, Short b) {
        Short result = new Short();
        result.x = (short)(a.x - b.x);

        return result;
    }

    /**
     * Short multiplication
     *
     * @param value
     */
    public void mul(Short value) {
        this.x *= value.x;
    }

    /**
     * Short multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Short mul(Short a, Short b) {
        Short result = new Short();
        result.x = (short)(a.x * b.x);

        return result;
    }

    /**
     * Short division
     *
     * @param value
     */
    public void div(Short value) {
        this.x /= value.x;
    }

    /**
     * Short division
     *
     * @param a
     * @param b
     * @return
     */
    public static Short div(Short a, Short b) {
        Short result = new Short();
        result.x = (short)(a.x / b.x);

        return result;
    }

    /**
     * Short Modulo
     *
     * @param value
     */
    public void mod(Short value) {
        this.x %= value.x;
    }

    /**
     * Short Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Short mod(Short a, Short b) {
        Short result = new Short();
        result.x = (short)(a.x % b.x);

        return result;
    }

    /**
     * set Short value by Short
     *
     * @param a
     */
    public void set(Short a) {
        this.x = a.x;
    }

    /**
     * set Short negate
     */
    public void negate() {
        this.x = (short)(-x);
    }
}
