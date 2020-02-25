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

#define LOG_TAG "ConnectivityDiagnosticsManager"

#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedUtfChars.h>
#include <nativehelper/ScopedLocalRef.h>
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <iostream>
#include <sstream>
#include <arpa/inet.h>
#include <errno.h>
#include <linux/errqueue.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/ip_icmp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <chrono>         // std::chrono::seconds
#include <time.h>
#include <unistd.h>

#include <stdio.h>
#include <stdlib.h>

#include <malloc.h>
#include <string.h>
#include <time.h>

#include <unistd.h>
#include <errno.h>
// #include <android/multinetwork.h>
#include "core_jni_helpers.h"


#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <netinet/ip_icmp.h>
#include <netinet/icmp6.h>
#include <arpa/inet.h>
#include <android-base/chrono_utils.h>
#include <chrono>

using namespace android;

namespace android {

const int BUFFER_SIZE = 1028;

static jclass class_ConcurrentLinkedQueue;
static jclass class_ProbeResponse;
static jclass class_InetAddress;
static jmethodID method_ConcurrentLinkedQueueInit;
static jmethodID method_ConcurrentLinkedQueueAdd;
static jmethodID method_ProbeResponseInit;
static jmethodID method_ProbeResponseTimeoutInit;
static jmethodID method_InetAddressGetByName;
static jmethodID method_InetAddressGetHostAddress;

template<typename T>
inline static T min(const T a, const T b) {
    return a < b ? a : b;
}

template<typename T>
inline static T max(const T a, const T b) {
    return a > b ? a : b;
}

static void throwErrnoException(JNIEnv* env, const char* functionName, int error) {
    ScopedLocalRef<jstring> detailMessage(env, env->NewStringUTF(functionName));
    if (detailMessage.get() == NULL) {
        // Not really much we can do here. We're probably dead in the water,
        // but let's try to stumble on...
        env->ExceptionClear();
    }
    static jclass errnoExceptionClass =
            MakeGlobalRefOrDie(env, FindClassOrDie(env, "android/system/ErrnoException"));

    static jmethodID errnoExceptionCtor =
            GetMethodIDOrDie(env, errnoExceptionClass,
            "<init>", "(Ljava/lang/String;I)V");

    jobject exception = env->NewObject(errnoExceptionClass,
                                       errnoExceptionCtor,
                                       detailMessage.get(),
                                       error);
    env->Throw(reinterpret_cast<jthrowable>(exception));
}

static std::string inetaddressToString(JNIEnv* env, jobject inetaddress) {
    jstring hostAddr = (jstring) env->CallObjectMethod(inetaddress, method_InetAddressGetHostAddress);

    jboolean isCopy;
    const char* result = env->GetStringUTFChars(hostAddr, &isCopy);
    std::string addr = std::string(result);
    env->ReleaseStringUTFChars(hostAddr, result);
    env->DeleteLocalRef(hostAddr);
    return addr;
}

static int writeIcmpPacket(
        JNIEnv* env,
        int protocolFamily,
        char packet[],
        int packetLen,
        char payload[],
        int payloadLen,
        int id,
        int seq)
{
    int hdrLen;

    switch (protocolFamily) {
        case AF_INET: {
            struct icmphdr icmpHdr;
            memset(&icmpHdr, 0, sizeof(icmpHdr));
            icmpHdr.type = ICMP_ECHO;
            icmpHdr.un.echo.id = id;
            icmpHdr.un.echo.sequence = seq;

            hdrLen = sizeof(icmpHdr);
            memcpy(packet, &icmpHdr, min(hdrLen, packetLen));
            break;
        }
        case AF_INET6: {
            struct icmp6_hdr icmpHdr;
            memset(&icmpHdr, 0, sizeof(icmpHdr));
            icmpHdr.icmp6_type = ICMP6_ECHO_REQUEST;
            icmpHdr.icmp6_id = id;
            icmpHdr.icmp6_seq = seq;

            hdrLen = sizeof(icmpHdr);
            memcpy(packet, &icmpHdr, min(hdrLen, packetLen));
            break;
        }
        default: {
            jclass exc = env->FindClass("java/lang/IllegalArgumentException");
            env->ThrowNew(exc, "Only AF_INET or AF_INET6 allowed");
            env->DeleteLocalRef(exc);
            return -1;
        }
    }

    memcpy(packet + hdrLen, payload, max(0, min(payloadLen, packetLen - hdrLen)));
    return 0;
}

static int recordProbeTimeout(JNIEnv* env, int ttl, jobject probeResponses) {
    jobject probeResponse = env->NewObject(class_ProbeResponse, method_ProbeResponseTimeoutInit,
            ttl);
    env->CallBooleanMethod(class_ConcurrentLinkedQueue, method_ConcurrentLinkedQueueAdd,
            probeResponse);
    return 0;
}

static int recordNodeResponse(
        JNIEnv* env,
        std::string node,
        double rtt,
        int ttl,
        jobject probeResponses)
{
    jstring jstringNode = env->NewStringUTF(node.c_str());
    jobject inetAddress = env->CallStaticObjectMethod(class_InetAddress,
            method_InetAddressGetByName, jstringNode);
    jobject probeResponse = env->NewObject(class_ProbeResponse, method_ProbeResponseInit,
            inetAddress, ttl, rtt);

    env->CallObjectMethod(class_ConcurrentLinkedQueue, method_ConcurrentLinkedQueueAdd,
            probeResponse);
    env->DeleteLocalRef(jstringNode);
    return 0;
}

int recordIcmpResponse(
        JNIEnv* env,
        int protocolFamily,
        sock_extended_err* sock_err,
        double rtt,
        int ttl,
        jobject probeResponses)
{
    char addr[INET6_ADDRSTRLEN];
    switch (protocolFamily) {
        case AF_INET: {
            struct sockaddr_in* remote_val;
            remote_val = (struct sockaddr_in*) SO_EE_OFFENDER(sock_err);
            inet_ntop(remote_val->sin_family,
                        &remote_val->sin_addr,
                        addr,
                        INET6_ADDRSTRLEN);
            break;
        }
        case AF_INET6: {
            struct sockaddr_in6* remote_val;
            remote_val = (struct sockaddr_in6*) SO_EE_OFFENDER(sock_err);
            inet_ntop(remote_val->sin6_family,
                        &remote_val->sin6_addr,
                        addr,
                        INET6_ADDRSTRLEN);
            break;
        }
        default: {
            jclass exc = env->FindClass("java/lang/IllegalArgumentException");
            env->ThrowNew(exc, "Only AF_INET or AF_INET6 allowed");
            env->DeleteLocalRef(exc);
            return -1;
        }
    }
    return recordNodeResponse(env, std::string(addr), rtt, ttl, probeResponses);
}

static jobject android_net_traceroute(
        JNIEnv *env,
        jobject thiz,
        jint jintProtocolFamily,
        jobject jobjectHost,
        jlong jlongNetworkHandle,
        jint jintMaxTtl,
        jint jintProbesPerHop,
        jint jintTimeoutMillis,
        jint jintPayloadSizeBytes,
        jint jintConcurrentProbesLimit)
{
    const int rcverr = 1;
    const int protocolFamily = jintProtocolFamily;
    // const long networkHandle = (long) jlongNetworkHandle;
    const int maxTtl = (int) jintMaxTtl;
    const int probesPerHop = (int) jintProbesPerHop;
    const int timeoutMillis = (int) jintTimeoutMillis;
    // const int payloadSizeBytes = (int) jintPayloadSizeBytes;
    // const int concurrentProbes = (int) jintConcurrentProbesLimit;

    std::string hostString = inetaddressToString(env, jobjectHost);

    char payload[] = "hello world";
    int payloadLen = sizeof(payload);

    int protocolType;
    int sockoptTtlLevel, sockoptTtlName, sockoptErrLevel, sockoptErrName;
    int errorCategory;
    int hdrLen, addrLen;
    struct sockaddr* addr;
    switch (protocolFamily) {
        case AF_INET:
            protocolType = IPPROTO_ICMP;
            sockoptTtlLevel = IPPROTO_IP;
            sockoptTtlName = IP_TTL;
            sockoptErrLevel = SOL_IP;
            sockoptErrName = IP_RECVERR;

            struct sockaddr_in sockaddrIn;
            memset(&sockaddrIn, 0, sizeof(sockaddrIn));
            sockaddrIn.sin_family = protocolFamily;
            inet_pton(protocolFamily, hostString.c_str(), &sockaddrIn.sin_addr);
            addr = (sockaddr*) &sockaddrIn;

            errorCategory = SO_EE_ORIGIN_ICMP;

            hdrLen = sizeof(struct icmphdr);
            addrLen = sizeof(sockaddrIn);
            break;
        case AF_INET6:
            protocolType = IPPROTO_ICMPV6;
            sockoptTtlLevel = IPPROTO_IPV6;
            sockoptTtlName = IPV6_UNICAST_HOPS;
            sockoptErrLevel = IPPROTO_IPV6;
            sockoptErrName = IPV6_RECVERR;

            struct sockaddr_in6 sockaddrIn6;
            memset(&sockaddrIn6, 0, sizeof(sockaddrIn6));
            sockaddrIn6.sin6_family = protocolFamily;
            inet_pton(protocolFamily, hostString.c_str(), &sockaddrIn6.sin6_addr);
            addr = (sockaddr*) &sockaddrIn6;

            errorCategory = SO_EE_ORIGIN_ICMP6;

            hdrLen = sizeof(struct icmp6_hdr);
            addrLen = sizeof(sockaddrIn6);
            break;
        default:
            jclass exc = env->FindClass("java/lang/IllegalArgumentException");
            env->ThrowNew(exc, "ProtocolFamily must be AF_INET or AF_INET6");
            env->DeleteLocalRef(exc);
            return NULL;
    }

    int packetLen = hdrLen + payloadLen;
    char packet[packetLen];

    // Create and configure a datagram ICMP socket
    int sock = socket(protocolFamily, SOCK_DGRAM, protocolType);
    if (sock < 0) {
        throwErrnoException(env, "socket", errno);
        return NULL;
    }

    /*
    if (android_setsocknetwork((net_handle_t) networkHandle, sock) < 0) {
        throwErrnoException(env, "android_setsocknetwork", errno);
                return NULL;
    }
    */

    struct timeval tv;
    tv.tv_sec = timeoutMillis / 1000;
    tv.tv_usec = (timeoutMillis % 1000) * 1000;
    if (setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (char*) &tv, sizeof(tv)) < 0) {
        throwErrnoException(env, "setsockopt(RCVTIMEO)", errno);
        return NULL;
    }
    if (setsockopt(sock, sockoptErrLevel, sockoptErrName, &rcverr, sizeof(rcverr)) < 0) {
        throwErrnoException(env, "setsockopt(RECVERR)", errno);
        return NULL;
    }

