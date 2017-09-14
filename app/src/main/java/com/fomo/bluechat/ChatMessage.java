package com.fomo.bluechat;

/**
 * Created by nguye on 14-Sep-17.
 */

public class ChatMessage {
    public String message;
    public boolean left;

    public ChatMessage(boolean left, String message) {
        super();
        this.left = left;
        this.message = message;
    }
}
