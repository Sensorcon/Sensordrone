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

import java.math.BigInteger;
import java.util.concurrent.RejectedExecutionException;

/**
 * A class to interact with the Pressure sensor. (This is also the Altitude sensor).
 * This sensor has an onboard temperature sensor as well, however its readings are
 * not exposed from the main Drone class.
 */
public class Pressure_V1 extends DroneSensor {

    /**
     * The measured temperature in Celsius
     */
    public float TEMPERATURE_CELSIUS;
    /**
     * The measured temperature in Fahrenheit
     */
    public float TEMPERATURE_FAHRENHEIT;
    /**
     * The measured temperature in Kelvin
     */
    public float TEMPERATURE_KELVIN;


    /**
     * Used to notify that a pressure measurement has been made
     */
    private DroneEventObject measured = new DroneEventObject(DroneEventObject.droneEventType.PRESSURE_MEASURED);
    /**
     * Used to notify that a pressure sensor has been enabled
     */
    private DroneEventObject enabled = new DroneEventObject(DroneEventObject.droneEventType.PRESSURE_ENABLED);
    /**
     * Used to notify that a pressure sensor has been disabled
     */
    private DroneEventObject disabled = new DroneEventObject(DroneEventObject.droneEventType.PRESSURE_DISABLED);
    /**
     * Used to notify that a pressure sensor on/off status has been checked
     */
    private DroneEventObject status = new DroneEventObject(DroneEventObject.droneEventType.PRESSURE_STATUS_CHECKED);

    /**
     * Used to notify that an altitude measurement has been made
     */
    private DroneEventObject altitudeMeasured = new DroneEventObject(DroneEventObject.droneEventType.ALTITUDE_MEASURED);
    /**
     * Used to notify that altitude has been enabled
     */
    private DroneEventObject altitudeEnabled = new DroneEventObject(DroneEventObject.droneEventType.ALTITUDE_ENABLED);
    /**
     * Used to notify that altitude has been disabled
     */
    private DroneEventObject altitudeDisabled = new DroneEventObject(DroneEventObject.droneEventType.ALTITUDE_DISABLED);
    /**
     * Used to notify that altitude on/off status has been checked
     */
    private DroneEventObject altitudeStatus = new DroneEventObject(DroneEventObject.droneEventType.ALTITUDE_STATUS_CHECKED);

    // Sensor specific i2c settings
    private byte I2C_BANK = 0x00;
    private byte I2C_SLAVE_ADDRESS = (byte) 0x60;

    /**
     * A single method to enable the sensor, that both Pressure and Altitude can call
     * @return
     */
    private byte[] enableRunnableMethod() {


        byte i2cRegisterAddress = (byte) 0x26;
        byte i2cWriteLength = 0x01;
        byte enableByte = 0x3f;
        byte[] enableCall = {0x50, 0x07, 0x11,
                I2C_BANK, I2C_SLAVE_ADDRESS, i2cWriteLength, i2cRegisterAddress, enableByte,
                0x00};
        // Pick sampling rate;
        byte SELECTED_MODE_BYTE = (byte) 0x38; // 512ms update
        // Other options are
        // byte SELECTED_MODE_BYTE = (byte) 0x00; // 6ms update
        // byte SELECTED_MODE_BYTE = (byte) 0x08; // 10ms update
        // byte SELECTED_MODE_BYTE = (byte) 0x10; // 18ms update
        // byte SELECTED_MODE_BYTE = (byte) 0x18; // 34ms update
        // byte SELECTED_MODE_BYTE = (byte) 0x20; // 66ms update
        // byte SELECTED_MODE_BYTE = (byte) 0x28; // 130ms update
        // byte SELECTED_MODE_BYTE = (byte) 0x30; // 258ms update

        // This sets the mode
        byte[] setMode = {0x50, 0x07, 0x11, 0x00, 0x60, 0x01, 0x26, SELECTED_MODE_BYTE, 0x00};
        // This enable data flags (allows checking pin to see if data is ready; not currently used)
        byte[] enableDataFlags = {0x50, 0x07, 0x11, 0x00, 0x60, 0x01, 0x13, 0x07, 0x00};
        // This sets the sensor from standby to active
        byte[] setActive = {0x50, 0x07, 0x11, 0x00, 0x60, 0x01, 0x26, (byte) (SELECTED_MODE_BYTE + 0x01), 0x00};

        // First we turn the sensor on
        byte[] firstResponse = sdCallAndResponse(enableCall);
        // Make sure the command was sent
        if (firstResponse == null) {
            return null;
        }
        // Set the mode
        sdCallAndResponse(setMode);
        // Enable data flags
        sdCallAndResponse(enableDataFlags);
        // Set active
        byte[] lastCall = sdCallAndResponse(setActive);
        return lastCall;
    }


