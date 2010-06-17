/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.bluetooth;

import android.content.Context;
import android.content.Intent;

import android.bluetooth.AtCommandHandler;
import android.bluetooth.AtCommandResult;

import android.util.Log;

import java.util.ArrayList;

/**
 * Handle an AT+XEVENT= command.  Broadcast an
 * android.bluetooth.device.action.HEADSET_XEVENT event with EXTRA_DEVICE and
 * EXTRA_XEVENT_ARGS extras.
 * @hide
 */
public class EventAtCommandHandler extends AtCommandHandler {

    private static final String TAG = "EventAtCommandHandler";

    private Context mContext;

    private BluetoothDevice mDevice;

    public EventAtCommandHandler(Context context, BluetoothDevice device) {
	mContext = context;
	mDevice = device;
    }

    /**
     * Handle Set command "AT+XEVENT=...".<p>
     * AT+XEVENT=<arg1>,[<arg2>,]
     * The first argument is a string denoting the event.  The subsequent
     * arguments depend on the first.  We broadcast an
     * android.bluetooth.device.action.HEADSET_XEVENT event with EXTRA_DEVICE
     * and EXTRA_XEVENT_ARGS extras.  EXTRA_XEVENT_ARGS contains all of the
     * arguments of the command, as a parsed array.
     *
     * @param args an array of objects, the first being a string.
     * @return     The result of this command.
     */
    @Override
    public AtCommandResult handleSetCommand(Object[] args) {
	Intent broadcastIntent =
	    new Intent(BluetoothDevice.ACTION_HEADSET_XEVENT);
	// convert the array of Strings and Integers to only Strings
	ArrayList<String> stringArgs = new ArrayList<String>(args.length);
	for (Object arg : args) {
	    stringArgs.add(arg.toString());
	}
	broadcastIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
	broadcastIntent.putExtra(BluetoothDevice.EXTRA_XEVENT_ARGS,
				 stringArgs);
	mContext.sendBroadcast(broadcastIntent);
	
	return new AtCommandResult(AtCommandResult.OK);
    }
}
