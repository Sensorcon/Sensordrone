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
package com.sensorcon.sensordrone.android.tools;


import java.util.Set;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import com.sensorcon.sensordrone.android.Drone;

/**
 * This class contains some useful connection methods to aid in the development of Apps for the Sensordrone.
 *
 * @author Mark Rudolph, Sensorcon, Inc.
 */
public class DroneConnectionHelper {

    private String MAC; // The MAC address we will connect to
    private BluetoothAdapter btAdapter;
    private BroadcastReceiver btReceiver;
    private IntentFilter btFilter;
    private Dialog scanDialog; // The Dialog we will display results in
    private AlertDialog.Builder dBuilder; // A builder for the Dialog

    /**
     * scantoConnect will launch an alert dialog, which will scan for Bluetooth devices,
     * and populate a list. Selecting an item from this list will attempt connection. It also checks
     * if Bluetooth is currently enabled on the device.
     *
     * @param drone                The Drone to connect
     * @param activity             Your apps activity
     * @param context              Your apps context
     * @param includePairedDevices True if you want to include the phone's paired devices in the list
     */
    public void scanToConnect(final Drone drone, final Activity activity, final Context context, boolean includePairedDevices) {

        // Set up our Bluetooth Adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Is Bluetooth on?
        boolean isOn = isBTEnabled(context, btAdapter);

        if (!isOn) {
            // Don't proceed until the user turns Bluetooth on.
            return;
        }

        // Make sure MAC String is initialized and empty
        MAC = "";

        // Set up our Dialog and Builder
        scanDialog = new Dialog(context);
        dBuilder = new AlertDialog.Builder(context);
        dBuilder.setTitle("Bluetooth Devices");

        // Set up our ListView to hold the items
        ListView macList = new ListView(context);
        // Set the overlay to transparent (can sometimes obscure text)
        macList.setCacheColorHint(Color.TRANSPARENT);
        // The text will be black, so we need to change the BG color
        macList.setBackgroundColor(Color.WHITE);

        // Set up our ArrayAdapter
        final ArrayAdapter<String> macAdapter = new ArrayAdapter<String>(
                context,
                android.R.layout.simple_list_item_1);
        macList.setAdapter(macAdapter);


        // Add in the paired devices if asked
        if (includePairedDevices) {
            // Get the list of paired devices
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            // Add paired devices to the list
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    // We only want Sensordrones
                    if (device.getName().contains("drone")) {
                        // Add the Name and MAC
                        macAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
            }
        }


        // What to do when we find a device
        btReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // We only want Sensordrones
                    try {
                        if (device.getName().contains("drone")) {
                            // Add the Name and MAC
                            macAdapter.add(device.getName() + "\n" + device.getAddress());
                        }
                    } catch (NullPointerException n) {
                        // Some times getName() will return null, which doesn't parse very well :-)
                        // Catch it here
                        Log.d("SDHelper", "Found MAC will null string");
                        // You can still add it to the list if you want, it just might not
                        // be a Sensordrone...
                        //macAdapter.add("nullDevice" + "\n" + device.getAddress());
                    }
                }
            }
        };

        // Set up our IntentFilters
        btFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(btReceiver, btFilter);
        // Don't forget to unregister when done!


        // Start scanning for Bluetooth devices
        btAdapter.startDiscovery();

        // Finish displaying the menu
        dBuilder.setView(macList);
        scanDialog = dBuilder.create();
        scanDialog.show();

        // Handle the Bluetooth device selection
        macList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int arg2, long arg3) {

                // Once an item is selected...

                // We don't need to scan anymore
                btAdapter.cancelDiscovery();
                // Unregister the receiver
                context.unregisterReceiver(btReceiver);

                // Get the MAC address
                MAC = macAdapter.getItem(arg2);
                int MACLength = MAC.length();
                MAC = MAC.substring(MACLength - 17, MACLength);

                // Dismiss the dialog
                scanDialog.dismiss();

            }
        });

        // Things to do when the Dialog is dismissed:
        // (When an item is selected, OR the user cancels)
        // Don't forget that when a Dialog is canceled, it is also dismissed.
        scanDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                // Don't connect if the MAC is not set
                // This protects against trying to connect if the user cancels
                if (MAC != "") {
                    // Display a message if the connect fails
                    if (!drone.btConnect(MAC)) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                                alert.setTitle("Couldn't connect");
                                alert.setMessage("Connection was not successful.\nPlease try again!");
                                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // If you wanted to try again directly, you could add that
                                        // here (and add a "cancel button" to not scan again).
                                    }
                                });

                                alert.show();
                            }
                        });

                    }
                }

            }
        });

        // Things to do if the dialog is canceled
        scanDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface arg0) {
                // Shut off discovery and unregister the receiver
                // if the user backs out
                btAdapter.cancelDiscovery();
                context.unregisterReceiver(btReceiver);
                // Clear the MAC so we don't try and connect
                MAC = "";
            }
        });

    }

    /**
     * Check if Bluetooth is currently enabled and, if not, asks the user to enable it.
     *
     * @param context   You apps context
     * @param btAdapter The devices BluetoothAdapter
     * @return Returns true if Bluetooth is already enabled
     */
    public boolean isBTEnabled(Context context, BluetoothAdapter btAdapter) {
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
            return false;
        } else {
            return true;
        }
    }

}
