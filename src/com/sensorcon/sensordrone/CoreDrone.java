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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.util.EventListener;
import java.util.concurrent.ExecutorService;


/**
 * The core class for the Sensordrone.
 *
 * This class allows you to interact with your Sensordrone and its sensor.
 *
 * This is an abstract class that is fairly implementation independent. It has to be
 * extended by a class that is implementation dependent; all connection methods would
 * be set up there.
 *
 * @author Sensorcon, Inc.
 * @version 1.1.1 Initial Release
 * @since 1.1.1
 */
public abstract class CoreDrone {

    /**
     * A boolean to toggle debug messages. This is not modifiable by users.
     */
    public final static boolean DEBUG = false;

    // The max hardware version of the Sensordrone this library will accommodate
    private static int apiHardwareVersion = 1;
    // The max version of the Sensordrone firmware this library will accommodate
    private static int apiFirwareVersion = 2;
    // The current revision of this library
    private static int apiLibraryRevision = 2;

    /**
     * This resets the values read from the Sensordrone about it's Firmware version
     */
    protected void resetFirmwareVersion() {
        hardwareVersion = 0;
        firmwareVersion = 0;
        firmwareRevision = 0;
    }

    //////////////////////////////////////////

    /**
     * The version of the API Library being used
     */
    public String apiLibraryVersion = String.valueOf(apiHardwareVersion) + "." +
            String.valueOf(apiFirwareVersion) + "." +
            String.valueOf(apiLibraryRevision);

    /**
     * The Sensordrone's hardware version. This is updated on connect
     * and is set to 0 when not connected.
     */
    public int hardwareVersion = 0;
    /**
     * The Sensordrone's firmware version. This is updated on connect
     * and is set to 0 when not connected.
     */
    public int firmwareVersion = 0;
    /**
     * The Sensordrone's firmware revision number. This is updated on connect
     * and is set to 0 when not connected.
     */
    public int firmwareRevision = 0;

    /**
     * A string used for logging
     */
    protected static final String TAG = "CoreDrone";
    /**
     * An object used for debug logging
     */
    protected Logger logger;
    /**
     * Used by this class to send/receive data such as firmware version, calibration data, etc...
     */
    protected DroneSensor localComms;


    /**
     * Used to notify the a connection was established
     */
    public DroneEventObject deConnected = new DroneEventObject(DroneEventObject.droneEventType.CONNECTED);
    /**
     * Used to notify that the device was disconnected
     */
    public DroneEventObject deDisconnected = new DroneEventObject(DroneEventObject.droneEventType.DISCONNECTED);

    /*
     * Controllers.
     *
     * There should be one for each internal sensor object
     * we want to work with.
     *
     * THESE CONTROL ALL OF THE COMMANDS!!!
     *
     * We typecast them to specific classes that extend DroneSensor once we know what
     * hardware version we are upon connection.
     */
    private DroneSensor ADC_CONTROLLER;
    private DroneSensor CAPACITANCE_CONTROLLER;
    private DroneSensor RED_OX_CONTROLLER;
    private DroneSensor HUMIDITY_CONTROLLER;
    private DroneSensor IR_CONTROLLER;
    private DroneSensor LED_CONTROLLER;
    private DroneSensor POWER_CONTROLLER;
    private DroneSensor PRECISION_GAS_CONTROLLER;
    private DroneSensor PRESSURE_CONTROLLER;
    private DroneSensor RGBC_CONTROLLER;
    private DroneSensor UART_CONTROLLER;
    private DroneSensor USB_UART_CONTROLLER;
//	private DroneSensor I2C_CONTROLLER;


    // The socket closing will be platform dependent
    protected abstract void closeSocket();

    /**
     * The input stream for our connection
     */
    protected InputStream iStream;
    /**
     * The output stream for our connection
     */
    protected OutputStream oStream;
    /**
     * Used to process all commands. Communications can be handled
     * in the background in a First-In First-Out manner.
     */
    protected ExecutorService commService;

    /*
     * Our ListenerLists
     */
    private EventListenerList droneEventListenerList;
    private EventListenerList droneStatusListenerList;
    private EventListenerList droneListenerList;



	
	/*
     * INTS for the quickSystem
	 */
    /**
     * The quickSystem value to trigger Altitude properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_ALTITUDE = 0;
    /**
     * The quickSystem value to trigger Capacitance properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_CAPACITANCE = 1;
    /**
     * The quickSystem value to trigger Humidity properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_HUMIDITY = 2;
    /**
     * The quickSystem value to trigger IR Temperature properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_IR_TEMPERATURE = 3;
    /**
     * The quickSystem value to trigger Oxidizing Gas properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_OXIDIZING_GAS = 4;
    /**
     * The quickSystem value to trigger Precision Gas properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_PRECISION_GAS = 5;
    /**
     * The quickSystem value to trigger Pressure properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_PRESSURE = 6;
    /**
     * The quickSystem value to trigger Reducing Gas properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_REDUCING_GAS = 7;
    /**
     * The quickSystem value to trigger RGBC properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_RGBC = 8;
    /**
     * The quickSystem value to trigger Temperature properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_TEMPERATURE = 9;
    /**
     * The quickSystem value to trigger External ADC properties.
     *
     * @since 1.1.1
     */
    public final static int QS_TYPE_ADC = 10;


