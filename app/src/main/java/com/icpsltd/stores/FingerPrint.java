package com.icpsltd.stores;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;

public class FingerPrint extends AppCompatActivity {
    private MaterialCardView fingerOne,fingerTwo,fingerThree,fingerFour;
    private TextView login_helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger_print);
        login_helper = findViewById(R.id.login_tv);
        fingerOne = findViewById(R.id.finger_one);
        fingerTwo = findViewById(R.id.finger_two);
        fingerThree = findViewById(R.id.finger_three);
        fingerFour = findViewById(R.id.finger_four);

        runOnUiThread(()->{
            login_helper.setText("Choose finger to scan");
            Toast.makeText(getApplicationContext(),"Choose finger to scan",Toast.LENGTH_SHORT).show();

            fingerOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.GREEN);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.BLACK);
                }
            });

            fingerTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.GREEN);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.BLACK);

                }
            });

            fingerThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.GREEN);
                    fingerFour.setStrokeColor(Color.BLACK);

                }
            });

            fingerFour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.GREEN);

                }
            });

        });
    }

    public void restart_login(View view) {
        Intent intent = new Intent(FingerPrint.this,BiometricLogin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}