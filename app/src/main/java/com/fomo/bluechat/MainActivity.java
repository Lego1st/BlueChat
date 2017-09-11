package com.fomo.bluechat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Activity";

    BluetoothAdapter bluetoothAdapter;

    private final static int REQUEST_ENABLE_BT = 1;

    Button scan_btn;
    ListView paired_list_view;
    ListView nearby_list_view;
    ArrayAdapter<String> paired_device_adapter;
    ArrayAdapter<String> nearby_device_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewIds();
//        connect_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent myIntent = new Intent(MainActivity.this, ChatActivity.class);
//                MainActivity.this.startActivity(myIntent);
//            }
//        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT);
            this.finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!bluetoothAdapter.isEnabled()) {
            Intent enable_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_intent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Paired devices
        paired_device_adapter = new ArrayAdapter<String>(this, R.layout.device_name);
        paired_list_view.setAdapter(paired_device_adapter);

        Set<BluetoothDevice> paired_devices = bluetoothAdapter.getBondedDevices();
        if(paired_devices.size() != 0) {
            for(BluetoothDevice device : paired_devices) {
                paired_device_adapter.add(device.getName());
            }
        }

        //finding devices around
        nearby_device_adapter = new ArrayAdapter<String>(this, R.layout.device_name);
        nearby_list_view.setAdapter(nearby_device_adapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discovering_receiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discovering_receiver, filter);

        //scan nearby devices
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan_btn.setText("Scanning");
                nearby_device_adapter.clear();
                doDiscovery();
            }
        });
    }

    private void doDiscovery() {

        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        Log.d(TAG, "doDiscovery()");

        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discovering_receiver);
    }

    private void findViewIds() {
        paired_list_view = (ListView) findViewById(R.id.paired_devices);
        scan_btn = (Button) findViewById(R.id.btn_scan);
        nearby_list_view = (ListView) findViewById(R.id.nearby_devices);
    }

    private final BroadcastReceiver discovering_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                Log.d(TAG, "found sth");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_NAME);

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    nearby_device_adapter.add(device.getName());
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if(nearby_device_adapter.getCount() == 0) {
                    String no_device = getResources().getText(R.string.none_found).toString();
                    nearby_device_adapter.add(no_device);
                }
                scan_btn.setText("Scan");
            }
        }
    };
}
