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
package com.sensorcon.sensordrone.dev.android;


import com.sensorcon.sensordrone.PrecisionGas_V1;

/**
 * A developer class to add things you might not want users to put into
 * a user library
 */
public class Drone extends com.sensorcon.sensordrone.android.Drone {


    // Example
    public PrecisionGas_V1 devExample;

    // For example, we implement writing calibration data here.
    // It extends from our main library, so it handles all the proper connections, etc...,
    // but we don't have to worry about users accidentally using the method and borking something.
    public void writeCal(byte[] data) {
        devExample.writeCalibrationData(data);
    }

}

