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

import java.util.EventObject;

/**
 * A custom class used for EventListener notifications (extends EventObject)
 */
public class DroneEventObject extends EventObject {

    /**
     * And enum of all the allowed event types.
     * If you're not on the list, then you're not getting in ;-)
     */
    public static enum droneEventType {
        CAPCACITANCE_MEASURED,
        CAPACITANCE_ENABLED,
        CAPACITANCE_DISABLED,
        CAPACITANCE_STATUS_CHECKED,

        ADC_MEASURED,
        ADC_ENABLED,
        ADC_DISABLED,
        ADC_STATUS_CHECKED,

        RGBC_MEASURED,
        RGBC_ENABLED,
        RGBC_DISABLED,
        RGBC_STATUS_CHECKED,

        PRESSURE_MEASURED,
        PRESSURE_ENABLED,
        PRESSURE_DISABLED,
        PRESSURE_STATUS_CHECKED,

        ALTITUDE_MEASURED,
        ALTITUDE_ENABLED,
        ALTITUDE_DISABLED,
        ALTITUDE_STATUS_CHECKED,

        IR_TEMPERATURE_MEASURED,
        IR_TEMPERATURE_ENABLED,
        IR_TEMPERATURE_DISABLED,
        IR_TEMPERATURE_STATUS_CHECKED,

        HUMIDITY_MEASURED,
        HUMIDITY_ENABLED,
        HUMIDITY_DISABLED,
        HUMIDITY_STATUS_CHECKED,

        TEMPERATURE_MEASURED,
        TEMPERATURE_ENABLED,
        TEMPERATURE_DISABLED,
        TEMPERATURE_STATUS_CHECKED,

        REDUCING_GAS_MEASURED,
        REDUCING_GAS_ENABLED,
        REDUCING_GAS_DISABLED,
        REDUCING_GAS_STATUS_CHECKED,

        OXIDIZING_GAS_MEASURED,
        OXIDIZING_GAS_ENABLED,
        OXIDIZING_GAS_DISABLED,
        OXIDIZING_GAS_STATUS_CHECKED,

        PRECISION_GAS_MEASURED,
        PRECISION_GAS_ENABLED,
        PRECISION_GAS_DISABLED,
        PRECISION_GAS_STATUS_CHECKED,

        UART_READ,
        USB_UART_READ,

        CONNECTED,
        DISCONNECTED,
        CONNECTION_LOST,

        BATTERY_VOLTAGE_MEASURED,
        CHARGING_STATUS,
        LOW_BATTERY,

        CUSTOM_EVENT,
        CUSTOM_STATUS


    }

    /**
     * Our default Constructor; only allows items that are in the droneEventType enum
     *
     * @param eventType
     *
     * @see droneEventType
     */
    public DroneEventObject(droneEventType eventType) {
        super(eventType);
    }

    /**
     * A method to quickly check if a triggered event matches a particular item in the droneEventType enum
     *
     * @param type
     * @return True if the triggered droneEventType matches the passed droneEventType, otherwise false;
     *
     * @see droneEventType
     *
     */
    public boolean matches(droneEventType type) {
        return this.getSource().toString().equals(type.toString());
    }

}
