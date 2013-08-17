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
 * A class that has methods relating to "Power" (e.g. battery level, charging status, ...)
 */
public class Power_V1 extends DroneSensor {

    /**
     * Used to notify that the charging status has been checked
     */
    private DroneEventObject chargingStatus = new DroneEventObject(DroneEventObject.droneEventType.CHARGING_STATUS);
    /**
     * Used to notify that the battery voltage level has been checked
     */
    private DroneEventObject batteryVoltage = new DroneEventObject(DroneEventObject.droneEventType.BATTERY_VOLTAGE_MEASURED);
    /**
     * Used to notify if the battery is low
     */
    private DroneEventObject lowBattery = new DroneEventObject(DroneEventObject.droneEventType.LOW_BATTERY);


    /**
     * Checks to see if the Sensordrone is currently charging or not.
     * @return
     */
    public boolean chargingStatus() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable chargingStatusRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] call = {0x50, 0x06, 0x10, 0x02, 0x48, 0x01, 0x02, 0x00};
                byte[] response = sdCallAndResponse(call);
                if (response != null) {
                    // Check the third bit
                    byte thirdBit = (byte) (response[0] & 0x00000004);
                    if (thirdBit == 0x04) {
                        myDrone.isCharging = true;
                    } else {
                        myDrone.isCharging = false;
                    }
                    myDrone.notifyDroneEventHandler(chargingStatus);
                    myDrone.notifyDroneStatusListener(chargingStatus);
                }

            }
        };

        try {
            myDrone.commService.submit(chargingStatusRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Measure the battery voltage of the Sensordrone
     * @return
     */
    public boolean measureBatteryVoltage() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable batteryVoltageRunnable = new Runnable() {
            public void run() {
                byte[] call = {0x50, 0x02, 0x22, 0x00};
                byte response[] = sdCallAndResponse(call);
                if (response != null) {
                    int MSB = response[1] & 0xff;
                    int LSB = response[0] & 0xff;
                    int ADC = (MSB << 8) + LSB;
                    float voltage = (float) (((float) ADC / 4095.0) * 6.0);
                    myDrone.batteryVoltage_Volts = voltage;
                    logger.infoLogger(TAG, "ADC: "
                            + Integer.toHexString(MSB & 0xff)
                            + Integer.toHexString(LSB & 0xff) +
                            " Voltage: " + String.valueOf(myDrone.batteryVoltage_Volts)
                            , CoreDrone.DEBUG);
                    // Notify that the battery voltage has been measured
                    myDrone.notifyDroneEventHandler(batteryVoltage);
                    myDrone.notifyDroneStatusListener(batteryVoltage);
                    // Notify of low battery if less than 3.25 Volts
                    if (voltage < 3.25) {
                        myDrone.notifyDroneEventHandler(lowBattery);
                        myDrone.notifyDroneStatusListener(lowBattery);
                    }
                }
            }
        };

        try {
            myDrone.commService.submit(batteryVoltageRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Our default Constructor
     * @param drone
     */
    public Power_V1(CoreDrone drone) {
        super(drone, "Power_V1");
    }
}