    /**
     * The Enable part of the quickSystem
     * <p/>
     * The will call the appropriate enable method for the designated
     * sensor QS_TYPE
     *
     * @param QS_TYPE
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean quickEnable(int QS_TYPE) {
        if (!isConnected) {
            return false;
        }
        if (QS_TYPE == QS_TYPE_ALTITUDE) {
            return enableAltitude();
        } else if (QS_TYPE == QS_TYPE_CAPACITANCE) {
            return enableCapacitance();
        } else if (QS_TYPE == QS_TYPE_HUMIDITY) {
            return enableHumidity();
        } else if (QS_TYPE == QS_TYPE_IR_TEMPERATURE) {
            return enableIRTemperature();
        } else if (QS_TYPE == QS_TYPE_OXIDIZING_GAS) {
            return enableOxidizingGas();
        } else if (QS_TYPE == QS_TYPE_PRECISION_GAS) {
            return enablePrecisionGas();
        } else if (QS_TYPE == QS_TYPE_PRESSURE) {
            return enablePressure();
        } else if (QS_TYPE == QS_TYPE_REDUCING_GAS) {
            return enableReducingGas();
        } else if (QS_TYPE == QS_TYPE_RGBC) {
            return enableRGBC();
        } else if (QS_TYPE == QS_TYPE_TEMPERATURE) {
            return enableTemperature();
        } else if (QS_TYPE == QS_TYPE_ADC) {
            return enableADC();
        }

        return false;
    }

    /**
     * The Disable part of the quickSystem
     * <p/>
     * The will call the appropriate disable method for the designated
     * sensor QS_TYPE
     *
     * @param QS_TYPE
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean quickDisable(int QS_TYPE) {
        if (!isConnected) {
            return false;
        }

        if (QS_TYPE == QS_TYPE_ALTITUDE) {
            return disableAltitude();
        } else if (QS_TYPE == QS_TYPE_CAPACITANCE) {
            return disableCapacitance();
        } else if (QS_TYPE == QS_TYPE_HUMIDITY) {
            return disableHumidity();
        } else if (QS_TYPE == QS_TYPE_IR_TEMPERATURE) {
            return disableIRTemperature();
        } else if (QS_TYPE == QS_TYPE_OXIDIZING_GAS) {
            return disableOxidizingGas();
        } else if (QS_TYPE == QS_TYPE_PRECISION_GAS) {
            return disablePrecisionGas();
        } else if (QS_TYPE == QS_TYPE_PRESSURE) {
            return disablePressure();
        } else if (QS_TYPE == QS_TYPE_REDUCING_GAS) {
            return disableReducingGas();
        } else if (QS_TYPE == QS_TYPE_RGBC) {
            return disableRGBC();
        } else if (QS_TYPE == QS_TYPE_TEMPERATURE) {
            return disableTemperature();
        } else if (QS_TYPE == QS_TYPE_ADC) {
            return disableADC();
        }

        return false;
    }

    /**
     * The Measure part of the quickSystem
     * <p/>
     * The will call the appropriate measure method for the designated
     * sensor QS_TYPE
     *
     * @param QS_TYPE
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean quickMeasure(int QS_TYPE) {
        if (!isConnected) {
            return false;
        }

        if (QS_TYPE == QS_TYPE_ALTITUDE) {
            return measureAltitude();
        } else if (QS_TYPE == QS_TYPE_CAPACITANCE) {
            return measureCapacitance();
        } else if (QS_TYPE == QS_TYPE_HUMIDITY) {
            return measureHumidity();
        } else if (QS_TYPE == QS_TYPE_IR_TEMPERATURE) {
            return measureIRTemperature();
        } else if (QS_TYPE == QS_TYPE_OXIDIZING_GAS) {
            return measureOxidizingGas();
        } else if (QS_TYPE == QS_TYPE_PRECISION_GAS) {
            return measurePrecisionGas();
        } else if (QS_TYPE == QS_TYPE_PRESSURE) {
            return measurePressure();
        } else if (QS_TYPE == QS_TYPE_REDUCING_GAS) {
            return measureReducingGas();
        } else if (QS_TYPE == QS_TYPE_RGBC) {
            return measureRGBC();
        } else if (QS_TYPE == QS_TYPE_TEMPERATURE) {
            return measureTemperature();
        } else if (QS_TYPE == QS_TYPE_ADC) {
            return measureExternalADC();
        }

        return false;
    }

    /**
     * The Status part of the quickSystem
     * <p/>
     * The will call the appropriate status method for the designated
     * sensor QS_TYPE
     *
     * @param QS_TYPE
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean quickStatus(int QS_TYPE) {
        if (!isConnected) {
            return false;
        }

        if (QS_TYPE == QS_TYPE_ALTITUDE) {
            return altitudeStatus;
        } else if (QS_TYPE == QS_TYPE_CAPACITANCE) {
            return capacitanceStatus;
        } else if (QS_TYPE == QS_TYPE_HUMIDITY) {
            return humidityStatus;
        } else if (QS_TYPE == QS_TYPE_IR_TEMPERATURE) {
            return irTemperatureStatus;
        } else if (QS_TYPE == QS_TYPE_OXIDIZING_GAS) {
            return oxidizingGasStatus;
        } else if (QS_TYPE == QS_TYPE_PRECISION_GAS) {
            return precisionGasStatus;
        } else if (QS_TYPE == QS_TYPE_PRESSURE) {
            return pressureStatus;
        } else if (QS_TYPE == QS_TYPE_REDUCING_GAS) {
            return reducingGasStatus;
        } else if (QS_TYPE == QS_TYPE_RGBC) {
            return rgbcStatus;
        } else if (QS_TYPE == QS_TYPE_TEMPERATURE) {
            return temperatureStatus;
        } else if (QS_TYPE == QS_TYPE_ADC) {
            return adcStatus;
        }

        return false;
    }

	/*
     * Temperature
	 */
    /**
     * The measured temperature in Celsius.
     *
     * @since 1.1.1
     */
    public float temperature_Celsius;
    /**
     * The measured temperature in Fahrenheit.
     *
     * @since 1.1.1
     */
    public float temperature_Fahrenheit;
    /**
     * The measured temperature in Kelvin.
     *
     * @since 1.1.1
     */
    public float temperature_Kelvin;
    /**
     * The enabled/disabled status for measuring Temperature
     *
     * @since 1.1.1
     */
    public boolean temperatureStatus;