    // Create ConcurrentLinkedQueue for storing ProbeResponse instances
    jobject probeResponses = env->NewObject(class_ConcurrentLinkedQueue,
            method_ConcurrentLinkedQueueInit);

    int ttl = 1;
    int seq = 1;
    char recvBuffer[BUFFER_SIZE];
    for (; ttl <= maxTtl; ttl ++) {
        // Set TTL
        if (setsockopt(sock, sockoptTtlLevel, sockoptTtlName, &ttl, sizeof(ttl)) < 0) {
            throwErrnoException(env, "setsockopt(TTL)", errno);
            return NULL;
        }

        int numReachedDst = 0;
        int numReachedNonDst = 0;
        for (int probe = 1; probe <= probesPerHop; probe++) {
            writeIcmpPacket(env, protocolFamily, packet, packetLen, payload, payloadLen, ttl, seq);
            errno = 0;

            auto start = std::chrono::high_resolution_clock::now();

            // Send packet to addr with TTL of hops
            if (sendto(sock, packet, packetLen, 0, addr, addrLen) < 0) {
                throwErrnoException(env, "sendto", errno);
                return NULL;
            }

            memset(&recvBuffer, 0, BUFFER_SIZE);
            int recvLen = recv(sock, recvBuffer, BUFFER_SIZE - 1, 0);
            
            auto end = std::chrono::high_resolution_clock::now();
            std::chrono::duration<double> elapsed_seconds = end - start;
            double rtt = elapsed_seconds.count() * 1000;

            if (recvLen < 0) {
                if (errno == EAGAIN || errno == EWOULDBLOCK) {
                    // request timed out
                    recordProbeTimeout(env, ttl, probeResponses);
                    continue;
                }
                ssize_t returnStatus;
                char buffer[BUFFER_SIZE];
                struct iovec iov;                       /* Data array */
                struct msghdr msg;                      /* Message header */
                struct cmsghdr* cmsg;                   /* Control related data */
                struct sock_extended_err* sock_err;     /* Struct describing the error */
                struct icmphdr icmph;                   /* ICMP header */
                struct sockaddr_in remote;              /* Our sockfd */

                iov.iov_base = &icmph;
                iov.iov_len = sizeof(icmph);
                msg.msg_name = (void*) &remote;
                msg.msg_namelen = sizeof(remote);
                msg.msg_iov = &iov;
                msg.msg_iovlen = 1;
                msg.msg_flags = 0;
                msg.msg_control = buffer;
                msg.msg_controllen = sizeof(buffer);

                /* Receiving errors flog is set */
                returnStatus = recvmsg(sock, &msg, MSG_ERRQUEUE);

                /* Control messages are always accessed via some macros
                 * http://www.kernel.org/doc/man-pages/online/pages/man3/cmsg.3.html
                 */
                for (cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
                    /* Ip level */
                    if (cmsg->cmsg_level == sockoptErrLevel) {
                        /* We received an error */
                        if (cmsg->cmsg_type == sockoptErrName) {
                            sock_err = (struct sock_extended_err*) CMSG_DATA(cmsg);
                            if (sock_err) {
                                /* We are interested in ICMP errors */
                                if (sock_err->ee_origin == errorCategory) {
                                    /* Handle ICMP errors types */
                                    switch (sock_err->ee_type) {
                                        case ICMP_TIME_EXCEEDED:
                                            // fall through
                                        case ICMP6_TIME_EXCEEDED:
                                            if (recordIcmpResponse(
                                                    env,
                                                    protocolFamily,
                                                    sock_err,
                                                    rtt,
                                                    ttl,
                                                    probeResponses) < 0) {
                                                return NULL;
                                            }
                                            ++numReachedNonDst;
                                            break;
                                        default: {
                                            // throw errnoexception
                                            return NULL;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Reached dst
                ++numReachedDst;
                recordNodeResponse(env, hostString, rtt, ttl, probeResponses);
            }
        }
        if (numReachedDst > 0 && numReachedNonDst == 0) {
            // traceroute is done
            break;
        }
    }
    close(sock);
    return probeResponses;
}

// ----------------------------------------------------------------------------

/*
 * JNI registration.
 */
static const JNINativeMethod gConnectivityDiagnosticsManagerMethods[] = {
    /* name, signature, funcPtr */
    { "traceroute", "(I;Ljava/net/InetAddress;JIIIII)Ljava/util/ConcurrentLinkedQueue;", (void*) android_net_traceroute },
};

int register_android_net_ConnectivityDiagnosticsManager(JNIEnv* env)
{
    class_ConcurrentLinkedQueue = env->FindClass("java/util/ConcurrentLinkedQueue");
    if (class_ConcurrentLinkedQueue == NULL) {
        goto error;
    }
    class_ConcurrentLinkedQueue = (jclass) env->NewGlobalRef(class_ConcurrentLinkedQueue);

    class_ProbeResponse = env->FindClass("android/net/ConnectivityDiagnosticsManager$RouteDiagnosticsCallback$ProbeResponse");
    if (class_ProbeResponse == NULL) {
        goto error;
    }
    class_ProbeResponse = (jclass) env->NewGlobalRef(class_ProbeResponse);

    class_InetAddress = env->FindClass("java/net/InetAddress");
    if (class_InetAddress == NULL) {
        goto error;
    }
    class_InetAddress = (jclass) env->NewGlobalRef(class_InetAddress);

    method_ConcurrentLinkedQueueInit = env->GetMethodID(class_ConcurrentLinkedQueue, "<init>", "()V");
    if (method_ConcurrentLinkedQueueInit == NULL) {
        goto error;
    }

    method_ConcurrentLinkedQueueAdd = env->GetMethodID(class_ConcurrentLinkedQueue, "add", "(Ljava/lang/Object;)Z");
    if (method_ConcurrentLinkedQueueAdd == NULL) {
        goto error;
    }

    method_ProbeResponseInit = env->GetMethodID(class_ProbeResponse, "<init>", "(Ljava/net/InetAddress;ID)V");
    if (method_ProbeResponseInit == NULL) {
        goto error;
    }

    method_ProbeResponseTimeoutInit = env->GetMethodID(class_ProbeResponse, "<init>", "(I)V");
    if (method_ProbeResponseTimeoutInit == NULL) {
        goto error;
    }

    method_InetAddressGetByName = env->GetStaticMethodID(
            class_InetAddress, "getByName", "(Ljava/lang/String;)Ljava/net/InetAddress;");
    if (method_InetAddressGetByName == NULL) {
        goto error;
    }

    method_InetAddressGetHostAddress = env->GetMethodID(
            class_InetAddress, "getHostAddress", "()Ljava/lang/String;");
    if (method_InetAddressGetHostAddress == NULL) {
        goto error;
    }

    return RegisterMethodsOrDie(env, "android/net/ConnectivityDiagnosticsManager",
                                gConnectivityDiagnosticsManagerMethods,
                                NELEM(gConnectivityDiagnosticsManagerMethods));

    error:
        ALOGE("Error registering android.net.ConnectivityDiagnosticsManager");
        return -1;
    }
};

