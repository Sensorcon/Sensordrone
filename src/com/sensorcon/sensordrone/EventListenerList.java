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

// We implement our own stripped-down EventListenerList, since it is in Java but not Android

import java.io.Serializable;

public class EventListenerList implements Serializable {


    // We set up listenerList like this so we don't have to parse for null objects in our listener
    protected transient Object[] listenerList = new Object[0];

    // Our getter
    public Object[] getListenerList() {
        return listenerList;
    }

    // Add to the list
    public synchronized <T extends java.util.EventListener> void
    add(final Class<T> eventListenerClass, final T eventListener) {

        // Make sure we didn't get passed a null object
        if (eventListener == null) {
            return;
        }

        // Set up a new object to hold it
        Object[] placeHolder = new Object[listenerList.length + 2];
        // Copy over what's already in the list
        System.arraycopy(listenerList, 0, placeHolder, 0, listenerList.length);
        // Add in the new items
        placeHolder[listenerList.length] = eventListenerClass;
        placeHolder[listenerList.length + 1] = eventListener;
        // Make it so
        listenerList = placeHolder;
    }

    // Remove from the list
    public synchronized <T extends java.util.EventListener> void
    remove(final Class<T> eventListenerClass, final T eventListener) {

        // Make sure we didn't get passed a null object
        if (eventListener == null) {
            return;
        }

        // If it's in the list, it's index will be >=0,
        // so set it to something it can't be.
        int listIndex = -1;
        // Check if it is in the list (starting from the end of the list)
        // We're checking the eventListener.Class and the eventListener, so we need to decrement by 2
        for (int i = listenerList.length - 1; i > 0; i -= 2) {
            if (eventListenerClass == listenerList[i - 1] && eventListener.equals(listenerList[i])) {
                listIndex = i - 1;
                break;
            }
        }

        // If listIndex isn't -1, then it's in the list
        if (listIndex != -1) {
            // Set up a new object to hold it
            Object[] placeHolder = new Object[listenerList.length - 2];
            // Copy over everything but the one we want to remove
            System.arraycopy(listenerList, 0, placeHolder, 0, listIndex);
            System.arraycopy(listenerList, listIndex + 2, placeHolder, listIndex, listenerList.length - listIndex - 2);
            // Make it so
            listenerList = placeHolder;
        }
    }

}
