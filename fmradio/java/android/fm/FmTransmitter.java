/*
 * Copyright (C) ST-Ericsson SA 2010
 * Copyright (C) 2010 The Android Open Source Project
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
 *
 * Author: Bjorn Pileryd (bjorn.pileryd@sonyericsson.com)
 * Author: Markus Grape (markus.grape@stericsson.com) for ST-Ericsson
 */

package android.fm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

/**
 * The FmTransmitter controls the output of FM radio from the device. When
 * started, the transmitter will transmit audio via FM signals. The unit for all
 * frequencies in this class is kHz. Note that this API only controls the output
 * of FM radio, to select the audio stream the MediaPlayer interface should be
 * used, see code example below the state diagram.
 * <p>
 * The output frequency can be changed at any time using
 * {@link #setFrequency(int)}. The transmitter also supports transmission of RDS
 * data, see {@link #setRdsData(Bundle)}.
 * </p>
 * <p>
 * Get an instance of this class by calling
 * {@link android.content.Context#getSystemService(String)
 * Context.getSystemService(Context.FM_TRANSMITTER_SERVICE)}.
 * </p>
 * <a name="StateDiagram"></a> <h3>State Diagram</h3>
 * <p>
 * The state machine is designed to take into account that some hardware may
 * need time to prepare, and that it is likely to consume more power when paused
 * and started than it does in the idle state. The hardware implementation of
 * this interface should do the time consuming preparation procedures in the
 * starting state. The switching between paused and started states should be
 * fast to give a good user experience.
 * </p>
 * <p>
 * <img src="../../../images/FmTransmitter_states.gif"
 * alt="FmTransmitter State diagram" border="0" />
 * </p>
 * <table border="1">
 * <tr>
 * <th>Method Name</th>
 * <th>Valid States</th>
 * <th>Invalid States</th>
 * <th>Comments</th>
 * </tr>
 * <tr>
 * <td>{@link #startAsync(FmBand)}</td>
 * <td>{idle}</td>
 * <td>{starting, paused, started, scanning}</td>
 * <td>Successful invocation of this method in a valid state transfers the
 * object to the starting state. Calling this method in an invalid state throws
 * an IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #start(FmBand)}</td>
 * <td>{idle}</td>
 * <td>{starting, paused, started, scanning}</td>
 * <td>Successful invocation of this method in a valid state transfers the
 * object to the started state. Calling this method in an invalid state throws
 * an IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #resume()}</td>
 * <td>{paused, started}</td>
 * <td>{idle, starting, scanning}</td>
 * <td>Successful invocation of this method in a valid state transfers the
 * object to the started state. Calling this method in an invalid state throws
 * an IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #pause()}</td>
 * <td>{started, paused}</td>
 * <td>{idle, starting, scanning}</td>
 * <td>Successful invocation of this method in a valid state transfers the
 * object to the paused state. Calling this method in an invalid state throws an
 * IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #reset()}</td>
 * <td>any</td>
 * <td>{}</td>
 * <td>Successful invocation of this method transfers the object to the idle
 * state, the object is like being just created.</td>
 * </tr>
 * <tr>
 * <td>{@link #getState()}</td>
 * <td>any</td>
 * <td>{}</td>
 * <td>This method can be called in any state and calling it does not change the
 * object state.</td>
 * </tr>
 * <tr>
 * <td>{@link #setFrequency(int)}</td>
 * <td>{paused, started}</td>
 * <td>{idle, starting, scanning}</td>
 * <td>Successful invocation of this method in a valid state does not change the
 * object state. Calling this method in an invalid state throws an
 * IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #getFrequency()}</td>
 * <td>{paused, started}</td>
 * <td>{idle, starting, scanning}</td>
 * <td>Successful invocation of this method in a valid state does not change the
 * object state. Calling this method in an invalid state throws an
 * IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #setRdsData(Bundle)}</td>
 * <td>{paused, started}</td>
 * <td>{idle, starting, scanning}</td>
 * <td>Successful invocation of this method in a valid state does not change the
 * object state. Calling this method in an invalid state throws an
 * IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #isBlockScanSupported()}</td>
 * <td>any</td>
 * <td>{}</td>
 * <td>This method can be called in any state and calling it does not change the
 * object state.</td>
 * </tr>
 * <tr>
 * <td>{@link #startBlockScan(int, int)}</td>
 * <td>{paused, started}</td>
 * <td>{idle, starting, scanning}</td>
 * <td>Successful invocation of this method in a valid state transfers the
 * object to the scanning state. Calling this method in an invalid state throws
 * an IllegalStateException.</td>
 * </tr>
 * <tr>
 * <td>{@link #stopScan()}</td>
 * <td>any</td>
 * <td>{}</td>
 * <td>Successful invocation of this method in a valid state tries to stop
 * performing a scan operation. The hardware might continue the scan for an
 * unspecified amount of time after this method is called. Once the scan has
 * stopped, it will be notified via {@link OnScanListener}</td>
 * </tr>
 * <tr>
 * <td>{@link #sendExtraCommand(String, String[])}</td>
 * <td>vendor specific</td>
 * <td>vendor specific</td>
 * <td>vendor specific</td>
 * </tr>
 * </table>
 * <a name="Examples"></a> <h3>Example code</h3>
 * <pre>
 * // prepare and start the FM transmitter
 * FmTransmitter fmt = (FmTransmitter) getSystemService(Context.FM_TRANSMITTER_SERVICE);
 * fmt.start(new FmBand(FmBand.BAND_EU));
 *
 * // prepare and start playback
 * MediaPlayer mp = new MediaPlayer();
 * mp.setDataSource(PATH_TO_FILE);
 * mp.prepare();
 * mp.start();
 * </pre>
 * <a name="FMHandling"></a> <h3>FM receiving/transmission handling</h3>
 * <p>
 * In this API, FM radio cannot be received and transmitted at the same time,
 * therefore the state machine is designed to prevent incorrect usage. The
 * FmReceiver and FmTransmitter has a separate state machine and only one can be
 * <i>active</i> (state other than idle).
 * <ul>
 * <li>If start is called on FmTransmitter and the FmReceiver is <i>active</i>,
 * the FmReceiver MUST release resources and change state to idle.</li>
 * <li>The FmReceiver will in that case be notified by
 * {@link android.fm.FmReceiver.OnForcedResetListener#onForcedReset(int)}.</li>
 * </ul>
 * </p>
 * <a name="ErrorHandling"></a> <h3>Error handling</h3>
 * <p>
 * In general, it is up to the application that uses this API to keep track of
 * events that could affect the FM radio user experience. The hardware
 * implementation side of this API should only take actions when it is really
 * necessary, e.g. if the hardware is forced to pause or reset, and notify the
 * application by using the {@link OnForcedPauseListener},
 * {@link OnForcedResetListener} or {@link OnErrorListener}.
 * </p>
 */
