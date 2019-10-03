package android.dummy;

import android.compat.annotation.ChangeId;
import android.compat.annotation.Disabled;
import android.compat.annotation.EnabledAfter;
/**
* @hide
*/
public class Gating {
	@ChangeId
    public static final long CHANGE_NORMAL = 42L;

    @ChangeId
    @Disabled
    public static final long CHANGE_DISABLED = 666L;

    @ChangeId
    @EnabledAfter(targetSdkVersion=29)
    public static final long CHANGE_AFTER_SDK = 666013L;
}