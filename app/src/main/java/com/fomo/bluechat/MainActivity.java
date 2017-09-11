package com.fomo.bluechat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button connect_btn;
    ListView paired_devices, nearby_devices;
    ArrayAdapter<String> paired_adapter, nearby_adapter;
    CheckBox discoverable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connect_btn = (Button) findViewById(R.id.btn);
        paired_devices = (ListView) findViewById(R.id.paired_devices);
        nearby_devices = (ListView) findViewById(R.id.nearby_devices);
        discoverable = (CheckBox) findViewById(R.id.is_visible);

        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, ChatActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        paired_adapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<String>());
        nearby_adapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<String>());

        paired_devices.setAdapter(paired_adapter);
        nearby_devices.setAdapter(nearby_adapter);

        discoverable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                
            }
        });

    }
}
