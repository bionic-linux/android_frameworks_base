/*
 * Copyright (C) 2021 The Android Open Source Project
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
package android.net.vcn;

import static org.junit.Assert.assertEquals;

import android.net.vcn.VcnLinkCriteria.EstimatedBandwidthCriteria;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

public class VcnLinkCriteriaTest {
    // public for use in NetworkPriorityClassiferTest
    public static final int TEST_MIN_UPSTREAM_BANDWIDTH_KBPS = 100;

    // public for use in NetworkPriorityClassiferTest
    public static final int TEST_MIN_DOWNSTREAM_BANDWIDTH_KBPS = 200;

    // public for use in NetworkPriorityClassiferTest
    public static Set<VcnLinkCriteria> getTestLinkCriterion() {
        return Collections.singleton(getTestEstimatedBandwidthCriteria());
    }

    // public for use in NetworkPriorityClassiferTest
    public static EstimatedBandwidthCriteria getTestEstimatedBandwidthCriteria() {
        return new EstimatedBandwidthCriteria(
                TEST_MIN_UPSTREAM_BANDWIDTH_KBPS, TEST_MIN_DOWNSTREAM_BANDWIDTH_KBPS);
    }

    @Test
    public void testEstimatedBandwidthCriteria_fields() {
        final EstimatedBandwidthCriteria criteria = getTestEstimatedBandwidthCriteria();
        assertEquals(TEST_MIN_UPSTREAM_BANDWIDTH_KBPS, criteria.minUpstreamBandwidthKbps);
        assertEquals(TEST_MIN_DOWNSTREAM_BANDWIDTH_KBPS, criteria.minDownstreamBandwidthKbps);
    }

    @Test
    public void testEstimatedBandwidthCriteria_persistableBundle() {
        final EstimatedBandwidthCriteria criteria = getTestEstimatedBandwidthCriteria();
        assertEquals(criteria, new EstimatedBandwidthCriteria(criteria.toPersistableBundle()));
    }
}
