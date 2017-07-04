//<MStar Software>
//******************************************************************************
// MStar Software
// Copyright (c) 2010 - 2015 MStar Semiconductor, Inc. All rights reserved.
// All software, firmware and related documentation herein ("MStar Software") are
// intellectual property of MStar Semiconductor, Inc. ("MStar") and protected by
// law, including, but not limited to, copyright law and international treaties.
// Any use, modification, reproduction, retransmission, or republication of all
// or part of MStar Software is expressly prohibited, unless prior written
// permission has been granted by MStar.
//
// By accessing, browsing and/or using MStar Software, you acknowledge that you
// have read, understood, and agree, to be bound by below terms ("Terms") and to
// comply with all applicable laws and regulations:
//
// 1. MStar shall retain any and all right, ownership and interest to MStar
//    Software and any modification/derivatives thereof.
//    No right, ownership, or interest to MStar Software and any
//    modification/derivatives thereof is transferred to you under Terms.
//
// 2. You understand that MStar Software might include, incorporate or be
//    supplied together with third party's software and the use of MStar
//    Software may require additional licenses from third parties.
//    Therefore, you hereby agree it is your sole responsibility to separately
//    obtain any and all third party right and license necessary for your use of
//    such third party's software.
//
// 3. MStar Software and any modification/derivatives thereof shall be deemed as
//    MStar's confidential information and you agree to keep MStar's
//    confidential information in strictest confidence and not disclose to any
//    third party.
//
// 4. MStar Software is provided on an "AS IS" basis without warranties of any
//    kind. Any warranties are hereby expressly disclaimed by MStar, including
//    without limitation, any warranties of merchantability, non-infringement of
//    intellectual property rights, fitness for a particular purpose, error free
//    and in conformity with any international standard.  You agree to waive any
//    claim against MStar for any loss, damage, cost or expense that you may
//    incur related to your use of MStar Software.
//    In no event shall MStar be liable for any direct, indirect, incidental or
//    consequential damages, including without limitation, lost of profit or
//    revenues, lost or damage of data, and unauthorized system use.
//    You agree that this Section 4 shall still apply without being affected
//    even if MStar Software has been modified by MStar in accordance with your
//    request or instruction for your use, except otherwise agreed by both
//    parties in writing.
//
// 5. If requested, MStar may from time to time provide technical supports or
//    services in relation with MStar Software to you for your use of
//    MStar Software in conjunction with your or your customer's product
//    ("Services").
//    You understand and agree that, except otherwise agreed by both parties in
//    writing, Services are provided on an "AS IS" basis and the warranty
//    disclaimer set forth in Section 4 above shall apply.
//
// 6. Nothing contained herein shall be construed as by implication, estoppels
//    or otherwise:
//    (a) conferring any license or right to use MStar name, trademark, service
//        mark, symbol or any other identification;
//    (b) obligating MStar or any of its affiliates to furnish any person,
//        including without limitation, you and your customers, any assistance
//        of any kind whatsoever, or any information; or
//    (c) conferring any license or right under any intellectual property right.
//
// 7. These terms shall be governed by and construed in accordance with the laws
//    of Taiwan, R.O.C., excluding its conflict of law rules.
//    Any and all dispute arising out hereof or related hereto shall be finally
//    settled by arbitration referred to the Chinese Arbitration Association,
//    Taipei in accordance with the ROC Arbitration Law and the Arbitration
//    Rules of the Association by three (3) arbitrators appointed in accordance
//    with the said Rules.
//    The place of arbitration shall be in Taipei, Taiwan and the language shall
//    be English.
//    The arbitration award shall be final and binding to both parties.
//
//******************************************************************************
//<MStar Software>


package com.android.server.hdmi;
import com.android.internal.util.Preconditions;
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
    interface RequestSADCallback {
        void updateSAD(String keyValuePairs, int key, int value);
    }

    // State in which the action sent <Request Short Audio Descriptor> and
    // is waiting for time out. If it receives <Feature Abort> within timeout.
    private static final int STATE_WAITING_TIMEOUT = 1;

    private final boolean mEnabled;
    private final int mAvrAddress;
    private final int mAvrPort;
    private static byte[] paramsBackup;
    private final int SAD_LEN_MAX = 12;
    private final int SAD_LEN = 3;   //length of short audio descriptor is 3 bytes
    private final RequestSADCallback mCallback;

    /**
     * @Constructor
     *
     * @param source {@link HdmiCecLocalDevice} instance
     * @param enabled whether to reset HDMI port supported AVR audio codecs.
     */
    RequestShortAudioDescriptorAction(HdmiCecLocalDevice source, int avrAddress,
            int avrPort, boolean enabled, RequestSADCallback callback) {
        super(source);
        HdmiUtils.verifyAddressType(getSourceAddress(), HdmiDeviceInfo.DEVICE_TV);
        HdmiUtils.verifyAddressType(avrAddress, HdmiDeviceInfo.DEVICE_AUDIO_SYSTEM);
        mAvrAddress = avrAddress;
        mEnabled = enabled;
        mAvrPort = avrPort;
        mCallback = Preconditions.checkNotNull(callback);
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
        params[0] = (byte) Constants.MSAPI_CODEC_DD;
        params[1] = (byte) Constants.MSAPI_CODEC_AAC;
        params[2] = (byte) Constants.MSAPI_CODEC_DTS;
        params[3] = (byte) Constants.MSAPI_CODEC_DDP;

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
        mCallback.updateSAD(keyValuePairs, Constants.OPTION_CEC_SUPPORT_MULTICHANNELS, Constants.DISABLED);
    }

    public static void removeAudioFormat() {
        HdmiLogger.debug("Remove audio format.");
        paramsBackup = null;
    }

    private int supportMultiChannels() {
        byte codec = Constants.MSAPI_CODEC_NONE;
        byte channels = 0;
        for (int index = 0; index < paramsBackup.length; index += SAD_LEN) {
            // bit 6~3: Audio Format Code
            codec = (byte) ((paramsBackup[index] & 0x78) >> 3); //enAudioFormatCode
            // bit 2~0: Max number of channels -1
            channels = (byte) (paramsBackup[index] & 0x07);
            if ((codec == Constants.MSAPI_CODEC_DDP) || (codec == Constants.MSAPI_CODEC_DD)) {
                if (channels >= 5) {
                    return Constants.ENABLED;
                }
            }
        }
        return Constants.DISABLED;
    }

    private void setAudioFormat() {
        byte[] buffer = new byte[2];
        String audioParameter = "set_ARC_format=";
        String keyValuePairs;

        buffer[0] = (byte) (paramsBackup.length);
        buffer[1] = (byte) (mAvrPort);
        keyValuePairs = audioParameter + Arrays.toString(buffer);
        keyValuePairs += Arrays.toString(paramsBackup);
        HdmiLogger.debug("keyValuePairs:"+keyValuePairs);
        mCallback.updateSAD(keyValuePairs, Constants.OPTION_CEC_SUPPORT_MULTICHANNELS, supportMultiChannels());
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
            if ((params[0] & 0xFF) == Constants.MSAPI_CODEC_NONE) {
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
