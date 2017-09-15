package com.fomo.bluechat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private static final boolean INCOME_MESSAGE = true;
    private static final boolean CONNECTED = true;

    private boolean state;
    private BluetoothAdapter bluetoothAdapter;
    private BTController btController;
    private ChatAdapter adapter;
    private ListView listMessages;
    private Button mess_btn;
    private EditText edit_text;
    private ActionBar action_bar;

    private String device_name;
    private String device_address;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btController = BTController.getInstance();
        btController.setChat_handler(this.handler);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Intent intent = this.getIntent();
        device_name = intent.getStringExtra(Constants.DEVICE_NAME);
        device_address = intent.getStringExtra(Constants.DEVICE_ADDRESS);

        action_bar = this.getSupportActionBar();
        action_bar.setTitle(device_name);

        if(btController.getState() == BTController.STATE_CONNECTED) {
            action_bar.setSubtitle("connected");
            state = CONNECTED;
        } else {
            action_bar.setSubtitle("connecting...");
            connect_device(device_address);
            state = !CONNECTED;
        }

        listMessages = (ListView) findViewById(R.id.listMessage);
        mess_btn = (Button) findViewById(R.id.mess_btn);
        edit_text = (EditText) findViewById(R.id.editText);

        // set chat adapter
        adapter = new ChatAdapter(getApplicationContext(), R.layout.right);
        listMessages.setAdapter(adapter);
        ChatActivity.this.setTitle(getIntent().getStringExtra(Constants.EXTRA_KEY));
        // Handle send button action
        mess_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edit_text.getText().toString();
                if(text.compareTo("") == 0) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Please input some text!",
                            Toast.LENGTH_LONG);
                    toast.show();
                }
                else if(state != CONNECTED) {
                    Toast.makeText(getApplicationContext(), "Device is not connected",
                            Toast.LENGTH_LONG).show();
                } else {
                    //sending message

//                    adapter.add(new ChatMessage(!INCOME_MESSAGE, text));
//                    edit_text.setText("");
//                    listMessages.setSelection(adapter.getCount()-1);
                    send_message(text);
                }
            }
        });
    }

    private void connect_device(String address) {

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        btController.connect(device);
    }

    private void send_message(String message) {

        if(message.length() > 0) {
            byte[] send = message.getBytes();
            btController.write(send);
        }
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CONNECTED:
                    Bundle bundle = msg.getData();
                    String device_name = bundle.getString(Constants.DEVICE_NAME);
                    if(!device_name.equals(ChatActivity.this.device_name)) {
                        Toast.makeText(ChatActivity.this, "Connection conflicts",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, device_name + " vs "+  ChatActivity.this.device_name);
                        finish();
                    } else {
                        state = CONNECTED;
                        action_bar.setSubtitle("connected");
                    }
                    break;
                case Constants.MESSAGE_CONNECT_FAILED:
                    Toast.makeText(ChatActivity.this, "Unable to connect device",
                            Toast.LENGTH_SHORT).show();
                    ChatActivity.this.finish();
                    break;
                case Constants.MESSAGE_CONNECT_LOST:
                    Toast.makeText(ChatActivity.this, "Lost connection",
                            Toast.LENGTH_SHORT).show();
                    ChatActivity.this.finish();
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    adapter.add(new ChatMessage(INCOME_MESSAGE,readMessage));
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    adapter.add(new ChatMessage(!INCOME_MESSAGE, writeMessage));
                    edit_text.setText("");
                    listMessages.setSelection(adapter.getCount()-1);
                    break;
            }
        }
    };
}