public class FmTransmitter {

    private static final String TAG = "FmTransmitter";

    /**
     * The FmTransmitter had to be shut down due to a non-critical error,
     * meaning that it is OK to attempt a restart immediately after this. An
     * example is when the hardware was shut down in order to save power after
     * being in the paused state for too long.
     */
    public static final int RESET_NON_CRITICAL = 0;

    /**
     * The FmTransmitter had to be shut down due to a critical error. The FM
     * hardware it not guaranteed to work as expected after receiving this
     * error.
     */
    public static final int RESET_CRITICAL = 1;

    /**
     * The FmReceiver was activated and therefore the FmTransmitter must be put
     * in idle.
     *
     * @see FmReceiver#startAsync(FmBand)
     */
    public static final int RESET_RX_IN_USE = 2;

    /**
     * The radio is not allowed to be used, typically when flight mode is
     * enabled.
     */
    public static final int RESET_RADIO_FORBIDDEN = 3;

    /**
     * Indicates that the FmTransmitter is in an idle state. No resources are
     * allocated and power consumption is kept to a minimum.
     */
    public static final int STATE_IDLE = 0;

    /**
     * Indicates that the FmTransmitter is allocating resources and preparing to
     * transmit FM radio.
     */
    public static final int STATE_STARTING = 1;

    /**
     * Indicates that the FmTransmitter is transmitting FM radio.
     */
    public static final int STATE_STARTED = 2;

    /**
     * Indicates that the FmTransmitter has allocated resources and is ready to
     * instantly transmit FM radio.
     */
    public static final int STATE_PAUSED = 3;

    /**
     * Indicates that the FmTransmitter is scanning. FM radio will not be
     * transmitted in this state.
     */
    public static final int STATE_SCANNING = 4;

    /**
     * Save the FmBand used to be able to validate frequencies.
     */
    private FmBand mBand;

    /**
     * Map from OnStateChanged to their associated ListenerTransport objects.
     */
    private HashMap<OnStateChangedListener, OnStateChangedListenerTransport> mOnStateChanged =
        new HashMap<OnStateChangedListener, OnStateChangedListenerTransport>();

    /**
     * Map from OnStarted to their associated ListenerTransport objects.
     */
    private HashMap<OnStartedListener, OnStartedListenerTransport> mOnStarted =
        new HashMap<OnStartedListener, OnStartedListenerTransport>();