    /**
     * Enables the pressure sensor
     * @return
     */
    public boolean enable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] lastCall = enableRunnableMethod();
                if (lastCall != null) {
                    myDrone.pressureStatus = true;
                    myDrone.notifyDroneEventHandler(enabled);
                    myDrone.notifyDroneStatusListener(enabled);
                }
            }
        };

        Runnable enableNotifyRunnable = new Runnable() {

            @Override
            public void run() {
                myDrone.pressureStatus = true;

                myDrone.notifyDroneEventHandler(enabled);
                myDrone.notifyDroneStatusListener(enabled);
            }
        };

        // Is "Altitude" already on?
        if (!myDrone.altitudeStatus) {
            try {
                myDrone.commService.submit(enableRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        } else {
            try {
                myDrone.commService.submit(enableNotifyRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Enables the Altitude
     * @return
     */
    public boolean enableAltitude() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableAltitudeRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] lastCall = enableRunnableMethod();
                if (lastCall != null) {
                    myDrone.altitudeStatus = true;

                    myDrone.notifyDroneEventHandler(altitudeEnabled);
                    myDrone.notifyDroneStatusListener(altitudeEnabled);
                }
            }
        };

        Runnable enableAltitudeNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.altitudeStatus = true;
                myDrone.notifyDroneEventHandler(altitudeEnabled);
                myDrone.notifyDroneStatusListener(altitudeEnabled);
            }
        };

        // Is the "pressure sensor" on?
        if (!myDrone.pressureStatus) {
            try {
                myDrone.commService.submit(enableAltitudeRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        } else {
            try {
                myDrone.commService.submit(enableAltitudeNotifyRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * A single method to disable the sensor, that both Pressure and Altitude can call
     * @return
     */
    private byte[] disableRunnableFunction() {
        byte i2cRegisterAddress = (byte) 0x26;
        byte i2cWriteLength = 0x01;
        byte disableByte = 0x00;
        byte[] call = {0x50, 0x07, 0x11,
                I2C_BANK, I2C_SLAVE_ADDRESS, i2cWriteLength, i2cRegisterAddress, disableByte,
                0x00};

        byte[] lastCall = sdCallAndResponse(call);
        return lastCall;
    }


    /**
     * Disable the Pressure
     * @return
     */
    public boolean disable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] lastCall = disableRunnableFunction();
                if (lastCall != null) {
                    myDrone.pressureStatus = false;

                    myDrone.notifyDroneEventHandler(disabled);
                    myDrone.notifyDroneStatusListener(disabled);
                }
            }
        };

        Runnable disableNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.pressureStatus = false;
                myDrone.notifyDroneEventHandler(disabled);
                myDrone.notifyDroneStatusListener(disabled);
            }
        };

        if (!myDrone.altitudeStatus) {
            try {
                myDrone.commService.submit(disableRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        } else {
            try {
                myDrone.commService.submit(disableNotifyRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Disable Altitude
     * @return
     */
    public boolean disableAltitude() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableAltitudeRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] lastCall = disableRunnableFunction();
                if (lastCall != null) {
                    myDrone.altitudeStatus = false;

                    myDrone.notifyDroneEventHandler(altitudeDisabled);
                    myDrone.notifyDroneStatusListener(altitudeDisabled);
                }
            }
        };

        Runnable disableAltitudeNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.altitudeStatus = false;
                myDrone.notifyDroneEventHandler(altitudeDisabled);
                myDrone.notifyDroneStatusListener(altitudeDisabled);
            }
        };

        if (!myDrone.pressureStatus) {
            try {
                myDrone.commService.submit(disableAltitudeRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        } else {
            try {
                myDrone.commService.submit(disableAltitudeNotifyRunnable);
            } catch (RejectedExecutionException e) {
                return false;
            }
        }
        return true;
    }


    /**
     * Check the on/off status of the Pressure sensor
     * @return
     */
    public boolean status() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable statusRunnable = new Runnable() {

            @Override
            public void run() {
                byte i2cRegisterAddress = (byte) 0x26;
                byte i2cReadLength = 0x02;
                byte[] call = {0x50, 0x06, 0x10,
                        I2C_BANK, I2C_SLAVE_ADDRESS, i2cRegisterAddress, i2cReadLength,
                        0x00};
                byte[] statusData = sdCallAndResponse(call);
                if (statusData == null) {
                    return;
                }
                byte statusCheck = (byte) (0x01 & statusData[0]);
                if (statusCheck == 0x01) {
                    myDrone.pressureStatus = true;
                } else {
                    myDrone.pressureStatus = false;
                }

                myDrone.notifyDroneEventHandler(status);
                myDrone.notifyDroneStatusListener(status);
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
     * Check the on/off status of the Altitude sensor
     * @return
     */
    public boolean statusAltitude() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable statusAltitudeRunnable = new Runnable() {

            @Override
            public void run() {
                byte i2cRegisterAddress = (byte) 0x26;
                byte i2cReadLength = 0x02;
                byte[] call = {0x50, 0x06, 0x10,
                        I2C_BANK, I2C_SLAVE_ADDRESS, i2cRegisterAddress, i2cReadLength,
                        0x00};
                byte[] statusData = sdCallAndResponse(call);
                if (statusData == null) {
                    return;
                }
                byte statusCheck = (byte) (0x01 & statusData[0]);
                if (statusCheck == 0x01) {
                    myDrone.altitudeStatus = true;
                } else {
                    myDrone.altitudeStatus = false;
                }

                myDrone.notifyDroneEventHandler(altitudeStatus);
                myDrone.notifyDroneStatusListener(altitudeStatus);
            }
        };

        try {
            myDrone.commService.submit(statusAltitudeRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Measures the sensors on-board Temperature. Not accessible from the Drone class.
     * @return
     */
    public boolean measureTemperature() {

        if (!myDrone.isConnected) {
            return false;
        }

        Runnable temperatureRunnable = new Runnable() {
            @Override
            public void run() {
                byte[] getData = {0x50, 0x05, 0x10, 0x00, 0x60, 0x01, 0x05};
                byte[] sensorData = sdCallAndResponse(getData);
                if (sensorData != null) {
                    // The integer portion is in 2's Compliment
                    byte[] tempByte = {sensorData[6]};
                    BigInteger bigTemp = new BigInteger(tempByte);
                    // The decimal portion is NOT in 2's Compliment
                    int tempDecimal = 0x000000ff & (int) sensorData[7];
                    TEMPERATURE_CELSIUS = (float) (bigTemp.floatValue() + (tempDecimal >> 4) / 16.0);
                    TEMPERATURE_KELVIN = (float) (TEMPERATURE_CELSIUS - 273.15);
                    TEMPERATURE_FAHRENHEIT = (float) (TEMPERATURE_CELSIUS * (9.0 / 5.0) + 32.0);
                }
            }
        };

        try {
            myDrone.commService.submit(temperatureRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Measure the Pressure
     * @return
     */
    public boolean measurePressure() {
        if (!myDrone.isConnected || !myDrone.pressureStatus) {
            return false;
        }

        Runnable measurePressureRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] getData = {0x50, 0x05, 0x10, 0x00, 0x60, 0x01, 0x05};
                byte[] sensorData = sdCallAndResponse(getData);
                if (sensorData != null) {

                    byte[] presByte = {sensorData[0], sensorData[1]};
                    // Two's compliment is easy with a BigInteger :-)
                    BigInteger bigPres = new BigInteger(presByte);
                    int presIntBits = 0x000000ff & ((int) sensorData[2] & 0x0c);
                    int presDecBits = 0x000000ff & ((int) sensorData[2] & 0x03);
                    myDrone.pressure_Pascals = (float) ((bigPres.intValue() << 2)
                            + presIntBits + (presDecBits / 4.0));
                    myDrone.pressure_Atmospheres = (float) (myDrone.pressure_Pascals * 9.86923267e-6);
                    myDrone.pressure_Torr = (float) (myDrone.pressure_Pascals * 0.00750061683);
                    // Notify the listener
                    myDrone.notifyDroneEventHandler(measured);
                    myDrone.notifyDroneEventListener(measured);
                }
            }
        };

        try {
            myDrone.commService.submit(measurePressureRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Measure the Altitude
     * @return
     */
    public boolean measureAltitude() {
        if (!myDrone.isConnected || !myDrone.altitudeStatus) {
            return false;
        }

        // For altitude, we will just measure Pressure and convert it ourselves
        // instead of switching modes.
        Runnable measureAltitudeRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] getData = {0x50, 0x05, 0x10, 0x00, 0x60, 0x01, 0x05};
                byte[] sensorData = sdCallAndResponse(getData);
                if (sensorData != null) {
                    // The Integer portion of the pressure is in Two's Compliment
                    byte[] presByte = {sensorData[0], sensorData[1]};
                    BigInteger bigPres = new BigInteger(presByte);
                    int presIntBits = 0x000000ff & ((int) sensorData[2] & 0x0c);
                    int presDecBits = 0x000000ff & ((int) sensorData[2] & 0x03);
                    float pressurePascals = (float) ((bigPres.intValue() << 2)
                            + presIntBits + (presDecBits / 4.0));
                    // Fancy math goes here
                    float pRatio = (float) (pressurePascals / 101326.0);
                    float altitudeMeters = (float) ((1 - Math.pow(pRatio, 0.1902632)) * 44330.77);
                    myDrone.altitude_Feet = (float) (altitudeMeters * 3.2084);
                    myDrone.altitude_Meters = altitudeMeters;
                    // Notify the listener
                    myDrone.notifyDroneEventHandler(altitudeMeasured);
                    myDrone.notifyDroneEventListener(altitudeMeasured);
                }
            }
        };

        try {
            myDrone.commService.submit(measureAltitudeRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Our default Constructor
     * @param drone
     */
    public Pressure_V1(CoreDrone drone) {
        super(drone, "Pressure_V1");
    }

}
