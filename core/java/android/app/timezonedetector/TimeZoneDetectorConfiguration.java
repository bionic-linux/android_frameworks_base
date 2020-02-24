/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.app.timezonedetector;

import android.annotation.NonNull;
import android.annotation.StringDef;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Configuration that controls the behavior of the time zone detector.
 *
 * <p>Configuration consists of the following properties:
 * <ul>
 *     <li>{@code automaticDetectionEnabled} controls whether a device will attempt to determine
 *     the time zone for the user (when enabled) or leave it to the user to choose (when disabled).
 *     </li>
 * </ul>
 * <p>Configuration properties can be present or missing. See {@link #isComplete()} to tell if all
 * properties are present, and {@link #hasProperty(String)} with {@code PROPERTY_} constants for
 * testing individual properties.
 *
 * @hide
 */
public final class TimeZoneDetectorConfiguration implements Parcelable {

    public static final @NonNull Creator<TimeZoneDetectorConfiguration> CREATOR =
            new Creator<TimeZoneDetectorConfiguration>() {
                public TimeZoneDetectorConfiguration createFromParcel(Parcel in) {
                    return TimeZoneDetectorConfiguration.createFromParcel(in);
                }

                public TimeZoneDetectorConfiguration[] newArray(int size) {
                    return new TimeZoneDetectorConfiguration[size];
                }
            };

    /** An enum of configuration properties. */
    @StringDef(PROPERTY_AUTOMATIC_DETECTION_ENABLED)
    @Retention(RetentionPolicy.SOURCE)
    @interface Property {}

    /** See {@link TimeZoneDetectorConfiguration} for details. */
    @Property
    public static final String PROPERTY_AUTOMATIC_DETECTION_ENABLED = "automaticDetectionEnabled";

    private final Bundle mBundle;

    private TimeZoneDetectorConfiguration(Builder builder) {
        this.mBundle = builder.mBundle;
    }

    private static TimeZoneDetectorConfiguration createFromParcel(Parcel in) {
        Bundle bundle = in.readBundle();
        return new TimeZoneDetectorConfiguration.Builder()
                .setPropertyBundleInternal(bundle)
                .build();
    }

    /** Returns {@code true} if all the properties are set. */
    public boolean isComplete() {
        return hasProperty(PROPERTY_AUTOMATIC_DETECTION_ENABLED);
    }

    /** Returns true if the specified property is set. */
    public boolean hasProperty(@Property String property) {
        return mBundle.containsKey(property);
    }

    /**
     * Returns the value of the {@code automaticDetectionEnabled} property.
     *
     * @throws IllegalStateException if the field has not been set
     */
    public boolean isAutomaticDetectionEnabled() {
        if (!mBundle.containsKey(PROPERTY_AUTOMATIC_DETECTION_ENABLED)) {
            throw new IllegalStateException(PROPERTY_AUTOMATIC_DETECTION_ENABLED + " is not set");
        }
        return mBundle.getBoolean(PROPERTY_AUTOMATIC_DETECTION_ENABLED);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBundle(mBundle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeZoneDetectorConfiguration that = (TimeZoneDetectorConfiguration) o;
        return mBundle.kindofEquals(that.mBundle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBundle);
    }

    @Override
    public String toString() {
        return "TimeZoneDetectorConfiguration{"
                + "mBundle=" + mBundle
                + '}';
    }

    /** @hide */
    public static class Builder {

        private Bundle mBundle = new Bundle();

        /**
         * Creates a new Builder with no properties set.
         */
        public Builder() {}

        /**
         * Creates a new Builder by copying properties from an existing instance.
         */
        public Builder(TimeZoneDetectorConfiguration toCopy) {
            this();
            mergeProperties(toCopy);
        }

        /**
         * Merges {@code other} properties into this instances, replacing existings values in this
         * where the properties appear in both.
         */
        public Builder mergeProperties(TimeZoneDetectorConfiguration other) {
            this.mBundle.putAll(other.mBundle);
            return this;
        }

        Builder setPropertyBundleInternal(Bundle bundle) {
            this.mBundle.putAll(bundle);
            return this;
        }

        /** Sets the desired state of automatic time zone detection. */
        public Builder setAutomaticDetectionEnabled(boolean enabled) {
            this.mBundle.putBoolean(PROPERTY_AUTOMATIC_DETECTION_ENABLED, enabled);
            return this;
        }

        /** Returns the {@link TimeZoneDetectorConfiguration}. */
        @NonNull
        public TimeZoneDetectorConfiguration build() {
            return new TimeZoneDetectorConfiguration(this);
        }
    }
}

