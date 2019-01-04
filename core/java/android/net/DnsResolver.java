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
import android.annotation.Nullable;
import android.os.Handler;
import android.os.MessageQueue;
import android.system.ErrnoException;
import android.util.Log;

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
    public interface RawAnswerCallback {
        /**
         * {@code byte[]} is {@code null} if query timed out
         */
        void onAnswer(@Nullable byte[] answer);
    }

    /**
     * Callback for receiving parsed answers
     */
    public interface InetAddressAnswerCallBack {
        /**
         * Will be called exactly once with all the answers to the query.
         * size of addresses will be zero if no available answer could be parsed.
         */
        void onAnswer(@NonNull List<InetAddress> addresses);
    }

    /**
     * Get instance for DnsResolver
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
     *         {@code null} for query on default network.
     * @param query blob message
     * @param flags flags as a combination of the FLAGS_* constants
     * @param handler {@link Handler} to specify the thread
     *         upon which the callback will be invoked.
     * @param callback a {@link RawAnswerCallback} which will be called to notify the caller
     *         of the result of dns query.
     */
    public void query(@Nullable Network network, @NonNull byte[] query, @QueryFlag int flags,
            @NonNull Handler handler, @NonNull RawAnswerCallback callback) throws ErrnoException {
        final FileDescriptor queryfd = resNetworkSend((network != null
                ? network.netId : NETID_UNSET), query, query.length, flags);
        registerFDListener(handler.getLooper().getQueue(), queryfd, callback);
    }

    /**
     * Pass in a domain name and corresponding setting,
     * get a blob back asynchronously with the entire raw answer.
     *
     * @param network {@link Network} specifying which network for querying.
     *         {@code null} for query on default network.
     * @param domain domain name for querying
     * @param nsClass dns class as one of the CLASS_* constants
     * @param nsType dns resource record (RR) type as one of the TYPE_* constants
     * @param flags flags as a combination of the FLAGS_* constants
     * @param handler {@link Handler} to specify the thread
     *         upon which the callback will be invoked.
     * @param callback a {@link RawAnswerCallback} which will be called to notify the caller
     *         of the result of dns query.
     */
    public void query(@Nullable Network network, @NonNull String domain, @QueryClass int nsClass,
            @QueryType int nsType, @QueryFlag int flags,
            @NonNull Handler handler, @NonNull RawAnswerCallback callback) throws ErrnoException {
        final FileDescriptor queryfd = resNetworkQuery((network != null
                ? network.netId : NETID_UNSET), domain, nsClass, nsType, flags);
        registerFDListener(handler.getLooper().getQueue(), queryfd, callback);
    }

    /**
     * Pass in a domain name and corresponding setting,
     * get back a set of InetAddresses asynchronously.
     *
     * @param network {@link Network} specifying which network for querying.
     *         {@code null} for query on default network.
     * @param domain domain name for querying
     * @param flags flags as a combination of the FLAGS_* constants
     * @param handler {@link Handler} to specify the thread
     *         upon which the callback will be invoked.
     * @param callback an {@link InetAddressAnswerCallBack} which will be called to
     *         notify the caller of the result of dns query.
     *
     */
    public void query(@Nullable Network network, @NonNull String domain, @QueryFlag int flags,
            @NonNull Handler handler, @NonNull InetAddressAnswerCallBack callback)
            throws ErrnoException {
        final FileDescriptor v4fd = resNetworkQuery((network != null
                ? network.netId : NETID_UNSET), domain, CLASS_IN, TYPE_A, flags);
        final FileDescriptor v6fd = resNetworkQuery((network != null
                ? network.netId : NETID_UNSET), domain, CLASS_IN, TYPE_AAAA, flags);

        final InetAddressAnswerAccumulator accmulator =
                new InetAddressAnswerAccumulator(2, callback);

        registerFDListener(handler.getLooper().getQueue(), v4fd, accmulator);
        registerFDListener(handler.getLooper().getQueue(), v6fd, accmulator);
    }

    private void registerFDListener(@NonNull MessageQueue queue,
            @NonNull FileDescriptor queryfd, @NonNull RawAnswerCallback answer) {
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

    private void registerFDListener(@NonNull MessageQueue queue, @NonNull FileDescriptor queryfd,
            @NonNull InetAddressAnswerAccumulator accmulator) {
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

                    accmulator.accumulate(parseAnswers(answerbuf));

                    // Unregister this fd listener
                    return 0;
                });
    }

    private class DnsAddressAnswer extends DnsPacket {
        private static final String TAG = "DnsResolver.DnsAddressAnswer";
        private static final boolean DBG = false;

        private final int mQueryType;

        DnsAddressAnswer(@NonNull byte[] data) throws ParseException {
            super(data);
            if ((mHeader.flags & (1 << 15)) == 0) {
                throw new ParseException("Not an answer packet");
            }
            if (mHeader.rcode != 0) {
                throw new ParseException("Response error, rcode:" + mHeader.rcode);
            }
            if (mHeader.getSectionCount(ANSECTION) == 0) {
                throw new ParseException("No available answer");
            }
            if (mHeader.getSectionCount(QDSECTION) == 0) {
                throw new ParseException("No question found");
            }
            // Assume only one question per answer packet. (RFC1035)
            mQueryType = mSections[QDSECTION].get(0).nsType;
        }

        public @NonNull List<InetAddress> getAddresses() {
            final List<InetAddress> results = new ArrayList<InetAddress>();
            for (final DnsSection ansSec : mSections[ANSECTION]) {
                // Only support A and AAAA
                int nsType = ansSec.nsType;
                if (nsType != TYPE_A && nsType != TYPE_AAAA && nsType != mQueryType) {
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

    private @Nullable List<InetAddress> parseAnswers(@Nullable byte[] data) {
        try {
            return (data == null) ? null : new DnsAddressAnswer(data).getAddresses();
        } catch (DnsPacket.ParseException e) {
            Log.e(TAG, "Parse answer fail " + e.getMessage());
            return null;
        }
    }

    private class InetAddressAnswerAccumulator {
        private final List<InetAddress> mAllAnswers;
        private final InetAddressAnswerCallBack mAnswerCB;
        private final int mTargetAnswerCount;
        private int mReceivedAnswerCount = 0;

        InetAddressAnswerAccumulator(int size, @NonNull InetAddressAnswerCallBack answer) {
            mTargetAnswerCount = size;
            mAllAnswers = new ArrayList<>();
            mAnswerCB = answer;
        }

        public void accumulate(@Nullable List<InetAddress> answer) {
            mAllAnswers.addAll(answer);
            if (++mReceivedAnswerCount == mTargetAnswerCount) {
                mAnswerCB.onAnswer(mAllAnswers);
            }
        }
    }
}
