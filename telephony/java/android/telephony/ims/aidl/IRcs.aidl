/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.telephony.ims.aidl;

import android.net.Uri;
import android.telephony.ims.RcsMessage;
import android.telephony.ims.RcsPart;
import android.telephony.ims.RcsParticipant;
import android.telephony.ims.Rcs1To1Thread;
import android.telephony.ims.RcsThreadQueryContinuationToken;
import android.telephony.ims.RcsThreadQueryParameters;
import android.telephony.ims.RcsThreadQueryResult;

/**
 * RPC definition between RCS storage APIs and phone process.
 * {@hide}
 */
interface IRcs {
    // RcsMessageStore APIs
    RcsThreadQueryResult getRcsThreads(in RcsThreadQueryParameters queryParameters);

    RcsThreadQueryResult getRcsThreadsWithToken(
        in RcsThreadQueryContinuationToken continuationToken);

    void deleteThread(int threadId);

    Rcs1To1Thread createRcs1To1Thread(in RcsParticipant participant);

    // RcsThread APIs
    void addMessage(in RcsMessage rcsOutgoingMessage, int rcsThreadId);

    void deleteMessage(int rcsMessageId, boolean isIncoming, int rcsThreadId, boolean isGroup);

    int getMessageCount(int rcsThreadId);

    // Rcs1To1Thread APIs
    void set1To1ThreadFallbackThreadId(int rcsThreadId, int fallbackId);

    // RcsGroupThread APIs
    void setGroupThreadName(int rcsThreadId, String groupName);

    void setGroupThreadIcon(int rcsThreadId, in Uri groupIcon);

    void setGroupThreadOwner(int rcsThreadId, in RcsParticipant participant);

    void setGroupThreadConferenceUri(int rcsThreadId, String conferenceUri);

    void addParticipantToGroupThread(int rcsThreadId, in RcsParticipant participant);

    void removeParticipantFromGroupThread(int rcsThreadId, in RcsParticipant participant);

    // RcsParticipant APIs
    RcsParticipant createRcsParticipant(String canonicalAddress);

    void updateRcsParticipantCanonicalAddress(int id, String canonicalAddress);

    void updateRcsParticipantAlias(int id, String alias);

    // RcsMessage APIs
    void setMessageSubId(int messageId, boolean isIncoming, int subId);

    void setMessageStatus(int messageId, boolean isIncoming, int status);

    void setMessageOriginationTimestamp(int messageId, boolean isIncoming, long originationTimestamp);

    void setGlobalMessageIdForMessage(int messageId, boolean isIncoming, String globalId);

    void addPartToMessage(int messageId, boolean isIncoming, in RcsPart rcsPart);

    void removePartFromMessage(int messageId, boolean isIncoming, in RcsPart rcsPart);

    void setMessageArrivalTimestamp(int messageId, boolean isIncoming, long arrivalTimestamp);

    void setMessageNotifiedTimestamp(int messageId, boolean isIncoming, long notifiedTimestamp);

    // RcsOutgoingMessageDelivery APIs
    void createOutgoingDelivery(int messageId, int participantId, long seenTimestamp, long deliveredTimestamp);

    void setOutgoingDeliveryDeliveredTimestamp(int messageId, int participantId, long deliveredTimestamp);

    void setOutgoingDeliverySeenTimestamp(int messageId, int participantId, long seenTimestamp);
}