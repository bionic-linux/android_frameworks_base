package com.android.systemfeatures;

import android.annotation.Nullable;
import android.content.Context;
import java.lang.Boolean;
import java.lang.String;

/**
 * This file is auto-generated. DO NOT MODIFY.
 * Args: com.android.systemfeatures.RwNoFeatures --readonly=false
 *
 * @hide
 */
public final class RwNoFeatures {
  private static boolean hasFeatureFallback(Context context, String featureName) {
    return context.getPackageManager().hasSystemFeature(featureName, 0);
  }

  @Nullable
  public static Boolean maybeHasFeature(String featureName, int version) {
    return null;
  }
}
