package com.fomo.bluechat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.conn.scheme.HostNameResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * Created by asus on 9/12/2017.
 */

public class BTController {

    //singleton
    private static BTController instance;

    private final static String TAG = "BTController";

    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final String APP_NAME = "BlueChat";

    private final BluetoothAdapter adapter;
    private int state;
    private AcceptThread accept_thread;
    private ConnectThread connect_thread;
    private ConnectedThread connected_thread;
    private Handler main_handler;
    private Handler chat_handler;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    private BTController() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        this.state = STATE_NONE;
    }

    public static synchronized BTController getInstance() {
        if(instance == null) {
            instance = new BTController();
        }

        return instance;
    }

    public synchronized void setMain_handler(Handler main_handler) {
        this.main_handler = main_handler;
    }

    public synchronized void setChat_handler(Handler chat_handler){
        this.chat_handler = chat_handler;
    }

    public synchronized int getState() {
        return this.state;
    }

    public synchronized void start() {
        Log.d(TAG, "START");
        if(connect_thread != null) {
            this.connect_thread.cancel();
            this.connect_thread = null;
        }

        if(connected_thread != null) {
            connected_thread.cancel();
            connected_thread = null;
        }

        if(accept_thread == null) {
            accept_thread = new AcceptThread();
            accept_thread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        if(this.state == STATE_CONNECTING) {
            if(connect_thread != null) {
                connect_thread.cancel();
                connect_thread = null;
            }
        }

        if(connected_thread != null) {
            connected_thread.cancel();
            connected_thread = null;
        }

        connect_thread = new ConnectThread(device);
        connect_thread.start();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        if(connect_thread != null) {
            connect_thread.cancel();
            connect_thread = null;
        }

        if(connected_thread != null) {
            connected_thread.cancel();
            connected_thread = null;
        }

        if(accept_thread != null) {
            accept_thread.cancel();
            accept_thread = null;
        }

        connected_thread = new ConnectedThread(socket);
        connected_thread.start();

        //send message connected to chat activity
        if(chat_handler != null) {
            Message msg = chat_handler.obtainMessage(Constants.MESSAGE_CONNECTED);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            chat_handler.sendMessage(msg);
        }
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if(connect_thread != null) {
            connect_thread.cancel();
            connect_thread = null;
        }

        if(connected_thread != null) {
            connected_thread.cancel();
            connected_thread = null;
        }

        if(accept_thread != null) {
            accept_thread.cancel();
            accept_thread = null;
        }

        this.state = STATE_NONE;

    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if(state != STATE_CONNECTED) return;
            r = this.connected_thread;
        }
        r.write(out);
    }

    private void connectionFailed(BluetoothDevice device) {
        Log.e(TAG, "Unable to connect device " + device.getName());

        //send message connect fail to chat activity
        if(chat_handler != null) {
            Message msg = chat_handler.obtainMessage(Constants.MESSAGE_CONNECT_FAILED);
            chat_handler.sendMessage(msg);
        }
        this.state = STATE_NONE;

        BTController.this.start();
    }

    private void connectionLost() {
        Log.e(TAG, "Lost connection");
        //send message lost connection to chat activity
        if(chat_handler != null) {
            Message msg = chat_handler.obtainMessage(Constants.MESSAGE_CONNECT_LOST);
            chat_handler.sendMessage(msg);
        }

        this.state = STATE_NONE;

        BTController.this.start();
    }

    private class AcceptThread extends Thread{
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = adapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }

            bluetoothServerSocket = tmp;
            BTController.this.state = STATE_LISTEN;
        }

        @Override
        public void run() {

            Log.d(TAG, "BEGIN AcceptThread");
            setName("AcceptThread");

            BluetoothSocket socket = null;
            while(BTController.this.state != STATE_CONNECTED) {
                try {
                    socket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept method failed", e);
                    break;
                }
                if (socket != null) {
                    synchronized (BTController.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //start connected thread
                                //send message accept connect to main activity
                                BluetoothDevice device = socket.getRemoteDevice();
                                String device_name = device.getName();
                                String device_address = device.getAddress();
                                if(main_handler != null) {
                                    Message msg = main_handler.obtainMessage(Constants.MESSAGE_ACCEPT_CONNECT);
                                    Bundle bundle = new Bundle();
                                    bundle.putString(Constants.DEVICE_NAME, device_name);
                                    bundle.putString(Constants.DEVICE_ADDRESS, device_address);
                                    msg.setData(bundle);
                                    main_handler.sendMessage(msg);
                                }
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "Cancel " + this);
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG,"Close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            socket = tmp;
            BTController.this.state = STATE_CONNECTING;
        }

        @Override
        public void run() {

            Log.i(TAG, "BEGIN connectThread");
            setName("ConnectThread");

            adapter.cancelDiscovery();
            try {
                this.socket.connect();
            } catch (IOException e) {
                try {
                    this.socket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                connectionFailed(device);
                return;
            }

            synchronized (BTController.this) {
                connect_thread = null;
            }

            //start connected thread
            connected(this.socket, this.device);


        }

        public void cancel() {
            try {
                this.socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of socket connect failed" , e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket connected_socket;
        private final InputStream connected_input_stream;
        private final OutputStream connected_output_stream;

        public ConnectedThread(BluetoothSocket socket) {
            connected_socket = socket;
            InputStream tmp_ip = null;
            OutputStream tmp_op = null;

            try {
                tmp_ip = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmp_op = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            connected_input_stream = tmp_ip;
            connected_output_stream = tmp_op;
            BTController.this.state = STATE_CONNECTED;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            while (BTController.this.state == STATE_CONNECTED) {
                try {
                    bytes = connected_input_stream.read(buffer);
                    //send message to chat activity
                    BTController.this.chat_handler.obtainMessage(Constants.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();

                } catch(IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connected_output_stream.write(buffer);
                //send writing message to the chat activity
                chat_handler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();

            } catch(IOException e) {
                Log.e(TAG, "Exception while writting", e);
            }
        }

        public void cancel() {
            try {
                this.connected_socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of socket connect failed" , e);
            }
        }

    }
}
