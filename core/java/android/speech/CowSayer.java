/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.speech;

import android.annotation.NonNull;
import android.annotation.SuppressLint;

@SuppressLint("UnflaggedApi")
public class CowSayer {


    @SuppressLint("UnflaggedApi")
    public CowSayer() {}

    /**
     * Speak as a cow.
     *
     * @param msg
     *  The intended addressee.
     *
     * @return
     *  A greeting message.
     */
    @NonNull
    @SuppressLint("UnflaggedApi")
    public String say(@NonNull String msg) {  // why does nullable param throw build errors?

        return String.format("----- \n"
                        + "%s \n"
                        + "------ \n"
                        + "\\   ^__^ \n"
                        + "\\   (oo)\\ ________ \n"
                        + "     (__)\\         )\\ /\\ \n"
                        + "          ||------w|\n"
                        + "          ||      ||",
                msg);
    }
}
