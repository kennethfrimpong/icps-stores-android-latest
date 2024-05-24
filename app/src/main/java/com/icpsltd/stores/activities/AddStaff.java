package com.icpsltd.stores.activities;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.icpsltd.stores.activities.NewIssue.FETCH_DELAY_TIME;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.icpsltd.stores.R;
import com.icpsltd.stores.adapterclasses.RetrievedStaff;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;
import com.icpsltd.stores.util.Variables;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.utils.TokenChecker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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

public class AddStaff extends AppCompatActivity {
    PendingIntent pendingIntent;
    BottomSheetDialog bottomSheetDialog;
    NfcAdapter nfcAdapter;

    private List<RetrievedStaff> fetchedStaffTable;

    ArrayAdapter<RetrievedStaff> arrayAdapter;

    private String receiver_full_name;

    private String receiver_dept;
    private Integer staffID;

    private String receiver_name;

    private String receiverFirstLast;

    private String canNumber;

    private boolean mIsCardReaderOpen = false;
    private static Biometrics.OnCardStatusListener onCardStatusListener;

    private OkHttpClient okHttpClient;

    private MaterialAutoCompleteTextView searchStaff;

    MaterialButton captureAccessButton;

    TextView department;
    TextView role;
    TextView addedBy;
    TextView dateAdded;
    TextView timeAdded;
    TextView accessIDstatus;
    
    String enrollmentStatus;

    BigInteger uidInt;

    BigInteger reservedUidInt;

    String apiHost;

