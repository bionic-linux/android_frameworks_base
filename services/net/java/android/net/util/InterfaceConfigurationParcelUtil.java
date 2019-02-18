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

package android.net.util;

import android.net.InetAddresses;
import android.net.InterfaceConfiguration;
import android.net.InterfaceConfigurationParcel;
import android.net.LinkAddress;
import android.text.TextUtils;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Utility methods to convert between {@link InterfaceConfiguration} and
 * {@link InterfaceConfigurationParcel}.
 * @hide
 */
public final class InterfaceConfigurationParcelUtil {
    /**
     * Convert from an {@link InterfaceConfigurationParcel} to an {@link InterfaceConfiguration}.
     */
    public static InterfaceConfiguration fromParcel(InterfaceConfigurationParcel p) {
        InterfaceConfiguration cfg = new InterfaceConfiguration();
        cfg.setHardwareAddress(p.hwAddr);

        final InetAddress addr = InetAddresses.parseNumericAddress(p.ipv4Addr);
        cfg.setLinkAddress(new LinkAddress(addr, p.prefixLength));
        for (String flag : p.flags) {
            cfg.setFlag(flag);
        }

        return cfg;
    }

    /**
     * Convert from an {@link InterfaceConfiguration} to an {@link InterfaceConfigurationParcel}.
     */
    public static InterfaceConfigurationParcel toParcel(InterfaceConfiguration cfg, String iface) {
        InterfaceConfigurationParcel cfgParcel = new InterfaceConfigurationParcel();
        cfgParcel.ifName = iface;
        if (!TextUtils.isEmpty(cfg.getHardwareAddress())) {
            cfgParcel.hwAddr = cfg.getHardwareAddress();
        } else {
            cfgParcel.hwAddr = "";
        }
        cfgParcel.ipv4Addr = cfg.getLinkAddress().getAddress().getHostAddress();
        cfgParcel.prefixLength = cfg.getLinkAddress().getPrefixLength();

        final ArrayList<String> flags = new ArrayList<>();
        cfg.getFlags().forEach(flags::add);
        cfgParcel.flags = new String[flags.size()];
        cfgParcel.flags = flags.toArray(cfgParcel.flags);

        return cfgParcel;
    }


    private InterfaceConfigurationParcelUtil() {}
}
