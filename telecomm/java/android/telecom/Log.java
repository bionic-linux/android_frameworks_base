/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.telecom;

import android.net.Uri;
import android.os.AsyncTask;
import android.telecom.Logging.Events;
import android.telecom.Logging.Session;
import android.telecom.Logging.Sessions;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Manages logging for the entire module.
 *
 * @hide
 */
public class Log {

    // Don't check in with this true!
    private static final boolean LOG_DBG = false;

    private static final long EXTENDED_LOGGING_DURATION_MILLIS = 60000 * 30; // 30 minutes

    private static final int EVENTS_TO_CACHE = 10;
    private static final int EVENTS_TO_CACHE_DEBUG = 20;

    // Generic tag for all In Call logging
    @VisibleForTesting
    public static String TAG = "TelecomFramework";

    private static final boolean FORCE_LOGGING = false; /* STOP SHIP if true */
    public static final boolean DEBUG = isLoggable(android.util.Log.DEBUG);
    public static final boolean INFO = isLoggable(android.util.Log.INFO);
    public static final boolean VERBOSE = isLoggable(android.util.Log.VERBOSE);
    public static final boolean WARN = isLoggable(android.util.Log.WARN);
    public static final boolean ERROR = isLoggable(android.util.Log.ERROR);

    // Used to synchronize singleton logging lazy initialization
    private static final Object sSingletonSync = new Object();
    private static Events sEvents;
    private static Sessions sSessions;

    /**
     * Create a container for the Android Logging class so that it can be mocked in testing.
     */
    public static class SystemLoggingContainer {

        public void v(String TAG, String msg) {
            android.util.Slog.v(TAG, msg);
        }

        public void d(String TAG, String msg) {
            android.util.Slog.d(TAG, msg);
        }

        public void i(String TAG, String msg) {
            android.util.Slog.i(TAG, msg);
        }

        public void w(String TAG, String msg) {
            android.util.Slog.w(TAG, msg);
        }

        public void e(String TAG, String msg, Throwable tr) {
            android.util.Slog.e(TAG, msg, tr);
        }

        public void wtf(String TAG, String msg, Throwable tr) {
            android.util.Slog.wtf(TAG, msg, tr);
        }
    }
    // Set the logging container to be the system's. This will only change when being mocked
    // during testing.
    private static SystemLoggingContainer systemLogger = new SystemLoggingContainer();

    /**
     * Tracks whether user-activated extended logging is enabled.
     */
    private static boolean mIsUserExtendedLoggingEnabled = false;

    /**
     * The time when user-activated extended logging should be ended.  Used to determine when
     * extended logging should automatically be disabled.
     */
    private static long mUserExtendedLoggingStopTime = 0;

    private Log() {
    }

