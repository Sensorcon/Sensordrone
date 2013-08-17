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
 * A class to interact with the RGBC Sensor
 */
public class RGBC_V1 extends DroneSensor {

    /**
     * Used to notify that a measurement is made.
     */
    private DroneEventObject measured = new DroneEventObject(DroneEventObject.droneEventType.RGBC_MEASURED);

    /**
     * Used to notify that the sensor has been enabled.
     */
    private DroneEventObject enabled = new DroneEventObject(DroneEventObject.droneEventType.RGBC_ENABLED);

    /**
     * Used to notify that the sensor has been disabled.
     */
    private DroneEventObject disabled = new DroneEventObject(DroneEventObject.droneEventType.RGBC_DISABLED);

    /**
     * Used to notify that the status of the sensor has been checked.
     */
    private DroneEventObject status = new DroneEventObject(DroneEventObject.droneEventType.RGBC_STATUS_CHECKED);

    /**
     * Enable the RBC sensor
     * @return
     */
    public boolean enable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] transistorOn = {0x50, 0x03, 0x35, 0x01, 0x00};
                byte[] powerOn = {0x50, 0x07, 0x11, 0x00, 0x39, 0x01, (byte) 0x80, 0x01, 0x00};
                //byte[] intTime_12 = { 0x50, 0x07, 0x11, 0x00, 0x39, 0x01, (byte) 0x81, 0x00, 0x00 };
                byte[] intTime_100 = {0x50, 0x07, 0x11, 0x00, 0x39, 0x01, (byte) 0x81, 0x01, 0x00};
                //byte[] intTime_400 = { 0x50, 0x07, 0x11, 0x00, 0x39, 0x01, (byte) 0x81, 0x02, 0x00 };
                byte[] initADC = {0x50, 0x07, 0x11, 0x00, 0x39, 0x01, (byte) 0x80, 0x03, 0x00};

                // Default integration time is 12ms.
                // We set it to 100 for response vs. performance
                // The other valid settings are commented out above

                // The RGBC sensor is behind a transistor; it needs to be enabled first
                sdCallAndResponse(transistorOn);
                // Now that the transistor is on, turn the sensor on
                sdCallAndResponse(powerOn);
                // Set the integration time
                sdCallAndResponse(intTime_100);
                byte[] lastCall = sdCallAndResponse(initADC);

                if (lastCall != null) {
                    myDrone.rgbcStatus = true;
                    // Notify that the sensor has been enabled
                    myDrone.notifyDroneEventHandler(enabled);
                    myDrone.notifyDroneStatusListener(enabled);
                }

            }
        };

        try {
            myDrone.commService.submit(enableRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Disable the RGBC sensor
     * @return
     */
    public boolean disable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] transistorOff = {0x50, 0x03, 0x35, 0x00, 0x00};
                byte[] powerOff = {0x50, 0x07, 0x11, 0x00, 0x39, 0x01, (byte) 0x80, 0x00, 0x00};

                // First we turn off the sensor
                sdCallAndResponse(powerOff);
                // Then we shut the transitor back off
                byte[] lastCall = sdCallAndResponse(transistorOff);

                if (lastCall != null) {
                    myDrone.rgbcStatus = false;
                    // Notify that the sensor has been shut down
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
     * Check the on/off status of the RGBC sensor
     * @return
     */
    public boolean status() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable statusRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] statusCall = {0x50, 0x03, 0x60, 0x01, 0x00};

                // This basically checks to see if the transistor is on or off (1 or 0).
                byte[] statusResponse = sdCallAndResponse(statusCall);

                if (statusResponse != null) {
                    if (statusResponse[0] == 0x01) {
                        myDrone.rgbcStatus = true;
                    } else {
                        myDrone.rgbcStatus = false;
                    }
                    // Notify the listeners
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
     * Take an RGBC measurement
     * @return
     */
    public boolean measure() {
        if (!myDrone.isConnected || !myDrone.rgbcStatus) {
            return false;
        }

        Runnable measureRunnable = new Runnable() {

            @Override
            public void run() {

                byte[] readColors = {0x50, 0x06, 0x10, 0x00, 0x39, (byte) 0x90, 0x08, 0x00};
                byte[] colorBytes = sdCallAndResponse(readColors);

                if (colorBytes != null) {
                    // Parse the output

                    float R = bytes2int(colorBytes[3], colorBytes[2]);
                    float G = bytes2int(colorBytes[1], colorBytes[0]);
                    float B = bytes2int(colorBytes[5], colorBytes[4]);
                    float C = bytes2int(colorBytes[7], colorBytes[6]);

                    // These are calibration factors measured for the absorbance loss
                    // due to the window material that the Sensordrone ships with.
                    double Rcal = 0.2639626007;
                    double Gcal = 0.2935368922;
                    double Bcal = 0.379682891;
                    double Ccal = 0.2053011829;

                    R += R * Rcal;
                    G += G * Gcal;
                    B += B * Bcal;
                    C += C * Ccal;

                    // Fancy math goes here

                    // These are calibration coefficients for three
                    // different intensity semi-full spectrum light sources.
                    // If you wanted to calibrate for a different color space,
                    // this is where the magic happens...
                    double X = -0.14282 * R + 1.54924 * G + -0.95641 * B;
                    double Y = -0.32466 * R + 1.57837 * G + -0.73191 * B;
                    double Z = -0.68202 * R + 0.77073 * G + 0.56332 * B;

                    double x = X / (X + Y + Z);
                    double y = Y / (X + Y + Z);

                    double n = (x - 0.3320) / (0.1858 - y);

                    double CCT = 449.0 * Math.pow(n, 3) +
                            3525.0 * Math.pow(n, 2) +
                            6823.3 * n +
                            5520.33;


                    // Set all of the values
                    myDrone.rgbcRedChannel = R;
                    myDrone.rgbcGreenChannel = G;
                    myDrone.rgbcBlueChannel = B;
                    myDrone.rgbcClearChannel = C;
                    myDrone.rgbcLux = (float) Y;
                    myDrone.rgbcColorTemperature = (float) CCT;

                    // Notify the listener
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
     * The default constructor
     * @param drone
     */
    public RGBC_V1(CoreDrone drone) {
        super(drone, "RGBC_V1");
    }

}
