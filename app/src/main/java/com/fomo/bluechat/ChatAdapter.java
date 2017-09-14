package com.fomo.bluechat;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nguye on 14-Sep-17.
 */

public class ChatAdapter extends ArrayAdapter<ChatMessage> {
    private TextView chat_text;
    private List<ChatMessage> chat_message_list = new ArrayList<>();
    private Context context;

    public ChatAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
        this.context = context;
    }

    @Override
    public void add(@Nullable ChatMessage object) {
        super.add(object);
        chat_message_list.add(object);
    }

    @Override
    public int getCount() {
        return chat_message_list.size();
    }

    public ChatMessage getItem(int index) {
        return this.chat_message_list.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage chat_message_obj = getItem(position);
        View row = convertView;
        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (chat_message_obj.left) {
            row = inflater.inflate(R.layout.right, parent, false);
        }else{
            row = inflater.inflate(R.layout.left, parent, false);
        }
        chat_text = (TextView) row.findViewById(R.id.msgr);
        chat_text.setText(chat_message_obj.message);
        return row;
    }
}
