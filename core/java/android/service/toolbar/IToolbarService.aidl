package android.service.toolbar;

import android.content.Intent;
import android.os.RemoteCallback;
import com.android.internal.infra.AndroidFuture;

/**
 * @hide
 */
interface IToolbarService {
    void getToolbarSlice(in Intent intent, int w, int h, in AndroidFuture future);
}
