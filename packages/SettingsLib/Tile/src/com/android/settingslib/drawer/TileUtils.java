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
package com.android.settingslib.drawer;

import static android.content.pm.PackageManager.CERT_INPUT_SHA256;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utils is a helper class that contains profile key, meta data, settings action
 * and static methods for get icon or text from uri.
 */
public class TileUtils {

    private static final boolean DEBUG_TIMING = false;

    private static final String LOG_TAG = "TileUtils";
    @VisibleForTesting
    static final String SETTING_PKG = "com.android.settings";

    /**
     * Settings will search for system activities of this action and add them as a top level
     * settings tile using the following parameters.
     *
     * <p>A category must be specified in the meta-data for the activity named
     * {@link #EXTRA_CATEGORY_KEY}
     *
     * <p>The title may be defined by meta-data named {@link #META_DATA_PREFERENCE_TITLE}
     * otherwise the label for the activity will be used.
     *
     * <p>The icon may be defined by meta-data named {@link #META_DATA_PREFERENCE_ICON}
     * otherwise the icon for the activity will be used.
     *
     * <p>A summary my be defined by meta-data named {@link #META_DATA_PREFERENCE_SUMMARY}
     */
    public static final String EXTRA_SETTINGS_ACTION = "com.android.settings.action.EXTRA_SETTINGS";

    /**
     * @See {@link #EXTRA_SETTINGS_ACTION}.
     */
    public static final String IA_SETTINGS_ACTION = "com.android.settings.action.IA_SETTINGS";

    /**
     * Same as #EXTRA_SETTINGS_ACTION but used for the platform Settings activities.
     */
    private static final String SETTINGS_ACTION = "com.android.settings.action.SETTINGS";

    private static final String OPERATOR_SETTINGS =
            "com.android.settings.OPERATOR_APPLICATION_SETTING";

    private static final String OPERATOR_DEFAULT_CATEGORY =
            "com.android.settings.category.wireless";

    private static final String MANUFACTURER_SETTINGS =
            "com.android.settings.MANUFACTURER_APPLICATION_SETTING";

    private static final String MANUFACTURER_DEFAULT_CATEGORY =
            "com.android.settings.category.device";

