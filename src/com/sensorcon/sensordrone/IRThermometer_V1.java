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
 * A class to interact with the IR temperature sensor
 */
public class IRThermometer_V1 extends DroneSensor {


    // Sensor specific i2c settings
    private byte I2C_BANK = 0x00;
    private byte I2C_SLAVE_ADDRESS = (byte) 0x41;

    // Terms used for calculating the objects Temperature
    private double a1 = 1.75E-3;
    private double a2 = -1.678E-5;
    private double T_REF = 298.15;
    private double b0 = -2.94E-5;
    private double b1 = -5.7E-7;
    private double b2 = 4.63E-9;
    private double c2 = 13.4;
    // s0 is the calibration factor
    // If you want to tweak the accuracy/range, this would be the number to mess with.
    private double s0 = 2.51E-14;

    // Used for notifications
    private DroneEventObject measured = new DroneEventObject(DroneEventObject.droneEventType.IR_TEMPERATURE_MEASURED);
    private DroneEventObject enabled = new DroneEventObject(DroneEventObject.droneEventType.IR_TEMPERATURE_ENABLED);
    private DroneEventObject disabled = new DroneEventObject(DroneEventObject.droneEventType.IR_TEMPERATURE_DISABLED);
    private DroneEventObject status = new DroneEventObject(DroneEventObject.droneEventType.IR_TEMPERATURE_STATUS_CHECKED);


