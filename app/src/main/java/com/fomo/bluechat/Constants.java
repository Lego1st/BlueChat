package com.fomo.bluechat;

/**
 * Created by asus on 9/13/2017.
 */

public interface Constants {

    public static final String EXTRA_KEY = "key";

    //message send to chat activity handler
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_CONNECTED = 3;
    public static final int MESSAGE_CONNECT_FAILED = 4;
    public static final int MESSAGE_CONNECT_LOST = 5;

    //message send to main activity handler
    public static final int MESSAGE_ACCEPT_CONNECT = 1;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
}
