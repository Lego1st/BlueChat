package com.fomo.bluechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BluetoothAdapter bluetoothAdapter;
    private BTController btController;

    private static final int REQUEST_ENABLE_BT = 1;

    ArrayAdapter<String> nearby_device_adapter;
    ArrayAdapter<String> paired_device_adapter;

    private ListView paired_devices_list;
    private ListView nearby_devices_list;
    private CheckBox is_visible;
    private Button scan_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewByIds();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!bluetoothAdapter.isEnabled()) {
            Intent enable_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_intent, REQUEST_ENABLE_BT);
        } else if (btController == null) {
            btController = BTController.getInstance();
            btController.setMain_handler(this.handler);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(btController != null) {
            if (btController.getState() == BTController.STATE_NONE) {
                btController.start();
            }
        }

        paired_device_adapter = new ArrayAdapter<String>(this, R.layout.device_name);
        paired_devices_list.setAdapter(paired_device_adapter);
        paired_devices_list.setOnItemClickListener(device_list_click_listener);

        Set<BluetoothDevice> paired_devices = bluetoothAdapter.getBondedDevices();
        if(paired_devices.size() > 0) {
            for (BluetoothDevice device : paired_devices) {
                paired_device_adapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String no_device = getResources().getText(R.string.none_paired).toString();
            paired_device_adapter.add(no_device);
        }

        nearby_device_adapter = new ArrayAdapter<String>(this, R.layout.device_name);
        nearby_devices_list.setAdapter(nearby_device_adapter);
        nearby_devices_list.setOnItemClickListener(device_list_click_listener);

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
                Log.d(TAG, "click scan button");
                scan_btn.setText("Scanning");
                nearby_device_adapter.clear();
                doDiscovery();
                scan_btn.setClickable(false);
            }
        });

        is_visible.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    Intent discoverable_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverable_intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 20);
                    startActivity(discoverable_intent);
                    is_visible.setClickable(false);
                }
            }
        });

        filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(discoverable_end_receiver, filter);
    }

    private void doDiscovery() {

        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        Log.d(TAG, "doDiscovery()");

        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(REQUEST_ENABLE_BT == requestCode) {
            if(resultCode == Activity.RESULT_OK) {
                btController = BTController.getInstance();
                btController.setMain_handler(this.handler);
            } else if(resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Please enable bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void findViewByIds() {
        paired_devices_list = (ListView) findViewById(R.id.paired_devices);
        nearby_devices_list = (ListView) findViewById(R.id.nearby_devices);
        is_visible = (CheckBox) findViewById(R.id.is_visible);
        scan_btn = (Button) findViewById(R.id.btn_scan);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discovering_receiver);
        unregisterReceiver(discoverable_end_receiver);

        if(btController != null) {
            btController.stop();
        }
    }

    private AdapterView.OnItemClickListener device_list_click_listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            bluetoothAdapter.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            if(!(getResources().getText(R.string.none_found).toString().equals(info) ||
                    getResources().getText(R.string.none_paired).toString().equals(info))) {

                String name = info.substring(0, info.length() - 18);
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra(Constants.DEVICE_NAME, name);
                intent.putExtra(Constants.DEVICE_ADDRESS, address);
                startActivity(intent);
            }
        }
    };

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
                scan_btn.setClickable(true);
            }
        }
    };

    private final BroadcastReceiver discoverable_end_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int scan_mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                    BluetoothAdapter.SCAN_MODE_NONE);
            Log.d(TAG, "Scan mode change");
            if(scan_mode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                is_visible.setChecked(false);
                is_visible.setClickable(true);
            }
        }
    };

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.MESSAGE_ACCEPT_CONNECT) {
                Bundle bundle = msg.getData();
                String device_name = bundle.getString(Constants.DEVICE_NAME);
                String device_address = bundle.getString(Constants.DEVICE_ADDRESS);

                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra(Constants.DEVICE_NAME, device_name);
                intent.putExtra(Constants.DEVICE_ADDRESS, device_address);

                startActivity(intent);
            }
        }
    };
}
