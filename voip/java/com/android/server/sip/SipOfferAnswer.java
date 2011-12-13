/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.server.sip;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import android.net.sip.SipProfile;
import android.util.Log;

import java.util.EventObject;

import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * Manages SDP (RFC2327) and offer/answer model (RFC3264)
 */
class SipOfferAnswer {
    private static final String TAG = "SipOfferAnswer";
    private static final boolean DEBUG = true;

    private SipSessionGroup mGroup;
    private SipHelper mSipHelper;
    private SipProfile mLocalProfile;
    private String mOffer;
    private String mAnswer;
    private boolean mSentOffer;

    /*-----------------------------------------------------------------*
     * constructor                                                     *
     *-----------------------------------------------------------------*/

    public SipOfferAnswer(
            SipSessionGroup group,
            SipHelper helper,
            SipProfile profile) {
        mGroup = group;
        mSipHelper = helper;
        mLocalProfile = profile;
        reset();
    }

    /*-----------------------------------------------------------------*
     * public methods                                                  *
     *-----------------------------------------------------------------*/

    public void reset() {
        mOffer = mAnswer = null;
        mSentOffer = false;
        return;
    }

    public void setOffer(String sdpMessage, boolean sentOffer) {
        mOffer = sdpMessage;
        mSentOffer = sentOffer;
        return;
    }

    public String getOffer() {
        return mOffer;
    }

    public void setAnswer(String sdpMessage) {
        mAnswer = sdpMessage;
        return;
    }

    public String getAnswer() {
        return mAnswer;
    }

    public void setAnswerOrRemoteOffer(String sdpMessage) {
        if (sdpMessage != null) {
            if (getOffer() != null) {
                setAnswer(sdpMessage);
            } else {
                setOffer(sdpMessage, false/*sent*/);
            }
        }
        return;
    }

    public void setAnswerOrLocalOffer(String sdpMessage) {
        if (sdpMessage != null) {
            if (getOffer() != null) {
                setAnswer(sdpMessage);
            } else {
                setOffer(sdpMessage, true/*sent*/);
            }
        }
        return;
    }

    public String getLocalSdpMessage() {
        return (mSentOffer ? mOffer : mAnswer);
    }

    public String getRemoteSdpMessage() {
        return (mSentOffer ? mAnswer : mOffer);
    }

    public boolean isCompleted() {
        return (mOffer != null && mAnswer != null);
    }

    public String getSdpMessage(Message message) {
        ContentTypeHeader ct;
        if (message instanceof Request) {
            Request request = (Request)message;
            ct = ((SIPRequest)request).getContentTypeHeader();
        } else if (message instanceof Response) {
            Response response = (Response)message;
            ct = ((SIPResponse)response).getContentTypeHeader();
        } else {
            Log.w(TAG, "getSdpMessage: Neither Request nor Response?");
            return null;
        }

        if (ct != null) {
            if (ct.getContentType().equalsIgnoreCase("application")
            &&  ct.getContentSubType().equalsIgnoreCase("sdp")) {
                ; /* ok, go ahead */
            } else {
                Log.w(TAG, "getSdpMessage: Content is not an application/sdp");
                return null;
            }
        } else {
            Log.w(TAG, "getSdpMessage: message has no ContentTypeHeader");
            return null;
        }

        return mGroup.extractContent(message);
    }

    public void rejectBadOffer(RequestEvent event, int warningCode) {
        try {
            mSipHelper.sendNotAcceptableHere(
                event, getHostName(),
                warningCode, SipWarning.toString(warningCode));
        } catch (SipException e) {
            Log.w(TAG, "rejectBadOffer: ", e);
        }
        return;
    }

    public void rejectBadAnswer(Dialog dialog) {
        try {
            mSipHelper.sendBye(dialog, Response.NOT_ACCEPTABLE_HERE);
        } catch (SipException e) {
            Log.w(TAG, "rejectBadAnswer: ", e);
        }
        return;
    }

    /*-----------------------------------------------------------------*
     * private methods                                                 *
     *-----------------------------------------------------------------*/

    private String getHostName() {
        return mLocalProfile.getSipAddress().getHost();
    }
}
