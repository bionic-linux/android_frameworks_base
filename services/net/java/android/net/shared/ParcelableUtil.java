package android.net.shared;

import android.annotation.NonNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility methods to help convert to/from stable parcelables.
 * @hide
 */
public final class ParcelableUtil {
    // Below methods could be implemented easily with streams, but streams are frowned upon in
    // frameworks code.
    public static <ParcelableType, BaseType> ParcelableType[] toParcelableArray(
            @NonNull List<BaseType> base,
            @NonNull Function<BaseType, ParcelableType> conv,
            @NonNull Class<ParcelableType> parcelClass) {
        final ParcelableType[] out = (ParcelableType[]) Array.newInstance(parcelClass, base.size());
        int i = 0;
        for (BaseType b : base) {
            out[i] = conv.apply(b);
            i++;
        }
        return out;
    }

    public static <ParcelableType, BaseType> ArrayList<BaseType> fromParcelableArray(
            @NonNull ParcelableType[] parceled,
            @NonNull Function<ParcelableType, BaseType> conv) {
        final ArrayList<BaseType> out = new ArrayList<>(parceled.length);
        for (ParcelableType t : parceled) {
            out.add(conv.apply(t));
        }
        return out;
    }
}