    private Handler sHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        if(!dbHandler.getPrivilege().equals("Administrator")){
            Intent intent = new Intent(AddStaff.this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        sHandler = new Handler();


        apiHost = dbHandler.getApiHost();
        TextView asname = findViewById(R.id.firstLastName);
        asname.setText("as "+dbHandler.getFirstName()+" "+dbHandler.getLastName());
        /*

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

         */

        department = findViewById(R.id.department);
        role =  findViewById(R.id.role);
        addedBy =  findViewById(R.id.addedBy);
        dateAdded = findViewById(R.id.dateAdded);
        timeAdded = findViewById(R.id.timeAdded);
        accessIDstatus = findViewById(R.id.access_status);

        captureAccessButton = findViewById(R.id.capture_access);

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
                        runOnUiThread(()->{ Toast.makeText(AddStaff.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

        MaterialAutoCompleteTextView materialAutoCompleteTextView = findViewById(R.id.staff_name);
        searchStaff = findViewById(R.id.staff_name);

        TextView textView = findViewById(R.id.remove);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialAutoCompleteTextView.setEnabled(true);
                materialAutoCompleteTextView.setText("");
                textView.setVisibility(View.GONE);
                department.setText("");
                role.setText("");
                addedBy.setText("");
                dateAdded.setText("");
                timeAdded.setText("");
                accessIDstatus.setText("");
            }
        });

        try{
            fetchReceiverInfo();
        } catch (Exception e){
            e.printStackTrace();
        }

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sHandler.removeCallbacksAndMessages(null);
                sHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchReceiverInfo();
                    }
                }, FETCH_DELAY_TIME);

            }

            @Override
            public void afterTextChanged(Editable s) {
                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.staffFetchProgress);
                linearProgressIndicator.setVisibility(View.VISIBLE);
                if (searchStaff.getText().toString().equals("")){
                    linearProgressIndicator.setVisibility(View.GONE);
                }

            }
        };

        searchStaff.addTextChangedListener(textWatcher);

        dbHandler.close();
    }

    private void fetchReceiverInfo(){
        AddStaff.FetchReceiverInfo fetchReceiverInfo = new AddStaff.FetchReceiverInfo(new AddStaff.FetchReceiverInfoListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {

                MaterialAutoCompleteTextView materialAutoCompleteTextView = findViewById(R.id.staff_name);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.staffFetchProgress);
                        linearProgressIndicator.setVisibility(View.GONE);
                        fetchedStaffTable = new ArrayList<>();
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        SQLiteDatabase db = dbHandler.getReadableDatabase();
                        Cursor cursor = db.rawQuery("SELECT * FROM staffTable LIMIT 6",null);
                        while (cursor.moveToNext()){
                            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("staffID"));
                            String firstName = cursor.getString(cursor.getColumnIndexOrThrow("staffFirstName"));
                            String middleName = cursor.getString(cursor.getColumnIndexOrThrow("staffMiddleName"));
                            if(middleName.equals("null")){
                                middleName = "";
                            }
                            String lastName = cursor.getString(cursor.getColumnIndexOrThrow("staffLastName"));
                            receiverFirstLast = firstName+" "+lastName;
                            String type = cursor.getString(cursor.getColumnIndexOrThrow("staffType"));
                            String department = cursor.getString(cursor.getColumnIndexOrThrow("staffDepartment"));
                            canNumber = cursor.getString(cursor.getColumnIndexOrThrow("canNumber"));
                            Log.e("Retrieved ",firstName+" "+middleName+" "+lastName);

                            String addedBy = cursor.getString(cursor.getColumnIndexOrThrow("addedBy"));
                            String dateAdded = cursor.getString(cursor.getColumnIndexOrThrow("dateAdded"));
                            String timeAdded = cursor.getString(cursor.getColumnIndexOrThrow("timeAdded"));
                            String accessIDstatus = cursor.getString(cursor.getColumnIndexOrThrow("accessIDstatus"));

                            RetrievedStaff retrievedStaff = new RetrievedStaff(id,firstName,middleName,lastName,department,type,canNumber, addedBy, dateAdded, timeAdded, accessIDstatus);
                            fetchedStaffTable.add(retrievedStaff);

                        }

                        dbHandler.close();
                        db.close();
                    }
                });


                arrayAdapter = new ArrayAdapter<RetrievedStaff>(getApplicationContext(),R.layout.staff_list_layout,R.id.staff_name, fetchedStaffTable){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);

                        RetrievedStaff retrievedStaff = getItem(position);
                        TextView staff_name = view.findViewById(R.id.staff_name);
                        TextView staff_type = view.findViewById(R.id.staff_type);
                        TextView staff_department = view.findViewById(R.id.staff_department);

                        if(retrievedStaff.getMiddleName() != null){
                            //staff_name.setText(retrievedStaff.getfirstName()+" "+retrievedStaff.getMiddleName()+" "+retrievedStaff.getlastName());
                            staff_name.setText(retrievedStaff.getfirstName()+" "+retrievedStaff.getlastName());

                        } else {
                            staff_name.setText(retrievedStaff.getfirstName()+" "+retrievedStaff.getlastName());
                        }
                        staff_type.setText(retrievedStaff.getType());
                        staff_department.setText(retrievedStaff.getDepartment());

                        //Selected receiver

                        materialAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                RetrievedStaff retrievedStaff1 = getItem(position);
                                MaterialAutoCompleteTextView materialAutoCompleteTextView1 = findViewById(R.id.staff_name);
                                if(retrievedStaff1.getMiddleName() != null || retrievedStaff1.getMiddleName() != "null"){
                                    materialAutoCompleteTextView1.setText(retrievedStaff1.getfirstName()+" "+retrievedStaff1.getMiddleName()+" "+retrievedStaff1.getlastName());
                                    //receiver_full_name =retrievedStaff1.getfirstName()+" "+retrievedStaff1.getMiddleName()+" "+retrievedStaff1.getlastName();
                                    receiver_full_name =retrievedStaff1.getfirstName()+" "+retrievedStaff1.getlastName();


                                } else {
                                    materialAutoCompleteTextView1.setText(retrievedStaff1.getfirstName()+" "+retrievedStaff1.getlastName());
                                    receiver_full_name = retrievedStaff1.getfirstName()+" "+retrievedStaff1.getlastName();
                                }
                                ///MyPrefs myPrefs = new MyPrefs();
                                //myPrefs.saveReceiverFirstName();
                                receiver_name = retrievedStaff1.getfirstName();
                                receiver_dept = retrievedStaff1.getDepartment();
                                staffID = retrievedStaff1.getID();

                                TextView textView = findViewById(R.id.remove);
                                textView.setVisibility(View.VISIBLE);
                                materialAutoCompleteTextView1.setEnabled(false);

                                department.setText(receiver_dept);
                                role.setText(retrievedStaff1.getType());
                                addedBy.setText(retrievedStaff1.getAddedBy());
                                dateAdded.setText(retrievedStaff1.getDateAdded());
                                timeAdded.setText(retrievedStaff1.getTimeAdded());
                                accessIDstatus.setText(retrievedStaff1.getAccessIDstatus());

                                if(retrievedStaff1.getAccessIDstatus().equals("Enrolled")){
                                    accessIDstatus.setTextColor(Color.parseColor("#386641"));
                                    captureAccessButton.setText("Reset Access");
                                    captureAccessButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.icps_red)));

                                } else if (retrievedStaff1.getAccessIDstatus().equals("Not Enrolled")) {
                                    accessIDstatus.setTextColor(Color.RED);
                                    captureAccessButton.setText("Capture Access");
                                    captureAccessButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.icps_blue)));

                                } else{

                                    accessIDstatus.setTextColor(Color.BLACK);
                                    captureAccessButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.icps_blue)));
                                }

                            }
                        });

                        return view;
                    }
                };

                materialAutoCompleteTextView.setThreshold(0);
                materialAutoCompleteTextView.setAdapter(arrayAdapter);
                arrayAdapter.notifyDataSetChanged();

            }
        });
        fetchReceiverInfo.execute();

    }

    public void showBottomSheet(){

        int layout = R.layout.login_with_access_sheet;
        View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);

        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView title = bottomSheetView.findViewById(R.id.title);
        
        if(enrollmentStatus.equals("Enrolled")){
            title.setText("Reset Access ID"); 
        } else if (enrollmentStatus.equals("Not Enrolled")) {
            title.setText("Capture Access");
        }
        
        

        /*

        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            //NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);

        } else {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_SHORT).show();
            //nfcAdapter.disableForegroundDispatch(this);
        }

         */

        bottomSheetDialog.show();
        new OpenCardReaderAsync().execute();

        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //nfcAdapter.disableForegroundDispatch(NewIssue.this);
                //Toast.makeText(NewIssue.this, "Dialog dismissed", Toast.LENGTH_SHORT).show();
                mIsCardReaderOpen = false;
            }
        });

    }


    public void captureAccess(View view) {
        TextView textView = findViewById(R.id.remove);

        if(textView.getVisibility() == View.VISIBLE){
            mIsCardReaderOpen = true;
            showBottomSheet();

        } else{
            Toast.makeText(this, "Select a staff first", Toast.LENGTH_SHORT).show();
            mIsCardReaderOpen = false;

        }


    }

    public interface FetchReceiverInfoListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    public class FetchReceiverInfo extends AsyncTask<Void, Void, JSONArray> {
        private AddStaff.FetchReceiverInfoListener listener;

        public FetchReceiverInfo(AddStaff.FetchReceiverInfoListener listener){
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
                dbHandler1.clearStaffTable();
                String searchedString = searchStaff.getText().toString();
                String matchesWholeWord = searchedString;
                String startsWith = searchedString+"%";
                String withHyphen = searchedString+"-%";
                String startsWithWordInSentence = "% "+searchedString+"%";
                //Log.i("SearchTest","Updated Search Started");
                //Cursor cursor = db.rawQuery(,null);
                String sql = "{\"entry\":\""+searchedString+"\",\"type\":\"staff_fetch\"}";
                //send sql string to api and wait for results.
                try{
                    RequestBody requestBody =  RequestBody.create(sql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                            .post(requestBody)
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .build();
                    Response response = okHttpClient.newCall(request).execute();
                    String resString = response.body().string();



                    JSONObject jsonObject = new JSONObject(resString);
                    String status = jsonObject.getString("status");
                    Log.d("Status",status);

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status, getApplicationContext(), AddStaff.this);


                    resString = jsonObject.getString("result");
                    jsonArray = new JSONArray(resString);
                    Log.d("JSON Array", jsonArray.toString() );
                    response.close();

                } catch (Exception e){
                    e.printStackTrace();
                }

            } catch (Exception e) {
                Log.e("STAFF ADD FAILED", "Error adding staff", e);
            }
            dbHandler1.close();

            return jsonArray;

        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            super.onPostExecute(jsonArray);
            if (listener != null) {
                try{
                    DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        int staffID = jsonObject.getInt("staffID");
                        String firstName = jsonObject.getString("firstName");

                        String middleName = jsonObject.getString("middleName");
                        String lastName = jsonObject.getString("lastName");
                        String type = jsonObject.getString("type");
                        String department = jsonObject.getString("department");
                        String canNumber = jsonObject.getString("canNumber");

                        String addedBy = jsonObject.getString("addedBy");
                        String dateAdded = jsonObject.getString("addedDate");
                        String timeAdded = jsonObject.getString("addedTime");
                        String accessIDstatus = jsonObject.getString("accessIDstatus");
                        
                        enrollmentStatus = accessIDstatus;

                        dbHandler1.syncStaffTable(staffID,firstName,middleName,lastName,type,department,canNumber,addedBy,dateAdded,timeAdded,accessIDstatus);

                    }
                    dbHandler1.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
                listener.onTaskComplete(jsonArray);
            }

        }
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
                uidInt = null;
                //Toast.makeText(this, "sw1="+sw1, Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "sw1="+sw2, Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "data="+ Arrays.toString(data), Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "HexString="+bytesToHexString(data), Toast.LENGTH_SHORT).show();
                uidInt = new BigInteger(bytesToHexString(data), 16);

                //Toast.makeText(this, "Integer Value: "+String.valueOf(uidInt), Toast.LENGTH_LONG).show();
                if(mIsCardReaderOpen){
                    new AddStaff.VerifyStaffTask(new AddStaff.VerifyStaffTaskListener() {
                        @Override
                        public void onTaskComplete(String response) {
                            Log.i("Response",response);
                        }
                    }, String.valueOf(uidInt), "initial").execute();
                }

                //verifyAccessCard(uidInt);
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
            //bottomSheetDialog.dismiss();
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
                                new AddStaff.CardReading().execute();

                            }
                        }
                    };

                    App.BioManager.registerCardStatusListener(onCardStatusListener);
                    Functions.cancel_loader();
                } else {
                    Functions.cancel_loader();
                    Functions.Show_Alert2(AddStaff.this, "Card Opening Error", "Error Opening Card Reader" + resultCode.toString());
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
        boolean isCardReaderOpened = Functions.getSharedPreference(AddStaff.this).getBoolean(Variables.is_card_reader_open, false);

        if (isCardReaderOpened == false) {
            openCardReaderMain();
        } else {
            boolean cardConnection = App.BioManager.cardConnectSync(1000);
            if (cardConnection) {
                onCardStatusListener = new Biometrics.OnCardStatusListener() {
                    @Override
                    public void onCardStatusChange(String s, int prevState, int currentState) {
                        if (Variables.CARD_ABSENT == currentState) {
                            Functions.Show_Alert2(AddStaff.this, "CARD ABSENT", "Place Card on top on the device");

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

    private String bytesToHexString ( byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public interface VerifyStaffTaskListener {
        void onTaskComplete(String jsonResponse);
    }

    @SuppressLint("StaticFieldLeak")
    public class VerifyStaffTask extends AsyncTask<Void, Void, String> {
        private AddStaff.VerifyStaffTaskListener listener;
        private String pinHash;

        private String type;

        public VerifyStaffTask(AddStaff.VerifyStaffTaskListener listener, String pinHash, String type) {
            this.listener = listener;
            this.pinHash = pinHash;
            this.type = type;
        }

        @Override
        protected String doInBackground(Void... voids) {

            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            String userID = String.valueOf(dbHandler1.getIssuerID());
            String resString = "";
            String sql = "";
            try {
                if (type.equals("initial")){
                    sql = "{\"type\":\"access_registration\",\"newAccessID\":\""+pinHash+"\",\"staff_id\":\""+staffID+"\",\"userID\":\""+userID+"\",\"accessType\":\"initial\"}";

                } else if (type.equals("forced")) {
                    sql = "{\"type\":\"access_registration\",\"newAccessID\":\""+pinHash+"\",\"staff_id\":\""+staffID+"\",\"userID\":\""+userID+"\",\"accessType\":\"overwrite\"}";

                }
                String status = null;
                //returns latest issue id if verified

                //send sql string to api and wait for results.
                try{

                    MyPrefs myPrefs = new MyPrefs();
                    RequestBody requestBody =  RequestBody.create(sql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/accessID")
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .post(requestBody)
                            .build();
                    Response response = okHttpClient.newCall(request).execute();
                    resString = response.body().string();
                    Log.d("Status",resString);

                    JSONObject jsonObject = new JSONObject(resString);
                    status = jsonObject.getString("status");
                    response.close();





                } catch (Exception e){
                    e.printStackTrace();
                    status = "client_error";
                }

                TokenChecker tokenChecker = new TokenChecker();
                tokenChecker.checkToken(status, getApplicationContext(), AddStaff.this);

            } catch (Exception e) {
                Log.e("ID UPDATE FAILED", "Error updating ID", e);

            }

            dbHandler1.close();
            return resString;

        }

        @Override
        protected void onPostExecute(String jsonResponse) {
            if (listener != null) {
                listener.onTaskComplete(jsonResponse);
            }

            DBHandler dbHandler = new DBHandler(getApplicationContext());
            String status = "";
            try{

                JSONObject jsonObject = new JSONObject(jsonResponse);
                status = jsonObject.getString("status");

                if(status.equals("success")){
                    Toast.makeText(AddStaff.this, "Access successfully updated", Toast.LENGTH_LONG).show();
                    mIsCardReaderOpen = false;
                    bottomSheetDialog.dismiss();
                    accessIDstatus.setText("Enrolled");
                    accessIDstatus.setTextColor(Color.parseColor("#386641"));
                    captureAccessButton.setText("Reset Access");
                    captureAccessButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.icps_red)));

                } else if (status.equals("exists")) {
                    Toast.makeText(AddStaff.this, "User already exists with ID", Toast.LENGTH_LONG).show();
                    mIsCardReaderOpen = false;

                    String name = jsonObject.getString("name");
                    String department = jsonObject.getString("department");
                    String access = jsonObject.getString("access");

                    String message = "";

                    if(access.equals("Active")){
                        bottomSheetDialog.dismiss();
                        mIsCardReaderOpen = false;

                        message = "There is a registered and active user with this ID associated with the name "+name+" and "+department+" department. To register this access to another person, revoke access from web first";
                        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(AddStaff.this);
                        materialAlertDialogBuilder.setTitle("User already exists")
                                .setMessage(message)
                                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();

                                    }
                                });
                        AlertDialog alertDialog = materialAlertDialogBuilder.create();
                        alertDialog.show();
                    } else if (access.equals("Revoked")) {
                        mIsCardReaderOpen = false;
                        message = "There is a Revoked user with this ID associated with the name "+name+" and "+department+" department. Do you wish to reassign this Access ID to "+receiver_full_name+"?";
                        //create message strig with name and department bolded
                        SpannableString spannableString = new SpannableString(message);
                        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 62, 62+name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 62+name.length()+5, 62+name.length()+6+department.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


                        //message = "There is a Revoked user with this ID associated with the name "+name+" and "+department+" department. Do you wish to reassign this Access ID to "+receiver_full_name+"?";
                        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(AddStaff.this);
                        materialAlertDialogBuilder.setTitle("User already exists")
                                .setMessage(spannableString)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Toast.makeText(AddStaff.this, "No function available yet", Toast.LENGTH_SHORT).show();

                                        new AddStaff.VerifyStaffTask(new AddStaff.VerifyStaffTaskListener() {
                                            @Override
                                            public void onTaskComplete(String response) {
                                                Log.i("Response",response);
                                            }
                                        }, String.valueOf(uidInt), "forced").execute();


                                        bottomSheetDialog.dismiss();
                                        mIsCardReaderOpen = false;
                                    }
                                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        bottomSheetDialog.dismiss();
                                        mIsCardReaderOpen = false;
                                    }
                                });
                        AlertDialog alertDialog = materialAlertDialogBuilder.create();
                        alertDialog.show();


                    } else {
                        Toast.makeText(AddStaff.this, "There was an error", Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                        mIsCardReaderOpen = false;
                    }



                } else{
                    Toast.makeText(AddStaff.this, "Error updating Access ID", Toast.LENGTH_SHORT).show();
                    mIsCardReaderOpen = false;
                    bottomSheetDialog.dismiss();
                }

            } catch (Exception e){
                e.printStackTrace();
            }

            dbHandler.close();

        }
    }
}