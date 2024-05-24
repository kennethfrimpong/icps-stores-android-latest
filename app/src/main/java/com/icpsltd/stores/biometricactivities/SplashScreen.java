package com.icpsltd.stores.biometricactivities;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.icpsltd.stores.CardReadTest;
import com.icpsltd.stores.activities.MainActivity;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.R;
import com.icpsltd.stores.activities.HomePage;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class SplashScreen extends AppCompatActivity {

    CountDownTimer countDownTimer;
    private LinearProgressIndicator linearProgressIndicator;

    private String fromClassName =null;

    private String canNumber =null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);


        MyPrefs myPrefs = new MyPrefs();
        if(!myPrefs.getLoginStatus(getApplicationContext())){

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    linearProgressIndicator = findViewById(R.id.splash_progress);
                    linearProgressIndicator.setVisibility(View.VISIBLE);
                    ImageView imageView = findViewById(R.id.logo);
                    imageView.setVisibility(View.VISIBLE);

                }
            });
            initializeBiometrics();
            requestPermissions();

        } else {


            try{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linearProgressIndicator = findViewById(R.id.splash_progress);
                        linearProgressIndicator.setVisibility(View.VISIBLE);
                        ImageView imageView = findViewById(R.id.logo);
                        imageView.setVisibility(View.VISIBLE);

                    }
                });
                initializeBiometrics();
                requestPermissions();

                fromClassName = getIntent().getStringExtra("fromClassName");

                if (fromClassName != null){
                    if(fromClassName.equals("com.icpsltd.stores.activities.NewIssue")){
                        canNumber = getIntent().getStringExtra("canNumber");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                linearProgressIndicator = findViewById(R.id.splash_progress);
                                linearProgressIndicator.setVisibility(View.VISIBLE);
                                ImageView imageView = findViewById(R.id.logo);
                                imageView.setVisibility(View.VISIBLE);

                            }
                        });
                        //initializeBiometrics();
                        //requestPermissions();


                    }

                } else{
                    
                }

            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void Set_Timer() {

        countDownTimer = new CountDownTimer(2500, 500) {

            public void onTick(long millisUntilFinished) {
                // this will call on every 500 ms

            }

            public void onFinish() {
                MyPrefs myPrefs = new MyPrefs();
                if (myPrefs.getLoginStatus(getApplicationContext())){
                    Intent intent = new Intent(SplashScreen.this, HomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                    Toast.makeText(SplashScreen.this, "Signed in", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(SplashScreen.this, "Please sign in", Toast.LENGTH_SHORT).show();

                    Intent intent;
                    //intent = new Intent(SplashScreen.this, BiometricLogin.class);
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                    if(canNumber != null){
                        intent.putExtra("canNumber",canNumber);
                        intent.putExtra("name",getIntent().getStringExtra("name"));
                        intent.putExtra("fromClassName",fromClassName);
                        intent.putExtra("new_id",getIntent().getStringExtra("new_id"));
                        intent.putExtra("bookNumber",getIntent().getStringExtra("bookNumber"));
                        intent.putExtra("issuer_name",getIntent().getStringExtra("issuer_name"));
                        intent.putExtra("staffID",getIntent().getStringExtra("staffID"));
                        intent.putExtra("receiver_full_name",getIntent().getStringExtra("receiver_full_name"));
                        intent.putExtra("receiver_dept",getIntent().getStringExtra("receiver_dept"));

                    }
                    startActivity(intent);
                    overridePendingTransition(R.anim.in_from_right, R.anim.fade_in);
                    finish();


                }

            }
        }.start();
    }

    private void initializeBiometrics() {
        App.Context = getApplicationContext();
        App.BioManager = new BiometricsManager(getApplicationContext());
        App.BioManager.initializeBiometrics(new Biometrics.OnInitializedListener() {
            @Override
            public void onInitialized(Biometrics.ResultCode resultCode, String s, String s1) {
                if (resultCode == Biometrics.ResultCode.OK) {
                    App.deviceFamily = App.BioManager.getDeviceFamily();
                    App.deviceType = App.BioManager.getDeviceType();

                    setPreferences();
                    Toast.makeText(SplashScreen.this, "Biometrics Initialized", Toast.LENGTH_SHORT).show();
                } else {
                    Functions.Show_Alert(getApplicationContext(), "BioManager Initialization", "Initialization Failed");
                }
            }

        });
    }

    private void setPreferences() {
        App.BioManager.setPreferences("PREF_TIMEOUT", "-1", new Biometrics.PreferencesListener() {
            @Override
            public void onPreferences(Biometrics.ResultCode resultCode, String s, String s1) {
                if (resultCode == Biometrics.ResultCode.OK) {
                    Log.d("Set_Prefernce", "Success setting timeout to never");
                } else {
                    Log.d("Set_Prefernce", "FAILURE setting timeout to never");

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            countDownTimer.cancel();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void requestPermissions() {
        // below line is use to request
        // permission in the current activity.
        Dexter.withActivity(this)
                // below line is use to request the number of
                // permissions which are required in our app.
                .withPermissions(android.Manifest.permission.CAMERA,
                        // below is the list of permissions
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.ACCESS_WIFI_STATE,
                        android.Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE)
                // after adding permissions we are
                // calling an with listener method.
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        // this method is called when all permissions are granted
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            // do you work now
                            Toast.makeText(SplashScreen.this, "All the permissions are granted..", Toast.LENGTH_SHORT).show();
                            Set_Timer();
                        }
                        // check for permanent denial of any permission
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permanently,
                            // we will show user a dialog message.
                            //showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        // this method is called when user grants some
                        // permission and denies some of them.
                        permissionToken.continuePermissionRequest();
                    }
                }).withErrorListener(new PermissionRequestErrorListener() {
                    // this method is use to handle error
                    // in runtime permissions
                    @Override
                    public void onError(DexterError error) {
                        // we are displaying a toast message for error message.
                        Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
                    }
                })
                // below line is use to run the permissions
                // on same thread and to check the permissions
                .onSameThread().check();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        try{
            fromClassName = getIntent().getStringExtra("fromClassName");

            if (fromClassName != null){
                if(fromClassName.equals("com.icpsltd.stores.activities.NewIssue")){

                }

            } else{
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }

        } catch (Exception e){
            e.printStackTrace();
        }

    }

}