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
package com.sensorcon.sensordrone.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import com.sensorcon.sensordrone.CoreDrone;
import com.sensorcon.sensordrone.DroneEventObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * The Drone class allows you to control a Sensordrone and interact with the on-board sensors via Android
 *
 * @see CoreDrone
 */
public class Drone extends CoreDrone {


    /*

    We really only need to handle Android specific things here; i.e. connections

     */



    /*
     * Bluetooth communication related items
     */
    protected BluetoothAdapter btAdapter;
    protected BluetoothDevice btDevice;
    protected BluetoothSocket btSocket;

    /**
     * Our default Constructor
     */
    public Drone() {
        super(new ALogger()); // Bad things happen if you don't have a logger set up.
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Connect to a Sensordrone via Bluetooth.
     *
     * @param MAC The MAC address of the Sensordrone to connect to. The MAC address should be formatted as
     *            XX:XX:XX:XX:XX:XX (2 digits, colon, 2 digits, colon, ...).
     *            <p/>
     *            Older versions of Android (pre-Gingerbread) will
     *            likely require a pin-code to connect. This pin code is 0000 (four zeros).
     * @return Returns true upon successful connection; false otherwise.
     * @since 1.1.1
     */
    public boolean btConnect(String MAC) {
        // Set up the Bluetooth Connection
        // MAC needs to be uppercase
        MAC = MAC.toUpperCase();

        btDevice = btAdapter.getRemoteDevice(MAC);
        // UUID for Serial Port Protocol
        UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        // Connect based on API Version
        // Open the socket
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            // This is an API 10 command (Gingerbread); it will obviously fail on
            // an API 8 (Froyo) device.
            try {
                btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(mUUID);

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        } else {
            // This is an an API 5 call, so Froyo should handle this!
            try {
                btSocket = btDevice.createRfcommSocketToServiceRecord(mUUID);
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
        }

        // Lets connect the socket
        try {
            btSocket.connect();
            iStream = btSocket.getInputStream();
            oStream = btSocket.getOutputStream();


            // We are connected: Set up an executor thread to handle communications.
            // All communications should be setup as a runnable executed on this thread.
            commService = Executors.newSingleThreadExecutor();
            // Store the MAC address
            lastMAC = MAC;

            // Get Hardware / Firmware #
            byte[] readHWFW = {0x50, 0x02, 0x33, 0x00};
            byte[] HWFW = localComms.sdCallAndResponse(readHWFW);
            // Cancel if we don't get a good response
            if (HWFW == null) {
                // If we get a null here, assume there was an error
                resetFirmwareVersion();
                oStream.close();
                iStream.close();
                btSocket.close();
                return false;
            } else {
                try {
                    hardwareVersion  = (HWFW[0] & 0xff);
                    firmwareVersion  = (HWFW[1] & 0xff);
                    firmwareRevision = (HWFW[2] & 0xff);
                } catch (ArrayIndexOutOfBoundsException o) {
                    //
                    resetFirmwareVersion();
                    oStream.close();
                    iStream.close();
                    btSocket.close();
                    return false;
                }
            }


            // Make sure the Controller objects are initialized correctly
            boolean hwCheck = initializeHardware(hardwareVersion);
            // Calibration constants are read in initializeHardware()
            if (!hwCheck) {
                oStream.close();
                iStream.close();
                btSocket.close();
                return false;
            }


            // notify that we're ready
            isConnected = true;
            notifyDroneEventHandler(deConnected);
            notifyDroneEventListener(deConnected);
        } catch (IOException e) {
            e.printStackTrace();
            // If we didn't connect to the socket, the user is going to have a bad time.
            return false;
        }
        // If we made it this far, everything must have worked. Huzzah!
        return true;
    }

    @Override
    protected void closeSocket() {
        try {
            btSocket.close();
        } catch (IOException e) {
            //
        }
    }

    /**
     * Disconnect from a Sensordrone.
     *
     * @return Returns true upon successful disconnection.
     * @since 1.1.1
     */
    public boolean disconnect() {

        if (!isConnected) {
            return false;
        }
        if (commService.isShutdown()) {
            return false;
        }

        Runnable shutDownRunnable = new Runnable() {
            @Override
            public void run() {

                // Stop the communications queue.
                // I should be the last one in the queue.
                commService.shutdown();


                // Try to close any input streams
                try {
                    iStream.close();
                } catch (IOException e1) {
                    //e1.printStackTrace();
                }
                // Try to close and output streams
                try {
                    oStream.close();
                } catch (IOException e1) {
                    //e1.printStackTrace();
                }
                // Try and close the socket
                try {
                    btSocket.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                isConnected = false;
                resetFirmwareVersion();
                notifyDroneEventHandler(deDisconnected);
                notifyDroneEventListener(deDisconnected);
            }
        };

        commService.submit(shutDownRunnable);

        return true;
    }

    /**
     * Disconnects from the Sensordrone (run from the thread the method was called in).
     *
     * Useful if the job queue is overloaded, not responding, etc...
     *
     * This is mainly used as a "force" disconnect, as it will cause a race condition between
     * disconnecting and any jobs are in the queue. If you have ever disconnected from the Sensordrone
     * Control app, and wondered why the LED lights were still on --- this is why.
     *
     */
    public void disconnectNow() {

        commService.shutdown();

        try {
            commService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            commService.shutdownNow();
        }

        // Try to close any input streams
        try {
            iStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        // Try to close and output streams
        try {
            oStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        // Try and close the socket
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isConnected = false;
        resetFirmwareVersion();
        notifyDroneEventHandler(deDisconnected);
        notifyDroneEventListener(deDisconnected);

    }
}
