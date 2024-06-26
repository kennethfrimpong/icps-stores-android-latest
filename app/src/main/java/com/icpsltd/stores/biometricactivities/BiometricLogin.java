package com.icpsltd.stores.biometricactivities;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.credenceid.biometrics.Biometrics;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.R;
import com.icpsltd.stores.activities.Configure;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;
import com.icpsltd.stores.util.Variables;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

    private MaterialButton loginButton;

    private static Biometrics.OnCardStatusListener onCardStatusListener;


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

        OpenCardReaderAsync openCardReaderAsync = new OpenCardReaderAsync();
        openCardReaderAsync.execute();
        configureHttpConnectionWithSSL();


        loginButton = findViewById(R.id.login_with_access_button);

        try{

            if (getIntent().getStringExtra("canNumber") != null){
                can_entry.setText(getIntent().getStringExtra("canNumber"));
                loginButton.setText("Confirm as "+getIntent().getStringExtra("name"));
                can_entry.setEnabled(false);

            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void begin_login_verification(View view) {

        can_fetch_status.setText("");
        if(can_entry.getText().length() == 6){
            linearProgressIndicator.setVisibility(View.VISIBLE);

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
                    String url = "https://"+apiHost+":"+apiPort+"/api/v1/verify_can/";
                    try{
                        if(getIntent().getStringExtra("fromClassName").equals("com.icpsltd.stores.activities.NewIssue")){
                            url = "https://"+apiHost+":"+apiPort+"/api/v1/verify_receiver_can/";
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    String json = "{\"can\":\""+can_entry.getText().toString()+"\"}";
                    RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(url)
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

                        Intent intentx = new Intent(BiometricLogin.this, FingerPrint.class);
                        intentx.putExtra("can_number",getIntent().getStringExtra("canNumber"));
                        intentx.putExtra("fromClassName",getIntent().getStringExtra("fromClassName"));
                        intentx.putExtra("new_id",getIntent().getStringExtra("new_id"));
                        intentx.putExtra("bookNumber",getIntent().getStringExtra("bookNumber"));
                        intentx.putExtra("issuer_name",getIntent().getStringExtra("issuer_name"));
                        intentx.putExtra("staffID",getIntent().getStringExtra("staffID"));
                        intentx.putExtra("receiver_full_name",getIntent().getStringExtra("receiver_full_name"));
                        intentx.putExtra("receiver_dept",getIntent().getStringExtra("receiver_dept"));
                        startActivity(intentx);


                    }
                    response.close();
                } catch (Exception e){
                    e.printStackTrace();
                    runOnUiThread(()->{
                        //Toast.makeText(this, "Could not establish server connection", Toast.LENGTH_SHORT).show();
                        linearProgressIndicator.setVisibility(View.GONE);
                        can_fetch_status.setText("Could not establish server connection, check API config");
                        can_fetch_status.setTextColor(Color.RED);

                    });
                    return null;

                }
                return null;
            });


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
        Intent intent = new Intent(BiometricLogin.this, Configure.class);
        startActivity(intent);

    }

    private class OpenCardReaderAsync extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Functions.Show_loader(BiometricLogin.this, false, true);
//            Functions.fetchAndSaveUsers(LoginActivity.this);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            openCardReader();
            return true;
        }

        protected void onPostExecute() {
            Functions.cancel_loader();
            //Functions.arrangeFingers(finger_1,finger_2,finger_3,finger_4,cardDetails.getGhanaIdCardFpTemplateInfosList());

        }

    }

    public void openCardReaderMain() {
      //  SharedPreferences.Editor editor = Functions.getEditor(LoginActivity.this);
        App.BioManager.cardOpenCommand(new Biometrics.CardReaderStatusListener() {
            @Override
            public void onCardReaderOpen(Biometrics.ResultCode resultCode) {
                if (resultCode == Biometrics.ResultCode.OK) {

                    onCardStatusListener = new Biometrics.OnCardStatusListener() {
                        @Override
                        public void onCardStatusChange(String s, int prevState, int currentState) {
                            if (Variables.CARD_ABSENT == currentState) {
                                Functions.Show_Alert2(BiometricLogin.this, "CARD ABSENT", "Place Card on top on the device");
                            } else {
                                Variables.mIsDocPresentOnEPassport = true;
                                Functions.hide_Alert2();
                            }
                        }
                    };

                    App.BioManager.registerCardStatusListener(onCardStatusListener);
                    Functions.cancel_loader();
                } else {
                    Functions.cancel_loader();
                    Functions.Show_Alert2(BiometricLogin.this, "Card Opening Error", "Error Opening Card Reader" + resultCode.toString());
                }
            }

            @Override
            public void onCardReaderClosed(Biometrics.ResultCode resultCode, Biometrics.CloseReasonCode closeReasonCode) {
                Functions.cancel_loader();
//                editor.putBoolean(Variables.is_card_reader_open, false);
//                editor.commit();
//                loginNotification.setText(Variables.restart_app);
//                btnLogin.setEnabled(false);
                //Functions.Show_Alert(LoginActivity.this,"Card Closed","Error Opening Card Reader");
            }
        });
    }

    private void openCardReader() {
        boolean isCardReaderOpened = Functions.getSharedPreference(BiometricLogin.this).getBoolean(Variables.is_card_reader_open, false);

        if (isCardReaderOpened == false) {
            openCardReaderMain();
        } else {
            boolean cardConnection = App.BioManager.cardConnectSync(1000);
            if (cardConnection) {
                onCardStatusListener = new Biometrics.OnCardStatusListener() {
                    @Override
                    public void onCardStatusChange(String s, int prevState, int currentState) {
                        if (Variables.CARD_ABSENT == currentState) {
                            Functions.Show_Alert2(BiometricLogin.this, "CARD ABSENT", "Place Card on top on the device");

                        } else {
                            Variables.mIsDocPresentOnEPassport = true;
                            Functions.hide_Alert2();

                        }
                    }
                };
                App.BioManager.registerCardStatusListener(onCardStatusListener);
                Functions.cancel_loader();
            } else {
                openCardReaderMain();
            }
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        onCardStatusListener = (String ATR,
                                int prevState,
                                int currentState) -> {
            /* If currentState is 1, then no card is present. */
            if (Variables.CARD_ABSENT == currentState) {
                Functions.Show_Alert2(BiometricLogin.this, "CARD ABSENT", "Place Card on top on the device");
            } else {
                Variables.mIsDocPresentOnEPassport = true;

                //CardDetails cardDetails = Functions.readGhanaCard(LoginActivity.this,"990409");
                //Log.d(Variables.TAG,cardDetails.toString());
                //readGhanaIdDocument(canNumber);
            }


        };

        App.BioManager.registerCardStatusListener(onCardStatusListener);

    }

}