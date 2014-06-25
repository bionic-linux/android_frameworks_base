/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.text.format;

import android.util.TimeFormatException;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import libcore.util.ZoneInfo;
import libcore.util.ZoneInfoDB;

/**
 * An alternative to the {@link java.util.Calendar} and
 * {@link java.util.GregorianCalendar} classes. An instance of the Time class represents
 * a moment in time, specified with second precision. It is modelled after
 * struct tm. This class is not thread-safe and does not consider leap seconds.
 *
 * <p>This class has a number of issues and it is recommended that
 * {@link java.util.GregorianCalendar} is used instead.
 *
 * <p>Known issues:
 * <ul>
 *     <li>For historical reasons, when performing time calculations all arithmetic takes place
 *     using 32-bit ints. This limits the reliable time range representable from 1902 until 2037.
 *     See <a href="http://en.wikipedia.org/wiki/Year_2038_problem">this</a> for details.</li>
 *     <li>Calling {@link #switchTimezone(String)} on a date that cannot exist, such as a wall time
 *     that was skipped due to a DST transition, will result in a date in 1969 (1 second before 1st
 *     Jan 1970).</li>
 *     <li>Much of the formatting / parsing assumes ASCII text and is therefore not suitable for
 *     use with non-ASCII scripts.</li>
 * </ul>
 */
public class Time {
    private static final String Y_M_D_T_H_M_S_000 = "%Y-%m-%dT%H:%M:%S.000";
    private static final String Y_M_D_T_H_M_S_000_Z = "%Y-%m-%dT%H:%M:%S.000Z";
    private static final String Y_M_D = "%Y-%m-%d";

    public static final String TIMEZONE_UTC = "UTC";

    /**
     * The Julian day of the epoch, that is, January 1, 1970 on the Gregorian
     * calendar.
     */
    public static final int EPOCH_JULIAN_DAY = 2440588;

    /**
     * The Julian day of the Monday in the week of the epoch, December 29, 1969
     * on the Gregorian calendar.
     */
    public static final int MONDAY_BEFORE_JULIAN_EPOCH = EPOCH_JULIAN_DAY - 3;

    /**
     * True if this is an allDay event. The hour, minute, second fields are
     * all zero, and the date is displayed the same in all time zones.
     */
    public boolean allDay;

    /**
     * Seconds [0-61] (2 leap seconds allowed)
     */
    public int second;

    /**
     * Minute [0-59]
     */
    public int minute;

    /**
     * Hour of day [0-23]
     */
    public int hour;

    /**
     * Day of month [1-31]
     */
    public int monthDay;

    /**
     * Month [0-11]
     */
    public int month;

    /**
     * Year. For example, 1970.
     */
    public int year;

    /**
     * Day of week [0-6]
     */
    public int weekDay;

    /**
     * Day of year [0-365]
     */
    public int yearDay;

    /**
     * This time is in daylight savings time. One of:
     * <ul>
     * <li><b>positive</b> - in dst</li>
     * <li><b>0</b> - not in dst</li>
     * <li><b>negative</b> - unknown</li>
     * </ul>
     */
    public int isDst;

    /**
     * Offset in seconds from UTC including any DST offset.
     */
    public long gmtoff;

    /**
     * The timezone for this Time.  Should not be null.
     */
    public String timezone;

    /*
     * Define symbolic constants for accessing the fields in this class. Used in
     * getActualMaximum().
     */
    public static final int SECOND = 1;
    public static final int MINUTE = 2;
    public static final int HOUR = 3;
    public static final int MONTH_DAY = 4;
    public static final int MONTH = 5;
    public static final int YEAR = 6;
    public static final int WEEK_DAY = 7;
    public static final int YEAR_DAY = 8;
    public static final int WEEK_NUM = 9;

    public static final int SUNDAY = 0;
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;

    // An object that is reused for date calculations.
    private TimeCalculator calculator;

    /**
     * Construct a Time object in the timezone named by the string
     * argument "timezone". The time is initialized to Jan 1, 1970.
     * @param timezoneId string containing the timezone to use.
     * @see TimeZone
     */
    public Time(String timezoneId) {
        if (timezoneId == null) {
            throw new NullPointerException("timezoneId is null!");
        }
        initialize(timezoneId);
    }

    /**
     * Construct a Time object in the default timezone. The time is initialized to
     * Jan 1, 1970.
     */
    public Time() {
        initialize(TimeZone.getDefault().getID());
    }

    /**
     * A copy constructor.  Construct a Time object by copying the given
     * Time object.  No normalization occurs.
     *
     * @param other
     */
    public Time(Time other) {
        initialize(other.timezone);
        set(other);
    }

    /** Initialize the Time to 00:00:00 1/1/1970 in the specified timezone. */
    private void initialize(String timezoneId) {
        this.timezone = timezoneId;
        this.year = 1970;
        this.monthDay = 1;
        // Set the daylight-saving indicator to the unknown value -1 so that
        // it will be recomputed.
        this.isDst = -1;

        // A reusable object that performs the date/time calculations.
        calculator = new TimeCalculator(timezoneId);
    }

    /**
     * Ensures the values in each field are in range. For example if the
     * current value of this calendar is March 32, normalize() will convert it
     * to April 1. It also fills in weekDay, yearDay, isDst and gmtoff.
     *
     * <p>
     * If "ignoreDst" is true, then this method sets the "isDst" field to -1
     * (the "unknown" value) before normalizing.  It then computes the
     * correct value for "isDst".
     *
     * <p>
     * See {@link #toMillis(boolean)} for more information about when to
     * use <tt>true</tt> or <tt>false</tt> for "ignoreDst".
     *
     * @return the UTC milliseconds since the epoch
     */
    public long normalize(boolean ignoreDst) {
        /*
            Time t;
            if (!java2time(env, &t, This)) return 0L;
            ACQUIRE_TIMEZONE(This, t)
            int64_t result = t.toMillis(ignoreDst != 0);

            time2java(env, This, t);
            RELEASE_TIMEZONE(This, t)

            return static_cast<jlong>(result);
        */
        calculator.copyFieldsFromTime(this);
        long timeInMillis = calculator.toMillis(ignoreDst);
        calculator.copyFieldsToTime(this);
        return timeInMillis;
    }

    /**
     * Convert this time object so the time represented remains the same, but is
     * instead located in a different timezone. This method automatically calls
     * normalize() in some cases.
     *
     * <p>This method can return incorrect results if the date / time cannot be normalized.
     */
    public void switchTimezone(String timezone) {
        /*
        Time t;
        if (!java2time(env, &t, This)) return;
        ACQUIRE_TIMEZONE(This, t)

        const char* timezone = env->GetStringUTFChars(timezoneObject, NULL);

        t.switchTimezone(timezone);

        time2java(env, This, t);
        env->ReleaseStringUTFChars(timezoneObject, timezone);
        RELEASE_TIMEZONE(This, t)

        // we do this here because there's no point in reallocating the string
        env->SetObjectField(This, g_timezoneField, timezoneObject);
         */
        calculator.copyFieldsFromTime(this);
        calculator.switchTimeZone(timezone);
        calculator.copyFieldsToTime(this);
        this.timezone = timezone;
    }