    /**
     * Same as #MANUFACTURER_SETTINGS.
     * IntentFilter action for customization of preferences.
     */
    private static final String MANUFACTURER_SETTINGS_CUSTOMIZATION =
            "com.android.settings.MANUFACTURER_APPLICATION_SETTING_CUSTOMIZATION";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml to specify the type
     * of customization to be performed.
     *
     * This works with {@link #MANUFACTURER_SETTINGS_CUSTOMIZATION} which should also be set in the
     * AndroidManifest.xml.
     * Valid values are either {@link #PREFERENCE_CATEGORY} (to inject a PreferenceCategory),
     * {@link #MOVE} (to move a preference) or {@link #REMOVE} (to remove a preference).
     */
    public static final String META_DATA_CUSTOMIZATION_TYPE =
            "com.android.settings.customization.type";

    /**
     * The type of the customization to be performed.
     *
     * Used as values for {@link #META_DATA_CUSTOMIZATION_TYPE}.
     */
    // Inject a PreferenceCategory
    public static final String PREFERENCE_CATEGORY = "preference-category";
    // Move a preference, see {@link #META_DATA_CUSTOMIZATION_MOVE_TARGET}
    public static final String MOVE = "move";
    // Remove a preference, see {@link #META_DATA_CUSTOMIZATION_REMOVE_TARGET}
    public static final String REMOVE = "remove";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml to specify that a
     * preference should be removed (hidden).
     * This should be the preference key of the preference to be removed, set using
     * {@code android:value}.
     *
     * To be used in combination with {@link #META_DATA_CUSTOMIZATION_TYPE} {@link #REMOVE}.
     */
    public static final String META_DATA_CUSTOMIZATION_REMOVE_TARGET =
            "com.android.settings.customization.type.remove.targetKey";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml to specify that a
     * preference should be moved.
     * This should be the preference key of the preference to be moved, set using
     * {@code android:value}.
     *
     * To be used in combination with {@link #META_DATA_CUSTOMIZATION_TYPE} {@link #MOVE} and one of
     * {@link #META_DATA_POSITION_BEFORE} or {@link #META_DATA_POSITION_AFTER} to designate the
     * destination of the move.
     */
    public static final String META_DATA_CUSTOMIZATION_MOVE_TARGET =
            "com.android.settings.customization.type.move.targetKey";

    /**
     * The key used to get the category from metadata of activities of action
     * {@link #EXTRA_SETTINGS_ACTION}
     * The value must be from {@link CategoryKey}.
     */
    static final String EXTRA_CATEGORY_KEY = "com.android.settings.category";

    /**
     * The key used to get the package name of the icon resource for the preference.
     */
    static final String EXTRA_PREFERENCE_ICON_PACKAGE = "com.android.settings.icon_package";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the key that should be used for the preference.
     */
    public static final String META_DATA_PREFERENCE_KEYHINT = "com.android.settings.keyhint";

    /**
     * Order of the item that should be displayed on screen. Bigger value items displays closer on
     * top.
     */
    public static final String META_DATA_KEY_ORDER = "com.android.settings.order";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the icon that should be displayed for the preference.
     */
    public static final String META_DATA_PREFERENCE_ICON = "com.android.settings.icon";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the icon background color. The value may or may not be used by Settings app.
     */
    public static final String META_DATA_PREFERENCE_ICON_BACKGROUND_HINT =
            "com.android.settings.bg.hint";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the icon background color as raw ARGB.
     */
    public static final String META_DATA_PREFERENCE_ICON_BACKGROUND_ARGB =
            "com.android.settings.bg.argb";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the content provider providing the icon that should be displayed for
     * the preference.
     *
     * Icon provided by the content provider overrides any static icon.
     */
    public static final String META_DATA_PREFERENCE_ICON_URI = "com.android.settings.icon_uri";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify whether the icon is tintable. This should be a boolean value {@code true} or
     * {@code false}, set using {@code android:value}
     */
    public static final String META_DATA_PREFERENCE_ICON_TINTABLE =
            "com.android.settings.icon_tintable";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the title that should be displayed for the preference.
     *
     * <p>Note: It is preferred to provide this value using {@code android:resource} with a string
     * resource for localization.
     */
    public static final String META_DATA_PREFERENCE_TITLE = "com.android.settings.title";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the content provider providing the title text that should be displayed for the
     * preference.
     *
     * Title provided by the content provider overrides any static title.
     */
    public static final String META_DATA_PREFERENCE_TITLE_URI =
            "com.android.settings.title_uri";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the summary text that should be displayed for the preference.
     */
    public static final String META_DATA_PREFERENCE_SUMMARY = "com.android.settings.summary";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the content provider providing the summary text that should be displayed for the
     * preference.
     *
     * Summary provided by the content provider overrides any static summary.
     */
    public static final String META_DATA_PREFERENCE_SUMMARY_URI =
            "com.android.settings.summary_uri";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the content provider providing the switch that should be displayed for the
     * preference.
     *
     * This works with {@link #META_DATA_PREFERENCE_KEYHINT} which should also be set in the
     * AndroidManifest.xml
     */
    public static final String META_DATA_PREFERENCE_SWITCH_URI =
            "com.android.settings.switch_uri";

    /**
     * Value for {@link #META_DATA_KEY_PROFILE}. When the device has a managed profile,
     * the app will always be run in the primary profile.
     *
     * @see #META_DATA_KEY_PROFILE
     */
    public static final String PROFILE_PRIMARY = "primary_profile_only";

    /**
     * Value for {@link #META_DATA_KEY_PROFILE}. When the device has a managed profile, the user
     * will be presented with a dialog to choose the profile the app will be run in.
     *
     * @see #META_DATA_KEY_PROFILE
     */
    public static final String PROFILE_ALL = "all_profiles";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the profile in which the app should be run when the device has a managed profile.
     * The default value is {@link #PROFILE_ALL} which means the user will be presented with a
     * dialog to choose the profile. If set to {@link #PROFILE_PRIMARY} the app will always be
     * run in the primary profile.
     *
     * @see #PROFILE_PRIMARY
     * @see #PROFILE_ALL
     */
    public static final String META_DATA_KEY_PROFILE = "com.android.settings.profile";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify whether the {@link android.app.Activity} should be launched in a separate task.
     * This should be a boolean value {@code true} or {@code false}, set using {@code android:value}
     */
    public static final String META_DATA_NEW_TASK = "com.android.settings.new_task";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify that the preference depends on a boolean resource.
     * This should point to a boolean config value (e.g "@bool/config_some_boolean"), set using
     * {@code android:value}.
     * The preference will be added if the resource equates to {@code true}, otherwise the
     * preference will be skipped.
     */
    public static final String META_DATA_DEPEND_ON_RESOURCE =
            "com.android.settings.dependOn.resource";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify that the preference depends on a system feature.
     * This should be a system feature (e.g "android.hardware.camera"), set using
     * {@code android:value}.
     * The preference will be added if the device has the specified system feature, otherwise the
     * preference will be skipped.
     */
    public static final String META_DATA_DEPEND_ON_SYSTEM_FEATURE =
            "com.android.settings.dependOn.systemFeature";


    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify that the preference depends on a system property.
     * This should be a boolean system property (e.g "rw.device.property.supported"), set using
     * {@code android:value}.
     * The preference will be added if the device has the specified system property set to
     * {@code true}, otherwise the preference will be skipped.
     */
    public static final String META_DATA_DEPEND_ON_SYSTEM_PROPERTY =
            "com.android.settings.dependOn.systemProperty";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify that the preference should be positioned after a specified existing preference.
     * This should be the preference key of the preference after which the tile shall be positioned,
     * set using {@code android:value}.
     *
     * Valid values are an existing preference key or {@link #META_DATA_FIRST_POSITION} to position
     * after the first existing preference or {@link #META_DATA_LAST_POSITION} to position after the
     * last existing preference.
     */
    public static final String META_DATA_POSITION_AFTER = "com.android.settings.position.after";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify that the preference should be positioned before a specified existing preference.
     * This should be the preference key of the preference before which the tile shall be
     * positioned, set using {@code android:value}.
     *
     * Valid values are an existing preference key or {@link #META_DATA_FIRST_POSITION} to position
     * before the first existing preference or {@link #META_DATA_LAST_POSITION} to position before
     * the last existing preference.
     */
    public static final String META_DATA_POSITION_BEFORE = "com.android.settings.position.before";

    /**
     * Used as value for {@link #META_DATA_POSITION_BEFORE} and {@link #META_DATA_POSITION_AFTER} to
     * position the Preference at the top of the group
     */
    public static final String META_DATA_FIRST_POSITION = "first";

    /**
     * Used as value for {@link #META_DATA_POSITION_BEFORE} and {@link #META_DATA_POSITION_AFTER} to
     * position the Preference at the bottom of the group
     */
    public static final String META_DATA_LAST_POSITION = "last";

    /**
     * Build a list of DashboardCategory.
     */
    public static List<DashboardCategory> getCategories(Context context,
            Map<Pair<String, String>, Tile> cache) {
        final long startTime = System.currentTimeMillis();
        final boolean setup =
                Global.getInt(context.getContentResolver(), Global.DEVICE_PROVISIONED, 0) != 0;
        final ArrayList<Tile> tiles = new ArrayList<>();
        final UserManager userManager = (UserManager) context.getSystemService(
                Context.USER_SERVICE);
        for (UserHandle user : userManager.getUserProfiles()) {
            // TODO: Needs much optimization, too many PM queries going on here.
            if (user.getIdentifier() == ActivityManager.getCurrentUser()) {
                // Only add Settings for this user.
                loadTilesForAction(context, user, SETTINGS_ACTION, cache, null, tiles, true);
                loadTilesForAction(context, user, OPERATOR_SETTINGS, cache,
                        OPERATOR_DEFAULT_CATEGORY, tiles, false);
                loadTilesForAction(context, user, MANUFACTURER_SETTINGS, cache,
                        MANUFACTURER_DEFAULT_CATEGORY, tiles, false);
                loadTilesForCustomization(context, user, MANUFACTURER_SETTINGS_CUSTOMIZATION, cache,
                        MANUFACTURER_DEFAULT_CATEGORY, tiles, false);            }
            if (setup) {
                loadTilesForAction(context, user, EXTRA_SETTINGS_ACTION, cache, null, tiles, false);
                loadTilesForAction(context, user, IA_SETTINGS_ACTION, cache, null, tiles, false);
            }
        }

        final HashMap<String, DashboardCategory> categoryMap = new HashMap<>();
        for (Tile tile : tiles) {
            final String categoryKey = tile.getCategory();
            DashboardCategory category = categoryMap.get(categoryKey);
            if (category == null) {
                category = new DashboardCategory(categoryKey);

                if (category == null) {
                    Log.w(LOG_TAG, "Couldn't find category " + categoryKey);
                    continue;
                }
                categoryMap.put(categoryKey, category);
            }
            category.addTile(tile);
        }
        final ArrayList<DashboardCategory> categories = new ArrayList<>(categoryMap.values());
        for (DashboardCategory category : categories) {
            category.sortTiles();
        }

        if (DEBUG_TIMING) {
            Log.d(LOG_TAG, "getCategories took "
                    + (System.currentTimeMillis() - startTime) + " ms");
        }
        return categories;
    }

    @VisibleForTesting
    static void loadTilesForAction(Context context,
            UserHandle user, String action, Map<Pair<String, String>, Tile> addedCache,
            String defaultCategory, List<Tile> outTiles, boolean requireSettings) {
        final Intent intent = new Intent(action);
        if (requireSettings) {
            intent.setPackage(SETTING_PKG);
        }
        loadActivityTiles(context, user, addedCache, defaultCategory, outTiles, intent);
        loadProviderTiles(context, user, addedCache, defaultCategory, outTiles, intent);
    }

    static void loadTilesForCustomization(Context context, UserHandle user, String action,
            Map<Pair<String, String>, Tile> addedCache, String defaultCategory, List<Tile> outTiles,
            boolean requireSettings) {

        final Intent intent = new Intent(action);
        if (requireSettings) {
            intent.setPackage(SETTING_PKG);
        }

        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> results = pm.queryIntentActivitiesAsUser(intent, 0,
                user.getIdentifier());
        for (ResolveInfo resolved : results) {
            if (!resolved.system) {
                // Do not allow every app to customize settings, only system ones.
                continue;
            }
            final ComponentName componentName= new ComponentName(resolved.activityInfo.packageName,
                    resolved.activityInfo.name);
            ActivityInfo activityInfo = null;
            try {
                activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_META_DATA);
            } catch (NameNotFoundException e) {
                Log.e(LOG_TAG, "Failed to query Intent Activities for customization", e);
                continue;
            }
            if (activityInfo != null) {
                final Bundle metaData = activityInfo.metaData;
                loadCustomization(context, user, addedCache, defaultCategory, outTiles, intent,
                        metaData, activityInfo);
            }
        }
    }

    private static void loadActivityTiles(Context context,
            UserHandle user, Map<Pair<String, String>, Tile> addedCache,
            String defaultCategory, List<Tile> outTiles, Intent intent) {
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> results = pm.queryIntentActivitiesAsUser(intent, 0,
                user.getIdentifier());
        for (ResolveInfo resolved : results) {
            if (!resolved.system) {
                // Do not allow every app to add to settings, only system ones.
                continue;
            }
            final ComponentName componentName= new ComponentName(resolved.activityInfo.packageName,
                    resolved.activityInfo.name);
            ActivityInfo activityInfo = null;
            try {
                activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_META_DATA);
            } catch (NameNotFoundException e) {
                Log.e(LOG_TAG, "Failed to query Intent Activities for action", e);
                continue;
            }
            if (activityInfo != null) {
                final Bundle metaData = activityInfo.metaData;
                loadTile(context, user, addedCache, defaultCategory, outTiles, intent, metaData,
                        activityInfo);
            }
        }
    }

    private static void loadProviderTiles(Context context,
            UserHandle user, Map<Pair<String, String>, Tile> addedCache,
            String defaultCategory, List<Tile> outTiles, Intent intent) {
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> results = pm.queryIntentContentProvidersAsUser(intent,
                0 /* flags */, user.getIdentifier());
        for (ResolveInfo resolved : results) {
            if (!resolved.system) {
                // Do not allow every app to add to settings, only system ones.
                continue;
            }
            final ProviderInfo providerInfo = resolved.providerInfo;
            final List<Bundle> switchData = getSwitchDataFromProvider(context,
                    providerInfo.authority);
            if (switchData == null || switchData.isEmpty()) {
                continue;
            }
            for (Bundle metaData : switchData) {
                loadTile(context, user, addedCache, defaultCategory, outTiles, intent, metaData,
                        providerInfo);
            }
        }
    }

    private static void loadTile(Context context, UserHandle user,
            Map<Pair<String, String>, Tile> addedCache, String defaultCategory, List<Tile> outTiles,
            Intent intent, Bundle metaData, ComponentInfo componentInfo) {
        // Skip loading tile if the component is tagged primary_profile_only but not running on
        // the current user.
        if (user.getIdentifier() != ActivityManager.getCurrentUser()
                && Tile.isPrimaryProfileOnly(componentInfo.metaData)) {
            Log.w(LOG_TAG, "Found " + componentInfo.name + " for intent "
                    + intent + " is primary profile only, skip loading tile for uid "
                    + user.getIdentifier());
            return;
        }

        String categoryKey = defaultCategory;
        // Load category
        if ((metaData == null || !metaData.containsKey(EXTRA_CATEGORY_KEY))
                && categoryKey == null) {
            Log.w(LOG_TAG, "Found " + componentInfo.name + " for intent "
                    + intent + " missing metadata "
                    + (metaData == null ? "" : EXTRA_CATEGORY_KEY));
            return;
        } else {
            categoryKey = metaData.getString(EXTRA_CATEGORY_KEY);
        }

        final boolean isProvider = componentInfo instanceof ProviderInfo;
        final Pair<String, String> key = isProvider
                ? new Pair<>(((ProviderInfo) componentInfo).authority,
                        metaData.getString(META_DATA_PREFERENCE_KEYHINT))
                : new Pair<>(componentInfo.packageName, componentInfo.name);
        Tile tile = addedCache.get(key);
        if (tile == null) {
            if (checkDependencies(context, metaData, componentInfo)) {
                tile = isProvider
                        ? new ProviderTile((ProviderInfo) componentInfo, categoryKey, metaData)
                        : new ActivityTile((ActivityInfo) componentInfo, categoryKey);
                addedCache.put(key, tile);
            } else {
                // Dependency check failed
                Log.w(LOG_TAG, "Dependency check failed for " + componentInfo.packageName);
                return;
            }
        } else {
            tile.setMetaData(metaData);
        }

        if (!tile.userHandle.contains(user)) {
            tile.userHandle.add(user);
        }
        if (!outTiles.contains(tile)) {
            outTiles.add(tile);
        }
    }

    private static void loadCustomization(Context context, UserHandle user,
            Map<Pair<String, String>, Tile> addedCache, String defaultCategory, List<Tile> outTiles,
            Intent intent, Bundle metaData, ComponentInfo componentInfo) {
        // Skip loading customization if the component is tagged primary_profile_only but not
        // running on the current user.
        if (user.getIdentifier() != ActivityManager.getCurrentUser()
                && Tile.isPrimaryProfileOnly(componentInfo.metaData)) {
            Log.w(LOG_TAG, "Found " + componentInfo.name + " for intent "
                    + intent + " is primary profile only, skip loading tile for uid "
                    + user.getIdentifier());
            return;
        }

        String categoryKey = defaultCategory;
        if ((metaData == null || !metaData.containsKey(EXTRA_CATEGORY_KEY))
                && categoryKey == null) {
            Log.w(LOG_TAG, "Found " + componentInfo.name + " for intent "
                    + intent + " missing metadata "
                    + (metaData == null ? "" : EXTRA_CATEGORY_KEY));
            return;
        } else {
            categoryKey = metaData.getString(EXTRA_CATEGORY_KEY);
        }

        final Pair<String, String> key = new Pair<>(componentInfo.packageName, componentInfo.name);
        Tile tile = addedCache.get(key);
        if (tile == null) {
            if (checkDependencies(context, metaData, componentInfo)) {
                if (checkEntitlement(context, componentInfo, CERT_INPUT_SHA256)) {
                    tile = new CustomizationTile((ActivityInfo) componentInfo, categoryKey);
                    addedCache.put(key, tile);
                } else {
                    // Entitlement check failed
                    Log.w(LOG_TAG, "Entitlement check failed for " + componentInfo.packageName);
                    return;
                }
            } else {
                // Dependency check failed
                Log.w(LOG_TAG, "Dependency check failed for " + componentInfo.packageName);
                return;
            }
        } else {
            tile.setMetaData(metaData);
        }

        if (!tile.userHandle.contains(user)) {
            tile.userHandle.add(user);
        }
        if (!outTiles.contains(tile)) {
            outTiles.add(tile);
        }
    }

    private static boolean checkDependencies(Context context, Bundle metaData,
            ComponentInfo componentInfo) {
        if (metaData == null) {
            return false;
        } else {
            boolean add = true;

            // Check if the Tile depends on a boolean resource
            if (metaData.containsKey(META_DATA_DEPEND_ON_RESOURCE)) {
                add = metaData.getBoolean(META_DATA_DEPEND_ON_RESOURCE, false);
            }

            // Check if the Tile depends on a system feature
            if (add && metaData.containsKey(META_DATA_DEPEND_ON_SYSTEM_FEATURE)) {
                String sysFeature = metaData.getString(META_DATA_DEPEND_ON_SYSTEM_FEATURE);
                if (!TextUtils.isEmpty(sysFeature)) {
                    add = context.getPackageManager().hasSystemFeature(sysFeature);
                }
            }

            // Check if the Tile depends on a system property
            if (add && metaData.containsKey(META_DATA_DEPEND_ON_SYSTEM_PROPERTY)) {
                String sysProperty = metaData.getString(META_DATA_DEPEND_ON_SYSTEM_PROPERTY);
                if (!TextUtils.isEmpty(sysProperty)) {
                    add = SystemProperties.getBoolean(sysProperty, false);
                }
            }

            return add;
        }
    }

    // Optional entitlement check when adding/moving/removing preferences, ensuring that only
    // packages with the same signature as the Settings package can modify settings Tiles.
    private static boolean checkEntitlement(Context context, ComponentInfo componentInfo,
            int type) {
        if (componentInfo != null && !TextUtils.isEmpty(componentInfo.packageName)) {
            try {
                final PackageManager pm = context.getPackageManager();
                final PackageInfo packageInfo = pm.getPackageInfo(SETTING_PKG,
                        PackageManager.GET_SIGNING_CERTIFICATES);
                final SigningInfo signingInfo = packageInfo.signingInfo;
                final Signature[] signatures = signingInfo.getApkContentsSigners();
                final ArrayList<byte[]> hashes = getHashSignatureArray(signatures);
                for (byte[] hash : hashes) {
                     if (pm.hasSigningCertificate(componentInfo.packageName, hash, type)) {
                        return true;
                     }
                }
                Log.w(LOG_TAG, "Entitlement check failed for '" + componentInfo.packageName +
                        "', for type " + type);
            } catch (NameNotFoundException e) {
                Log.e(LOG_TAG, "Failed to get package info for " + SETTING_PKG);
                return false;
            }
            Log.w(LOG_TAG, "Entitlement check failed for " + componentInfo.name);
        }
        return false;
    }

    private static ArrayList<byte[]> getHashSignatureArray(Signature[] signatures) {
        if (signatures == null) {
            return null;
        }

        ArrayList<byte[]> hashes = new ArrayList<>(signatures.length);
        for (Signature signature : signatures) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(signature.toByteArray());
                hashes.add(digest.digest());
            } catch (NoSuchAlgorithmException e) {
                Log.w(LOG_TAG, "No SHA-256 algorithm found!");
            }
        }
        return hashes;
    }

    /** Returns the switch data of the key specified from the provider */
    // TODO(b/144732809): rearrange methods by access level modifiers
    static Bundle getSwitchDataFromProvider(Context context, String authority, String key) {
        final Map<String, IContentProvider> providerMap = new ArrayMap<>();
        final Uri uri = buildUri(authority, SwitchesProvider.METHOD_GET_SWITCH_DATA, key);
        return getBundleFromUri(context, uri, providerMap, null /* bundle */);
    }

    /** Returns all switch data from the provider */
    private static List<Bundle> getSwitchDataFromProvider(Context context, String authority) {
        final Map<String, IContentProvider> providerMap = new ArrayMap<>();
        final Uri uri = buildUri(authority, SwitchesProvider.METHOD_GET_SWITCH_DATA);
        final Bundle result = getBundleFromUri(context, uri, providerMap, null /* bundle */);
        return result != null
                ? result.getParcelableArrayList(SwitchesProvider.EXTRA_SWITCH_DATA)
                : null;
    }

    /**
     * Returns the complete uri from the meta data key of the tile.
     *
     * A complete uri should contain at least one path segment and be one of the following types:
     *      content://authority/method
     *      content://authority/method/key
     *
     * If the uri from the tile is not complete, build a uri by the default method and the
     * preference key.
     *
     * @param tile          Tile which contains meta data
     * @param metaDataKey   Key mapping to the uri in meta data
     * @param defaultMethod Method to be attached to the uri by default if it has no path segment
     * @return Uri associated with the key
     */
    public static Uri getCompleteUri(Tile tile, String metaDataKey, String defaultMethod) {
        final String uriString = tile.getMetaData().getString(metaDataKey);
        if (TextUtils.isEmpty(uriString)) {
            return null;
        }

        final Uri uri = Uri.parse(uriString);
        final List<String> pathSegments = uri.getPathSegments();
        if (pathSegments != null && !pathSegments.isEmpty()) {
            return uri;
        }

        final String key = tile.getMetaData().getString(META_DATA_PREFERENCE_KEYHINT);
        if (TextUtils.isEmpty(key)) {
            Log.w(LOG_TAG, "Please specify the meta-data " + META_DATA_PREFERENCE_KEYHINT
                    + " in AndroidManifest.xml for " + uriString);
            return buildUri(uri.getAuthority(), defaultMethod);
        }
        return buildUri(uri.getAuthority(), defaultMethod, key);
    }

    static Uri buildUri(String authority, String method, String key) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath(method)
                .appendPath(key)
                .build();
    }

    private static Uri buildUri(String authority, String method) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath(method)
                .build();
    }

    /**
     * Gets the icon package name and resource id from content provider.
     *
     * @param context     context
     * @param packageName package name of the target activity
     * @param uri         URI for the content provider
     * @param providerMap Maps URI authorities to providers
     * @return package name and resource id of the icon specified
     */
    public static Pair<String, Integer> getIconFromUri(Context context, String packageName,
            Uri uri, Map<String, IContentProvider> providerMap) {
        final Bundle bundle = getBundleFromUri(context, uri, providerMap, null /* bundle */);
        if (bundle == null) {
            return null;
        }
        final String iconPackageName = bundle.getString(EXTRA_PREFERENCE_ICON_PACKAGE);
        if (TextUtils.isEmpty(iconPackageName)) {
            return null;
        }
        int resId = bundle.getInt(META_DATA_PREFERENCE_ICON, 0);
        if (resId == 0) {
            return null;
        }
        // Icon can either come from the target package or from the Settings app.
        if (iconPackageName.equals(packageName)
                || iconPackageName.equals(context.getPackageName())) {
            return Pair.create(iconPackageName, resId);
        }
        return null;
    }

    /**
     * Gets text associated with the input key from the content provider.
     *
     * @param context     context
     * @param uri         URI for the content provider
     * @param providerMap Maps URI authorities to providers
     * @param key         Key mapping to the text in bundle returned by the content provider
     * @return Text associated with the key, if returned by the content provider
     */
    public static String getTextFromUri(Context context, Uri uri,
            Map<String, IContentProvider> providerMap, String key) {
        final Bundle bundle = getBundleFromUri(context, uri, providerMap, null /* bundle */);
        return (bundle != null) ? bundle.getString(key) : null;
    }

    /**
     * Gets boolean associated with the input key from the content provider.
     *
     * @param context     context
     * @param uri         URI for the content provider
     * @param providerMap Maps URI authorities to providers
     * @param key         Key mapping to the text in bundle returned by the content provider
     * @return Boolean associated with the key, if returned by the content provider
     */
    public static boolean getBooleanFromUri(Context context, Uri uri,
            Map<String, IContentProvider> providerMap, String key) {
        final Bundle bundle = getBundleFromUri(context, uri, providerMap, null /* bundle */);
        return (bundle != null) ? bundle.getBoolean(key) : false;
    }

    /**
     * Puts boolean associated with the input key to the content provider.
     *
     * @param context     context
     * @param uri         URI for the content provider
     * @param providerMap Maps URI authorities to providers
     * @param key         Key mapping to the text in bundle returned by the content provider
     * @param value       Boolean associated with the key
     * @return Bundle associated with the action, if returned by the content provider
     */
    public static Bundle putBooleanToUriAndGetResult(Context context, Uri uri,
            Map<String, IContentProvider> providerMap, String key, boolean value) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(key, value);
        return getBundleFromUri(context, uri, providerMap, bundle);
    }

    private static Bundle getBundleFromUri(Context context, Uri uri,
            Map<String, IContentProvider> providerMap, Bundle bundle) {
        final Pair<String, String> args = getMethodAndKey(uri);
        if (args == null) {
            return null;
        }
        final String method = args.first;
        final String key = args.second;
        if (TextUtils.isEmpty(method)) {
            return null;
        }
        final IContentProvider provider = getProviderFromUri(context, uri, providerMap);
        if (provider == null) {
            return null;
        }
        if (!TextUtils.isEmpty(key)) {
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putString(META_DATA_PREFERENCE_KEYHINT, key);
        }
        try {
            return provider.call(context.getAttributionSource(),
                    uri.getAuthority(), method, uri.toString(), bundle);
        } catch (RemoteException e) {
            return null;
        }
    }

    private static IContentProvider getProviderFromUri(Context context, Uri uri,
            Map<String, IContentProvider> providerMap) {
        if (uri == null) {
            return null;
        }
        final String authority = uri.getAuthority();
        if (TextUtils.isEmpty(authority)) {
            return null;
        }
        if (!providerMap.containsKey(authority)) {
            providerMap.put(authority, context.getContentResolver().acquireUnstableProvider(uri));
        }
        return providerMap.get(authority);
    }

    /** Returns method and key of the complete uri. */
    private static Pair<String, String> getMethodAndKey(Uri uri) {
        if (uri == null) {
            return null;
        }
        final List<String> pathSegments = uri.getPathSegments();
        if (pathSegments == null || pathSegments.isEmpty()) {
            return null;
        }
        final String method = pathSegments.get(0);
        final String key = pathSegments.size() > 1 ? pathSegments.get(1) : null;
        return Pair.create(method, key);
    }
}