    /**
     * Enables measuring of temperature.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableTemperature() {

        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).enableTemperature();
        }
        return false;
    }

    /**
     * Disables measuring of temperature
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableTemperature() {

        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).disableTemperature();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status of measuring temperature
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfTemperature() {
        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).status();
        }
        return false;
    }

    /**
     * Measures the current temperature.
     * <p/>
     * Updates temperature_Celsius, temperature_Fahrenheit, and temperature_Kelvin.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureTemperature() {
        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).measureTemperature();
        }
        return false;
    }

	/*
     * Humidity
	 */
    /**
     * The measured relative percent humidity.
     *
     * @since 1.1.1
     */
    public float humidity_Percent;
    /**
     * The enabled/disabled status for measuring humidity.
     *
     * @since 1.1.1
     */
    public boolean humidityStatus;

    /**
     * Enables measuring of humidity.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableHumidity() {

        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).enable();
        }
        return false;
    }

    /**
     * Disables measuring of humidity.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableHumidity() {

        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).disable();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring humidity.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfHumidity() {
        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).temperatureStatus();
        }
        return false;
    }

    /**
     * Measures the humidity. Updates humidity_Percent.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureHumidity() {
        if (hardwareVersion == 1) {
            return ((Humidity_V1) HUMIDITY_CONTROLLER).measure();
        }

        return false;
    }

	/*
     * Pressure
	 */
    /**
     * The measured pressure in Atmospheres.
     *
     * @since 1.1.1
     */
    public float pressure_Atmospheres;
    /**
     * The measured pressure in Pascals.
     *
     * @since 1.1.1
     */
    public float pressure_Pascals;
    /**
     * The measured pressure in Torr.
     *
     * @since 1.1.1
     */
    public float pressure_Torr;
    /**
     * The enabled/disabled status for measuring pressure.
     *
     * @since 1.1.1
     */
    public boolean pressureStatus;

    /**
     * Enables measuring of pressure.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enablePressure() {

        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).enable();
        }
        return false;
    }

    /**
     * Disables measuring of pressure.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disablePressure() {
        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).disable();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring pressure.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfPressure() {
        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).status();
        }
        return false;
    }

    /**
     * Measures the current pressure.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measurePressure() {
        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).measurePressure();
        }
        return false;
    }

	/*
     * Altitude
	 */
    /**
     * The measured altitude in feet.
     *
     * @since 1.1.1
     */
    public float altitude_Feet;
    /**
     * The measured altitude in meters.
     *
     * @since 1.1.1
     */
    public float altitude_Meters;
    /**
     * The enabled/disabled status for measuring altitude.
     *
     * @since 1.1.1
     */
    public boolean altitudeStatus;

    /**
     * Enables measuring of altitude.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableAltitude() {
        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).enableAltitude();
        }
        return false;
    }

    /**
     * Disables measuring of altitude.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableAltitude() {
        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).disableAltitude();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status of measuring altitude.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfAltitude() {
        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).statusAltitude();
        }
        return false;
    }

    /**
     * Measure altitude. Updates altitude_Feet, and altitude_Meters.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureAltitude() {
        if (hardwareVersion == 1) {
            return ((Pressure_V1) PRESSURE_CONTROLLER).measureAltitude();
        }
        return false;
    }

	/*
     * RGBC
	 */
    /**
     * The measured red channel of RGBC.
     *
     * @since 1.1.1
     */
    public float rgbcRedChannel;
    /**
     * The measured green channel of RGBC.
     *
     * @since 1.1.1
     */
    public float rgbcGreenChannel;
    /**
     * The measured blue channel of RGBC.
     *
     * @since 1.1.1
     */
    public float rgbcBlueChannel;
    /**
     * The measured clear channel of RGBC.
     *
     * @since 1.1.1
     */
    public float rgbcClearChannel;
    /**
     * The measured illuminance in Lux for broadband light
     *
     * @since 1.1.1
     */
    public float rgbcLux;
    /**
     * The measured color temperature in Kelvin for broadband light
     *
     * @since 1.1.1
     */
    public float rgbcColorTemperature;
    /**
     * The enabled/disabled status for measuring RGBC.
     *
     * @since 1.1.1
     */
    public boolean rgbcStatus;

