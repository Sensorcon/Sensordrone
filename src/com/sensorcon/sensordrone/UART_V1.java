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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * This class is used for serial communications over the external connector TX/RX pins
 */
public class UART_V1 extends DroneSensor {

    /**
     * An InputStream that USB Serial reads can be made from.
     */
    public PipedInputStream uartInputStream;

    /**
     * An OutputStream that loops back to usbUartInputStream for supplyig data
     */
    private PipedOutputStream uartInputDummy;

    /**
     * The size of a serial read (the firmware expects chunks of 32).
     */
    protected int UART_BUFFER_LENGTH = 32;

    /**
     * Used to set the buffer size of usbUartInputStream.
     * Currently big enough to hold 2 reads a second for 60 seconds.
     */
    //private int UART_STREAM_BUFFER_SIZE = UART_BUFFER_LENGTH * 2 * 60;
    // Note: Passing a buffer size to the Constructor is only supported in Android 9 and later.
    // To keep compatibility, this will need to be set to the default of 1024.
    // Later, we will make it choose based on OS version, for now, make it simple.
    private int UART_STREAM_BUFFER_SIZE = 1024;

    /**
     * Used to set the baud to 2400
     */
    private final byte BAUDRATE_2400 = 0x00;

    /**
     * Used to set the baud to 9600
     */
    private final byte BAUDRATE_9600 = 0x01;

    /**
     * Used to set the baudto 19200
     */
    private final byte BAUDRATE_19200 = 0x02;

    /**
     * Used to set the baud to 38400
     */
    private final byte BAUDRATE_38400 = 0x03;

    /**
     * Used to set the baud to 115200
     */
    private final byte BAUDRATE_115200 = 0x04;


    /**
     * Used to notify listeners
     */
    private DroneEventObject read = new DroneEventObject(DroneEventObject.droneEventType.UART_READ);


