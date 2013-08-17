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
 * A class to interact with the Sensordrone's LEDs
 */
public class LEDS_V1 extends DroneSensor {


    /*
    We store all of the individual values here, and load them in to one runnable to execute
    a command on the Sensordrone.

    Both the left and the right are written during the same command, so this helps us control them separately
     */
    private byte LEFT_RED = 0x00;
    private byte RIGHT_RED = 0x00;
    private byte LEFT_GREEN = 0x00;
    private byte RIGHT_GREEN = 0x00;
    private byte LEFT_BLUE = 0x00;
    private byte RIGHT_BLUE = 0x00;


    /**
     * Set the Sensordrone's left LED
     * @param RED
     * @param GREEN
     * @param BLUE
     * @return
     */
    public boolean setLeftLED(int RED, int GREEN, int BLUE) {
        if (!myDrone.isConnected) {
            return false;
        }
        // In case people enter numbers out of range.
        RED = ledColorRange(RED);
        GREEN = ledColorRange(GREEN);
        BLUE = ledColorRange(BLUE);

        LEFT_RED = intToByte(RED);
        LEFT_GREEN = intToByte(GREEN);
        LEFT_BLUE = intToByte(BLUE);


        try {
            // Call the runnable to set the colors
            myDrone.commService.submit(colorRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Set the Sensordrone's right LED
     * @param RED
     * @param GREEN
     * @param BLUE
     * @return
     */
    public boolean setRightLED(int RED, int GREEN, int BLUE) {
        if (!myDrone.isConnected) {
            return false;
        }
        // In case people enter numbers out of range.
        RED = ledColorRange(RED);
        GREEN = ledColorRange(GREEN);
        BLUE = ledColorRange(BLUE);

        RIGHT_RED = intToByte(RED);
        RIGHT_GREEN = intToByte(GREEN);
        RIGHT_BLUE = intToByte(BLUE);

        try {
            // Call the runnable to set the colors
            myDrone.commService.submit(colorRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Set both of the Sensordrone's LEDs to the same color
     * @param RED
     * @param GREEN
     * @param BLUE
     * @return
     */
    public boolean setLEDs(int RED, int GREEN, int BLUE) {
        if (!myDrone.isConnected) {
            return false;
        }
        // In case people enter numbers out of range.
        RED = ledColorRange(RED);
        GREEN = ledColorRange(GREEN);
        BLUE = ledColorRange(BLUE);

        LEFT_RED = RIGHT_RED = intToByte(RED);
        LEFT_GREEN = RIGHT_GREEN = intToByte(GREEN);
        LEFT_BLUE = RIGHT_BLUE = intToByte(BLUE);

        try {
            // Call the runnable to set the colors
            myDrone.commService.submit(colorRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /*
     * This function is used to make sure only 0-255 is used to set the LEDS.
     */

    /**
     * This function is used to make sure only 0-255 is used to set the LEDS.
     * Less than 0 defauls to 0 and greater than 255 defaults to 255
     * @param colorValue
     * @return
     */
    private int ledColorRange(int colorValue) {
        if (colorValue > 255) {
            return 255;
        } else if (colorValue < 0) {
            return 0;
        }
        return colorValue;
    }

    /**
     * The runnable used to set the LED colors
     */
    private Runnable colorRunnable = new Runnable() {
        public void run() {
            byte[] rgbValues = {0x50, 0x08, 0x15,
                    LEFT_RED, LEFT_GREEN, LEFT_BLUE,
                    RIGHT_RED, RIGHT_GREEN, RIGHT_BLUE,
                    0x00};

            sdCallAndResponse(rgbValues);

        }
    };

    /**
     * Our default Constructor
     * @param drone
     */
    public LEDS_V1(CoreDrone drone) {
        super(drone, "LEDS_V1");
    }

}
