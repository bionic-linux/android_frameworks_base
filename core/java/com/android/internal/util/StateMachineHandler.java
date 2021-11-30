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

    /** The debug flag */
    protected boolean mDbg = false;

    /** Reference to the StateMachine */
    protected IStateMachine mSm;

    /** true if construction of the state machine has not been completed */
    protected boolean mIsConstructionCompleted;

    /** The map of all of the states in the state machine */
    protected HashMap<State, StateInfo> mStateInfo = new HashMap<State, StateInfo>();

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
    }
}
