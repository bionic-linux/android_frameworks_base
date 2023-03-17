/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.statusbar.phone

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.LocaleList
import android.view.View.LAYOUT_DIRECTION_RTL
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.policy.ConfigurationController

import java.util.ArrayList
import javax.inject.Inject

@SysUISingleton
class ConfigurationControllerImpl @Inject constructor(context: Context) : ConfigurationController {

    private val listeners: MutableList<ConfigurationController.ConfigurationListener> = ArrayList()
    private val lastConfig = Configuration()
    private var density: Int = 0
    private var smallestScreenWidth: Int = 0
    private var maxBounds = Rect()
    private var fontScale: Float = 0.toFloat()
    private val inCarMode: Boolean
    private var uiMode: Int = 0
    private var localeList: LocaleList? = null
    private val context: Context
    private var layoutDirection: Int

    init {
        val currentConfig = context.resources.configuration
        this.context = context
        fontScale = currentConfig.fontScale
        density = currentConfig.densityDpi
        smallestScreenWidth = currentConfig.smallestScreenWidthDp
        maxBounds.set(currentConfig.windowConfiguration.maxBounds)
        inCarMode = currentConfig.uiMode and Configuration.UI_MODE_TYPE_MASK ==
                Configuration.UI_MODE_TYPE_CAR
        uiMode = currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        localeList = currentConfig.locales
        layoutDirection = currentConfig.layoutDirection
    }

    override fun notifyThemeChanged() {
        val listeners = ArrayList(listeners)

        listeners.asSequence().filter { listeners.contains(it) }.forEach {
            it.onThemeChanged()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Avoid concurrent modification exception
        val listeners = ArrayList(listeners)

        listeners.asSequence().filter { listeners.contains(it) }.forEach {
            it.onConfigChanged(newConfig)
        }
        val fontScale = newConfig.fontScale
        val density = newConfig.densityDpi
        val uiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val uiModeChanged = uiMode != this.uiMode
        if (density != this.density || fontScale != this.fontScale ||
                inCarMode && uiModeChanged) {
            listeners.asSequence().filter { listeners.contains(it) }.forEach {
                it.onDensityOrFontScaleChanged()
            }
            this.density = density
            this.fontScale = fontScale
        }

        val smallestScreenWidth = newConfig.smallestScreenWidthDp
        if (smallestScreenWidth != this.smallestScreenWidth) {
            this.smallestScreenWidth = smallestScreenWidth
            listeners.asSequence().filter { listeners.contains(it) }.forEach {
                it.onSmallestScreenWidthChanged()
            }
        }

        val maxBounds = newConfig.windowConfiguration.maxBounds
        if (maxBounds != this.maxBounds) {
            // Update our internal rect to have the same bounds, instead of using
            // `this.maxBounds = maxBounds` directly. Setting it directly means that `maxBounds`
            // would be a direct reference to windowConfiguration.maxBounds, so the if statement
            // above would always fail. See b/245799099 for more information.
            this.maxBounds.set(maxBounds)
            listeners.asSequence().filter { listeners.contains(it) }.forEach {
                it.onMaxBoundsChanged()
            }
        }

        val localeList = newConfig.locales
        if (localeList != this.localeList) {
            this.localeList = localeList
            listeners.asSequence().filter { listeners.contains(it) }.forEach {
                it.onLocaleListChanged()
            }
        }

        if (uiModeChanged) {
            // We need to force the style re-evaluation to make sure that it's up to date
            // and attrs were reloaded.
            context.theme.applyStyle(context.themeResId, true)

            this.uiMode = uiMode
            listeners.asSequence().filter { listeners.contains(it) }.forEach {
                it.onUiModeChanged()
            }
        }

        if (layoutDirection != newConfig.layoutDirection) {
            layoutDirection = newConfig.layoutDirection
            listeners.asSequence().filter { listeners.contains(it) }.forEach {
                it.onLayoutDirectionChanged(layoutDirection == LAYOUT_DIRECTION_RTL)
            }
        }

        if (lastConfig.updateFrom(newConfig) and ActivityInfo.CONFIG_ASSETS_PATHS != 0) {
            listeners.asSequence().filter { listeners.contains(it) }.forEach {
                it.onThemeChanged()
            }
        }
    }

    override fun addCallback(listener: ConfigurationController.ConfigurationListener) {
        listeners.add(listener)
        listener.onDensityOrFontScaleChanged()
    }

    override fun removeCallback(listener: ConfigurationController.ConfigurationListener) {
        listeners.remove(listener)
    }

    override fun isLayoutRtl(): Boolean {
        return layoutDirection == LAYOUT_DIRECTION_RTL
    }
}
