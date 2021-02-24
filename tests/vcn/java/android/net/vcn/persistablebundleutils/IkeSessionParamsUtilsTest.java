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

package android.net.vcn.persistablebundleutils;

import static org.junit.Assert.assertEquals;

import android.net.InetAddresses;
import android.net.ipsec.ike.IkeFqdnIdentification;
import android.net.ipsec.ike.IkeIdentification;
import android.net.ipsec.ike.IkeSaProposal;
import android.net.ipsec.ike.IkeSessionParams;
import android.net.ipsec.ike.SaProposal;
import android.os.PersistableBundle;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class IkeSessionParamsUtilsTest {
    private static final InetAddress SERVER_ADDRESS =
            InetAddresses.parseNumericAddress("192.0.2.100");

    private static final IkeSaProposal SA_PROPOSAL =
            new IkeSaProposal.Builder()
                    .addEncryptionAlgorithm(
                            SaProposal.ENCRYPTION_ALGORITHM_3DES, SaProposal.KEY_LEN_UNUSED)
                    .addIntegrityAlgorithm(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96)
                    .addPseudorandomFunction(SaProposal.PSEUDORANDOM_FUNCTION_AES128_XCBC)
                    .addDhGroup(SaProposal.DH_GROUP_1024_BIT_MODP)
                    .build();

    private static final IkeIdentification LOCAL_ID =
            new IkeFqdnIdentification("client.test.android.net");
    private static final IkeIdentification REMOTE_ID =
            new IkeFqdnIdentification("server.test.android.net");

    private static final byte[] PSK = "psk".getBytes();

    private static IkeSessionParams.Builder createBuilderMinimum() {
        return new IkeSessionParams.Builder()
                .setServerHostname(SERVER_ADDRESS.getHostAddress())
                .addSaProposal(SA_PROPOSAL)
                .setLocalIdentification(LOCAL_ID)
                .setRemoteIdentification(REMOTE_ID)
                .setAuthPsk(PSK);
    }

    private static void verifyPersistableBundleEncodeDecodeIsLossless(IkeSessionParams params) {
        final PersistableBundle bundle = IkeSessionParamsUtils.toPersistableBundle(params);
        final IkeSessionParams result = IkeSessionParamsUtils.fromPersistableBundle(bundle);

        assertEquals(result, params);
    }

    @Test
    public void testIkeSessionParamsWithPskEncodeDecodeIsLossLess() throws Exception {
        final IkeSessionParams params = createBuilderMinimum().setAuthPsk(PSK).build();
        verifyPersistableBundleEncodeDecodeIsLossless(params);
    }
}
