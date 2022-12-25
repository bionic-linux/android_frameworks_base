/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.util;

import android.annotation.Nullable;

import java.util.Objects;

/**
 * Container to ease passing around a tuple of four objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 */
public class Quadruple<F, S, T, Q> {
    public final F first;
    public final S second;
    public final T third;
    public final Q fourth;

    /**
     * Constructor for a Quadruple.
     *
     * @param first the first object in the quadruple
     * @param second the second object in the quadruple
     * @param third the third object in the quadruple
     * @param fourth the fourth object in the quadruple
     */
    public Quadruple(F first, S second, T third, Q fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    /**
     * Checks the four objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link Quadruple} to which this one is to be checked for equality
     * @return true if the underlying objects of the Quadruple are all considered
     *         equal
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof Quadruple)) {
            return false;
        }
        Quadruple<?, ?, ?> q = (Quadruple<?, ?, ?, ?>) o;
        return Objects.equals(q.first, first) && Objects.equals(q.second, second) && Objects.equals(q.third, third) && Objects.equals(q.fourth, fourth);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the Quadruple
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode()) ^ (third == null ? 0 : third.hashCode()) ^ (fourth == null ? 0 : fourth.hashCode());
    }

    @Override
    public String toString() {
        return "Quadruple{" + String.valueOf(first) + " " + String.valueOf(second) + " " + String.valueOf(third) + " " + String.valueOf(fourth) + "}";
    }

    /**
     * Convenience method for creating an appropriately typed quadruple.
     * @param a the first object in the Quadruple
     * @param b the second object in the Quadruple
     * @param c the third object in the Quadruple
     * @param d the fourth object in the Quadruple
     * @return a Quadruple that is templatized with the types of a, b, c and d
     */
    public static <A, B, C, D> Triple <A, B, C, D> create(A a, B b, C c, D d) {
        return new Quadruple<A, B, C, D>(a, b, c, d);
    }
}
