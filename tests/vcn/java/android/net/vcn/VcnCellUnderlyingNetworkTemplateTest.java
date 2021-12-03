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

import static android.net.vcn.VcnUnderlyingNetworkTemplate.MATCH_ANY;
import static android.net.vcn.VcnUnderlyingNetworkTemplate.MATCH_REQUIRED;
import static android.net.vcn.VcnUnderlyingNetworkTemplate.NETWORK_QUALITY_ANY;
import static android.net.vcn.VcnUnderlyingNetworkTemplate.NETWORK_QUALITY_OK;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class VcnCellUnderlyingNetworkTemplateTest {
    private static final Set<String> ALLOWED_PLMN_IDS = new HashSet<>();
    private static final Set<Integer> ALLOWED_CARRIER_IDS = new HashSet<>();

    // Package private for use in VcnGatewayConnectionConfigTest
    static VcnCellUnderlyingNetworkTemplate getTestNetworkTemplate() {
        return new VcnCellUnderlyingNetworkTemplate.Builder()
                .setNetworkQuality(NETWORK_QUALITY_OK)
                .setNotMeteredMatch(MATCH_ANY)
                .setMatchingOperatorPlmnIds(ALLOWED_PLMN_IDS)
                .setMatchingSpecificCarrierIds(ALLOWED_CARRIER_IDS)
                .setNotRoamingMatch(MATCH_ANY)
                .setOpportunisticMatch(MATCH_REQUIRED)
                .build();
    }

    @Test
    public void testBuilderAndGetters() {
        final VcnCellUnderlyingNetworkTemplate networkPriority = getTestNetworkTemplate();
        assertEquals(NETWORK_QUALITY_OK, networkPriority.getNetworkQuality());
        assertEquals(MATCH_ANY, networkPriority.getNotMeteredMatch());
        assertEquals(ALLOWED_PLMN_IDS, networkPriority.getMatchingOperatorPlmnIds());
        assertEquals(ALLOWED_CARRIER_IDS, networkPriority.getMatchingSpecificCarrierIds());
        assertEquals(MATCH_ANY, networkPriority.getNotRoamingMatch());
        assertEquals(MATCH_REQUIRED, networkPriority.getOpportunisticMatch());
    }

    @Test
    public void testBuilderAndGettersForDefaultValues() {
        final VcnCellUnderlyingNetworkTemplate networkPriority =
                new VcnCellUnderlyingNetworkTemplate.Builder().build();
        assertEquals(NETWORK_QUALITY_ANY, networkPriority.getNetworkQuality());
        assertEquals(MATCH_ANY, networkPriority.getNotMeteredMatch());
        assertEquals(new HashSet<String>(), networkPriority.getMatchingOperatorPlmnIds());
        assertEquals(new HashSet<Integer>(), networkPriority.getMatchingSpecificCarrierIds());
        assertEquals(MATCH_ANY, networkPriority.getNotRoamingMatch());
        assertEquals(MATCH_ANY, networkPriority.getOpportunisticMatch());
    }

    @Test
    public void testPersistableBundle() {
        final VcnCellUnderlyingNetworkTemplate networkPriority = getTestNetworkTemplate();
        assertEquals(
                networkPriority,
                VcnUnderlyingNetworkTemplate.fromPersistableBundle(
                        networkPriority.toPersistableBundle()));
    }
}
