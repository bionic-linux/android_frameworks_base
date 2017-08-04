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

package android.telephony.mbms.vendor;

import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.mbms.IMbmsStreamingManagerCallback;
import android.telephony.mbms.IStreamingServiceCallback;
import android.telephony.mbms.MbmsException;
import android.telephony.mbms.MbmsStreamingManagerCallback;
import android.telephony.mbms.StreamingService;
import android.telephony.mbms.StreamingServiceCallback;
import android.telephony.mbms.StreamingServiceInfo;

import java.util.List;

/**
 * @hide
 */
@SystemApi
public class MbmsStreamingServiceBase extends Service {

    private final IBinder mBinder = new IMbmsStreamingService.Stub() {
        @Override
        public int initialize(IMbmsStreamingManagerCallback listener, final int subscriptionId)
                throws RemoteException {
            final int callingUid = Binder.getCallingUid();
            listener.asBinder().linkToDeath(new DeathRecipient() {
                @Override
                public void binderDied() {
                    onCallbackDied(callingUid, subscriptionId);
                }
            }, 0);

            return MbmsStreamingServiceBase.this.initialize(new MbmsStreamingManagerCallback() {
                @Override
                public void error(int errorCode, String message) {
                    try {
                        listener.error(errorCode, message);
                    } catch (RemoteException e) {
                        onCallbackDied(callingUid, subscriptionId);
                    }
                }

                @Override
                public void streamingServicesUpdated(List<StreamingServiceInfo> services) {
                    try {
                        listener.streamingServicesUpdated(services);
                    } catch (RemoteException e) {
                        onCallbackDied(callingUid, subscriptionId);
                    }
                }

                @Override
                public void middlewareReady() {
                    try {
                        listener.middlewareReady();
                    } catch (RemoteException e) {
                        onCallbackDied(callingUid, subscriptionId);
                    }
                }
            }, subscriptionId);
        }

        @Override
        public int getStreamingServices(int subId, List<String> serviceClasses) throws
                RemoteException {
            return MbmsStreamingServiceBase.this.getStreamingServices(subId, serviceClasses);
        }

        @Override
        public int startStreaming(final int subscriptionId, String serviceId,
                IStreamingServiceCallback listener) throws RemoteException {
            final int callingUid = Binder.getCallingUid();
            listener.asBinder().linkToDeath(new DeathRecipient() {
                @Override
                public void binderDied() {
                    onCallbackDied(callingUid, subscriptionId);
                }
            }, 0);

            return MbmsStreamingServiceBase.this.startStreaming(subscriptionId, serviceId,
                    new StreamingServiceCallback() {
                        @Override
                        public void error(int errorCode, String message) {
                            try {
                                listener.error(errorCode, message);
                            } catch (RemoteException e) {
                                onCallbackDied(callingUid, subscriptionId);
                            }
                        }

                        @Override
                        public void streamStateUpdated(@StreamingService.StreamingState int state,
                                @StreamingService.StreamingStateChangeReason int reason) {
                            try {
                                listener.streamStateUpdated(state, reason);
                            } catch (RemoteException e) {
                                onCallbackDied(callingUid, subscriptionId);
                            }
                        }

                        @Override
                        public void mediaDescriptionUpdated() {
                            try {
                                listener.mediaDescriptionUpdated();
                            } catch (RemoteException e) {
                                onCallbackDied(callingUid, subscriptionId);
                            }
                        }

                        @Override
                        public void broadcastSignalStrengthUpdated(int signalStrength) {
                            try {
                                listener.broadcastSignalStrengthUpdated(signalStrength);
                            } catch (RemoteException e) {
                                onCallbackDied(callingUid, subscriptionId);
                            }
                        }

                        @Override
                        public void streamMethodUpdated(int methodType) {
                            try {
                                listener.streamMethodUpdated(methodType);
                            } catch (RemoteException e) {
                                onCallbackDied(callingUid, subscriptionId);
                            }
                        }
            });
        }

        @Override
        public Uri getPlaybackUri(int subId, String serviceId) throws RemoteException {
            return MbmsStreamingServiceBase.this.getPlaybackUri(subId, serviceId);
        }

        @Override
        public void stopStreaming(int subId, String serviceId) throws RemoteException {
            MbmsStreamingServiceBase.this.stopStreaming(subId, serviceId);
        }

        @Override
        public void disposeStream(int subId, String serviceId) throws RemoteException {
            MbmsStreamingServiceBase.this.disposeStream(subId, serviceId);
        }

        @Override
        public void dispose(int subId) throws RemoteException {
            MbmsStreamingServiceBase.this.dispose(subId);
        }
    };

