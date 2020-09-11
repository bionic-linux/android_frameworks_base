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

package com.android.networkstack.tethering;

import static android.net.netlink.NetlinkSocket.DEFAULT_RECV_BUFSIZE;
import static android.net.netlink.StructNlMsgHdr.NLM_F_DUMP;
import static android.net.netlink.StructNlMsgHdr.NLM_F_REQUEST;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.net.netlink.StructNlMsgHdr;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.NativeHandle;
import android.system.Os;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class OffloadHardwareInterfaceTest {
    // Reference kernel/uapi/linux/netfilter/nfnetlink_compat.h
    private static final int NF_NETLINK_CONNTRACK_NEW = 1;
    private static final int NF_NETLINK_CONNTRACK_DESTROY = 4;
    // Reference libnetfilter_conntrack/linux_nfnetlink_conntrack.h
    public static final short NFNL_SUBSYS_CTNETLINK = 1;
    public static final short IPCTNL_MSG_CT_NEW = 0;
    public static final short IPCTNL_MSG_CT_GET = 1;

    private static final long TIMEOUT = 500;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private final SharedLog mLog = new SharedLog("privileged-test");;

    //private final TestLooper mTestLooper = new TestLooper();

    private OffloadHardwareInterface mOffloadHw;
    private OffloadHardwareInterface.Dependencies mDeps;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mHandlerThread = new HandlerThread(getClass().getSimpleName());
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        // Looper must be prepared here since AndroidJUnitRunner runs tests on separate threads.
        if (Looper.myLooper() == null) Looper.prepare();

        mDeps = new OffloadHardwareInterface.Dependencies(mLog);
        mOffloadHw = new OffloadHardwareInterface(mHandler, mLog, mDeps);
    }

    @Test
    public void testConntrackSocket() throws Exception {
        // set up server and connect
        InetSocketAddress anyAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(anyAddress);
        SocketAddress theAddress = serverSocket.getLocalSocketAddress();

        // make a connection to the server, then close the server
        Socket socket = new Socket();
        socket.connect(theAddress);
        Socket stillActiveSocket = serverSocket.accept();

        final NativeHandle handle = mDeps.createConntrackSocket(
                NF_NETLINK_CONNTRACK_NEW | NF_NETLINK_CONNTRACK_DESTROY);
        mOffloadHw.sendNetlinkMessage(handle,
                (short) ((NFNL_SUBSYS_CTNETLINK << 8) | IPCTNL_MSG_CT_GET),
                (short) (NLM_F_REQUEST | NLM_F_DUMP));

        boolean conntrack_entry = false;
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_RECV_BUFSIZE);
        while (Os.read(handle.getFileDescriptor(), buffer) > 0) {
            buffer.flip();
            buffer.order(ByteOrder.nativeOrder());

            final StructNlMsgHdr nlmsghdr = StructNlMsgHdr.parse(buffer);
            assertNotNull(nlmsghdr);

            // As long as 1 conntrack entry is found test case will pass, even if it's not
            // the from the TCP connection above
            if (nlmsghdr.nlmsg_type == ((NFNL_SUBSYS_CTNETLINK << 8) | IPCTNL_MSG_CT_NEW)) {
                conntrack_entry = true;
                break;
            }
        }
        socket.close();
        serverSocket.close();

        assertTrue(conntrack_entry);
    }
}
