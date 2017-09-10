package com.fomo.bluechat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    ArrayList<String> messages = new ArrayList<>();
    ArrayAdapter<String> adapter;
    ListView listMessages;
    Button mess_btn;
    EditText editText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        listMessages = (ListView) findViewById(R.id.listMessage);
        mess_btn = (Button) findViewById(R.id.mess_btn);
        editText = (EditText) findViewById(R.id.editText);

        // set chat adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        listMessages.setAdapter(adapter);

        // Handle send button action
        mess_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                if(text.compareTo("") == 0) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Please input your text!", Toast.LENGTH_LONG);
                    toast.show();
                }
                else {
                    adapter.add(text);
                }
            }
        });
    }
}
