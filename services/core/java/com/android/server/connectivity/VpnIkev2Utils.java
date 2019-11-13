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

package com.android.server.connectivity;

import static android.Manifest.permission.BIND_VPN_SERVICE;
import static android.net.ConnectivityManager.NETID_UNSET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING;
import static android.net.RouteInfo.RTN_THROW;
import static android.net.RouteInfo.RTN_UNREACHABLE;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.Ikev2VpnProfile;
import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.IpSecManager;
import android.net.IpSecManager.IpSecTunnelInterface;
import android.net.IpSecManager.UdpEncapsulationSocket;
import android.net.IpSecTransform;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkProvider;
import android.net.NetworkRequest;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.VpnManager;
import android.net.VpnService;
import android.net.eap.EapSessionConfig;
import android.net.ipsec.ike.ChildSaProposal;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.ChildSessionConfiguration;
import android.net.ipsec.ike.ChildSessionParams;
import android.net.ipsec.ike.IkeFqdnIdentification;
import android.net.ipsec.ike.IkeIdentification;
import android.net.ipsec.ike.IkeIpv4AddrIdentification;
import android.net.ipsec.ike.IkeIpv6AddrIdentification;
import android.net.ipsec.ike.IkeKeyIdIdentification;
import android.net.ipsec.ike.IkeRfc822AddrIdentification;
import android.net.ipsec.ike.IkeSaProposal;
import android.net.ipsec.ike.IkeSession;
import android.net.ipsec.ike.IkeSessionCallback;
import android.net.ipsec.ike.IkeSessionConfiguration;
import android.net.ipsec.ike.IkeSessionParams;
import android.net.ipsec.ike.IkeTrafficSelector;
import android.net.ipsec.ike.SaProposal;
import android.net.ipsec.ike.TunnelModeChildSessionParams;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.util.IpRange;
import android.os.Binder;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemService;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.messages.nano.SystemMessageProto.SystemMessage;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.HexDump;
import com.android.server.ConnectivityService;
import com.android.server.DeviceIdleController;
import com.android.server.LocalServices;
import com.android.server.net.BaseNetworkObserver;

import libcore.io.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class to build and convert IKEv2/IPsec parameters.
 *
 * @hide
 */
public class VpnIkev2Utils {
    static IkeSessionParams buildIkeSessionParams(@NonNull Ikev2VpnProfile profile,
            @NonNull UdpEncapsulationSocket socket) throws UnknownHostException {
        // TODO: Give IKE the string hostname/IP literal, and let it do DNS resolution.
        final InetAddress serverAddr = InetAddress.getByName(profile.getServerAddr());
        final IkeIdentification localId = parseIkeIdentification(profile.getUserIdentity());
        final IkeIdentification remoteId = parseIkeIdentification(profile.getServerAddr());

        final IkeSessionParams.Builder ikeOptionsBuilder =
                new IkeSessionParams.Builder()
                        .setServerAddress(serverAddr)
                        .setUdpEncapsulationSocket(socket)
                        .setLocalIdentification(localId)
                        .setRemoteIdentification(remoteId);
        setIkeAuth(profile, ikeOptionsBuilder);

        for (final IkeSaProposal ikeProposal : getIkeSaProposals()) {
            ikeOptionsBuilder.addSaProposal(ikeProposal);
        }

        return ikeOptionsBuilder.build();
    }

    static ChildSessionParams buildChildSessionParams() {
        final TunnelModeChildSessionParams.Builder childOptionsBuilder =
                new TunnelModeChildSessionParams.Builder();

        for (final ChildSaProposal childProposal : getChildSaProposals()) {
            childOptionsBuilder.addSaProposal(childProposal);
        }

        childOptionsBuilder.addInternalAddressRequest(OsConstants.AF_INET);
        childOptionsBuilder.addInternalAddressRequest(OsConstants.AF_INET6);
        childOptionsBuilder.addInternalDnsServerRequest(OsConstants.AF_INET);
        childOptionsBuilder.addInternalDnsServerRequest(OsConstants.AF_INET6);

        return childOptionsBuilder.build();
    }

    private static void setIkeAuth(@NonNull Ikev2VpnProfile profile,
            @NonNull IkeSessionParams.Builder builder) {
        switch (profile.getType()) {
            case VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS:
                final EapSessionConfig eapConfig =
                        new EapSessionConfig.Builder()
                                .setEapMsChapV2Config(
                                        profile.getUsername(), profile.getPassword())
                                .build();
                builder.setAuthEap(profile.getServerRootCaCert(), eapConfig);
                break;
            case VpnProfile.TYPE_IKEV2_IPSEC_PSK:
                builder.setAuthPsk(profile.getPresharedKey());
                break;
            case VpnProfile.TYPE_IKEV2_IPSEC_RSA:
                builder.setAuthDigitalSignature(
                        profile.getServerRootCaCert(),
                        profile.getUserCert(),
                        profile.getRsaPrivateKey());
                break;
            default:
                throw new IllegalArgumentException("Unknown auth method set");
        }
    }