    /**
     * Map from OnError to their associated ListenerTransport objects.
     */
    private HashMap<OnErrorListener, OnErrorListenerTransport> mOnError =
        new HashMap<OnErrorListener, OnErrorListenerTransport>();

    /**
     * Map from OnBlockScan to their associated ListenerTransport objects.
     */
    private HashMap<OnScanListener, OnBlockScanListenerTransport> mOnBlockScan =
        new HashMap<OnScanListener, OnBlockScanListenerTransport>();

    /**
     * Map from OnForcedPause to their associated ListenerTransport objects.
     */
    private HashMap<OnForcedPauseListener, OnForcedPauseListenerTransport> mOnForcedPause =
        new HashMap<OnForcedPauseListener, OnForcedPauseListenerTransport>();

    /**
     * Map from OnForcedReset to their associated ListenerTransport objects.
     */
    private HashMap<OnForcedResetListener, OnForcedResetListenerTransport> mOnForcedReset =
        new HashMap<OnForcedResetListener, OnForcedResetListenerTransport>();

    /**
     * Map from OnExtraCommand to their associated ListenerTransport objects.
     */
    private HashMap<OnExtraCommandListener, OnExtraCommandListenerTransport> mOnExtraCommand =
        new HashMap<OnExtraCommandListener, OnExtraCommandListenerTransport>();

    private class OnStateChangedListenerTransport extends IOnStateChangedListener.Stub {
        private static final int TYPE_ON_STATE_CHANGED = 1;

        private OnStateChangedListener mListener;
        private final Handler mListenerHandler;

