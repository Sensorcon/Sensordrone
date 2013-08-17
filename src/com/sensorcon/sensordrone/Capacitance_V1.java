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
package com.sensorcon.sensordrone;

import java.util.concurrent.RejectedExecutionException;

/**
 * A class to interact with the Sensordrone's proximity capacitance electrode
 */
public class Capacitance_V1 extends DroneSensor {

    /*
     IMPORTANT:
     This sensor has a "memory". Settings changed are remembered: even on reboot/power down/etc.
     If you change something and don't reset it, it will drive you mad while wondering why another
     features isn't working correctly :-)

    The electrode used for the capacitance sensor can be operated in single ended mode, or differential mode,
    within certain ranges ( PF_4_00 represents a 0-4 pF range).
    Imagine the capacitance electrode as having 3 leads:

    ##### (Positive Lead)
    ##### (Ground Lead)
    ##### (Negative Lead)

    Single ended mode Looks at the difference between Ground and Positive, ignoring the Negative.

    Differential mode looks at the difference between (Ground and Positive) and (Ground and Negative).

    We've found the single ended mode to operate the best.
    The electrodes have a baseline capacitance of a little over 2pF, so we operate it at
    the 4pF range (or else it will just rail at max range for any of the lower ones).

     */

    // Single Ended Mode
//	private static final byte PF_0_50 = (byte) 0x40;
//	private static final byte PF_1_00 = (byte) 0x80;
//	private static final byte PF_2_00 = 0x00;
    private static final byte PF_4_00 = (byte) 0xc0;

    // Diff Mode
//	private static final byte DF_0_25 = (byte) 0x60;
//	private static final byte DF_0_50 = (byte) 0xa0;
//	private static final byte DF_1_00 = 0x20;
//	private static final byte DF_2_00 = (byte) 0xe0;


    // Used for notifications
    private DroneEventObject measured = new DroneEventObject(DroneEventObject.droneEventType.CAPCACITANCE_MEASURED);
    private DroneEventObject enabled = new DroneEventObject(DroneEventObject.droneEventType.CAPACITANCE_ENABLED);
    private DroneEventObject disabled = new DroneEventObject(DroneEventObject.droneEventType.CAPACITANCE_DISABLED);
    private DroneEventObject status = new DroneEventObject(DroneEventObject.droneEventType.CAPACITANCE_STATUS_CHECKED);


    /**
     * Enable the sensor
     * @return
     */
    public boolean enable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableRunnable = new Runnable() {

            @Override
            public void run() {


                byte[] enable = {0x50, 0x07, 0x11, 0x01, 0x48, 0x01, (byte) 0x0f, 0x11, 0x00};
                byte[] offset = {0x50, 0x08, 0x11, 0x01, 0x48, 0x01, 0x05, (byte) 0x30, 0x00, 0x00};
                byte[] range = {0x50, 0x07, 0x11, 0x01, 0x48, 0x01, 0x0b, PF_4_00, 0x00};

                // I always reset the offset, just in case it got changed :-D
                sdCallAndResponse(offset);
                // Set out mode/range
                sdCallAndResponse(range);

                // Enable
                byte[] lastCall = sdCallAndResponse(enable);

                if (lastCall != null) {
                    // change the status and notify the listener
                    myDrone.capacitanceStatus = true;
                    myDrone.notifyDroneEventHandler(enabled);
                    myDrone.notifyDroneStatusListener(enabled);

                }
            }
        };

        try {
            // submit the enable runnable
            myDrone.commService.submit(enableRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Disable the sensor
     * @return
     */
    public boolean disable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] call = {0x50, 0x07, 0x11,
                        0x01, 0x48, 0x01, 0x0f, 0x00,
                        0x00};
                byte[] lastCall = sdCallAndResponse(call);

                if (lastCall != null) {
                    // Update the status and notify the listener.
                    myDrone.capacitanceStatus = false;
                    myDrone.notifyDroneEventHandler(disabled);
                    myDrone.notifyDroneStatusListener(disabled);
                }
            }
        };

        try {
            myDrone.commService.submit(disableRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Checks the on/off status of the sensor
     * @return
     */
    public boolean status() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable statusRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] call = {0x50, 0x06, 0x10,
                        0x01, 0x48, 0x00, 0x10,
                        0x00};
                byte[] statusByte = sdCallAndResponse(call);
                if (statusByte != null) {
                    byte statusCheck = (byte) (0x00000080 & statusByte[0]);
                    if (statusCheck == 0x80) {
                        myDrone.capacitanceStatus = true;
                    } else {
                        myDrone.capacitanceStatus = false;
                    }
                    // Notify the listener
                    myDrone.notifyDroneEventHandler(status);
                    myDrone.notifyDroneStatusListener(status);
                }
            }
        };

        try {
            myDrone.commService.submit(statusRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Takes a measurement from the senosor
     * @return
     */
    public boolean measure() {
        if (!myDrone.isConnected || !myDrone.capacitanceStatus) {
            return false;
        }

        Runnable measureRunnable = new Runnable() {
            public void run() {
                byte[] call = {0x50, 0x06, 0x10, 0x01, 0x48, 0x00, 0x03, 0x00};
                // Number of bytes expected to be returned
                byte[] response = sdCallAndResponse(call);

                if (response != null) {
                    // Parse the data
                    int MSB = 0x000000ff & ((int) response[1]);
                    int LSB = 0x000000ff & ((int) response[2]);
                    int ADC = (MSB << 8) + LSB;
                    logger.debugLogger(TAG, "ADC: " + String.valueOf(ADC), myDrone.DEBUG);
                    // *4000 is nF
                    myDrone.capacitance_femtoFarad = (float) (((float) ADC / 65520.0) * 4000);
                    // Notify the Listener that we've updated the values
                    myDrone.notifyDroneEventHandler(measured);
                    myDrone.notifyDroneEventListener(measured);
                }
            }
        };

        try {
            myDrone.commService.submit(measureRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Our default constructor
     * @param drone
     */
    public Capacitance_V1(CoreDrone drone) {
        super(drone, "Capacitance_V1");
    }

}
