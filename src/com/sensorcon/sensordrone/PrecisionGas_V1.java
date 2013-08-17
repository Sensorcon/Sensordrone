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

import java.nio.ByteBuffer;
import java.util.concurrent.RejectedExecutionException;


/**
 * A class to interact with the Precision Gas Sensor
 */
public class PrecisionGas_V1 extends DroneSensor {


    /**
     * These are the multipliers needed for the appropriate (automatic) gain stage on the sensor
     */
    protected final int gainRes[] =
            {
                    2200000,
                    301961,
                    113793,
                    34452,
                    13911,
                    6978,
                    3494,
                    2747
            };

    /**
     * The ADC value from the sensor
     */
    int PRECISION_GAS_ADC;

    /**
     * The calibrated baseline (stored as an ADC value)
     */
    private float calibratedBaseline;
    /**
     * The calibrated sensitivity (stored in unis of nA/ppm)
     */
    private float calibratedSensitivity;

    // These are variables used for developer
    float PRECISION_GAS_BASELINE;
    float PRECISION_GAS_SENSITIVITY;

    /**
     * Used to notify that a measurement was made
     */
    private DroneEventObject measured = new DroneEventObject(DroneEventObject.droneEventType.PRECISION_GAS_MEASURED);
    /**
     * Used to notify that the sensor was enabled
     */
    private DroneEventObject enabled = new DroneEventObject(DroneEventObject.droneEventType.PRECISION_GAS_ENABLED);
    /**
     * Used to notify that the sensor was disabled
     */
    private DroneEventObject disabled = new DroneEventObject(DroneEventObject.droneEventType.PRECISION_GAS_DISABLED);
    /**
     * Used to notify that the on/off status was checked
     */
    private DroneEventObject status = new DroneEventObject(DroneEventObject.droneEventType.PRECISION_GAS_STATUS_CHECKED);


    /**
     * Our default Constructor
     * @param drone
     */
    public PrecisionGas_V1(CoreDrone drone) {
        super(drone, "PrecisionGas_V1");
    }

