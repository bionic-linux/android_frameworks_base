/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.os.storage;

import android.annotation.NonNull;
import android.annotation.FlaggedApi;
import android.security.Flags;

import dalvik.system.CloseGuard;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A handle to an open storage area.
 *
 * A {@code StorageArea} object is returned from {@link StorageManager#openStorageArea}, which
 * provides the following API:
 * - {@code getName}: get the name of the area
 * - {@code getDirectory}: get the path to the area
 * - {@code close}: close the area. Using an area through a try-with-resources will invoke
 *                  {@code close} automatically.
 *
 * An opened area represents a requirement for the storage area to remain accessible;
 * more detailed semantics are documented in {@link StorageArea#close}. To re-open a
 * closed area, {@link StorageManager#openStorageArea} needs to be invoked.
 *
 * Users can delete storage areas using {@link StorageManager#deleteStorageArea}.
 */
@FlaggedApi(Flags.FLAG_UNLOCKED_STORAGE_API)
public class StorageArea implements Closeable {

    private final StorageManager mManager;
    private final String mName;
    private final File mDirectory;
    private AtomicBoolean mClosed;

    private final CloseGuard mCloseGuard = CloseGuard.get();

    /** {@hide} */
    public StorageArea(StorageManager manager, String name, File directory) {
        mManager = Objects.requireNonNull(manager);
        mName = Objects.requireNonNull(name);
        mDirectory = Objects.requireNonNull(directory);
        mClosed = new AtomicBoolean(false); // starts open
        // tracker to warn if a user did not call "close"
        mCloseGuard.open("close");
    }

    /**
     * Returns the name of the storage area.
     *
     * @return the name of the storage area
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns a directory that the app can read and write while the storage area is open.
     * <p>
     * Accessing a closed storage area without first re-opening it, even in the case where an {@link
     * IOException} was thrown due to open files, has unspecified behavior. Since a closed storage
     * area is locked, the name of the area is encrypted and so a path to the non-encrypted name
     * of the area no longer points to the area.
     *
     * @return the directory path
     */
    @NonNull
    @FlaggedApi(Flags.FLAG_UNLOCKED_STORAGE_API)
    public File getDirectory() {
        return mDirectory;
    }

    /**
     * Closes the storage area. This complies with the {@link Closeable} interface, so that {@link
     * StorageArea} can be used with try-with-resources.
     * <p>
     * If other {@link StorageArea}s exist for the same storage area, then this method only
     * decrements an internal reference count. Otherwise, this method tries to lock the storage
     * area. In this case, there must no longer be any open files in the storage area; otherwise an
     * {@link IOException} will be thrown and the storage area is not guaranteed to be locked.
     * <p>
     * Accessing a closed storage area without first re-opening it, even in the case where an {@link
     * IOException} was thrown due to open files, has unspecified behavior. Since a closed storage
     * area is locked, the name of the area is encrypted and so a path to the non-encrypted name
     * of the area no longer points to the area.
     * <p>
     * If this method was already called, then calling it again has no effect.
     *
     * @throws IOException if the storage area failed to be properly closed
     */
    @Override
    @FlaggedApi(Flags.FLAG_UNLOCKED_STORAGE_API)
    public void close() throws IOException {
        if (mClosed.compareAndSet(false, true)) { // if not closed, close it
            mManager.closeStorageArea(this);
        }
    }

    /** {@hide} */
    @Override
    public void finalize() throws IOException {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        this.close();
    }
}