    private static final int[] DAYS_PER_MONTH = { 31, 28, 31, 30, 31, 30, 31,
            31, 30, 31, 30, 31 };

    /**
     * Return the maximum possible value for the given field given the value of
     * the other fields. Requires that it be normalized for MONTH_DAY and
     * YEAR_DAY.
     * @param field one of the constants for HOUR, MINUTE, SECOND, etc.
     * @return the maximum value for the field.
     */
    public int getActualMaximum(int field) {
        switch (field) {
        case SECOND:
            return 59; // leap seconds, bah humbug
        case MINUTE:
            return 59;
        case HOUR:
            return 23;
        case MONTH_DAY: {
            int n = DAYS_PER_MONTH[this.month];
            if (n != 28) {
                return n;
            } else {
                int y = this.year;
                return ((y % 4) == 0 && ((y % 100) != 0 || (y % 400) == 0)) ? 29 : 28;
            }
        }
        case MONTH:
            return 11;
        case YEAR:
            return 2037;
        case WEEK_DAY:
            return 6;
        case YEAR_DAY: {
            int y = this.year;
            // Year days are numbered from 0, so the last one is usually 364.
            return ((y % 4) == 0 && ((y % 100) != 0 || (y % 400) == 0)) ? 365 : 364;
        }
        case WEEK_NUM:
            throw new RuntimeException("WEEK_NUM not implemented");
        default:
            throw new RuntimeException("bad field=" + field);
        }
    }

    /**
     * Clears all values, setting the timezone to the given timezone. Sets isDst
     * to a negative value to mean "unknown".
     * @param timezoneId the timezone to use.
     */
    public void clear(String timezoneId) {
        if (timezoneId == null) {
            throw new NullPointerException("timezone is null!");
        }
        this.timezone = timezoneId;
        this.allDay = false;
        this.second = 0;
        this.minute = 0;
        this.hour = 0;
        this.monthDay = 0;
        this.month = 0;
        this.year = 0;
        this.weekDay = 0;
        this.yearDay = 0;
        this.gmtoff = 0;
        this.isDst = -1;
    }

    /**
     * Compare two {@code Time} objects and return a negative number if {@code
     * a} is less than {@code b}, a positive number if {@code a} is greater than
     * {@code b}, or 0 if they are equal.
     *
     * @param a first {@code Time} instance to compare
     * @param b second {@code Time} instance to compare
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@link #allDay} is true but {@code
     *             hour}, {@code minute}, and {@code second} are not 0.
     * @return a negative result if {@code a} is earlier, a positive result if
     *         {@code a} is earlier, or 0 if they are equal.
     */
    public static int compare(Time a, Time b) {
        if (a == null) {
            throw new NullPointerException("a == null");
        } else if (b == null) {
            throw new NullPointerException("b == null");
        }
        /*
        // nativeCompare(a, b):
        Time a, b;

        if (!java2time(env, &a, aObject)) return 0;
        ACQUIRE_TIMEZONE(aObject, a)

        if (!java2time(env, &b, bObject)) return 0;
        ACQUIRE_TIMEZONE(bObject, b)

        int result = Time::compare(a, b);

        RELEASE_TIMEZONE(aObject, a)
        RELEASE_TIMEZONE(bObject, b)

        return static_cast<jint>(result);
        */
        a.calculator.copyFieldsFromTime(a);
        b.calculator.copyFieldsFromTime(b);

        return TimeCalculator.compare(a.calculator, b.calculator);
    }

    /**
     * Print the current value given the format string provided. See man
     * strftime for what means what. The final string must be less than 256
     * characters.
     * @param format a string containing the desired format.
     * @return a String containing the current time expressed in the current locale.
     */
    public String format(String format) {
        // format1(String):
        // Probably error in C - would have thrown, not return "".
        calculator.copyFieldsFromTime(this);
        return calculator.format(format);
    }

    /**
     * Return the current time in YYYYMMDDTHHMMSS<tz> format
     */
    @Override
    public String toString() {
        /*
        Time t;
        if (!java2time(env, &t, This)) return env->NewStringUTF("");
        ACQUIRE_TIMEZONE(This, t)

        String8 r = t.toString();

        RELEASE_TIMEZONE(This, t)

        return env->NewStringUTF(r.string());
         */
        // Note: Original code had bug. Does not reset exception so will throw?

        // toString() uses its own calculator rather than the shared one. Otherwise crazy stuff
        // happens during debugging when the debugger calls toString().
        TimeCalculator calculator = new TimeCalculator(this.timezone);
        calculator.copyFieldsFromTime(this);
        return calculator.toStringInternal();
    }

    /**
     * Parses a date-time string in either the RFC 2445 format or an abbreviated
     * format that does not include the "time" field.  For example, all of the
     * following strings are valid:
     *
     * <ul>
     *   <li>"20081013T160000Z"</li>
     *   <li>"20081013T160000"</li>
     *   <li>"20081013"</li>
     * </ul>
     *
     * Returns whether or not the time is in UTC (ends with Z).  If the string
     * ends with "Z" then the timezone is set to UTC.  If the date-time string
     * included only a date and no time field, then the <code>allDay</code>
     * field of this Time class is set to true and the <code>hour</code>,
     * <code>minute</code>, and <code>second</code> fields are set to zero;
     * otherwise (a time field was included in the date-time string)
     * <code>allDay</code> is set to false. The fields <code>weekDay</code>,
     * <code>yearDay</code>, and <code>gmtoff</code> are always set to zero,
     * and the field <code>isDst</code> is set to -1 (unknown).  To set those
     * fields, call {@link #normalize(boolean)} after parsing.
     *
     * To parse a date-time string and convert it to UTC milliseconds, do
     * something like this:
     *
     * <pre>
     *   Time time = new Time();
     *   String date = "20081013T160000Z";
     *   time.parse(date);
     *   long millis = time.normalize(false);
     * </pre>
     *
     * @param s the string to parse
     * @return true if the resulting time value is in UTC time
     * @throws android.util.TimeFormatException if s cannot be parsed.
     */
    public boolean parse(String s) {
        if (s == null) {
            throw new NullPointerException("time string is null");
        }
        if (parse2(s)) {
            timezone = TIMEZONE_UTC;
            return true;
        }
        return false;
    }