    /**
     * Take a measurement
     * @return
     */
    public boolean measure() {
        if (!myDrone.isConnected || !myDrone.precisionGasStatus) {
            return false;
        }

        Runnable measureRunnable = new Runnable() {

            @Override
            public void run() {
                byte call[] = {0x50, 0x02, 0x20, 0x00};
                byte[] response = sdCallAndResponse(call);

                if (response != null) {
                    int LSB = 0x000000FF & response[0];
                    int MSB = 0x000000FF & response[1];
                    int gainStage = 0x000000FF & response[2];
                    int ADC = (MSB << 8) + LSB;
                    logger.debugLogger(TAG, "ADC: " + String.valueOf(ADC), CoreDrone.DEBUG);
                    logger.debugLogger(TAG, "Gain stage: " + String.valueOf(gainStage), CoreDrone.DEBUG);
                    logger.debugLogger(TAG, "Gain Resistor: " + String.valueOf(gainRes[gainStage]), CoreDrone.DEBUG);
                    // PPM Calculation
                    float deltaADC = (float) ADC - calibratedBaseline;
                    float gasResponse = (float) ((deltaADC * 3.0e9) / 4096.0);
                    // Uncomment the following if statement if you don't display negative values
                    // (e.g. from perhaps a small baseline shift)
//                    if (deltaADC < 0.0) {
//                        gasResponse = 0;
//                    }
                    myDrone.precisionGas_ppmCarbonMonoxide = gasResponse / (calibratedSensitivity * (float) gainRes[gainStage]);

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

    // This should be executed in CoreDrone.btConnect()

    /**
     * This methods reads and sets up the calibration values. This should be run in any connect method!
     * @return
     */
    public boolean readCalibrationData() {
        // Don't test for isConnected here.
        // This is done before the system knows it's connected, so we don't check.

        Runnable readCalibrationRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] call = {0x50, 0x02, 0x40, 0x00};
                byte[] response = sdCallAndResponse(call);
                if (response != null) {
                    int sensitivityLSB = response[0] & 0x000000ff;
                    int sensitivityMSB = ((response[1] & 0x000000ff) << 8);
                    calibratedSensitivity = (float) ((sensitivityMSB + sensitivityLSB) / 1000.0);
                    logger.debugLogger(TAG, "Sensitivity set to " + String.valueOf(calibratedSensitivity), CoreDrone.DEBUG);
                    int baselineLSB = response[2] & 0x000000ff;
                    int baselineMSB = ((response[3] & 0x000000ff) << 8);
                    calibratedBaseline = (float) (baselineMSB + baselineLSB);
                    logger.debugLogger(TAG, "Baseline set to " + String.valueOf(calibratedBaseline), CoreDrone.DEBUG);
                }
            }
        };

        try {
            myDrone.commService.submit(readCalibrationRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Enable the sensor
     * @return
     */
    public boolean enable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.precisionGasStatus = true;
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
     * Disable the sensor
     * @return
     */
    public boolean disable() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableNotifyRunnable = new Runnable() {
            @Override
            public void run() {
                myDrone.precisionGasStatus = false;
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
     * Check the on/off status of the sensor
     * @return
     */
    public boolean checkStatus() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable statusRunnable = new Runnable() {
            @Override
            public void run() {
                // Nothing to check, just running notify through commService
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
     * This is a developer method, used to read a particular register for this sensor
     * @param Register
     */
    public void readRegister(final byte Register) {
        if (!myDrone.isConnected) {
            return;
        }
        Runnable getTheData = new Runnable() {

            @Override
            public void run() {
                final byte[] call = {0x50, 0x06, 0x10, 0x00, 0x48, (byte) Register, 0x02, 0x00};
                byte[] data = sdCallAndResponse(call);
                logger.debugLogger(TAG, "Register " +
                        Integer.toHexString(Register & 0xff) + ": " +
                        Integer.toHexString(data[0] & 0xff), myDrone.DEBUG);
            }
        };

        try {
            myDrone.commService.submit(getTheData);
        } catch (RejectedExecutionException e) {
            return;
        }
    }


    /**
     * This is a developer method that behaves similar to the measure() method, except it
     * has two accessible variables useful for calibration (not implemented here).
     */
    public void measureADC() {

        if (!myDrone.isConnected) {
            return;
        }


        Runnable adcRunnable = new Runnable() {

            @Override
            public void run() {
                byte call[] = {0x50, 0x02, 0x20, 0x00};
                byte[] response = sdCallAndResponse(call);

                if (response != null) {
                    int LSB = 0x000000FF & response[0];
                    int MSB = 0x000000FF & response[1];
                    int gainStage = 0x000000FF & response[2];
                    int ADC = (MSB << 8) + LSB;

                    PRECISION_GAS_ADC = ADC;

                    // PPM Calculation
                    float deltaADC = (float) ((float) ADC - calibratedBaseline);
                    float gasResponse = (float) ((deltaADC * 3e9) / 4096.0);
                    float PPM = (float) (gasResponse / (calibratedSensitivity * (float) gainRes[gainStage]));

                    double CALIBRATION_GAS_CONCENTRATION = 50.0;
                    // This corresponds to a baseline of
                    PRECISION_GAS_BASELINE = ADC;
                    // and a sensitivity of
                    PRECISION_GAS_SENSITIVITY = (float) (gasResponse / (CALIBRATION_GAS_CONCENTRATION * (float) gainRes[gainStage]));

                    myDrone.precisionGas_ppmCarbonMonoxide = PPM;

                    myDrone.notifyDroneEventHandler(measured);
                    myDrone.notifyDroneEventListener(measured);
                }
            }
        };

        try {
            myDrone.commService.submit(adcRunnable);
        } catch (RejectedExecutionException e) {
            return;
        }
        return;
    }

    /**
     * A developer method to store calibration data. If you mess this up, you're gonna have a bad time.
     * @param data
     */
    public void writeCalibrationData(byte[] data) {


        if (!myDrone.isConnected) {
            return;
        }
        if (data.length != 4) {
            return;
        }

        final ByteBuffer calibrationData = ByteBuffer.wrap(data);

        Runnable writeCalibrationRunnable = new Runnable() {


            @Override
            public void run() {
                ByteBuffer call = ByteBuffer.allocate(8);
                byte[] firstThree = {0x50, 0x06, 0x41};
                byte[] zero = {0x00};
                call.put(firstThree);
                call.put(calibrationData.array());
                call.put(zero);
                sdCallAndResponse(call.array());
            }
        };

        try {
            myDrone.commService.submit(writeCalibrationRunnable);
        } catch (RejectedExecutionException e) {
            return;
        }
        return;
    }
}
