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

/**
 * StateMahineHandler is responsible for providing detail implementation of state machine to handle
 * message and state transition.
 *
 * {@hide}
 */
public class StateMachineHandler extends Handler {
    protected StateMachineHandler(Looper looper) {
        super(looper);
    }

    protected void onPreHandleMessage(Message msg) { }

    protected void onPostHandleMessage(Message msg) { }

    protected void unhandledMessage(Message msg) { }

    protected void log(final String s) { }

    protected void logwtf(final String s) { }

    protected void maybeRecordTransitionLogMessage(State msgProcessedState, Message msg) { }
}
