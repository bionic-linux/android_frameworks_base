package com.android.systemfeatures;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageManager;
import java.lang.Boolean;
import java.lang.String;

/**
 * This file is auto-generated. DO NOT MODIFY.
 * Args: com.android.systemfeatures.RwFeatures --readonly=false --feature=WATCH:1 --feature=WIFI:0 --feature=VULKAN:-1 --feature=AUTO:
 *
 * @hide
 */
public final class RwFeatures {
  public static boolean hasFeatureWatch(Context context) {
    return hasFeatureFallback(context, PackageManager.FEATURE_WATCH);
  }

  public static boolean hasFeatureWifi(Context context) {
    return hasFeatureFallback(context, PackageManager.FEATURE_WIFI);
  }

  public static boolean hasFeatureVulkan(Context context) {
    return hasFeatureFallback(context, PackageManager.FEATURE_VULKAN);
  }

  public static boolean hasFeatureAuto(Context context) {
    return hasFeatureFallback(context, PackageManager.FEATURE_AUTO);
  }

  private static boolean hasFeatureFallback(Context context, String featureName) {
    return context.getPackageManager().hasSystemFeature(featureName, 0);
  }

  @Nullable
  public static Boolean maybeHasFeature(String featureName, int version) {
    return null;
  }
}
