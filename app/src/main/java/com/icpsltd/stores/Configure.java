package com.icpsltd.stores;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Configure extends AppCompatActivity {

    private String api_host;
    private String api_port;
    private String db_host;
    private String db_name;
    private String db_username;
    private String db_password;

    EditText api_host_tv;
    EditText api_port_tv;
    EditText db_host_tv;
    EditText db_name_tv;
    EditText db_username_tv;
    EditText db_password_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        api_host_tv = findViewById(R.id.api_host_input);
        api_port_tv = findViewById(R.id.api_port_input);
        db_host_tv = findViewById(R.id.db_host_input);
        db_name_tv = findViewById(R.id.db_name_input);
        db_username_tv =  findViewById(R.id.db_username_input);
        db_password_tv = findViewById(R.id.db_password_input);

        populateTexts();


    }

    private void populateTexts(){
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        try{
            ContentValues values = dbHandler.getSavedCredentials();
            api_host = values.getAsString("ApiHost");
            api_port = values.getAsString("ApiPort");
            db_host = values.getAsString("DatabaseHost");
            db_name = values.getAsString("DatabaseName");
            db_username = values.getAsString("DatabaseUsername");
            db_password = values.getAsString("DatabasePassword");

            api_host_tv.setText(api_host);
            api_port_tv.setText(api_port);
            db_host_tv.setText(db_host);
            db_name_tv.setText(db_name);
            db_username_tv.setText(db_username);
            db_password_tv.setText(db_password);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void go_back(View view) {
        Intent intent = new Intent(Configure.this, MainActivity.class);
        startActivity(intent);
    }

    public void syncCredentials(View view) {
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        String apiHost = api_host_tv.getText().toString();
        String apiPort = api_port_tv.getText().toString();
        String dbHost = db_host_tv.getText().toString();
        String dbName = db_name_tv.getText().toString();
        String dbUser = db_username_tv.getText().toString();
        String dbPass = db_password_tv.getText().toString();
        dbHandler.syncAppCredentials(apiHost,apiPort,dbHost,dbName,dbUser,dbPass);
        populateTexts();
        Toast.makeText(this, "Credentials saved successfully", Toast.LENGTH_SHORT).show();
    }
}