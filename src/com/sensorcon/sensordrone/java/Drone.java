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
package com.sensorcon.sensordrone.java;

import com.sensorcon.sensordrone.CoreDrone;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * The Drone class allows you to control a Sensordrone and interact with the on-board sensors via Java
 *
 * @see CoreDrone
 */
public class Drone extends CoreDrone {

    // Java specific stuff here



    /*
     * Bluetooth communication related items
     */
    protected StreamConnection btSocket;
    protected InputStream iStream;
    protected OutputStream oStream;


    /**
     * Connect to a Sensordrone via Bluetooth.
     * @param MAC The MAC address of the Sensordrone to connect to.
     * @return Returns true upon successful connection; false otherwise.
     * @since 1.2.0
     */
    public boolean btConnect(String MAC) {
        // Set up the Bluetooth Connection
        // MAC needs to be uppercase
        MAC = MAC.toUpperCase();
        // No colons
        MAC = MAC.replaceAll(":", "");

        String connectURL = "btspp://" + MAC + ":1";

        try {
            btSocket = (StreamConnection) Connector.open(connectURL);
        } catch (IOException e) {
            return false;
        }
        // Lets connect the socket
        try {
            iStream = btSocket.openDataInputStream();
            oStream = btSocket.openDataOutputStream();

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
                    hardwareVersion = (int)(HWFW[0]);
                    firmwareVersion = (int)(HWFW[1]);
                    firmwareRevision = (int)(HWFW[2]);
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
            if (!hwCheck) {
                oStream.close();
                iStream.close();
                btSocket.close();
                return false;
            }


            // notify that we're ready
            isConnected = true;
            notifyDroneEventListener(deConnected);
            notifyDroneEventHandler(deConnected);
        } catch (IOException e) {
            e.printStackTrace();
            // If we didn't connect to the socket, the user is going to have a bad time.
            return false;
        }
        // If we made it this far, everything must have worked. Huzzah!
        return true;
    }


    /**
     * Disconnect from a Sensordrone.
     * @return Returns true upon successful disconnection.
     * @since 1.2.0
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

    /**
     * Closing a socket is implementation dependent, but we need to call it
     * from a core class that doesn't know if it's Java or Android
     */
    @Override
    protected void closeSocket() {
        try {
            btSocket.close();
        } catch (IOException e) {
            //
        }
    }

    public Drone() {
        super(new JLogger());
    }
}
