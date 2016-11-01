/**
 * Copyright (c) 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi.hotspot2.pps;

import android.net.wifi.ParcelUtil;
import android.os.Parcelable;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class representing Credential subtree in the PerProviderSubscription (PPS)
 * Management Object (MO) tree.
 * For more info, refer to Hotspot 2.0 PPS MO defined in section 9.1 of the Hotspot 2.0
 * Release 2 Technical Specification.
 *
 * In addition to the fields in the Credential subtree, this will also maintain necessary
 * information for the private key and certificates associated with this credential.
 *
 * Currently we only support the nodes that are used by Hotspot 2.0 Release 1.
 *
 * @hide
 */
public final class Credential implements Parcelable {
    private static final String TAG = "Credential";

    /**
     * Supported EAP types.
     */
    public static final int EAP_TLS = 13;
    public static final int EAP_SIM = 18;
    public static final int EAP_TTLS = 21;
    public static final int EAP_AKA = 23;
    public static final int EAP_AKA_PRIME = 50;

    /**
     * Max string length for realm.  Refer to Credential/Realm node in Hotspot 2.0 Release 2
     * Technical Specification Section 9.1 for more info.
     */
    private static final int MAX_REALM_LENGTH = 253;

    /**
     * The realm associated with this credential.  It will be used to determine
     * if this credential can be used to authenticate with a given hotspot by
     * comparing the realm specified in that hotspot's ANQP element.
     */
    public String realm = null;

    /**
     * Username-password based credential.
     * Contains the fields under PerProviderSubscription/Credential/UsernamePassword subtree.
     */
    public static final class UserCredential implements Parcelable {
        /**
         * Maximum string length for username.  Refer to Credential/UsernamePassword/Username
         * node in Hotspot 2.0 Release 2 Technical Specification Section 9.1 for more info.
         */
        private static final int MAX_USERNAME_LENGTH = 63;

        /**
         * Maximum string length for password.  Refer to Credential/UsernamePassword/Password
         * in Hotspot 2.0 Release 2 Technical Specification Section 9.1 for more info.
         */
        private static final int MAX_PASSWORD_LENGTH = 255;

        /**
         * Supported Non-EAP inner methods.  Refer to
         * Credential/UsernamePassword/EAPMethod/InnerEAPType in Hotspot 2.0 Release 2 Technical
         * Specification Section 9.1 for more info.
         */
        private static final Set<String> SUPPORTED_AUTH =
                new HashSet<String>(Arrays.asList("PAP", "CHAP", "MS-CHAP", "MS-CHAP-V2"));

        /**
         * Username of the credential.
         */
        public String username = null;

        /**
         * Base64-encoded password.
         */
        public String password = null;

        /**
         * EAP (Extensible Authentication Protocol) method type.
         * Refer to http://www.iana.org/assignments/eap-numbers/eap-numbers.xml#eap-numbers-4
         * for valid values.
         * Using Integer.MIN_VALUE to indicate unset value.
         */
        public int eapType = Integer.MIN_VALUE;

        /**
         * Non-EAP inner authentication method.
         */
        public String nonEapInnerMethod = null;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(username);
            dest.writeString(password);
            dest.writeInt(eapType);
            dest.writeString(nonEapInnerMethod);
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (!(thatObject instanceof UserCredential)) {
                return false;
            }

            UserCredential that = (UserCredential) thatObject;
            return TextUtils.equals(username, that.username) &&
                    TextUtils.equals(password, that.password) &&
                    eapType == that.eapType &&
                    TextUtils.equals(nonEapInnerMethod, that.nonEapInnerMethod);
        }

