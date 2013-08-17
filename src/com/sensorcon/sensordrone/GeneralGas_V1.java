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
 * A class to interact with the oxidizing and reducing General Gas Sensor
 */
public class GeneralGas_V1 extends DroneSensor {

    // Used to notify about the oxidizing gas sensor
    private DroneEventObject oxidizingMeasured = new DroneEventObject(DroneEventObject.droneEventType.OXIDIZING_GAS_MEASURED);
    private DroneEventObject oxidizingEnabled = new DroneEventObject(DroneEventObject.droneEventType.OXIDIZING_GAS_ENABLED);
    private DroneEventObject oxidizingDisabled = new DroneEventObject(DroneEventObject.droneEventType.OXIDIZING_GAS_DISABLED);
    private DroneEventObject oxidizingStatus = new DroneEventObject(DroneEventObject.droneEventType.OXIDIZING_GAS_STATUS_CHECKED);

    // Used to notify about the reducing gas sensor
    private DroneEventObject reducingMeasured = new DroneEventObject(DroneEventObject.droneEventType.REDUCING_GAS_MEASURED);
    private DroneEventObject reducingEnabled = new DroneEventObject(DroneEventObject.droneEventType.REDUCING_GAS_ENABLED);
    private DroneEventObject reducingDisabled = new DroneEventObject(DroneEventObject.droneEventType.REDUCING_GAS_DISABLED);
    private DroneEventObject reducingStatus = new DroneEventObject(DroneEventObject.droneEventType.REDUCING_GAS_STATUS_CHECKED);

    /**
     * Enable the oxidizing gas sensor
     * @return
     */
    public boolean enableOxidizingGas() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableOxidizingRunnable = new Runnable() {

            @Override
            public void run() {

                byte[] call = {0x50, 0x03, 0x18, (byte) 0x84, 0x00};
                byte[] lastCall = sdCallAndResponse(call);

                if (lastCall != null) {
                    myDrone.oxidizingGasStatus = true;

                    myDrone.notifyDroneEventHandler(oxidizingEnabled);
                    myDrone.notifyDroneStatusListener(oxidizingEnabled);
                }
            }
        };

