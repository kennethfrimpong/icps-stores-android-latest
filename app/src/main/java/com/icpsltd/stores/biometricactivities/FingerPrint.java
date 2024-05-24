package com.icpsltd.stores.biometricactivities;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.icao.GhanaIdCardFpTemplateInfo;
import com.google.android.material.card.MaterialCardView;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.R;
import com.icpsltd.stores.activities.HomePage;
import com.icpsltd.stores.activities.ReceiptPage;
import com.icpsltd.stores.model.CardDetails;
import com.icpsltd.stores.model.CardDetailsSummary;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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

public class FingerPrint extends AppCompatActivity {

    private CardDetails cardDetails = null;
    private byte[] mFingerprintOneFMDTemplate = null;

    private CardDetailsSummary cardDetailToHold = null;
    private static List<GhanaIdCardFpTemplateInfo> ghanaIdCardFpTemplateInfosList = new ArrayList<>();

    private int selectedFingerIndex = 0;

    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private AlertDialog alertDialog;

    private MaterialCardView fingerOne,fingerTwo,fingerThree,fingerFour;
    private TextView login_helper, finger_one_textview, finger_two_textview, finger_three_textview, finger_four_textview, can_number_textview;
    private String can_number;

    private ImageView finger_one_imageView, finger_two_imageView, finger_three_imageView, finger_four_imageView;
    private OkHttpClient okHttpClient;

    private String fromClassName =null;

    private String URL;
    private String USER;
    private String PASSWORD;

    private String balance;

    private Integer dbquantity;

    private Integer localquantity;

    private Integer new_quantity;

    private boolean blocktransaction = false;
    private String localname;

