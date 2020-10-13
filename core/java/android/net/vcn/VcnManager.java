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
package android.net.vcn;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.SystemService;
import android.content.Context;

/**
 * VcnManager publishes APIs for carrier apps to manage Virtual Carrier Networks (VCNs)
 *
 * <p>A VCN virtualizes an Carrier's network by building tunnels to a carrier's core network over
 * carrier-managed physical links, and supports a IP mobility layer to ensure seamless transitions
 * between the underlying networks. Each VCN is configured based on a Subscription Group (see {@link
 * SubscriptionManager}), and aggregates all networks that are brought up based on a profile or
 * suggestion in the specified Subscription Group.
 *
 * <p>The VCN can be configured to expose one or more Networks, each with different capabilities,
 * allowing for APN virtualization.
 *
 * <p>Upon failure, the VCN will attempt to retry a number of times. If a tunnel fails to connect
 * after a system-determined timeout, the VCN Safe Mode (see below) will be entered.
 *
 * <p>The VCN Safe Mode ensures users (and carriers) have a fallback to restore system connectivity
 * to update profiles, diagnose issues, contact support, or perform other remediation tasks. In Safe
 * Mode, the system will allow underlying cellular networks to be used as default. Additionally,
 * during Safe Mode, the VCN will continue to retry the connections, and will automatically exit
 * Safe Mode if all active tunnels connect successfully.
 */
@SystemService(Context.VCN_MANAGEMENT_SERVICE)
public final class VcnManager {
    @NonNull private static final String TAG = VcnManager.class.getSimpleName();

    @NonNull private final Context mContext;
    @NonNull private final IVcnManagementService mService;

    /**
     * Construct an instance of VcnManager within an application context.
     *
     * @param ctx the application context for this manager
     * @param service the VcnManagementService binder backing this manager
     *
     * @hide
     */
    public VcnManager(@NonNull Context ctx, @NonNull IVcnManagementService service) {
        mContext = requireNonNull(ctx, "missing context");
        mService = requireNonNull(service, "missing service");
    }
}
