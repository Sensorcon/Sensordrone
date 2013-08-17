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

import java.util.EventListener;

/**
 * The DroneEventListener is used to perform actions
 * when the Sensordrone has successfully made a measurement.
 *
 * It has a block of numerous status events to be triggered. If you only want to work with a few particular events,
 * feel free to check out DroneEventHandler instead
 *
 * @see DroneEventHandler
 * @since 1.1.1
 */
public interface DroneEventListener extends EventListener {

    /**
     * Things to do when the Capacitance measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void capacitanceMeasured(DroneEventObject event);

    /**
     * Things to do when the External ADC measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void adcMeasured(DroneEventObject event);

    /**
     * Things to do when the RGBC measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void rgbcMeasured(DroneEventObject event);

    /**
     * Things to do when the Pressure measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void pressureMeasured(DroneEventObject event);

    /**
     * Things to do when the Altitude measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void altitudeMeasured(DroneEventObject event);

    /**
     * Things to do when the IR Temperature measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void irTemperatureMeasured(DroneEventObject event);

    /**
     * Things to do when the Humidity measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void humidityMeasured(DroneEventObject event);

    /**
     * Things to do when the Temperature measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void temperatureMeasured(DroneEventObject event);

    /**
     * Things to do when the (Reducing) General Gas measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void reducingGasMeasured(DroneEventObject event);

    /**
     * Things to do when the (Oxidizing) General Gas  measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void oxidizingGasMeasured(DroneEventObject event);

    /**
     * Things to do when the Precision Gas measurement is ready.
     *
     * @param event
     * @since 1.1.1
     */
    public void precisionGasMeasured(DroneEventObject event);

    /**
     * Things to do when the UART is read
     *
     * @param event
     * @since 1.1.1
     */
    public void uartRead(DroneEventObject event);

    /**
     * Things to do when the I2C is read
     *
     * @param event
     * @since 1.1.1
     */
    public void i2cRead(DroneEventObject event);

    /**
     * Things to do when the USB UART is read
     *
     * @param event
     * @since 1.1.1
     */
    public void usbUartRead(DroneEventObject event);

    /**
     * A catch for a custom event if you need one
     *
     * @param event
     * @since 1.1.1
     */
    public void customEvent(DroneEventObject event);

    /**
     * Things to do when a Sensordrone connection is made.
     *
     * @param event
     * @since 1.1.1
     */
    public void connectEvent(DroneEventObject event);

    /**
     * Things to do when a Sensordrone is disconnected.
     *
     * @param event
     * @since 1.1.1
     */
    public void disconnectEvent(DroneEventObject event);

    /**
     * Things to do when a Sensordrone connection is lost.
     *
     * @param event
     * @since 1.1.1
     */
    public void connectionLostEvent(DroneEventObject event);

    /**
     * This is only used if the event is unknown (i.e. a typo, or other error).
     * Can be useful for checking if customEvent is working properly
     *
     * @param event
     * @since 1.1.1
     */
    public void unknown(DroneEventObject event);

}
