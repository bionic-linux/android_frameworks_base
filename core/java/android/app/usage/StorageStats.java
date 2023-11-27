/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.app.usage;

import android.annotation.BytesLong;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

/**
 * Storage statistics for a UID, package, or {@link UserHandle} on a single
 * storage volume.
 *
 * @see StorageStatsManager
 */
public final class StorageStats implements Parcelable {
    /** {@hide} */ public long codeBytes;
    /** {@hide} */ public long dataBytes;
    /** {@hide} */ public long cacheBytes;
    /** {@hide} */ public long externalCacheBytes;
    /** {@hide} */ public long apkBytes;
    /** {@hide} */ public long curProfileBytes;
    /** {@hide} */ public long refProfileBytes;

    /** {@hide} */ @IntDef({
      FileType.APK, FileType.ART, FileType.ODEX,
      FileType.VDEX, FileType.DM, FileType.LIB,
      FileType.CUR_PROFILE, FileType.REF_PROFILE, FileType.OTHER})


    public @interface FileType {
      int APK = 0;
      int ART = 1;
      int ODEX = 2;
      int VDEX = 3;
      int DM = 4;
      int LIB = 5;
      int CUR_PROFILE = 6;
      int REF_PROFILE = 7;
      int RUNTIME_APP_IMAGE = 8;
      int OTHER = 9;
    }

    /**
     * Return the size of app. This includes {@code APK} files, optimized
     * compiler output, and unpacked native libraries.
     * <p>
     * If the primary external/shared storage is hosted on this storage device,
     * then this includes files stored under {@link Context#getObbDir()}.
     * <p>
     * Code is shared between all users on a multiuser device.
     */
    public @BytesLong long getAppBytes() {
        return codeBytes;
    }

    /**
     * Return the size of the specified data type. This includes files stored under
     * {@link Context#getPackageCodePath()}, /data/misc/ and
     * /data/user_de/.
     * <p>
     * Data is isolated for each user on a multiuser device.
     */
    @FlaggedApi(Flags.FLAG_GET_APP_BYTES_BY_DATA_TYPE)
    public long getAppBytesByDataType(int fileType) {
        switch (fileType) {
          case FileType.APK: return apkBytes;
          case FileType.CUR_PROFILE: return curProfileBytes;
          case FileType.REF_PROFILE: return refProfileBytes;
          /* ... */			     
	  default: return 0; /* ToDo: return others */
        }
    }

    /**
     * Return the size of all data. This includes files stored under
     * {@link Context#getDataDir()}, {@link Context#getCacheDir()},
     * {@link Context#getCodeCacheDir()}.
     * <p>
     * If the primary external/shared storage is hosted on this storage device,
     * then this includes files stored under
     * {@link Context#getExternalFilesDir(String)},
     * {@link Context#getExternalCacheDir()}, and
     * {@link Context#getExternalMediaDirs()}.
     * <p>
     * Data is isolated for each user on a multiuser device.
     */
    public @BytesLong long getDataBytes() {
        return dataBytes;
    }

    /**
     * Return the size of all cached data. This includes files stored under
     * {@link Context#getCacheDir()} and {@link Context#getCodeCacheDir()}.
     * <p>
     * If the primary external/shared storage is hosted on this storage device,
     * then this includes files stored under
     * {@link Context#getExternalCacheDir()}.
     * <p>
     * Cached data is isolated for each user on a multiuser device.
     */
    public @BytesLong long getCacheBytes() {
        return cacheBytes;
    }

    /**
     * Return the size of all cached data in the primary external/shared storage.
     * This includes files stored under
     * {@link Context#getExternalCacheDir()}.
     * <p>
     * Cached data is isolated for each user on a multiuser device.
     */
    public @BytesLong long getExternalCacheBytes() {
        return externalCacheBytes;
    }

    /** {@hide} */
    public StorageStats() {
    }

    /** {@hide} */
    public StorageStats(Parcel in) {
        this.codeBytes = in.readLong();
        this.dataBytes = in.readLong();
        this.cacheBytes = in.readLong();
        this.externalCacheBytes = in.readLong();
	this.apkBytes = in.readLong();
	this.curProfileBytes = in.readLong();
	this.refProfileBytes = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(codeBytes);
        dest.writeLong(dataBytes);
        dest.writeLong(cacheBytes);
        dest.writeLong(externalCacheBytes);
	dest.writeLong(apkBytes);
	dest.writeLong(curProfileBytes);
	dest.writeLong(refProfileBytes);
    }

    public static final @android.annotation.NonNull Creator<StorageStats> CREATOR = new Creator<StorageStats>() {
        @Override
        public StorageStats createFromParcel(Parcel in) {
            return new StorageStats(in);
        }

        @Override
        public StorageStats[] newArray(int size) {
            return new StorageStats[size];
        }
    };
}
