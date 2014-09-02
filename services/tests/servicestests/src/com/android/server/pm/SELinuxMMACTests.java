/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.FileUtils;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.frameworks.servicestests.R;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;

/**
 * Test the {@link SELinuxMMAC} functionality. An emphasis is placed on testing the
 * seinfo assignments that result from various mac_permissions.xml files. To run this
 * test individually use the following commands:
 *
 * <pre>
 * {@code
 * cd $ANDROID_BUILD_TOP
 * mmma -j8 frameworks/base/services/tests/servicestests/
 * adb install -r out/target/product/mako/data/app/FrameworksServicesTests.apk
 * adb shell am instrument -w -e class com.android.server.pm.SELinuxMMACTests com.android.frameworks.servicestests/android.test.InstrumentationTestRunner
 * }
 *
 */
public class SELinuxMMACTests extends AndroidTestCase {

    private static final String TAG = "SELinuxMMACTests";

    private static File MAC_INSTALL_TMP;
    private static File APK_INSTALL_TMP;

    // temporary locations for current test policy and apks
    private static final String MAC_INSTALL_TMP_NAME = "macperms_test_policy";
    private static final String APK_INSTALL_TMP_NAME = "test_install.apk";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Use the test apps data directory as scratch space
        File filesDir = mContext.getFilesDir();
        assertNotNull(filesDir);

        // Need a tmp file to hold mmac policy
        MAC_INSTALL_TMP = new File(filesDir, MAC_INSTALL_TMP_NAME);

        // Need a tmp file to hold the apk
        APK_INSTALL_TMP = new File(filesDir, APK_INSTALL_TMP_NAME);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Just in case tmp files still exist
        MAC_INSTALL_TMP.delete();
        APK_INSTALL_TMP.delete();
    }

    /**
     * Fake an app install. Simply call the PackageParser to parse and save the contents
     * of the app.
     */
    private PackageParser.Package parsePackage(Uri packageURI) {
        String archiveFilePath = packageURI.getPath();
        PackageParser packageParser = new PackageParser(archiveFilePath);
        File sourceFile = new File(archiveFilePath);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        PackageParser.Package pkg = packageParser.parsePackage(sourceFile,
                archiveFilePath, metrics, 0);
        assertNotNull(pkg);
        // Tell the package parser to collect the certs for this package
        boolean savedCerts = packageParser.collectCertificates(pkg, 0);
        assertTrue(savedCerts);
        return pkg;
    }

    /**
     * Dump the contents of a resource to a file. This is just an ancillary function
     * used for copying the apk and mac_permissions.xml policy files.
     */
    private Uri getResourceURI(int fileResId, File outFile) {
        try(InputStream is = mContext.getResources().openRawResource(fileResId)) {
            assertNotNull(is);
            boolean copied = FileUtils.copyToFile(is, outFile);
            assertTrue(copied);
        } catch (NotFoundException | IOException ex) {
            fail("Expecting to load resource with id: " + fileResId + ". " + ex);
        }

        return Uri.fromFile(outFile);
    }

    /**
     * Takes the policy xml file as a resource, the apk as a resource and the expected
     * seinfo string. Determines if the assigned seinfo string matches the passed string.
     */
    void checkInstallMMAC(int policyRes, int apkRes, String expectedSeinfo) {
        // Grab policy file as a uri
        Uri policyURI = getResourceURI(policyRes, MAC_INSTALL_TMP);
        assertNotNull(policyURI);

        // Parse the policy file via SELinuxMMAC
        boolean ret = SELinuxMMAC.readInstallPolicy(policyURI.getPath());
        assertTrue(ret);

        // Grab the apk as a uri
        Uri apkURI = getResourceURI(apkRes, APK_INSTALL_TMP);
        assertNotNull(apkURI);

        // "install" the apk
        PackageParser.Package pkg = parsePackage(apkURI);
        assertNotNull(pkg);
        assertNotNull(pkg.packageName);

        // Assign the apk an seinfo value
        SELinuxMMAC.assignSeinfoValue(pkg);

        // Check for expected seinfo against assigned seinfo value
        String assignedSeinfo = pkg.applicationInfo.seinfo;
        assertEquals(expectedSeinfo, assignedSeinfo);

        // delete policy and apk
        MAC_INSTALL_TMP.delete();
        APK_INSTALL_TMP.delete();
    }

    /*
     * Start of the SElinuxMMAC tests
     */

    /*
     * Requested policy file doesn't exist. Should return false
     * meaning that the install policy was not found.
     */
    public void testINSTALL_POLICY_BADPATH() {
        boolean ret = SELinuxMMAC.readInstallPolicy("/d/o/e/s/n/t/e/x/i/s/t");
        assertFalse(ret);
    }

    /*
     * Raw resource xml file names can be decoded with:
     *  c = signature stanza included (cert)
     *  s = seinfo tag attached to parent tag is included
     *  p = package tag attached to the parent is included
     *  d = default tag included
     *  n = means the next abbreviation is missing
     *
     * Example: R.raw.mmac_cs_pns_ds.xml would translate to
     * signer stanza with a seinfo tag attached followed
     * by a child package tag which *doesn't* have an seinfo
     * tag; notice the ns. Lastly, there is a default tag
     * with an attached seinfo tag.
     */

    /*
     * Policy with only one sig stanza and no default stanza.
     */
    public void testCS_NP_ND() {
        checkInstallMMAC(R.raw.mmac_cs_np_nd, R.raw.signed_platform, "platform");
    }

    public void testCS_PS_ND() {
        checkInstallMMAC(R.raw.mmac_cs_ps_nd, R.raw.signed_platform, "package");
    }

    public void testCNS_PS_ND() {
        checkInstallMMAC(R.raw.mmac_cns_ps_nd, R.raw.signed_platform, "package");
    }
    /*
     * End of one sig stanza only
     */

    /*
     * Policy with only one sig stanza and the default stanza.
     */
    public void testCS_NP_DS() {
        checkInstallMMAC(R.raw.mmac_cs_np_ds, R.raw.signed_platform, "platform");
    }

    public void testCS_PS_DS() {
        checkInstallMMAC(R.raw.mmac_cs_ps_ds, R.raw.signed_platform, "package");
    }

    public void testCNS_PS_DS() {
        checkInstallMMAC(R.raw.mmac_cns_ps_ds, R.raw.signed_platform, "package");
    }

    public void testNC_NP_DS() {
        checkInstallMMAC(R.raw.mmac_nc_np_ds, R.raw.signed_platform, "default");
    }
    /*
     * End of test default stanza
     */

    // Test for empty policy (i.e. no stanzas at all)
    public void testNC_NP_ND() {
        checkInstallMMAC(R.raw.mmac_nc_np_nd, R.raw.signed_platform, null);
    }
}
