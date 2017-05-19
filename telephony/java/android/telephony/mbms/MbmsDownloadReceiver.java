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
 * limitations under the License
 */

package android.telephony.mbms;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.MbmsDownloadManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @hide
 */
public class MbmsDownloadReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "MbmsDownloadReceiver";

    public static final String MBMS_FILE_PROVIDER_META_DATA_KEY = "mbms-file-provider-authority";

    private String mFileProviderAuthorityCache = null;
    private String mMiddlewarePackageNameCache = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MbmsDownloadManager.ACTION_DOWNLOAD_RESULT_INTERNAL.equals(intent.getAction())) {
            moveDownloadedFiles(context, intent);
            cleanupTempFiles(context, intent);
        } else if (MbmsDownloadManager.ACTION_FILE_DESCRIPTOR_REQUEST.equals(intent.getAction())) {
            generateTempFiles(context, intent);
        }
    }

    private void cleanupTempFiles(Context context, Intent intent) {
        // TODO: account for in-use temp files
        DownloadRequest request = intent.getParcelableExtra(MbmsDownloadManager.EXTRA_REQUEST);
        if (request == null) {
            Log.w(LOG_TAG, "Intent does not include a DownloadRequest. Ignoring.");
            return;
        }

        List<Uri> tempFiles = intent.getParcelableExtra(MbmsDownloadManager.EXTRA_TEMP_LIST);
        if (tempFiles == null) {
            return;
        }

        for (Uri tempFileUri : tempFiles) {
            deleteTempFile(context, request, tempFileUri);
        }
    }

    private void moveDownloadedFiles(Context context, Intent intent) {
        int result = intent.getIntExtra(MbmsDownloadManager.EXTRA_RESULT,
                MbmsDownloadManager.RESULT_VOID);

        if (result == MbmsDownloadManager.RESULT_VOID) {
            Log.w(LOG_TAG, "Download result did not include a result code. Ignoring.");
            setResultCode(1 /* TODO: define error constants */);
            return;
        }

        DownloadRequest request = intent.getParcelableExtra(MbmsDownloadManager.EXTRA_REQUEST);
        if (request == null) {
            Log.w(LOG_TAG, "Download result did not include the associated request. Ignoring.");
            setResultCode(1 /* TODO: define error constants */);
            return;
        }

        Uri destinationUri = request.getDestinationUri();
        Uri finalTempFile = intent.getParcelableExtra(MbmsDownloadManager.EXTRA_FINAL_URI);
        if (!verifyTempFilePath(context, request, finalTempFile)) {
            Log.w(LOG_TAG, "Download result specified an invalid temp file " + finalTempFile);
            setResultCode(1);
            return;
        }

        if (!moveTempFile(finalTempFile, destinationUri)) {
            Log.w(LOG_TAG, "Failed to move temp file to final destination");
            setResultCode(1);
        }

        context.sendBroadcast(request.getIntentForApp());
        setResultCode(0);
    }

    private void generateTempFiles(Context context, Intent intent) {
        DownloadRequest request = intent.getParcelableExtra(MbmsDownloadManager.EXTRA_REQUEST);
        if (request == null) {
            Log.w(LOG_TAG, "Temp file request did not include the associated request. Ignoring.");
            setResultCode(1 /* TODO: define error constants */);
            return;
        }
        int fdCount = intent.getIntExtra(MbmsDownloadManager.EXTRA_FD_COUNT, 0);
        List<Uri> pausedList = intent.getParcelableExtra(MbmsDownloadManager.EXTRA_PAUSED_LIST);

        if (fdCount == 0 && (pausedList == null || pausedList.size() == 0)) {
            Log.i(LOG_TAG, "No temp files actually requested. Ending.");
            setResultCode(0);
            return;
        }

        List<UriPathPair> freshTempFiles = generateFreshTempFiles(context, request, fdCount);
        List<UriPathPair> pausedFiles = generateUrisForPausedFiles(context, request, pausedList);

        Bundle result = new Bundle();
        result.putParcelableList(MbmsDownloadManager.EXTRA_FREE_URI_LIST, freshTempFiles);
        result.putParcelableList(MbmsDownloadManager.EXTRA_PAUSED_URI_LIST, pausedFiles);
        setResultCode(0);
        setResultExtras(result);
    }

    private List<UriPathPair> generateUrisForPausedFiles(Context context, DownloadRequest request,
            List<Uri> pausedFiles) {
        List<UriPathPair> result = new LinkedList<>();
        if (pausedFiles == null) {
            return result;
        }

        for (Uri fileUri : pausedFiles) {
            if (!verifyTempFilePath(context, request, fileUri)) {
                Log.w(LOG_TAG, "Supplied file " + fileUri + " is not a valid temp file to resume");
                continue;
            }
            File tempFile = new File(fileUri.getSchemeSpecificPart());
            if (!tempFile.exists()) {
                Log.w(LOG_TAG, "Supplied file " + fileUri + " does not exist.");
                continue;
            }
            Uri contentUri = MbmsTempFileProvider.getUriForFile(
                    context, getFileProviderAuthorityCached(context), tempFile);
            context.grantUriPermission(getMiddlewarePackage(context), contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            result.add(new UriPathPair(fileUri, contentUri));
        }
        return result;
    }

    private List<UriPathPair> generateFreshTempFiles(Context context, DownloadRequest request,
            int freshFdCount) {
        File tempFileDir = getEmbmsTempFileDirForRequest(context, request);
        if (!tempFileDir.exists()) {
            tempFileDir.mkdirs();
        }

        // Name the files with the template "N-UUID", where N is the request ID and UUID is a
        // random uuid.
        List<UriPathPair> result = new LinkedList<>();
        for (int i = 0; i < freshFdCount; i++) {
            String fileName = String.valueOf(request.getDownloadId()) + "-" + UUID.randomUUID();
            File tempFile = new File(tempFileDir, fileName);
            Uri fileUri;
            try {
                // TODO: what happens if this returns false?
                tempFile.createNewFile();
                fileUri = Uri.fromParts(
                        ContentResolver.SCHEME_FILE, tempFile.getCanonicalPath(), null);
            } catch (IOException e) {
                // TODO: anything better we can do to recover?
                continue;
            }
            Uri contentUri = MbmsTempFileProvider.getUriForFile(
                    context, getFileProviderAuthorityCached(context), tempFile);
            context.grantUriPermission(getMiddlewarePackage(context), contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            result.add(new UriPathPair(fileUri, contentUri));
        }

        return result;
    }

    private static void deleteTempFile(Context context, DownloadRequest request, Uri tempFileUri) {
        if (verifyTempFilePath(context, request, tempFileUri)) {
            File tempFile = new File(tempFileUri.getSchemeSpecificPart());
            tempFile.delete();
        }
    }

    private static boolean moveTempFile(Uri fromPath, Uri toPath) {
        if (!ContentResolver.SCHEME_FILE.equals(fromPath.getScheme())) {
            Log.w(LOG_TAG, "Moving source uri " + fromPath+ " does not have a file scheme");
            return false;
        }
        if (!ContentResolver.SCHEME_FILE.equals(toPath.getScheme())) {
            Log.w(LOG_TAG, "Moving destination uri " + toPath + " does not have a file scheme");
            return false;
        }

        File fromFile = new File(fromPath.getSchemeSpecificPart());
        File toFile = new File(toPath.getSchemeSpecificPart());
        // TODO: This may not work if the two files are on different filesystems. Should we
        // enforce that the temp file storage and the permanent storage are both in the same fs?
        return fromFile.renameTo(toFile);
    }

    private static boolean verifyTempFilePath(Context context, DownloadRequest request,
            Uri filePath) {
        if (!ContentResolver.SCHEME_FILE.equals(filePath.getScheme())) {
            Log.w(LOG_TAG, "Uri " + filePath + " does not have a file scheme");
            return false;
        }

        String path = filePath.getSchemeSpecificPart();
        File tempFile = new File(path);
        if (!tempFile.exists()) {
            Log.w(LOG_TAG, "File at " + path + " does not exist.");
            return false;
        }

        if (!MbmsUtils.isContainedIn(getEmbmsTempFileDirForRequest(context, request), tempFile)) {
            return false;
        }

        return true;
    }

    /**
     * Returns a File linked to the directory used to store temp files for this request
     */
    private static File getEmbmsTempFileDirForRequest(Context context, DownloadRequest request) {
        File embmsTempFileDir = MbmsTempFileProvider.getEmbmsTempFileDir(
                context, getFileProviderAuthority(context));

        // TODO: better naming scheme for temp file dirs
        String tempFileDirName = String.valueOf(request.getDownloadId());
        return new File(embmsTempFileDir, tempFileDirName);
    }

    private String getFileProviderAuthorityCached(Context context) {
        if (mFileProviderAuthorityCache != null) {
            return mFileProviderAuthorityCache;
        }

        mFileProviderAuthorityCache = getFileProviderAuthority(context);
        return mFileProviderAuthorityCache;
    }

    private static String getFileProviderAuthority(Context context) {
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Package manager couldn't find " + context.getPackageName());
        }
        String authority = appInfo.metaData.getString(MBMS_FILE_PROVIDER_META_DATA_KEY);
        if (authority == null) {
            throw new RuntimeException("Must declare the file provider authority as meta data");
        }
        return authority;
    }

    private String getMiddlewarePackage(Context context) {
        if (mMiddlewarePackageNameCache != null) {
            return mMiddlewarePackageNameCache;
        }

        // Query for the proper service
        PackageManager packageManager = context.getPackageManager();
        Intent queryIntent = new Intent();
        queryIntent.setAction(MbmsDownloadManager.MBMS_DOWNLOAD_SERVICE_ACTION);
        List<ResolveInfo> downloadServices = packageManager.queryIntentServices(queryIntent,
                PackageManager.MATCH_SYSTEM_ONLY);

        if (downloadServices == null || downloadServices.size() == 0) {
            Log.w(LOG_TAG, "No download services found, cannot get package name");
            return null;
        }

        if (downloadServices.size() > 1) {
            Log.w(LOG_TAG, "More than one download service found, cannot get unique package name");
            return null;
        }
        mMiddlewarePackageNameCache =
                downloadServices.get(0).getComponentInfo().getComponentName().getPackageName();
        return mMiddlewarePackageNameCache;
    }
}
