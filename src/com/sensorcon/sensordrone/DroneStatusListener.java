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
 * The DroneStatusListener is used to perform actions
 * when the Sensordrone has successfully checked a status (i.e. is a particular sensor enabled or disabled).
 *
 * It has a block of numerous status events to be triggered. If you only want to work with a few particular events,
 * feel free to check out DroneEventHandler
 *
 * @see DroneEventHandler
 * @since 1.1.1
 */
public interface DroneStatusListener extends EventListener {

    /**
     * Things to do when the enable/disable status of the Capacitance sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void capacitanceStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the ADC sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void adcStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the RGBC color sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void rgbcStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the Pressure sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void pressureStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the Altitude (pseudo) sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void altitudeStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the IR Thermometer sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void irStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the Humidity sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void humidityStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the Temperature sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void temperatureStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the oxidizing gas sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void oxidizingGasStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the reducing gas sensor has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void reducingGasStatus(DroneEventObject status);

    /**
     * Things to do when the enable/disable status of the Precision Gas sensor has been checked
     *
     * @param status
     * @since 1.1.1
     */
    public void precisionGasStatus(DroneEventObject status);

    /**
     * Things to do when the Sensordrone's battery voltage has been checked. This method will also
     * trigger the lowBatteryStatus event if the battery voltage is below 3.25 Volts.
     *
     * @param status
     * @since 1.1.1
     */
    public void batteryVoltageStatus(DroneEventObject status);

    /**
     * Things to do when the Sensordrone's charging status has been checked.
     *
     * @param status
     * @since 1.1.1
     */
    public void chargingStatus(DroneEventObject status);


    /**
     * A custom status for you to use with this listener.
     *
     * @param status
     * @since 1.1.1
     */
    public void customStatus(DroneEventObject status);

    /**
     * This is only used if the status event is unknown (i.e. a typo, or other error).
     * Can be useful for checking if customStatus is working properly
     *
     * @param status
     * @since 1.1.1
     */
    public void unknownStatus(DroneEventObject status);

    /**
     * Things to do when the Sensordrone's battery is low (below 3.25 Volts). This event is currently
     * triggered by the measureBatteryVoltage method and the setLED methods.
     *
     * @param status
     * @since 1.1.1
     */
    public void lowBatteryStatus(DroneEventObject status);

}