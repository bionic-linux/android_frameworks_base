
package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.provider.Settings;
import android.util.Slog;
import android.content.Context;
import java.util.Arrays;


/**
 * Feature action that handles Request Short Audio Description.
 * To detect AVR device supported audio codec.
 */
final class RequestShortAudioDescriptorAction extends HdmiCecFeatureAction {
    private static final String TAG = "RequestShortAudioDescriptor";

    // State in which the action sent <Request Short Audio Descriptor> and
    // is waiting for time out. If it receives <Feature Abort> within timeout.
    private static final int STATE_WAITING_TIMEOUT = 1;

    private final boolean mEnabled;
    private final int mAvrAddress;
    private final int mAvrPort;
    private static byte[] paramsBackup;

    private final int SAD_LEN_MAX = 12;

    /// Audio Digital Output Codec type
    private final int MSAPI_CODEC_NONE = 0x0;
    private final int MSAPI_CODEC_LPCM = 0x1;       ///< Support LPCM
    private final int MSAPI_CODEC_DD = 0x2;         ///< Support DD
    private final int MSAPI_CODEC_MPEG1 = 0x3;      ///< Support MPEG1
    private final int MSAPI_CODEC_MP3 = 0x4;        ///< Support MP3
    private final int MSAPI_CODEC_MPEG2 = 0x5;      ///< Support MPEG2
    private final int MSAPI_CODEC_AAC = 0x6;        ///< Support AAC
    private final int MSAPI_CODEC_DTS = 0x7;        ///< Support DTS
    private final int MSAPI_CODEC_ATRAC = 0x8;      ///< Support ATRAC
    private final int MSAPI_CODEC_ONEBITAUDIO = 0x9;///< Support One-Bit Audio
    private final int MSAPI_CODEC_DDP = 0xA;        ///< Support DDP
    private final int MSAPI_CODEC_DTSHD = 0xB;      ///< Support DTSHD
    private final int MSAPI_CODEC_TRUEHD = 0xC;     ///< Support MLP/TRUE-HD
    private final int MSAPI_CODEC_DST = 0xD;        ///< Support DST
    private final int MSAPI_CODEC_WMAPRO = 0xE;     ///< Support WMA-Pro
    private final int MSAPI_CODEC_MAX = 0xF;

    /**
     * @Constructor
     *
     * @param source {@link HdmiCecLocalDevice} instance
     * @param enabled whether to reset HDMI port supported AVR audio codecs.
     */
    RequestShortAudioDescriptorAction(HdmiCecLocalDevice source, int avrAddress,
            int avrPort, boolean enabled) {
        super(source);
        HdmiUtils.verifyAddressType(getSourceAddress(), HdmiDeviceInfo.DEVICE_TV);
        HdmiUtils.verifyAddressType(avrAddress, HdmiDeviceInfo.DEVICE_AUDIO_SYSTEM);
        mAvrAddress = avrAddress;
        mEnabled = enabled;
        mAvrPort = avrPort;
    }

    @Override
    boolean start() {
        if (mEnabled) {
            mState = STATE_WAITING_TIMEOUT;
            addTimer(mState, HdmiConfig.TIMEOUT_MS);
            if (paramsBackup != null) {
                HdmiLogger.debug("Set old audio format");
                setAudioFormat();
            } else {
                HdmiLogger.debug("No old audio format. Send a command to reqeust.");
                sendRequestShortAudioDescriptor();
            }
        } else {
            resetShortAudioDescriptor();
            finish();
        }
        return true;
    }

    private void sendRequestShortAudioDescriptor() {
        byte[] params = new byte[4];
        params[0] = (byte) MSAPI_CODEC_DD;
        params[1] = (byte) MSAPI_CODEC_AAC;
        params[2] = (byte) MSAPI_CODEC_DTS;
        params[3] = (byte) MSAPI_CODEC_DDP;

        HdmiCecMessage command =
                HdmiCecMessageBuilder.buildRequestShortAudioDescriptor(getSourceAddress(),
                    mAvrAddress, params);
        sendCommand(command, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                switch (error) {
                    case Constants.SEND_RESULT_SUCCESS:
                    case Constants.SEND_RESULT_BUSY:
                    case Constants.SEND_RESULT_FAILURE:
                        //Ignores it silently.
                        break;
                    case Constants.SEND_RESULT_NAK:
                        HdmiLogger.debug("Failed to send <Request Short Audio Descriptor>.");
                        finish();
                        break;
                }
            }
        });
    }

    private void resetShortAudioDescriptor() {
        String audioParameter = "set_ARC_format=";
        String keyValuePairs;
        byte[] buffer = new byte[2];
        buffer[0] = (byte) 0x00;
        buffer[1] = (byte) mAvrPort;
        keyValuePairs = audioParameter + Arrays.toString(buffer);
        tv().setAudioParameters(keyValuePairs);
    }

    public static void removeAudioFormat() {
        HdmiLogger.debug("Remove audio format.");
        paramsBackup = null;
    }
    private void setAudioFormat() {
        byte[] buffer = new byte[paramsBackup.length+2];
        String audioParameter = "set_ARC_format=";
        String keyValuePairs;

        buffer[0] = (byte) (paramsBackup.length);
        buffer[1] = (byte) (mAvrPort);
        for (int index = 0; index < paramsBackup.length; index++) {
            buffer[index+2] = paramsBackup[index];
        }
        keyValuePairs = audioParameter + Arrays.toString(buffer);
        tv().setAudioParameters(keyValuePairs);
        finish();
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        if (mState != STATE_WAITING_TIMEOUT) {
            return false;
        }

        int opcode = cmd.getOpcode();
        byte[] params = cmd.getParams();
        if (opcode == Constants.MESSAGE_FEATURE_ABORT) {
            int originalOpcode = cmd.getParams()[0] & 0xFF;
            if (originalOpcode == Constants.MESSAGE_REQUEST_SHORT_AUDIO_DESCRIPTOR) {
                HdmiLogger.debug("Feature aborted for <Request Short Audio Descriptor>");
                finish();
                return true;
            }
        } else if (opcode == Constants.MESSAGE_REPORT_SHORT_AUDIO_DESCRIPTOR) {
            HdmiLogger.debug("ProcessCommand: <Report Short Audio Descriptor>");
            HdmiLogger.debug("length:"+params.length);
            if ((params.length == 0)||(params.length > SAD_LEN_MAX))
            {
                finish();
                return false;
            }
            if ((params[0] & 0xFF) == MSAPI_CODEC_NONE) {
                resetShortAudioDescriptor();
                finish();
                return true;
            }

            paramsBackup = new byte[params.length];
            paramsBackup = Arrays.copyOf(params, params.length);
            setAudioFormat();
            return true;
        }
        return false;
    }

    @Override
    void handleTimerEvent(int state) {
        if (mState != state || mState != STATE_WAITING_TIMEOUT) {
            return;
        }
        // Expire timeout for <Feature Abort>.
        HdmiLogger.debug("[T]RequestShortAudioDescriptorAction.");
        finish();
    }
}