    /**
     * Parse a time in the current zone in YYYYMMDDTHHMMSS format.
     */
    private boolean parse2(String s) {
        /*
        jsize len = env->GetStringLength(strObj);
        if (len < 8) {
            jniThrowException(env, "android/util/TimeFormatException",
                              "String too short -- expected at least 8 characters.");
            return JNI_FALSE;
        }
        */
        int len = s.length();
        if (len < 8) {
            throw new TimeFormatException("String is too short: Expected at least 8 characters.");
        }

        /*
        jboolean inUtc = JNI_FALSE;

        ScopedStringChars s(env, strObj);
        */
        boolean inUtc = false;

        /*
        // year
        int n;
        bool thrown = false;
        n = get_char(env, s, 0, 1000, &thrown);
        n += get_char(env, s, 1, 100, &thrown);
        n += get_char(env, s, 2, 10, &thrown);
        n += get_char(env, s, 3, 1, &thrown);
        if (thrown) return JNI_FALSE;
        env->SetIntField(This, g_yearField, n);
        */
        // year
        int n;
        n = getChar(s, 0, 1000);
        n += getChar(s, 1, 100);
        n += getChar(s, 2, 10);
        n += getChar(s, 3, 1);
        year = n;

        /*
        // month
        n = get_char(env, s, 4, 10, &thrown);
        n += get_char(env, s, 5, 1, &thrown);
        n--;
        if (thrown) return JNI_FALSE;
        env->SetIntField(This, g_monField, n);
        */
        // month
        n = getChar(s, 4, 10);
        n += getChar(s, 5, 1);
        n--;
        month = n;

        /*
        // day of month
        n = get_char(env, s, 6, 10, &thrown);
        n += get_char(env, s, 7, 1, &thrown);
        if (thrown) return JNI_FALSE;
        env->SetIntField(This, g_mdayField, n);
        */
        // day of month
        n = getChar(s, 6, 10);
        n += getChar(s, 7, 1);
        monthDay = n;

        /*
        if (len > 8) {
        */
        if (len > 8) {

        /* This check was missing from the C code but would probably fail due to uninitialized
           memory not containing valid characters.
         */
            if (len < 15) {
                throw new TimeFormatException(
                        "String is too short: If there are more than 8 characters there must be at"
                                + " least 15.");
            }

        /*
            // T
            if (!check_char(env, s, 8, 'T')) return JNI_FALSE;
            env->SetBooleanField(This, g_allDayField, JNI_FALSE);
        */
            checkChar(s, 8, 'T');
            allDay = false;

         /*
            // hour
            n = get_char(env, s, 9, 10, &thrown);
            n += get_char(env, s, 10, 1, &thrown);
            if (thrown) return JNI_FALSE;
            env->SetIntField(This, g_hourField, n);
         */
            // hour
            n = getChar(s, 9, 10);
            n += getChar(s, 10, 1);
            hour = n;
         /*
            // min
            n = get_char(env, s, 11, 10, &thrown);
            n += get_char(env, s, 12, 1, &thrown);
            if (thrown) return JNI_FALSE;
            env->SetIntField(This, g_minField, n);
         */
            // min
            n = getChar(s, 11, 10);
            n += getChar(s, 12, 1);
            minute = n;
         /*
            // sec
            n = get_char(env, s, 13, 10, &thrown);
            n += get_char(env, s, 14, 1, &thrown);
            if (thrown) return JNI_FALSE;
            env->SetIntField(This, g_secField, n);
        */
            // sec
            n = getChar(s, 13, 10);
            n += getChar(s, 14, 1);
            second = n;

        /*
            if (len > 15) {
                // Z
                if (!check_char(env, s, 15, 'Z')) return JNI_FALSE;
                inUtc = JNI_TRUE;
            }
        */
            if (len > 15) {
                // Z
                checkChar(s, 15, 'Z');
                inUtc = true;
            }

        /*
        } else {
            env->SetBooleanField(This, g_allDayField, JNI_TRUE);
            env->SetIntField(This, g_hourField, 0);
            env->SetIntField(This, g_minField, 0);
            env->SetIntField(This, g_secField, 0);
            */
        } else {
            allDay = true;
            hour = 0;
            minute = 0;
            second = 0;
        }

        /*
        env->SetIntField(This, g_wdayField, 0);
        env->SetIntField(This, g_ydayField, 0);
        env->SetIntField(This, g_isdstField, -1);
        env->SetLongField(This, g_gmtoffField, 0);
        */
        weekDay = 0;
        yearDay = 0;
        isDst = -1;
        gmtoff = 0;

        /*
        return inUtc;
         */
        return inUtc;
    }

    private void checkChar(String s, int spos, char expected) {
        /*
        jchar c = s[spos];
        if (c != expected) {
            jniThrowExceptionFmt(env, "android/util/TimeFormatException",
                                 "Unexpected character 0x%02x at pos=%d.  Expected %c.",
                                 c, spos, expected);
            return false;
        }
        return true;
        */
        char c = s.charAt(spos);
        if (c != expected) {
            throw new TimeFormatException(String.format(
                    "Unexpected character 0x%02d at pos=%d.  Expected 0x%02d (\'%c\').",
                    (int) c, spos, (int) expected, expected));
        }
    }

    private static int getChar(String s, int spos, int mul) {
        /*
        jchar c = s[spos];
        if (c >= '0' && c <= '9') {
            return (c - '0') * mul;
        } else {
            if (!*thrown) {
                jniThrowExceptionFmt(env, "android/util/TimeFormatException",
                                     "Parse error at pos=%d", spos);
                *thrown = true;
            }
            return 0;
        }
         */
        char c = s.charAt(spos);
        if (Character.isDigit(c)) {
            return Character.getNumericValue(c) * mul;
        } else {
            throw new TimeFormatException("Parse error at pos=" + spos);
        }
    }

    /**
     * Parse a time in RFC 3339 format.  This method also parses simple dates
     * (that is, strings that contain no time or time offset).  For example,
     * all of the following strings are valid:
     *
     * <ul>
     *   <li>"2008-10-13T16:00:00.000Z"</li>
     *   <li>"2008-10-13T16:00:00.000+07:00"</li>
     *   <li>"2008-10-13T16:00:00.000-07:00"</li>
     *   <li>"2008-10-13"</li>
     * </ul>
     *
     * <p>
     * If the string contains a time and time offset, then the time offset will
     * be used to convert the time value to UTC.
     * </p>
     *
     * <p>
     * If the given string contains just a date (with no time field), then
     * the {@link #allDay} field is set to true and the {@link #hour},
     * {@link #minute}, and  {@link #second} fields are set to zero.
     * </p>
     *
     * <p>
     * Returns true if the resulting time value is in UTC time.
     * </p>
     *
     * @param s the string to parse
     * @return true if the resulting time value is in UTC time
     * @throws android.util.TimeFormatException if s cannot be parsed.
     */
     public boolean parse3339(String s) {
         if (s == null) {
             throw new NullPointerException("time string is null");
         }
         if (parse3339_2(s)) {
             timezone = TIMEZONE_UTC;
             return true;
         }
         return false;
     }

