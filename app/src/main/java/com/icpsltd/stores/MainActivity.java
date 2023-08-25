package com.icpsltd.stores;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;


import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private String URL;
    private String USER;
    private String PASSWORD;

    private OkHttpClient okHttpClient;

    private String email_to_check;

    private String password_to_check;

    private Response response;

    private String responseString;

    private LinearProgressIndicator linearProgressIndicator = null;

    private String apiHost;
    private String apiPort;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fetchCredentials();
        responseString = null;

        MyPrefs myPrefs = new MyPrefs();
        if (myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        configureHttpConnectionWithSSL();

        linearProgressIndicator = findViewById(R.id.login_progress);



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
        try{
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            String dbHost = dbHandler.getDatabaseHost();
            String dbName = dbHandler.getDatabaseName();
            String dbUser = dbHandler.getDatabaseUsername();
            String dbPass = dbHandler.getDatabasePassword();
            URL = "jdbc:mysql://"+dbHost+":3306/"+dbName;
            Log.i("URL",URL);
            USER = dbUser;
            PASSWORD = dbPass;
        } catch (Exception e){
            e.printStackTrace();
        }

    }


    public void showBottomSheet(){
        int layout = R.layout.login_with_access_sheet;
        View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
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

        try{
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            String dbHost = dbHandler.getDatabaseHost();
            String dbName = dbHandler.getDatabaseName();
            String dbUser = dbHandler.getDatabaseUsername();
            String dbPass = dbHandler.getDatabasePassword();
            URL = "jdbc:mysql://"+dbHost+":3306/"+dbName;
            Log.i("URL",URL);
            USER = dbUser;
            PASSWORD = dbPass;
        } catch (Exception e){
            e.printStackTrace();
            runOnUiThread(()->{linearProgressIndicator.setVisibility(View.GONE);});
            tv.setText("Server connection error, check configuration");
            tv.setVisibility(View.VISIBLE);
        }



        EditText email_input = findViewById(R.id.email_input);
        email_to_check = email_input.getText().toString();
        EditText password_input = findViewById(R.id.password_input);
        password_to_check = password_input.getText().toString();
        Security security = new Security();
        String passhash = security.createMD5Hash(password_to_check);
        Log.i("hash",passhash);

        TextView textViewName = findViewById(R.id.login_status);
        MyPrefs myPrefs = new MyPrefs();

        try{
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            String apiHost = dbHandler.getApiHost();
            String apiPort = dbHandler.getApiPort();

            if (!email_to_check.isEmpty()&&!password_to_check.isEmpty()){
                linearProgressIndicator.setVisibility(View.VISIBLE);

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future<String> futureResult = executorService.submit(() -> {
                    try{
                        String json = "{\"email\":\""+email_to_check+"\",\"password\":\""+passhash+"\"}";
                        RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                        Request request = new Request.Builder()
                                .url("https://"+apiHost+":"+apiPort+"/api/v1/login/")
                                .post(requestBody)
                                .build();
                        response = okHttpClient.newCall(request).execute();
                        String resString = response.body().string();
                        Log.i("Response",resString);
                        if(resString.equals("verified")){
                            return resString;
                        } else if (resString.equals("not verified")) {
                            runOnUiThread(()->{
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText("Incorrect Password");
                                textViewName.setTextColor(Color.parseColor("#E30613"));
                                myPrefs.saveLoginStatus(getApplicationContext(),false);
                                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            });

                            return resString;
                        } else if (resString.equals("Account not found")) {
                            runOnUiThread(()->{
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText("Account not found");
                                textViewName.setTextColor(Color.parseColor("#E30613"));
                                linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            });

                            return resString;
                        } else if (resString.equals("-1")) {
                            runOnUiThread(()->{
                                textViewName.setVisibility(View.VISIBLE);
                                textViewName.setText("Could not establish database connection");
                                textViewName.setTextColor(Color.parseColor("#E30613"));
                                linearProgressIndicator = findViewById(R.id.login_progress);
                                linearProgressIndicator.setVisibility(View.GONE);
                            });

                            return resString;}
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
                        new InfoAsyncTask().execute();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


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
    @SuppressLint("StaticFieldLeak")
    public class InfoAsyncTask extends AsyncTask<Void, Void, Map<String, String>> {
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            Map<String, String> info = new HashMap<>();

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
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


    @Override
    protected void onStart() {
        super.onStart();
        MyPrefs myPrefs = new MyPrefs();
        if (myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        configureHttpConnectionWithSSL();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyPrefs myPrefs = new MyPrefs();
        if (myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
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
                String json = "{\"email\":\""+email_to_check+"\",\"password\":\""+password_to_check+"\"}";
                RequestBody requestBody =  RequestBody.create(json, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://"+apiHost+":"+apiPort+"/api/v1/login/")
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
}