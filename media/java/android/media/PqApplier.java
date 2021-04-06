package android.media;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Bundle;
import android.os.IPqRepository;
import android.os.IPqRepositoryChangeListener;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

final public class PqApplier implements AutoCloseable {
    private static final String TAG = "PqApplier";
    private MediaCodec mMc;
    private String mPackageName;
    private String mSession;
    private IPqRepository mPqRepository;

    PqApplier(MediaCodec mc, String packageName) {
        mPackageName = packageName;
        mMc= mc;
        mSession = null;
        // register callback when PQParams are updated by apps
        try {
            getPqRepository().setOnChangeListener(mPqRepositoryChangeListener, packageName);
        }
        catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    // Listener for change on PQ repo
    private IPqRepositoryChangeListener mPqRepositoryChangeListener = new IPqRepositoryChangeListener.Stub() {
        // Non PQ Aware case: mSession == null && session == null
        // PQ Aware case: mSession != null && mSession == session
        @Override
        public void onChanged(String packageName, String session){
            // Check own data or not
            if(mPackageName != packageName) return;

            // set PQ params to PQ HAL
            String pqParams = null;
            pqParams = getPqParamsFromPqRepo(packageName, session);
            Log.d(TAG, "onChanged " + mPackageName + " " + session + " " + pqParams);
            Bundle params = new Bundle();
            params.putObject("vendor.mtk-pq.pqsetting", pqParams);
            if(pqParams != null){
                mMc.setParameters(params);
            }
        }
    };

    // get PQ Repository interface
    private IPqRepository getPqRepository() {
        if (mPqRepository != null) {
            return mPqRepository;
        }
        mPqRepository = IPqRepository.Stub.asInterface(ServiceManager.getService("PqRepositoryService"));
        if (mPqRepository == null) {
            Log.e(TAG, "getPqRepository() PqRepositoryService == null");
        }

        return mPqRepository;
    }

    private String getPqParamsFromPqRepo(@NonNull String packageName, /*PQsession*/String session) {
        String pqParams = null;
        try {
            pqParams = getPqRepository().getPqParams(packageName, session);
        }
        catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        return pqParams;
    }

    private void setPqParamsToPqRepo(@NonNull String packageName, String session, @NonNull String pqParams) {
        try {
            getPqRepository().setPqParams(packageName, session, pqParams);
        }
        catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    // setPQParams() for package (Currently no uasge)
    public void setPqParamsToPqRepo(@NonNull String pqParams) {
        setPqParamsToPqRepo(mPackageName, null, pqParams);
    }

    // setPQParams() for per-stream (PQ Aware App)
    public void setPqParamsToPqRepoWithSession(@NonNull String pqParams) {
        try {
            if(mSession == null) {
                mSession = getPqRepository().startSession(mPackageName);
            }
            Log.d(TAG, "setPqParamsToPqRepo " + mPackageName + " " + mSession + " " + pqParams);

            getPqRepository().setOnChangeListenerWithSession(mPqRepositoryChangeListener, mPackageName, mSession);
        }
        catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        setPqParamsToPqRepo(mPackageName, mSession, pqParams);
    }

    public void setPqParamsToHal() {
        // Set PQ Parameters into PQ HAL
        String pqParams = getPqParamsFromPqRepo(mPackageName, mSession);
        if (pqParams == null) {
            pqParams = "default";
        }
        Log.d(TAG, "setPqParamsToHal " + mPackageName + " " + mSession + " " + pqParams);

        if (pqParams != null) {
            Bundle params = new Bundle();
            params.putObject("vendor.mtk-pq.pqsetting", pqParams);
            mMc.setParameters(params);
        }
    }

    @Override
    public void close() {
        try {
            getPqRepository().stopSession(mPackageName);
        }
        catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
