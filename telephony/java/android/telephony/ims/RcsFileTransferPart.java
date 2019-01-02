/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.telephony.ims;

import android.annotation.IntDef;
import android.annotation.WorkerThread;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.aidl.IRcs;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A part of a composite {@link RcsMessage} that holds a file transfer.
 *
 * @hide - TODO(109759350) make this public
 */
public class RcsFileTransferPart implements Parcelable {
    public static final @RcsFileTransferStatus int DRAFT = 1;
    public static final @RcsFileTransferStatus int SENDING = 2;
    public static final @RcsFileTransferStatus int SENDING_PAUSED = 3;
    public static final @RcsFileTransferStatus int SENDING_FAILED = 4;
    public static final @RcsFileTransferStatus int SENDING_CANCELLED = 5;
    public static final @RcsFileTransferStatus int DOWNLOADING = 6;
    public static final @RcsFileTransferStatus int DOWNLOADING_PAUSED = 7;
    public static final @RcsFileTransferStatus int DOWNLOADING_FAILED = 8;
    public static final @RcsFileTransferStatus int DOWNLOADING_CANCELLED = 9;
    public static final @RcsFileTransferStatus int SUCCEEDED = 10;

    @IntDef({
            DRAFT, SENDING, SENDING_PAUSED, SENDING_FAILED, SENDING_CANCELLED, DOWNLOADING,
            DOWNLOADING_PAUSED, DOWNLOADING_FAILED, DOWNLOADING_CANCELLED, SUCCEEDED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RcsFileTransferStatus {
    }

    private int mId;
    private String mRcsFileTransferSessionId;
    private Uri mContentUri;
    private String mContentType;
    private long mFileSize;
    private long mTransferOffset;
    private int mWidth;
    private int mHeight;
    private long mLength;
    private Uri mPreviewUri;
    private String mPreviewType;
    private @RcsFileTransferStatus int mFileTransferStatus;

    RcsFileTransferPart(String rcsFileTransferSessionId, Uri contentUri, String contentType,
            long fileSize, long offset, int width, int height,
            long length, Uri previewUri, String previewType,
            @RcsFileTransferStatus int fileTransferStatus) {
        mRcsFileTransferSessionId = rcsFileTransferSessionId;
        mContentUri = contentUri;
        mContentType = contentType;
        mFileSize = fileSize;
        mTransferOffset = offset;
        mWidth = width;
        mHeight = height;
        mLength = length;
        mPreviewUri = previewUri;
        mPreviewType = previewType;
        mFileTransferStatus = fileTransferStatus;
    }

    /**
     * @return Returns a builder for {@link RcsIncomingMessage}.
     */
    public static RcsIncomingMessage.Builder builder() {
        return new RcsIncomingMessage.Builder();
    }

    /**
     * @hide
     */
    public void setId(int id) {
        mId = id;
    }

    /**
     * @hide
     */
    public int getId() {
        return mId;
    }

    /**
     * Sets the RCS file transfer session ID for this file transfer and persists into storage.
     * @param sessionId The session ID to be used for this file transfer.
     */
    @WorkerThread
    public void setFileTransferSessionId(String sessionId) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferSessionId(mId, sessionId);
                mRcsFileTransferSessionId = sessionId;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsFileTransferPart: Exception happened during setFileTransferSessionId", re);
        }
    }

    /**
     * @return Returns the file transfer session ID.
     */
    public String getFileTransferSessionId() {
        return mRcsFileTransferSessionId;
    }

