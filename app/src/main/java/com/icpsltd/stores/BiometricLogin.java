package com.icpsltd.stores;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.icao.GhanaIdCardFpTemplateInfo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.icpsltd.stores.model.CardDetails;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;

import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BiometricLogin extends AppCompatActivity {


    LinearLayout can_entry_layout;
    ConstraintLayout fingerprint_layout;

    TextView login_helper;

    EditText can_entry;
    private String apiHost;
    private String apiPort;
    private OkHttpClient okHttpClient;
    private Response response;
    private TextView can_fetch_status;

    private String resString;

    private LinearProgressIndicator linearProgressIndicator;
    private MaterialButton configure_button;

    MaterialCardView fingerOne, fingerTwo, fingerThree, fingerFour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biometric_login);
        can_entry_layout = findViewById(R.id.can_entry_layout);
        can_entry = findViewById(R.id.can_entry);
        login_helper = findViewById(R.id.login_tv);
        can_fetch_status = findViewById(R.id.can_fetch_status);
        linearProgressIndicator = findViewById(R.id.can_fetch_lpi);
        can_fetch_status = findViewById(R.id.can_fetch_status);
        configure_button = findViewById(R.id.configure_button);
        configureHttpConnectionWithSSL();

    }

    public void begin_login_verification(View view) {
        can_fetch_status.setText("");
        if(can_entry.getText().length() == 6){
            linearProgressIndicator.setVisibility(View.VISIBLE);

            Intent intent = new Intent(BiometricLogin.this, FingerPrint.class);
            intent.putExtra("can_number",can_entry.getText().toString());
            startActivity(intent);


            /*

            try{
                DBHandler dbHandler = new DBHandler(getApplicationContext());
                apiHost = dbHandler.getApiHost();
                apiPort = dbHandler.getApiPort();

            } catch (Exception e){
                e.printStackTrace();
                runOnUiThread(()->{
                    //Toast.makeText(this, "Server connection error, check config", Toast.LENGTH_SHORT).show();
                    linearProgressIndicator.setVisibility(View.GONE);
                    can_fetch_status.setText("Server connection error, check configuration");
                    can_fetch_status.setTextColor(Color.RED);
                });

            }


            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<String> futureResult = executorService.submit(() -> {

                try{
                    String json = "{\"can\":\""+can_entry.getText().toString()+"\"}";
                    RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+apiHost+":"+apiPort+"/api/v1/verify_can/")
                            .post(requestBody)
                            .build();
                    response = okHttpClient.newCall(request).execute();
                    resString = response.body().string();
                    Log.i("Response",resString);
                    if(resString.equals("error")){
                        //error
                        runOnUiThread(()->{
                            //Toast.makeText(this, "There was an error", Toast.LENGTH_SHORT).show();
                            linearProgressIndicator.setVisibility(View.GONE);
                            can_fetch_status.setText("There was an error");
                            can_fetch_status.setTextColor(Color.RED);
                        });
                        return resString;
                    } else if (resString.equals("!exist")) {
                        //no user
                        runOnUiThread(()->{
                            //Toast.makeText(this, "No user was found", Toast.LENGTH_SHORT).show();
                            linearProgressIndicator.setVisibility(View.GONE);
                            can_fetch_status.setText("No user was found");
                            can_fetch_status.setTextColor(Color.RED);
                        });
                        return resString;

                    } else if (resString.equals("verified")) {

                        Intent intent = new Intent(BiometricLogin.this, FingerPrint.class);
                        intent.putExtra("can_number",can_entry.getText().toString());
                        startActivity(intent);

                    }
                    response.close();
                } catch (Exception e){
                    e.printStackTrace();
                    runOnUiThread(()->{
                        //Toast.makeText(this, "Could not establish server connection", Toast.LENGTH_SHORT).show();
                        linearProgressIndicator.setVisibility(View.GONE);
                        can_fetch_status.setText("Could not establish server connection");
                        can_fetch_status.setTextColor(Color.RED);

                    });
                    return null;

                }
                return null;
            });

            */

            /*

            try {
                String result = futureResult.get();
                //runOnUiThread(() -> Toast.makeText(this, result, Toast.LENGTH_SHORT).show());
                if(result.equals("verified")){
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Authenticated Successfully", Toast.LENGTH_SHORT).show();
                        //TextView textViewName = findViewById(R.id.login_status);
                        textViewName.setVisibility(View.VISIBLE);
                        textViewName.setText("Connecting to database...");
                        textViewName.setTextColor(Color.parseColor("#FFC107"));
                    });
                    //new BiometricLogin().InfoAsyncTask().execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

             */

        } else if (can_entry.getText().length() != 6) {
            Toast.makeText(this, "CAN number length should be 6", Toast.LENGTH_SHORT).show();
            can_fetch_status.setText("CAN number length should be 6");
            can_fetch_status.setTextColor(Color.RED);
        }


    }

    public void restart_login(View view) {
        can_entry_layout.setVisibility(View.VISIBLE);
        configure_button.setVisibility(View.VISIBLE);
        fingerprint_layout.setVisibility(View.GONE);
        login_helper.setText("Place your card at the top \nand enter  Ghana Card CAN number");
        Toast.makeText(getApplicationContext(),"Biometric Login Restarted",Toast.LENGTH_SHORT).show();

    }

    public void configureHttpConnectionWithSSL(){
        try{
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            apiHost = dbHandler.getApiHost();
            apiPort = dbHandler.getApiPort();
        } catch (Exception e){
            e.printStackTrace();
        }

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
                        runOnUiThread(()->{ Toast.makeText(BiometricLogin.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });
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
    }

    public void configure(View view) {
        Intent intent = new Intent(BiometricLogin.this,Configure.class);
        startActivity(intent);
    }


}