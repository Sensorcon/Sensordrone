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
 * A class to interact with the Humidity sensor (Which is also the Temperature sensor).
 */
public class Humidity_V1 extends DroneSensor {


    // Sensor specific i2c settings
    private byte I2C_BANK = 0x00;
    private byte I2C_SLAVE_ADDRESS = (byte) 0x40;

    // Used to notify about humidity
    private DroneEventObject measured = new DroneEventObject(DroneEventObject.droneEventType.HUMIDITY_MEASURED);
    private DroneEventObject enabled = new DroneEventObject(DroneEventObject.droneEventType.HUMIDITY_ENABLED);
    private DroneEventObject disabled = new DroneEventObject(DroneEventObject.droneEventType.HUMIDITY_DISABLED);
    private DroneEventObject status = new DroneEventObject(DroneEventObject.droneEventType.HUMIDITY_STATUS_CHECKED);

    // Used to notify about temperature
    private DroneEventObject tempMeasured = new DroneEventObject(DroneEventObject.droneEventType.TEMPERATURE_MEASURED);
    private DroneEventObject tempEnabled = new DroneEventObject(DroneEventObject.droneEventType.TEMPERATURE_ENABLED);
    private DroneEventObject tempDisabled = new DroneEventObject(DroneEventObject.droneEventType.TEMPERATURE_DISABLED);
    private DroneEventObject tempStatus = new DroneEventObject(DroneEventObject.droneEventType.TEMPERATURE_STATUS_CHECKED);


    /**
     * Our default Constructor
     * @param drone
     */
    Humidity_V1(final CoreDrone drone) {
        super(drone, "Humidity_V1");
    }

    /**
     * Take a humidity measurement
     * @return
     */
    public boolean measure() {
        if (!myDrone.isConnected || !myDrone.humidityStatus) {
            return false;
        }

        Runnable measureHumidityRunnable = new Runnable() {

            @Override
            public void run() {

                byte I2C_REGISTER_ADDRESS = (byte) 0xE5;
                byte I2C_REGISTER_READ_LENGTH = 0x02;

                byte[] humidity_call = {0x50, 0x06, 0x10,
                        I2C_BANK, I2C_SLAVE_ADDRESS,
                        I2C_REGISTER_ADDRESS,
                        I2C_REGISTER_READ_LENGTH,
                        0x00};

                byte[] humidity_response = sdCallAndResponse(humidity_call);

                if (humidity_response != null) {
                    // Parse data
                    int MSB = 0x000000ff & ((int) humidity_response[0]);
                    int LSB = 0x000000fc & ((int) humidity_response[1]); // fc not ff

                    int ADC = LSB + (MSB << 8);
                    logger.debugLogger(TAG, "Humidity ADC: " + String.valueOf(ADC), CoreDrone.DEBUG);

                    // RH above water
                    float humidity = (float) (-6.0 + 125.0 * ((float) ADC / Math.pow(2, 16)));
                    myDrone.humidity_Percent = humidity;

                    // There is a different equation for RH over ice.
                    // I can add it if you want.

                    myDrone.notifyDroneEventHandler(measured);
                    myDrone.notifyDroneEventListener(measured);
                }
            }
        };

        try {
            myDrone.commService.submit(measureHumidityRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Take a temperature measurement
     * @return
     */
    public boolean measureTemperature() {
        if (!myDrone.isConnected || !myDrone.temperatureStatus) {
            return false;
        }

        Runnable measureTemperatureRunnable = new Runnable() {

            @Override
            public void run() {
                byte I2C_REGISTER_READ_LENGTH = 0x02;
                byte I2C_REGISTER_ADDRESS = (byte) 0xE3;
                byte[] temperature_call = {0x50, 0x06, 0x10,
                        I2C_BANK, I2C_SLAVE_ADDRESS,
                        I2C_REGISTER_ADDRESS, I2C_REGISTER_READ_LENGTH,
                        0x00};


                byte[] temperature_response = sdCallAndResponse(temperature_call);

                if (temperature_response != null) {
                    // Parse data
                    int MSB = 0x000000ff & ((int) temperature_response[0]);
                    int LSB = 0x000000fc & ((int) temperature_response[1]); // fc not ff
                    int ADC = LSB + (MSB << 8);
                    logger.debugLogger(TAG,
                            "Temperature ADC: " + String.valueOf(ADC),
                            CoreDrone.DEBUG);
                    float temperature = (float) (-46.85 + 175.72 * ((float) ADC / Math
                            .pow(2, 16)));
                    myDrone.temperature_Celsius = temperature;
                    myDrone.temperature_Kelvin = (float) (myDrone.temperature_Celsius + 273.15);
                    myDrone.temperature_Fahrenheit = (float) (myDrone.temperature_Celsius
                            * (9.0 / 5.0) + 32.0);

                    myDrone.notifyDroneEventHandler(tempMeasured);
                    myDrone.notifyDroneEventListener(tempMeasured);
                }
            }
        };

        try {
            myDrone.commService.submit(measureTemperatureRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Enable the Humidity sensor
     * @return
     */
    public boolean enable() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable enableRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.humidityStatus = true;

                myDrone.notifyDroneEventHandler(enabled);
                myDrone.notifyDroneStatusListener(enabled);
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
     * Disable the Humidity sensor
     * @return
     */
    public boolean disable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.humidityStatus = false;
                myDrone.notifyDroneEventHandler(disabled);
                myDrone.notifyDroneStatusListener(disabled);
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
     * Enable the temperature sensor
     * @return
     */
    public boolean enableTemperature() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableTemperatureRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.temperatureStatus = true;
                myDrone.notifyDroneEventHandler(tempEnabled);
                myDrone.notifyDroneStatusListener(tempEnabled);
            }
        };
        try {
            myDrone.commService.submit(enableTemperatureRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Disable the Temperature sensor
     * @return
     */
    public boolean disableTemperature() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableTemperatureRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.temperatureStatus = false;
                myDrone.notifyDroneEventHandler(tempDisabled);
                myDrone.notifyDroneStatusListener(tempDisabled);
            }
        };

        try {
            myDrone.commService.submit(disableTemperatureRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }

        return true;
    }

    /**
     * Check the on/off status of the Humidity sensor
     * @return
     */
    public boolean status() {
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
     * Check the on/off status of the Temperature sensor
     * @return
     */
    public boolean temperatureStatus() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable tempStatusRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.notifyDroneEventHandler(tempStatus);
                myDrone.notifyDroneStatusListener(tempStatus);
            }
        };
        try {
            myDrone.commService.submit(tempStatusRunnable);
        } catch (RejectedExecutionException r) {
            return false;
        }
        return true;
    }
}
