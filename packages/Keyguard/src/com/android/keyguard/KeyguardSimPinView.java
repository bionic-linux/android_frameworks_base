/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.IccCardConstants;

import android.content.Context;
import android.content.DialogInterface;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.View;
import android.util.Log;
import android.view.WindowManager;
import android.telephony.SubscriptionController;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Button;


/**
 * Displays a PIN pad for unlocking.
 */
public class KeyguardSimPinView extends KeyguardAbsKeyInputView
        implements KeyguardSecurityView, OnEditorActionListener, TextWatcher {
    private static final String LOG_TAG = "KeyguardSimPinView";
    private static final boolean DEBUG = KeyguardViewMediator.DEBUG;
    public static final String TAG = "KeyguardSimPinView";

    private ProgressDialog mSimUnlockProgressDialog = null;
    private CheckSimPin mCheckSimPinThread;

    private AlertDialog mRemainingAttemptsDialog;
    KeyguardUpdateMonitor mUpdateMonitor;
    KeyguardUtils mKeyguardUtils;
    private int mSubIdx = -1;
    private TextView mSIMCardName = null;

    private KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onSubInfoContentChanged(long subId, String column, 
                                String sValue, int iValue) {
            if (column != null && column.equals(SubscriptionController.DISPLAY_NAME)
                && mSubIdx == mUpdateMonitor.getSubIdxBySubId(subId)) {
                    dealwithSIMInfoChanged();
            }
        }

        @Override
        public void onSimStateChanged(IccCardConstants.State simState, int subIdx) {
            if (DEBUG) Log.d(TAG, "onSimStateChanged: " + simState + ", subIdx=" + subIdx);

            switch (simState) {
                case NOT_READY:
                case ABSENT:
                    if (subIdx == mSubIdx) {
                        mUpdateMonitor.reportSimUnlocked(mSubIdx);
                        mCallback.dismiss(true);
                    }
                    break;
            }
        }
    };

    public KeyguardSimPinView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        mKeyguardUtils = new KeyguardUtils(context);
     }

    public void resetState() {
        mSecurityMessageDisplay.setMessage(R.string.kg_sim_pin_instructions, true);
        mPasswordEntry.setEnabled(true);
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;

        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R.string.kg_password_wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = getContext().getResources()
                    .getQuantityString(R.plurals.kg_password_wrong_pin_code, attemptsRemaining,
                            attemptsRemaining);
        } else {
            displayMessage = getContext().getString(R.string.kg_password_pin_failed);
        }
        if (DEBUG) Log.d(LOG_TAG, "getPinPasswordErrorMessage:"
                + " attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    @Override
    protected boolean shouldLockout(long deadline) {
        // SIM PIN doesn't have a timed lockout
        return false;
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.pinEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSIMCardName = (TextView) findViewById(R.id.sim_card_name);
        mSubIdx = mUpdateMonitor.getSimPinSubIdx();
        if ( mUpdateMonitor.getNumOfSubscription() > 1 ) {
            View simIcon = findViewById(R.id.sim_icon);
            if (simIcon != null) {
                simIcon.setVisibility(View.GONE);
            }
            View simInfoMsg = findViewById(R.id.sim_info_message);
            if (simInfoMsg != null) {
                simInfoMsg.setVisibility(View.VISIBLE);
            }
            dealwithSIMInfoChanged();
        }

        final View ok = findViewById(R.id.key_enter);
        if (ok != null) {
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doHapticKeyClick();
                    verifyPasswordAndUnlock();
                }
            });
        }

        // The delete button is of the PIN keyboard itself in some (e.g. tablet) layouts,
        // not a separate view
        View pinDelete = findViewById(R.id.delete_button);
        if (pinDelete != null) {
            pinDelete.setVisibility(View.VISIBLE);
            pinDelete.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    CharSequence str = mPasswordEntry.getText();
                    if (str.length() > 0) {
                        mPasswordEntry.setText(str.subSequence(0, str.length()-1));
                    }
                    doHapticKeyClick();
                }
            });
            pinDelete.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    mPasswordEntry.setText("");
                    doHapticKeyClick();
                    return true;
                }
            });
        }

        final Button dismissButton = (Button)findViewById(R.id.key_dismiss);
        if (dismissButton != null) {
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doHapticKeyClick();
                    if (mUpdateMonitor.isSimLockDismissable()) {
                        mUpdateMonitor.setSimLockDismissFlag(mSubIdx, true);
                        mUpdateMonitor.reportSimUnlocked(mSubIdx);
                        mCallback.dismiss(true);
                    }
                }
            });
            dismissButton.setText(R.string.kg_dismiss);

            if (mUpdateMonitor.isSimLockDismissable()) {
                dismissButton.setVisibility(View.VISIBLE);
            }
        }

        mPasswordEntry.setKeyListener(DigitsKeyListener.getInstance());
        mPasswordEntry.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        mPasswordEntry.requestFocus();

        mSecurityMessageDisplay.setTimeout(0); // don't show ownerinfo/charging status by default
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mUpdateMonitor.registerCallback(mUpdateCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mUpdateMonitor.removeCallback(mUpdateCallback);
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public void onPause() {
        // dismiss the dialog.
        if (mSimUnlockProgressDialog != null) {
            mSimUnlockProgressDialog.dismiss();
            mSimUnlockProgressDialog = null;
        }
    }

    /**
     * Since the IPC can block, we want to run the request in a separate thread
     * with a callback.
     */
    private abstract class CheckSimPin extends Thread {
        private final String mPin;

        protected CheckSimPin(String pin) {
            mPin = pin;
        }

        abstract void onSimCheckResponse(final int result, final int attemptsRemaining);

        @Override
        public void run() {
            try {
                long subId = mUpdateMonitor.getSubIdBySubIdx(mSubIdx);
                Log.v(TAG, "call supplyPinReportResultUsingSub() subId = " + subId);
                final int[] result = ITelephony.Stub.asInterface(ServiceManager
                        .checkService("phone")).supplyPinReportResultUsingSub(mPin, subId);
                Log.v(TAG, "supplyPinReportResultUsingSub returned: " + result[0] + " " + result[1]);
                post(new Runnable() {
                    public void run() {
                        onSimCheckResponse(result[0], result[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException for supplyPinReportResultUsingSub:", e);
                post(new Runnable() {
                    public void run() {
                        onSimCheckResponse(PhoneConstants.PIN_GENERAL_FAILURE, -1);
                    }
                });
            }
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (mSimUnlockProgressDialog == null) {
            mSimUnlockProgressDialog = new ProgressDialog(mContext);
            mSimUnlockProgressDialog.setMessage(
                    mContext.getString(R.string.kg_sim_unlock_progress_dialog_message));
            mSimUnlockProgressDialog.setIndeterminate(true);
            mSimUnlockProgressDialog.setCancelable(false);
            mSimUnlockProgressDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
        return mSimUnlockProgressDialog;
    }

    private Dialog getSimRemainingAttemptsDialog(int remaining) {
        String msg = getPinPasswordErrorMessage(remaining);
        if (mRemainingAttemptsDialog == null) {
            Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setNeutralButton(R.string.ok, null);
            mRemainingAttemptsDialog = builder.create();
            mRemainingAttemptsDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        } else {
            mRemainingAttemptsDialog.setMessage(msg);
        }
        return mRemainingAttemptsDialog;
    }

    @Override
    protected void verifyPasswordAndUnlock() {
        String entry = mPasswordEntry.getText().toString();

        if (entry.length() < 4) {
            // otherwise, display a message to the user, and don't submit.
            mSecurityMessageDisplay.setMessage(R.string.kg_invalid_sim_pin_hint, true);
            mPasswordEntry.setText("");
            mCallback.userActivity(0);
            return;
        }

        getSimUnlockProgressDialog().show();

        if (mCheckSimPinThread == null) {
            mCheckSimPinThread = new CheckSimPin(mPasswordEntry.getText().toString()) {
                void onSimCheckResponse(final int result, final int attemptsRemaining) {
                    post(new Runnable() {
                        public void run() {
                            if (mSimUnlockProgressDialog != null) {
                                mSimUnlockProgressDialog.hide();
                            }
                            if (result == PhoneConstants.PIN_RESULT_SUCCESS) {
                                mUpdateMonitor.reportSimUnlocked(mSubIdx);
                                mCallback.dismiss(true);
                            } else {
                                if (result == PhoneConstants.PIN_PASSWORD_INCORRECT) {
                                    if (attemptsRemaining <= 2) {
                                        // this is getting critical - show dialog
                                        getSimRemainingAttemptsDialog(attemptsRemaining).show();
                                    } else {
                                        // show message
                                        mSecurityMessageDisplay.setMessage(
                                                getPinPasswordErrorMessage(attemptsRemaining), true);
                                    }
                                } else {
                                    // "PIN operation failed!" - no idea what this was and no way to
                                    // find out. :/
                                    mSecurityMessageDisplay.setMessage(getContext().getString(
                                            R.string.kg_password_pin_failed), true);
                                }
                                if (DEBUG) Log.d(LOG_TAG, "verifyPasswordAndUnlock "
                                        + " CheckSimPin.onSimCheckResponse: " + result
                                        + " attemptsRemaining=" + attemptsRemaining);
                                mPasswordEntry.setText("");
                            }
                            mCallback.userActivity(0);
                            mCheckSimPinThread = null;
                        }
                    });
                }
            };
            mCheckSimPinThread.start();
        }
    }

    private void dealwithSIMInfoChanged() {
        String operName = null;

        try {
           operName = mKeyguardUtils.getOptrNameBySubIdx(mContext, mSubIdx);
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, "getOptrNameBySlot exception, mSubIdx=" + mSubIdx);
        }
        if (DEBUG) Log.i(TAG, "dealwithSIMInfoChanged, mSubIdx="+mSubIdx+", operName="+operName);
        TextView forText = (TextView)findViewById(R.id.for_text);
        if (null == operName) { //this is the new SIM card inserted
            if (DEBUG) Log.d(TAG, "SIM" + mSubIdx + " is first reboot");
            setForTextNewCard(forText, mSubIdx);
            mSIMCardName.setVisibility(View.GONE);
        } else {
            if (DEBUG) Log.d(TAG, "dealwithSIMInfoChanged, we will refresh the SIMinfo");
            forText.setText(mContext.getString(R.string.kg_slot_id,mSubIdx+ 1)+ " ");
            mKeyguardUtils.setOptrBackgroundBySubIdx(mSIMCardName, mSubIdx, mContext, operName);
            mSIMCardName.setVisibility(View.VISIBLE);
        }
    }

    private void setForTextNewCard(TextView forText, int subIdx) {
        StringBuffer forSb = new StringBuffer();

        forSb.append(mContext.getString(R.string.kg_slot_id,subIdx + 1));
        forSb.append(" ");
        forSb.append(mContext.getText(R.string.kg_new_simcard));
        forText.setText(forSb.toString());
    }

}