    public static void d(String prefix, String format, Object... args) {
        if (mIsUserExtendedLoggingEnabled) {
            maybeDisableLogging();
            systemLogger.i(TAG, buildMessage(prefix, format, args));
        } else if (DEBUG) {
            systemLogger.d(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void d(Object objectPrefix, String format, Object... args) {
        if (mIsUserExtendedLoggingEnabled) {
            maybeDisableLogging();
            systemLogger.i(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        } else if (DEBUG) {
            systemLogger.d(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void i(String prefix, String format, Object... args) {
        if (INFO) {
            systemLogger.i(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void i(Object objectPrefix, String format, Object... args) {
        if (INFO) {
            systemLogger.i(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void v(String prefix, String format, Object... args) {
        if (mIsUserExtendedLoggingEnabled) {
            maybeDisableLogging();
            systemLogger.i(TAG, buildMessage(prefix, format, args));
        } else if (VERBOSE) {
            systemLogger.v(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void v(Object objectPrefix, String format, Object... args) {
        if (mIsUserExtendedLoggingEnabled) {
            maybeDisableLogging();
            systemLogger.i(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        } else if (VERBOSE) {
            systemLogger.v(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void w(String prefix, String format, Object... args) {
        if (WARN) {
            systemLogger.w(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void w(Object objectPrefix, String format, Object... args) {
        if (WARN) {
            systemLogger.w(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void e(String prefix, Throwable tr, String format, Object... args) {
        if (ERROR) {
            systemLogger.e(TAG, buildMessage(prefix, format, args), tr);
        }
    }

    public static void e(Object objectPrefix, Throwable tr, String format, Object... args) {
        if (ERROR) {
            systemLogger.e(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args),
                    tr);
        }
    }

    public static void wtf(String prefix, Throwable tr, String format, Object... args) {
        systemLogger.wtf(TAG, buildMessage(prefix, format, args), tr);
    }

    public static void wtf(Object objectPrefix, Throwable tr, String format, Object... args) {
        systemLogger.wtf(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args),
                tr);
    }

    public static void wtf(String prefix, String format, Object... args) {
        String msg = buildMessage(prefix, format, args);
        systemLogger.wtf(TAG, msg, new IllegalStateException(msg));
    }

    public static void wtf(Object objectPrefix, String format, Object... args) {
        String msg = buildMessage(getPrefixFromObject(objectPrefix), format, args);
        systemLogger.wtf(TAG, msg, new IllegalStateException(msg));
    }

    /**
     * The ease of use methods below only act mostly as proxies to the Session and Event Loggers.
     * They also control the lazy loaders of the singleton instances, which will never be loaded if
     * the proxy methods aren't used.
     *
     * Please see each method's documentation inside of their respective implementations in the
     * loggers.
     */

    public static void startSession(String shortMethodName) {
        getSessions().startSession(shortMethodName, null);
    }
    public static void startSession(String shortMethodName,
            String callerIdentification) {
        getSessions().startSession(shortMethodName, callerIdentification);
    }

    public static Session createSubsession() {
        return getSessions().createSubsession();
    }

    public static void cancelSubsession(Session subsession) {
        getSessions().cancelSubsession(subsession);
    }

    public static void continueSession(Session subsession, String shortMethodName) {
        getSessions().continueSession(subsession, shortMethodName);
    }

    public static void endSession() {
        getSessions().endSession();
    }

    public static String getSessionId() {
        // If the Session logger has not been initialized, then there have been no sessions logged.
        // Don't load it now!
        synchronized (sSingletonSync) {
            if (sSessions != null) {
                return getSessions().getSessionId();
            } else {
                return "";
            }
        }
    }

    public static void addEvent(Events.EventRecordEntry recordEntry, String event) {
        getEvents().event(recordEntry, event, null);
    }

    public static void addEvent(Events.EventRecordEntry recordEntry, String event, Object data) {
        getEvents().event(recordEntry, event, data);
    }

    public static void addEvent(Events.EventRecordEntry recordEntry, String event, String format,
            Object... args) {
        getEvents().event(recordEntry, event, format, args);
    }

    public static void registerEventListener(Events.EventListener e) {
        getEvents().registerEventListener(e);
    }

    public static void addRequestResponsePair(Events.TimedEventPair p) {
        getEvents().addRequestResponsePair(p);
    }

    public static void dumpEvents(IndentingPrintWriter pw) {
        // If the Events logger has not been initialized, then there have been no events logged.
        // Don't load it now!
        synchronized (sSingletonSync) {
            if (sEvents != null) {
                getEvents().dumpEvents(pw);
            } else {
                pw.println("No Historical Events Logged.");
            }
        }
    }

    /**
     * Enable or disable extended telecom logging.
     *
     * @param isExtendedLoggingEnabled {@code true} if extended logging should be enabled,
     *          {@code false} if it should be disabled.
     */
    public static void setIsExtendedLoggingEnabled(boolean isExtendedLoggingEnabled) {
        // If the state hasn't changed, bail early.
        if (mIsUserExtendedLoggingEnabled == isExtendedLoggingEnabled) {
            return;
        }

        if (sEvents != null) {
            sEvents.changeEventCacheSize(isExtendedLoggingEnabled ?
                    EVENTS_TO_CACHE_DEBUG : EVENTS_TO_CACHE);
        }

        mIsUserExtendedLoggingEnabled = isExtendedLoggingEnabled;
        if (mIsUserExtendedLoggingEnabled) {
            mUserExtendedLoggingStopTime = System.currentTimeMillis()
                    + EXTENDED_LOGGING_DURATION_MILLIS;
        } else {
            mUserExtendedLoggingStopTime = 0;
        }
    }

    private static Events getEvents() {
        // Checking for null again outside of synchronization because we only need to synchronize
        // during the lazy loading of the events logger. We don't need to synchronize elsewhere.
        if(sEvents == null) {
            synchronized (sSingletonSync) {
                if(sEvents == null) {
                    sEvents = new Events(Log::getSessionId);
                    return sEvents;
                }
            }
        }
        return sEvents;
    }

    private static Sessions getSessions() {
        // Checking for null again outside of synchronization because we only need to synchronize
        // during the lazy loading of the session logger. We don't need to synchronize elsewhere.
        if(sSessions == null) {
            synchronized (sSingletonSync) {
                if(sSessions == null) {
                    sSessions = new Sessions();
                    return sSessions;
                }
            }
        }
        return sSessions;
    }

    private static MessageDigest sMessageDigest;

    static void initMd5Sum() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... args) {
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    md = null;
                }
                sMessageDigest = md;
                return null;
            }
        }.execute();
    }

    public static void setTag(String tag) {
        TAG = tag;
    }

    @VisibleForTesting
    public static void setLoggingContainer(SystemLoggingContainer logger) {
        systemLogger = logger;
    }

    /**
     * If user enabled extended logging is enabled and the time limit has passed, disables the
     * extended logging.
     */
    private static void maybeDisableLogging() {
        if (!mIsUserExtendedLoggingEnabled) {
            return;
        }

        if (mUserExtendedLoggingStopTime < System.currentTimeMillis()) {
            mUserExtendedLoggingStopTime = 0;
            mIsUserExtendedLoggingEnabled = false;
        }
    }

    public static boolean isLoggable(int level) {
        return FORCE_LOGGING || android.util.Log.isLoggable(TAG, level);
    }

    public static String piiHandle(Object pii) {
        if (pii == null || VERBOSE) {
            return String.valueOf(pii);
        }

        StringBuilder sb = new StringBuilder();
        if (pii instanceof Uri) {
            Uri uri = (Uri) pii;
            String scheme = uri.getScheme();

            if (!TextUtils.isEmpty(scheme)) {
                sb.append(scheme).append(":");
            }

            String textToObfuscate = uri.getSchemeSpecificPart();
            if (PhoneAccount.SCHEME_TEL.equals(scheme)) {
                for (int i = 0; i < textToObfuscate.length(); i++) {
                    char c = textToObfuscate.charAt(i);
                    sb.append(PhoneNumberUtils.isDialable(c) ? "*" : c);
                }
            } else if (PhoneAccount.SCHEME_SIP.equals(scheme)) {
                for (int i = 0; i < textToObfuscate.length(); i++) {
                    char c = textToObfuscate.charAt(i);
                    if (c != '@' && c != '.') {
                        c = '*';
                    }
                    sb.append(c);
                }
            } else {
                sb.append(pii(pii));
            }
        }

        return sb.toString();
    }

    /**
     * Redact personally identifiable information for production users.
     * If we are running in verbose mode, return the original string, otherwise
     * return a SHA-1 hash of the input string.
     */
    public static String pii(Object pii) {
        if (pii == null || VERBOSE) {
            return String.valueOf(pii);
        }
        return "[" + secureHash(String.valueOf(pii).getBytes()) + "]";
    }

    private static String secureHash(byte[] input) {
        if (sMessageDigest != null) {
            sMessageDigest.reset();
            sMessageDigest.update(input);
            byte[] result = sMessageDigest.digest();
            return encodeHex(result);
        } else {
            return "Uninitialized SHA1";
        }
    }

    private static String encodeHex(byte[] bytes) {
        StringBuffer hex = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            int byteIntValue = bytes[i] & 0xff;
            if (byteIntValue < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toString(byteIntValue, 16));
        }

        return hex.toString();
    }

    private static String getPrefixFromObject(Object obj) {
        return obj == null ? "<null>" : obj.getClass().getSimpleName();
    }

    private static String buildMessage(String prefix, String format, Object... args) {
        // Incorporate thread ID and calling method into prefix
        String sessionPostfix = "";
        Session currentSession = null;//TODO: Add sSessionMapper.get(getCallingThreadId());
        if (currentSession != null) {
            sessionPostfix = ": " + currentSession.toString();
        }

        String msg;
        try {
            msg = (args == null || args.length == 0) ? format
                    : String.format(Locale.US, format, args);
        } catch (IllegalFormatException ife) {
            e("Log", ife, "IllegalFormatException: formatString='%s' numArgs=%d", format,
                    args.length);
            msg = format + " (An error occurred while formatting the message.)";
        }
        return String.format(Locale.US, "%s: %s%s", prefix, msg, sessionPostfix);
    }
}
