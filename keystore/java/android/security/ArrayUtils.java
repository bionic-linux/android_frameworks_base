package android.security;

/**
 * @hide
 */
abstract class ArrayUtils {
    static final String[] EMPTY_STRING_ARRAY = new String[0];
    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    static final int[] EMPTY_INT_ARRAY = new int[0];

    private ArrayUtils() {}

    public static String[] nullToEmpty(String[] array) {
        return (array != null) ? array : EMPTY_STRING_ARRAY;
    }

    public static String[] cloneIfNotEmpty(String[] array) {
        return ((array != null) && (array.length > 0)) ? array.clone() : null;
    }

    public static int[] cloneIfNotEmpty(int[] array) {
        return ((array != null) && (array.length > 0)) ? array.clone() : null;
    }

    public static boolean contains(int[] values, int value) {
        for (int element : values) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public static byte[] concat(byte[] arr1, byte[] arr2) {
        if ((arr1 == null) || (arr1.length == 0)) {
            return arr2;
        } else if ((arr2 == null) || (arr2.length == 0)) {
            return arr1;
        } else {
            byte[] result = new byte[arr1.length + arr2.length];
            System.arraycopy(arr1, 0, result, 0, arr1.length);
            System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
            return result;
        }
    }

    public static byte[] concat(byte[] arr1, int offset1, int len1, byte[] arr2, int offset2,
            int len2) {
        if (len1 == 0) {
            return subarray(arr2, offset2, len2);
        } else if (len2 == 0) {
            return subarray(arr1, offset1, len1);
        } else {
            byte[] result = new byte[len1 + len2];
            System.arraycopy(arr1, offset1, result, 0, len1);
            System.arraycopy(arr2, offset2, result, len1, len2);
            return result;
        }
    }

    public static byte[] subarray(byte[] arr, int offset, int len) {
        if (len == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        if ((offset == 0) && (len == arr.length)) {
            return arr;
        }
        byte[] result = new byte[len];
        System.arraycopy(arr, offset, result, 0, len);
        return result;
    }

    public static int[] concat(int[] arr1, int[] arr2) {
        if ((arr1 == null) || (arr1.length == 0)) {
            return arr2;
        } else if ((arr2 == null) || (arr2.length == 0)) {
            return arr1;
        } else {
            int[] result = new int[arr1.length + arr2.length];
            System.arraycopy(arr1, 0, result, 0, arr1.length);
            System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
            return result;
        }
    }
}