     private boolean parse3339_2(String s) {
         /*
        jsize len = env->GetStringLength(strObj);
        if (len < 10) {
            jniThrowException(env, "android/util/TimeFormatException",
                              "String too short --- expected at least 10 characters.");
            return JNI_FALSE;
        }
        */
         int len = s.length();
         if (len < 10) {
             throw new TimeFormatException("String too short --- expected at least 10 characters.");
         }
        /*
        jboolean inUtc = JNI_FALSE;

        ScopedStringChars s(env, strObj);
        */
         boolean inUtc = false;

         /*
        // year
        int n;
        bool thrown = false;
        n = get_char(env, s, 0, 1000, &thrown);
        n += get_char(env, s, 1, 100, &thrown);
        n += get_char(env, s, 2, 10, &thrown);
        n += get_char(env, s, 3, 1, &thrown);
        if (thrown) return JNI_FALSE;
        env->SetIntField(This, g_yearField, n);
        */
         // year
         int n;
         n = getChar(s, 0, 1000);
         n += getChar(s, 1, 100);
         n += getChar(s, 2, 10);
         n += getChar(s, 3, 1);
         year = n;
        /*
        // -
        if (!check_char(env, s, 4, '-')) return JNI_FALSE;
        */
         checkChar(s, 4, '-');
        /*
        // month
        n = get_char(env, s, 5, 10, &thrown);
        n += get_char(env, s, 6, 1, &thrown);
        --n;
        if (thrown) return JNI_FALSE;
        env->SetIntField(This, g_monField, n);
        */
         // month
         n = getChar(s, 5, 10);
         n += getChar(s, 6, 1);
         --n;
         month = n;

         /*
        // -
        if (!check_char(env, s, 7, '-')) return JNI_FALSE;
        */
         checkChar(s, 7, '-');

        /*
        // day
        n = get_char(env, s, 8, 10, &thrown);
        n += get_char(env, s, 9, 1, &thrown);
        if (thrown) return JNI_FALSE;
        env->SetIntField(This, g_mdayField, n);
        */
         n = getChar(s, 8, 10);
         n += getChar(s, 9, 1);
         monthDay = n;

         /*
        if (len >= 19) {
        */
         if (len >= 19) {
             /*
            // T
            if (!check_char(env, s, 10, 'T')) return JNI_FALSE;
            env->SetBooleanField(This, g_allDayField, JNI_FALSE);
            */
             // T
             checkChar(s, 10, 'T');
             allDay = false;

             /*
            // hour
            n = get_char(env, s, 11, 10, &thrown);
            n += get_char(env, s, 12, 1, &thrown);
            if (thrown) return JNI_FALSE;
            int hour = n;
            // env->SetIntField(This, g_hourField, n);
            */
             n = getChar(s, 11, 10);
             n += getChar(s, 12, 1);
             int hour = n;
             // Note the commenting in original code. hour not set.

             /*
            // :
            if (!check_char(env, s, 13, ':')) return JNI_FALSE;
            */
             checkChar(s, 13, ':');

            /*
            // minute
            n = get_char(env, s, 14, 10, &thrown);
            n += get_char(env, s, 15, 1, &thrown);
            if (thrown) return JNI_FALSE;
            int minute = n;
            // env->SetIntField(This, g_minField, n);
            */
             // minute
             n = getChar(s, 14, 10);
             n += getChar(s, 15, 1);
             int minute = n;
             // Note the commenting in original code. minute not set.

             /*
            // :
            if (!check_char(env, s, 16, ':')) return JNI_FALSE;
            */
             checkChar(s, 16, ':');

             /*
            // second
            n = get_char(env, s, 17, 10, &thrown);
            n += get_char(env, s, 18, 1, &thrown);
            if (thrown) return JNI_FALSE;
            env->SetIntField(This, g_secField, n);
            */
             n = getChar(s, 17, 10);
             n += getChar(s, 18, 1);
             second = n;

             /*
            int tz_index = 19;
            if (tz_index < len && s[tz_index] == '.') {
                do {
                    tz_index++;
                } while (tz_index < len
                    && s[tz_index] >= '0'
                    && s[tz_index] <= '9');
            }
            */
             // skip the '.XYZ' -- we don't care about subsecond precision.

             int tzIndex = 19;
             if (tzIndex < len && s.charAt(tzIndex) == '.') {
                 do {
                     tzIndex++;
                 } while (tzIndex < len && Character.isDigit(s.charAt(tzIndex)));
             }

             /*
            int offset = 0;
            if (len > tz_index) {
            */
             int offset = 0;
             if (len > tzIndex) {
                 /*
                char c = s[tz_index];

                // NOTE: the offset is meant to be subtracted to get from local time
                // to UTC.  we therefore use 1 for '-' and -1 for '+'.
                switch (c) {
                case 'Z':
                    // Zulu time -- UTC
                    offset = 0;
                    break;
                case '-':
                    offset = 1;
                    break;
                case '+':
                    offset = -1;
                    break;
                default:
                    jniThrowExceptionFmt(env, "android/util/TimeFormatException",
                                         "Unexpected character 0x%02x at position %d.  Expected + or -",
                                         c, tz_index);
                    return JNI_FALSE;
                }
                inUtc = JNI_TRUE;
                  */
                 char c = s.charAt(tzIndex);
                 // NOTE: the offset is meant to be subtracted to get from local time
                 // to UTC.  we therefore use 1 for '-' and -1 for '+'.
                 switch (c) {
                     case 'Z':
                         // Zulu time -- UTC
                         offset = 0;
                         break;
                     case '-':
                         offset = 1;
                         break;
                     case '+':
                         offset = -1;
                         break;
                     default:
                         throw new TimeFormatException(String.format(
                                 "Unexpected character 0x%02d at position %d.  Expected + or -",
                                 (int) c, tzIndex));
                 }
                 inUtc = true;

                 /*
                if (offset != 0) {
                */
                 if (offset != 0) {
                     /*
                    if (len < tz_index + 6) {
                        jniThrowExceptionFmt(env, "android/util/TimeFormatException",
                                             "Unexpected length; should be %d characters",
                                             tz_index + 6);
                        return JNI_FALSE;
                    }
                    */
                     if (len < tzIndex + 6) {
                         throw new TimeFormatException(
                                 String.format("Unexpected length; should be %d characters",
                                         tzIndex + 6));
                     }

                     /*
                    // hour
                    n = get_char(env, s, tz_index + 1, 10, &thrown);
                    n += get_char(env, s, tz_index + 2, 1, &thrown);
                    if (thrown) return JNI_FALSE;
                    n *= offset;
                    hour += n;
                    */
                     // hour
                     n = getChar(s, tzIndex + 1, 10);
                     n += getChar(s, tzIndex + 2, 1);
                     n *= offset;
                     hour += n;

                     /*
                    // :
                    if (!check_char(env, s, tz_index + 3, ':')) return JNI_FALSE;
                    */
                     checkChar(s, tzIndex + 3, ':');
                    /*
                    // minute
                    n = get_char(env, s, tz_index + 4, 10, &thrown);
                    n += get_char(env, s, tz_index + 5, 1, &thrown);
                    if (thrown) return JNI_FALSE;
                    n *= offset;
                    minute += n;
                    */
                     n = getChar(s, tzIndex + 4, 10);
                     n += getChar(s, tzIndex + 5, 1);
                     n *= offset;
                     minute += n;
                     /*
                }
            }
            */
                 }
             }
             /*
            env->SetIntField(This, g_hourField, hour);
            env->SetIntField(This, g_minField, minute);
            */
             this.hour = hour;
             this.minute = minute;

//            if (offset != 0) {
//                // we need to normalize after applying the hour and minute offsets
//                android_text_format_Time_normalize(env, This, false /* use isdst */);
//             // The timezone is set to UTC in the calling Java code.
//             }
             if (offset != 0) {
                 normalize(false);
                 // The timezone is set to UTC in the calling Java code.
             }
             /*
        } else {
                env->SetBooleanField(This, g_allDayField, JNI_TRUE);
                env->SetIntField(This, g_hourField, 0);
                env->SetIntField(This, g_minField, 0);
                env->SetIntField(This, g_secField, 0);
        }
        */
         } else {
             allDay = true;
             this.hour = 0;
             this.minute = 0;
             this.second = 0;
         }

         /*
        env->SetIntField(This, g_wdayField, 0);
        env->SetIntField(This, g_ydayField, 0);
        env->SetIntField(This, g_isdstField, -1);
        env->SetLongField(This, g_gmtoffField, 0);
        */
         this.weekDay = 0;
         this.yearDay = 0;
         this.isDst = -1;
         this.gmtoff = 0;

         /*
        return inUtc;
        */
         return inUtc;
     }

