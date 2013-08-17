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

import java.io.IOException;


/**
 * This is an abstract class that every low-level sensor object extends.
 * It contains common functions, and the communication methods used
 * to interact with the Sensordrone.
 */
public abstract class DroneSensor {

    /**
     * Used to notify if the connection to the Sensordrone was lost
     */
    private DroneEventObject deConnectionLost = new DroneEventObject(DroneEventObject.droneEventType.CONNECTION_LOST);
    /**
     * Used to notify if the Sensordrone's battery is low
     */
    private DroneEventObject dsLowBattery = new DroneEventObject(DroneEventObject.droneEventType.LOW_BATTERY);

    /*
     * Error codes
     */
    private static final byte ERROR_GENERIC = 0x00;
    private static final byte ERROR_COMAND_NOT_RECOGNIZED = 0x01;
    private static final byte ERROR_LOW_BATTERY = 0x02;
    private static final byte ERROR_I2C_TIMEOUT = 0x03;
    /*
     * Protected things to share amongst the Sensor classes extending this
     */
    protected Logger logger;
    protected CoreDrone myDrone;
    protected String TAG;

    /**
     * Our default Constructor
     * @param drone
     */
    public DroneSensor(CoreDrone drone) {
        myDrone = drone;
        logger = myDrone.logger;
    }

    /**
     * Our preferred Constructor
     * @param drone
     * @param tag
     */
    public DroneSensor(CoreDrone drone, String tag) {
        myDrone = drone;
        logger = myDrone.logger;
        TAG = tag;
    }

    /**
     * A method to send a data packet to the Sensordrone, and receive a responding data packet back.
     * @param call
     * @return
     */
    public byte[] sdCallAndResponse(byte[] call) {

        // Don't do anything if the CoreDrone's commService is shut down!
        if (myDrone.commService.isShutdown()) {
            logger.debugLogger(TAG, "commService is down. Aborting call...", CoreDrone.DEBUG);
            return null;
        }
        byte[] response;
        sdWrite(call);

        response = sdRead();

        // Will pass null if bad response
        return response;

    }

    /**
     * A method to write a data packet to the Sensordrone without automatically reading back a response
     * @param call
     */
    private void sdWrite(byte[] call) {


        // Don't do anything if the CoreDrone's commService is shut down!
        if (myDrone.commService.isShutdown()) {
            logger.debugLogger(TAG, "commService is down. Aborting call...", CoreDrone.DEBUG);
            return;
        }

        // Make the Call
        try {
            logger.debugLogger(TAG, "Making the call", CoreDrone.DEBUG);
            myDrone.oStream.write(call);
            myDrone.oStream.flush();
            logger.txLogger(TAG, call, CoreDrone.DEBUG);

        } catch (IOException e) {
            // If an IOException is thrown, it's safe to assume
            // there was an disconnect. Try to disconnect gracefully
            logger.debugLogger(TAG, "Communication lost... disconnecting", CoreDrone.DEBUG);
            connectionLost();
        }

    }

    /**
     * A method to read a data packet from the Sensordrone
     * @return
     */
    private byte[] sdRead() {

        // Don't do anything if the CoreDrone's commService is shut down!
        if (myDrone.commService.isShutdown()) {
            logger.debugLogger(TAG, "commSerive is down. Aborting call...", CoreDrone.DEBUG);
            return null;
        }

        // Read 2 bytes to get the length
        int headerLength = 2;
        byte[] header = new byte[headerLength];
        try {
            logger.debugLogger(TAG, "Reading the first two bytes", CoreDrone.DEBUG);
            myDrone.iStream.read(header, 0, headerLength);
            logger.rxLogger(TAG, header, CoreDrone.DEBUG);
        } catch (IOException e) {
            // If an IOException is thrown, it's safe to assume
            // there was an disconnect. Try to disconnect gracefully
            logger.debugLogger(TAG, "Communication lost... disconnecting", CoreDrone.DEBUG);
            connectionLost();
            return null;
        }


        // Read the rest based on the determined length
        int dataLength = (int) header[1];
        if (headerLength > 0) {
            byte[] data = new byte[dataLength];
            try {
                logger.debugLogger(TAG, "Reading the rest of the data", CoreDrone.DEBUG);
                myDrone.iStream.read(data, 0, dataLength);
                logger.rxLogger(TAG, data, CoreDrone.DEBUG);
            } catch (IOException e) {
                // If an IOException is thrown, it's safe to assume
                // there was an disconnect. Try to disconnect gracefully
                logger.debugLogger(TAG, "Communication lost... disconnecting", CoreDrone.DEBUG);
                connectionLost();
                return null;
            }

            // Was the response an error message?
            byte commandType = data[0];
            if (commandType == (byte) 0x99) {
                // It's an error.
                byte errorCode = data[1];
                // Handle different Error
                if (errorCode == ERROR_GENERIC) {
                    return null;
                } else if (errorCode == ERROR_COMAND_NOT_RECOGNIZED) {
                    return null;
                } else if (errorCode == ERROR_LOW_BATTERY) {
                    // Notify that there is a low battery condition
                    myDrone.notifyDroneStatusListener(dsLowBattery);
                    return null;
                } else if (errorCode == ERROR_I2C_TIMEOUT) {
                    return null;
                } else {
                    return null;
                }
            }
            // So far we just return null / fail silently; none of these errors are show-stoppers.
            // Some logging would be a good thing to add here.


            // If we've made it this far, there is no error

            // Strip off the command type and null packet
            byte[] response = new byte[data.length - 2];
            for (int i = 0; i < response.length; i++) {
                response[i] = data[i + 1];
            }
            return response;
        }
        return null;
    }

    /*
     * Used to convert an MSB and an LSB to an int.
     */

    /**
     * A method used to convert an MSB and an LSB to an int
     * @param MSB
     * @param LSB
     * @return
     */
    public int bytes2int(byte MSB, byte LSB) {
        int intMSB = 0x000000ff & ((int) MSB);
        int intLSB = 0x000000ff & ((int) LSB);
        return ((intMSB << 8) + intLSB);
    }

    /**
     * A method to convert an int to a byte (0-255)
     * @param intVal
     * @return
     */
    public byte intToByte(int intVal) {
        if (intVal < 0) {
            return 0x00;
        }
        if (intVal > 255) {
            return (byte) 0xff;
        }
        byte returnValue;
        returnValue = (byte) (intVal & 0x000000ff);
        return returnValue;
    }


    /**
     * A method to try and gracefully "shut down stuff" if the
     * connection to the Sensordrone is lost.
     */
    void connectionLost() {


        // Stop the communications queue.
        myDrone.commService.shutdownNow();

        // Try to close any input streams
        try {
            myDrone.iStream.close();
        } catch (IOException e1) {

        }
        // Try to close and output streams
        try {
            myDrone.oStream.close();
        } catch (IOException e1) {

        }
        // Try and close the socket (implementation dependant!)
        myDrone.closeSocket();
        myDrone.isConnected = false;
        myDrone.resetFirmwareVersion();
        myDrone.notifyDroneEventHandler(deConnectionLost);
        myDrone.notifyDroneEventListener(deConnectionLost);

    }


}
