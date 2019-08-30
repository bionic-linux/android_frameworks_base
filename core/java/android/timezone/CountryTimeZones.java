/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.timezone;

import android.annotation.SystemApi;
import android.icu.util.TimeZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Information about a country's time zones.
 *
 * @hide
 */
@SystemApi
public final class CountryTimeZones {

    /**
     * A mapping to a time zone ID with some associated metadata.
     *
     * @hide
     */
    @SystemApi
    public static final class TimeZoneMapping {

        private libcore.timezone.CountryTimeZones.TimeZoneMapping mDelegate;

        TimeZoneMapping(libcore.timezone.CountryTimeZones.TimeZoneMapping delegate) {
            this.mDelegate = Objects.requireNonNull(delegate);
        }

        /**
         * Returns the ID for this mapping. See also {@link #getTimeZone()} which handles when the
         * ID is unrecognized.
         *
         * @hide
         */
        @SystemApi
        public String getTimeZoneId() {
            return mDelegate.timeZoneId;
        }

        /**
         * Returns a {@link TimeZone} object for this mapping, or {@code null} if the ID is
         * unrecognized.
         *
         * @hide
         */
        @SystemApi
        public TimeZone getTimeZone() {
            return mDelegate.getTimeZone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TimeZoneMapping that = (TimeZoneMapping) o;
            return this.mDelegate.equals(that.mDelegate);
        }

        @Override
        public int hashCode() {
            return this.mDelegate.hashCode();
        }

        @Override
        public String toString() {
            return mDelegate.toString();
        }
    }

    /**
     * The result of lookup up a time zone using offset information (and possibly more).
     *
     * @hide
     */
    @SystemApi
    public static final class OffsetResult {

        private final TimeZone mTimeZone;
        private final boolean mIsOnlyMatch;

        /** @hide */
        @SystemApi
        public OffsetResult(TimeZone timeZone, boolean isOnlyMatch) {
            mTimeZone = Objects.requireNonNull(timeZone);
            mIsOnlyMatch = isOnlyMatch;
        }

        /**
         * Returns a time zone that matches the supplied criteria.
         *
         * @hide
         */
        @SystemApi
        public TimeZone getTimeZone() {
            return mTimeZone;
        }

        /**
         * Returns {@code true} if there is only one matching time zone for the supplied criteria.
         *
         * @hide
         */
        @SystemApi
        public boolean getIsOnlyMatch() {
            return mIsOnlyMatch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OffsetResult that = (OffsetResult) o;
            return mIsOnlyMatch == that.mIsOnlyMatch
                    && mTimeZone.getID().equals(that.mTimeZone.getID());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mTimeZone, mIsOnlyMatch);
        }

        @Override
        public String toString() {
            return "OffsetResult{"
                    + "mTimeZone=" + mTimeZone
                    + ", mIsOnlyMatch=" + mIsOnlyMatch
                    + '}';
        }
    }

    private final libcore.timezone.CountryTimeZones mDelegate;

    CountryTimeZones(libcore.timezone.CountryTimeZones delegate) {
        mDelegate = delegate;
    }

    /**
     * Returns true if the ISO code for the country is a match for the one specified.
     *
     * @hide
     */
    @SystemApi
    public boolean isForCountryCode(String countryIso) {
        return mDelegate.isForCountryCode(countryIso);
    }

    /**
     * Returns the default time zone ID for the country. Can return null in cases when no data is
     * available or the time zone ID was not recognized.
     *
     * @hide
     */
    @SystemApi
    public String getDefaultTimeZoneId() {
        return mDelegate.getDefaultTimeZoneId();
    }

    /**
     * Returns the default time zone for the country. Can return null in cases when no data is
     * available or the time zone ID was not recognized.
     *
     * @hide
     */
    @SystemApi
    public TimeZone getDefaultTimeZone() {
        return mDelegate.getDefaultTimeZone();
    }

    /**
     * Returns true if the country has at least one zone that is the same as UTC at the given time.
     *
     * @hide
     */
    @SystemApi
    public boolean hasUtcZone(long whenMillis) {
        return mDelegate.hasUtcZone(whenMillis);
    }

    /**
     * Returns a time zone for the country, if there is one, that matches the desired properties. If
     * there are multiple matches and the {@code bias} is one of them then it is returned, otherwise
     * an arbitrary match is returned based on the {@link #getEffectiveTimeZoneMappingsAt(long)}
     * ordering.
     *
     * @param totalOffsetMillis the offset from UTC at {@code whenMillis}
     * @param isDst the Daylight Savings Time state at {@code whenMillis}. {@code true} means DST,
     *     {@code false} means not DST, {@code null} means unknown
     * @param dstOffsetMillis the part of {@code totalOffsetMillis} contributed by DST, only used if
     *     {@code isDst} is {@code true}. The value can be {@code null} if the DST offset is
     *     unknown
     * @param whenMillis the UTC time to match against
     * @param bias the time zone to prefer, can be {@code null}
     *
     * @hide
     */
    @SystemApi
    public OffsetResult lookupByOffsetWithBias(int totalOffsetMillis, Boolean isDst,
            Integer dstOffsetMillis, long whenMillis, TimeZone bias) {
        libcore.timezone.CountryTimeZones.OffsetResult delegateOffsetResult =
                mDelegate.lookupByOffsetWithBias(
                        totalOffsetMillis, isDst, dstOffsetMillis, whenMillis, bias);
        return delegateOffsetResult == null ? null :
                new OffsetResult(delegateOffsetResult.mTimeZone, delegateOffsetResult.mOneMatch);
    }

    /**
     * Returns an immutable, ordered list of time zone mappings for the country in an undefined but
     * "priority" order, filtered so that only "effective" time zone IDs are returned. An
     * "effective" time zone is one that differs from another time zone used in the country after
     * {@code whenMillis}. The list can be empty if there were no zones configured or the configured
     * zone IDs were not recognized.
     *
     * @hide
     */
    @SystemApi
    public List<TimeZoneMapping> getEffectiveTimeZoneMappingsAt(long whenMillis) {
        List<libcore.timezone.CountryTimeZones.TimeZoneMapping> delegateList =
                mDelegate.getEffectiveTimeZoneMappingsAt(whenMillis);

        List<TimeZoneMapping> toReturn = new ArrayList<>(delegateList.size());
        for (libcore.timezone.CountryTimeZones.TimeZoneMapping delegateMapping : delegateList) {
            toReturn.add(new TimeZoneMapping(delegateMapping));
        }
        return Collections.unmodifiableList(toReturn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CountryTimeZones that = (CountryTimeZones) o;
        return mDelegate.equals(that.mDelegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDelegate);
    }

    @Override
    public String toString() {
        return mDelegate.toString();
    }
}
