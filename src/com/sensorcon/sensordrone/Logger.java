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


/**
 * This is an abstract class that MUST be implemented in android/java/whatever.
 * The Drone class needs a Logger, or else it will throw a null pointer exception.
 *
 * If the CoreDrone DEBUG boolean is set to TRUE, then a lot of information with be dumped
 * to some type of standard out.
 *
 * It is platform specific, but this can/is simply implemented as a "System.out.println()"
 * for Java or Log.d() for Android.
 */
public abstract class Logger {


    abstract public void txLogger(String TAG, byte[] data, boolean debug);

    abstract public void rxLogger(String TAG, byte[] data, boolean debug);

    abstract public void debugLogger(String TAG, String msg, boolean debug);

    abstract public void infoLogger(String TAG, String msg, boolean display);


}
