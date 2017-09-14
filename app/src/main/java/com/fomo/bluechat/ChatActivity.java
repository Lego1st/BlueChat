package com.fomo.bluechat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private boolean side = false;
    ChatAdapter adapter;
    ListView listMessages;
    Button mess_btn;
    EditText edit_text;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
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
                    Toast toast = Toast.makeText(getApplicationContext(), "Please input some text!", Toast.LENGTH_LONG);
                    toast.show();
                }
                else {
                    adapter.add(new ChatMessage(side, text));
                    side = !side;
                    edit_text.setText("");
                    listMessages.setSelection(adapter.getCount()-1);
                }
            }
        });
    }
}
