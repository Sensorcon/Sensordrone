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
 * A light weight EventListener.
 */
public interface DroneEventHandler extends EventListener {

    /**
     * This method can be triggered by any DroneEventObject.
     *
     * By checking the event, you can easily parse for particular events (and/or status'),
     * without having a large block of unimplemented code as you would with DroneStatusListener
     * or DroneEventListener.
     *
     * @param event
     * @see DroneStatusListener
     * @see DroneEventListener
     */
    public void parseEvent(DroneEventObject event);

}