    /**
     * Returns the timezone string that is currently set for the device.
     */
    public static String getCurrentTimezone() {
        return TimeZone.getDefault().getID();
    }

    /**
     * Sets the time of the given Time object to the current time.
     */
    public void setToNow() {
        /*
        env->SetBooleanField(This, g_allDayField, JNI_FALSE);
        Time t;
        ACQUIRE_TIMEZONE(This, t)

        t.setToNow();

        time2java(env, This, t);
        RELEASE_TIMEZONE(This, t)
         */
        set(System.currentTimeMillis());
    }

    /**
     * Converts this time to milliseconds. Suitable for interacting with the
     * standard java libraries. The time is in UTC milliseconds since the epoch.
     * This does an implicit normalization to compute the milliseconds but does
     * <em>not</em> change any of the fields in this Time object.  If you want
     * to normalize the fields in this Time object and also get the milliseconds
     * then use {@link #normalize(boolean)}.
     *
     * <p>
     * If "ignoreDst" is false, then this method uses the current setting of the
     * "isDst" field and will adjust the returned time if the "isDst" field is
     * wrong for the given time.  See the sample code below for an example of
     * this.
     *
     * <p>
     * If "ignoreDst" is true, then this method ignores the current setting of
     * the "isDst" field in this Time object and will instead figure out the
     * correct value of "isDst" (as best it can) from the fields in this
     * Time object.  The only case where this method cannot figure out the
     * correct value of the "isDst" field is when the time is inherently
     * ambiguous because it falls in the hour that is repeated when switching
     * from Daylight-Saving Time to Standard Time.
     *
     * <p>
     * Here is an example where <tt>toMillis(true)</tt> adjusts the time,
     * assuming that DST changes at 2am on Sunday, Nov 4, 2007.
     *
     * <pre>
     * Time time = new Time();
     * time.set(4, 10, 2007);  // set the date to Nov 4, 2007, 12am
     * time.normalize(false);       // this sets isDst = 1
     * time.monthDay += 1;     // changes the date to Nov 5, 2007, 12am
     * millis = time.toMillis(false);   // millis is Nov 4, 2007, 11pm
     * millis = time.toMillis(true);    // millis is Nov 5, 2007, 12am
     * </pre>
     *
     * <p>
     * To avoid this problem, use <tt>toMillis(true)</tt>
     * after adding or subtracting days or explicitly setting the "monthDay"
     * field.  On the other hand, if you are adding
     * or subtracting hours or minutes, then you should use
     * <tt>toMillis(false)</tt>.
     *
     * <p>
     * You should also use <tt>toMillis(false)</tt> if you want
     * to read back the same milliseconds that you set with {@link #set(long)}
     * or {@link #set(Time)} or after parsing a date string.
     */
    public long toMillis(boolean ignoreDst) {
        /*
        Time t;
        if (!java2time(env, &t, This)) return 0L;
        ACQUIRE_TIMEZONE(This, t)

        int64_t result = t.toMillis(ignoreDst != 0);

        RELEASE_TIMEZONE(This, t)

        return static_cast<jlong>(result);
         */
        calculator.copyFieldsFromTime(this);
        return calculator.toMillis(ignoreDst);
    }

    /**
     * Sets the fields in this Time object given the UTC milliseconds.  After
     * this method returns, all the fields are normalized.
     * This also sets the "isDst" field to the correct value.
     *
     * @param millis the time in UTC milliseconds since the epoch.
     */
    public void set(long millis) {
        /*
        env->SetBooleanField(This, g_allDayField, JNI_FALSE);
        Time t;
        ACQUIRE_TIMEZONE(This, t)

        t.set(millis);

        time2java(env, This, t);
        RELEASE_TIMEZONE(This, t)
         */
        allDay = false;
        calculator.timezone = timezone;
        calculator.setTimeInMillis(millis);
        calculator.copyFieldsToTime(this);
    }

    /**
     * Format according to RFC 2445 DATETIME type.
     *
     * <p>
     * The same as format("%Y%m%dT%H%M%S").
     */
    public String format2445() {
        /*
        Time t;
        if (!java2time(env, &t, This)) return env->NewStringUTF("");
        bool allDay = env->GetBooleanField(This, g_allDayField);

        if (!allDay) {
            ACQUIRE_TIMEZONE(This, t)
            bool inUtc = strcmp("UTC", t.timezone) == 0;
            short buf[16];
            t.format2445(buf, true);
            RELEASE_TIMEZONE(This, t)
            if (inUtc) {
                // The letter 'Z' is appended to the end so allow for one
                // more character in the buffer.
                return env->NewString((jchar*)buf, 16);
            } else {
                return env->NewString((jchar*)buf, 15);
            }
        } else {
            short buf[8];
            t.format2445(buf, false);
            return env->NewString((jchar*)buf, 8);
        }
        */
        calculator.copyFieldsFromTime(this);
        // Some of the allDay logic moved to TimeCalculator.format2445(). It returns a string
        // instead of the char[] and does not preallocate the buffer.
        return calculator.format2445(!allDay);
    }

