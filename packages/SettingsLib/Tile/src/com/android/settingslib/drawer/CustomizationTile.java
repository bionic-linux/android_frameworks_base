/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settingslib.drawer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.os.Parcel;

import java.util.Objects;

/**
 * An invisible Tile used as a metadata container for customization of Preferences
 * (add PreferenceCategory, move/remove Preferences)
 */
public class CustomizationTile extends Tile {

    public CustomizationTile(ActivityInfo info, String category) {
        super(info, category, info.metaData);
    }

    CustomizationTile(Parcel in) {
        super(in);
    }

    @Override
    public int getId() {
        return Objects.hash(getPackageName(), getComponentName());
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    protected ComponentInfo getComponentInfo(Context context) {
        // ComponentInfo for a CustomizationTile isn't supported.
        return null;
    }

    @Override
    protected CharSequence getComponentLabel(Context context) {
        // Label for a CustomizationTile isn't supported.
        return null;
    }

    @Override
    protected int getComponentIcon(ComponentInfo componentInfo) {
        // Icon for a CustomizationTile isn't supported.
        return 0;
    }
}
