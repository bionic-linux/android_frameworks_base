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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.HashMap;

/**
 * StateMahineHandler is responsible for providing detail implementation of state machine to handle
 * message and state transition.
 *
 * {@hide}
 */
public class StateMachineHandler extends Handler {
    /** Message.what value when quitting */
    protected static final int SM_QUIT_CMD = -1;

    /** Message.what value when initializing */
    protected static final int SM_INIT_CMD = -2;

    /**
     * Convenience constant that maybe returned by processMessage
     * to indicate the message was processed and is not to be
     * processed by parent states
     */
    public static final boolean HANDLED = true;

    /**
     * Convenience constant that maybe returned by processMessage
     * to indicate the message was NOT processed and is to be
     * processed by parent states
     */
    public static final boolean NOT_HANDLED = false;

    /** true if construction of the state machine has not been completed */
    protected boolean mIsConstructionCompleted;

    /** The map of all of the states in the state machine */
    private HashMap<State, StateInfo> mStateInfo = new HashMap<State, StateInfo>();

    /** Stack used to manage the current hierarchy of states */
    protected StateInfo mStateStack[];

    /** Top of mStateStack */
    protected int mStateStackTopIndex = -1;

    /** A temporary stack used to manage the state stack */
    protected StateInfo mTempStateStack[];

    /** The top of the mTempStateStack */
    protected int mTempStateStackCount;

    /** The initial state that will process the first message */
    private State mInitialState;

    /** The destination state when transitionTo has been invoked */
    protected State mDestState;

    /**
     * Indicates if a transition is in progress
     *
     * This will be true for all calls of State.exit and all calls of State.enter except for the
     * last enter call for the current destination state.
     */
    private boolean mTransitionInProgress = false;

    /**
     * Information about a state.
     * Used to maintain the hierarchy.
     */
    protected class StateInfo {
        /** The state */
        State state;

        /** The parent of this state, null if there is no parent */
        StateInfo parentStateInfo;

        /** True when the state has been entered and on the stack */
        boolean active;

        /**
         * Convert StateInfo to string
         */
        @Override
        public String toString() {
            return "state=" + state.getName() + ",active=" + active + ",parent="
                    + ((parentStateInfo == null) ? "null" : parentStateInfo.state.getName());
        }
    }

    protected StateMachineHandler(Looper looper) {
        super(looper);
    }

    /**
     * Complete the construction of the state machine.
     */
    protected void completeConstruction() {
        /**
         * Determine the maximum depth of the state hierarchy
         * so we can allocate the state stacks.
         */
        int maxDepth = 0;
        for (StateInfo si : mStateInfo.values()) {
            int depth = 0;
            for (StateInfo i = si; i != null; depth++) {
                i = i.parentStateInfo;
            }
            if (maxDepth < depth) {
                maxDepth = depth;
            }
        }
        log("completeConstruction: maxDepth=" + maxDepth);

        mStateStack = new StateInfo[maxDepth];
        mTempStateStack = new StateInfo[maxDepth];
        setupInitialStateStack();
    }

    /**
     * Initialize StateStack to mInitialState.
     */
    private final void setupInitialStateStack() {
        log("setupInitialStateStack: E mInitialState=" + mInitialState.getName());

        StateInfo curStateInfo = mStateInfo.get(mInitialState);
        for (mTempStateStackCount = 0; curStateInfo != null; mTempStateStackCount++) {
            mTempStateStack[mTempStateStackCount] = curStateInfo;
            curStateInfo = curStateInfo.parentStateInfo;
        }

        // Empty the StateStack
        mStateStackTopIndex = -1;

        moveTempStateStackToStateStack();
    }

    /**
     * Move the contents of the temporary stack to the state stack
     * reversing the order of the items on the temporary stack as
     * they are moved.
     *
     * @return index into mStateStack where entering needs to start
     */
    protected final int moveTempStateStackToStateStack() {
        int startingIndex = mStateStackTopIndex + 1;
        int i = mTempStateStackCount - 1;
        int j = startingIndex;
        while (i >= 0) {
            log("moveTempStackToStateStack: i=" + i + ",j=" + j);
            mStateStack[j] = mTempStateStack[i];
            j += 1;
            i -= 1;
        }

        mStateStackTopIndex = j - 1;
        log("moveTempStackToStateStack: X mStateStackTop=" + mStateStackTopIndex
                + ",startingIndex=" + startingIndex + ",Top="
                + mStateStack[mStateStackTopIndex].state.getName());
        return startingIndex;
    }

    /** Set initial state of state machine. */
    public void setInitialState(State initialState) {
        log("setInitialState: initialState=" + initialState.getName());
        mInitialState = initialState;
    }

    /** Transition state to destState. */
    public final void transitionTo(IState destState) {
        if (mTransitionInProgress) {
            logwtf("transitionTo called while transition already in progress to " + mDestState
                    + ", new target state=" + destState);
        }
        mDestState = (State) destState;
        log("transitionTo: destState=" + mDestState.getName());
    }

    /**
     * Cleanup SM handler after the SM has been quit.
     */
    protected void cleanupAfterQuitting() {
        mStateStack = null;
        mTempStateStack = null;
        mStateInfo.clear();
        mInitialState = null;
    }

