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
 * A class to interact with the ADC pin on the external connector: 12 bit resolution over 0-3 Volts
 */
public class ADC_V1 extends DroneSensor {

    // Used for notifications
    private DroneEventObject measured = new DroneEventObject(DroneEventObject.droneEventType.ADC_MEASURED);
    private DroneEventObject enabled = new DroneEventObject(DroneEventObject.droneEventType.ADC_ENABLED);
    private DroneEventObject disabled = new DroneEventObject(DroneEventObject.droneEventType.ADC_DISABLED);
    private DroneEventObject status = new DroneEventObject(DroneEventObject.droneEventType.ADC_STATUS_CHECKED);


    /**
     * Take an ADC measurement
     * @return
     */
    public boolean measureADC() {


        if (!myDrone.isConnected) {
            return false;
        }

        Runnable measureAdcRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] readExtADC = {0x50, 0x02, 0x21, 0x00};
                byte[] returnByte = sdCallAndResponse(readExtADC);

                if (returnByte != null) {
                    // Parse data
                    int MSB = 0x000000ff & ((int) returnByte[1]);
                    int LSB = 0x000000ff & ((int) returnByte[0]);
                    int ADC = LSB + (MSB << 8);
                    myDrone.externalADC = ADC;
                    myDrone.externalADC_Volts = (float) (((float) ADC / 4095.0) * 3.0);
                    // Notify the listener
                    myDrone.notifyDroneEventHandler(measured);
                    myDrone.notifyDroneEventListener(measured);
                }
            }
        };

        try {
            myDrone.commService.submit(measureAdcRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Check the on/off status of the ADC pin
     * @return
     */
    public boolean adcStatus() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable statusRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.notifyDroneEventHandler(status);
                myDrone.notifyDroneStatusListener(status);
            }
        };

        try {
            myDrone.commService.submit(statusRunnable);
        } catch (RejectedExecutionException r) {
            return false;
        }
        return true;
    }

    /**
     * Enable the ADC pin
     * @return
     */
    public boolean enable() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable enableNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.adcStatus = true;
                myDrone.notifyDroneEventHandler(enabled);
                myDrone.notifyDroneStatusListener(enabled);
            }
        };

        try {
            myDrone.commService.submit(enableNotifyRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Disable the ADC pin
     * @return
     */
    public boolean disable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.adcStatus = false;
                myDrone.notifyDroneEventHandler(disabled);
                myDrone.notifyDroneStatusListener(disabled);
            }
        };

        try {
            myDrone.commService.submit(disableNotifyRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }

        return true;
    }

    /**
     * Our default Constructor
     * @param drone
     */
    public ADC_V1(CoreDrone drone) {
        super(drone, "ADC_V1");
    }
}