        try {
            myDrone.commService.submit(enableOxidizingRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Enable the reducing gas sensor
     * @return
     */
    public boolean enableReducingGas() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable enableReducingRunnable = new Runnable() {

            @Override
            public void run() {

                byte[] call = {0x50, 0x03, 0x19, (byte) 0xba, 0x00};
                byte[] lastCall = sdCallAndResponse(call);

                if (lastCall != null) {
                    myDrone.reducingGasStatus = true;
                    myDrone.notifyDroneEventHandler(reducingEnabled);
                    myDrone.notifyDroneStatusListener(reducingEnabled);
                }
            }
        };

        try {
            myDrone.commService.submit(enableReducingRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Disable the reducing gas sensor
     * @return
     */
    public boolean disableReducingGas() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableReducingGas = new Runnable() {

            @Override
            public void run() {

                byte[] call = {0x50, 0x03, 0x19, 0x00, 0x00};
                byte[] lastCall = sdCallAndResponse(call);

                if (lastCall != null) {
                    myDrone.reducingGasStatus = false;
                    myDrone.notifyDroneEventHandler(reducingDisabled);
                    myDrone.notifyDroneStatusListener(reducingDisabled);
                }

            }
        };

        try {
            myDrone.commService.submit(disableReducingGas);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Disable the oxidizing gas sensor
     * @return
     */
    public boolean disableOxidizingGas() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable disableOxidizingGas = new Runnable() {

            @Override
            public void run() {

                byte[] call = {0x50, 0x03, 0x18, 0x00, 0x00};
                byte[] lastCall = sdCallAndResponse(call);

                if (lastCall != null) {
                    myDrone.oxidizingGasStatus = false;
                    myDrone.notifyDroneEventHandler(oxidizingDisabled);
                    myDrone.notifyDroneStatusListener(oxidizingDisabled);
                }

            }
        };

        try {
            myDrone.commService.submit(disableOxidizingGas);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Measure from the oxidizing gas sensor
     * @return
     */
    public boolean measureOX() {
        if (!myDrone.isConnected || !myDrone.oxidizingGasStatus) {
            return false;
        }

        Runnable measureOXRunnable = new Runnable() {

            @Override
            public void run() {
                // Set up the call/response data
                byte[] oxRead = {0x50, 0x02, 0x1c, 0x00};
                byte[] oxData = sdCallAndResponse(oxRead);

                if (oxData != null) {
                    // Parse the data
                    int oxMSB = 0x000000ff & ((int) oxData[1]);
                    int oxLSB = 0x000000ff & ((int) oxData[0]);
                    int oxADC = oxLSB + (oxMSB << 8);
                    float voltage = (float) (((float) oxADC / 4095.0) * 3.3);
                    float resistance = (float) ((18000.0 * 3.3 / voltage) - 18000.0);
                    myDrone.oxidizingGas_Ohm = resistance;
                    // Notify the Listener
                    myDrone.notifyDroneEventHandler(oxidizingMeasured);
                    myDrone.notifyDroneEventListener(oxidizingMeasured);
                }

            }
        };

        try {
            myDrone.commService.submit(measureOXRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Measure from the reducing gas sensor
     * @return
     */
    public boolean measureRED() {
        if (!myDrone.isConnected || !myDrone.reducingGasStatus) {
            return false;
        }

        Runnable measureREDRunnable = new Runnable() {

            @Override
            public void run() {

                // Set up the call/response data
                byte[] redRead = {0x50, 0x02, 0x1d, 0x00};
                byte[] redData = sdCallAndResponse(redRead);

                if (redData != null) {
                    // Parse the data
                    int redMSB = 0x000000ff & ((int) redData[1]);
                    int redLSB = 0x000000ff & ((int) redData[0]);
                    int redADC = redLSB + (redMSB << 8);
                    float voltage = (float) ((redADC / 4095.0) * 3.3);
                    float resistance = (float) ((270000.0 * 3.3 / voltage) - 270000.0);
                    myDrone.reducingGas_Ohm = resistance;
                    // Notify the Listener
                    myDrone.notifyDroneEventHandler(reducingMeasured);
                    myDrone.notifyDroneEventListener(reducingMeasured);
                }

            }
        };

        try {
            myDrone.commService.submit(measureREDRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Check the on/off status of the reducing gas sensor
     * @return
     */
    public boolean reducingStatus() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable reducingStatusRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] call = {0x50, 0x02, 0x1a, 0x00};
                byte[] status = sdCallAndResponse(call);

                if (status != null) {
                    if (status[0] == 0x00) {
                        myDrone.reducingGasStatus = false;
                    } else {
                        myDrone.reducingGasStatus = true;
                    }
                    myDrone.notifyDroneEventHandler(reducingStatus);
                    myDrone.notifyDroneStatusListener(reducingStatus);
                }
            }
        };

        try {
            myDrone.commService.submit(reducingStatusRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Check the on/off status of the oxidizing gas sensor
     * @return
     */
    public boolean oxidizingStatus() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable oxidizingStatusRunnable = new Runnable() {

            @Override
            public void run() {
                byte[] call = {0x50, 0x02, 0x1b, 0x00};
                byte[] status = sdCallAndResponse(call);

                if (status != null) {
                    if (status[0] == 0x00) {
                        myDrone.oxidizingGasStatus = false;
                    } else {
                        myDrone.oxidizingGasStatus = true;
                    }
                    myDrone.notifyDroneEventHandler(oxidizingStatus);
                    myDrone.notifyDroneStatusListener(oxidizingStatus);
                }
            }
        };

        try {
            myDrone.commService.submit(oxidizingStatusRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Our default constructor
     * @param drone
     */
    GeneralGas_V1(CoreDrone drone) {
        super(drone, "GeneralGas_V1");
    }
}
