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

import io.grpc.ManagedChannelBuilder;

import android.security.provisioner.CertificateRequest;
import android.security.provisioner.DeviceInfo;
import android.security.provisioner.GetGeekResponse;
import android.security.provisioner.IRemoteProvisioner;
import android.security.provisioner.SignCertificateRequest;
import android.security.provisioner.SignCertificateResponse;
import android.security.KeyStore;

import java.io.InputStream;
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

    private KeyStore mKeyStore;
    private RemoteProvisioner mRemoteProvisioner;

    @Override
    public void onStart() {
        mRemoteProvisioner = new RemoteProvisioner();
        publishBinderService("RemoteProvisionerTool", mRemoteProvisioner);
        mKeyStore = KeyStore.getInstance();
        mKeyStore.setUnsignedPoolCount(DEFAULT_POOL_COUNT);
        Thread provisionCheck = new Thread(new ProvisioningTask());
        provisionCheck.start();
    }

    private X509Certificate[] formatX509Certs(SignCertificateResponse resp)
            throws CertificateException {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        InputStream in = new InputStream(resp.getCertificates());
        ArrayList<X509Certificate> certs =
                (ArrayList<X509Certificate>) fact.generateCertificates(in);
        return (X509Certificate[]) certs.toArray();
    }

    private void provisionCerts(int numCerts) throws CertificateException {
        if (numCerts <= 0) {
            return;
        }
        ManagedChannelBuilder channel = ManagedChannelBuilder.forTarget(GEEK_URL)
                                                         .useTransportSecurity()
                                                         .build();
        ProvisionerService stub = ProvisionerService.newBlockingStub(mChannel);
        GetGeekResponse resp = stub.getGeek(google.protobuf.Empty);
        byte[] payload;
        mKeyStore.generateCsr(numCerts,
                              resp.getGeekChain(),
                              resp.getChallenge(),
                              payload);
        SignCertificateRequest signCertReq =
                SignCertificateRequest.newBuilder()
                                      .setDeviceInfo(DeviceInfo.newBuilder().build())
                                      .setChallenge(resp.getChallenge())
                                      .addCertificateRequests(CertificateRequest.newBuilder()
                                          .setCertificateSigningRequest(payload)
                                          .build())
                                      .build();
        mKeystore.provisionCertChain(formatX509Certs(stub.signCertificate(signCertReq)));
    }

    private class ProvisioningTask implements Runnable {
        public void run() {
            while (true) {
                Calendar dateCheck = Calendar.getInstance();
                dateCheck.add(Calendar.DAY, DAYS_UNTIL_EXPIRATION);
                AttestationPoolStatus stat = mKeyStore.getPoolStatus(dateCheck.getTime());
                int numCsrs = stat.expiring;
                byte[] payload;
                if (numCsrs > 0) {
                    provisionCerts(numCsrs);
                }
                Thread.sleep(1000 * 60 * 60 * 24);
            }
        }
    }

    private final class RemoteProvisioner extends IRemoteProvisioner.Stub {
        public byte[] getCertificateRequest(boolean testMode,
                                            int keyCount,
                                            byte[] endpointEncryptionKey,
                                            byte[] challenge) {
            byte[] payload;
            return mKeyStore.generateCsr(keyCount, endpointEncryptionKey, challenge, payload);
        }
    }
}
