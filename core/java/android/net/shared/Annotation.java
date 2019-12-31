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

package android.net.shared;

import android.annotation.IntDef;
import android.net.IpSecManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Collection of network annotations.
 *
 * <p>Mainline modules cannot reference hidden @IntDef. Moving network annotations to this class to
 * allow others to statically include it.
 *
 * @hide
 */
public class Annotation {
    @IntDef(value = {IpSecManager.DIRECTION_IN, IpSecManager.DIRECTION_OUT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PolicyDirection {}
}