    /**
     * Sets the content URI for this file transfer and persists into storage. The file transfer
     * should be reachable using this URI.
     * @param contentUri The URI for this file transfer.
     */
    @WorkerThread
    public void setContentUri(Uri contentUri) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferContentUri(mId, contentUri.toString());
                mContentUri = contentUri;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsFileTransferPart: Exception happened during setContentUri", re);
        }
    }

    /**
     * @return Returns the URI for this file transfer
     */
    public Uri getContentUri() {
        return mContentUri;
    }

    /**
     * Sets the MIME type of this file transfer and persists into storage. Whether this type
     * actually matches any known or supported types is not checked.
     * @param contentType The type of this file transfer.
     */
    @WorkerThread
    public void setContentType(String contentType) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferContentType(mId, contentType);
                mContentType = contentType;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsFileTransferPart: Exception happened during setContentType", re);
        }
    }

    /**
     * @return Returns the content type of this file transfer
     */
    public String getContentType() {
        return mContentType;
    }

    /**
     * Sets the content length (i.e. file size) for this file transfer and persists into storage.
     * @param contentLength The content length of this file transfer
     */
    @WorkerThread
    public void setFileSize(long contentLength) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferFileSize(mId, contentLength);
                mFileSize = contentLength;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsFileTransferPart: Exception happened during setContentLength", re);
        }
    }

    /**
     * @return Returns the content length (i.e. file size) for this file transfer.
     */
    public long getFileSize() {
        return mFileSize;
    }

    /**
     * Sets the transfer offset for this file transfer and persists into storage. The file transfer
     * offset is defined as how many bytes have been successfully transferred to the receiver of
     * this file transfer.
     * @param transferOffset The transfer offset for this file transfer.
     */
    @WorkerThread
    public void setTransferOffset(long transferOffset) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferTransferOffset(mId, transferOffset);
                mTransferOffset = transferOffset;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsFileTransferPart: Exception happened during setTransferOffsett", re);
        }
    }

    /**
     * @return Returns the number of bytes that have successfully transferred.
     */
    public long getTransferOffset() {
        return mTransferOffset;
    }

    /**
     * Sets the status for this file transfer and persists into storage.
     * @param status The status of this file transfer.
     */
    @WorkerThread
    public void setFileTransferStatus(@RcsFileTransferStatus int status) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferStatus(mId, status);
                mFileTransferStatus = status;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsFileTransferPart: Exception happened during setTransferOffsett", re);
        }
    }

    /**
     * @return Returns the width of this multi-media message part in pixels.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Sets the width of this RCS multi-media message part and persists into storage.
     * @param width The width value in pixels
     */
    @WorkerThread
    public void setWidth(int width) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferWidth(mId, width);
                mWidth = width;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMultiMediaPart: Exception happened during setWidth", re);
        }
    }

    /**
     * @return Returns the height of this multi-media message part in pixels.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Sets the height of this RCS multi-media message part and persists into storage.
     * @param height The height value in pixels
     */
    @WorkerThread
    public void setHeight(int height) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferHeight(mId, height);
                mHeight = height;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMultiMediaPart: Exception happened during setHeight", re);
        }
    }

    /**
     * @return Returns the length of this multi-media file (e.g. video or audio) in milliseconds.
     */
    public long getLength() {
        return mLength;
    }

    /**
     * Sets the length of this multi-media file (e.g. video or audio) and persists into storage.
     * @param length The length of the file in milliseconds.
     */
    @WorkerThread
    public void setLength(long length) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferLength(mId, length);
                mLength = length;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMultiMediaPart: Exception happened during setLength", re);
        }
    }

    /**
     * @return Returns the URI for the preview of this multi-media file (e.g. an image thumbnail for
     * a video)
     */
    public Uri getPreviewUri() {
        return mPreviewUri;
    }

    /**
     * Sets the URI for the preview of this multi-media file and persists into storage.
     * @param previewUri The URI to access to the preview file.
     */
    @WorkerThread
    public void setPreviewUri(Uri previewUri) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferPreviewUri(mId, previewUri.toString());
                mPreviewUri = previewUri;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMultiMediaPart: Exception happened during setPreviewUri", re);
        }
    }

    /**
     * @return Returns the MIME type of this multi-media file's preview.
     */
    public String getPreviewType() {
        return mPreviewType;
    }

    /**
     * Sets the MIME type for this multi-media file's preview and persists into storage.
     * @param previewType The MIME type for the preview
     */
    @WorkerThread
    public void setPreviewType(String previewType) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setFileTransferPreviewType(mId, previewType);
                mPreviewType = previewType;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMultiMediaPart: Exception happened during setPreviewType", re);
        }
    }

    /**
     * Use this builder for getting an instance of {@link RcsFileTransferPart}. The instance will
     * not be persisted into storage until it is added to an {@link RcsMessage}.
     */
    public class Builder {
        private String mRcsFileTransferSessionId;
        private Uri mContentUri;
        private String mContentType;
        private long mFileSize;
        private long mTransferOffset;
        private int mWidth;
        private int mHeight;
        private long mLength;
        private Uri mPreviewUri;
        private String mPreviewType;
        private @RcsFileTransferStatus int mFileTransferStatus;

        /**
         * Sets the RCS file transfer session ID for the file transfer to be built.
         * @param sessionId The session ID to be used for this file transfer.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setFileTransferSessionId(String sessionId) {
            mRcsFileTransferSessionId = sessionId;
            return this;
        }

        /**
         * Sets the content URI for the file transfer to be built. The file transfer should be
         * reachable using this URI.
         * @param contentUri The URI for this file transfer.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setContentUri(Uri contentUri) {
            mContentUri = contentUri;
            return this;
        }

        /**
         * Sets the MIME type of the file transfer to be built. Whether this type actually matches
         * any known or supported types is not checked.
         * @param contentType The type of this file transfer.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setContentType(String contentType) {
            mContentType = contentType;
            return this;
        }

        /**
         * Sets the content length (i.e. file size) for the file transfer to be built.
         * @param size The content length of this file transfer
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setFileSize(long size) {
            mFileSize = size;
            return this;
        }

        /**
         * Sets the transfer offset for the file transfer to be built. The file transfer offset is
         * defined as how many bytes have been successfully transferred to the receiver of
         * this file transfer.
         * @param offset The transfer offset for this file transfer.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setTransferOffset(long offset) {
            mTransferOffset = offset;
            return this;
        }

        /**
         * Sets the width of the multi-media file transfer to be built.
         * @param width The width value in pixels
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setWidth(int width) {
            mWidth = width;
            return this;
        }

        /**
         * Sets the height of the multi-media file transfer to be built.
         * @param height The height value in pixels
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setHeight(int height) {
            mHeight = height;
            return this;
        }

        /**
         * Sets the length of the multi-media file transfer to be built (e.g. video or audio length)
         * @param length The length of the file in milliseconds.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setLength(long length) {
            mLength = length;
            return this;
        }

        /**
         * Sets the URI for the preview of this multi-media file.
         * @param previewUri The URI to access to the preview file.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setPreviewUri(Uri previewUri) {
            mPreviewUri = previewUri;
            return this;
        }

        /**
         * Sets the MIME type for this multi-media file's preview.
         * @param previewType The MIME type for the preview
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setPreviewType(String previewType) {
            mPreviewType = previewType;
            return this;
        }

        /**
         * Sets the status for this file transfer.
         * @param status The status of this file transfer.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setFileTransferStatus(@RcsFileTransferStatus int status) {
            mFileTransferStatus = status;
            return this;
        }

        /**
         * Builds an RcsFileTransferPart. This part is not persisted into storage unless
         * {@link RcsMessage#addFileTransferPart(RcsFileTransferPart)} is called.
         */
        public void build() {
            RcsFileTransferPart rcsFileTransferPart = new RcsFileTransferPart(
                    mRcsFileTransferSessionId, mContentUri, mContentType, mFileSize,
                    mTransferOffset, mWidth, mHeight, mLength, mPreviewUri, mPreviewType,
                    mFileTransferStatus);
        }
    }

    /**
     * @return Returns the status of this file transfer.
     */
    public @RcsFileTransferStatus int getFileTransferStatus() {
        return mFileTransferStatus;
    }

    public static final Creator<RcsFileTransferPart> CREATOR = new Creator<RcsFileTransferPart>() {
        @Override
        public RcsFileTransferPart createFromParcel(Parcel in) {
            return new RcsFileTransferPart(in);
        }

        @Override
        public RcsFileTransferPart[] newArray(int size) {
            return new RcsFileTransferPart[size];
        }
    };

    protected RcsFileTransferPart(Parcel in) {
        mId = in.readInt();
        mRcsFileTransferSessionId = in.readString();
        mContentType = in.readString();
        mContentUri = Uri.parse(in.readString());
        mFileSize = in.readLong();
        mTransferOffset = in.readLong();
        mFileTransferStatus = in.readInt();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mLength = in.readLong();
        mPreviewUri = Uri.parse(in.readString());
        mPreviewType = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mRcsFileTransferSessionId);
        dest.writeString(mContentType);
        dest.writeString(mContentUri.toString());
        dest.writeLong(mFileSize);
        dest.writeLong(mTransferOffset);
        dest.writeLong(mFileTransferStatus);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeLong(mLength);
        dest.writeString(mPreviewUri.toString());
        dest.writeString(mPreviewType);
    }
}
