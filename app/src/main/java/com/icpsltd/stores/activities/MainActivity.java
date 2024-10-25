package com.icpsltd.stores.activities;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.icpsltd.stores.CardReadTest;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;
import com.icpsltd.stores.util.Variables;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.R;
import com.icpsltd.stores.utils.Security;


import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

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

public class MainActivity extends AppCompatActivity {

    private OkHttpClient okHttpClient;

    private String email_to_check;

    private String password_to_check;

    private Response response;

    private String responseString;

    private LinearProgressIndicator linearProgressIndicator = null;

    private String apiHost;
    private String apiPort;

    PendingIntent pendingIntent;

    BottomSheetDialog bottomSheetDialog;

    private static Biometrics.OnCardStatusListener onCardStatusListener;
    private boolean mIsCardReaderOpen = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fetchCredentials();
        responseString = null;
        mIsCardReaderOpen = false;
        /*

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

         */

        MyPrefs myPrefs = new MyPrefs();
        if (myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        configureHttpConnectionWithSSL();

        linearProgressIndicator = findViewById(R.id.login_progress);


        Log.d("STAGE", "OnCreate");


    }

    public void configureHttpConnectionWithSSL(){
        try{
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            apiHost = dbHandler.getApiHost();
            apiPort = dbHandler.getApiPort();
            dbHandler.close();
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
                        runOnUiThread(()->{ Toast.makeText(MainActivity.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });
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

    public void fetchCredentials(){

    }


    public void showBottomSheet(){
        mIsCardReaderOpen = true;
        int layout = R.layout.login_with_access_sheet;
        View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);

        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);

        /*

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            //NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
            bottomSheetDialog.show();
        } else {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_SHORT).show();
            //nfcAdapter.disableForegroundDispatch(this);
        }

         */

        try{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bottomSheetDialog.show();
                }
            });

