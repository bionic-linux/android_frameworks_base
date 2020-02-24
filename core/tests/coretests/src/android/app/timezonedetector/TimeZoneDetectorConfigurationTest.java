/*
 * Copyright 2020 The Android Open Source Project
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

import static android.app.timezonedetector.ParcelableTestSupport.assertRoundTripParcelable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimeZoneDetectorConfigurationTest {

    @Test
    public void testBuilder_copyConstructor() {
        TimeZoneDetectorConfiguration.Builder builder1 =
                new TimeZoneDetectorConfiguration.Builder().setAutomaticDetectionEnabled(true);
        TimeZoneDetectorConfiguration configuration1 = builder1.build();

        TimeZoneDetectorConfiguration configuration2 =
                new TimeZoneDetectorConfiguration.Builder(configuration1).build();

        assertEquals(configuration1, configuration2);
    }

    @Test
    public void testIsComplete() {
        TimeZoneDetectorConfiguration incompleteConfiguration =
                new TimeZoneDetectorConfiguration.Builder()
                        .build();
        assertFalse(incompleteConfiguration.isComplete());

        TimeZoneDetectorConfiguration completeConfiguration =
                new TimeZoneDetectorConfiguration.Builder()
                        .setAutomaticDetectionEnabled(true)
                        .build();
        assertTrue(completeConfiguration.isComplete());
    }

    @Test
    public void testBuilder_mergeProperties() {
        TimeZoneDetectorConfiguration configuration1 =
                new TimeZoneDetectorConfiguration.Builder()
                        .setAutomaticDetectionEnabled(true)
                        .build();

        {
            TimeZoneDetectorConfiguration mergedEmptyAnd1 =
                    new TimeZoneDetectorConfiguration.Builder()
                            .mergeProperties(configuration1)
                            .build();
            assertEquals(configuration1, mergedEmptyAnd1);
        }

        {
            TimeZoneDetectorConfiguration configuration2 =
                    new TimeZoneDetectorConfiguration.Builder()
                            .setAutomaticDetectionEnabled(false)
                            .build();

            // With only one property to merge in, merging configuration2 into configuration1
            // results in a configuration equals() to configuration2.
            TimeZoneDetectorConfiguration merged1And2 =
                    new TimeZoneDetectorConfiguration.Builder(configuration1)
                            .mergeProperties(configuration2)
                            .build();
            assertEquals(configuration2, merged1And2);
        }
    }

    @Test
    public void testEquals() {
        TimeZoneDetectorConfiguration.Builder builder1 =
                new TimeZoneDetectorConfiguration.Builder();
        {
            TimeZoneDetectorConfiguration one = builder1.build();
            assertEquals(one, one);
        }

        TimeZoneDetectorConfiguration.Builder builder2 =
                new TimeZoneDetectorConfiguration.Builder();
        {
            TimeZoneDetectorConfiguration one = builder1.build();
            TimeZoneDetectorConfiguration two = builder2.build();
            assertEquals(one, two);
            assertEquals(two, one);
        }

        builder1.setAutomaticDetectionEnabled(true);
        {
            TimeZoneDetectorConfiguration one = builder1.build();
            TimeZoneDetectorConfiguration two = builder2.build();
            assertNotEquals(one, two);
        }

        builder2.setAutomaticDetectionEnabled(false);
        {
            TimeZoneDetectorConfiguration one = builder1.build();
            TimeZoneDetectorConfiguration two = builder2.build();
            assertNotEquals(one, two);
        }

        builder1.setAutomaticDetectionEnabled(false);
        {
            TimeZoneDetectorConfiguration one = builder1.build();
            TimeZoneDetectorConfiguration two = builder2.build();
            assertEquals(one, two);
        }
    }

    @Test
    public void testParcelable() {
        TimeZoneDetectorConfiguration.Builder builder =
                new TimeZoneDetectorConfiguration.Builder();
        assertRoundTripParcelable(builder.build());

        builder.setAutomaticDetectionEnabled(true);
        assertRoundTripParcelable(builder.build());

        builder.setAutomaticDetectionEnabled(false);
        assertRoundTripParcelable(builder.build());
    }
}
