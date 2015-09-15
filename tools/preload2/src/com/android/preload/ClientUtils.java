/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.preload;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;

public class ClientUtils {

    public static Client findClient(IDevice device, String processName, int processPid) {
        WaitForClient wfc = new WaitForClient(device, processName, processPid, 10000);
        return wfc.get();
    }

    public static Client[] findAllClients(IDevice device) {
        if (device.hasClients()) {
            return device.getClients();
        }
        WaitForClients wfc = new WaitForClients(device, 10000);
        return wfc.get();
    }

    private static class WaitForClient implements IClientChangeListener {

        private IDevice device;
        private String processName;
        private int processPid;
        private long timeout;
        private Client result;

        public WaitForClient(IDevice device, String processName, int processPid, long timeout) {
            this.device = device;
            this.processName = processName;
            this.processPid = processPid;
            this.timeout = timeout;
            this.result = null;
        }

        public Client get() {
            synchronized (this) {
                AndroidDebugBridge.addClientChangeListener(this);

                // Maybe it's already there.
                checkDevice(device);

                if (result == null) {
                    try {
                        wait(timeout); // Note: doesn't guard for spurious wakeup.
                    } catch (Exception exc) {
                    }
                }
            }

            AndroidDebugBridge.removeClientChangeListener(this);
            return result;
        }

        private void checkDevice(IDevice device) {
            Client[] clients = device.getClients();
            if (processName != null) {
                result = device.getClient(processName);
            }
            if (result == null && processPid > 0) {
                String name = device.getClientName(processPid);
                if (name != null && !name.isEmpty()) {
                    result = device.getClient(name);
                }
            }
            if (result == null && processPid > 0) {
                // Try manual search.
                for (Client cl : clients) {
                    if (cl.getClientData().getPid() == processPid
                            && cl.getClientData().getClientDescription() != null) {
                        result = cl;
                        break;
                    }
                }
            }
        }

        private boolean matches(Client c) {
            if (processPid > 0 && c.getClientData().getPid() == processPid) {
                return true;
            }
            if (processName != null
                    && processName.equals(c.getClientData().getClientDescription())) {
                return true;
            }
            return false;
        }

        @Override
        public void clientChanged(Client arg0, int arg1) {
            synchronized (this) {
                if ((arg1 & Client.CHANGE_INFO) != 0 && (arg0.getDevice() == device)) {
                    if (matches(arg0)) {
                        result = arg0;
                        notifyAll();
                    }
                }
            }
        }
    }

    private static class WaitForClients implements IClientChangeListener {

        private IDevice device;
        private long timeout;

        public WaitForClients(IDevice device, long timeout) {
            this.device = device;
            this.timeout = timeout;
        }

        public Client[] get() {
            synchronized (this) {
                AndroidDebugBridge.addClientChangeListener(this);

                if (device.hasClients()) {
                    return device.getClients();
                }

                try {
                    wait(timeout); // Note: doesn't guard for spurious wakeup.
                } catch (Exception exc) {
                }

                // Sleep a little bit longer, as we wake up after the first client info.
                try {
                    Thread.sleep(500);
                } catch (Exception exc) {
                }
            }

            AndroidDebugBridge.removeClientChangeListener(this);

            return device.getClients();
        }

        @Override
        public void clientChanged(Client arg0, int arg1) {
            synchronized (this) {
                if ((arg1 & Client.CHANGE_INFO) != 0 && (arg0.getDevice() == device)) {
                    notifyAll();
                }
            }
        }
    }
}