    /**
     * Copy the value of that to this Time object. No normalization happens.
     */
    public void set(Time that) {
        this.timezone = that.timezone;
        this.allDay = that.allDay;
        this.second = that.second;
        this.minute = that.minute;
        this.hour = that.hour;
        this.monthDay = that.monthDay;
        this.month = that.month;
        this.year = that.year;
        this.weekDay = that.weekDay;
        this.yearDay = that.yearDay;
        this.isDst = that.isDst;
        this.gmtoff = that.gmtoff;
    }

    /**
     * Sets the fields. Sets weekDay, yearDay and gmtoff to 0, and isDst to -1.
     * Call {@link #normalize(boolean)} if you need those.
     */
    public void set(int second, int minute, int hour, int monthDay, int month, int year) {
        this.allDay = false;
        this.second = second;
        this.minute = minute;
        this.hour = hour;
        this.monthDay = monthDay;
        this.month = month;
        this.year = year;
        this.weekDay = 0;
        this.yearDay = 0;
        this.isDst = -1;
        this.gmtoff = 0;
    }

    /**
     * Sets the date from the given fields.  Also sets allDay to true.
     * Sets weekDay, yearDay and gmtoff to 0, and isDst to -1.
     * Call {@link #normalize(boolean)} if you need those.
     *
     * @param monthDay the day of the month (in the range [1,31])
     * @param month the zero-based month number (in the range [0,11])
     * @param year the year
     */
    public void set(int monthDay, int month, int year) {
        this.allDay = true;
        this.second = 0;
        this.minute = 0;
        this.hour = 0;
        this.monthDay = monthDay;
        this.month = month;
        this.year = year;
        this.weekDay = 0;
        this.yearDay = 0;
        this.isDst = -1;
        this.gmtoff = 0;
    }

    /**
     * Returns true if the time represented by this Time object occurs before
     * the given time.
     *
     * @param that a given Time object to compare against
     * @return true if this time is less than the given time
     */
    public boolean before(Time that) {
        return Time.compare(this, that) < 0;
    }


    /**
     * Returns true if the time represented by this Time object occurs after
     * the given time.
     *
     * @param that a given Time object to compare against
     * @return true if this time is greater than the given time
     */
    public boolean after(Time that) {
        return Time.compare(this, that) > 0;
    }

    /**
     * This array is indexed by the weekDay field (SUNDAY=0, MONDAY=1, etc.)
     * and gives a number that can be added to the yearDay to give the
     * closest Thursday yearDay.
     */
    private static final int[] sThursdayOffset = { -3, 3, 2, 1, 0, -1, -2 };

    /**
     * Computes the week number according to ISO 8601.  The current Time
     * object must already be normalized because this method uses the
     * yearDay and weekDay fields.
     *
     * <p>
     * In IS0 8601, weeks start on Monday.
     * The first week of the year (week 1) is defined by ISO 8601 as the
     * first week with four or more of its days in the starting year.
     * Or equivalently, the week containing January 4.  Or equivalently,
     * the week with the year's first Thursday in it.
     * </p>
     *
     * <p>
     * The week number can be calculated by counting Thursdays.  Week N
     * contains the Nth Thursday of the year.
     * </p>
     *
     * @return the ISO week number.
     */
    public int getWeekNumber() {
        // Get the year day for the closest Thursday
        int closestThursday = yearDay + sThursdayOffset[weekDay];

        // Year days start at 0
        if (closestThursday >= 0 && closestThursday <= 364) {
            return closestThursday / 7 + 1;
        }

        // The week crosses a year boundary.
        Time temp = new Time(this);
        temp.monthDay += sThursdayOffset[weekDay];
        temp.normalize(true /* ignore isDst */);
        return temp.yearDay / 7 + 1;
    }

    /**
     * Return a string in the RFC 3339 format.
     * <p>
     * If allDay is true, expresses the time as Y-M-D</p>
     * <p>
     * Otherwise, if the timezone is UTC, expresses the time as Y-M-D-T-H-M-S UTC</p>
     * <p>
     * Otherwise the time is expressed the time as Y-M-D-T-H-M-S +- GMT</p>
     * @return string in the RFC 3339 format.
     */
    public String format3339(boolean allDay) {
        if (allDay) {
            return format(Y_M_D);
        } else if (TIMEZONE_UTC.equals(timezone)) {
            return format(Y_M_D_T_H_M_S_000_Z);
        } else {
            String base = format(Y_M_D_T_H_M_S_000);
            String sign = (gmtoff < 0) ? "-" : "+";
            int offset = (int) Math.abs(gmtoff);
            int minutes = (offset % 3600) / 60;
            int hours = offset / 3600;

            return String.format(Locale.US, "%s%s%02d:%02d", base, sign, hours, minutes);
        }
    }

    /**
     * Returns true if the day of the given time is the epoch on the Julian Calendar
     * (January 1, 1970 on the Gregorian calendar).
     *
     * @param time the time to test
     * @return true if epoch.
     */
    public static boolean isEpoch(Time time) {
        long millis = time.toMillis(true);
        return getJulianDay(millis, 0) == EPOCH_JULIAN_DAY;
    }

    /**
     * Computes the Julian day number for a point in time in a particular
     * timezone. The Julian day for a given date is the same for every
     * timezone. For example, the Julian day for July 1, 2008 is 2454649.
     *
     * <p>Callers must pass the time in UTC millisecond (as can be returned
     * by {@link #toMillis(boolean)} or {@link #normalize(boolean)})
     * and the offset from UTC of the timezone in seconds (as might be in
     * {@link #gmtoff}).
     *
     * <p>The Julian day is useful for testing if two events occur on the
     * same calendar date and for determining the relative time of an event
     * from the present ("yesterday", "3 days ago", etc.).
     *
     * @param millis the time in UTC milliseconds
     * @param gmtoff the offset from UTC in seconds
     * @return the Julian day
     */
    public static int getJulianDay(long millis, long gmtoff) {
        long offsetMillis = gmtoff * 1000;
        long julianDay = (millis + offsetMillis) / DateUtils.DAY_IN_MILLIS;
        return (int) julianDay + EPOCH_JULIAN_DAY;
    }

