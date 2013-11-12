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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.RejectedExecutionException;

/**
 * This class is used for serial communications over USB
 */
public class USB_UART_V1 extends DroneSensor {

    /**
     * An InputStream that USB Serial reads can be made from.
     */
    public PipedInputStream usbUartInputStream;

    /**
     * An OutputStream that loops back to usbUartInputStream for supplyig data
     */
    private PipedOutputStream usbUartInputDummy;

    /**
     * Used to notify listeners
     */
    private DroneEventObject read = new DroneEventObject(DroneEventObject.droneEventType.USB_UART_READ);

    /**
     * The size of a serial read (the firmware expects chunks of 32).
     */
    private int USB_UART_BUFFER_LENGTH = 32;

    /**
     * Used to set the buffer size of usbUartInputStream.
     * Currently big enough to hold 2 reads a second for 60 seconds.
     */
    //private int USB_UART_STREAM_BUFFER_SIZE = USB_UART_BUFFER_LENGTH * 2 * 60;
    // Note: Passing a buffer size to the Constructor is only supported in Android 9 and later.
    // To keep compatibility, this will need to be set to the default of 1024.
    // Later, we will make it choose based on OS version, for now, make it simple.
    private int USB_UART_STREAM_BUFFER_SIZE = 1024;

    /**
     * Our default constructor
     * @param drone
     */
    public USB_UART_V1(CoreDrone drone) {
        super(drone, "USB_UART_V1");
        // See note above about USB_UART_STREAM_BUFFER_SIZE
//        usbUartInputStream = new PipedInputStream(USB_UART_STREAM_BUFFER_SIZE);
        usbUartInputStream = new PipedInputStream();
        try {
            usbUartInputDummy = new PipedOutputStream(usbUartInputStream);
        } catch (IOException e) {
            logger.infoLogger(TAG, "Failed to connect stream", CoreDrone.DEBUG);
        }
    }




    /**
     * Read a data packet from the USB host
     * @return Returns false if the Sensordrone is not connected or not accepting jobs.
     * Returns true if the command was successfully sent to the Sensordrone.
     */
    public boolean readUSBUart() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable USBUartReadRunnable = new Runnable() {

            @Override
            public void run() {

                byte[] call = {0x50, 0x02, 0x2b, 0x00};
                byte[] response = sdCallAndResponse(call);

                if (response != null) {
                    // Put the response into the array
                    myDrone.usbUartReadBuffer = ByteBuffer.wrap(response);
                    // Also load it into our PipedInputStream
                    synchronized (usbUartInputStream) {
                        try {
                            // We don't want to overflow the buffer!
                            int bytesLeft = USB_UART_STREAM_BUFFER_SIZE - usbUartInputStream.available();
                            if (bytesLeft < USB_UART_BUFFER_LENGTH) {
                                int needToRemove = USB_UART_BUFFER_LENGTH - bytesLeft;
                                // Clear out the needed space
                                for (int i = 0; i < needToRemove; i++) {
                                    usbUartInputStream.read();
                                }
                            }
                            // Load it in
                            usbUartInputDummy.write(response);
                        } catch (IOException e) {
                            logger.infoLogger(TAG, "Failed to add data to stream", CoreDrone.DEBUG);
                        }
                    }

                    // Notify our listeners
                    myDrone.notifyDroneEventListener(read);
                    myDrone.notifyDroneEventHandler(read);
                }

            }
        };

        try {
            myDrone.commService.submit(USBUartReadRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Writes a data packet to the USB Host
     * @param data The data packet to send (max length of 32).
     * @return Returns false if the Sensordrone is not connected,
     * not executing jobs, or if the length of data[] is
     * greater than USB_UART_BUFFER_LENGTH (currently 32). Returns true
     * if the command was successfully sent to the Sensordrone.
     */
    public boolean USBUartWrite(byte[] data) {
        if (!myDrone.isConnected) {
            return false;
        }
        if (data.length > USB_UART_BUFFER_LENGTH) {
            return false;
        }

        final  ByteBuffer USB_UART_WRITE_BUFFER;
        USB_UART_WRITE_BUFFER = ByteBuffer.wrap(data);

        Runnable uartWriteRunnable = new Runnable() {

            @Override
            public void run() {
                // How many bytes are we sending?
                int dataLength = USB_UART_WRITE_BUFFER.array().length;
                // Set up a buffer
                ByteBuffer callBuffer = ByteBuffer.allocate(dataLength + 4);
                // Set up our packet header
                byte[] first3 = {0x50, (byte) ((dataLength + 2) & 0x000000ff), 0x2a};
                // Set up out packet terminator
                byte[] zero = {0x00};
                // Load it up
                callBuffer.put(first3);
                callBuffer.put(USB_UART_WRITE_BUFFER.array());
                callBuffer.put(zero);
                // Make the call
                sdCallAndResponse(callBuffer.array());

            }
        };

        // Submit the job
        try {
            myDrone.commService.submit(uartWriteRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


}
