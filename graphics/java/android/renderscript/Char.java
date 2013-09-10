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
 * Basic Char type.
 */
public final class Char {
    public byte x;

    public Char() {
    }

    public Char(Char value) {
        this.x = value.x;
    }

    public Char(byte value) {
        this.x = value;
    }

    /**
     * Char add
     *
     * @param value
     */
    public void add(Char value) {
        this.x += value.x;
    }

    /**
     * Char add
     *
     * @param a
     * @param b
     * @return
     */
    public static Char add(Char a, Char b) {
        Char result = new Char();
        result.x = (byte)(a.x + b.x);

        return result;
    }

    /**
     * Char subtraction
     *
     * @param value
     */
    public void sub(Char value) {
        this.x -= value.x;
    }

    /**
     * Char subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Char sub(Char a, Char b) {
        Char result = new Char();
        result.x = (byte)(a.x - b.x);

        return result;
    }

    /**
     * Char multiplication
     *
     * @param value
     */
    public void mul(Char value) {
        this.x *= value.x;
    }

    /**
     * Char multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Char mul(Char a, Char b) {
        Char result = new Char();
        result.x = (byte)(a.x * b.x);

        return result;
    }

    /**
     * Char division
     *
     * @param value
     */
    public void div(Char value) {
        this.x /= value.x;
    }

    /**
     * Char division
     *
     * @param a
     * @param b
     * @return
     */
    public static Char div(Char a, Char b) {
        Char result = new Char();
        result.x = (byte)(a.x / b.x);

        return result;
    }

    /**
     * set Char value by Char
     *
     * @param a
     */
    public void set(Char a) {
        this.x = a.x;
    }

    /**
     * set Char negate
     */
    public void negate() {
        this.x = (byte)(-x);
    }
}