        OnStateChangedListenerTransport(OnStateChangedListener listener) {
            mListener = listener;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        public void onStateChanged(int oldState, int newState) {
            Message msg = Message.obtain();
            msg.what = TYPE_ON_STATE_CHANGED;
            Bundle b = new Bundle();
            b.putInt("oldState", oldState);
            b.putInt("newState", newState);
            msg.obj = b;
            mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {
            switch (msg.what) {
            case TYPE_ON_STATE_CHANGED:
                Bundle b = (Bundle) msg.obj;
                int oldState = b.getInt("oldState");
                int newState = b.getInt("newState");
                mListener.onStateChanged(oldState, newState);
                break;
            }
        }
    }

    private class OnStartedListenerTransport extends IOnStartedListener.Stub {
        private static final int TYPE_ON_STARTED = 1;

        private OnStartedListener mListener;
        private final Handler mListenerHandler;

        OnStartedListenerTransport(OnStartedListener listener) {
            mListener = listener;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        public void onStarted() {
            Message msg = Message.obtain();
            msg.what = TYPE_ON_STARTED;
            mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {
            switch (msg.what) {
            case TYPE_ON_STARTED:
                mListener.onStarted();
                break;
            }
        }
    }

    private class OnErrorListenerTransport extends IOnErrorListener.Stub {
        private static final int TYPE_ON_ERROR = 1;

        private OnErrorListener mListener;
        private final Handler mListenerHandler;

        OnErrorListenerTransport(OnErrorListener listener) {
            mListener = listener;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        public void onError() {
            Message msg = Message.obtain();
            msg.what = TYPE_ON_ERROR;
            mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {
            switch (msg.what) {
            case TYPE_ON_ERROR:
                mListener.onError();
                break;
            }
        }
    }

    private class OnBlockScanListenerTransport extends IOnBlockScanListener.Stub {
        private static final int TYPE_ON_BLOCKSCAN = 1;

        private OnScanListener mListener;
        private final Handler mListenerHandler;

        OnBlockScanListenerTransport(OnScanListener listener) {
            mListener = listener;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        public void onBlockScan(int[] frequency, int[] signalStrength, boolean aborted) {
            Message msg = Message.obtain();
            msg.what = TYPE_ON_BLOCKSCAN;
            Bundle b = new Bundle();
            b.putIntArray("frequency", frequency);
            b.putIntArray("signalStrength", signalStrength);
            b.putBoolean("aborted", aborted);
            msg.obj = b;
            mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {

            switch (msg.what) {
            case TYPE_ON_BLOCKSCAN:
                Bundle b = (Bundle) msg.obj;
                int[] frequency = b.getIntArray("frequency");
                int[] signalStrengths = b.getIntArray("signalStrength");
                boolean aborted = b.getBoolean("aborted");
                mListener.onBlockScan(frequency, signalStrengths, aborted);
                break;
            }
        }
    }

    private class OnForcedPauseListenerTransport extends IOnForcedPauseListener.Stub {
        private static final int TYPE_ON_FORCEDPAUSE = 1;

        private OnForcedPauseListener mListener;
        private final Handler mListenerHandler;

        OnForcedPauseListenerTransport(OnForcedPauseListener listener) {
            mListener = listener;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        public void onForcedPause() {
            Message msg = Message.obtain();
            msg.what = TYPE_ON_FORCEDPAUSE;
            mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {
            switch (msg.what) {
            case TYPE_ON_FORCEDPAUSE:
                mListener.onForcedPause();
                break;
            }
        }
    }

    private class OnForcedResetListenerTransport extends IOnForcedResetListener.Stub {
        private static final int TYPE_ON_FORCEDRESET = 1;

        private OnForcedResetListener mListener;
        private final Handler mListenerHandler;

        OnForcedResetListenerTransport(OnForcedResetListener listener) {
            mListener = listener;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        public void onForcedReset(int reason) {
            Message msg = Message.obtain();
            msg.what = TYPE_ON_FORCEDRESET;
            Bundle b = new Bundle();
            b.putInt("reason", reason);
            msg.obj = b;
            mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {
            switch (msg.what) {
            case TYPE_ON_FORCEDRESET:
                Bundle b = (Bundle) msg.obj;
                int reason = b.getInt("reason");
                mListener.onForcedReset(reason);
                break;
            }
        }
    }

    private class OnExtraCommandListenerTransport extends IOnExtraCommandListener.Stub {
        private static final int TYPE_ON_EXTRA_COMMAND = 1;

        private OnExtraCommandListener mListener;
        private final Handler mListenerHandler;

        OnExtraCommandListenerTransport(OnExtraCommandListener listener) {
            mListener = listener;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        public void onExtraCommand(String response, Bundle extras) {
            Message msg = Message.obtain();
            msg.what = TYPE_ON_EXTRA_COMMAND;
            Bundle b = new Bundle();
            b.putString("response", response);
            b.putBundle("extras", extras);
            msg.obj = b;
            mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {
            Bundle b;
            boolean aborted;

            switch (msg.what) {
            case TYPE_ON_EXTRA_COMMAND:
                b = (Bundle) msg.obj;
                String response = b.getString("response");
                Bundle extras = b.getBundle("extras");
                mListener.onExtraCommand(response, extras);
                break;
            }
        }
    }

    private IFmTransmitter mService;

    /**
     * Creates a new FmTransmitter instance. Applications will almost always
     * want to use {@link android.content.Context#getSystemService
     * Context.getSystemService()} to retrieve the standard
     * {@link android.content.Context Context.FM_TRANSMITTER_SERVICE}.
     *
     * @param service
     *            the Binder interface
     * @hide - hide this because it takes in a parameter of type IFmReceiver,
     *       which is a system private class.
     */
    public FmTransmitter(IFmTransmitter service) {
        mService = service;
    }

    /**
     * Starts reception of the FM hardware. This is an asynchronous method since
     * different hardware can have varying startup times. When the reception is
     * started a callback to {@link OnStartedListener#onStarted()} is made.
     * <p>
     * When calling this method, an FmBand parameter must be passed that
     * describes the properties of the band that the FmTransmitter should
     * prepare for. If the band is null, invalid or not supported, an exception
     * will be thrown.
     * </p>
     * <p>
     * If the FmReceiver is active it will be forced to reset. See
     * {@link FmReceiver#RESET_TX_IN_USE}.
     * </p>
     *
     * @param band
     *            the band to use
     * @throws IllegalArgumentException
     *             if the band is null
     * @throws UnsupportedOperationException
     *             if the band is not supported by the hardware
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IOException
     *             if the FM hardware failed to start
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     * @see FmBand
     */
    public void startAsync(FmBand band) throws IOException {
        if (band == null) {
            throw new IllegalArgumentException("Band cannot be null");
        }
        try {
            mService.startAsync(band);
            mBand = band;
        } catch (RemoteException ex) {
            Log.e(TAG, "startAsync: RemoteException", ex);
        }
    }

    /**
     * Starts reception of the FM hardware. This is a synchronous method and the
     * method call will block until the hardware is started.
     * <p>
     * When calling this method, an FmBand parameter must be passed that
     * describes the properties of the band that the FmTransmitter should
     * prepare for. If the band is null, invalid or not supported, an exception
     * will be thrown.
     * </p>
     * <p>
     * If the FmReceiver is active it will be forced to reset. See
     * {@link FmReceiver#RESET_TX_IN_USE}.
     * </p>
     *
     * @param band
     *            the band to use
     * @throws IllegalArgumentException
     *             if the band is null
     * @throws UnsupportedOperationException
     *             if the band is not supported by the hardware
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IOException
     *             if the FM hardware failed to start
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     * @see FmBand
     */
    public void start(FmBand band) throws IOException {
        if (band == null) {
            throw new IllegalArgumentException("Band cannot be null");
        }
        try {
            mService.start(band);
            mBand = band;
        } catch (RemoteException ex) {
            Log.e(TAG, "start: RemoteException", ex);
        }
    }

    /**
     * Resumes FM transmission.
     * <p>
     * Calling this method when the FmTransmitter is in started state has no
     * affect.
     * </p>
     *
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IOException
     *             if the FM hardware failed to resume
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void resume() throws IOException {
        try {
            mService.resume();
        } catch (RemoteException ex) {
            Log.e(TAG, "resume: RemoteException", ex);
        }
    }

    /**
     * Pauses FM transmission. No signals are sent when the FmTransmitter is
     * paused. Call {@link #resume()} to resume transmission. The hardware
     * should be able to start transmission quickly from the paused state to
     * give a good user experience.
     * <p>
     * Note that the hardware provider may choose to turn off the hardware after
     * being paused a certain amount of time to save power. This will be
     * reported in {@link OnForcedResetListener#onForcedReset(int)} with reason
     * {@link #RESET_NON_CRITICAL} and the FmTransmitter will be set to the idle
     * state.
     * </p>
     * <p>
     * Calling this method when the FmTransmitter is in paused state has no
     * affect.
     * </p>
     *
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IOException
     *             if the FM hardware failed to pause
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void pause() throws IOException {
        try {
            mService.pause();
        } catch (RemoteException ex) {
            Log.e(TAG, "pause: RemoteException", ex);
        }
    }

    /**
     * Resets the FmTransmitter to its idle state.
     *
     * @throws IOException
     *             if the FM hardware failed to reset
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void reset() throws IOException {
        try {
            mService.reset();
            mBand = null;
        } catch (RemoteException ex) {
            Log.e(TAG, "reset: RemoteException", ex);
        }
    }

    /**
     * Returns the state of the FmTransmitter.
     *
     * @return One of {@link #STATE_IDLE}, {@link #STATE_STARTING},
     *         {@link #STATE_STARTED}, {@link #STATE_PAUSED},
     *         {@link #STATE_SCANNING}
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public int getState() {
        try {
            return mService.getState();
        } catch (RemoteException ex) {
            Log.e(TAG, "getState: RemoteException", ex);
            return STATE_IDLE;
        }
    }

    /**
     * Sets the output frequency. The frequency must be within the band that the
     * FmTransmitter prepared for.
     *
     * @param frequency
     *            the output frequency to use in kHz
     * @throws IllegalArgumentException
     *             if the frequency is not supported
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IOException
     *             if the FM hardware failed to set frequency
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void setFrequency(int frequency) throws IOException {
        if (mBand != null && !mBand.isFrequencyValid(frequency)) {
            throw new IllegalArgumentException(
                    "Frequency is not valid in this band.");
        }
        try {
            mService.setFrequency(frequency);
        } catch (RemoteException ex) {
            Log.e(TAG, "setFrequency: RemoteException", ex);
        }
    }

    /**
     * Returns the output frequency.
     *
     * @return the output frequency in kHz
     *
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IOException
     *             if the FM hardware failed to get the frequency
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public int getFrequency() throws IOException {
        try {
            return mService.getFrequency();
        } catch (RemoteException ex) {
            Log.e(TAG, "getFrequency: RemoteException", ex);
            return FmBand.FM_FREQUENCY_UNKNOWN;
        }
    }

    /**
     * Sets the RDS data to transmit. See RDS table in FmReceiver for data that
     * can be set.
     *
     * @param rdsData
     *            the RDS data to transmit, set to null to disable RDS
     *            transmission
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IllegalArgumentException
     *             if the rdsData parameter has invalid syntax
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void setRdsData(Bundle rdsData) {
        try {
            mService.setRdsData(rdsData);
        } catch (RemoteException ex) {
            Log.e(TAG, "setRdsData: RemoteException", ex);
        }
    }

    /**
     * Returns true if the hardware/implementation supports block scan. If true
     * the {@link FmTransmitter#startBlockScan(int, int)} will work.
     * <p>
     * The motivation for having this function is that an application can take
     * this capability into account when laying out its UI.
     * </p>
     *
     * @return true if block scan is supported by the FmTransmitter, false
     *         otherwise
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public boolean isBlockScanSupported() {
        try {
            return mService.isBlockScanSupported();
        } catch (RemoteException ex) {
            Log.e(TAG, "isBlockScanSupported: RemoteException", ex);
            return false;
        }
    }

    /**
     * Starts a block scan. The tuner will scan the frequency band between
     * startFrequency and endFrequency for unused frequencies. The application
     * should register for callbacks using
     * {@link #addOnScanListener(OnScanListener)} to receive a callback when
     * frequencies are found.
     * <p>
     * If the application wants to stop the block scan, a call to
     * {@link #stopScan()} should be made.
     * </p>
     *
     * @param startFrequency
     *            the frequency to start the block scan
     * @param endFrequency
     *            the frequency to end the block scan
     * @throws IllegalStateException
     *             if it is called in an invalid state
     * @throws IllegalArgumentException
     *             if the startFrequency or endFrequency it not within the
     *             currently used FmBand
     * @throws UnsupportedOperationException
     *             if the hardware/implementation does not supports block scan
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void startBlockScan(int startFrequency, int endFrequency) {
        try {
            mService.startBlockScan(startFrequency, endFrequency);
        } catch (RemoteException ex) {
            Log.e(TAG, "startBlockScan: RemoteException", ex);
        }
    }

    /**
     * Stops performing a scan operation. The hardware might continue the scan
     * for an unspecified amount of time after this method is called. Once the
     * scan has stopped, it will be notified via {@link OnScanListener}.
     * <p>
     * Note that this method has no affect if called in other states than the
     * scanning state.
     * </p>
     *
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void stopScan() {
        try {
            mService.stopScan();
        } catch (RemoteException ex) {
            Log.e(TAG, "stopScan: RemoteException", ex);
        }
    }

    /**
     * This method can be used to send vendor specific commands. These commands
     * must not follow any common design for all vendors, and information about
     * the commands that a vendor implements is out of scope in this API.
     * <p>
     * However, one command must be supported by all vendors that implements
     * vendor specific commands, the <i>vendor_information</i> command. In the
     * Bundle parameter in
     * {@link OnExtraCommandListener#onExtraCommand(String, Bundle)} the FM
     * radio device name and version can be extracted according to the table
     * below.
     * </p>
     * <table border="1">
     * <tr>
     * <th>key name</th>
     * <th>value type</th>
     * </tr>
     * <tr>
     * <td>device_name</td>
     * <td>string</td>
     * </tr>
     * <tr>
     * <td>device_version</td>
     * <td>string</td>
     * </tr>
     * </table>
     *
     * @param command
     *            the command to send
     * @param extras
     *            extra parameters to the command
     * @return true if the command was accepted, otherwise false
     *
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public boolean sendExtraCommand(String command, String[] extras) {
        try {
            return mService.sendExtraCommand(command, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "sendExtraCommand: RemoteException", ex);
            return false;
        }
    }

    /**
     * Register a callback to be invoked when the FmTransmitter is started.
     *
     * @param listener
     *            the callback that will be run
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void addOnStartedListener(OnStartedListener listener) {
        if (mOnStarted.get(listener) != null) {
            // listener is already registered
            return;
        }
        try {
            synchronized (mOnStarted) {
                OnStartedListenerTransport transport = new OnStartedListenerTransport(listener);
                mService.addOnStartedListener(transport);
                mOnStarted.put(listener, transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "addOnStartedListener: RemoteException", ex);
        }
    }

    /**
     * Unregister a callback to be invoked when the FmTransmitter is started.
     *
     * @param listener
     *            the callback to remove
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void removeOnStartedListener(OnStartedListener listener) {
        try {
            OnStartedListenerTransport transport = mOnStarted.remove(listener);
            if (transport != null) {
                mService.removeOnStartedListener(transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "removeOnStartedListener: DeadObjectException", ex);
        }
    }

    /**
     * Register a callback to be invoked during a scan.
     *
     * @param listener
     *            the callback that will be run
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void addOnScanListener(OnScanListener listener) {
        if (mOnBlockScan.get(listener) != null) {
            // listener is already registered
            return;
        }
        try {
            synchronized (mOnBlockScan) {
                OnBlockScanListenerTransport transport = new OnBlockScanListenerTransport(listener);
                mService.addOnBlockScanListener(transport);
                mOnBlockScan.put(listener, transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "addOnScanListener: RemoteException", ex);
        }
    }

    /**
     * Unregister a callback to be invoked during a scan.
     *
     * @param listener
     *            the callback to remove
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void removeOnScanListener(OnScanListener listener) {
        try {
            OnBlockScanListenerTransport transport = mOnBlockScan.remove(listener);
            if (transport != null) {
                mService.removeOnBlockScanListener(transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "removeOnScanListener: DeadObjectException", ex);
        }
    }

    /**
     * Register a callback to be invoked when an error has happened during an
     * asynchronous operation.
     *
     * @param listener
     *            the callback that will be run
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void addOnErrorListener(OnErrorListener listener) {
        if (mOnError.get(listener) != null) {
            // listener is already registered
            return;
        }
        try {
            synchronized (mOnError) {
                OnErrorListenerTransport transport = new OnErrorListenerTransport(listener);
                mService.addOnErrorListener(transport);
                mOnError.put(listener, transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "addOnErrorListener: RemoteException", ex);
        }
    }

    /**
     * Unregister a callback to be invoked when an error has happened during an
     * asynchronous operation.
     *
     * @param listener
     *            the callback to remove
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void removeOnErrorListener(OnErrorListener listener) {
        try {
            OnErrorListenerTransport transport = mOnError.remove(listener);
            if (transport != null) {
                mService.removeOnErrorListener(transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "removeOnErrorListener: DeadObjectException", ex);
        }
    }

    /**
     * Register a callback to be invoked when the FmTransmitter is forced to
     * pause due to external reasons.
     *
     * @param listener
     *            the callback that will be run
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void addOnForcedPauseListener(OnForcedPauseListener listener) {
        if (mOnForcedPause.get(listener) != null) {
            // listener is already registered
            return;
        }
        try {
            synchronized (mOnForcedPause) {
                OnForcedPauseListenerTransport transport = new OnForcedPauseListenerTransport(
                        listener);
                mService.addOnForcedPauseListener(transport);
                mOnForcedPause.put(listener, transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "addOnForcedPauseListener: RemoteException", ex);
        }
    }

    /**
     * Unregister a callback to be invoked when the FmTransmitter is forced to
     * pause due to external reasons.
     *
     * @param listener
     *            the callback to remove
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void removeOnForcedPauseListener(OnForcedPauseListener listener) {
        try {
            OnForcedPauseListenerTransport transport = mOnForcedPause.remove(listener);
            if (transport != null) {
                mService.removeOnForcedPauseListener(transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "removeOnForcedPauseListener: DeadObjectException", ex);
        }
    }

    /**
     * Register a callback to be invoked when the FmTransmitter is forced to
     * reset due to external reasons.
     *
     * @param listener
     *            the callback that will be run
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void addOnForcedResetListener(OnForcedResetListener listener) {
        if (mOnForcedReset.get(listener) != null) {
            // listener is already registered
            return;
        }
        try {
            synchronized (mOnForcedReset) {
                OnForcedResetListenerTransport transport = new OnForcedResetListenerTransport(
                        listener);
                mService.addOnForcedResetListener(transport);
                mOnForcedReset.put(listener, transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "addOnForcedResetListener: RemoteException", ex);
        }
    }

    /**
     * Unregister a callback to be invoked when the FmTransmitter is forced to
     * reset due to external reasons.
     *
     * @param listener
     *            the callback to remove
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void removeOnForcedResetListener(OnForcedResetListener listener) {
        try {
            OnForcedResetListenerTransport transport = mOnForcedReset.remove(listener);
            if (transport != null) {
                mService.removeOnForcedResetListener(transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "removeOnForcedResetListener: DeadObjectException", ex);
        }
    }

    /**
     * Register a callback to be invoked when the FmTransmitter changes state.
     * Having a listener registered to this method may cause frequent callbacks,
     * hence it is good practice to only have a listener registered for this
     * when necessary.
     *
     * @param listener
     *            the callback that will be run
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void addOnStateChangedListener(OnStateChangedListener listener) {
        if (mOnStateChanged.get(listener) != null) {
            // listener is already registered
            return;
        }
        try {
            synchronized (mOnStateChanged) {
                OnStateChangedListenerTransport transport = new OnStateChangedListenerTransport(
                        listener);
                mService.addOnStateChangedListener(transport);
                mOnStateChanged.put(listener, transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "addOnStateChangedListener: RemoteException", ex);
        }
    }

    /**
     * Unregister a callback to be invoked when the FmTransmitter changes state.
     *
     * @param listener
     *            the callback to remove
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void removeOnStateChangedListener(OnStateChangedListener listener) {
        try {
            OnStateChangedListenerTransport transport = mOnStateChanged.remove(listener);
            if (transport != null) {
                mService.removeOnStateChangedListener(transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "removeOnStateChangedListener: DeadObjectException", ex);
        }
    }

    /**
     * Register a callback to be invoked when the FmTransmitter want's to invoke
     * a vendor specific callback.
     *
     * @param listener
     *            the callback that will be run
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void addOnExtraCommandListener(OnExtraCommandListener listener) {
        if (mOnExtraCommand.get(listener) != null) {
            // listener is already registered
            return;
        }
        try {
            synchronized (mOnExtraCommand) {
                OnExtraCommandListenerTransport transport = new OnExtraCommandListenerTransport(
                        listener);
                mService.addOnExtraCommandListener(transport);
                mOnExtraCommand.put(listener, transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "addOnExtraCommandListener: RemoteException", ex);
        }
    }

    /**
     * Unregister a callback to be invoked when the FmTransmitter want's to
     * invoke a vendor specific callback.
     *
     * @param listener
     *            the callback to remove
     * @throws SecurityException
     *             if the FM_RADIO_TRANSMITTER permission is not present
     */
    public void removeOnExtraCommandListener(OnExtraCommandListener listener) {
        try {
            OnExtraCommandListenerTransport transport = mOnExtraCommand.remove(listener);
            if (transport != null) {
                mService.removeOnExtraCommandListener(transport);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "removeOnExtraCommandListener: DeadObjectException", ex);
        }
    }

    /**
     * Interface definition of a callback to be invoked when the FmTransmitter
     * is started.
     */
    public interface OnStartedListener {
        /**
         * Called when the FmTransmitter is started. The FmTransmitter is now
         * transmitting FM radio.
         */
        void onStarted();
    }

    /**
     * Interface definition of a callback to be invoked when a scan operation is
     * complete.
     */
    public interface OnScanListener {
        /**
         * Called when the block scan is completed.
         * <p>
         * If the block scan is aborted with stopScan, this will be indicated
         * with the aborted argument.
         * <p>
         * If an error occurs during a block scan, it will be reported via
         * {@link OnErrorListener#onError()} and this method callback will not
         * be invoked.
         * </p>
         *
         * @param frequency
         *            the frequency in kHz where the channel was found
         * @param signalStrength
         *            the signal strength, 0-1000
         * @param aborted
         *            true if the block scan was aborted, false otherwise
         */
        void onBlockScan(int[] frequency, int[] signalStrength, boolean aborted);
    }

    /**
     * Interface definition of a callback to be invoked when there has been an
     * error during an asynchronous operation.
     */
    public interface OnErrorListener {
        /**
         * Called to indicate an error.
         */
        boolean onError();
    }

    /**
     * Interface definition of a callback to be invoked when the FmTransmitter
     * was forced to pause due to external reasons.
     */
    public interface OnForcedPauseListener {
        /**
         * Called when an external reason caused the FmTransmitter to pause.
         * When this callback is received, the FmTransmitter is still able to
         * resume transmission by calling {@link FmTransmitter#resume()}.
         */
        void onForcedPause();
    }

    /**
     * Interface definition of a callback to be invoked when the FmTransmitter
     * was forced to reset due to external reasons.
     */
    public interface OnForcedResetListener {
        /**
         * Called when an external reason caused the FmTransmitter to reset. The
         * application that uses the FmTransmitter should take action according
         * to the reason for resetting.
         *
         * @param reason
         *            reason why the FmTransmitter reset:
         *            <ul>
         *            <li>{@link FmTransmitter#RESET_NON_CRITICAL}
         *            <li>{@link FmTransmitter#RESET_CRITICAL}
         *            <li>{@link FmTransmitter#RESET_RX_IN_USE}
         *            <li>{@link FmTransmitter#RESET_RADIO_FORBIDDEN}
         *            </ul>
         */
        void onForcedReset(int reason);
    }

    /**
     * Interface definition of a callback to be invoked when the FmTransmitter
     * changes state.
     */
    public interface OnStateChangedListener {
        /**
         * Called when the state is changed in the FmTransmitter. This is useful
         * if an application want's to monitor the FmTransmitter state.
         *
         * @param oldState
         *            the old state of the FmTransmitter
         * @param newState
         *            the new state of the FmTransmitter
         */
        void onStateChanged(int oldState, int newState);
    }

    /**
     * Interface definition of a callback to be invoked when the FmTransmitter
     * responds to a vendor specific command.
     */
    public interface OnExtraCommandListener {
        /**
         * Called when the FmTransmitter responds to a vendor specific command.
         *
         * @param response
         *            the command the FmTransmitter responds to
         * @param extras
         *            extra parameters to the command
         */
        void onExtraCommand(String response, Bundle extras);
    }
}
