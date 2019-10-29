/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.UnsupportedAppUsage;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import com.android.internal.net.VpnConfig;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * This class provides an interface for managing VPN profiles
 *
 * <p>Apps can use this API to provide profiles with which the platform can set up a VPN without
 * further app intermediation. If the app is selected as an always-on VPN, the platform will trigger
 * the negotiation of the VPN without binding to the provisioning app.
 *
 * <p>VPN apps using supported protocols should preferentially use this API over the
 * {@link VpnService}, due to improved platform integration.
 */
public class VpnManager {
    /**
     * Use IConnectivityManager since these methods are hidden and not
     * available in ConnectivityManager.
     */
    private static IConnectivityManager getService() {
        return IConnectivityManager.Stub.asInterface(
                ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    }

    /**
     * Package method to validate address and prefixLength.
     */
    static void validateInetAddress(InetAddress address, int prefixLength) {
        if (address.isLoopbackAddress()) {
            throw new IllegalArgumentException("Bad address");
        }
        if (address instanceof Inet4Address) {
            if (prefixLength < 0 || prefixLength > 32) {
                throw new IllegalArgumentException("Bad prefixLength");
            }
        } else if (address instanceof Inet6Address) {
            if (prefixLength < 0 || prefixLength > 128) {
                throw new IllegalArgumentException("Bad prefixLength");
            }
        } else {
            throw new IllegalArgumentException("Unsupported family");
        }
    }

    /**
     * Pushes a VpnProfile configuration.
     *
     * @param profile the VpnProfile provided by this package. May be null
     *     to remove any previous profile provisioned by this package. Will
     *     override any previous VpnProfile stored for this package.
     * @returns an intent to request user consent if needed
     *     (null otherwise).
     */
    public Intent provisionVpn(@NonNull Context ctx, @Nullable VpnProfile profile) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Requests the starting of a previously provisioned VPN.
     *
     * @throws SecurityException exception if user consent was not obtained,
     *     or user settings prevent this VPN from being setup.
     */
    public void startVpn(){
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Tears down the VPN provided by this package (if any)
     */
    public void stopVpn(){
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Incrementally builds and validates {@link VpnProfile} instances for use in IKEv2/IPsec VPNs
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296 - Internet Key
     * Exchange, Version 2 (IKEv2)</a>
     */
    public static class Ikev2VpnProfileBuilder {
        private final VpnProfile mProfile;

        public Ikev2VpnProfileBuilder(@NonNull Context context, @NonNull String serverAddr) {
            checkNotNull(context, "Required parameter was null: context");
            checkNotNull(serverAddr, "Required parameter was null: serverAddr");

            mProfile = new VpnProfile("PlatformVpn_" + context.getPackageName());
            mProfile.server = serverAddr;
        }

        /**
         * Set the name of this VPN. It will be displayed in system-managed dialogs and
         * notifications. This is recommended, but not required.
         *
         * @param name the session name
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setSessionName(@NonNull String name){
            checkNotNull(name, "Required parameter was null: name");

            mProfile.name = name;
            return this;
        }

        /**
         * Sets the identity string to be used for IKEv2 authentication
         *
         * @param identity the identity string
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setIdentity(@NonNull String identity){
            checkNotNull(identity, "Required parameter was null: identity");

            mProfile.ipsecIdentifier = identity;
            return this;
        }

        @NonNull
        private Ikev2VpnProfileBuilder setUserCert(@NonNull X509Certificate cert) throws CertificateEncodingException {
            checkNotNull(cert, "Required parameter was null: cert");

            mProfile.ipsecUserCert = Base64.getEncoder().encodeToString(cert.getEncoded());
            return this;
        }

        @NonNull
        private Ikev2VpnProfileBuilder setServerRootCa(@NonNull X509Certificate cert) throws CertificateEncodingException {
            checkNotNull(cert, "Required parameter was null: cert");

            mProfile.ipsecCaCert = Base64.getEncoder().encodeToString(cert.getEncoded());
            return this;
        }

        /**
         * Set the IKEv2 authentication to use the provided username/password
         *
         * <p>Setting this will configure IKEv2 authentication using EAP-MSCHAPv2. Only one
         * authentication method may be selected.
         *
         * @param user the username to be used for EAP-MSCHAPv2 authentication
         * @param pass the password to be used for EAP-MSCHAPv2 authentication
         * @param serverRootCa the root certificate to be used for verifying the identity of the server
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setAuthUsernamePassword(@NonNull String user, @NonNull String pass, @NonNull X509Certificate serverRootCa) throws CertificateEncodingException {
            checkNotNull(user, "Required parameter was null: user");
            checkNotNull(pass, "Required parameter was null: pass");
            checkNotNull(serverRootCa, "Required parameter was null: serverRootCa");

            setServerRootCa(serverRootCa);
            mProfile.username = user;
            mProfile.password = pass;
            mProfile.type = VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS;
            return this;
        }

        /**
         * Set the IKEv2 authentication to use Digital Signature Authentication with the given key
         *
         * <p>Setting this will configure IKEv2 authentication using a Digital Signature scheme.
         * Only one authentication method may be selected.
         *
         * @param userCert the username to be used for EAP-MSCHAPv2 authentication
         * @param key the PrivateKey instance associated with the user ceritificate, used for constructing the signature
         * @param serverRootCa the root certificate to be used for verifying the identity of the server
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setAuthDigitalSignature(@NonNull X509Certificate userCert, @NonNull PrivateKey key, @NonNull X509Certificate serverRootCa) throws CertificateEncodingException {
            checkNotNull(userCert, "Required parameter was null: userCert");
            checkNotNull(key, "Required parameter was null: key");
            checkNotNull(serverRootCa, "Required parameter was null: serverRootCa");

            setServerRootCa(serverRootCa);
            setUserCert(userCert);
            mProfile.ipsecSecret = Base64.getEncoder().encodeToString(key.getEncoded());
            mProfile.type = VpnProfile.TYPE_IKEV2_IPSEC_RSA;
            return this;
        }

        /**
         * Set the IKEv2 authentication to use Preshared keys
         *
         * <p>Setting this will configure IKEv2 authentication using a Preshared Key. Only one
         * authentication method may be selected.
         *
         * @param psk the username to be used for EAP-MSCHAPv2 authentication
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setAuthPsk(@NonNull String psk){
            checkNotNull(psk, "Required parameter was null: name");

            mProfile.ipsecSecret = psk;
            mProfile.type = VpnProfile.TYPE_IKEV2_IPSEC_PSK;
            return this;
        }

        /**
         * Allow all apps to bypass this VPN connection.
         *
         * By default, all traffic from apps is forwarded through the VPN interface and it is not
         * possible for apps to side-step the VPN. If this method is called, apps may use methods
         * such as {@link ConnectivityManager#bindProcessToNetwork} to instead send/receive
         * directly over the underlying network or any other network they have permissions for.
         *
         * @param isBypassable Whether or not the VPN should be considered bypassable. Defaults to
         *     {@code false}.
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setBypassable(boolean isBypassable) {
            mProfile.isBypassable = isBypassable;
            return this;
        }

        /**
         * Sets an HTTP proxy for the VPN network.
         *
         * <p> Note that this proxy is only a recommendation and it may be ignored by apps.
         *
         * @param the ProxyInfo to be set for the VPN network
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setHttpProxy(@Nullable ProxyInfo proxy) {
            mProfile.proxy = proxy;
            return this;
        }

        /**
         * Set the upper bound of the maximum transmission unit (MTU) of the VPN interface.
         *
         * <p>If it is not set, a safe value will be used. Additionally, the actual link MTU will be
         * dynamically calculated/updated based on the underlying link's mtu.
         *
         * @param mtu the MTU (in bytes) of the VPN interface.
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         * @throws IllegalArgumentException if the value is not at least greater than the minimum IPv6 MTU.
         */
        @NonNull
        public Ikev2VpnProfileBuilder setMaxMtu(int mtu) {
            if (mtu <= 1280) {
                throw new IllegalArgumentException("MTU must be greater than 1280");
            }
            mProfile.maxMtu = mtu;
            return this;
        }

        /**
         * Marks the VPN network as metered.
         *
         * <p>A VPN network is classified as metered when the user is
         * sensitive to heavy data usage due to monetary costs and/or data limitations. In such
         * cases, you should set this to {@code true} so that apps on the system can avoid doing
         * large data transfers. Otherwise, set this to {@code false}. Doing so would cause VPN
         * network to inherit its meteredness from the underlying network.
         *
         * @param isMetered {@code true} if the VPN network should be treated as metered regardless
         *     of underlying network meteredness
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         * @see ConnectivityManager#isActiveNetworkMetered()
         */
        @NonNull
        public Ikev2VpnProfileBuilder setMetered(boolean isMetered) {
            mProfile.isMetered = isMetered;
            return this;
        }

        /**
         * Sets the allowable set of IPsec algorithms
         *
         * <p>A list of allowed IPsec algorithms as defined in {@link IpSecAlgorithm}
         *
         * @param algorithmNames the list of supported IPsec algorithms
         * @return this {@link Ikev2VpnProfileBuilder} object to facilitate chaining of method calls.
         * @see IpSecAlgorithm
         */
        @NonNull
        public Ikev2VpnProfileBuilder setAllowedAlgorithms(@NonNull List<String> algorithmNames) {
            checkNotNull(algorithmNames, "Required parameter was null: algorithmNames");

            mProfile.allowedAlgorithms = algorithmNames;
            return this;
        }

        /**
         * Validates and builds the VpnProfile instance
         *
         * @return a VpnProfile instance representing the current state of the {@link Ikev2VpnProfileBuilder}
         */
        @NonNull
        public VpnProfile build(){
            mProfile.validateIkev2Ipsec();

            try{
                return mProfile.clone();
            } catch (CloneNotSupportedException e){
                throw new IllegalStateException("Internal error - clone not supported");
            }
        }
    }
}
