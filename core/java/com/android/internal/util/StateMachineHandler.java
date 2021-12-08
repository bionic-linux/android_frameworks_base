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

import static com.android.internal.util.IStateMachine.NOT_HANDLED;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.internal.util.StateMachine.LogRec;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

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

    /** The SmHandler object, identifies that message is internal */
    protected static final Object mSmHandlerObj = new Object();

    /** The debug flag */
    protected boolean mDbg = false;

    /** Reference to the StateMachine */
    protected IStateMachine mSm;

    /** The current message */
    protected Message mMsg;

    /** A list of log records including messages this state machine has processed */
    protected LogRecords mLogRecords = new LogRecords();

    /** true if construction of the state machine has not been completed */
    protected boolean mIsConstructionCompleted;

    /** The map of all of the states in the state machine */
    private HashMap<State, StateInfo> mStateInfo = new HashMap<State, StateInfo>();

    /** Stack used to manage the current hierarchy of states */
    protected StateInfo[] mStateStack;

    /** Top of mStateStack */
    protected int mStateStackTopIndex = -1;

    /** A temporary stack used to manage the state stack */
    protected StateInfo[] mTempStateStack;

    /** The top of the mTempStateStack */
    protected int mTempStateStackCount;

    /** The initial state that will process the first message */
    private State mInitialState;

    /** The destination state when transitionTo has been invoked */
    protected State mDestState;

    /** State used when state machine is halted */
    protected HaltingState mHaltingState = new HaltingState();

    /** State used when state machine is quitting */
    protected QuittingState mQuittingState = new QuittingState();

    /**
     * Indicates if a transition is in progress
     *
     * This will be true for all calls of State.exit and all calls of State.enter except for the
     * last enter call for the current destination state.
     */
    private boolean mTransitionInProgress = false;

    /**
     * A list of log records including messages recently processed by the state machine.
     *
     * The class maintains a list of log records including messages
     * recently processed. The list is finite and may be set in the
     * constructor or by calling setSize. The public interface also
     * includes size which returns the number of recent records,
     * count which is the number of records processed since the
     * the last setSize, get which returns a record and
     * add which adds a record.
     */
    static class LogRecords {

        private static final int DEFAULT_SIZE = 20;

        private Vector<LogRec> mLogRecVector = new Vector<LogRec>();
        private int mMaxSize = DEFAULT_SIZE;
        private int mOldestIndex = 0;
        private int mCount = 0;
        private boolean mLogOnlyTransitions = false;

        /**
         * private constructor use add
         */
        private LogRecords() {
        }

        /**
         * Set size of messages to maintain and clears all current records.
         *
         * @param maxSize number of records to maintain at anyone time.
        */
        synchronized void setSize(int maxSize) {
            // TODO: once b/28217358 is fixed, add unit tests  to verify that these variables are
            // cleared after calling this method, and that subsequent calls to get() function as
            // expected.
            mMaxSize = maxSize;
            mOldestIndex = 0;
            mCount = 0;
            mLogRecVector.clear();
        }

        synchronized void setLogOnlyTransitions(boolean enable) {
            mLogOnlyTransitions = enable;
        }

        synchronized boolean logOnlyTransitions() {
            return mLogOnlyTransitions;
        }

        /**
         * @return the number of recent records.
         */
        synchronized int size() {
            return mLogRecVector.size();
        }

        /**
         * @return the max number of records.
         */
        synchronized int getMaxSize() {
            return mMaxSize;
        }

        /**
         * @return the total number of records processed since size was set.
         */
        synchronized int count() {
            return mCount;
        }

        /**
         * Clear the list of records.
         */
        synchronized void cleanup() {
            mLogRecVector.clear();
        }

        /**
         * @return the information on a particular record. 0 is the oldest
         * record and size()-1 is the newest record. If the index is to
         * large null is returned.
         */
        synchronized LogRec get(int index) {
            int nextIndex = mOldestIndex + index;
            if (nextIndex >= mMaxSize) {
                nextIndex -= mMaxSize;
            }
            if (nextIndex >= size()) {
                return null;
            } else {
                return mLogRecVector.get(nextIndex);
            }
        }

        /**
         * Add a processed message.
         *
         * @param msg
         * @param messageInfo to be stored
         * @param state that handled the message
         * @param orgState is the first state the received the message but
         * did not processes the message.
         * @param transToState is the state that was transitioned to after the message was
         * processed.
         *
         */
        synchronized void add(IStateMachine sm, Message msg, String messageInfo, IState state,
                IState orgState, IState transToState) {
            mCount += 1;
            if (mLogRecVector.size() < mMaxSize) {
                mLogRecVector.add(new LogRec(sm, msg, messageInfo, state, orgState, transToState));
            } else {
                LogRec pmi = mLogRecVector.get(mOldestIndex);
                mOldestIndex += 1;
                if (mOldestIndex >= mMaxSize) {
                    mOldestIndex = 0;
                }
                pmi.update(sm, msg, messageInfo, state, orgState, transToState);
            }
        }

        /**
         * @return a copy of LogRecs as a collection
         */
        synchronized Collection<LogRec> copyLogRecs() {
            Vector<LogRec> vlr = new Vector<LogRec>();
            for (LogRec lr : mLogRecVector) {
                vlr.add(lr);
            }

            return vlr;
        }
    }

    /**
     * Information about a state.
     * Used to maintain the hierarchy.
     */
    protected class StateInfo {
        /** The state */
        public State state;

        /** The parent of this state, null if there is no parent */
        public StateInfo parentStateInfo;

        /** True when the state has been entered and on the stack */
        public boolean active;

        /**
         * Convert StateInfo to string
         */
        @Override
        public String toString() {
            return "state=" + state.getName() + ",active=" + active + ",parent="
                    + ((parentStateInfo == null) ? "null" : parentStateInfo.state.getName());
        }
    }

    /**
     * State entered when transitionToHaltingState is called.
     */
    public class HaltingState extends State {
        @Override
        public boolean processMessage(Message msg) {
            mSm.haltedProcessMessage(msg);
            return true;
        }
    }

    /**
     * State entered when a valid quit message is handled.
     */
    public class QuittingState extends State {
        @Override
        public boolean processMessage(Message msg) {
            return NOT_HANDLED;
        }
    }

    protected StateMachineHandler(Looper looper, IStateMachine sm) {
        super(looper);

        mSm = sm;
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
        if (mDbg) mSm.log("completeConstruction: maxDepth=" + maxDepth);

        mStateStack = new StateInfo[maxDepth];
        mTempStateStack = new StateInfo[maxDepth];
        setupInitialStateStack();
    }

    /**
     * Initialize StateStack to mInitialState.
     */
    private void setupInitialStateStack() {
        if (mDbg) mSm.log("setupInitialStateStack: E mInitialState=" + mInitialState.getName());

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
            if (mDbg) mSm.log("moveTempStackToStateStack: i=" + i + ",j=" + j);
            mStateStack[j] = mTempStateStack[i];
            j += 1;
            i -= 1;
        }

        mStateStackTopIndex = j - 1;
        if (mDbg) {
            mSm.log("moveTempStackToStateStack: X mStateStackTop=" + mStateStackTopIndex
                    + ",startingIndex=" + startingIndex + ",Top="
                    + mStateStack[mStateStackTopIndex].state.getName());
        }
        return startingIndex;
    }

    /** Set initial state of state machine. */
    public void setInitialState(State initialState) {
        if (mDbg) mSm.log("setInitialState: initialState=" + initialState.getName());
        mInitialState = initialState;
    }

    /** Transition state to destState. */
    public final void transitionTo(IState destState) {
        if (mTransitionInProgress) {
            Log.wtf(mSm.getName(), "transitionTo called while transition already in progress to "
                    + mDestState + ", new target state=" + destState);
        }
        mDestState = (State) destState;
        if (mDbg) mSm.log("transitionTo: destState=" + mDestState.getName());
    }

    /**
     * Cleanup SM handler after the SM has been quit.
     */
    protected void cleanupAfterQuitting() {
        mSm.cleanupAfterQuitting();
        mSm = null;
        mStateStack = null;
        mTempStateStack = null;
        mStateInfo.clear();
        mInitialState = null;
        mDestState = null;
        mMsg = null;
        mLogRecords.cleanup();
    }

    /**
     * Do any transitions
     * @param msgProcessedState is the state that processed the message
     * @return the finial destination state after transitions.
     */
    private void performTransitions(State destState) {
        /**
         * Process the transitions including transitions in the enter/exit methods
         */
        while (true) {
            if (mDbg) mSm.log("handleMessage: new destination call exit/enter");

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

        /**
         * After processing all transitions check and
         * see if the last transition was to quit or halt.
         */
        if (destState == mQuittingState) {
            /**
             * Call onQuitting to let subclasses cleanup.
             */
            mSm.onQuitting();
            cleanupAfterQuitting();
        } else if (destState == mHaltingState) {
            /**
             * Call onHalting() if we've transitioned to the halting
             * state. All subsequent messages will be processed in
             * in the halting state which invokes haltedProcessMessage(msg);
             */
            mSm.onHalting();
        }
    }

    /**
     * Do transitions if destState is available
     * @param msgProcessedState is the state that processed the message
     */
    protected void maybePerformTransitions(State msgProcessedState, Message msg) {
        /**
         * If transitionTo has been called, exit and then enter
         * the appropriate states. We loop on this to allow
         * enter and exit methods to use transitionTo.
         */
        State orgState = mStateStack[mStateStackTopIndex].state;

        /**
         * Record whether message needs to be logged before we transition and
         * and we won't log special messages SM_INIT_CMD or SM_QUIT_CMD which
         * always set msg.obj to the handler.
         */
        boolean recordLogMsg = mSm.recordLogRec(mMsg) && (msg.obj != mSmHandlerObj);

        if (mLogRecords.logOnlyTransitions()) {
            /** Record only if there is a transition */
            if (mDestState != null) {
                mLogRecords.add(mSm, mMsg, mSm.getLogRecString(mMsg), msgProcessedState,
                        orgState, mDestState);
            }
        } else if (recordLogMsg) {
            /** Record message */
            mLogRecords.add(mSm, mMsg, mSm.getLogRecString(mMsg), msgProcessedState, orgState,
                    mDestState);
        }

        if (mDestState != null) performTransitions(mDestState);
    }


    /**
     * Call the exit method for each state from the top of stack
     * up to the common ancestor state.
     */
    private void invokeExitMethods(StateInfo commonStateInfo) {
        while ((mStateStackTopIndex >= 0)
                && (mStateStack[mStateStackTopIndex] != commonStateInfo)) {
            State curState = mStateStack[mStateStackTopIndex].state;
            if (mDbg) mSm.log("invokeExitMethods: " + curState.getName());
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
            if (mDbg) mSm.log("invokeEnterMethods: " + mStateStack[i].state.getName());
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
    private StateInfo setupTempStateStackWithStatesToEnter(State destState) {
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

        if (mDbg) {
            mSm.log("setupTempStateStackWithStatesToEnter: X mTempStateStackCount="
                    + mTempStateStackCount + ",curStateInfo: " + curStateInfo);
        }
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
        if (mDbg) {
            mSm.log("addStateInternal: E state=" + state.getName() + ",parent="
                    + ((parent == null) ? "" : parent.getName()));
        }
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
        if (mDbg) mSm.log("addStateInternal: X stateInfo: " + stateInfo);
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

    /** Move the deferred message to the front of the message queue. */
    protected void moveDeferredMessageAtFrontOfQueue() { }
}
