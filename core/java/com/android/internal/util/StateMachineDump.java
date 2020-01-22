/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.text.TextUtils;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;

/**
 * @hide
 *
 * <p>A util to dump internal state of a {@link StateMachine} as a {@link StateMachineProto}
 * for debugging purposes.
 *
 * <p>It is created to avoid adding extra dependencies (mainly platform protos and
 * {@link android.util.proto.ProtoOutputStream}) to every target that uses StateMachine. Only add
 * it case by case when a target needs to dump {@link StateMachineProto}.
 */
public class StateMachineDump {
    private StateMachineDump() {
    }

    /**
     * Dump the StateMachine as StateMachineProto.
     *
     * If the output belongs to a sub message, the caller is responsible for wrapping this function
     * between {@link ProtoOutputStream#start(long)} and {@link ProtoOutputStream#end(long)}.
     *
     * @param sm      the StateMachine to be dumped
     * @param proto   the ProtoOutputStream to write to
     */
    public static void dump(StateMachine sm, ProtoOutputStream proto) {
        proto.write(StateMachineProto.NAME, sm.getName());
        proto.write(StateMachineProto.TOTAL_RECORDS, sm.getLogRecCount());
        IState currentState = sm.getCurrentState();
        if (currentState != null) {
            proto.write(StateMachineProto.CURRENT_STATE, currentState.getName());
        }
        final int logRecSize = sm.getLogRecSize();
        ArraySet<Long> whats = new ArraySet<>();
        for (int i = 0; i < logRecSize; i++) {
            StateMachine.LogRec rec = sm.getLogRec(i);
            dumpLogRec(rec, proto);
            whats.add(rec.getWhat());
        }
        for (long what : whats) {
            long kToken = proto.start(StateMachineProto.WHAT_KEYS);
            proto.write(StateMachineProto.WhatKey.WHAT, what);
            proto.write(StateMachineProto.WhatKey.WHAT_STRING, sm.getWhatToString((int) what));
            proto.end(kToken);
        }
    }

    private static void dumpLogRec(StateMachine.LogRec rec, ProtoOutputStream proto) {
        final long token = proto.start(StateMachineProto.LOG_RECORDS);
        proto.write(StateMachineProto.LogRecord.TIME, rec.getTime());
        proto.write(StateMachineProto.LogRecord.STATE, getStateString(rec.getState()));
        proto.write(StateMachineProto.LogRecord.ORG_STATE, getStateString(rec.getOriginalState()));
        proto.write(StateMachineProto.LogRecord.DST_STATE, getStateString(rec.getDestState()));
        proto.write(StateMachineProto.LogRecord.WHAT, rec.getWhat());
        if (!TextUtils.isEmpty(rec.getInfo())) {
            proto.write(StateMachineProto.LogRecord.INFO, rec.getInfo());
        }
        proto.end(token);
    }

    private static String getStateString(IState state) {
        return state == null ? "null" : state.getName();
    }
}
