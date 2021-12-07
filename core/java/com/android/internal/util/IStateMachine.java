/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.util;

import android.os.Message;
import android.util.Log;

/**
 * Interface that handle the StateMachine messages.
 */
abstract class IStateMachine {
    /**
     * @return the name of StateMachine.
     */
    public abstract String getName();

    /** Cleanup StateMachine. */
    protected abstract void cleanupAfterQuitting();

    /**
     * @return true if msg should be saved in the log, default is true.
     */
    protected boolean recordLogRec(Message msg) {
        return true;
    }

    /**
     * Notifies subclass that the StateMachine handler is about to process the Message msg.
     * @param msg The message that is being handled.
     */
    protected void onPreHandleMessage(Message msg) { }

    /**
     * Notifies subclass that the StateMachine handler has finished processing the Message msg and
     * has possibly transitioned to a new state.
     * @param msg The message that is being handled.
     */
    protected void onPostHandleMessage(Message msg) { }

    /**
     * Called when message wasn't handled.
     *
     * @param msg that couldn't be handled.
     */
    protected void unhandledMessage(Message msg) { }

    /**
     * Called for any message that is received after
     * transitionToHalting is called.
     */
    protected void haltedProcessMessage(Message msg) { }

    /**
     * This will be called once after handling a message that called
     * transitionToHalting. All subsequent messages will invoke
     * {@link IStateMachine#haltedProcessMessage(Message)}
     */
    protected void onHalting() { }

    /**
     * This will be called once after a quit message that was NOT handled by
     * the derived StateMachine. The StateMachine will stop and any subsequent messages will be
     * ignored. In addition, if this StateMachine created the thread, the thread will
     * be stopped after this method returns.
     */
    protected void onQuitting() { }

    /**
     * @return the string for Message.what
     */
    protected String getWhatToString(int what) {
        return null;
    }

    /**
     * Return a string to be logged by LogRec, default
     * is an empty string. Override if additional information is desired.
     *
     * @param msg that was processed
     * @return information to be logged as a String
     */
    protected String getLogRecString(Message msg) {
        return "";
    }

    /**
     * Log with debug
     *
     * @param s is string log
     */
    protected void log(String s) {
        Log.d(getName(), s);
    }
}
