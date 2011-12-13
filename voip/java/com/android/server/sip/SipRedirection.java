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

import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.ContactList;
import gov.nist.javax.sip.header.SIPHeaderNames;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPResponse;

import android.net.sip.ISipSession;
import android.net.sip.ISipSessionListener;
import android.net.sip.SipErrorCode;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.ListeningPoint;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.Header;
import javax.sip.header.Parameters;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * This class manages 3xx responses for UAC.
 */
class SipRedirection {
    private static final String TAG = "SipRedirection";
    private static final boolean DEBUG = true;

    private SipSessionGroup mGroup;
    private SipSessionGroup.SipSessionImpl mSession;
    private SipHelper mSipHelper;
    private String mLocalSessionDescription;
    private int mCallTimeout;

    /*
     * To prevent redirection loop, keep track of RURI history.
     */
    private ArrayList<SipURI> mRuriHistory = new ArrayList<SipURI>();

    /*
     * For redirection loop control, keep a (subset) copy of given
     * Contact headers of last received 3xx response.
     */
    private ContactList mContactList = new ContactList();

    /*-----------------------------------------------------------------*
     * constructor                                                     *
     *-----------------------------------------------------------------*/

    public SipRedirection(
            SipSessionGroup group,
            SipSessionGroup.SipSessionImpl session,
            SipHelper helper) {
        mGroup = group;
        mSession = session;
        mSipHelper = helper;
    }

    /*-----------------------------------------------------------------*
     * public methods                                                  *
     *-----------------------------------------------------------------*/

    public void init(
            SipProfile peerProfile, String sessionDescription, int timeout) {
        /* Keep last INVITE parameters for the next INVITE. */
        addRuriHistory(peerProfile.getUri());
        mLocalSessionDescription = sessionDescription;
        mCallTimeout = timeout;
        return;
    }

    public void reset() {
        mContactList.clear();
        resetRuriHistory();
        return;
    }

    public void handleRedirect(EventObject evt) {
        ResponseEvent event = (ResponseEvent) evt;
        Response response = event.getResponse();
        int statusCode = response.getStatusCode();

        switch (statusCode) {
        case Response.MOVED_PERMANENTLY:
        case Response.MOVED_TEMPORARILY:
        case Response.USE_PROXY:
            break;
        case Response.MULTIPLE_CHOICES:
        case Response.ALTERNATIVE_SERVICE:
        default:
            /* Cannot proceed without user intervention. *//* XXX */
            resetRuriHistory();
            mSession.onError(response);
            return;
        }

        ContactList contactList =
                ((SIPResponse)response).getContactHeaders();
        if (contactList == null) {
            /*
             * A proxy might have deleted the Contact headers
             * for privacy reasons.
             */
            mSession.onError(SipErrorCode.SERVER_ERROR,
                "Missing Contact headers on 3xx response");
            return;
        }
        extractAvailableContacts(contactList);

        if (! tryNextCandidate(evt)) {
            mSession.onError(SipErrorCode.SERVER_ERROR,
                "No available Contact on 3xx response");
        }
        return;
    }

    public boolean tryNextCandidate(EventObject evt) {
        boolean processed = false;

        /* Cleanup previous transaction, etc. */
        mSession.reset();

        for (ContactHeader hdr = getNextContact(); hdr != null; ) {
            if (tryRedirect(evt, hdr)) {
                processed = true;
                break;
            }
        }
        if (!processed) {
            if (DEBUG) {
                Log.d(TAG, "Contact list has exhausted, give up.");
            }
        }
        return processed;
    }

    /*-----------------------------------------------------------------*
     * private methods                                                 *
     *-----------------------------------------------------------------*/

    private void addRuriHistory(SipURI uri) {
        if (DEBUG) {
            Log.d(TAG, "addRuriHistory: URI=" + uri.toString());
        }
        mRuriHistory.add(uri);
        return;
    }

    private void resetRuriHistory() {
        if (DEBUG) {
            Log.d(TAG, "resetRuriHistory");
        }
        mRuriHistory.clear();
        return;
    }

    private SipURI getOriginalRuri() {
        return (mRuriHistory.isEmpty() ? null : mRuriHistory.get(0));
    }

