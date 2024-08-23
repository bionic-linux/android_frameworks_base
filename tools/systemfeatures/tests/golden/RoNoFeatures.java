package com.android.systemfeatures;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageManager;
import java.lang.Boolean;
import java.lang.String;

/**
 * This file is auto-generated. DO NOT MODIFY.
 * Args: com.android.systemfeatures.RoNoFeatures --readonly=true --feature-apis=WATCH
 *
 * @hide
 */
public final class RoNoFeatures {
  public static boolean hasFeatureWatch(Context context) {
    return hasFeatureFallback(context, PackageManager.FEATURE_WATCH);
  }

  private static boolean hasFeatureFallback(Context context, String featureName) {
    return context.getPackageManager().hasSystemFeature(featureName, 0);
  }

  @Nullable
  public static Boolean maybeHasFeature(String featureName, int version) {
    return null;
  }
}