    private static List<IkeSaProposal> getIkeSaProposals() {
        // TODO: filter this based on allowedAlgorithms
        final List<IkeSaProposal> proposals = new ArrayList<>();

        // Encryption Algorithms: Currently only AES_CBC is supported.
        final IkeSaProposal.Builder normalModeBuilder = new IkeSaProposal.Builder();

        // Currently only AES_CBC is supported.
        normalModeBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, SaProposal.KEY_LEN_AES_128);
        normalModeBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, SaProposal.KEY_LEN_AES_192);
        normalModeBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, SaProposal.KEY_LEN_AES_256);

        // Authentication/Integrity Algorithms
        normalModeBuilder.addIntegrityAlgorithm(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96);
        normalModeBuilder.addIntegrityAlgorithm(SaProposal.INTEGRITY_ALGORITHM_AES_XCBC_96);
        normalModeBuilder.addIntegrityAlgorithm(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128);
        normalModeBuilder.addIntegrityAlgorithm(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_384_192);
        normalModeBuilder.addIntegrityAlgorithm(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_512_256);

        // Add AEAD options
        final IkeSaProposal.Builder aeadBuilder = new IkeSaProposal.Builder();
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_128);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_192);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_256);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12, SaProposal.KEY_LEN_AES_128);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12, SaProposal.KEY_LEN_AES_192);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12, SaProposal.KEY_LEN_AES_256);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16, SaProposal.KEY_LEN_AES_128);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16, SaProposal.KEY_LEN_AES_192);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16, SaProposal.KEY_LEN_AES_256);

        // Add dh, prf for both builders
        for (final IkeSaProposal.Builder builder :
                Arrays.asList(normalModeBuilder, aeadBuilder)) {
            for (final int dh : Arrays.asList(
                    SaProposal.DH_GROUP_2048_BIT_MODP,
                    SaProposal.DH_GROUP_1024_BIT_MODP)) {
                builder.addDhGroup(dh);
            }
            for (final int prf : Arrays.asList(
                    SaProposal.PSEUDORANDOM_FUNCTION_AES128_XCBC,
                    SaProposal.PSEUDORANDOM_FUNCTION_HMAC_SHA1)) {
                builder.addPseudorandomFunction(prf);
            }
        }

        proposals.add(normalModeBuilder.build());
        proposals.add(aeadBuilder.build());
        return proposals;
    }

    private static List<ChildSaProposal> getChildSaProposals() {
        // TODO: filter this based on allowedAlgorithms
        final List<ChildSaProposal> proposals = new ArrayList<>();

        // Add non-AEAD options
        final ChildSaProposal.Builder normalModeBuilder = new ChildSaProposal.Builder();

        // Encryption Algorithms: Currently only AES_CBC is supported.
        normalModeBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, SaProposal.KEY_LEN_AES_128);
        normalModeBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, SaProposal.KEY_LEN_AES_192);
        normalModeBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, SaProposal.KEY_LEN_AES_256);

        // Authentication/Integrity Algorithms
        normalModeBuilder.addIntegrityAlgorithm(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96);
        normalModeBuilder.addIntegrityAlgorithm(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128);
        normalModeBuilder.addIntegrityAlgorithm(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_384_192);
        normalModeBuilder.addIntegrityAlgorithm(
                SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_512_256);

        // Add AEAD options
        final ChildSaProposal.Builder aeadBuilder = new ChildSaProposal.Builder();
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_128);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_192);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_256);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12, SaProposal.KEY_LEN_AES_128);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12, SaProposal.KEY_LEN_AES_192);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12, SaProposal.KEY_LEN_AES_256);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16, SaProposal.KEY_LEN_AES_128);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16, SaProposal.KEY_LEN_AES_192);
        aeadBuilder.addEncryptionAlgorithm(
                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16, SaProposal.KEY_LEN_AES_256);

        proposals.add(normalModeBuilder.build());
        proposals.add(aeadBuilder.build());
        return proposals;
    }

    /** Identity parsing logic using similar logic to open source implementations of IKEv2 */
    private static IkeIdentification parseIkeIdentification(@NonNull String identityStr) {
        // TODO: Add identity formatting to public API javadocs.
        if (identityStr.contains("@")) {
            if (identityStr.startsWith("@#")) {
                // KEY_ID
                final String hexStr = identityStr.substring(2);
                return new IkeKeyIdIdentification(HexDump.hexStringToByteArray(hexStr));
            } else if (identityStr.startsWith("@") && !identityStr.startsWith("@@")) {
                // FQDN
                return new IkeFqdnIdentification(identityStr.substring(1));
            } else if (identityStr.startsWith("@@")) {
                // RFC822 (USER_FQDN)
                return new IkeRfc822AddrIdentification(identityStr.substring(2));
            } else {
                // RFC822 (USER_FQDN)
                return new IkeRfc822AddrIdentification(identityStr);
            }
        } else {
            if (InetAddresses.isNumericAddress(identityStr)) {
                final InetAddress addr =
                        InetAddresses.parseNumericAddress(identityStr);
                if (addr instanceof Inet4Address) {
                    // IPv4
                    return new IkeIpv4AddrIdentification((Inet4Address) addr);
                } else if (addr instanceof Inet6Address) {
                    // IPv6
                    return new IkeIpv6AddrIdentification((Inet6Address) addr);
                } else {
                    throw new IllegalArgumentException("IP version not supported");
                }
            } else {
                if (identityStr.contains(":")) {
                    // KEY_ID
                    return new IkeKeyIdIdentification(identityStr.getBytes());
                } else {
                    // FQDN
                    return new IkeFqdnIdentification(identityStr);
                }
            }
        }
    }

    static Collection<RouteInfo> getRoutesFromTrafficSelectors(
            List<IkeTrafficSelector> trafficSelectors) {
        final HashSet<IpPrefix> prefixes = new HashSet<>();
        final HashSet<RouteInfo> routes = new HashSet<>();

        for (final IkeTrafficSelector selector : trafficSelectors) {
            for (final IpPrefix prefix :
                    new IpRange(selector.startingAddress, selector.endingAddress)
                            .asIpPrefixes()) {
                routes.add(new RouteInfo(prefix, null));
            }
        }

        return routes;
    }
}