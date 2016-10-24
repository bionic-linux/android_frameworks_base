/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.net.wifi.hotspot2;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.wifi.FakeKeys;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSP;
import android.test.suitebuilder.annotation.SmallTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.junit.Test;

/**
 * Unit tests for {@link android.net.wifi.hotspot2.ConfigBuilder}.
 */
@SmallTest
public class ConfigBuilderTest {
    /**
     * Hotspot 2.0 Release 1 installation file that contains a Passpoint profile and a
     * CA (Certificate Authority) X.509 certificate {@link FakeKeys#CA_CERT0}.
     */
    private static final String PASSPOINT_INSTALLATION_FILE = "assets/HSR1ProfileWithCACert.conf";

    /**
     * Read the content of the given resource file into a String.
     *
     * @param filename String name of the file
     * @return String
     * @throws IOException
     */
    private String loadResourceFile(String filename) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        return builder.toString();
    }

    /**
     * Generate a {@link PasspointConfiguration} that matches the configuration specified in the
     * XML file {@link #PASSPOINT_INSTALLATION_FILE}.
     *
     * @return {@link PasspointConfiguration}
     */
    private PasspointConfiguration generateConfigurationFromProfile() {
        PasspointConfiguration config = new PasspointConfiguration();

        // HomeSP configuration.
        config.homeSp = new HomeSP();
        config.homeSp.friendlyName = "Century House";
        config.homeSp.fqdn = "mi6.co.uk";
        config.homeSp.roamingConsortiumOIs = new long[] {0x112233L, 0x445566L};

        // Credential configuration.
        config.credential = new Credential();
        config.credential.realm = "shaken.stirred.com";
        config.credential.userCredential = new Credential.UserCredential();
        config.credential.userCredential.username = "james";
        config.credential.userCredential.password = "Ym9uZDAwNw==";
        config.credential.userCredential.eapType = 21;
        config.credential.userCredential.nonEapInnerMethod = "MS-CHAP-V2";
        config.credential.certCredential = new Credential.CertificateCredential();
        config.credential.certCredential.certType = "x509v3";
        config.credential.certCredential.certSha256FingerPrint = new byte[32];
        Arrays.fill(config.credential.certCredential.certSha256FingerPrint, (byte)0x1f);
        config.credential.simCredential = new Credential.SimCredential();
        config.credential.simCredential.imsi = "imsi";
        config.credential.simCredential.eapType = 24;
        config.credential.caCertificate = FakeKeys.CA_CERT0;
        return config;
    }

    @Test
    public void parseConfigFile() throws Exception {
        String configStr = loadResourceFile(PASSPOINT_INSTALLATION_FILE);
        PasspointConfiguration expectedConfig = generateConfigurationFromProfile();
        PasspointConfiguration actualConfig =
                ConfigBuilder.buildPasspointConfig(
                        "application/x-wifi-config", configStr.getBytes());
        assertTrue(actualConfig.equals(expectedConfig));
    }

    @Test
    public void parseConfigFileWithInvalidMimeType() throws Exception {
        String configStr = loadResourceFile(PASSPOINT_INSTALLATION_FILE);
        assertNull(ConfigBuilder.buildPasspointConfig(
                "application/wifi-config", configStr.getBytes()));
    }
}