    /**
     * Initialize streaming service for this app and subId, registering the listener.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}, which
     * will be intercepted and passed to the app as
     * {@link android.telephony.mbms.MbmsException.InitializationErrors#ERROR_UNABLE_TO_INITIALIZE}
     *
     * May return any value from {@link android.telephony.mbms.MbmsException.InitializationErrors}
     * or {@link MbmsException#SUCCESS}. Non-successful error codes will be passed to the app via
     * {@link IMbmsStreamingManagerCallback#error(int, String)}.
     *
     * @param listener The callback to use to communicate with the app.
     * @param subscriptionId The subscription ID to use.
     */
    public int initialize(MbmsStreamingManagerCallback listener, int subscriptionId)
            throws RemoteException {
        return 0;
    }

    /**
     * Registers serviceClasses of interest with the appName/subId key.
     * Starts async fetching data on streaming services of matching classes to be reported
     * later via {@link IMbmsStreamingManagerCallback#streamingServicesUpdated(List)}
     *
     * Note that subsequent calls with the same uid and subId will replace
     * the service class list.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     * @param serviceClasses The service classes that the app wishes to get info on. The strings
     *                       may contain arbitrary data as negotiated between the app and the
     *                       carrier.
     * @return {@link MbmsException#SUCCESS} or any of the errors in
     * {@link android.telephony.mbms.MbmsException.GeneralErrors}
     */
    public int getStreamingServices(int subscriptionId, List<String> serviceClasses) {
        return 0;
    }

    /**
     * Starts streaming on a particular service. This method may perform asynchronous work. When
     * the middleware is ready to send bits to the frontend, it should inform the app via
     * {@link IStreamingServiceCallback#streamStateUpdated(int, int)}.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     * @param serviceId The ID of the streaming service that the app has requested.
     * @param listener The listener object on which the app wishes to receive updates.
     * @return Any error in {@link android.telephony.mbms.MbmsException.GeneralErrors}
     */
    public int startStreaming(int subscriptionId, String serviceId,
            StreamingServiceCallback listener) throws RemoteException {
        return 0;
    }

    /**
     * Retrieves the streaming URI for a particular service. If the middleware is not yet ready to
     * stream the service, this method may return null.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     * @param serviceId The ID of the streaming service that the app has requested.
     * @return An opaque {@link Uri} to be passed to a video player that understands the format.
     */
    public @Nullable Uri getPlaybackUri(int subscriptionId, String serviceId) {
        return null;
    }

    /**
     * Stop streaming the stream identified by {@code serviceId}. Notification of the resulting
     * stream state change should be reported to the app via
     * {@link IStreamingServiceCallback#streamStateUpdated(int, int)}.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     * @param serviceId The ID of the streaming service that the app wishes to stop.
     */
    public void stopStreaming(int subscriptionId, String serviceId) {
    }

    /**
     * Dispose of the stream identified by {@code serviceId} for the app identified by the
     * {@code appName} and {@code subscriptionId} arguments along with the caller's uid.
     * No notification back to the app is required for this operation, and the callback provided via
     * {@link #startStreaming(int, String, StreamingServiceCallback)} should no longer be
     * used after this method has called by the app.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     * @param serviceId The ID of the streaming service that the app wishes to dispose of.
     */
    public void disposeStream(int subscriptionId, String serviceId)
            throws RemoteException {
    }

    /**
     * Signals that the app wishes to dispose of the session identified by the
     * {@code subscriptionId} argument and the caller's uid. No notification back to the
     * app is required for this operation, and the corresponding callback provided via
     * {@link #initialize(MbmsStreamingManagerCallback, int)} should no longer be used
     * after this method has been called by the app.
     *
     * May throw an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     */
    public void dispose(int subscriptionId) {
    }

    /**
     * Indicates that the client app identified by {@code uid} and {@code subscriptionId} has died.
     * @param uid The uid of the calling app, as returned from {@link Binder#getCallingUid()}.
     * @param subscriptionId The subscription ID that the app is using.
     */
    public void onCallbackDied(int uid, int subscriptionId) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