        /**
         * Validate the configuration data.
         *
         * @return true on success or false on failure
         */
        public boolean validate() {
            if (username == null || username.isEmpty()) {
                Log.d(TAG, "Missing username");
                return false;
            }
            if (username.length() > MAX_USERNAME_LENGTH) {
                Log.d(TAG, "username exceeding maximum length: " + username.length());
                return false;
            }

            if (password == null || password.isEmpty()) {
                Log.d(TAG, "Missing password");
                return false;
            }
            if (password.length() > MAX_PASSWORD_LENGTH) {
                Log.d(TAG, "password exceeding maximum length: " + password.length());
                return false;
            }

            if (eapType != EAP_TLS && eapType != EAP_TTLS) {
                Log.d(TAG, "Invalid EAP Type for user credential: " + eapType);
                return false;
            }

            // Verify Non-EAP inner method for EAP-TTLS.
            if (eapType == EAP_TTLS && !SUPPORTED_AUTH.contains(nonEapInnerMethod)) {
                Log.d(TAG, "Invalid non-EAP inner method for EAP-TTLS: " + nonEapInnerMethod);
                return false;
            }
            return true;
        }

        public static final Creator<UserCredential> CREATOR =
            new Creator<UserCredential>() {
                @Override
                public UserCredential createFromParcel(Parcel in) {
                    UserCredential userCredential = new UserCredential();
                    userCredential.username = in.readString();
                    userCredential.password = in.readString();
                    userCredential.eapType = in.readInt();
                    userCredential.nonEapInnerMethod = in.readString();
                    return userCredential;
                }

                @Override
                public UserCredential[] newArray(int size) {
                    return new UserCredential[size];
                }
            };
    }
    public UserCredential userCredential = null;

    /**
     * Certificate based credential.
     * Contains fields under PerProviderSubscription/Credential/DigitalCertificate subtree.
     */
    public static final class CertificateCredential implements Parcelable {
        /**
         * Supported certificate types.
         */
        private static final String CERT_TYPE_X509V3 = "x509v3";

        /**
         * Certificate SHA-256 fingerprint length.
         */
        private static final int CERT_SHA256_FINGER_PRINT_LENGTH = 32;

        /**
         * Certificate type.
         */
        public String certType = null;

        /**
         * The SHA-256 fingerprint of the certificate.
         */
        public byte[] certSha256FingerPrint = null;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(certType);
            dest.writeByteArray(certSha256FingerPrint);
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (!(thatObject instanceof CertificateCredential)) {
                return false;
            }

            CertificateCredential that = (CertificateCredential) thatObject;
            return TextUtils.equals(certType, that.certType) &&
                    Arrays.equals(certSha256FingerPrint, that.certSha256FingerPrint);
        }

        /**
         * Validate the configuration data.
         *
         * @return true on success or false on failure
         */
        public boolean validate() {
            if (!TextUtils.equals(CERT_TYPE_X509V3, certType)) {
                Log.d(TAG, "Unsupported certificate type: " + certType);
                return false;
            }
            if (certSha256FingerPrint == null ||
                    certSha256FingerPrint.length != CERT_SHA256_FINGER_PRINT_LENGTH) {
                Log.d(TAG, "Invalid SHA-256 fingerprint");
                return false;
            }
            return true;
        }

        public static final Creator<CertificateCredential> CREATOR =
            new Creator<CertificateCredential>() {
                @Override
                public CertificateCredential createFromParcel(Parcel in) {
                    CertificateCredential certCredential = new CertificateCredential();
                    certCredential.certType = in.readString();
                    certCredential.certSha256FingerPrint = in.createByteArray();
                    return certCredential;
                }

                @Override
                public CertificateCredential[] newArray(int size) {
                    return new CertificateCredential[size];
                }
            };
    }
    public CertificateCredential certCredential = null;

    /**
     * SIM (Subscriber Identify Module) based credential.
     * Contains fields under PerProviderSubscription/Credential/SIM subtree.
     */
    public static final class SimCredential implements Parcelable {
        /**
         * International Mobile device Subscriber Identity.
         */
        public String imsi = null;

        /**
         * EAP (Extensible Authentication Protocol) method type for using SIM credential.
         * Refer to http://www.iana.org/assignments/eap-numbers/eap-numbers.xml#eap-numbers-4
         * for valid values.
         * Using Integer.MIN_VALUE to indicate unset value.
         */
        public int eapType = Integer.MIN_VALUE;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (!(thatObject instanceof SimCredential)) {
                return false;
            }

            SimCredential that = (SimCredential) thatObject;
            return TextUtils.equals(imsi, that.imsi) &&
                    eapType == that.eapType;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(imsi);
            dest.writeInt(eapType);
        }

        /**
         * Validate the configuration data.
         *
         * @return true on success or false on failure
         */
        public boolean validate() {
            if (imsi == null || imsi.isEmpty()) {
                Log.d(TAG, "Missing IMSI");
                return false;
            }
            if (eapType != EAP_SIM && eapType != EAP_AKA && eapType != EAP_AKA_PRIME) {
                Log.d(TAG, "Invalid EAP Type for SIM credential: " + eapType);
                return false;
            }
            return true;
        }

        public static final Creator<SimCredential> CREATOR =
            new Creator<SimCredential>() {
                @Override
                public SimCredential createFromParcel(Parcel in) {
                    SimCredential simCredential = new SimCredential();
                    simCredential.imsi = in.readString();
                    simCredential.eapType = in.readInt();
                    return simCredential;
                }

                @Override
                public SimCredential[] newArray(int size) {
                    return new SimCredential[size];
                }
            };
    }
    public SimCredential simCredential = null;

    /**
     * CA (Certificate Authority) X509 certificate.
     */
    public X509Certificate caCertificate = null;

    /**
     * Client side X509 certificate chain.
     */
    public X509Certificate[] clientCertificateChain = null;

    /**
     * Client side private key.
     */
    public PrivateKey clientPrivateKey = null;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(realm);
        dest.writeParcelable(userCredential, flags);
        dest.writeParcelable(certCredential, flags);
        dest.writeParcelable(simCredential, flags);
        ParcelUtil.writeCertificate(dest, caCertificate);
        ParcelUtil.writeCertificates(dest, clientCertificateChain);
        ParcelUtil.writePrivateKey(dest, clientPrivateKey);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof Credential)) {
            return false;
        }

        Credential that = (Credential) thatObject;
        return TextUtils.equals(realm, that.realm) &&
                (userCredential == null ? that.userCredential == null :
                    userCredential.equals(that.userCredential)) &&
                (certCredential == null ? that.certCredential == null :
                    certCredential.equals(that.certCredential)) &&
                (simCredential == null ? that.simCredential == null :
                    simCredential.equals(that.simCredential)) &&
                isX509CertificateEquals(caCertificate, that.caCertificate) &&
                isX509CertificatesEquals(clientCertificateChain, that.clientCertificateChain) &&
                isPrivateKeyEquals(clientPrivateKey, that.clientPrivateKey);
    }

    /**
     * Validate the configuration data.
     *
     * @return true on success or false on failure
     */
    public boolean validate() {
        if (realm == null || realm.isEmpty()) {
            Log.d(TAG, "Missing realm");
            return false;
        }
        if (realm.length() > MAX_REALM_LENGTH) {
            Log.d(TAG, "realm exceeding maximum length: " + realm.length());
            return false;
        }

        if (userCredential != null) {
            if (!userCredential.validate()) {
                return false;
            }
            if (simCredential != null) {
                Log.d(TAG, "Contained both user and SIM credential");
                return false;
            }
            if (caCertificate == null) {
                Log.d(TAG, "Missing CA Certificate for user credential");
                return false;
            }
            if (userCredential.eapType == EAP_TLS) {
                if (!verifyCertificateCredentialForEapTls()) {
                    return false;
                }
            }
        } else if (simCredential != null) {
            if (!simCredential.validate()) {
                return false;
            }
        } else {
            Log.d(TAG, "Missing required credential");
            return false;
        }

        return true;
    }

    public static final Creator<Credential> CREATOR =
        new Creator<Credential>() {
            @Override
            public Credential createFromParcel(Parcel in) {
                Credential credential = new Credential();
                credential.realm = in.readString();
                credential.userCredential = in.readParcelable(null);
                credential.certCredential = in.readParcelable(null);
                credential.simCredential = in.readParcelable(null);
                credential.caCertificate = ParcelUtil.readCertificate(in);
                credential.clientCertificateChain = ParcelUtil.readCertificates(in);
                credential.clientPrivateKey = ParcelUtil.readPrivateKey(in);
                return credential;
            }

            @Override
            public Credential[] newArray(int size) {
                return new Credential[size];
            }
        };

    /**
     * Verify certificate credential, private key, and certificates for EAP-TLS.
     *
     * @return true if necessary credentials for EAP-TLS are provided, false otherwise.
     */
    private boolean verifyCertificateCredentialForEapTls() {
        if (certCredential == null) {
            Log.d(TAG, "Missing digital certificate for EAP-TLS");
            return false;
        }
        if (!certCredential.validate()) {
            return false;
        }
        if (clientPrivateKey == null) {
            Log.d(TAG, "Missing client private key for EAP-TLS");
            return false;
        }
        try {
            // Verify SHA-256 fingerprint for client certificate.
            if (!verifySha256Fingerprint(clientCertificateChain,
                    certCredential.certSha256FingerPrint)) {
                Log.d(TAG, "SHA-256 fingerprint mismatch");
                return false;
            }
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            Log.d(TAG, "Failed to verify SHA-256 fingerprint: " + e.getMessage());
            return false;
        }

        return true;
    }

    private static boolean isPrivateKeyEquals(PrivateKey key1, PrivateKey key2) {
        if (key1 == null && key2 == null) {
            return true;
        }

        /* Return false if only one of them is null */
        if (key1 == null || key2 == null) {
            return false;
        }

        return TextUtils.equals(key1.getAlgorithm(), key2.getAlgorithm()) &&
                Arrays.equals(key1.getEncoded(), key2.getEncoded());
    }

    private static boolean isX509CertificateEquals(X509Certificate cert1, X509Certificate cert2) {
        if (cert1 == null && cert2 == null) {
            return true;
        }

        /* Return false if only one of them is null */
        if (cert1 == null || cert2 == null) {
            return false;
        }

        boolean result = false;
        try {
            result = Arrays.equals(cert1.getEncoded(), cert2.getEncoded());
        } catch (CertificateEncodingException e) {
            /* empty, return false. */
        }
        return result;
    }

    private static boolean isX509CertificatesEquals(X509Certificate[] certs1,
                                                    X509Certificate[] certs2) {
        if (certs1 == null && certs2 == null) {
            return true;
        }

        /* Return false if only one of them is null */
        if (certs1 == null || certs2 == null) {
            return false;
        }

        if (certs1.length != certs2.length) {
            return false;
        }

        for (int i = 0; i < certs1.length; i++) {
            if (!isX509CertificateEquals(certs1[i], certs2[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verify that the digest for a certificate in the certificate chain matches expected
     * fingerprint.  The certificate that matches the fingerprint is the client certificate.
     *
     * @param certChain Chain of certificates
     * @param expectedFingerprint The expected SHA-256 digest of the client certificate
     * @return true if the certificate chain contains a matching certificate, false otherwise
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    private static boolean verifySha256Fingerprint(X509Certificate[] certChain,
                                                   byte[] expectedFingerprint)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        if (certChain == null) {
            return false;
        }
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        for (X509Certificate certificate : certChain) {
            digester.reset();
            byte[] fingerprint = digester.digest(certificate.getEncoded());
            if (Arrays.equals(expectedFingerprint, fingerprint)) {
                return true;
            }
        }
        return false;
    }
}