    /**
     * Do any transitions
     * @param msgProcessedState is the state that processed the message
     * @return the finial destination state after transitions.
     */
    protected State performTransitions(State destState) {
        /**
         * Process the transitions including transitions in the enter/exit methods
         */
        while (true) {
            log("handleMessage: new destination call exit/enter");

            /**
             * Determine the states to exit and enter and return the
             * common ancestor state of the enter/exit states. Then
             * invoke the exit methods then the enter methods.
             */
            StateInfo commonStateInfo = setupTempStateStackWithStatesToEnter(destState);
            // flag is cleared in invokeEnterMethods before entering the target state
            mTransitionInProgress = true;
            invokeExitMethods(commonStateInfo);
            int stateStackEnteringIndex = moveTempStateStackToStateStack();
            invokeEnterMethods(stateStackEnteringIndex);

            /**
             * Since we have transitioned to a new state we need to have
             * any deferred messages moved to the front of the message queue
             * so they will be processed before any other messages in the
             * message queue.
             */
            moveDeferredMessageAtFrontOfQueue();

            if (destState != mDestState) {
                // A new mDestState so continue looping
                destState = mDestState;
            } else {
                // No change in mDestState so we're done
                break;
            }
        }
        mDestState = null;
        return destState;
    }

    /**
     * Call the exit method for each state from the top of stack
     * up to the common ancestor state.
     */
    private final void invokeExitMethods(StateInfo commonStateInfo) {
        while ((mStateStackTopIndex >= 0)
                && (mStateStack[mStateStackTopIndex] != commonStateInfo)) {
            State curState = mStateStack[mStateStackTopIndex].state;
            log("invokeExitMethods: " + curState.getName());
            curState.exit();
            mStateStack[mStateStackTopIndex].active = false;
            mStateStackTopIndex -= 1;
        }
    }

    /**
     * Invoke the enter method starting at the entering index to top of state stack
     */
    protected final void invokeEnterMethods(int stateStackEnteringIndex) {
        for (int i = stateStackEnteringIndex; i <= mStateStackTopIndex; i++) {
            if (stateStackEnteringIndex == mStateStackTopIndex) {
                // Last enter state for transition
                mTransitionInProgress = false;
            }
            log("invokeEnterMethods: " + mStateStack[i].state.getName());
            mStateStack[i].state.enter();
            mStateStack[i].active = true;
        }
        mTransitionInProgress = false; // ensure flag set to false if no methods called
    }

    /**
     * Setup the mTempStateStack with the states we are going to enter.
     *
     * This is found by searching up the destState's ancestors for a
     * state that is already active i.e. StateInfo.active == true.
     * The destStae and all of its inactive parents will be on the
     * TempStateStack as the list of states to enter.
     *
     * @return StateInfo of the common ancestor for the destState and
     * current state or null if there is no common parent.
     */
    private final StateInfo setupTempStateStackWithStatesToEnter(State destState) {
        /**
         * Search up the parent list of the destination state for an active
         * state. Use a do while() loop as the destState must always be entered
         * even if it is active. This can happen if we are exiting/entering
         * the current state.
         */
        mTempStateStackCount = 0;
        StateInfo curStateInfo = mStateInfo.get(destState);
        do {
            mTempStateStack[mTempStateStackCount++] = curStateInfo;
            curStateInfo = curStateInfo.parentStateInfo;
        } while ((curStateInfo != null) && !curStateInfo.active);

        log("setupTempStateStackWithStatesToEnter: X mTempStateStackCount="
                + mTempStateStackCount + ",curStateInfo: " + curStateInfo);
        return curStateInfo;
    }

    /**
     * Add a new state to the state machine. Bottom up addition
     * of states is allowed but the same state may only exist
     * in one hierarchy.
     *
     * @param state the state to add
     * @param parent the parent of state
     * @return stateInfo for this state
     */
    public final StateInfo addState(State state, State parent) {
        log("addStateInternal: E state=" + state.getName() + ",parent="
                + ((parent == null) ? "" : parent.getName()));
        StateInfo parentStateInfo = null;
        if (parent != null) {
            parentStateInfo = mStateInfo.get(parent);
            if (parentStateInfo == null) {
                // Recursively add our parent as it's not been added yet.
                parentStateInfo = addState(parent, null);
            }
        }
        StateInfo stateInfo = mStateInfo.get(state);
        if (stateInfo == null) {
            stateInfo = new StateInfo();
            mStateInfo.put(state, stateInfo);
        }

        // Validate that we aren't adding the same state in two different hierarchies.
        if ((stateInfo.parentStateInfo != null)
                && (stateInfo.parentStateInfo != parentStateInfo)) {
            throw new RuntimeException("state already added");
        }
        stateInfo.state = state;
        stateInfo.parentStateInfo = parentStateInfo;
        stateInfo.active = false;
        log("addStateInternal: X stateInfo: " + stateInfo);
        return stateInfo;
    }

    /**
     * Remove a state from the state machine. Will not remove the state if it is currently
     * active or if it has any children in the hierarchy.
     * @param state the state to remove
     */
    public void removeState(State state) {
        StateInfo stateInfo = mStateInfo.get(state);
        if (stateInfo == null || stateInfo.active) {
            return;
        }
        boolean isParent = mStateInfo.values().stream()
                .filter(si -> si.parentStateInfo == stateInfo)
                .findAny()
                .isPresent();
        if (isParent) {
            return;
        }
        mStateInfo.remove(state);
    }

    /**
     * @return current state
     */
    public final IState getCurrentState() {
        return mStateStack[mStateStackTopIndex].state;
    }

    protected void onPreHandleMessage(Message msg) { }

    protected void onPostHandleMessage(Message msg) { }

    protected void unhandledMessage(Message msg) { }

    protected void log(final String s) { }

    protected void logwtf(final String s) { }

    protected void maybeRecordTransitionLogMessage(State msgProcessedState, Message msg) { }

    /** Move the deferred message to the front of the message queue. */
    protected void moveDeferredMessageAtFrontOfQueue() { }
}