    /**
     * Enable the IR sensor
     * @return
     */
    public boolean enable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableRunnable = new Runnable() {

            @Override
            public void run() {
                byte i2cStatusRegister = 0x02;
                byte i2cWriteLength = 0x01;
                byte enableByte = 0x75;
                byte[] enableCall = {0x50, 0x07, 0x11,
                        I2C_BANK, I2C_SLAVE_ADDRESS, i2cWriteLength, i2cStatusRegister, enableByte,
                        0x00};

                byte[] lastCall = sdCallAndResponse(enableCall);
                if (lastCall != null) {
                    myDrone.irTemperatureStatus = true;

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
     * Disable the IR sensor
     * @return
     */
    public boolean disable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableRunnable = new Runnable() {

            @Override
            public void run() {
                byte i2cStatusRegister = 0x02;
                byte disableByte = 0x00;
                byte i2cWriteLength = 0x01;
                byte[] disableCall = {0x50, 0x07, 0x11,
                        I2C_BANK, I2C_SLAVE_ADDRESS, i2cWriteLength, i2cStatusRegister, disableByte,
                        0x00
                };
                byte[] lastCall = sdCallAndResponse(disableCall);
                if (lastCall != null) {
                    myDrone.irTemperatureStatus = false;
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
     * Check the on/off status of the IR sensor
     * @return
     */
    public boolean status() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable statusRunnable = new Runnable() {

            @Override
            public void run() {
                byte i2cStatusRegister = 0x02;
                byte i2cReadLength = 0x01;
                byte[] statusCall = {0x50, 0x06, 0x10,
                        I2C_BANK, I2C_SLAVE_ADDRESS, i2cStatusRegister, i2cReadLength,
                        0x00
                };

                byte[] statusCheck = sdCallAndResponse(statusCall);

                if (statusCheck != null) {
                    byte statusByte = (byte) (0x0000000e & statusCheck[0]);
                    if (statusByte == 0x0e) {
                        myDrone.irTemperatureStatus = true;
                    } else {
                        myDrone.irTemperatureStatus = false;
                    }
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


    // Equations for calculating Target Object Temperatures
    private double S(double T_DIE, double calibration_constant) {
        double sensitivity = calibration_constant * (1 + a1 * (T_DIE - T_REF) + a2 * (T_DIE - T_REF) * (T_DIE - T_REF));
        return sensitivity;
    }
    private double V_os(double T_DIE) {
        double offset = b0 + b1 * (T_DIE - T_REF) + b2 * (T_DIE - T_REF) * (T_DIE - T_REF);
        return offset;
    }
    private double Seebeck(double V_Obj, double V_os) {
        double f_V_Obj = (V_Obj - V_os) + c2 * (V_Obj - V_os) * (V_Obj - V_os);
        return f_V_Obj;
    }

    // This one's a biggie
    private Runnable measureRunnable = new Runnable() {

        @Override
        public void run() {
            // We need to get the Die Temperature and Voltage
            byte I2C_REGISTER_ADDRESS_TEMP = 0x01;
            byte I2C_REGISTER_ADDRESS_VOLT = 0x00;
            byte I2C_REGISTER_READ_LENGTH = 0x02;
            // Getting the die temperature and the object voltage need to be done in two separate calls.
            // Making one read of twice the length will only result in headaches, tears, and an incorrect reading.
            byte[] call_temp = {0x50, 0x06, 0x10,
                    I2C_BANK, I2C_SLAVE_ADDRESS, I2C_REGISTER_ADDRESS_TEMP, I2C_REGISTER_READ_LENGTH,
                    0x00};
            byte[] call_voltage = {0x50, 0x06, 0x10,
                    I2C_BANK, I2C_SLAVE_ADDRESS, I2C_REGISTER_ADDRESS_VOLT, I2C_REGISTER_READ_LENGTH,
                    0x00};
            byte[] data_temp = sdCallAndResponse(call_temp);
            if (data_temp == null) {
                return;
            }
            byte[] data_volt = sdCallAndResponse(call_voltage);
            if (data_volt != null) {
                // Data is in two's complement so we should be able to use a BigInteger
                byte[] twosC_temp = {data_temp[0], data_temp[1]};
                byte[] twosC_volt = {data_volt[0], data_volt[1]};
                BigInteger bigIntTemp = new BigInteger(twosC_temp);
                BigInteger bigIntVolt = new BigInteger(twosC_volt);
                int T_DIE = bigIntTemp.intValue();
                logger.debugLogger(TAG, "T_DIE: " + String.valueOf(T_DIE),
                        myDrone.DEBUG);
                int V_OBJ = bigIntVolt.intValue();
                logger.debugLogger(TAG, "V_Object: " + String.valueOf(V_OBJ),
                        myDrone.DEBUG);
                // Parse the data
                double dT_Die = (double) ((T_DIE / (32.0 * 4.0)) + 273.15); // Should be Kelvin.
                // The *4 was reversed engineered by me. I probably jut didn't bit shift it correctly,
                // but, hey, the data sheet didn't tell me to.
                double dV_Obj = (double) (V_OBJ * 156.25e-9); // Should be in Volts
                double Vos = V_os(dT_Die);
                double sensitivity = S(dT_Die, s0);
                double fVobj = Seebeck(dV_Obj, Vos);
                double TMP = dT_Die * dT_Die * dT_Die * dT_Die
                        + (fVobj / sensitivity);
                double temperature = Math.sqrt(TMP);
                temperature = Math.sqrt(temperature);

                // Some of you may be asking yourselves about that s0 factor above.
                // Here is the general run down of how to get a good one.

                // If you KNOW the temperature of the object you are point the device at
                double KNOWN_TEMPERATURE = 273.15;
                // You can calculate an (X,Y) pair
                double calX = Math.pow(KNOWN_TEMPERATURE, 4) - Math.pow(dT_Die, 4);
                double calY = fVobj / (1 + a1 * (dT_Die - T_REF) + a2 * (dT_Die - T_REF));
                // Which you can log
                logger.debugLogger(TAG, String.valueOf(KNOWN_TEMPERATURE) + ": (" + calX + "," + calY + ")", myDrone.DEBUG);
                // If you do this for at least two different KNOWN_TEMPERATURES and
                // plot the data, the slope of that line will be your calibration factor.
                // More known temperature points = a better calibration factor.
                // A wider range of know temperatures = a better calibration factor

                // Assign our values
                myDrone.irTemperature_Kelvin = (float) temperature;
                myDrone.irTemperature_Celsius = (float) (myDrone.irTemperature_Kelvin - 273.15);
                myDrone.irTemperature_Fahrenheit = (float) (myDrone.irTemperature_Celsius
                        * (9.0 / 5.0) + 32.0);
                // Notify our listener that we are done
                myDrone.notifyDroneEventHandler(measured);
                myDrone.notifyDroneEventListener(measured);
            }
        }
    };

    /**
     * Take a measurement from the sensor
     * @return
     */
    public boolean measure() {
        if (!myDrone.isConnected || !myDrone.irTemperatureStatus) {
            return false;
        }
        try {
            myDrone.commService.submit(measureRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Our default Constructor
     * @param drone
     */
    public IRThermometer_V1(CoreDrone drone) {
        super(drone, "IRThermometer_V1");
    }

}
