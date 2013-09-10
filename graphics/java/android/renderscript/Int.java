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
 * Basic Int type.
 */
public final class Int {
    public int x;

    public Int() {
    }

    public Int(Int value) {
        this.x = value.x;
    }

    public Int(int value) {
        this.x = value;
    }

    /**
     * Int add
     *
     * @param value
     */
    public void add(Int value) {
        this.x += value.x;
    }

    /**
     * Int add
     *
     * @param a
     * @param b
     * @return
     */
    public static Int add(Int a, Int b) {
        Int result = new Int();
        result.x = a.x + b.x;

        return result;
    }

    /**
     * Int subtraction
     *
     * @param value
     */
    public void sub(Int value) {
        this.x -= value.x;
    }

    /**
     * Int subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Int sub(Int a, Int b) {
        Int result = new Int();
        result.x = a.x - b.x;

        return result;
    }

    /**
     * Int multiplication
     *
     * @param value
     */
    public void mul(Int value) {
        this.x *= value.x;
    }

    /**
     * Int multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Int mul(Int a, Int b) {
        Int result = new Int();
        result.x = a.x * b.x;

        return result;
    }

    /**
     * Int division
     *
     * @param value
     */
    public void div(Int value) {
        this.x /= value.x;
    }

    /**
     * Int division
     *
     * @param a
     * @param b
     * @return
     */
    public static Int div(Int a, Int b) {
        Int result = new Int();
        result.x = a.x / b.x;

        return result;
    }

    /**
     * Int Modulo
     *
     * @param value
     */
    public void mod(Int value) {
        this.x %= value.x;
    }

    /**
     * Int Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Int mod(Int a, Int b) {
        Int result = new Int();
        result.x = a.x % b.x;

        return result;
    }

    /**
     * set Int value by Int
     *
     * @param a
     */
    public void set(Int a) {
        this.x = a.x;
    }

    /**
     * set Int negate
     */
    public void negate() {
        this.x = -x;
    }
}
