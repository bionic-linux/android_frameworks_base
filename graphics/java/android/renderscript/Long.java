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
 * Basic Long type.
 */
public final class Long {
    public long x;

    public Long() {
    }

    public Long(Long value) {
        this.x = value.x;
    }

    public Long(long value) {
        this.x = value;
    }

    /**
     * Long add
     *
     * @param value
     */
    public void add(Long value) {
        this.x += value.x;
    }

    /**
     * Long add
     *
     * @param a
     * @param b
     * @return
     */
    public static Long add(Long a, Long b) {
        Long result = new Long();
        result.x = a.x + b.x;

        return result;
    }

    /**
     * Long subtraction
     *
     * @param value
     */
    public void sub(Long value) {
        this.x -= value.x;
    }

    /**
     * Long subtraction
     *
     * @param a
     * @param b
     * @return
     */
    public static Long sub(Long a, Long b) {
        Long result = new Long();
        result.x = a.x - b.x;

        return result;
    }

    /**
     * Long multiplication
     *
     * @param value
     */
    public void mul(Long value) {
        this.x *= value.x;
    }

    /**
     * Long multiplication
     *
     * @param a
     * @param b
     * @return
     */
    public static Long mul(Long a, Long b) {
        Long result = new Long();
        result.x = a.x * b.x;

        return result;
    }

    /**
     * Long division
     *
     * @param value
     */
    public void div(Long value) {
        this.x /= value.x;
    }

    /**
     * Long division
     *
     * @param a
     * @param b
     * @return
     */
    public static Long div(Long a, Long b) {
        Long result = new Long();
        result.x = a.x / b.x;

        return result;
    }

    /**
     * Long Modulo
     *
     * @param value
     */
    public void mod(Long value) {
        this.x %= value.x;
    }

    /**
     * Long Modulo
     *
     * @param a
     * @param b
     * @return
     */
    public static Long mod(Long a, Long b) {
        Long result = new Long();
        result.x = a.x % b.x;

        return result;
    }

    /**
     * set Long value by Long
     *
     * @param a
     */
    public void set(Long a) {
        this.x = a.x;
    }

    /**
     * set Long negate
     */
    public void negate() {
        this.x = -x;
    }
}