    /**
     * Enables measuring RGBC Color and illuminance.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableRGBC() {
        if (hardwareVersion == 1) {
            return ((RGBC_V1) RGBC_CONTROLLER).enable();
        }
        return false;
    }

    /**
     * Disables measuring RGBC.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableRGBC() {
        if (hardwareVersion == 1) {
            return ((RGBC_V1) RGBC_CONTROLLER).disable();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring RGBC.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfRGBC() {
        if (hardwareVersion == 1) {
            return ((RGBC_V1) RGBC_CONTROLLER).status();
        }
        return false;
    }

    /**
     * Measures RGBC channels, color temperature, and illuminance.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureRGBC() {
        if (hardwareVersion == 1) {
            return ((RGBC_V1) RGBC_CONTROLLER).measure();
        }
        return false;
    }

	/*
	 * Capacitance
	 */
    /**
     * The measured capacitance in femtoFarad.
     *
     * @since 1.1.1
     */
    public float capacitance_femtoFarad;
    /**
     * The current enabled/disabled status for measuring capacitance.
     *
     * @since 1.1.1
     */
    public boolean capacitanceStatus;

    /**
     * Enables measuring capacitance.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableCapacitance() {
        if (hardwareVersion == 1) {
            return ((Capacitance_V1) CAPACITANCE_CONTROLLER).enable();
        }
        return false;
    }

    /**
     * Disables measuring capacitance.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableCapacitance() {
        if (hardwareVersion == 1) {
            return ((Capacitance_V1) CAPACITANCE_CONTROLLER).disable();
        }
        return false;
    }

    /**
     * Checks the enabled/disabled status for measuring capacitance.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfCapacitance() {
        if (hardwareVersion == 1) {
            return ((Capacitance_V1) CAPACITANCE_CONTROLLER).status();
        }
        return false;
    }

    /**
     * Measures capacitance.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureCapacitance() {
        if (hardwareVersion == 1) {
            return ((Capacitance_V1) CAPACITANCE_CONTROLLER).measure();
        }
        return false;
    }
	
	/*
	 * Oven
	 */
    /**
     * The measured oxidizing gas value (Ohm).
     *
     * @since 1.1.1
     */
    public float oxidizingGas_Ohm;
    /**
     * The measured reducing gas value (Ohm).
     *
     * @since 1.1.1
     */
    public float reducingGas_Ohm;
    /**
     * The current enabled/disable status for measuring oxidizing gas.
     *
     * @since 1.1.1
     */
    public boolean oxidizingGasStatus;
    /**
     * The current enabled/disable status for measuring reducing gas.
     *
     * @since 1.1.1
     */
    public boolean reducingGasStatus;

    /**
     * Enables measuring of oxidizing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableOxidizingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).enableOxidizingGas();
        }
        return false;
    }

    /**
     * Disables measuring of oxidizing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableOxidizingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).disableOxidizingGas();
        }
        return false;
    }

    /**
     * Enables measuring of reducing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableReducingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).enableReducingGas();
        }
        return false;
    }

    /**
     * Disables measuring of reducing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableReducingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).disableReducingGas();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring oxidizing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfOxidizingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).oxidizingStatus();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring reducing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfReducingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).reducingStatus();
        }
        return false;
    }


    /**
     * Measures oxidizing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureOxidizingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).measureOX();
        }
        return false;
    }

    /**
     * Measures reducing gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureReducingGas() {
        if (hardwareVersion == 1) {
            return ((GeneralGas_V1) RED_OX_CONTROLLER).measureRED();
        }
        return false;
    }
	
	/*
	 * Precision Gas
	 */
    /**
     * The measured gas value corresponding to parts per million of carbon monoxide.
     *
     * @since 1.1.1
     */
    public float precisionGas_ppmCarbonMonoxide;
    /**
     * The current enabled/disabled status
     *
     * @since 1.1.1
     */
    public boolean precisionGasStatus;

    /**
     * Enables the measuring of gas with the precision gas sensor.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enablePrecisionGas() {
        if (hardwareVersion == 1) {
            return ((PrecisionGas_V1) PRECISION_GAS_CONTROLLER).enable();
        }
        return false;
    }

    /**
     * Disables the measuring of gas with the precision gas sensor.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disablePrecisionGas() {
        if (hardwareVersion == 1) {
            return ((PrecisionGas_V1) PRECISION_GAS_CONTROLLER).disable();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring (precision) gas.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfPrecisionGas() {
        if (hardwareVersion == 1) {
            return ((PrecisionGas_V1) PRECISION_GAS_CONTROLLER).checkStatus();
        }
        return false;
    }

    /**
     * Measures gas with the precision gas sensor.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measurePrecisionGas() {
        if (hardwareVersion == 1) {
            return ((PrecisionGas_V1) PRECISION_GAS_CONTROLLER).measure();
        }
        return false;
    }
	
	
	/*
	 * IR
	 */
    /**
     * The measured IR temperature in Celsius.
     *
     * @since 1.1.1
     */
    public float irTemperature_Celsius;
    /**
     * The measured IR temperature in Fahrenheit.
     *
     * @since 1.1.1
     */
    public float irTemperature_Fahrenheit;
    /**
     * The measured IR temperature in Kelvin.
     *
     * @since 1.1.1
     */
    public float irTemperature_Kelvin;
    /**
     * The current enabled/disabled status for measuring IR temperature.
     *
     * @since 1.1.1
     */
    public boolean irTemperatureStatus;

