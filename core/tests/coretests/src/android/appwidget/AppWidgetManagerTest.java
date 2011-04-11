/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.appwidget;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.test.AndroidTestCase;

import java.util.List;

public class AppWidgetManagerTest extends AndroidTestCase {

    // For some reason it takes a really long time before the enable settings to
    // take effect in the widget provider list.
    private static final long WIDGET_CHANGE_TIME = 12000;

    private PackageManager pm;

    @Override
    protected void setUp() throws Exception {
        pm = getContext().getPackageManager();
    }

    @Override
    protected void tearDown() throws Exception {
    }

    /**
     * Test that an enabled AppWidgetProvider is added to the installed
     * AppWidgetProvider's
     *
     * @throws Throwable
     */
    public void testWidgetEnabled() throws Throwable {
        setTestWidetEnableSetting(COMPONENT_ENABLED_STATE_DISABLED);
        SystemClock.sleep(WIDGET_CHANGE_TIME);
        setTestWidetEnableSetting(COMPONENT_ENABLED_STATE_ENABLED);
        SystemClock.sleep(WIDGET_CHANGE_TIME);
        AppWidgetManager awm = AppWidgetManager.getInstance(getContext());
        List<AppWidgetProviderInfo> installedProviders = awm.getInstalledProviders();
        assertEquals("There should be one TestWidget provider", 1,
                countTestWidget(installedProviders));
    }

    /**
     * Test that a disabled AppWidgetProvider is removed from the installed
     * AppWidgetProvider's
     *
     * @throws Throwable
     */
    public void testWidgetDisabled() throws Throwable {
        setTestWidetEnableSetting(COMPONENT_ENABLED_STATE_ENABLED);
        SystemClock.sleep(WIDGET_CHANGE_TIME);
        setTestWidetEnableSetting(COMPONENT_ENABLED_STATE_DISABLED);
        SystemClock.sleep(WIDGET_CHANGE_TIME);
        AppWidgetManager awm = AppWidgetManager.getInstance(getContext());
        List<AppWidgetProviderInfo> installedProviders = awm.getInstalledProviders();
        assertSame("Installed widget provider list should not include TestWidget", 0,
                countTestWidget(installedProviders));
    }

    /**
     * Test to ensure that we can't add the same AppWidgetProvider multiple
     * times to the list of providers
     *
     * @throws Throwable
     */
    public void testWidgetMultipleEnabled() throws Throwable {
        setTestWidetEnableSetting(COMPONENT_ENABLED_STATE_ENABLED);
        setTestWidetEnableSetting(COMPONENT_ENABLED_STATE_DEFAULT);
        setTestWidetEnableSetting(COMPONENT_ENABLED_STATE_ENABLED);
        SystemClock.sleep(WIDGET_CHANGE_TIME);
        AppWidgetManager awm = AppWidgetManager.getInstance(getContext());
        List<AppWidgetProviderInfo> installedProviders = awm.getInstalledProviders();
        assertEquals("There should be one TestWidget provider", 1,
                countTestWidget(installedProviders));
    }

    private void setTestWidetEnableSetting(int state) {
        pm.setComponentEnabledSetting(new ComponentName(getContext(), TestWidget.class), state,
                PackageManager.DONT_KILL_APP);

    }

    private int countTestWidget(List<AppWidgetProviderInfo> installedProviders) {
        int occurances = 0;
        for (AppWidgetProviderInfo appWidgetProviderInfo : installedProviders) {
            if (appWidgetProviderInfo.provider.getClassName().equals(TestWidget.class.getName())) {
                occurances++;
            }
        }
        return occurances;
    }
}
