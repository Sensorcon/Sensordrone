/*
   Copyright 2013 Sensorcon, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.sensorcon.sensordrone.android;

import android.util.Log;

/**
 * Our Logger class for Android. Displays messages via Log.d()
 */
class ALogger extends com.sensorcon.sensordrone.Logger {


    /**
     * Our default constructor
     */
    public ALogger() {

    }

    /**
     * Log a data packet, prefixed with TX:
     * @param TAG
     * @param data
     * @param debug
     */
    public void txLogger(String TAG, byte[] data, boolean debug) {
        // If debugging isn't enabled, show nothing
        if (!debug) {
            return;
        }
        // Parse through the byte array
        String dataString = "TX:";
        for (int i = 0; i < data.length; i++) {
            dataString += " " + Integer.toHexString(data[i] & 0x000000ff);
        }
        Log.d(TAG, dataString);
        return;
    }

    /**
     * Log a data packet, prefixed with RX:
     * @param TAG
     * @param data
     * @param debug
     */
    public void rxLogger(String TAG, byte[] data, boolean debug) {
        // If debugging isn't enabled, show nothing
        if (!debug) {
            return;
        }
        // Parse through the byte array
        String dataString = "RX:";
        for (int i = 0; i < data.length; i++) {
            dataString += " " + Integer.toHexString(data[i] & 0x000000ff);
        }
        Log.d(TAG, dataString);
        return;
    }


    /**
     * Display calculated values (and other debug messages) which are not provided to the API,
     * easily controlled by a boolean (such as Drone.DEBUG).
     *
     * @param TAG
     * @param msg
     * @param debug
     */
    public void debugLogger(String TAG, String msg, boolean debug) {
        // If debugging isn't enabled, show nothing
        if (!debug) {
            return;
        }
        Log.d(TAG, msg);
        return;
    }


    /**
     * Display useful info to the user over logcat. Uses a boolean as an on/off switch
     * @param TAG
     * @param msg
     * @param display
     */
    public void infoLogger(String TAG, String msg, boolean display) {
        if (!display) {
            return;
        }
        Log.i(TAG, msg);
        return;
    }
}