    /**
     * Enables measuring of IR temperature.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableIRTemperature() {
        if (hardwareVersion == 1) {
            return ((IRThermometer_V1) IR_CONTROLLER).enable();
        }
        return false;
    }

    /**
     * Disables measuring of IR temperature.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableIRTemperature() {
        if (hardwareVersion == 1) {
            return ((IRThermometer_V1) IR_CONTROLLER).disable();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring IR temperature.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfIRTemperature() {
        if (hardwareVersion == 1) {
            return ((IRThermometer_V1) IR_CONTROLLER).status();
        }
        return false;
    }

    /**
     * Measures IR temperature.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureIRTemperature() {
        if (hardwareVersion == 1) {
            return ((IRThermometer_V1) IR_CONTROLLER).measure();
        }
        return false;
    }
	
	/*
	 * LEDs
	 */

    /**
     * Set the Sensordrone's left LED color.
     *
     * @param RED   0-255
     * @param GREEN 0-255
     * @param BLUE  0-255
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setLeftLED(int RED, int GREEN, int BLUE) {
        if (hardwareVersion == 1) {
            return ((LEDS_V1) LED_CONTROLLER).setLeftLED(RED, GREEN, BLUE);
        }
        return false;
    }

    /**
     * Set the Sensordrone's right LED color.
     *
     * @param RED   0-255
     * @param GREEN 0-255
     * @param BLUE  0-255
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setRightLED(int RED, int GREEN, int BLUE) {
        if (hardwareVersion == 1) {
            return ((LEDS_V1) LED_CONTROLLER).setRightLED(RED, GREEN, BLUE);
        }
        return false;
    }

    /**
     * Set the Sensordrone's left and right LED color concurrently.
     *
     * @param RED   0-255
     * @param GREEN 0-255
     * @param BLUE  0-255
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setLEDs(int RED, int GREEN, int BLUE) {
        if (hardwareVersion == 1) {
            return ((LEDS_V1) LED_CONTROLLER).setLEDs(RED, GREEN, BLUE);
        }
        return false;
    }

//	/*
//	 * I2C
//	 */
//	/**
//	 * A ByteBuffer containing the contents from the last I2C Read.
//	 */
//	public ByteBuffer i2cReadBuffer;
//	/**
//	 * Performs an I2C read from the external I2C pins. Clears and updates i2cReadBuffer.
//	 * @return Returns true on successful communication to the Drone.
//	 */
//	public boolean i2cRead() {
//		return true;
//	}
//	/**
//	 * Performs an I2C write to the external I2C pins.
//	 * @param data The data to be sent.
//	 * @return Returns true on successful communication to the Drone.
//	 */
//	public boolean i2cWrite(byte[] data) {
//		return true;
//	}
	
	/*
	 * UART
	 */
    /**
     * A ByteBuffer containing the contents from the last UART Read.
     *
     * @since 1.1.1
     */
    public ByteBuffer uartReadBuffer;

    /**
     * An input stream for the external UART.
     *
     * @since 1.1.2
     */
    public PipedInputStream uartInputStream;