    /**
     * Sets the baud to 2400
     * @return Returns false if the Sensordrone is not connected or taking jobs.
     * Returns true if the command was successfully sent to the Sensordrone.
     */
    public boolean setBaudRate_2400() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable b_2400_Runnable = new Runnable() {
            public void run() {
                byte[] call = {0x50, 0x03, 0x26, BAUDRATE_2400, 0x00};
                sdCallAndResponse(call);
            }
        };
        try {
            myDrone.commService.submit(b_2400_Runnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Sets the baud to 9600
     * @return Returns false if the Sensordrone is not connected or taking jobs.
     * Returns true if the command was successfully sent to the Sensordrone.
     */
    public boolean setBaudRate_9600() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable b_9600_Runnable = new Runnable() {
            public void run() {
                byte[] call = {0x50, 0x03, 0x26, BAUDRATE_9600, 0x00};
                sdCallAndResponse(call);
            }
        };
        try {
            myDrone.commService.submit(b_9600_Runnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Sets the baud to 19200
     * @return Returns false if the Sensordrone is not connected or taking jobs.
     * Returns true if the command was successfully sent to the Sensordrone.
     */
    public boolean setBaudRate_19200() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable b_19200_Runnable = new Runnable() {
            public void run() {
                byte[] call = {0x50, 0x03, 0x26, BAUDRATE_19200, 0x00};
                sdCallAndResponse(call);
            }
        };
        try {
            myDrone.commService.submit(b_19200_Runnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Sets the baud to 38400
     * @return Returns false if the Sensordrone is not connected or taking jobs.
     * Returns true if the command was successfully sent to the Sensordrone.
     */
    public boolean setBaudRate_38400() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable b_38400_Runnable = new Runnable() {
            public void run() {
                byte[] call = {0x50, 0x03, 0x26, BAUDRATE_38400, 0x00};
                sdCallAndResponse(call);
            }
        };
        try {
            myDrone.commService.submit(b_38400_Runnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Sets the baud to 115200
     * @return Returns false if the Sensordrone is not connected or taking jobs.
     * Returns true if the command was successfully sent to the Sensordrone.
     */
    public boolean setBaudRate_115200() {
        if (!myDrone.isConnected) {
            return false;
        }
        Runnable b_115200_Runnable = new Runnable() {
            public void run() {
                byte[] call = {0x50, 0x03, 0x26, BAUDRATE_115200, 0x00};
                sdCallAndResponse(call);
            }
        };
        try {
            myDrone.commService.submit(b_115200_Runnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }


    /**
     * Read a data packet from the Sensordrone external RX pin.
     * @return Returns false if the Sensordrone is disconnected or not taking jobs.
     * Returns true if the command was successfully sent to the Sensordrone.
     */
    public boolean externalUartRead() {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable uartReadRunnalbe = new Runnable() {

            @Override
            public void run() {
                byte[] call = {0x50, 0x02, 0x25, 0x00};
                byte[] response = sdCallAndResponse(call);
                if (response != null) {
                    synchronized (uartInputStream) {
                        // Put the data into the array
                        myDrone.uartReadBuffer = ByteBuffer.wrap(response);
                        // Load it into the InputStream
                        try {
                            // We don't want to overflow the buffer!
                            int bytesLeft = UART_STREAM_BUFFER_SIZE - uartInputStream.available();
                            if (bytesLeft < UART_BUFFER_LENGTH) {
                                int needToRemove = UART_BUFFER_LENGTH - bytesLeft;
                                for (int i = 0; i < needToRemove; i++) {
                                    uartInputStream.read();
                                }
                            }
                            uartInputDummy.write(response);
                        } catch (IOException e) {
                            logger.infoLogger(TAG, "Failed to add data to stream", CoreDrone.DEBUG);
                        }
                    }

                    // Notify the listeners
                    myDrone.notifyDroneEventHandler(read);
                    myDrone.notifyDroneEventListener(read);
                }
            }
        };

        try {
            myDrone.commService.submit(uartReadRunnalbe);
        } catch (RejectedExecutionException e) {
            //
        }
        return true;
    }


    /**
     * Writes a data packet to the external TX pin.
     * @param data The data packet to send (max length of 32).
     * @return Returns false if the Sensordrone is not connected,
     * not executing jobs, or if the length of data[] is
     * greater than UART_BUFFER_LENGTH (currently 32). Returns true
     * if the command was successfully sent to the Sensordrone.
     */
    public boolean exernalUartWrite(byte[] data) {
        if (!myDrone.isConnected) {
            return false;
        }
        if (data.length > UART_BUFFER_LENGTH) {
            return false;
        }

        final ByteBuffer EXTERNAL_UART_WRITE_BUFFER;
        EXTERNAL_UART_WRITE_BUFFER = ByteBuffer.wrap(data);

        Runnable uartWriteRunnable = new Runnable() {

            @Override
            public void run() {
                // How many bytes are we sending?
                int dataLength = EXTERNAL_UART_WRITE_BUFFER.array().length;
                // Set up a buffer
                ByteBuffer callBuffer = ByteBuffer.allocate(dataLength + 4);
                // Set up the packet header / terminator
                byte[] first3 = {0x50, (byte) ((dataLength + 2) & 0x000000ff), 0x24};
                byte[] zero = {0x00};
                // Load it up
                callBuffer.put(first3);
                callBuffer.put(EXTERNAL_UART_WRITE_BUFFER.array());
                callBuffer.put(zero);
                // Send the packet
                sdCallAndResponse(callBuffer.array());
            }
        };

        try {
            myDrone.commService.submit(uartWriteRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * A method to write a byte[], and follow it immediately (after the designated delay) with a read
     * (blocks executing thread!).
     *
     * This is useful for working with (Serial) modules, where you send it a command
     * and expect a response based on that command, instead of parsing a Serial response asynchronously.
     *
     * @param write The byte[] to write
     * @param  msDelay after the write byte[] is sent, wait this long before reading and returning a response
     *
     * @return
     */
    public byte[] writeForRead(byte[] write, int msDelay) {
        // Some return values so we don't have to give back a null
        byte[] notConnected = {0x00};
        byte[] writeTooLong = {0x01};
        byte[] badWrite = {0x02};
        byte[] badRead = {0x03};
        byte[] badBlock = {0x04};




        // Make sure we're connected
        if (!myDrone.isConnected) {
            return notConnected;
        }
        // Make sure we don't write too much
        if (write.length > UART_BUFFER_LENGTH) {
            return writeTooLong;
        }

        // Set up the packet
        int dataLength = write.length;
        final ByteBuffer callBuffer = ByteBuffer.allocate(dataLength + 4);
        byte[] first3 = {0x50, (byte) ((dataLength + 2) & 0x000000ff), 0x24};
        byte[] zero = {0x00};
        callBuffer.put(first3);
        callBuffer.put(write);
        callBuffer.put(zero);

        // All communication should be done through the executor service!
        Callable<byte[]> writeCall = new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                byte[] response = sdCallAndResponse(callBuffer.array());
                return response;
            }
        };
        Future<byte[]> bgWriteCall = myDrone.commService.submit(writeCall);
        byte[] writeResponse;
        try {
            writeResponse = bgWriteCall.get();
        } catch (InterruptedException e) {
            return badBlock;
        } catch (ExecutionException e) {
            return badBlock;
        }

        // Make sure we got something back
        if (writeResponse == null) {
            return badWrite;
        }


        // Wait for any delay
        if (msDelay <= 0) {
            //
        }
        else {
            try {
                Thread.sleep(msDelay);
            } catch (InterruptedException e) {
                //
            }
        }

        // Now we want to to read the response
        Callable<byte[]> readCall = new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                byte[] read = {0x50, 0x02, 0x25, 0x00};
                byte[] response = sdCallAndResponse(read);
                return response;
            }
        };
        Future<byte[]> bgReadCall = myDrone.commService.submit(readCall);
        byte[] readResponse;
        try {
            readResponse = bgReadCall.get();
        } catch (InterruptedException e) {
            return badBlock;
        } catch (ExecutionException e) {
            return badBlock;
        }


        if (readResponse == null) {
            return badRead;
        } else {
            return readResponse;
        }
    }


    /**
     * Our default constructor
     * @param drone
     */
    public UART_V1(CoreDrone drone) {
        super(drone, "UART_V1");

          // Sett comment above about UART_STREAM_BUFFER_SIZE
//        uartInputStream = new PipedInputStream(UART_STREAM_BUFFER_SIZE);
        uartInputStream = new PipedInputStream();
        try {
            uartInputDummy = new PipedOutputStream(uartInputStream);
        } catch (IOException e) {
            logger.infoLogger(TAG, "Failed to connect stream", CoreDrone.DEBUG);
        }
    }



}
