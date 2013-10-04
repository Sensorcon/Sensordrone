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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;

import com.sensorcon.sensordrone.CoreDrone;


/**
 * This is an interface that can perform a repeatable task at a designated interval, whose
 * start/stop can be controlled.
 * (Basically an on/off repeating runnable)
 */
public abstract class DroneStreamer implements Runnable {

    ExecutorService streamService;
    Handler streamHandler;
    protected CoreDrone myDrone;
    protected Boolean OnOff = false;
    public boolean isRunning;
    int rate;

    /**
     * Our default Constructor
     * @param aDrone
     * @param streamRate
     */
    public DroneStreamer(CoreDrone aDrone, int streamRate) {
        myDrone = aDrone;
        rate = streamRate;
        isRunning = false;
        streamHandler = new Handler();
    }

    /**
     * A method to set the frequency that the repeatableTask is performed at.
     * @param streamRate Rate in milliseconds
     */
    public void setRate(int streamRate) {
        rate = streamRate;
    }

    /**
     * Start the repeatableTask at the designated streamRate
     */
    public void start() {
        this.OnOff = true;
        this.isRunning = true;
        streamService = Executors.newFixedThreadPool(1);
        streamService.execute(this);
    }

    /**
     * Stop the repeatableTask from repeating
     */
    public void stop() {
        this.OnOff = false;
        this.isRunning = false;
        // If we call stop before start, this could be null
        if (streamService != null) {
            streamService.shutdown();
        }
    }

    @Override
    public void run() {
        if (!myDrone.isConnected) {
            return;
        }
        if (streamService.isShutdown()) {
            return;
        }

        if (OnOff) {
            // Run the code
            repeatableTask();
            // Set it up to run the code again
            streamHandler.postDelayed(this, rate);
        } else {
            // Don't repeat
            return;
        }

    }

    /**
     * The code you want to repeatably run at the designated streamRate
     */
    public abstract void repeatableTask();

}