    /**
     * Sets the baud rate of the UART pins to 2400.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setBaudRate_2400() {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).setBaudRate_2400();
        }
        return false;
    }

    /**
     * Sets the baud rate of the UART pins to 9600.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setBaudRate_9600() {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).setBaudRate_9600();
        }
        return false;
    }

    /**
     * Sets the baud rate of the UART pins to 38400.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setBaudRate_38400() {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).setBaudRate_38400();
        }
        return false;
    }

    /**
     * Sets the baud rate of the UART pins to 19200.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setBaudRate_19200() {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).setBaudRate_19200();
        }
        return false;
    }

    /**
     * Sets the baud rate of the UART pins to 115200.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean setBaudRate_115200() {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).setBaudRate_115200();
        }
        return false;
    }

    /**
     * Performs a UART read from the external UART pins.
     * <p/>
     * Clears and updates uartReadBuffer.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean uartRead() {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).externalUartRead();
        }
        return false;
    }

    /**
     * Performs a UART write to the external UART pins. There is a limit of 32 bytes per uartWrite.
     *
     * @param data The data to be sent.
     * @return Returns true on successful communication to the CoreDrone. Will return false if sending more than
     *         32 bytes at a time.
     * @since 1.1.1
     */
    public boolean uartWrite(byte[] data) {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).exernalUartWrite(data);
        }
        return false;
    }

    /**
     * A method to write a byte[], and follow it immediately (after the designated delay) with a read
     * (blocks executing thread!).
     *
     * This is useful for working with (Serial) modules, where you send it a command
     * and expect a response based on that command, instead of parsing a Serial response asynchronously.
     *
     * @param data The byte[] to write
     * @param  msDelay after the write byte[] is sent, wait this long before reading and returning a response
     * @since 1.1.2-SNAPSHOT
     *
     */
    public byte[] uartWriteForRead(byte[] data, int msDelay) {
        if (hardwareVersion == 1) {
            return ((UART_V1) UART_CONTROLLER).writeForRead(data, msDelay);
        }
        return new byte[] {0x00};
    }

	/*
	 * USBUart
	 */

    /**
     * A ByteBuffer containing the contents from the last USB UART Read.
     *
     * @since 1.1.1
     */
    public ByteBuffer usbUartReadBuffer;

    /**
     * An inputstream for the usb UART.
     *
     * @since 1.1.2
     */
    public PipedInputStream usbUartInputStream;

    /**
     * Performs a UART read from the USB UART.
     * Operates at baud rate 9600.
     * Clears and updates usbUartBuffer.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean usbUartRead() {
        if (hardwareVersion == 1) {
            return ((USB_UART_V1) USB_UART_CONTROLLER).readUSBUart();
        }
        return false;
    }

    /**
     * Performs a USB UART write to the USB UART. There is a limit of 32 bytes per usbUartWrite.
     * Operates at baud rate 9600.
     *
     * @param data The data to be sent.
     * @return Returns true on successful communication to the CoreDrone. Will return false if more than 32
     *         bytes are sent at a time.
     * @since 1.1.1
     */
    public boolean usbUartWrite(byte[] data) {
        if (hardwareVersion == 1) {
            return ((USB_UART_V1) USB_UART_CONTROLLER).USBUartWrite(data);
        }
        return false;
    }
	
	/*
	 * ADC
	 */
    /**
     * The measured ADC value from the external connector.
     * 12 bit resolution to from 0 to 3.0 Volts
     *
     * @since 1.1.1
     */
    public int externalADC;
    /**
     * The measured ADC value from the external connector converted to Volts.
     *
     * @since 1.1.1
     */
    public float externalADC_Volts;
    /**
     * The current enabled/disabled status for measuring the ADC.
     *
     * @since 1.1.1
     */
    public boolean adcStatus;

    /**
     * Enables measuring the ADC.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean enableADC() {
        if (hardwareVersion == 1) {
            return ((ADC_V1) ADC_CONTROLLER).enable();
        }
        return false;
    }

    /**
     * Disables measuring the ADC.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean disableADC() {
        if (hardwareVersion == 1) {
            return ((ADC_V1) ADC_CONTROLLER).disable();
        }
        return false;
    }

    /**
     * Checks the current enabled/disabled status for measuring the ADC.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean statusOfADC() {
        if (hardwareVersion == 1) {
            return ((ADC_V1) ADC_CONTROLLER).adcStatus();
        }
        return false;
    }

    /**
     * Measures the external ADC pin.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureExternalADC() {
        if (hardwareVersion == 1) {
            return ((ADC_V1) ADC_CONTROLLER).measureADC();
        }
        return false;
    }
	
	/*
	 * Status and Misc
	 */
    /**
     * The measured value of the Sensordrone's battery voltage in Volts.
     * Updated by the method getBatteryVoltage().
     *
     * @since 1.1.1
     */
    public float batteryVoltage_Volts;

    /**
     * Measures the Sensordrone's current battery voltage level.
     * Triggers a low battery status event (DroneStatusEventListener) if the
     * measured voltage is below 3.25V.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean measureBatteryVoltage() {
        if (hardwareVersion == 1) {
            return ((Power_V1) POWER_CONTROLLER).measureBatteryVoltage();
        }
        return false;
    }

    /**
     * A logical variable to indicate if the Sensordrone is charging or not.
     * Updated by the method isCharging().
     */
    public boolean isCharging;

    /**
     * Checks if the Sensordrone is currently charging or not.
     *
     * @return Returns true on successful communication to the CoreDrone.
     * @since 1.1.1
     */
    public boolean checkIfCharging() {
        if (hardwareVersion == 1) {
            return ((Power_V1) POWER_CONTROLLER).chargingStatus();
        }
        return false;
    }

    /**
     * The MAC address of the last Sensordrone connected to via btConnect(). Note, if you are connected, then
     * it is the current MAC address.
     *
     * @since 1.1.1
     */
    public String lastMAC = "";
	
	
	/*
	 * Connections
	 */
    /**
     * Indicates if the Sensordrone is currently connected or not. This is software controlled!
     * It is set true upon successful completion of a connect command, and false upon a successful
     * disconnect command. If communication is lost, or hangs, a disconnect routine is also
     * called which will update this.
     *
     * @since 1.1.1
     */
    public boolean isConnected;
    /*
     * Connection methods are handled via android/java specific files
     */


    /**
     * Used to notify a custom event
     */
    private DroneEventObject customEvent = new DroneEventObject(DroneEventObject.droneEventType.CUSTOM_EVENT);

    /**
     * A method to trigger a Custom Event in the DroneEventListener
     *
     * @since 1.1.1
     */
    public void customEventNotify() {
        notifyDroneEventHandler(customEvent);
        notifyDroneEventListener(customEvent);
    }

    /**
     * used to notify a custom status
     */
    private DroneEventObject customStatus = new DroneEventObject(DroneEventObject.droneEventType.CUSTOM_STATUS);

    /**
     * A method to trigger a Custom Status in the DroneStatusListener
     *
     * @since 1.1.1
     */
    public void customStatusNotifty() {
        notifyDroneEventHandler(customStatus);
        notifyDroneStatusListener(customStatus);
    }



    /**
     * A method used to notify our DroneEventHandler
     * @param event
     */
    protected void notifyDroneEventHandler(DroneEventObject event) {
        Object[] currentListeners = droneListenerList.getListenerList();
        for (int i = 0; i < currentListeners.length; i += 2) {
            // Only want DroneEventHandler
            if (!(currentListeners[i + 1] instanceof DroneEventHandler)) {
                continue;
            }
            if (currentListeners[i] == DroneEventHandler.class) {
                ((DroneEventHandler) currentListeners[i + 1]).parseEvent(event);
            }
        }
    }


    /**
     * A method used to notify the listener that a DroneEventType event has happened.
     * @param event
     */
    protected void notifyDroneEventListener(DroneEventObject event) {
        Object[] listeners = droneListenerList.getListenerList();
        String eventType = event.getSource().toString();
        for (int i = 0; i < listeners.length; i += 2) {
            // Only want DroneEventListeners
            if (!(listeners[i + 1] instanceof DroneEventListener)) {
                continue;
            }
            if (listeners[i] == DroneEventListener.class) {
        		/* 
        		 * Go through all of the DroneEventObject enums
        		 */

                if (eventType.equals(DroneEventObject.droneEventType.CAPCACITANCE_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).capacitanceMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.ADC_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).adcMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.PRECISION_GAS_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).precisionGasMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.HUMIDITY_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).humidityMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.TEMPERATURE_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).temperatureMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.IR_TEMPERATURE_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).irTemperatureMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.PRESSURE_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).pressureMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.ALTITUDE_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).altitudeMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.REDUCING_GAS_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).reducingGasMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.OXIDIZING_GAS_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).oxidizingGasMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.RGBC_MEASURED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).rgbcMeasured(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.UART_READ.toString())) {
                    ((DroneEventListener) listeners[i + 1]).uartRead(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.USB_UART_READ.toString())) {
                    ((DroneEventListener) listeners[i + 1]).usbUartRead(event);
                }
