/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net;

/**
 * Corresponds to C's {@code struct tcp_repair_window} from
 * include/uapi/linux/tcp.h
 *
 * @hide
 */
public final class TcpRepairWindow {
    public final int snd_wl1;
    public final int snd_wnd;
    public final int max_window;
    public final int rcv_wnd;
    public final int rcv_wup;

    /**
     * Constructs an instance with the given field values.
     */
    public TcpRepairWindow(int snd_wl1, int snd_wnd,
            int max_window, int rcv_wnd, int rcv_wup) {
        this.snd_wl1 = snd_wl1;
        this.snd_wnd = snd_wnd;
        this.max_window = max_window;
        this.rcv_wnd = rcv_wnd;
        this.rcv_wup = rcv_wup;
    }
}
