package android.net.shared;

import android.net.INetd;
import android.net.Network;
import android.net.ProvisioningConfigurationParcelable;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;

import java.util.StringJoiner;

/**
 * This class encapsulates parameters to be passed to
 * IpClient#startProvisioning(). A defensive copy is made by IpClient
 * and the values specified herein are in force until IpClient#stop()
 * is called.
 *
 * Example use:
 *
 *     final ProvisioningConfiguration config =
 *             mIpClient.buildProvisioningConfiguration()
 *                     .withPreDhcpAction()
 *                     .withProvisioningTimeoutMs(36 * 1000)
 *                     .build();
 *     mIpClient.startProvisioning(config);
 *     ...
 *     mIpClient.stop();
 *
 * The specified provisioning configuration will only be active until
 * IpClient#stop() is called. Future calls to IpClient#startProvisioning()
 * must specify the configuration again.
 * @hide
 */
public class ProvisioningConfiguration {
    // TODO: Delete this default timeout once those callers that care are
    // fixed to pass in their preferred timeout.
    //
    // We pick 36 seconds so we can send DHCP requests at
    //
    //     t=0, t=2, t=6, t=14, t=30
    //
    // allowing for 10% jitter.
    private static final int DEFAULT_TIMEOUT_MS = 36 * 1000;

    public static class Builder {
        private ProvisioningConfiguration mConfig = new ProvisioningConfiguration();

        public Builder withoutIPv4() {
            mConfig.mEnableIPv4 = false;
            return this;
        }

        public Builder withoutIPv6() {
            mConfig.mEnableIPv6 = false;
            return this;
        }

        public Builder withoutMultinetworkPolicyTracker() {
            mConfig.mUsingMultinetworkPolicyTracker = false;
            return this;
        }

        public Builder withoutIpReachabilityMonitor() {
            mConfig.mUsingIpReachabilityMonitor = false;
            return this;
        }

        public Builder withPreDhcpAction() {
            mConfig.mRequestedPreDhcpActionMs = DEFAULT_TIMEOUT_MS;
            return this;
        }

        public Builder withPreDhcpAction(int dhcpActionTimeoutMs) {
            mConfig.mRequestedPreDhcpActionMs = dhcpActionTimeoutMs;
            return this;
        }

        public Builder withInitialConfiguration(InitialConfiguration initialConfig) {
            mConfig.mInitialConfig = initialConfig;
            return this;
        }

        public Builder withStaticConfiguration(StaticIpConfiguration staticConfig) {
            mConfig.mStaticIpConfig = staticConfig;
            return this;
        }

        public Builder withApfCapabilities(ApfCapabilities apfCapabilities) {
            mConfig.mApfCapabilities = apfCapabilities;
            return this;
        }

        public Builder withProvisioningTimeoutMs(int timeoutMs) {
            mConfig.mProvisioningTimeoutMs = timeoutMs;
            return this;
        }

        public Builder withRandomMacAddress() {
            mConfig.mIPv6AddrGenMode = INetd.IPV6_ADDR_GEN_MODE_EUI64;
            return this;
        }

        public Builder withStableMacAddress() {
            mConfig.mIPv6AddrGenMode = INetd.IPV6_ADDR_GEN_MODE_STABLE_PRIVACY;
            return this;
        }

        public Builder withNetwork(Network network) {
            mConfig.mNetwork = network;
            return this;
        }

        public Builder withDisplayName(String displayName) {
            mConfig.mDisplayName = displayName;
            return this;
        }

        public ProvisioningConfiguration build() {
            return new ProvisioningConfiguration(mConfig);
        }
    }

    public boolean mEnableIPv4 = true;
    public boolean mEnableIPv6 = true;
    public boolean mUsingMultinetworkPolicyTracker = true;
    public boolean mUsingIpReachabilityMonitor = true;
    public int mRequestedPreDhcpActionMs;
    public InitialConfiguration mInitialConfig;
    public StaticIpConfiguration mStaticIpConfig;
    public ApfCapabilities mApfCapabilities;
    public int mProvisioningTimeoutMs = DEFAULT_TIMEOUT_MS;
    public int mIPv6AddrGenMode = INetd.IPV6_ADDR_GEN_MODE_STABLE_PRIVACY;
    public Network mNetwork = null;
    public String mDisplayName = null;