//        		else if (eventType.equals(droneEventType.I2C_READ.toString())){
//        			((DroneEventListener)listeners[i+1]).i2cRead(event);
//        		} 
                else if (eventType.equals(DroneEventObject.droneEventType.CUSTOM_EVENT.toString())) {
                    ((DroneEventListener) listeners[i + 1]).customEvent(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.CONNECTED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).connectEvent(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.DISCONNECTED.toString())) {
                    ((DroneEventListener) listeners[i + 1]).disconnectEvent(event);
                } else if (eventType.equals(DroneEventObject.droneEventType.CONNECTION_LOST.toString())) {
                    ((DroneEventListener) listeners[i + 1]).connectionLostEvent(event);
                } else {
                    ((DroneEventListener) listeners[i + 1]).unknown(event);
                }
            }
        }
    }

    /*
     * Notify the listener that a DroneStatusType event has happened.
     * Leave access modifier blank so other classes in the package can access this.
     * @param event The EventObject which has occurred.
     */

    /**
     * A method used to notify the listener that a DroneEventType status event has happened.
     * @param status
     */
    protected void notifyDroneStatusListener(DroneEventObject status) {
        Object[] statuses = droneListenerList.getListenerList();
        String statusType = status.getSource().toString();
        for (int i = 0; i < statuses.length; i += 2) {
            // Only fire DroneStatusListener events!
            if (!(statuses[i + 1] instanceof DroneStatusListener)) {
                continue;
            }
            if (statusType.equals(DroneEventObject.droneEventType.BATTERY_VOLTAGE_MEASURED.toString())) {
                ((DroneStatusListener) statuses[i + 1]).batteryVoltageStatus(status);
            } else if (statusType.equals(DroneEventObject.droneEventType.LOW_BATTERY.toString())) {
                ((DroneStatusListener) statuses[i + 1]).lowBatteryStatus(status);
            } else if (statusType.equals(DroneEventObject.droneEventType.CHARGING_STATUS.toString())) {
                ((DroneStatusListener) statuses[i + 1]).chargingStatus(status);
            } else if (statusType.equals(DroneEventObject.droneEventType.CUSTOM_STATUS.toString())) {
                ((DroneStatusListener) statuses[i + 1]).customStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.PRECISION_GAS_ENABLED.toString())) {
                ((DroneStatusListener) statuses[i + 1]).precisionGasStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.OXIDIZING_GAS_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.OXIDIZING_GAS_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.OXIDIZING_GAS_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).oxidizingGasStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.REDUCING_GAS_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.REDUCING_GAS_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.REDUCING_GAS_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).reducingGasStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.HUMIDITY_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.HUMIDITY_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.HUMIDITY_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).humidityStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.TEMPERATURE_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.TEMPERATURE_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.TEMPERATURE_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).temperatureStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.IR_TEMPERATURE_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.IR_TEMPERATURE_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.IR_TEMPERATURE_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).irStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.PRESSURE_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.PRESSURE_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.PRESSURE_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).pressureStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.ALTITUDE_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.ALTITUDE_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.ALTITUDE_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).altitudeStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.RGBC_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.RGBC_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.RGBC_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).rgbcStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.ADC_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.ADC_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.ADC_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).adcStatus(status);
            } else if (
                    statusType.equals(DroneEventObject.droneEventType.CAPACITANCE_ENABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.CAPACITANCE_DISABLED.toString()) ||
                            statusType.equals(DroneEventObject.droneEventType.CAPACITANCE_STATUS_CHECKED.toString())
                    ) {
                ((DroneStatusListener) statuses[i + 1]).capacitanceStatus(status);
            } else {
                ((DroneStatusListener) statuses[i + 1]).unknownStatus(status);
            }
        }
    }

    /**
     * Register the DroneEventListener
     *
     * @param listener
     * @since 1.1.1
     * @deprecated Please use registerDroneListener
     */
    public void registerDroneEventListener(DroneEventListener listener) {
        droneListenerList.add(DroneEventListener.class, listener);
    }

    /**
     * Unregister the DroneEventListener
     *
     * @param listener
     * @since 1.1.1
     * @deprecated please use unregisterDroneListener
     */
    public void unregisterDroneEventListener(DroneEventListener listener) {
        droneListenerList.remove(DroneEventListener.class, listener);
    }

    /**
     * Register the DroneStatusListener
     *
     * @param listener
     * @since 1.1.1
     * @deprecated Please use registerDroneListener
     */
    public void registerDroneStatusListener(DroneStatusListener listener) {
        droneListenerList.add(DroneStatusListener.class, listener);
    }

    /**
     * Unregister the DroneStatusListener
     *
     * @param listener
     * @since 1.1.1
     * @deprecated Please use unregisterDroneListener
     */
    public void unregisterDroneStatusListener(DroneStatusListener listener) {
        droneListenerList.remove(DroneStatusListener.class, listener);
    }

    /**
     * The new, preferred way to register a DroneStatusListener, DroneEventListener, or DroneEventHandler
     *
     * @param listener
     */
    public void registerDroneListener(EventListener listener) {
        if (listener instanceof DroneEventHandler) {
            droneListenerList.add(DroneEventHandler.class, (DroneEventHandler) listener);
        } else if (listener instanceof DroneEventListener) {
            droneListenerList.add(DroneEventListener.class, (DroneEventListener) listener);
        } else if (listener instanceof DroneStatusListener) {
            droneListenerList.add(DroneStatusListener.class, (DroneStatusListener) listener);
        }
    }

    /**
     * The new, preferred way to unregister a DroneStatusListener, DroneEventListener, or DroneEventHandler
     * @param listener
     */
    public void unregisterDroneListener(EventListener listener) {
        if (listener instanceof DroneEventHandler) {
            droneListenerList.remove(DroneEventHandler.class, (DroneEventHandler) listener);
        } else if (listener instanceof DroneEventListener) {
            droneListenerList.remove(DroneEventListener.class, (DroneEventListener) listener);
        } else if (listener instanceof DroneStatusListener) {
            droneListenerList.remove(DroneStatusListener.class, (DroneStatusListener) listener);
        }
    }

    // A basic Constructor
    public CoreDrone() {
        isConnected = false;
        droneEventListenerList = new EventListenerList();
        droneStatusListenerList = new EventListenerList();
        droneListenerList = new EventListenerList();
    }

    /**
     * Our default Constructor
     * @param log
     */
    protected CoreDrone(Logger log) {
        this();

        logger = log;
        localComms = new DroneSensor(this, TAG) {
            // Empty class, just for using the read/write functions of DroneSensor
        };
    }

    /**
     * A method to initialize the proper sensor objects based upon the hardware version
     * read from the Sensordrone in the connect process.
     *
     * @param hwVersion
     * @return
     */
    protected boolean initializeHardware(int hwVersion) {
		
		/*
		 * We define "controller objects" here, that can handle different hardware versions.
		 * 
		 * e.g.
		 * if (HW = 1) {
		 *  Humidity_V1 TEMEPRATURE_CONTROLLER = new Humidity_V1(this);
		 *  } else if ( HW= 2 ) {
		 *  Temperature_V2 TEMEPRATURE_CONTROLLER = new Temperature_V2(this)
		 *  }
		 *  
		 *  and the method measureTemperature will ALWAYS call
		 *  
		 *  TEMPERATURE_CONTROLLER.measureTemp();
		 *  
		 *  but use the correct "hardware class" to make the call.
		 *  
		 *  We do this, because the "sensor objects" have settings that we don't want to have to
		 *  recreate every time a command is called.
		 *  
		 *  The appropriate methods HAVE TO HAVE THE SAME LOGIC AS BELOW. 
		 *  The objects are not initialized for them,
		 *  so they do not have the correct methods to be called unless they are first cast
		 *  as the appropriate hardware type. (i.e. they are DroneSensor until they are cast 
		 *  to UART_V1, and DroneSensor does not have the method setBaudRate)
		 *  
		 */
        if (firmwareVersion == 1) {
            // Any logic based on firmware?
        }
        if (hwVersion == 1) {
            // Make sure any calibration stuff was read properly
            boolean calRead;
            logger.debugLogger(TAG, "Setting up hardware version 1", DEBUG);
            ADC_CONTROLLER = new ADC_V1(this);
            CAPACITANCE_CONTROLLER = new Capacitance_V1(this);
            RED_OX_CONTROLLER = new GeneralGas_V1(this);
            HUMIDITY_CONTROLLER = new Humidity_V1(this);
            //I2C_CONTROLLER = new I2C_V1(this);
            IR_CONTROLLER = new IRThermometer_V1(this);
            LED_CONTROLLER = new LEDS_V1(this);
            POWER_CONTROLLER = new Power_V1(this);
            PRECISION_GAS_CONTROLLER = new PrecisionGas_V1(this);
            // Read calibration data
            calRead = ((PrecisionGas_V1) PRECISION_GAS_CONTROLLER).readCalibrationData();
            if (calRead == false) {
                return false;
            }
            PRESSURE_CONTROLLER = new Pressure_V1(this);
            RGBC_CONTROLLER = new RGBC_V1(this);
            UART_CONTROLLER = new UART_V1(this);
            uartInputStream = ((UART_V1) UART_CONTROLLER).uartInputStream;
            USB_UART_CONTROLLER = new USB_UART_V1(this);
            usbUartInputStream = ((USB_UART_V1) USB_UART_CONTROLLER).usbUartInputStream;
            return true;
        } else {
            return false;
        }
    }


}