    /**
     * <p>Sets the time from the given Julian day number, which must be based on
     * the same timezone that is set in this Time object.  The "gmtoff" field
     * need not be initialized because the given Julian day may have a different
     * GMT offset than whatever is currently stored in this Time object anyway.
     * After this method returns all the fields will be normalized and the time
     * will be set to 12am at the beginning of the given Julian day.
     * </p>
     *
     * <p>
     * The only exception to this is if 12am does not exist for that day because
     * of daylight saving time.  For example, Cairo, Eqypt moves time ahead one
     * hour at 12am on April 25, 2008 and there are a few other places that
     * also change daylight saving time at 12am.  In those cases, the time
     * will be set to 1am.
     * </p>
     *
     * @param julianDay the Julian day in the timezone for this Time object
     * @return the UTC milliseconds for the beginning of the Julian day
     */
    public long setJulianDay(int julianDay) {
        // Don't bother with the GMT offset since we don't know the correct
        // value for the given Julian day.  Just get close and then adjust
        // the day.
        long millis = (julianDay - EPOCH_JULIAN_DAY) * DateUtils.DAY_IN_MILLIS;
        set(millis);

        // Figure out how close we are to the requested Julian day.
        // We can't be off by more than a day.
        int approximateDay = getJulianDay(millis, gmtoff);
        int diff = julianDay - approximateDay;
        monthDay += diff;

        // Set the time to 12am and re-normalize.
        hour = 0;
        minute = 0;
        second = 0;
        millis = normalize(true);
        return millis;
    }

