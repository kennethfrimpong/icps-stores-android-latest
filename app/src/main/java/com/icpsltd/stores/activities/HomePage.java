package com.icpsltd.stores.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.icpsltd.stores.AddImages;
import com.icpsltd.stores.biometricactivities.BiometricLogin;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.R;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomePage extends AppCompatActivity {

    private Response response;
    private Headers headers;
    private OkHttpClient okHttpClient;
    String apiHost;
    String apiPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        DBHandler dbHandler = new DBHandler(getApplicationContext());

        apiHost = dbHandler.getApiHost();
        apiPort = dbHandler.getApiPort();

        MyPrefs myPrefs = new MyPrefs();
        if(!myPrefs.getLoginStatus(getApplicationContext())){
            Toast.makeText(getApplicationContext(),"You need to login first",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomePage.this, BiometricLogin.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        } else{

            TextView tv = findViewById(R.id.welcome);
            TextView pv = findViewById(R.id.privilege);
            tv.setText(getString(R.string.welcome)+dbHandler.getFirstName());
            pv.setText(dbHandler.getPrivilege());

            if(!dbHandler.getPrivilege().equals("Administrator")){
                ImageView imageView = findViewById(R.id.staffAccessRegistration);
                imageView.setVisibility(View.GONE);
            }
        }

        MaterialCardView materialCardView = findViewById(R.id.new_issue);
        materialCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, NewIssue.class);
                startActivity(intent);
            }
        });

        try{
            InputStream certInputStream = getResources().openRawResource(R.raw.localhost);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null,null);
            keystore.setCertificateEntry("localhost",certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,trustManagers,null);

            //remove this when ca trusted SSL is available and traffic is over https, remove hostname verifier method too in OkHttp builder

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if (hostname.equals(apiHost)){
                        return true;
                    } else {
                        runOnUiThread(()->{ Toast.makeText(HomePage.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });
                        return false;

                    }

                }
            };
            okHttpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager) trustManagers[0])
                    .hostnameVerifier(hostnameVerifier)
                    .build();

        } catch (Exception e){
            e.printStackTrace();

        }

        dbHandler.clearOngoingIssueTable();
        dbHandler.clearOngoingIssueMetaTable();
        dbHandler.clearStaffTable();
        dbHandler.clearStockTable();
        dbHandler.clearStoreTable();
        dbHandler.clearLocationTable();
        dbHandler.clearMoveTable();
        dbHandler.close();

        //make api call to check if token is valid
        //if token is not valid, logout
        //if token is valid, do nothing
        /*

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = (Future<String>) executorService.submit(() -> {

            try{

                String json = "{}";
                RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://"+apiHost+":"+apiPort+"/api/v1/pingMobile/")
                        .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                        .post(requestBody)
                        .build();
                response = okHttpClient.newCall(request).execute();
                String resString = response.body().string();
                JSONObject jsonObject = new JSONObject(resString);
                String status = jsonObject.optString("status");
                Log.i("Ping Mobile",status);


            } catch (Exception e){
                e.printStackTrace();


            }

        });

         */


    }

    @Override
    protected void onStart() {
        super.onStart();
        MyPrefs myPrefs = new MyPrefs();
        if(!myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(HomePage.this, BiometricLogin.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyPrefs myPrefs = new MyPrefs();
        if(!myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(HomePage.this, BiometricLogin.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = (Future<String>) executorService.submit(() -> {

            try{

                String json = "{}";
                RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://"+apiHost+":"+apiPort+"/api/v1/pingMobile/")
                        .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                        .post(requestBody)
                        .build();
                response = okHttpClient.newCall(request).execute();
                String resString = response.body().string();
                Log.d("Ping Mobile", resString);
                JSONObject jsonObject = new JSONObject(resString);
                String status = jsonObject.optString("status");
                Log.i("Ping Mobile",status);

                return status;

            } catch (Exception e){
                e.printStackTrace();
                return "client_error";

            }

        });

        try {
            String status = future.get();

            if(status.equals("client_error")){
                runOnUiThread(()->{
                    Toast.makeText(HomePage.this, "Error connecting to server", Toast.LENGTH_SHORT).show();
                    Toast.makeText(HomePage.this, "Are you connected?", Toast.LENGTH_SHORT).show();
                });
            } else if (status.equals("db_error")) {
                runOnUiThread(()->{
                    Toast.makeText(HomePage.this, "Error connecting to database", Toast.LENGTH_SHORT).show();
                    Toast.makeText(HomePage.this, "Please contact admin", Toast.LENGTH_SHORT).show();
                });
                
            } else if (status.equals("error")){
                runOnUiThread(()->{
                    Toast.makeText(HomePage.this, "Error connecting to server", Toast.LENGTH_SHORT).show();
                    Toast.makeText(HomePage.this, "Please contact system admin", Toast.LENGTH_SHORT).show();
                });
            } else if (status.equals("success")){

            } else if (status.equals("expired") || status.equals("invalid") || status.equals("missing") || status.equals("restricted")){
                runOnUiThread(()->{

                    if (status.equals("expired")){
                        Toast.makeText(HomePage.this, "Session Expired", Toast.LENGTH_SHORT).show();
                        Toast.makeText(HomePage.this, "Please login again", Toast.LENGTH_SHORT).show();
                    } else if (status.equals("invalid")){
                        Toast.makeText(HomePage.this, "Please login again", Toast.LENGTH_SHORT).show();
                    } else if (status.equals("missing")){
                        Toast.makeText(HomePage.this, "Please login again", Toast.LENGTH_SHORT).show();
                    } else if (status.equals("restricted")){
                        Toast.makeText(HomePage.this, "You do not have access to this application", Toast.LENGTH_SHORT).show();
                        Toast.makeText(HomePage.this, "Please contact system admin", Toast.LENGTH_SHORT).show();
                    }
                    DBHandler dbHandler = new DBHandler(getApplicationContext());
                    try {
                        dbHandler.logOut(getApplicationContext());
                    } catch (GeneralSecurityException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    dbHandler.close();
                    Intent intent = new Intent(HomePage.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    public void logout(View view) {
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(HomePage.this);
        materialAlertDialogBuilder.setTitle("Logout")
                .setMessage("Would you like to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MyPrefs myPrefs = new MyPrefs();

                        ExecutorService executorService = Executors.newSingleThreadExecutor();
                        Future<String> future = (Future<String>) executorService.submit(() -> {

                            try{

                                String json = "{\"type\":\"delete\"}";
                                RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                                Request request = new Request.Builder()
                                        .url("https://"+apiHost+":"+apiPort+"/api/v1/mobileLogout/")
                                        .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                        .post(requestBody)
                                        .build();
                                response = okHttpClient.newCall(request).execute();
                                String resString = response.body().string();
                                Log.d("Ping Mobile", resString);
                                JSONObject jsonObject = new JSONObject(resString);
                                String status = jsonObject.optString("status");
                                Log.i("Ping Mobile",status);

                                return status;

                            } catch (Exception e){
                                e.printStackTrace();
                                return "client_error";

                            }

                        });


                        DBHandler dbHandler = new DBHandler(getApplicationContext());

                        try {
                            dbHandler.logOut(getApplicationContext());
                        } catch (GeneralSecurityException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        dbHandler.close();
                        Intent intent = new Intent(HomePage.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = materialAlertDialogBuilder.create();
        alertDialog.show();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            onBackPressed();
        }
        return true;
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void goToAddStock(View view) {
        Intent intent = new Intent(HomePage.this, AddStock.class);
        startActivity(intent);
    }

    public void go_to_issue_history(View view) {
        Intent intent = new Intent(HomePage.this, IssueHistory.class);
        startActivity(intent);
    }

    public void goToQBP(View view) {
        Intent intent = new Intent(HomePage.this, QueryByProduct.class);
        startActivity(intent);
    }

    public void goToQBL(View view) {
        Intent intent = new Intent(HomePage.this, QueryByLocation.class);
        startActivity(intent);
    }

    public void goToReturnStock(View view) {
        Intent intent = new Intent(HomePage.this, ReturnStock.class);
        startActivity(intent);
    }

    public void goToStockAdjustment(View view) {
        Intent intent = new Intent(HomePage.this, StockAdjustment.class);
        startActivity(intent);
    }

    public void addNewStaff(View view) {
        Intent intent = new Intent(HomePage.this, AddStaff.class);
        startActivity(intent);
    }

    public void goToAddImages(View view) {
        Intent intent = new Intent(HomePage.this, AddImages.class);
        startActivity(intent);
    }
}