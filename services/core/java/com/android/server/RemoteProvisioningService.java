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

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
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
    private static final String GEEK_URL = PROVISIONING_URL + "";
    private static final String CERTIFICATE_SIGNING_URL = PROVISIONING_URL + "";
    private static final String TAG = "RemoteProvisioningService";

    private Context mContext;
    private KeyStore mKeyStore;
    private RemoteProvisioner mRemoteProvisioner;
    private boolean isInternetConnected;

    public RemoteProvisioningService(Context context) {
        super(context);
        mContext = context;
        ConnectivityManager cm =
            (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerDefaultNetworkCallback(new ConnectionCallback());
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

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
             
            StringBuffer json = new StringBuffer(1024);
            String tmp="";
            while((tmp=reader.readLine())!=null)
                json.append(tmp).append("\n");
            reader.close();
             
            /*JSONObject data = new JSONObject(json.toString());

            if(data.getInt("cod") != 200){
                return null;
            }*/
             
            return new GeekResp(new byte[0], new byte[0]);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch GEEK from the servers.", e);
            return null;
        }
    }

    private byte[] requestSignedCertificates(byte[] cborBlob) {
        try {
            URL url = new URL(CERTIFICATE_SIGNING_URL);           
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            // may not be able to use try-with-resources here if the connection gets closed due to the
            // output stream being automatically closed
            try(OutputStream os = con.getOutputStream()) {
                os.write(cborBlob, 0, cborBlob.length);			
            } catch (Exception e) {
                return null;
            }

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuffer json = new StringBuffer(1024);
                String tmp = "";
                while((tmp = reader.readLine()) != null) {
                
                }
            }
                             
            /*JSONObject data = new JSONObject(json.toString());

            if(data.getInt("cod") != 200){
                return null;
            }*/

            return new byte[0];
        } catch (IOException e) {
            Log.w(TAG, "Failed to request signed certificates from the server", e);
            return null;
        }
    }       

    private void provisionCerts(int numCerts) {
        if (numCerts < 0) {
            return;
        }
        
        GeekResp geek = fetchGeek();
        if (geek == null) {
            return;
        }
        byte[] payload = new byte[0];
        mKeyStore.generateCsr(numCerts,
                              geek.mGeek,
                              geek.mChallenge,
                              payload);
        if (payload == null) {
            return;
        }
        try {
            mKeyStore.provisionCertChain(formatX509Certs(requestSignedCertificates(payload)));
        } catch (CertificateException e) {
            Log.e(TAG, "Failed to parse certificates", e);
        }
    }

    private class ProvisioningTask implements Runnable {
        public void run() {
            while (true) {
                Calendar dateCheck = Calendar.getInstance();
                dateCheck.add(Calendar.DAY_OF_MONTH, DAYS_UNTIL_EXPIRATION);
                AttestationPoolStatus stat = mKeyStore.getPoolStatus(dateCheck.getTime());
                int numCsrs = stat.expiring;
                if (numCsrs > 0) {
                    provisionCerts(numCsrs);
                }
                try {
                    Thread.sleep(1000 * 60 * 60 * 24);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Thread Interrupted", e);
                }
            }
        }
    }

    private class GeekResp {
        public byte[] mChallenge;
        public byte[] mGeek;
        public GeekResp(byte[] challenge, byte[] geek) {
            mChallenge = challenge;
            mGeek = geek;
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