    /**
     * Returns the week since {@link #EPOCH_JULIAN_DAY} (Jan 1, 1970) adjusted
     * for first day of week. This takes a julian day and the week start day and
     * calculates which week since {@link #EPOCH_JULIAN_DAY} that day occurs in,
     * starting at 0. *Do not* use this to compute the ISO week number for the
     * year.
     *
     * @param julianDay The julian day to calculate the week number for
     * @param firstDayOfWeek Which week day is the first day of the week, see
     *            {@link #SUNDAY}
     * @return Weeks since the epoch
     */
    public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
        int diff = THURSDAY - firstDayOfWeek;
        if (diff < 0) {
            diff += 7;
        }
        int refDay = EPOCH_JULIAN_DAY - diff;
        return (julianDay - refDay) / 7;
    }

    /**
     * Takes a number of weeks since the epoch and calculates the Julian day of
     * the Monday for that week. This assumes that the week containing the
     * {@link #EPOCH_JULIAN_DAY} is considered week 0. It returns the Julian day
     * for the Monday week weeks after the Monday of the week containing the
     * epoch.
     *
     * @param week Number of weeks since the epoch
     * @return The julian day for the Monday of the given week since the epoch
     */
    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        return MONDAY_BEFORE_JULIAN_EPOCH + week * 7;
    }

    /**
     * A class that handles date/time calculations.
     *
     * This class originated as a port of a native C++ class ("android.Time") to pure Java. It is
     * separate from the enclosing class because some methods copy the result of calculations back
     * to the enclosing object, but others do not: thus separate state is retained.
     */
    private static class TimeCalculator {
        public final ZoneInfo.WallTime wallTime;
        public String timezone;

        // Information about the current timezone.
        private ZoneInfo zoneInfo;

        public TimeCalculator(String timezoneId) {
            this.zoneInfo = lookupZoneInfo(timezoneId);
            this.wallTime = new ZoneInfo.WallTime();
        }

        public long toMillis(boolean ignoreDst) {
            /*
            if (ignoreDst) {
                this->t.tm_isdst = -1;
            }
            */
            if (ignoreDst) {
                wallTime.setIsDst(-1);
            }

            /*
            int64_t r = mktime_tz(&(this->t), this->timezone);
            if (r == -1)
                return -1;
            return r * 1000;
            */

            int r = wallTime.mktime(zoneInfo);
            if (r == -1) {
                return -1;
            }
            return r * 1000L;
        }

        public void setTimeInMillis(long millis) {
            /*
            time_t seconds = millis / 1000;
            localtime_tz(&seconds, &(this->t), this->timezone);
            */

            // Preserve old 32-bit Android behavior.
            int intSeconds = (int) (millis / 1000);

            updateZoneInfoFromTimeZone();
            wallTime.localtime(intSeconds, zoneInfo);
        }

        public String format(String format) {
            /*
            char buf[257];
            int n = strftime_tz(buf, 257, format, &(this->t), locale);
            if (n > 0) {
                return String8(buf);
            } else {
                return String8();
            }
            */
            if (format == null) {
                format = "%c";
            }
            TimeFormatter formatter = new TimeFormatter();
            return formatter.format(format, wallTime, zoneInfo);
        }

        private void updateZoneInfoFromTimeZone() {
            if (!zoneInfo.getID().equals(timezone)) {
                this.zoneInfo = lookupZoneInfo(timezone);
            }
        }

        private static ZoneInfo lookupZoneInfo(String timezoneId) {
            try {
                ZoneInfo zoneInfo = ZoneInfoDB.getInstance().makeTimeZone(timezoneId);
                if (zoneInfo == null) {
                    zoneInfo = ZoneInfoDB.getInstance().makeTimeZone("GMT");
                }
                if (zoneInfo == null) {
                    throw new IllegalStateException("GMT not found: \"" + timezoneId + "\"");
                }
                return zoneInfo;
            } catch (IOException e) {
                // This should not ever be thrown.
                throw new IllegalStateException(
                        "Error loading timezone: \"" + timezoneId + "\"", e);
            }
        }

        public void switchTimeZone(String timezone) {
            /*
            time_t seconds = mktime_tz(&(this->t), this->timezone);
            localtime_tz(&seconds, &(this->t), timezone);
            */
            int seconds = wallTime.mktime(zoneInfo);
            this.timezone = timezone;
            updateZoneInfoFromTimeZone();
            wallTime.localtime(seconds, zoneInfo);
        }

        public String format2445(boolean hasTime) {
            char[] buf = new char[hasTime ? 16 : 8];
            int n = wallTime.getYear();

            buf[0] = toChar(n / 1000);
            n %= 1000;
            buf[1] = toChar(n / 100);
            n %= 100;
            buf[2] = toChar(n / 10);
            n %= 10;
            buf[3] = toChar(n);

            n = wallTime.getMonth() + 1;
            buf[4] = toChar(n / 10);
            buf[5] = toChar(n % 10);

            n = wallTime.getMonthDay();
            buf[6] = toChar(n / 10);
            buf[7] = toChar(n % 10);

            if (!hasTime) {
                return new String(buf, 0, 8);
            }

            buf[8] = 'T';

            n = wallTime.getHour();
            buf[9] = toChar(n / 10);
            buf[10] = toChar(n % 10);

            n = wallTime.getMinute();
            buf[11] = toChar(n / 10);
            buf[12] = toChar(n % 10);

            n = wallTime.getSecond();
            buf[13] = toChar(n / 10);
            buf[14] = toChar(n % 10);

            if (TIMEZONE_UTC.equals(timezone)) {
                // The letter 'Z' is appended to the end.
                buf[15] = 'Z';
                return new String(buf, 0, 16);
            } else {
                return new String(buf, 0, 15);
            }
        }

        private char toChar(int n) {
            return (n >= 0 && n <= 9) ? (char) (n + '0') : ' ';
        }

        /**
         * A method that will return the state of this object in string form. Note: it has side
         * effects and so has deliberately not been made the default {@link #toString()}.
         */
        public String toStringInternal() {
//            String8 str;
//            char* s = str.lockBuffer(150);
//            #ifdef HAVE_TM_GMTOFF
//                long tm_gmtoff = t.tm_gmtoff;
//            #else
//                long tm_gmtoff = 0;
//            #endif
//            sprintf(s, "%04d%02d%02dT%02d%02d%02d%s(%d,%d,%ld,%d,%d)",
//                    t.tm_year+1900, t.tm_mon+1, t.tm_mday, t.tm_hour, t.tm_min,
//                    t.tm_sec, timezone, t.tm_wday, t.tm_yday, tm_gmtoff, t.tm_isdst,
//                    (int)(((Time*)this)->toMillis(false /* use isDst */)/1000));
//            str.unlockBuffer();
//            return str;

            // This implementation possibly displays the un-normalized fields because that is
            // what it has always done.
            return String.format("%04d%02d%02dT%02d%02d%02d%s(%d,%d,%d,%d,%d)",
                    wallTime.getYear(),
                    wallTime.getMonth() + 1,
                    wallTime.getMonthDay(),
                    wallTime.getHour(),
                    wallTime.getMinute(),
                    wallTime.getSecond(),
                    timezone,
                    wallTime.getWeekDay(),
                    wallTime.getYearDay(),
                    wallTime.getGmtOffset(),
                    wallTime.getIsDst(),
                    toMillis(false /* use isDst */) / 1000
            );

        }

        public static int compare(TimeCalculator aObject, TimeCalculator bObject) {
            if (aObject.timezone.equals(bObject.timezone)) {
                // if the timezones are the same, we can easily compare the two
                // times.  Otherwise, convert to milliseconds and compare that.
                // This requires that object be normalized.
                int diff = aObject.wallTime.getYear() - bObject.wallTime.getYear();
                if (diff != 0) {
                    return diff;
                }

                diff = aObject.wallTime.getMonth() - bObject.wallTime.getMonth();
                if (diff != 0) {
                    return diff;
                }

                diff = aObject.wallTime.getMonthDay() - bObject.wallTime.getMonthDay();
                if (diff != 0) {
                    return diff;
                }

                diff = aObject.wallTime.getHour() - bObject.wallTime.getHour();
                if (diff != 0) {
                    return diff;
                }

                diff = aObject.wallTime.getMinute() - bObject.wallTime.getMinute();
                if (diff != 0) {
                    return diff;
                }

                diff = aObject.wallTime.getSecond() - bObject.wallTime.getSecond();
                if (diff != 0) {
                    return diff;
                }

                return 0;
            } else {
                // Note: For dates that do not exist: toMillis() can return -1, which is a valid
                // time.
                long am = aObject.toMillis(false /* use isDst */);
                long bm = bObject.toMillis(false /* use isDst */);
                long diff = am - bm;
                return (diff < 0) ? -1 : ((diff > 0) ? 1 : 0);
            }

        }

        // time2java
        public void copyFieldsToTime(Time time) {
            /*
            env->SetIntField(o, g_secField, t.t.tm_sec);
            env->SetIntField(o, g_minField, t.t.tm_min);
            env->SetIntField(o, g_hourField, t.t.tm_hour);
            env->SetIntField(o, g_mdayField, t.t.tm_mday);
            env->SetIntField(o, g_monField, t.t.tm_mon);
            env->SetIntField(o, g_yearField, t.t.tm_year+1900);
            env->SetIntField(o, g_wdayField, t.t.tm_wday);
            env->SetIntField(o, g_ydayField, t.t.tm_yday);
            env->SetIntField(o, g_isdstField, t.t.tm_isdst);
            env->SetLongField(o, g_gmtoffField, t.t.tm_gmtoff);
            */
            time.second = wallTime.getSecond();
            time.minute = wallTime.getMinute();
            time.hour = wallTime.getHour();
            time.monthDay = wallTime.getMonthDay();
            time.month = wallTime.getMonth();
            time.year = wallTime.getYear();

            // Read-only fields that are derived from other information above.
            time.weekDay = wallTime.getWeekDay();
            time.yearDay = wallTime.getYearDay();

            // < 0: DST status unknown, 0: is not in DST, 1: is in DST
            time.isDst = wallTime.getIsDst();
            // This is in seconds and includes any DST offset too.
            time.gmtoff = wallTime.getGmtOffset();
        }

        // java2time
        // + ACQUIRE_TIMEZONE
        public void copyFieldsFromTime(Time time) {
            /*
            t->t.tm_sec = env->GetIntField(o, g_secField);
            t->t.tm_min = env->GetIntField(o, g_minField);
            t->t.tm_hour = env->GetIntField(o, g_hourField);
            t->t.tm_mday = env->GetIntField(o, g_mdayField);
            t->t.tm_mon = env->GetIntField(o, g_monField);
            t->t.tm_year = (env->GetIntField(o, g_yearField))-1900;
            t->t.tm_wday = env->GetIntField(o, g_wdayField);
            t->t.tm_yday = env->GetIntField(o, g_ydayField);
            t->t.tm_isdst = env->GetIntField(o, g_isdstField);
            t->t.tm_gmtoff = env->GetLongField(o, g_gmtoffField);
            */
            wallTime.setSecond(time.second);
            wallTime.setMinute(time.minute);
            wallTime.setHour(time.hour);
            wallTime.setMonthDay(time.monthDay);
            wallTime.setMonth(time.month);
            wallTime.setYear(time.year);
            wallTime.setWeekDay(time.weekDay);
            wallTime.setYearDay(time.yearDay);
            wallTime.setIsDst(time.isDst);
            wallTime.setGmtOffset((int) time.gmtoff);

            /*
            bool allDay = env->GetBooleanField(o, g_allDayField);
            if (allDay &&
               ((t->t.tm_sec !=0) || (t->t.tm_min != 0) || (t->t.tm_hour != 0))) {
                jniThrowException(env, "java/lang/IllegalArgumentException",
                                  "allDay is true but sec, min, hour are not 0.");
                return false;
            }
            return true;
             */
            if (time.allDay && (time.second != 0 || time.minute != 0 || time.hour != 0)) {
                throw new IllegalArgumentException("allDay is true but sec, min, hour are not 0.");
            }

            // ACQUIRE_TIMEZONE
            /*
            jstring timezoneString_##This \
            = (jstring) env->GetObjectField(This, g_timezoneField); \
            t.timezone = env->GetStringUTFChars(timezoneString_##This, NULL);
            */
            timezone = time.timezone;
            updateZoneInfoFromTimeZone();
        }
    }
}
