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

package com.android.server;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.security.provisioner.IRemoteProvisioner;
import android.security.keystore.AttestationPoolStatus;
import android.security.KeyStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.InterruptedException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Calendar;

import javax.security.cert.X509Certificate;

/**
 * <p>This service handles the communication between Keystore and provisioning servers required to
 * provision the device with attestation certificates signed by the servers. This service primarily
 * checks in with Keystore once a day to see how many certificates will be expiring soon. It will
 * retrieve certificate signing requests to send off to the provisioning servers for however many
 * certificates are expiring.</p>
 *
 * <p>This service is not designed to be interacted with by other entities on device except through
 * the callback it provides to Keystore to allow Keystore to proactively notify the provisioning
 * service when it consumes another attestation key.</p>
 *
 * <p>The only other inteded interaction for this service is through a shell command line interface
 * for testing purposes, whereby a host machine will act as a provisioning server to ensure
 * the feature is functioning as intended.</p>
 */
public class RemoteProvisioningService extends SystemService {
    private static final int DEFAULT_POOL_COUNT = 5;
    private static final int DAYS_UNTIL_EXPIRATION = 5;
    private static final String PROVISIONING_URL = "";
    private static final String GEEK_URL = PROVISIONING_URL + "/v1/eekchain";
    private static final String CERTIFICATE_SIGNING_URL =
        PROVISIONING_URL + "/v1:signCertificates?challenge=";
    private static final String TAG = "RemoteProvisioningService";

    private Context mContext;
    private KeyStore mKeyStore;
    private RemoteProvisioner mRemoteProvisioner;
    private boolean isInternetConnected;

    public RemoteProvisioningService(Context context) {
        super(context);
        mContext = context;
        // TODO(jbires): This is started before Connectivity Manager. Resolve this.
        //ConnectivityManager cm =
            //(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //cm.registerDefaultNetworkCallback(new ConnectionCallback());
    }

    @Override
    public void onStart() {
        mRemoteProvisioner = new RemoteProvisioner();
        publishBinderService("RemoteProvisionerTool", mRemoteProvisioner);
        mKeyStore = KeyStore.getInstance();
        mKeyStore.setUnsignedPoolCount(DEFAULT_POOL_COUNT);
        Thread provisionCheck = new Thread(new ProvisioningTask());
        provisionCheck.start();
    }

    private X509Certificate[] formatX509Certs(byte[] certStream)
            throws CertificateException {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream in = new ByteArrayInputStream(certStream);
        ArrayList<Certificate> certs = new ArrayList<Certificate>(fact.generateCertificates(in));
        return certs.toArray(new X509Certificate[certs.size()]);
    }

    private GeekResp fetchGeek() {
        try {
            URL url = new URL(GEEK_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Server connection for GEEK failed, response code: "
                           + con.getResponseCode());
            }

            BufferedInputStream inputStream = new BufferedInputStream(con.getInputStream());
            ByteArrayOutputStream cborBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            while((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                cborBytes.write(buffer, 0, read);
            }
            inputStream.close();
            return new GeekResp(cborBytes.toByteArray());
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch GEEK from the servers.", e);
            return null;
        }
    }

    private byte[] requestSignedCertificates(byte[] cborBlob, byte[] challenge) {
        try {
            URL url = new URL(CERTIFICATE_SIGNING_URL + new String(challenge, "UTF-8"));           
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Server connection for signing failed, response code: "
                           + con.getResponseCode());
            }
            // may not be able to use try-with-resources here if the connection gets closed due to the
            // output stream being automatically closed
            try (OutputStream os = con.getOutputStream()) {
                os.write(cborBlob, 0, cborBlob.length);			
            } catch (Exception e) {
                return null;
            }

            
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuffer data = new StringBuffer(1024);
                String tmp = "";
                while ((tmp = reader.readLine()) != null) {
                    data.append(tmp);
                }
            }
            return new byte[0];
        } catch (IOException e) {
            Log.w(TAG, "Failed to request signed certificates from the server", e);
            return null;
        }
    }

    private boolean provisionCerts(int numCerts) {
        if (numCerts < 0) {
            Log.e(TAG, "Illegal number of certificates requested: " + numCerts);
            return false;
        }
        
        GeekResp geek = fetchGeek();
        if (geek == null) {
            Log.e(TAG, "The geek is null");
            return false;
        }
        byte[] payload = new byte[0];
        mKeyStore.generateCsr(numCerts,
                              geek.mGeek,
                              geek.mChallenge,
                              payload);
        if (payload == null) {
            Log.e(TAG, "Keystore failed to generate a payload");
            return false;
        }
        try {
            mKeyStore.provisionCertChain(formatX509Certs(
                    requestSignedCertificates(payload, geek.mChallenge)));
            return true;
        } catch (CertificateException e) {
            Log.e(TAG, "Failed to parse certificates", e);
            return false;
        }
    }

    private class ProvisioningTask implements Runnable {
        public void run() {
            while (true) {
                try {
                    //Thread.sleep(1000 * 60 * 60 * 24);
                    Thread.sleep(1000*60);
                    Log.e(TAG, "HEYA");
                } catch (InterruptedException e) {
                    Log.w(TAG, "Thread Interrupted", e);
                }
                Calendar dateCheck = Calendar.getInstance();
                dateCheck.add(Calendar.DAY_OF_MONTH, DAYS_UNTIL_EXPIRATION);
                AttestationPoolStatus stat = mKeyStore.getPoolStatus(dateCheck.getTime());
                if (stat == null) {
                    Log.w(TAG, "Keystore failed to return attestation pool status");
                    continue;
                }
                int numCsrs = stat.expiring;
                if (numCsrs > 0) {
                    if (!provisionCerts(numCsrs)) Log.w(TAG, "Attestation certificate provisioning failed");
                }
            }
        }
    }

    private class GeekResp {
        public byte[] mChallenge;
        public byte[] mGeek;
        public GeekResp(byte[] geekServerResponse) {
            // parse CBOR here
            mChallenge = new byte[0];
            mGeek = new byte[0];
        }
    }

    private class ConnectionCallback extends NetworkCallback {
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            isInternetConnected =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        public void onLost(Network network) {
            isInternetConnected = false;
        }
    }

    private final class RemoteProvisioner extends IRemoteProvisioner.Stub {
        public byte[] getCertificateRequest(boolean testMode,
                                            int keyCount,
                                            byte[] endpointEncryptionKey,
                                            byte[] challenge) {
            byte[] payload = new byte[0];
            return mKeyStore.generateCsr(keyCount, endpointEncryptionKey, challenge, payload);
        }
    }
}
