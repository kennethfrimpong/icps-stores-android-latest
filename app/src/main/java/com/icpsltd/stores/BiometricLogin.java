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

    private CardDetails cardDetails = null;
    private byte[] mFingerprintOneFMDTemplate = null;
    private static List<GhanaIdCardFpTemplateInfo> ghanaIdCardFpTemplateInfosList = new ArrayList<>();

    private int selectedFingerIndex = 0;

    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private AlertDialog alertDialog;
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
        fingerprint_layout = findViewById(R.id.fingerprint_layout);
        can_entry = findViewById(R.id.can_entry);
        login_helper = findViewById(R.id.login_tv);
        fingerOne = findViewById(R.id.finger_one);
        fingerTwo = findViewById(R.id.finger_two);
        fingerThree = findViewById(R.id.finger_three);
        fingerFour = findViewById(R.id.finger_four);
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

                        runOnUiThread(()->{
                            linearProgressIndicator.setVisibility(View.GONE);
                            can_entry_layout.setVisibility(View.GONE);
                            fingerprint_layout.setVisibility(View.VISIBLE);
                            configure_button.setVisibility(View.GONE);
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

    private class CardReading extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Functions.Show_loader(BiometricLogin.this, false, false);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
//
            cardDetails = Functions.readGhanaCard(BiometricLogin.this, canNumber, finger_1, finger_2, finger_3, finger_4);
            //Functions.arrangeFingers(finger_1,finger_2,finger_3,finger_4,cardDetails.getGhanaIdCardFpTemplateInfosList());
            //Functions.openFingerPrintReader();
            App.BioManager.openFingerprintReader(mFingerprintOpenCloseListener);

            return true;
        }

        protected void onPostExecute() {
            Functions.cancel_loader();

        }



    }

    private Biometrics.FingerprintReaderStatusListener mFingerprintOpenCloseListener =
            new Biometrics.FingerprintReaderStatusListener() {
                @Override
                public void
                onOpenFingerprintReader(Biometrics.ResultCode resultCode,
                                        String hint) {

                    if (hint != null && !hint.isEmpty())
                    {
                        Functions.show_toast(BiometricLogin.this,hint);

                    }

                    if (OK == resultCode) {
                        Functions.show_toast(BiometricLogin.this,"Finger Open Successful");

                        Functions.cancel_loader();
                    }

                    else if (FAIL == resultCode) {
                        Functions.cancel_loader();
                    }
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void
                onCloseFingerprintReader(Biometrics.ResultCode resultCode,
                                         Biometrics.CloseReasonCode closeReasonCode) {

                    if (OK == resultCode) {


                    }
                    else if (FAIL == resultCode) {

                    }
                }

            };


    private void captureFingerprintOne(ImageView imageView) {

        mFingerprintOneFMDTemplate = null;
        ghanaIdCardFpTemplateInfosList = Functions.cardDetails.getGhanaIdCardFpTemplateInfosList();
        cardDetailToHold = new CardDetailsSummary(Functions.cardDetails);
        App.BioManager.grabFingerprint(Biometrics.ScanType.SINGLE_FINGER, new Biometrics.OnFingerprintGrabbedNewListener() {
            @Override
            public void onFingerprintGrabbed(Biometrics.ResultCode resultCode, Bitmap bitmap, byte[] bytes, String s) {
                if (OK == resultCode) {
                    if (null != bitmap)
                        imageView.setImageBitmap(bitmap);

                    //mStatusTextView.setText("WSQ File: " + wsqFilepath);
                    //mInfoTextView.setText("WSQ Quality: " + i);

                    /* Create template from fingerprint image. */
                    AsynucCreateFMDTemplate(bitmap);
                    //createFMDTemplate(bitmap);
                }
                if(resultCode == INTERMEDIATE)
                {
                    if (null != bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }

            @Override
            public void onCloseFingerprintReader(Biometrics.ResultCode resultCode, Biometrics.CloseReasonCode closeReasonCode) {

            }
        });


    }

    private void AsynucCreateFMDTemplate(Bitmap bitmap)
    {
        new AsyncTask<Object, Boolean, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Functions.Show_loader(BiometricLogin.this, false, false);
            }
            @Override
            protected Boolean doInBackground(Object... objects) {
                createFMDTemplate(bitmap);
                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                // Functions.cancel_loader();
            }


        }.execute();
    }

    private void createFMDTemplate(Bitmap bitmap)
    {
        App.BioManager.convertToFMD(bitmap, Biometrics.FMDFormat.ISO_19794_2_2005, new Biometrics.OnConvertToFMDListener() {
            @Override
            public void onConvertToFMD(Biometrics.ResultCode resultCode, byte[] bytes) {
                if(resultCode == OK)
                {
                    mFingerprintOneFMDTemplate = Arrays.copyOf(bytes, bytes.length);
                    if (mFingerprintOneFMDTemplate != null )
                    {
                        selectedFingerIndex = selectedFingerIndex -1;
                        byte[] data = ghanaIdCardFpTemplateInfosList.get(selectedFingerIndex).getMinutiae();
                        matchFMDTemplates(data,mFingerprintOneFMDTemplate);
                    }
                }
                else if (resultCode == INTERMEDIATE)
                {

                }
                else if(resultCode == FAIL)
                {

                }
            }
        });
    }

    private void matchFMDTemplates(byte[] templateOne,
                                   byte[] templateTwo) {
        //Functions.show_toast(FingerPrintActivity.this,"STARTING VERIFICATION");
        App.BioManager.compareFMD(templateOne, templateTwo, Biometrics.FMDFormat.ISO_19794_2_2005, new Biometrics.OnCompareFMDListener() {
            @Override
            public void onCompareFMD(Biometrics.ResultCode resultCode, float v) {
                Functions.cancel_loader();
                String responseTimestamp = dateFormat.format(new Date());

                if(resultCode == OK)
                {
                    Functions.show_toast(BiometricLogin.this,"FINISHED VERIFICATION");
                    if(v >70.0)
                    {

                        try {


                            JSONObject nhisData = new JSONObject();
                            nhisData.put("bioMatchResult","MFP");
                            nhisData.put("cardNo",cardDetailToHold.getCardNumber());
                            nhisData.put("cardType","GHANACARD");

                        }catch (Exception e)
                        {
                            Functions.cancel_loader();
                            e.printStackTrace();
                            alertDialog = Functions.showDialogWithOneButton(
                                    BiometricLogin.this,
                                    "Status Check not completed, Communication Error",
                                    R.drawable.not_verified,
                                    view -> alertDialog.dismiss());
                            alertDialog.show();
                        }


//                        Intent intent = new Intent(FingerPrintActivity.this, CardDetailsActivity.class);
//                        intent.putExtra(Variables.cardDetial, cardDetailToHold);
//                        intent.putExtra(Variables.offline_kyc, offlineKyc);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
//                        finish();
                    }
                    else{

                        alertDialog = Functions.showNotVerifiedDialog(BiometricLogin.this, "12345", view -> {

                            alertDialog.dismiss();
                            App.BioManager.closeFingerprintReader();
                            App.BioManager.cardCloseCommand();
                            App.BioManager.cardDisconnectSync(1);
                            Intent intent = new Intent(BiometricLogin.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }, view -> {
                            alertDialog.dismiss();
                        });
                        alertDialog.show();
                    }

                }
                else if (resultCode == INTERMEDIATE){

                }
                else if (resultCode == FAIL){

                }
            }
        });
    }


    public static List<GhanaIdCardFpTemplateInfo> getGhanaIdCardFpTemplateInfosList() {
        return ghanaIdCardFpTemplateInfosList;
    }


}