    private boolean lookupRuriHistory(SipURI probe) {
        boolean found = false;
        for (int i = 0, n = mRuriHistory.size(); i < n; i++) {
            SipUri sipUri = (SipUri)(mRuriHistory.get(i));
            if (sipUri.equals(probe)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private void extractAvailableContacts(ContactList contactList) {
        /*
         * Contact list might have created by previous 3xx
         * response; reset it before processing.
         */
        mContactList.clear();

        /*
         * Setup my own Contact list by extracting acceptable
         * entries from the given list.
         *
         * NB:
         * Unfortunately, here we cannot simply call method
         * "contactList.clone()", due to possibility of
         * ClassCastException. The exception occures if any
         * element in given Contact list contains non-SIP
         * scheme such like "tel", "mailto" etc.
         *
        mContactList = (ContactList)contactList.clone();
         ***/

        SipProfile localProfile = mSession.getLocalProfile();
        String stackTransport = localProfile.getProtocol();
        SipURI myUri = localProfile.getUri();
        SipURI originalRuri = getOriginalRuri();

        for (int i = 0, n = contactList.size(); i < n; i++) {
            ContactHeader hdr = contactList.get(i);

            /*
             * Insane server might set identical Contact entries
             * multiple times in the list.
             */
            if (lookupContact(hdr)) {
                if (DEBUG) {
                    Log.d(TAG, "REJECT: duplicated contact?");
                }
                continue;
            }

            /*
             * Exclude this entry if it is a wildcard.
             * For 3xx responses, this case should never happen.
             */
            if (hdr.isWildCard()) {
                if (DEBUG) {
                    Log.d(TAG, "REJECT: wildcard contact?");
                }
                continue;
            }

            /*
             * Exclude this entry if it has non-SIP scheme.
             * Otherwise, we will see ClassCastException.
             */
            if (! hdr.getAddress().isSIPAddress()) {
                if (DEBUG) {
                    Log.d(TAG, "REJECT: non-SIP scheme: URI=" +
                            hdr.getAddress().getURI().toString());
                }
                continue;
            }
            SipURI uri = (SipURI)hdr.getAddress().getURI();

            /*
             * Exclude this entry if it has different transport
             * parameter value against current local profile.
             * Otherwise, we will see NullPointerException within
             * "gov.nist.javax.sip.
             *      SipProviderImpl.getNewClientTransaction()".
             */
            String transport = (uri.hasTransport() ?
                uri.getTransportParam() : getDefaultTransportParam(uri));

            if (! stackTransport.equalsIgnoreCase(transport)) {
                if (DEBUG) {
                    Log.d(TAG, "REJECT: transport mismatch: stack=" + stackTransport + ", uri=" + transport);
                }
                continue;
            }

            /*
             * According to RFC3261, section 8.1.3.4, we SHOULD
             * inform to user if the original RURI is SIPS URI
             * and going to recurse to a non-SIPS URI.
             *
             * Since we have no way to inform this situation to
             * user, simply avoid mixing SIP and SIPS in the
             * target list, for now...
             */
            if (originalRuri.isSecure() != uri.isSecure()) {
                if (DEBUG) {
                    Log.d(TAG, "REJECT: security mismatch: org=" + originalRuri.getScheme() + ", uri=" + uri.getScheme());
                }
                continue;
            }

            /*
             * Excerpt from RFC3261, section 19.1.5:
             *
             * "If the URI contains a method parameter, its value MUST
             * be used as the method of the request."
             */
            String method = uri.getMethodParam();
            if (method != null && !method.equals("")) {
                if (! isAcceptableMethod(method)) {
                    Log.d(TAG, "REJECT: unsupported method: " + method);
                    continue;
                }
            }

            /*
             * For now, we don't use any preferences among Contact
             * entries being stored here; q-value will be ignored.
             * Simply follow the appearance order in original list.
             */
            ContactHeader clonedHdr = (ContactHeader)hdr.clone();
            mContactList.add((Contact)clonedHdr);
            if (DEBUG) {
                Log.d(TAG, "ACCEPT: uri=" + uri.toString());
            }
        }
        return;
    }

    private boolean lookupContact(ContactHeader probe) {
        boolean found = false;
        String probeString = probe.toString().toLowerCase();

        for (int i = 0, n = mContactList.size(); i < n; i++) {
            ContactHeader hdr = mContactList.get(i);

            /*** Object comparison seems not to work...
            if (hdr.equals(probe)) {
                found = true;
                break;
            }
            ***/
            /* Try string comparison as an alternative. */
            if (hdr.toString().toLowerCase().equals(probeString)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private ContactHeader getNextContact() {
        ContactHeader found = null;
        for (int i = 0, n = mContactList.size(); i < n; i++) {
            ContactHeader hdr = mContactList.get(i);

            /*
             * SIP-URI specified in Contact header has special syntax.
             * Here we cannot simply use the following method:
             *
             *   SipURI uri = (SipURI)hdr.getAddress().getURI();
             *
             * Instead, we need to consider the modified version of URI
             * to be set as the RURI for the next outgoing message.
             */
            SipURI uri = getRedirectedUri(hdr);

            /* Avoid redirection loop */
            if (! lookupRuriHistory(uri)) {
                found = hdr;
                uri = null; /* make the GC target */
                break;
            }
            uri = null; /* make the GC target */
        }
        return found;
    }

    private SipURI getRedirectedUri(ContactHeader hdr) {
        SipURI uri = (SipURI)hdr.getAddress().getURI();

        /*
         * Excerpt from RFC3261, section 8.1.3.4:
         *
         * "In order to create a request based on a contact address in a 3xx
         * response, a UAC MUST copy the entire URI from the target set into
         * the Request-URI, except for the "method-param" and "header" URI
         * parameters."
         *
         * NB:
         * We also exclude contact-parameters (such like "q", "expires")
         * and generic parameters (such like "isfocus") to formulate the
         * RURI here.
         */
        SipUri sipUri = (SipUri)(((SipUri)uri).clone());
        sipUri.removeMethod();
        sipUri.clearQheaders();

        return (SipURI)sipUri;
    }

    private boolean tryRedirect(EventObject evt, ContactHeader hdr) {
        boolean processed = false;
        SipURI uri = (SipURI)hdr.getAddress().getURI();
        if (DEBUG) {
            Log.d(TAG, "tryRedirect: next=" + uri.toString());
        }

        /*
         * Currently, redirection is supported only for INVITE;
         * we treat timeout case as an INVITE request failure.
         */
        if ((evt instanceof TimeoutEvent)
        ||  (mGroup.expectResponse(Request.INVITE, evt))) {
            String method = uri.getMethodParam();
            if (method == null
            ||  method.equalsIgnoreCase(Request.INVITE)) {
                processed = tryRedirectedInvite(evt, hdr);
            } else {
                Log.w(TAG, "tryRedirect: Unsupported method: " + method);
            }
        } else if (mGroup.expectResponse(Request.REGISTER, evt)) {
            Log.w(TAG, "REGISTER redirection not yet supported");
        } else if (mGroup.expectResponse(Request.OPTIONS, evt)) {
            Log.w(TAG, "OPTIONS redirection not yet supported");
        } else {
            Log.w(TAG, "tryRedirect: Unexpected case?");
        }
        return processed;
    }

    private boolean tryRedirectedInvite(EventObject evt, ContactHeader hdr) {
        boolean processed = false;
        try {
            /* Replace the peer profile using redirected URI */
            SipProfile profile = buildPeerProfile(hdr);
            if (DEBUG) {
                Log.d(TAG, "tryRedirectedInvite: peer=" + profile.getUri());
            }

            /* There may exist "headers" parameter in this Contact */
            ArrayList<Header> hlist = extractHeaderParams(hdr);

            /*
             * Excerpts from RFC3261, section 8.1.3.4:
             *
             * "It is RECOMMENDED that the UAC reuse the same To, From,
             * and Call-ID used in the original redirected request,
             * but the UAC MAY also choose to update the Call-ID header
             * field value for new requests, for example.
             */
            boolean updateCallId = false; /* depends on policy *//* XXX */
            if (updateCallId) {
                mSession.makeCall(profile,
                    mLocalSessionDescription, mCallTimeout);
            } else {
                mSession.makeRedirectedCall(profile,
                    mLocalSessionDescription, mCallTimeout, evt, hlist);
            }

            /*
             * Now that a redirected INVITE will be sent to network,
             * flush this Contact from the local cache.
             * And register the RURI to prevent redirection loop.
             */
            mContactList.remove(hdr);
            addRuriHistory(profile.getUri());

            /* Ok, let's see what happens... */
            processed = true;
        } catch (ParseException e) {
            Log.w(TAG, "tryRedirectedInvite", e);
        }
        return processed;
    }

    private SipProfile buildPeerProfile(ContactHeader hdr)
            throws ParseException {
        SipURI uri = getRedirectedUri(hdr);
        String redirectedUriString = uri.toString();
        uri = null; /* make the GC target */

        SipProfile.Builder builder =
            new SipProfile.Builder(redirectedUriString);

        builder.setSendKeepAlive(false);
        builder.setAutoRegistration(false);

        if (hdr.getAddress().hasDisplayName()) {
            builder.setDisplayName(hdr.getAddress().getDisplayName());
        }
        return builder.build();
    }

    private ArrayList<Header> extractHeaderParams(ContactHeader hdr) {
        ArrayList<Header> hlist = null;
        SipURI uri = (SipURI)hdr.getAddress().getURI();

        Iterator itr = uri.getHeaderNames();
        if (itr.hasNext()) {
            hlist = new ArrayList<Header>();

            while (itr.hasNext()) {
                String name = (String)itr.next();
                String value = uri.getHeader(name);

                if (isUnacceptableHeader(name)) {
                    Log.w(TAG, "We SHOULD NOT honor this header: " + name);
                    continue;
                }
                try {
                    hlist.add(mSipHelper.createGenericHeader(name, value));
                } catch (ParseException e) {
                    Log.w(TAG, "mSipHelper.createGenericHeader: " + name, e);
                }
            }
            if (hlist.isEmpty()) {
                hlist = null;
            }
        }
        return hlist;
    }

    private final String getDefaultTransportParam(SipURI uri) {
         /*
          * The default transport is scheme dependent.
          * [cf] RFC3261, section 19.1.1, Table 1
          */
         return (uri.isSecure() ? ListeningPoint.TCP : ListeningPoint.UDP);
    }

    private boolean isAcceptableMethod(String name) {
        final String[] whitelist = {
            Request.INVITE,
        };

        boolean match = false;
        for (int i = 0, n = whitelist.length; i < n; i++) {
            if (name.equalsIgnoreCase(whitelist[i])) {
                match = true;
                break;
            }
        }
        return match;
    }

    private boolean isUnacceptableHeader(String name) {
        final String[] blacklist = {
            /*
             * Excerpts from RFC3261, section 19.1.5:
             *
             * "An implementation SHOULD NOT honor these obviously
             * dangerous header fields: From, Call-ID, CSeq, Via,
             * and Record-Route."
             */
            SIPHeaderNames.FROM,
            SIPHeaderNames.CALL_ID,
            SIPHeaderNames.CSEQ,
            SIPHeaderNames.VIA,
            SIPHeaderNames.RECORD_ROUTE,

            /*
             * "An implementation SHOULD NOT honor any requested Route
             * header field values in order to not be used as an unwitting
             * agent in malicious attacks."
             */
            SIPHeaderNames.ROUTE,

            /*
             * "An implementation SHOULD NOT honor requests to include
             * header fields that may cause it to falsely advertise its
             * location or capabilities.
             * These include: Accept, Accept-Encoding, Accept-Language,
             * Allow, Contact (in its dialog usage), Organization,
             * Supported, and User-Agent."
             */
            SIPHeaderNames.ACCEPT,
            SIPHeaderNames.ACCEPT_ENCODING,
            SIPHeaderNames.ACCEPT_LANGUAGE,
            SIPHeaderNames.ALLOW,
            SIPHeaderNames.CONTACT,
            SIPHeaderNames.ORGANIZATION,
            SIPHeaderNames.SUPPORTED,
            SIPHeaderNames.USER_AGENT,
        };

        boolean match = false;
        for (int i = 0, n = blacklist.length; i < n; i++) {
            if (name.equalsIgnoreCase(blacklist[i])) {
                match = true;
                break;
            }
        }
        return match;
    }
}