    public ProvisioningConfiguration() {} // used by Builder

    public ProvisioningConfiguration(ProvisioningConfiguration other) {
        mEnableIPv4 = other.mEnableIPv4;
        mEnableIPv6 = other.mEnableIPv6;
        mUsingMultinetworkPolicyTracker = other.mUsingMultinetworkPolicyTracker;
        mUsingIpReachabilityMonitor = other.mUsingIpReachabilityMonitor;
        mRequestedPreDhcpActionMs = other.mRequestedPreDhcpActionMs;
        mInitialConfig = InitialConfiguration.copy(other.mInitialConfig);
        mStaticIpConfig = other.mStaticIpConfig;
        mApfCapabilities = other.mApfCapabilities;
        mProvisioningTimeoutMs = other.mProvisioningTimeoutMs;
        mIPv6AddrGenMode = other.mIPv6AddrGenMode;
        mNetwork = other.mNetwork;
        mDisplayName = other.mDisplayName;
    }

    public ProvisioningConfigurationParcelable toStableParcelable() {
        final ProvisioningConfigurationParcelable p = new ProvisioningConfigurationParcelable();
        p.enableIPv4 = mEnableIPv4;
        p.enableIPv6 = mEnableIPv6;
        p.usingMultinetworkPolicyTracker = mUsingMultinetworkPolicyTracker;
        p.usingIpReachabilityMonitor = mUsingIpReachabilityMonitor;
        p.requestedPreDhcpActionMs = mRequestedPreDhcpActionMs;
        p.staticIpConfig = IpConfigurationParcelableUtil.toStableParcelable(mStaticIpConfig);
        p.provisioningTimeoutMs = mProvisioningTimeoutMs;
        p.ipv6AddrGenMode = mIPv6AddrGenMode;
        p.network = NetworkParcelableUtil.toStableParcelable(mNetwork);
        p.displayName = mDisplayName;
        return p;
    }

    public static ProvisioningConfiguration fromStableParcelable(
            ProvisioningConfigurationParcelable p) {
        final ProvisioningConfiguration config = new ProvisioningConfiguration();
        config.mEnableIPv4 = p.enableIPv4;
        config.mEnableIPv6 = p.enableIPv6;
        config.mUsingMultinetworkPolicyTracker = p.usingMultinetworkPolicyTracker;
        config.mUsingIpReachabilityMonitor = p.usingIpReachabilityMonitor;
        config.mRequestedPreDhcpActionMs = p.requestedPreDhcpActionMs;
        config.mStaticIpConfig = IpConfigurationParcelableUtil.fromStableParcelable(
                p.staticIpConfig);
        config.mProvisioningTimeoutMs = p.provisioningTimeoutMs;
        config.mIPv6AddrGenMode = p.ipv6AddrGenMode;
        config.mNetwork = NetworkParcelableUtil.fromStableParcelable(p.network);
        config.mDisplayName = p.displayName;
        return config;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "{", "}")
                .add("mEnableIPv4: " + mEnableIPv4)
                .add("mEnableIPv6: " + mEnableIPv6)
                .add("mUsingMultinetworkPolicyTracker: " + mUsingMultinetworkPolicyTracker)
                .add("mUsingIpReachabilityMonitor: " + mUsingIpReachabilityMonitor)
                .add("mRequestedPreDhcpActionMs: " + mRequestedPreDhcpActionMs)
                .add("mInitialConfig: " + mInitialConfig)
                .add("mStaticIpConfig: " + mStaticIpConfig)
                .add("mApfCapabilities: " + mApfCapabilities)
                .add("mProvisioningTimeoutMs: " + mProvisioningTimeoutMs)
                .add("mIPv6AddrGenMode: " + mIPv6AddrGenMode)
                .add("mNetwork: " + mNetwork)
                .add("mDisplayName: " + mDisplayName)
                .toString();
    }

    public boolean isValid() {
        return (mInitialConfig == null) || mInitialConfig.isValid();
    }
}
