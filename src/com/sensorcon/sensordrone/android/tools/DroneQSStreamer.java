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
package com.sensorcon.sensordrone.android.tools;


import android.os.Handler;

import com.sensorcon.sensordrone.CoreDrone;

/**
 * This is an interface that takes a Drone QuickSystem, whose
 * enable/disable can be controlled. This is useful for triggering a measurement, and
 * then on the onMeasure() event, calling it again automatically (delay can be controlled from
 * the object's streamHandler).
 * (Basically an on/off runnable for used with QuickSystem of Drone())
 */
public class DroneQSStreamer implements Runnable {

    public Handler streamHandler;
    private int quickSystemInt;
    private CoreDrone myDrone;
    private Boolean OnOff = false;

    public DroneQSStreamer(CoreDrone aDrone, int qsInt) {
        myDrone = aDrone;
        quickSystemInt = qsInt;
        streamHandler = new Handler();
    }

    @Override
    public void run() {
        // Don't do anything if we're not connected
        if (!myDrone.isConnected) {
            disable();
        }
        // If we're enabled, take a measurement!
        if (OnOff) {
            // Take a measurement
            myDrone.quickMeasure(quickSystemInt);
        }
    }

    public void enable() {
        if (quickSystemInt >= 0 && quickSystemInt <= 10) {
            OnOff = true;
        }
    }

    public void disable() {
        OnOff = false;
        streamHandler.removeCallbacksAndMessages(null);
    }


}