            new MainActivity.OpenCardReaderAsync().execute();
        } catch (Exception e){
            e.printStackTrace();
        }

        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                App.BioManager.cardCloseCommand();
                App.BioManager.cardDisconnectSync(1);
                mIsCardReaderOpen = false;
            }
        });

    }

    public void displayResetHelper(View view) {
        Toast.makeText(this, "          Reset your password on web at\nhttps://stores.app.icps/reset-account.html", Toast.LENGTH_LONG).show();
    }

    private class CardReading extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //bottomSheetDialog.show();
            //Functions.Show_loader(MainActivity.this, false, false);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            String mAPDURead1k = "FF"         // MiFare Card
                    + "B0"                            // MiFare Card READ Command
                    + "00"                            // P1
                    + "00"                            // P2: Block Number
                    + "000400";                       // Number of bytes to read
            String mAPDURead4k = "FF"         // MiFare Card
                    + "B0"                            // MiFare Card READ Command
                    + "00"                            // P1
                    + "00"                            // P2: Block Number
                    + "001000";                       // Number of bytes to read
            /* Reads 2048 (2K) number of bytes from card. */
            String mAPDURead2k = "FF"         // MiFare Card
                    + "B0"                            // MiFare Card READ Command
                    + "00"                            // P1
                    + "00"                            // P2: Block Number
                    + "000800";                       // Number of bytes to read
            /* Reads 1024 (1K) number of bytes from card. */

            String mAPDUReadSpecialData = "FF"  // MiFare Card
                    + "B0"                              // MiFare Card READ Command
                    + "00"                              // P1
                    + "01"                              // P2: Block Number
                    + "00";                             // Number of bytes to read

            String mAPDUcustom = "FF"  // MiFare Card
                    + "CA"                              // MiFare Card READ Command
                    + "00"                              // P1 or "FF"
                    + "00"                              // P2: Block Number
                    + "00";                             // Number of bytes to read

            readCardAsync(mAPDUcustom);


            return true;
        }

        protected void onPostExecute() {
            //Functions.cancel_loader();
            //bottomSheetDialog.dismiss();

        }

    }

    private void readCardAsync (String APDUcommand){
        App.BioManager.cardCommand(new ApduCommand(APDUcommand), false, (Biometrics.ResultCode resultcode, byte sw1, byte sw2, byte[] data) ->{
            if(OK == resultcode){
//                Logger.getLogger("CardReader").info("Data: "+ Arrays.toString(data));
//                Logger.getLogger("CardReader").info("Data: "+bytesToHexString(data));
                BigInteger uidInt = new BigInteger(bytesToHexString(data), 16);
                verifyAccessCard(uidInt);
            } else if (INTERMEDIATE == resultcode) {
                Toast.makeText(this, "INTERMEDIATE", Toast.LENGTH_SHORT).show();
            } else if (FAIL == resultcode) {
                Toast.makeText(this, "FAILED.. \n Did you hold card on reader?", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private class OpenCardReaderAsync extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //Functions.Show_loader(MainActivity.this, false, true);
//            Functions.fetchAndSaveUsers(LoginActivity.this);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            if(mIsCardReaderOpen == true){
                openCardReader();
            }

            return true;
        }

        protected void onPostExecute() {
            bottomSheetDialog.dismiss();
            //App.BioManager.cardCloseCommand();
            //App.BioManager.cardDisconnectSync(1);
            //Functions.cancel_loader();
            //Functions.arrangeFingers(finger_1,finger_2,finger_3,finger_4,cardDetails.getGhanaIdCardFpTemplateInfosList());

        }

    }

    public void openCardReaderMain() {
        //  SharedPreferences.Editor editor = Functions.getEditor(LoginActivity.this);
        App.BioManager.cardOpenCommand(new Biometrics.CardReaderStatusListener() {
            @Override
            public void onCardReaderOpen(Biometrics.ResultCode resultCode) {
                if (resultCode == Biometrics.ResultCode.OK) {
                    //mIsCardReaderOpen = true;
                    onCardStatusListener = new Biometrics.OnCardStatusListener() {
                        @Override
                        public void onCardStatusChange(String s, int prevState, int currentState) {
                            if (Variables.CARD_ABSENT == currentState) {
                                //Functions.Show_Alert2(MainActivity.this, "CARD ABSENT", "Place Card on top on the device");
                            } else {
                                Variables.mIsDocPresentOnEPassport = true;
                                //Functions.hide_Alert2();

                                //Toast.makeText(MainActivity.this, "CARD DETECTED", Toast.LENGTH_SHORT).show();
                                new MainActivity.CardReading().execute();

                            }
                        }
                    };

                    App.BioManager.registerCardStatusListener(onCardStatusListener);
                    Functions.cancel_loader();
                } else {
                    Functions.cancel_loader();
                    Functions.Show_Alert2(MainActivity.this, "Card Opening Error", "Error Opening Card Reader" + resultCode.toString());
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
        boolean isCardReaderOpened = Functions.getSharedPreference(MainActivity.this).getBoolean(Variables.is_card_reader_open, false);

        if (isCardReaderOpened == false) {
            openCardReaderMain();
        } else {
            boolean cardConnection = App.BioManager.cardConnectSync(1000);
            if (cardConnection) {
                onCardStatusListener = new Biometrics.OnCardStatusListener() {
                    @Override
                    public void onCardStatusChange(String s, int prevState, int currentState) {
                        if (Variables.CARD_ABSENT == currentState) {
                            Functions.Show_Alert2(MainActivity.this, "CARD ABSENT", "Place Card on top on the device");

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

    public void verifyAccessCard(BigInteger uidInt) {
        //TextView textViewName = findViewById(R.id.login_status);
        //textViewName.setVisibility(View.VISIBLE);
        //textViewName.setText("Connecting to database...");
        //textViewName.setTextColor(Color.parseColor("#FFC107"));

        //Toast.makeText(this, String.valueOf(uidInt), Toast.LENGTH_SHORT).show();
        MyPrefs myPrefs = new MyPrefs();
        DBHandler dbHandler = new DBHandler(MainActivity.this);
        try {
            String apiHost = dbHandler.getApiHost();
            String apiPort = dbHandler.getApiPort();
            dbHandler.close();

            if (apiPort == null || apiPort.equals("") || apiHost == null || apiHost.equals("")) {
                //textViewName.setText("Connection failed, check API configuration");
                Toast.makeText(this, "Connection failed, check API configuration", Toast.LENGTH_SHORT).show();
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<String> futureResult = executorService.submit(() -> {
                try{

                    String json = "{\"type\":\"access_card\",\"uid\":\""+uidInt+"\"}";
                    RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+apiHost+":"+apiPort+"/api/v1/login/")
                            .post(requestBody)
                            .build();
                    response = okHttpClient.newCall(request).execute();
                    String resString = response.body().string();
                    JSONObject jsonObject = new JSONObject(resString);
                    String status = jsonObject.optString("status");
                    Log.i("Response",status);

                    if(status.equals("verified")){
                        Headers headers = response.headers();
                        String jwt_token = headers.get("Set-Cookie");
                        jwt_token = jwt_token.substring(10,jwt_token.indexOf(";"));
                        //Log.d("Token",jwt_token);
                        myPrefs.saveToken(getApplicationContext(),jwt_token);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Authenticated Successfully", Toast.LENGTH_SHORT).show();

                            String firstName = jsonObject.optString("firstName");
                            String lastName = jsonObject.optString("lastName");
                            String privilege = jsonObject.optString("privilege");
                            String userID = jsonObject.optString("userID");

                            MyPrefs myPref = new MyPrefs();
                            DBHandler dbHandler1 = new DBHandler(MainActivity.this);
                            TextView textViewName1 = findViewById(R.id.login_status);

                            dbHandler1.addUserSession(userID,firstName,lastName,privilege,1,"accessID");
                            dbHandler1.close();
                            myPref.saveLoginStatus(getApplicationContext(),true);
                            textViewName1.setVisibility(View.VISIBLE);
                            textViewName1.setText("Connected to database");
                            textViewName1.setTextColor(Color.parseColor("#56AF54"));
                            Intent intent = new Intent(MainActivity.this, HomePage.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            bottomSheetDialog.dismiss();
                            mIsCardReaderOpen = false;
                            startActivity(intent);
                            finish();

                        });

                        return status;
                    } else if (status.equals("Account not found")) {
                        runOnUiThread(()->{
                            TextView textViewName = findViewById(R.id.login_status);
                            textViewName.setVisibility(View.VISIBLE);
                            textViewName.setText("Account not found");
                            textViewName.setTextColor(Color.parseColor("#E30613"));
                            Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                            try{
                                mIsCardReaderOpen = false;

                                bottomSheetDialog.dismiss();
                                linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            } catch (Exception e){
                                e.printStackTrace();
                            }

                        });

                        return status;
                    } else if (status.equals("otpActive")) {
                        runOnUiThread(()->{
                            TextView textViewName = findViewById(R.id.login_status);
                            textViewName.setVisibility(View.VISIBLE);
                            textViewName.setText(jsonObject.optString("message"));
                            textViewName.setTextColor(Color.parseColor("#E30613"));
                            Toast.makeText(this, jsonObject.optString("message"), Toast.LENGTH_SHORT).show();
                            try{
                                mIsCardReaderOpen = false;

                                bottomSheetDialog.dismiss();
                                linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            } catch (Exception e){
                                e.printStackTrace();
                            }

                        });

                        return status;
                    } else if (status.equals("-1")) {
                        runOnUiThread(()->{
                            TextView textViewName = findViewById(R.id.login_status);
                            mIsCardReaderOpen = false;
                            textViewName.setVisibility(View.VISIBLE);
                            textViewName.setText("Could not establish database connection");
                            textViewName.setTextColor(Color.parseColor("#E30613"));
                            Toast.makeText(this, "There was an error", Toast.LENGTH_SHORT).show();
                            try{
                                bottomSheetDialog.dismiss();
                            } catch (Exception e){
                                e.printStackTrace();
                            }

                            linearProgressIndicator = findViewById(R.id.login_progress);
                            linearProgressIndicator.setVisibility(View.GONE);
                        });

                        return status;}
                    else {
                        responseString = null;
                    }
                    response.close();
                } catch (Exception e){
                    e.printStackTrace();
                    runOnUiThread(()->{
                        TextView textViewName = findViewById(R.id.login_status);
                        textViewName.setVisibility(View.VISIBLE);
                        textViewName.setText("Could not establish server connection \n Check wifi connection or API configuration");
                        textViewName.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        textViewName.setTextColor(Color.parseColor("#E30613"));
                        Toast.makeText(this, "   Could not establish server connection \n Check wifi connection or API configuration", Toast.LENGTH_SHORT).show();

                        myPrefs.saveLoginStatus(getApplicationContext(),false);
                        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                        linearProgressIndicator.setVisibility(View.GONE);
                    });
                    return null;

                }
                return null;
            });



        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void access_login(View view) {

        showBottomSheet();
    }


    public void configure(View view) {
        Intent intent = new Intent(MainActivity.this, Configure.class);
        startActivity(intent);
    }

    public void login_with_pass(View view) throws NoSuchAlgorithmException {
        TextView tv = findViewById(R.id.login_status);
        tv.setText("");
        tv.setVisibility(View.GONE);

        EditText email_input = findViewById(R.id.email_input);
        email_to_check = email_input.getText().toString();
        EditText password_input = findViewById(R.id.password_input);
        password_to_check = password_input.getText().toString();
        Security security = new Security();
        String passhash = security.createMD5Hash(password_to_check);

        TextView textViewName = findViewById(R.id.login_status);
        MyPrefs myPrefs = new MyPrefs();

        try{
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            String apiHost = dbHandler.getApiHost();
            String apiPort = dbHandler.getApiPort();
            dbHandler.close();

            if(apiPort == null || apiPort.equals("") || apiHost == null || apiHost.equals("") ){
                textViewName.setText("Connection failed, check API configuration");
            }


            if (!email_to_check.isEmpty()&&!password_to_check.isEmpty()){
                linearProgressIndicator.setVisibility(View.VISIBLE);


                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future<String> futureResult = executorService.submit(() -> {
                    try{
                        String json = "{\"email\":\""+email_to_check+"\",\"password\":\""+passhash+"\",\"type\":\"password\"}";
                        RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                        Request request = new Request.Builder()
                                .url("https://"+apiHost+":"+apiPort+"/api/v1/login/")
                                .post(requestBody)
                                .build();
                        response = okHttpClient.newCall(request).execute();
                        String resString = response.body().string();
                        JSONObject jsonObject = new JSONObject(resString);
                        Log.d("Response",resString);
                        Log.d("Response",jsonObject.toString());
                        String status = jsonObject.optString("status");
                        Log.i("Response",status);

                        if(status.equals("verified")){
                            //String jwt_token = response.header("Authorization");
                            //assert jwt_token != null;
                            //Log.d("Response",jwt_token);
                            //log all response headers
                            Headers headers = response.headers();
                            String jwt_token = headers.get("Set-Cookie");
                            jwt_token = jwt_token.substring(10,jwt_token.indexOf(";"));
                            //Log.d("Token",jwt_token);

                            myPrefs.saveToken(getApplicationContext(),jwt_token);
                            //Log.d("Token"," Saved");

                            //String token = myPrefs.getToken(getApplicationContext());
                            //Log.d("Token Retrieved ",token);

                            runOnUiThread(() -> {
                                Toast.makeText(this, "Authenticated Successfully", Toast.LENGTH_SHORT).show();
                                //TextView textViewName = findViewById(R.id.login_status);
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText("Connecting to database...");
                                textViewName.setTextColor(Color.parseColor("#FFC107"));


                                //String email = jsonObject.optString("email");
                                //String accessID = jsonObject.optString("accessID");
                                String firstName = jsonObject.optString("firstName");
                                String lastName = jsonObject.optString("lastName");
                                String privilege = jsonObject.optString("privilege");
                                String userID = jsonObject.optString("userID");

                                MyPrefs myPref = new MyPrefs();
                                DBHandler dbHandler1 = new DBHandler(MainActivity.this);
                                TextView textViewName1 = findViewById(R.id.login_status);

                                dbHandler1.addUserSession(userID,firstName,lastName,privilege,1,"accessID");
                                dbHandler1.close();
                                myPref.saveLoginStatus(getApplicationContext(),true);
                                textViewName1.setVisibility(View.VISIBLE);
                                textViewName1.setText("Connected to database");
                                textViewName1.setTextColor(Color.parseColor("#56AF54"));
                                Intent intent = new Intent(MainActivity.this, HomePage.class);
                                startActivity(intent);
                                finish();
                            });




                            return status;
                        } else if (status.equals("not verified")) {
                            runOnUiThread(()->{
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText("Incorrect Password");
                                textViewName.setTextColor(Color.parseColor("#E30613"));
                                myPrefs.saveLoginStatus(getApplicationContext(),false);
                                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            });

                            return status;
                        } else if (status.equals("locked") || status.equals("otpActive")) {
                            runOnUiThread(()->{
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText(jsonObject.optString("message"));
                                textViewName.setTextColor(Color.parseColor("#E30613"));
                                myPrefs.saveLoginStatus(getApplicationContext(),false);
                                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            });

                            return status;
                        } else if (status.equals("Account not found")) {
                            runOnUiThread(()->{
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText("Account not found");
                                textViewName.setTextColor(Color.parseColor("#E30613"));
                                linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            });

                            return status;
                        } else if (status.equals("-1")) {
                            runOnUiThread(()->{
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText("Could not establish database connection");
                                textViewName.setTextColor(Color.parseColor("#E30613"));
                                linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            });

                            return status;}
                        else {
                            responseString = null;
                        }
                        response.close();
                    } catch (Exception e){
                        e.printStackTrace();
                        runOnUiThread(()->{
                            textViewName.setVisibility(View.VISIBLE);
                            textViewName.setText("Could not establish server connection");
                            textViewName.setTextColor(Color.parseColor("#E30613"));
                            myPrefs.saveLoginStatus(getApplicationContext(),false);
                            LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                            linearProgressIndicator.setVisibility(View.GONE);
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

                        //new InfoAsyncTask().execute();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                 */


            } else if (email_to_check.equals("")&&password_to_check != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                        //linearProgressIndicator.setVisibility(View.GONE);
                        TextView textViewName = findViewById(R.id.login_status);
                        textViewName.setVisibility(View.VISIBLE);
                        textViewName.setText("Email field cannot be empty");
                        textViewName.setTextColor(Color.parseColor("#E30613"));
                    }
                });


            }
            else if (email_to_check != null && password_to_check.equals("")) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                        //linearProgressIndicator.setVisibility(View.GONE);
                        TextView textViewName = findViewById(R.id.login_status);
                        textViewName.setVisibility(View.VISIBLE);
                        textViewName.setText("Password field cannot be empty");
                        textViewName.setTextColor(Color.parseColor("#E30613"));
                    }
                });

            } else {
                //linearProgressIndicator.setVisibility(View.GONE);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        /*

        new InfoAsyncTask().execute();

         */
    }

    /*


    @SuppressLint("StaticFieldLeak")
    public class InfoAsyncTask extends AsyncTask<Void, Void, Map<String, String>> {
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            Map<String, String> info = new HashMap<>();

            try  {
                String sql = "SELECT firstName, lastName, email, userID, privilege, accessID FROM userTable WHERE email ='"+email_to_check+"' LIMIT 1";
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    info.put("firstName", resultSet.getString("firstName"));
                    info.put("lastName", resultSet.getString("lastName"));
                    info.put("email", resultSet.getString("email"));
                    info.put("userID", resultSet.getString("userID"));
                    info.put("privilege", resultSet.getString("privilege"));
                    info.put("accessID", String.valueOf(resultSet.getInt("accessID")));
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView textViewName = findViewById(R.id.login_status);
                            textViewName.setVisibility(View.VISIBLE);
                            textViewName.setText("Account not found");
                            textViewName.setTextColor(Color.parseColor("#E30613"));
                            LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                            linearProgressIndicator.setVisibility(View.GONE);
                        }
                    });

                }
            } catch (Exception e) {
                Log.e("InfoAsyncTask", "Error reading login information", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textViewName = findViewById(R.id.login_status);
                        textViewName.setVisibility(View.VISIBLE);
                        //textViewName.setText(String.valueOf(e));
                        textViewName.setText("Could not establish connection to database");
                        textViewName.setTextColor(Color.parseColor("#E30613"));
                        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                        linearProgressIndicator.setVisibility(View.GONE);
                    }
                });

            }

            return info;
        }

        @Override
        protected void onPostExecute(Map<String, String> result) {

            if (!result.isEmpty()) {

                MyPrefs myPrefs = new MyPrefs();
                DBHandler dbHandler = new DBHandler(MainActivity.this);
                TextView textViewName = findViewById(R.id.login_status);
                textViewName.setVisibility(View.VISIBLE);
                textViewName.setText("Connected to database");
                textViewName.setTextColor(Color.parseColor("#56AF54"));
                dbHandler.addUserSession(result.get("userID"),result.get("firstName"),result.get("lastName"),result.get("privilege"),1,result.get("accessID"));
                myPrefs.saveLoginStatus(getApplicationContext(),true);
                Intent intent = new Intent(MainActivity.this, HomePage.class);
                startActivity(intent);

            }

        }
    }

     */

/*
    @Override
    protected void onStart() {
        super.onStart();
        MyPrefs myPrefs = new MyPrefs();
        if (myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        configureHttpConnectionWithSSL();

        Log.d("STAGE", "OnStart");
    }

 */

    @Override
    protected void onResume() {
        super.onResume();
        mIsCardReaderOpen = false;
        configureHttpConnectionWithSSL();
        MyPrefs myPrefs = new MyPrefs();
        if (myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        Log.d("STAGE", "OnResume");

        //NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
        //nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    public interface VerifyUserTaskListener {
        void onTaskComplete(Request request);
    }

    @SuppressLint("StaticFieldLeak")
    public class VerifyUserTask extends AsyncTask<Void, Void, Request> {
        private MainActivity.VerifyUserTaskListener listener;

        public VerifyUserTask(VerifyUserTaskListener verifyUserTaskListener) {
            this.listener = listener;
        }

        @Override
        protected Request doInBackground(Void... voids) {

            try{
                MyPrefs myPrefs = new MyPrefs();
                String json = "{\"email\":\""+email_to_check+"\",\"password\":\""+password_to_check+"\"}";
                RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://"+apiHost+":"+apiPort+"/api/v1/login/")
                        .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                        .post(requestBody)
                        .build();
                response = okHttpClient.newCall(request).execute();
                String resString = response.body().string();
                Log.i("Response",resString);
                if(resString.equals("verified")){
                    responseString = resString;
                } else if (resString.equals("not verified")) {
                    responseString = resString;

                } else {
                    responseString = null;
                }
                response.close();
            } catch (Exception e){
                e.printStackTrace();

            }

            return null;

        }

        @Override
        protected void onPostExecute(Request request) {
            if (listener != null) {
                listener.onTaskComplete(request);
            }

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);



        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            //Toast.makeText(this, "NDEF Discovered", Toast.LENGTH_SHORT).show();


        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            //Toast.makeText(this, "TECH Discovered", Toast.LENGTH_SHORT).show();
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            //Toast.makeText(this, "TAG Discovered", Toast.LENGTH_SHORT).show();
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);



            if (tag != null) {
                //try reading MifareClassic
                try{

                    MifareClassic mifareClassic = MifareClassic.get(tag);
                    if (mifareClassic != null) {

                        try {
                            mifareClassic.connect();
                            byte[] uid = mifareClassic.getTag().getId();
                            String uidString = bytesToHexString(uid);
                            String[] techList = mifareClassic.getTag().getTechList();
                            String techListString = Arrays.toString(techList);
                            BigInteger uidInt = new BigInteger(uidString, 16);
                            verifyAccessCard(uidInt);

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                mifareClassic.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

                IsoDep isoDep = IsoDep.get(tag);

                try{

                    if(isoDep != null){
                        isoDep.connect();
                        byte[] uid = isoDep.getTag().getId();
                        String uidString = bytesToHexString(uid);

                        String[] techList = isoDep.getTag().getTechList();
                        String techListString = Arrays.toString(techList);

                        BigInteger uidInt = new BigInteger(uidString, 16);
                        verifyAccessCard(uidInt);



                    }

                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    try {
                        if(isoDep != null){
                            isoDep.close();
                        }

                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }


        }


    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("STAGE", "OnPause");
        //NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
        //nfcAdapter.disableForegroundDispatch(this);
    }

}