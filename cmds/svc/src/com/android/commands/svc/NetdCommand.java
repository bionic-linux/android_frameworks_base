/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.commands.svc;

import android.content.Context;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.IBinder;

public class NetdCommand extends Svc.Command {
    public NetdCommand() {
        super("netd");
    }

    public String shortHelp() {
        return "set dns information";
    }

    public String longHelp() {
        return shortHelp() + "\n"
                + "\n"
                + "usage: svc netd setdefaultif iface\n"
                + "          set default interface for dns\n"
                + "       svc netd setifdns iface nameserver\n"
                + "          set nameserver for interface\n"
                + "       svc netd setdefaultifdns iface nameserver\n"
                + "          set the default interface for dns and nameserver for it\n";
    }

    public void run(String[] args) {
        try {
            if (args.length >= 2) {
                IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                INetworkManagementService netdService = INetworkManagementService.Stub.asInterface(b);
                if ("setdefaultif".equals(args[1]) && args.length == 3) {
                    netdService.setDefaultInterfaceForDns(args[2]);
                    return;
                } else if ("setifdns".equals(args[1]) && args.length == 4) {
                    netdService.setDnsServersForInterface(args[2], new String[]{args[3]}, null);
                    return;
                } else if ("setdefaultifdns".equals(args[1]) && args.length == 4) {
                    netdService.setDefaultInterfaceForDns(args[2]);
                    netdService.setDnsServersForInterface(args[2], new String[]{args[3]}, null);
                    return;
                } else {
                    System.err.println(longHelp());
                    return;
                }
            }
        }catch(Exception e){
            System.err.println(e);
        }
        System.err.println(longHelp());
    }
}
