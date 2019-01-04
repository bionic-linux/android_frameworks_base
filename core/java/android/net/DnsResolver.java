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

import static android.net.NetworkUtils.resNetworkQuery;
import static android.net.NetworkUtils.resNetworkResult;
import static android.net.NetworkUtils.resNetworkSend;
import static android.os.MessageQueue.OnFileDescriptorEventListener.EVENT_ERROR;
import static android.os.MessageQueue.OnFileDescriptorEventListener.EVENT_INPUT;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.system.ErrnoException;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.io.FileDescriptor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


/**
 * Dns resolver class for asynchronous dns querying
 *
 */
public final class DnsResolver {
    private static final String TAG = "DnsResolver";
    private static final int FD_EVENTS = EVENT_INPUT | EVENT_ERROR;
    private static final int MAXPACKET = 8 * 1024;

    @IntDef(prefix = { "CLASS_" }, value = {
            CLASS_IN
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface QueryClass {}
    public static final int CLASS_IN = 1;

    @IntDef(prefix = { "TYPE_" },  value = {
            TYPE_A,
            TYPE_AAAA
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface QueryType {}
    public static final int TYPE_A = 1;
    public static final int TYPE_AAAA = 28;

    @IntDef(prefix = { "FLAG_" }, value = {
            FLAG_EMPTY,
            FLAG_NO_RETRY,
            FLAG_NO_CACHE_STORE,
            FLAG_NO_CACHE_LOOKUP
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface QueryFlag {}
    public static final int FLAG_EMPTY = 0;
    public static final int FLAG_NO_RETRY = 1 << 0;
    public static final int FLAG_NO_CACHE_STORE = 1 << 1;
    public static final int FLAG_NO_CACHE_LOOKUP = 1 << 2;

    private static final int DNS_RAW_RESPONSE = 1;

    private static final int NETID_UNSET = 0;

    private static final DnsResolver sInstance = new DnsResolver();

    /**
     * Callback for receiving raw answers
     */
    public interface RawAnswer {
        /**
         * |answer| is Null if query timed out
         */
        void onAnswer(byte[] answer);
    }

    /**
     * Callback for receiving parsed answers
     */
    public interface InetAddressAnswer {
        /**
         * It will be called after both v4 and v6 are parsed done.
         * size of addresses will be zero if no available answer could be parsed.
         */
        void onAnswer(List<InetAddress> addresses);
    }

    /**
     * Get instanse for DnsResolver
     */
    public static DnsResolver getInstance() {
        return sInstance;
    }

    private DnsResolver() {}

    /**
     * Pass in a blob and corresponding setting,
     * get a blob back asynchronously with the entire raw answer.
     *
     * @param network {@link Network} specifying which network for querying.
     *         Null for query on default network.
     * @param query blob message
     * @param flags flags which is used in asynchronous DNS API
     * @param handler {@link Handler} to specify the thread
     *         upon which the callback will be invoked.
     * @param callback a {@link RawAnswer} which will be called to notify the caller
     *         of the result of dns query.
     */
    public void query(Network network, @NonNull byte[] query, @QueryFlag int flags,
            @NonNull Handler handler, @NonNull RawAnswer callback) throws ErrnoException {
        FileDescriptor queryfd = resNetworkSend((network != null ? network.netId : NETID_UNSET),
                query, query.length, flags);
        registerFDListener(handler.getLooper().getQueue(), queryfd, callback);
    }

    /**
     * Pass in a domain name and corresponding setting,
     * get a blob back asynchronously with the entire raw answer.
     *
     * @param network {@link Network} specifying which network for querying.
     *         Null for query on default network.
     * @param domain domain name for querying
     * @param nsClass dns class for querying
     * @param nsType dns resource record (RR) type for querying
     * @param flags flags which is used in asynchronous DNS API
     * @param handler {@link Handler} to specify the thread
     *         upon which the callback will be invoked.
     * @param callback a {@link RawAnswer} which will be called to notify the caller
     *         of the result of dns query.
     */
    public void query(Network network, @NonNull String domain, @QueryClass int nsClass,
            @QueryType int nsType, @QueryFlag int flags,
            @NonNull Handler handler, @NonNull RawAnswer callback) throws ErrnoException {
        FileDescriptor queryfd = resNetworkQuery((network != null ? network.netId : NETID_UNSET),
                domain, nsClass, nsType, flags);
        registerFDListener(handler.getLooper().getQueue(), queryfd, callback);
    }

    /**
     * Pass in a domain name and corresponding setting,
     * get back a set of InetAddresses asynchronously.
     *
     * @param network {@link Network} specifying which network for querying.
     *         Null for query on default network.
     * @param domain domain name for querying
     * @param flags flags which is used in asynchronous DNS API
     * @param handler {@link Handler} to specify the thread
     *         upon which the callback will be invoked.
     * @param callback an {@link InetAddressAnswer} which will be called to notify the caller
     *         of the result of dns query.
     */
    public void query(Network network, String domain, @QueryFlag int flags,
            @NonNull Handler handler, @NonNull InetAddressAnswer callback)
            throws ErrnoException {
        FileDescriptor v4fd = resNetworkQuery((network != null ? network.netId : NETID_UNSET),
                domain, CLASS_IN, TYPE_A, flags);
        FileDescriptor v6fd = resNetworkQuery((network != null ? network.netId : NETID_UNSET),
                domain, CLASS_IN, TYPE_AAAA, flags);

        ResolverHandler resolverHandler = new ResolverHandler(handler, callback);

        registerFDListener(resolverHandler, v4fd);
        registerFDListener(resolverHandler, v6fd);
    }

    private void registerFDListener(MessageQueue queue, FileDescriptor queryfd, RawAnswer answer) {
        queue.addOnFileDescriptorEventListener(
                queryfd,
                FD_EVENTS,
                (fd, events) -> {
                    byte[] answerbuf = null;
                    try {
                    // TODO: Implement result function in Java side instead of using JNI
                    //       Because JNI method close fd prior than unregistering fd on
                    //       event listener.
                        answerbuf = resNetworkResult(fd);
                    } catch (ErrnoException e) {
                        Log.e(TAG, "resNetworkResult:" + e.toString());
                    }
                    answer.onAnswer(answerbuf);

                    // Unregister this fd listener
                    return 0;
                });
    }

    private void registerFDListener(ResolverHandler handler, FileDescriptor queryfd) {
        handler.getLooper().getQueue().addOnFileDescriptorEventListener(
                queryfd,
                FD_EVENTS,
                (fd, events) -> {
                    byte[] answerbuf = null;
                    try {
                    // TODO: Implement result function in Java side instead of using JNI
                    //       Because JNI method close fd prior than unregistering fd on
                    //       event listener.
                        answerbuf = resNetworkResult(fd);
                    } catch (ErrnoException e) {
                        Log.e(TAG, "resNetworkResult:" + e.toString());
                    }

                    Message message = Message.obtain();
                    message.obj = answerbuf;
                    message.what = DNS_RAW_RESPONSE;
                    handler.sendMessage(message);

                    // Unregister this fd listener
                    return 0;
                });
    }

    private class DnsAddressAnswer extends DnsPacket {
        private static final String TAG = "DnsResolver.DnsAddressAnswer";
        private static final boolean DBG = false;

        private final int mQueryType;

        DnsAddressAnswer(byte[] data) throws ParseException {
            super(data);
            if ((mHeader.getFlags() & (1 << 15)) == 0) {
                throw new ParseException("Not a answer packet");
            }
            if (mHeader.getRcode() != 0) {
                throw new ParseException("Response error, rcode:" + mHeader.getRcode());
            }
            if (mHeader.getSectionCount(ANSECTION) == 0) {
                throw new ParseException("No available answer");
            }
            if (mHeader.getSectionCount(QDSECTION) == 0) {
                throw new ParseException("No question found");
            }
            // Suspect 1 question
            mQueryType = mSections[QDSECTION].get(0).getDnsType();
        }

        public List<InetAddress> getAddresses() {
            final List<InetAddress> results = new ArrayList<InetAddress>();
            for (DnsSection ansSec : mSections[ANSECTION]) {
                // Only support A and AAAA
                int nsType = ansSec.getDnsType();
                if (nsType != 1 && nsType != 28 && nsType != mQueryType) {
                    continue;
                }
                try {
                    results.add(InetAddress.getByAddress(ansSec.getRR()));
                } catch (UnknownHostException e) {
                    if (DBG) {
                        Log.w(TAG, "rr to address fail");
                    }
                }
            }
            return results;
        }
    }

    private class ResolverHandler extends Handler {
        private static final String TAG = "DnsResolver.ResolverHandler";
        private static final boolean DBG = false;
        private final InetAddressAnswer mAnswerCB;
        private List<InetAddress> mAnswerList;
        private int mResponseCount = 0;

        ResolverHandler(Looper looper, InetAddressAnswer answer) {
            super(looper);
            mAnswerList = new ArrayList<>();
            mAnswerCB = answer;
        }

        ResolverHandler(Handler handler, InetAddressAnswer answer) {
            this(Preconditions.checkNotNull(handler,
                    "Handler cannot be null.").getLooper(), answer);
        }

        @Override
        public void handleMessage(Message message) {
            final byte[] data = (byte[]) message.obj;
            switch (message.what) {
                case DNS_RAW_RESPONSE: {
                    mResponseCount++;
                    parseAnswers(data);

                    if (DBG) {
                        Log.w(TAG, "ResolverHandler count:" + mResponseCount);
                    }

                    if (mResponseCount == 2) {
                        mAnswerCB.onAnswer(mAnswerList);
                    }
                    break;
                }
            }
        }

        private void parseAnswers(byte[] data) {
            try {
                mAnswerList.addAll(new DnsAddressAnswer(data).getAddresses());
            } catch (DnsPacket.ParseException e) {
                Log.e(TAG, "Parse answer fail " + e.getMessage());
            }
        }
    }
}
