/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.server.pq;

import java.lang.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.Context;
import android.os.RemoteException;
import android.os.IPqRepository;
import android.os.IPqRepositoryChangeListener;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;

import android.annotation.NonNull;
import android.annotation.Nullable;
import com.android.server.SystemService;

public class PqRepositoryService extends SystemService {
    private static final String TAG = "PqRepositoryService";
    private static final boolean DEBUG = false;
    private final AtomicInteger mSession = new AtomicInteger();

    private class MapKey {
        private String mPackageName;
        private String SessionId;
        private MapKey(String packageName, String session) {
            this.mPackageName = packageName;
            this.SessionId = session;
        }

        public void setPackageName(@NonNull String packageName) {
            this.mPackageName = packageName;
        }

        public void setSessionId(@Nullable String session) {
            this.SessionId = session;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public String getSessionId() {
            return this.SessionId;
        }
    }

    private class MapValue {
        private String PqParam;
        private IPqRepositoryChangeListener Listener;
        private MapValue(String pqParams, IPqRepositoryChangeListener listener) {
            this.PqParam = pqParams;
            this.Listener = listener;
        }

        public void setPqParam(@NonNull String param) {
            this.PqParam = param;
        }

        public void setListener(@NonNull IPqRepositoryChangeListener listener) {
            if (listener != null) {
                this.Listener = listener;
            }
        }

        public String getPqParam() {
            return this.PqParam;
        }

        public IPqRepositoryChangeListener getListener() {
            return this.Listener;
        }
    }

    public PqRepositoryService(@Nullable Context context) {
        super(context);
    }

    private Map<MapKey, MapValue> mMap = new HashMap<MapKey, MapValue>();

    @Override
    public void onStart() {
        ServiceManager.addService("PqRepositoryService", new mBinderService());
    }

    private final class mBinderService extends IPqRepository.Stub {

        @Override
        public String getPqParams(String packageName, String session) {
            String pqParams = null;
            MapKey key = findMapValue(packageName, session);

            if (key != null) {
                pqParams = mMap.get(key).getPqParam();
            }

            return pqParams;

        }

        @Override
        public void setPqParams(String packageName, String session, String pqParams) {
            IPqRepositoryChangeListener mListener = null;
            MapKey key = findMapValue(packageName, session);
            if (session != null) {

                if (key != null) {
                    MapValue value = (MapValue) mMap.get(key);
                    if (value != null) {
                        mListener = (IPqRepositoryChangeListener) value.getListener();
                        value.setPqParam(pqParams);
                        mMap.put(key, value);
                    }
                }

                // onChanged PqParams
                if(mListener != null) {
                    try {
                        mListener.onChanged(packageName, session);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                // When session is null
                if (key == null) {
                    // Create new key
                    key = new MapKey(packageName, session);
                    MapValue value = new MapValue(pqParams, null);
                    mMap.put(key, value);
                } else {
                    MapValue value = (MapValue) mMap.get(key);
                    if (value != null) {
                        value.setPqParam(pqParams);
                        mMap.put(key, value);
                    }
                }
            }

        }

        // per-stream callback
        @Override
        public void setOnChangeListenerWithSession(IPqRepositoryChangeListener listener,
               String packageName, String session) {
            MapKey key = findMapValue(packageName, session);

            if (key != null) {
                MapValue value = new MapValue(null, listener);
                if (value != null) {
                    mMap.put(key, value);
                }
            }

        }

        // global callback (currently not used)
        @Override
        public void setOnChangeListener(IPqRepositoryChangeListener listener, String packageName) {
            String pqParam = null;
            MapKey non_aware_key = findMapValue(packageName, null);
            if (non_aware_key != null) {
                pqParam = mMap.get(non_aware_key).getPqParam();
            } else {
                return;
            }

            MapKey key = new MapKey(packageName, null);
            MapValue value = new MapValue(pqParam, listener);
            mMap.put(key, value);
        }

        @Override
        public String startSession(String packageName) {
            String session = generateUniqueId();
            MapKey key = new MapKey(packageName, session);
            mMap.put(key, null);
            return session;
        }

        @Override
        public void stopSession(String packageName) {
            MapKey key = null;
            Iterator<MapKey> it = mMap.keySet().iterator();
            while (it.hasNext()) {
                key = it.next();
                if (key.getPackageName().equals(packageName)) {
                    // remove per-stream session
                    if (key.getSessionId() != null) {
                        mMap.remove(key);
                    } else {
                        // remove non-aware listener
                        if (mMap.get(key).getListener() != null) {
                            mMap.remove(key);
                        }
                    }
                }
            }
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("PQ REPOSITORY SRV (dumpsys PqRepositoryService)\n");
            pw.println("PqRepository List:");
            int index = 0;
            MapKey key = null;
            Iterator<MapKey> it = mMap.keySet().iterator();
            while (it.hasNext()) {
                key = it.next();
                pw.println("PQ index : " + index);
                pw.println("  PackageName = " + key.getPackageName());
                pw.println("  SessionId   = " + key.getSessionId());
                if (mMap.get(key) != null) {
                    pw.println("  PqParams    = " + mMap.get(key).getPqParam());
                } else {
                    pw.println("  MapValue is null " );
                }
                index++;
            }
        }
    }

    private String generateUniqueId() {
        return String.valueOf(mSession.incrementAndGet());
    }

    // Use for find the MapValue by packageName and session
    private MapKey findMapValue(String packageName, String sessionId) {
        MapKey key = null;
        Iterator<MapKey> it = mMap.keySet().iterator();
        while (it.hasNext()) {
            key = it.next();
            if (key.getPackageName().equals(packageName)) {
                if (key.getSessionId() == null) {
                    return key;
                } else {
                    if (key.getSessionId().equals(sessionId)) {
                        return key;
                    }
                }
            }
        }
        return key;
    }

}

