/**
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

import android.os.Looper;
import android.os.Message;

import com.android.internal.annotations.VisibleForTesting;

/**
 * The synchronized state machine defined here is a hierarchical state machine which processes
 * messages and can have states arranged hierarchically. The caller should always run message in
 * state machine handler thread and the mesasge would be process synchronized.
 */
public class SynchronizedStateMachine extends IStateMachine {
    private final boolean mTestMode;
    // Name of the state machine and used as logging tag
    private String mName;

    private StateMachineHandler mSmHandler;

    @Override
    protected final void cleanupAfterQuitting() {
        mSmHandler = null;
    }

    /**
     * Constructor creates a Synchronized StateMachine using the looper.
     *
     * @param name of the state machine
     * @param looper of the state machine
     */
    public SynchronizedStateMachine(String name, Looper looper) {
        this(name, looper, false /* isTest */);
    }

    /**
     * Constructor creates a Synchronized StateMachine.
     *
     * @param name of the state machine
     * @param looper of the state machine
     * @param isTest indicate whether this is test mode.
     */
    @VisibleForTesting
    public SynchronizedStateMachine(String name, Looper looper, boolean isTest) {
        mTestMode = isTest;
        mName = name;
        mSmHandler = new StateMachineHandler(looper, this);
    }
    /**
     * Add a new state to the state machine
     * @param state the state to add
     * @param parent the parent of state
     */
    public final void addState(State state, State parent) {
        mSmHandler.addState(state, parent);
    }

    /**
     * Removes a state from the state machine, unless it is currently active or if it has children.
     * @param state state to remove
     */
    public final void removeState(State state) {
        mSmHandler.removeState(state);
    }

    /**
     * Set the initial state. This must be invoked before
     * and messages are sent to the state machine.
     *
     * @param initialState is the state which will receive the first message.
     */
    public final void setInitialState(State initialState) {
        mSmHandler.setInitialState(initialState);
    }

    /**
     * @return current state
     */
    public final IState getCurrentState() {
        return mSmHandler.getCurrentState();
    }

    /**
     * transition to destination state. Upon returning
     * from processMessage the current state's exit will
     * be executed and upon the next message arriving
     * destState.enter will be invoked.
     *
     * this function can also be called inside the enter function of the
     * previous transition target, but the behavior is undefined when it is
     * called mid-way through a previous transition (for example, calling this
     * in the enter() routine of a intermediate node when the current transition
     * target is one of the nodes descendants).
     *
     * @param destState will be the state that receives the next message.
     */
    public final void transitionTo(IState destState) {
        mSmHandler.transitionTo(destState);
    }

    /**
     * @return the name
     */
    public final String getName() {
        return mName;
    }

    /**
     * Run a message to this state machine.
     *
     * Message is ignored if state machine has quit.
     */
    public void runMessage(int what) {
        runMessage(Message.obtain(mSmHandler, what));
    }

    /**
     * Run a message to this state machine.
     *
     * Message is ignored if state machine has quit.
     */
    public void runMessage(int what, int arg1) {
        runMessage(Message.obtain(mSmHandler, what, arg1));
    }

    /**
     * Run a message to this state machine.
     *
     * Message is ignored if state machine has quit.
     */
    public void runMessage(int what, int arg1, int arg2) {
        runMessage(Message.obtain(mSmHandler, what, arg1, arg2));
    }

    /**
     * Run a message to this state machine.
     *
     * Message is ignored if state machine has quit.
     */
    public void runMessage(int what, int arg1, int arg2, Object obj) {
        runMessage(Message.obtain(mSmHandler, what, arg1, arg2, obj));
    }

    /**
     * Run a message to this state machine.
     *
     * Message is ignored if state machine has quit.
     */
    public void runMessage(Message msg) {
        if (!mSmHandler.getLooper().isCurrentThread() && !mTestMode) {
            throw new RuntimeException(
                    "Synchronized StateMachine doesn't support mutilple threads");
        }

        mSmHandler.dispatchMessage(msg);
    }

}