    private Integer new_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger_print);
        login_helper = findViewById(R.id.login_tv);
        fingerOne = findViewById(R.id.finger_one);
        fingerTwo = findViewById(R.id.finger_two);
        fingerThree = findViewById(R.id.finger_three);
        fingerFour = findViewById(R.id.finger_four);

        //finger textviews
        finger_one_textview = findViewById(R.id.finger_one_tv);
        finger_two_textview = findViewById(R.id.finger_two_tv);
        finger_three_textview = findViewById(R.id.finger_three_tv);
        finger_four_textview = findViewById(R.id.finger_four_tv);
        can_number_textview = findViewById(R.id.can_number_tv);

        //finger Imageviews
        finger_one_imageView = findViewById(R.id.finger_one_imageView);
        finger_two_imageView = findViewById(R.id.finger_two_imageView);
        finger_three_imageView = findViewById(R.id.finger_three_imageView);
        finger_four_imageView = findViewById(R.id.finger_four_imageView);

        try{
            can_number = getIntent().getStringExtra("can_number");
            can_number_textview.setText("CAN #: "+can_number);
            fromClassName = getIntent().getStringExtra("fromClassName");
            new_id = Integer.valueOf(getIntent().getStringExtra("new_id"));
            Log.i("CAN_NUMBER",can_number);

            DBHandler dbHandler = new DBHandler(getApplicationContext());
            String dbHost = dbHandler.getDatabaseHost();
            String dbName = dbHandler.getDatabaseName();
            String dbUser = dbHandler.getDatabaseUsername();
            String dbPass = dbHandler.getDatabasePassword();
            String apiHost = dbHandler.getApiHost();
            URL = "jdbc:mysql://"+dbHost+":3306/"+dbName;
            Log.i("URL",URL);
            USER = dbUser;
            PASSWORD = dbPass;

            CardReading cardReading = new CardReading();
            cardReading.execute();
        } catch (Exception e){
            e.printStackTrace();
            can_number_textview.setVisibility(View.GONE);
        }

        runOnUiThread(()->{
            login_helper.setText("Choose finger to scan");
            //Toast.makeText(getApplicationContext(),"Choose finger to scan",Toast.LENGTH_SHORT).show();

            fingerOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.GREEN);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.BLACK);
                    selectedFingerIndex = 1;
                    captureFingerprintOne(finger_one_imageView);
                }
            });

            fingerTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.GREEN);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.BLACK);
                    selectedFingerIndex = 2;
                    captureFingerprintOne(finger_two_imageView);
                }
            });

            fingerThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.GREEN);
                    fingerFour.setStrokeColor(Color.BLACK);
                    selectedFingerIndex = 3;
                    captureFingerprintOne(finger_three_imageView);
                }
            });

            fingerFour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.GREEN);
                    selectedFingerIndex = 4;
                    captureFingerprintOne(finger_four_imageView);

                }
            });

        });

        configureOKhttp();
    }

    private void configureOKhttp() {
        try{
            DBHandler dbHandler = new DBHandler(getApplicationContext());
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
            String apiHost = dbHandler.getApiHost();

            //remove this when ca trusted SSL is available and traffic is over https, remove hostname verifier method too in OkHttp builder

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if (hostname.equals(apiHost)){
                        return true;
                    } else {
                        runOnUiThread(()->{ Toast.makeText(FingerPrint.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

    public void restart_login(View view) {
        Intent intent = new Intent(FingerPrint.this, BiometricLogin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    private class CardReading extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Functions.Show_loader(FingerPrint.this, false, false);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
//
            cardDetails = Functions.readGhanaCard(FingerPrint.this, can_number, finger_one_textview, finger_two_textview, finger_three_textview, finger_four_textview);
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
                        Functions.show_toast(FingerPrint.this,hint);

                    }

                    if (OK == resultCode) {
                        Functions.show_toast(FingerPrint.this,"Finger Open Successful");

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
                Functions.Show_loader(FingerPrint.this, false, false);
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
                    Functions.show_toast(FingerPrint.this,"Authenticated Successfully");
                    if(v >70.0)
                    {

                        try {


                            JSONObject nhisData = new JSONObject();
                            nhisData.put("bioMatchResult","MFP");
                            nhisData.put("cardNo",cardDetailToHold.getCardNumber());
                            nhisData.put("cardType","GHANACARD");

                            try{

                                if(fromClassName != null && fromClassName.equals("com.icpsltd.stores.activities.NewIssue")){

                                    String new_date;
                                    String new_time;
                                    DBHandler dbHandler = new DBHandler(getApplicationContext());
                                    MyPrefs myPrefs = new MyPrefs();

                                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                                    Future<String> futureResult = executorService.submit(() -> {

                                        try{
                                            Request request = new Request.Builder()
                                                    .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/tst/getDateTime")
                                                    .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                                    //.post(requestBody)
                                                    .build();
                                            Response response = okHttpClient.newCall(request).execute();
                                            String resString = response.body().string();
                                            Log.i("Response",resString);

                                            response.close();
                                            return resString;
                                        } catch (Exception e){
                                            e.printStackTrace();

                                        }
                                        return null;
                                    });

                                    try {
                                        String result = futureResult.get();
                                        JSONObject jsonObject = new JSONObject(result);
                                        new_date = jsonObject.optString("date");
                                        new_time = jsonObject.optString("time");

                                        String staffID = getIntent().getStringExtra("staffID");
                                        String receiver_full_name = getIntent().getStringExtra("receiver_full_name");
                                        String receiver_dept = getIntent().getStringExtra("receiver_dept");

                                        String bookNumber = getIntent().getStringExtra("bookNumber");
                                        String issuer_name = getIntent().getStringExtra("issuer_name");

                                        dbHandler.addOngoingIssueMeta(new_id, dbHandler.getIssuerID(),issuer_name, Integer.valueOf(staffID),receiver_full_name,receiver_dept,new_date,new_time);
                                        dbHandler.updateOngoingIssueTable(new_id, dbHandler.getIssuerID(),issuer_name, Integer.valueOf(staffID),receiver_full_name,receiver_dept,new_date,new_time,bookNumber,null);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }


                                    SyncQuantityTask syncQuantityTask = new SyncQuantityTask(new SyncQuantityTaskListener() {
                                        @Override
                                        public void onTaskComplete(Connection connection) {
                                            if (!blocktransaction==true){

                                                SyncHistoryTask syncHistoryTask = new SyncHistoryTask(new SyncHistoryTaskListener() {
                                                    @Override
                                                    public void onTaskComplete(Connection connection) {
                                                        Intent intent = new Intent(FingerPrint.this, ReceiptPage.class);
                                                        startActivity(intent);
                                                    }
                                                });
                                                syncHistoryTask.execute();
                                            } else {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(FingerPrint.this, "No data was synced", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }

                                        }
                                    });
                                    syncQuantityTask.execute();
                                } else{
                                    GetStaffInfo getStaffInfo = new GetStaffInfo(new GetStaffInfoListener() {
                                        @Override
                                        public void onTaskComplete(JSONArray jsonArray) {
                                            Log.i("biotest1","Full name: "+cardDetailToHold.getFullName());
                                            Log.i("biotest1","Card Number: "+cardDetailToHold.getCardNumber());
                                            Log.i("biotest1","DOB: "+cardDetailToHold.getDateOfBirth());
                                            Log.i("biotest1","Gender: "+cardDetailToHold.getGender());

                                            try{
                                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                for (int i = 0; i < jsonArray.length(); i++) {
                                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                    int staffID = jsonObject.getInt("userID");
                                                    String firstName = jsonObject.getString("firstName");
                                                    String privilege = jsonObject.getString("privilege");
                                                    String lastName = jsonObject.getString("lastName");
                                                    dbHandler1.addUserSession(String.valueOf(staffID),firstName,lastName,privilege,1,null);

                                                }
                                                MyPrefs myPrefs = new MyPrefs();
                                                myPrefs.saveLoginStatus(getApplicationContext(),true);
                                                Intent intent = new Intent(FingerPrint.this, HomePage.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);

                                            } catch (Exception e){
                                                e.printStackTrace();
                                            }

                                        }
                                    });
                                    getStaffInfo.execute();
                                }

                            } catch (Exception e){
                                e.printStackTrace();
                            }


                        }catch (Exception e)
                        {
                            Functions.cancel_loader();
                            e.printStackTrace();
                            alertDialog = Functions.showDialogWithOneButton(
                                    FingerPrint.this,
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

                        alertDialog = Functions.showNotVerifiedDialog(FingerPrint.this, "12345", view -> {

                            alertDialog.dismiss();
                            App.BioManager.closeFingerprintReader();
                            App.BioManager.cardCloseCommand();
                            App.BioManager.cardDisconnectSync(1);
                            Intent intent = new Intent(FingerPrint.this, BiometricLogin.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

    public interface GetStaffInfoListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    public class GetStaffInfo extends AsyncTask<Void, Void, JSONArray>{
        private FingerPrint.GetStaffInfoListener listener;

        public GetStaffInfo(FingerPrint.GetStaffInfoListener listener){
            this.listener = listener;

        }


        @Override
        protected JSONArray doInBackground(Void... voids) {

            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            dbHandler1.clearStaffTable();
            JSONArray jsonArray = null;
            MyPrefs myPrefs = new MyPrefs();
            try {

                String sql = "{\"type\":\"staff_login\",\"can_number\":\""+can_number+"\"}";
                //send sql string to api and wait for results.
                try{
                    RequestBody requestBody =  RequestBody.create(sql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .post(requestBody)
                            .build();
                    Response response = okHttpClient.newCall(request).execute();
                    String resString = response.body().string();

                    jsonArray = new JSONArray(resString);


                    response.close();

                } catch (Exception e){
                    e.printStackTrace();

                }

            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

            }

            return jsonArray;

        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            super.onPostExecute(jsonArray);
            if (listener != null) {
                listener.onTaskComplete(jsonArray);


            }

        }
    }

    public interface SyncHistoryTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class SyncHistoryTask extends AsyncTask<Void, Void, Connection> {
        private FingerPrint.SyncHistoryTaskListener listener;

        public SyncHistoryTask(FingerPrint.SyncHistoryTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.openDatabase(getApplicationContext());
            Cursor cursor = db.rawQuery("SELECT * FROM ongoingIssueTable",null);

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                Statement statement = connection.createStatement();


                while (cursor.moveToNext()){
                    //get balance
                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    Integer TransactionID = cursor.getInt(cursor.getColumnIndexOrThrow("TransactionID"));
                    String sqlx = "SELECT productQuantity FROM stockTable WHERE id ='"+id+"'";
                    PreparedStatement statementx = connection.prepareStatement(sqlx);
                    ResultSet resultSetx = statementx.executeQuery();
                    while (resultSetx.next()){
                        Integer fetchedQTY = resultSetx.getInt("productQuantity");
                        balance = String.valueOf(fetchedQTY);
                    }





                    String ProductName = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                    String ProductDesc = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                    Integer ProductQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
                    String ProductStore = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                    String ProductLocation = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));

                    Integer IssuerID = cursor.getInt(cursor.getColumnIndexOrThrow("IssuerID"));
                    String IssuerName = cursor.getString(cursor.getColumnIndexOrThrow("IssuerName"));
                    Integer ReceiverID = cursor.getInt(cursor.getColumnIndexOrThrow("ReceiverID"));
                    String ReceiverName = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverName"));
                    String ReceiverDept = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverDepartment"));
                    String TransactionDate = cursor.getString(cursor.getColumnIndexOrThrow("TransactionDate"));
                    String TransactionTime = cursor.getString(cursor.getColumnIndexOrThrow("TransactionTime"));
                    String BookNumber = cursor.getString(cursor.getColumnIndexOrThrow("BookNumber"));
                    String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));

                    String sql = "INSERT INTO issueHistoryTable (`TransactionID`, `id`, `ProductName`, `ProductDesc`, `ProductQuantity`, `ProductStore`, `ProductLocation`, `IssuerID`, `IssuerName`, `ReceiverID`, `ReceiverName`, `ReceiverDepartment`, `TransactionDate`, `TransactionTime`, `BookNumber`, `TransactionType`, `Balance`, `ProductUnit`) VALUES ('"+TransactionID+"','"+id+"', '"+ProductName+"', '"+ProductDesc+"', '"+ProductQuantity+"', '"+ProductStore+"', '"+ProductLocation+"', '"+IssuerID+"', '"+IssuerName+"','"+ReceiverID+"', '"+ReceiverName+"', '"+ReceiverDept+"', '"+TransactionDate+"', '"+TransactionTime+"','"+BookNumber+"','Stock Issue','"+balance+"','"+unit+"');";

                    statement.executeUpdate(sql);



                }

                Integer updatedID = new_id;
                String sqlx = "UPDATE metaTable SET lastID = "+updatedID+" WHERE activityName = 'stockIssue';";
                statement.executeUpdate(sqlx);

            } catch (Exception e) {
                Log.e("SYNC HISTORY TASK", "Error syncing history", e);

            }
            return null;
        }

        @Override
        protected void onPostExecute(Connection connection) {
            if (listener != null) {
                listener.onTaskComplete(connection);
            }

            try {


            } catch (Exception e) {
                Log.e("SYNC HISTORY TASK", "Error syncing history", e);

            }

        }
    }

    public interface SyncQuantityTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class SyncQuantityTask extends AsyncTask<Void, Void, Connection> {
        private FingerPrint.SyncQuantityTaskListener listener;

        public SyncQuantityTask(FingerPrint.SyncQuantityTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.openDatabase(getApplicationContext());
            Cursor cursor = db.rawQuery("SELECT id, ProductQuantity, ProductName FROM ongoingIssueTable",null);

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                //PreparedStatement statement = (PreparedStatement) connection.createStatement();



                while (cursor.moveToNext()){

                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    localquantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
                    localname = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));

                    String sql = "SELECT productQuantity FROM stockTable WHERE id ='"+id+"'";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery();


                    while (resultSet.next()){
                        dbquantity = resultSet.getInt("productQuantity");
                        if(dbquantity>=localquantity){
                            blocktransaction = false;

                        } else if (localquantity>dbquantity) {
                            blocktransaction = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(FingerPrint.this, "Quantity of "+localname+" exceeds quantity in database.", Toast.LENGTH_LONG).show();
                                    Toast.makeText(FingerPrint.this, "Please update with new quantity of "+localname, Toast.LENGTH_LONG).show();
                                }
                            });
                            break;
                        } else{
                            blocktransaction = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(FingerPrint.this, "Quantity of "+localname+" has changed in database.", Toast.LENGTH_LONG).show();
                                    Toast.makeText(FingerPrint.this, "Please  update with new quantity of "+localname, Toast.LENGTH_LONG).show();
                                }
                            });
                            break;
                        }
                    }
                    if(blocktransaction){
                        break;
                    }

                }


                if (!blocktransaction){
                    Cursor cursorx = db.rawQuery("SELECT id, ProductQuantity, ProductName FROM ongoingIssueTable",null);

                    while (cursorx.moveToNext()){

                        Integer idx = cursorx.getInt(cursorx.getColumnIndexOrThrow("id"));
                        localquantity = cursorx.getInt(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                        localname = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));

                        String sqlx = "SELECT productQuantity FROM stockTable WHERE id ='"+idx+"'";
                        PreparedStatement statement = connection.prepareStatement(sqlx);
                        ResultSet resultSetx = statement.executeQuery();

                        while (resultSetx.next()){
                            dbquantity = resultSetx.getInt("productQuantity");
                            Log.i("RT",String.valueOf(dbquantity));
                            if(dbquantity>=localquantity){
                                blocktransaction = false;
                                new_quantity = dbquantity-localquantity;

                                String quantity_update = "UPDATE stockTable SET productQuantity ="+new_quantity+" WHERE id='"+idx+"'";
                                PreparedStatement preparedStatement = connection.prepareStatement(quantity_update);
                                preparedStatement.executeUpdate();
                                //statement.executeUpdate(quantity_update);
                            } else{
                                blocktransaction = true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(FingerPrint.this, "Quantity of "+localname+" has changed in database. Please go back and update with new quantity", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    }
                    Integer updatedID = new_id;
                    String sqlx = "UPDATE metaTable SET lastID = "+updatedID+" WHERE activityName = 'stockIssue';";
                    PreparedStatement statement = connection.prepareStatement(sqlx);
                    statement.executeUpdate();

                }



            } catch (Exception e) {
                Log.e("SYNC HISTORY TASK", "Error syncing history", e);

            }
            return null;
        }

        @Override
        protected void onPostExecute(Connection connection) {
            if (listener != null) {
                listener.onTaskComplete(connection);
            }

        }
    }

}