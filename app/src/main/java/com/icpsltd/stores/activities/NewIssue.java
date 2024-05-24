package com.icpsltd.stores.activities;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.icpsltd.stores.R;
import com.icpsltd.stores.adapterclasses.RetrievedStaff;
import com.icpsltd.stores.adapterclasses.RetrievedStock;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;
import com.icpsltd.stores.util.Variables;
import com.icpsltd.stores.utils.CustomEditText;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.utils.IssueHistoryData;
import com.icpsltd.stores.utils.ItemLocationParser;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.utils.TokenChecker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
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

public class NewIssue extends AppCompatActivity {

    private List<RetrievedStock> fetchedStockTable;
    private List<RetrievedStaff> fetchedStaffTable;
    private List<RetrievedStock> fetchedOngoingIssueTable;

    private CustomEditText searchString;
    private ArrayAdapter<RetrievedStock> adapter;
    private ArrayAdapter<RetrievedStock> ongoingIssueAdapter;

    ArrayAdapter<RetrievedStaff> arrayAdapter;
    private ListView ongoingListview;
    private MaterialAutoCompleteTextView searchStaff;

    private String receiver_name;

    private Float balance;

    private Integer receiver_ID;
    private String receiver_full_name;

    private String receiver_dept;
    private Integer staffID;

    private Integer staffInt;

    private Integer latestIssueID;

    private Integer new_id;

    private Float item_qty;

    private Float dbquantity;

    private Float localquantity;

    private Float new_quantity;

    private boolean blocktransaction = false;
    private String localname;

    private OkHttpClient okHttpClient;

    private String canNumber;

    private String receiverFirstLast;

    private Response response;

    private String IssueValue;
    private String IssueRate;

    private Timer timer;



    PendingIntent pendingIntent;

    NfcAdapter nfcAdapter;

    LinearProgressIndicator bslinearProgressIndicator;

    BottomSheetDialog bsbottomSheetDialog;

    int height;

    EditText jobNumberEditText;

    private String FifoStatus;

    private boolean mIsCardReaderOpen = false;
    private static Biometrics.OnCardStatusListener onCardStatusListener;
    private MyPrefs myPrefs;

    private ImageView thumbnail;
    private TextView imageCaptureStatus;

    private String base64Image = "";
    private boolean isImageCaptured = false;

    private Handler shandler;

    public static final int FETCH_DELAY_TIME = 700;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_issue);
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        String dbHost = dbHandler.getDatabaseHost();
        String dbName = dbHandler.getDatabaseName();
        String dbUser = dbHandler.getDatabaseUsername();
        String dbPass = dbHandler.getDatabasePassword();
        String apiHost = dbHandler.getApiHost();
        dbHandler.clearOngoingIssueTable();
        myPrefs = new MyPrefs();
        timer = new Timer();
        shandler = new Handler();
        //URL = "jdbc:mysql://"+dbHost+":3306/"+dbName;
        //Log.i("URL",URL);
        //USER = dbUser;
        //PASSWORD = dbPass;

        jobNumberEditText = findViewById(R.id.job_number);

        thumbnail = findViewById(R.id.thumbnail);
        imageCaptureStatus = findViewById(R.id.imageCaptureStatus);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;

        /*

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

         */


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
                        runOnUiThread(()->{ Toast.makeText(NewIssue.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

        searchStaff = findViewById(R.id.receiver_name);
        searchStaff.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                TextView no_item_txt = findViewById(R.id.no_item_txt);
                if(hasFocus){
                    no_item_txt.setVisibility(View.GONE);
                } else {
                    no_item_txt.setVisibility(View.VISIBLE);
                }
            }
        });


        EditText editText = findViewById(R.id.book_number);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                TextView no_item_txt = findViewById(R.id.no_item_txt);
                if(hasFocus){
                    no_item_txt.setVisibility(View.GONE);
                } else {
                    no_item_txt.setVisibility(View.VISIBLE);
                }
            }
        });

        EditText editText1 = findViewById(R.id.job_number);
        editText1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                TextView no_item_txt = findViewById(R.id.no_item_txt);
                if(hasFocus){
                    no_item_txt.setVisibility(View.GONE);
                } else {
                    no_item_txt.setVisibility(View.VISIBLE);
                }
            }
        });

        try{
            fetchReceiverInfo();
        } catch (Exception e){
            e.printStackTrace();
        }


        TextView asname = findViewById(R.id.firstLastName);
        asname.setText("as "+dbHandler.getFirstName()+" "+dbHandler.getLastName());
        ongoingListview = findViewById(R.id.ongoing_item_list);
        ongoingListview.setVisibility(View.VISIBLE);
        ongoingIssue();

        MaterialAutoCompleteTextView materialAutoCompleteTextView = findViewById(R.id.receiver_name);

        TextView textView = findViewById(R.id.remove);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialAutoCompleteTextView.setEnabled(true);
                materialAutoCompleteTextView.setText("");
                textView.setVisibility(View.GONE);
            }
        });


        TextInputEditText textInputEditText = findViewById(R.id.book_number);



        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                shandler.removeCallbacksAndMessages(null);
                shandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchReceiverInfo();
                    }
                },FETCH_DELAY_TIME);

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

        int layout = R.layout.add_issue_item;
        View bottomSheet = LayoutInflater.from(this).inflate(layout, null);


        //BottomSheetDialog bottomSheet = findViewById(R.id.add);

        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getCurrentFocus() != null) {
                    if (isTouchInsideBottomSheetContent(event)) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        v.requestFocus();
                    }
                }
                return false;
            }
        });

        dbHandler.close();


    }

    private boolean isTouchInsideBottomSheetContent(MotionEvent event) {
        int layout = R.layout.add_issue_item;
        View bottomSheet = LayoutInflater.from(this).inflate(layout, null);
        int bottomSheetHeight = (int) (bottomSheet.getHeight() * 1);
        int[] location = new int[2];
        bottomSheet.getLocationOnScreen(location);
        int bottomSheetTop = location[1];
        int bottomSheetBottom = bottomSheetTop + bottomSheetHeight;

        float touchX = event.getRawX();
        float touchY = event.getRawY();

        return touchX >= bottomSheet.getLeft() && touchX <= bottomSheet.getRight()
                && touchY >= bottomSheetTop && touchY <= bottomSheetBottom;
    }

    private void fetchReceiverInfo(){

        FetchReceiverInfo fetchReceiverInfo = new FetchReceiverInfo(new FetchReceiverInfoListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {

                MaterialAutoCompleteTextView materialAutoCompleteTextView = findViewById(R.id.receiver_name);
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

                            RetrievedStaff retrievedStaff = new RetrievedStaff(id,firstName,middleName,lastName,department,type,canNumber,null,null,null,null);
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
                                MaterialAutoCompleteTextView materialAutoCompleteTextView1 = findViewById(R.id.receiver_name);
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

    public void ongoingIssue(){
        fetchedOngoingIssueTable = new ArrayList<>();
        //load existing ongoing issue
        DBHandler dbHandler = new DBHandler(getApplicationContext());

        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit FROM ongoingIssueTable",null);
        while (cursor.moveToNext()){
            TextView textView = findViewById(R.id.no_item_txt);
            textView.setVisibility(View.GONE);
            String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
            Float quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("ProductQuantity"));
            String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
            String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
            String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
            Log.e("Retrieved",name);

            RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,location,store,unit,null,null);
            fetchedOngoingIssueTable.add(retrievedStock);

        }
        ongoingIssueAdapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedOngoingIssueTable) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView store_location = view.findViewById(R.id.store_location);
                TextView product_name = view.findViewById(R.id.product_name);
                TextView product_quantity = view.findViewById(R.id.product_quantity);
                TextView product_location = view.findViewById(R.id.product_location);
                RetrievedStock retrievedStock = getItem(position);
                product_name.setText(retrievedStock.getName());
                product_name.setSelected(true);
                if (product_name.length() > 15){
                    product_name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    //make textview scrollable
                    //product_name.setMovementMethod(new ScrollingMovementMethod());

                }
                store_location.setText(retrievedStock.getStore());
                product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                product_location.setText(retrievedStock.getLocation());
                DBHandler dbHandler1 = new DBHandler(getApplicationContext());

                ongoingListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        RetrievedStock retrievedStock = getItem(position);


                        String ID = retrievedStock.getID();
                        if(item_qty==null){
                            item_qty = dbHandler1.checkOngoingIssueItemQuantity(ID);
                        }

                        //TextView item_title = new TextView(NewIssue.this);
                        //item_title.setText(retrievedStock.getName());
                        //item_title.setTypeface(ResourcesCompat.getFont(NewIssue.this, R.font.sfprodisplaybold));

                        View editLayout = getLayoutInflater().inflate(R.layout.edit_item, null);
                        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(NewIssue.this);
                        materialAlertDialogBuilder.setTitle("Edit "+retrievedStock.getName())
                                .setView(editLayout)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        CustomEditText customEditText = editLayout.findViewById(R.id.issue_quantity_update);
                                        if(Float.valueOf(customEditText.getText().toString()) > 0){
                                            //customEditText.setText(String.valueOf(inc+1));
                                            dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),Float.valueOf(customEditText.getText().toString()),retrievedStock.getStore(),retrievedStock.getLocation(), retrievedStock.getUnit(),null);
                                            Toast.makeText(getApplicationContext(),retrievedStock.getName()+" updated successfully",Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(),"Enter a valid quantity for "+retrievedStock.getName(),Toast.LENGTH_SHORT).show();
                                        }


                                        //update listview
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ongoingIssue();
                                            }
                                        });


                                    }
                                }).setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                        dbHandler1.deleteFromOngoingIssueTable(retrievedStock.getID());

                                        //update listview
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ongoingIssue();
                                            }
                                        });
                                        dialog.dismiss();
                                        TextView tv = findViewById(R.id.no_item_txt);
                                        if(dbHandler1.checkOngoingIssueTableEmptiness().equals("empty")){
                                            tv.setVisibility(View.VISIBLE);
                                        }

                                    }
                                });
                        AlertDialog alertDialog = materialAlertDialogBuilder.create();
                        CustomEditText customEditText = editLayout.findViewById(R.id.issue_quantity_update);
                        AppCompatImageButton increasebutton = editLayout.findViewById(R.id.plus);
                        AppCompatImageButton decreasebutton = editLayout.findViewById(R.id.minus);
                        //these check if
                        increasebutton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Float inc = Float.valueOf(customEditText.getText().toString());
                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                if (Float.valueOf(customEditText.getText().toString())>0){
                                    customEditText.setText(String.valueOf(inc+1));
                                }

                                /**
                                if(dbHandler1.checkOngoingIssueItemQuantity(retrievedStock.getID()) >= 0){
                                    if(Integer.valueOf(customEditText.getText().toString()) < dbHandler1.checkOngoingIssueItemQuantity(retrievedStock.getID())){
                                        customEditText.setText(String.valueOf(inc+1));
                                    }
                                } else {
                                    //Toast.makeText(NewIssue.this,"NOT FOUND",Toast.LENGTH_SHORT).show();
                                }
                                 **/

                                dbHandler1.close();


                            }
                        });
                        decreasebutton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Float inc = Float.valueOf(customEditText.getText().toString());
                                if (customEditText.getText().toString().equals("1")){

                                } else{
                                    customEditText.setText(String.valueOf(inc-1));
                                }

                            }
                        });
                        customEditText.setText(String.valueOf(retrievedStock.getQuantity()));
                        alertDialog.show();




                    }
                });

                return view;
            }
        };

        assert ongoingListview != null;
        ongoingListview.setAdapter(ongoingIssueAdapter);
        ongoingIssueAdapter.notifyDataSetChanged();

        dbHandler.close();
        db.close();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsCardReaderOpen = false;

    }

    private void makeIssue(){
        TextView rmtv = findViewById(R.id.remove);
        EditText bknm = findViewById(R.id.book_number);
        DBHandler dbHandler = new DBHandler(getApplicationContext());

        if(rmtv.getVisibility() == View.VISIBLE){
            if (!bknm.getText().toString().equals("") && !jobNumberEditText.getText().toString().equals("")){

                if(dbHandler.checkOngoingIssueTableEmptiness().equals("notempty")){
                    mIsCardReaderOpen = true;

                    if (mIsCardReaderOpen) {
                        //NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
                        /*
                        String fromClassName = NewIssue.class.getName();
                        Intent intent = new Intent(NewIssue.this, SplashScreen.class);
                        intent.putExtra("fromClassName",fromClassName);
                        intent.putExtra("canNumber",canNumber);
                        intent.putExtra("name",receiverFirstLast);
                        //change to fetch actual
                        //latestIssueID = 2051;
                        //new_id = latestIssueID+1;
                        intent.putExtra("new_id",String.valueOf(new_id));
                        TextInputEditText bk = findViewById(R.id.book_number);
                        String bookNumber = bk.getText().toString();
                        intent.putExtra("bookNumber",bookNumber);
                        String issuer_name = dbHandler.getFirstName()+" "+dbHandler.getLastName();
                        intent.putExtra("staffID",staffID);
                        intent.putExtra("receiver_full_name",receiver_full_name);
                        intent.putExtra("receiver_dept",receiver_dept);

                        intent.putExtra("issuer_name",issuer_name);
                        Log.i("className",fromClassName);

                         */

                        //startActivity(intent);

                        int layout = R.layout.confirm_transaction_sheet;
                        View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);
                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

                        /*
                        try{
                            BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());

                            bottomSheetBehavior.setPeekHeight(0);
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            bottomSheetBehavior.setDraggable(false);
                        } catch (Exception e){
                            e.printStackTrace();
                        }

                         */


                        bsbottomSheetDialog = bottomSheetDialog;
                        bottomSheetDialog.setContentView(bottomSheetView);
                        TextView textView = bottomSheetDialog.findViewById(R.id.receiver_confirm);
                        EditText passpin = bottomSheetDialog.findViewById(R.id.pin_input);
                        LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.verify_progress);
                        bslinearProgressIndicator = linearProgressIndicator;

                        //nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
                        //triggered native android nfc
                        bottomSheetDialog.show();

                        new OpenCardReaderAsync().execute();

                        textView.setText(receiver_name+", Confirm with Access ID");


                        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                //nfcAdapter.disableForegroundDispatch(NewIssue.this);
                                //Toast.makeText(NewIssue.this, "Dialog dismissed", Toast.LENGTH_SHORT).show();
                                mIsCardReaderOpen = false;
                            }
                        });


                    } else {
                        Toast.makeText(this, "NFC is not available", Toast.LENGTH_SHORT).show();
                        try{

                            //nfcAdapter.disableForegroundDispatch(this);
                        } catch (Exception e){
                            e.printStackTrace();

                        }

                    }






                } else {
                    Toast.makeText(this, "Add at least 1 item", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Add a book/job number first", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Select a receiver from list first", Toast.LENGTH_SHORT).show();
        }

        dbHandler.close();
    }

    public void create_issue(View view) {
        if(!isImageCaptured){
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(NewIssue.this);
            materialAlertDialogBuilder.setTitle("No image captured")
                    .setMessage("Do you want to proceed without capturing an image of the issue?")
                    .setPositiveButton("Capture", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            View view1 = new View(NewIssue.this);
                            goToCamera(view1);
                        }
                    }).setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(NewIssue.this, "Please capture an image of the issue before proceeding", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            makeIssue();
                        }
                    });
            AlertDialog alertDialog = materialAlertDialogBuilder.create();
            alertDialog.show();
        } else{
            makeIssue();
        }
    }

    public void showBottomSheet(){

        int layout = R.layout.add_issue_item;
        View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());

        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);




        LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.fetch_progress);
        linearProgressIndicator.setVisibility(View.VISIBLE);
        searchString = bottomSheetDialog.findViewById(R.id.stockSearch);
        ListView listView = (ListView) bottomSheetDialog.findViewById(R.id.add_items_listview);
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        searchString.setInputType(InputType.TYPE_CLASS_TEXT);


        GetStockTable getStockTable = new GetStockTable(new GetStockTableListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {
                //Log.i("checkpoint",String.valueOf(j));


                try{
                    DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Log.i("checkpoint",jsonObject.toString());

                        String itemCode = jsonObject.getString("itemCode");
                        String productName = jsonObject.getString("productName");

                        String productDesc = jsonObject.getString("productDesc");
                        double productQuantity = jsonObject.getDouble("productQuantity");
                        String productStore = jsonObject.getString("productStore");
                        String productLocation = jsonObject.getString("productLocation");
                        String productUnit = jsonObject.getString("productUnit");
                        String imageAvailable = jsonObject.optString("imageAvailable");

                        ItemLocationParser itemLocationParser = new ItemLocationParser();
                        String[] parsedLocation = itemLocationParser.parseLocation(productLocation);
                        productStore = parsedLocation[0];
                        productLocation = parsedLocation[1];



                        try{
                            dbHandler1.syncStockTable(itemCode,productName,productDesc,(float)productQuantity,productStore,productLocation,productUnit,imageAvailable);
                        } catch (Exception e){
                            e.printStackTrace();
                        }

                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("checkpoint","UI Thread started");
                            linearProgressIndicator.setVisibility(View.GONE);

                            fetchedStockTable = new ArrayList<>();
                            DBHandler dbHandler = new DBHandler(getApplicationContext());
                            SQLiteDatabase db = dbHandler.getReadableDatabase();
                            Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit, ImageAvailable FROM stockTable LIMIT 10",null);
                            while (cursor.moveToNext()){
                                String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                                String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                                Float quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("ProductQuantity"));
                                String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                                String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                                String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                                String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
                                String imageAvailable = cursor.getString(cursor.getColumnIndexOrThrow("ImageAvailable"));
                                Log.e("Retrieved",name);

                                RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,location,store,unit,null,imageAvailable);
                                fetchedStockTable.add(retrievedStock);

                            }

                            adapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedStockTable) {
                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    View view = super.getView(position, convertView, parent);

                                    TextView store_location = view.findViewById(R.id.store_location);
                                    TextView product_name = view.findViewById(R.id.product_name);
                                    TextView product_quantity = view.findViewById(R.id.product_quantity);
                                    TextView product_location = view.findViewById(R.id.product_location);
                                    RetrievedStock retrievedStock = getItem(position);
                                    product_name.setText(retrievedStock.getName());
                                    product_name.setSelected(true);
                                    if (product_name.length() > 15){
                                        product_name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                        //make textview scrollable
                                        //product_name.setMovementMethod(new ScrollingMovementMethod());

                                    }
                                    store_location.setText(retrievedStock.getStore());
                                    product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                                    product_location.setText(retrievedStock.getLocation());
                                    Log.i("Item",retrievedStock.getName());


                                    return view;
                                }
                            };

                            assert listView != null;
                            listView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    RetrievedStock retrievedStock = adapter.getItem(position);
                                    TextInputLayout textInputLayout = bottomSheetDialog.findViewById(R.id.search_item_box);
                                    textInputLayout.setVisibility(View.GONE);
                                    listView.setVisibility(View.GONE);
                                    MaterialButton materialButton = bottomSheetDialog.findViewById(R.id.search_item_button);
                                    materialButton.setVisibility(View.GONE);

                                    InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    manager.hideSoftInputFromWindow(view.getWindowToken(), 0);


                                    //Set issue quantity views visible

                                    MaterialButton materialButton2 = bottomSheetDialog.findViewById(R.id.item_code_display);
                                    materialButton2.setVisibility(View.VISIBLE);

                                    ConstraintLayout constraintLayout1 = bottomSheetDialog.findViewById(R.id.name_box);
                                    constraintLayout1.setVisibility(View.VISIBLE);


                                    ConstraintLayout constraintLayout2 = bottomSheetDialog.findViewById(R.id.location_box);
                                    constraintLayout2.setVisibility(View.VISIBLE);

                                    ConstraintLayout constraintLayout3 = bottomSheetDialog.findViewById(R.id.store_box);
                                    constraintLayout3.setVisibility(View.VISIBLE);

                                    ConstraintLayout constraintLayout4 = bottomSheetDialog.findViewById(R.id.quantity_box);
                                    constraintLayout4.setVisibility(View.VISIBLE);

                                    TextInputLayout textInputLayout2 = bottomSheetDialog.findViewById(R.id.quantity_entry_box);
                                    textInputLayout2.setVisibility(View.VISIBLE);

                                    MaterialButton materialButton3 = bottomSheetDialog.findViewById(R.id.add_item_button);
                                    materialButton3.setVisibility(View.VISIBLE);

                                    //this populates textviews with selected item
                                    String name = retrievedStock.getName();
                                    String location = retrievedStock.getLocation();
                                    String store = retrievedStock.getStore();
                                    String retrievedquantity = String.valueOf(retrievedStock.getQuantity());
                                    String retrievedunit = retrievedStock.getUnit();

                                    String itemCode = retrievedStock.getID();
                                    materialButton2.setText(itemCode);

                                    TextView name_tv = bottomSheetView.findViewById(R.id.item_name);
                                    name_tv.setText(name);
                                    name_tv.setSelected(true);
                                    //make textview scrollable
                                    name_tv.setMovementMethod(new ScrollingMovementMethod());
                                    //Toast.makeText(NewIssue.this, name, Toast.LENGTH_SHORT).show();


                                    TextView location_tv = bottomSheetView.findViewById(R.id.item_location);
                                    location_tv.setText(location);

                                    TextView store_tv = bottomSheetView.findViewById(R.id.item_store);
                                    store_tv.setText(store);

                                    TextView qty_tv = bottomSheetView.findViewById(R.id.item_quantity_remaining);
                                    qty_tv.setText(retrievedquantity+" "+retrievedunit);

                                    CustomEditText customEditText = bottomSheetDialog.findViewById(R.id.issue_quantity);


                                    materialButton3.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            //Toast.makeText(getApplicationContext(),String.valueOf(retrievedStock.getID()),Toast.LENGTH_SHORT).show();
                                            String squantity = customEditText.getText().toString();
                                            try {
                                                if (!squantity.equals("") && !squantity.equals("0")){
                                                    //////
                                                    Float quantity = Float.valueOf(squantity);
                                                    if (quantity <= retrievedStock.getQuantity()) {
                                                        DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                        dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),quantity,retrievedStock.getStore(),retrievedStock.getLocation(),retrievedStock.getUnit(),null);
                                                        Toast.makeText(getApplicationContext(),retrievedStock.getName()+" added successfully",Toast.LENGTH_SHORT).show();

                                                        //update listview
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ongoingIssue();
                                                            }
                                                        });
                                                        bottomSheetDialog.dismiss();

                                                        dbHandler1.close();

                                                    } else if (quantity > retrievedStock.getQuantity()) {
                                                        Toast.makeText(getApplicationContext(),"Error: Quantity entered exceeds retrieved quantity ",Toast.LENGTH_LONG).show();

                                                    }
                                                } else {
                                                    Toast.makeText(getApplicationContext(),"Enter a valid quantity ",Toast.LENGTH_LONG).show();
                                                }

                                            } catch (Exception e){
                                                Log.e("Error",e.toString());
                                                Toast.makeText(getApplicationContext(),"Enter a valid number",Toast.LENGTH_LONG).show();
                                            }




                                        }
                                    });
                                }
                            });

                            dbHandler.close();


                        }
                    });

                    dbHandler1.close();

                } catch (Exception e){
                    e.printStackTrace();
                }




            }
        }, "false");
        getStockTable.execute();



        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{

                            adapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedStockTable);
                            adapter.clear();
                            adapter.notifyDataSetChanged();

                        } catch (Exception e){
                            e.printStackTrace();
                        }



                    }
                });




            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //implement debounce or delay to avoid multiple requests with the handler class
                shandler.removeCallbacksAndMessages(null);
                shandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                String searchedString = searchString.getText().toString();
                                String firstPer = "%"+searchedString;
                                String queryString = firstPer+"%";


                                if (searchString.getText().toString().length() < 1){
                                    //fetches default 5 most issued items if search box is empty, change values in getStockStable and here to update results number
                                    GetStockTable getStockTable = new GetStockTable(new GetStockTableListener() {
                                        @Override
                                        public void onTaskComplete(JSONArray jsonArray) {
                                            Log.i("checkpoint","Task Completed");

                                            try{
                                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                for (int i = 0; i < jsonArray.length(); i++) {
                                                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                                                    String itemCode = jsonObject.optString("itemCode");
                                                    String productName = jsonObject.getString("productName");

                                                    String productDesc = jsonObject.getString("productDesc");
                                                    double productQuantity = jsonObject.getDouble("productQuantity");
                                                    String productStore = jsonObject.getString("productStore");
                                                    String productLocation = jsonObject.getString("productLocation");
                                                    String productUnit = jsonObject.getString("productUnit");
                                                    String imageAvailable = jsonObject.optString("imageAvailable");

                                                    ItemLocationParser itemLocationParser = new ItemLocationParser();
                                                    String[] parsedLocation = itemLocationParser.parseLocation(productLocation);
                                                    productStore = parsedLocation[0];
                                                    productLocation = parsedLocation[1];

                                                    dbHandler1.syncStockTable(itemCode,productName,productDesc,(float)productQuantity,productStore,productLocation,productUnit,imageAvailable);

                                                }
                                                Log.i("checkpoint","INSERT COMPLETED");

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.i("checkpoint","UI Thread started");
                                                        linearProgressIndicator.setVisibility(View.GONE);

                                                        List<RetrievedStock> fetchedStockTable = new ArrayList<>();
                                                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                                                        SQLiteDatabase db = dbHandler.getReadableDatabase();
                                                        Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit, ImageAvailable FROM stockTable LIMIT 10",null);
                                                        while (cursor.moveToNext()){
                                                            String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                                                            String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                                                            Float quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("ProductQuantity"));
                                                            String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                                                            String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                                                            String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                                                            String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
                                                            String imageAvailable = cursor.getString(cursor.getColumnIndexOrThrow("ImageAvailable"));
                                                            Log.e("Retrieved",name);

                                                            RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,location,store,unit,null,imageAvailable);
                                                            fetchedStockTable.add(retrievedStock);

                                                        }

                                                        ArrayAdapter<RetrievedStock> adapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedStockTable) {
                                                            @Override
                                                            public View getView(int position, View convertView, ViewGroup parent) {
                                                                View view = super.getView(position, convertView, parent);

                                                                TextView store_location = view.findViewById(R.id.store_location);
                                                                TextView product_name = view.findViewById(R.id.product_name);
                                                                TextView product_quantity = view.findViewById(R.id.product_quantity);
                                                                TextView product_location = view.findViewById(R.id.product_location);
                                                                RetrievedStock retrievedStock = getItem(position);
                                                                product_name.setText(retrievedStock.getName());
                                                                product_name.setSelected(true);
                                                                if (product_name.length() > 15){
                                                                    product_name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                                                    //make textview scrollable
                                                                    //product_name.setMovementMethod(new ScrollingMovementMethod());

                                                                }
                                                                store_location.setText(retrievedStock.getStore());
                                                                product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                                                                product_location.setText(retrievedStock.getLocation());
                                                                Log.i("Item",retrievedStock.getName());


                                                                return view;
                                                            }
                                                        };

                                                        assert listView != null;
                                                        listView.setAdapter(adapter);
                                                        adapter.notifyDataSetChanged();

                                                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                            @Override
                                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                                RetrievedStock retrievedStock = adapter.getItem(position);
                                                                TextInputLayout textInputLayout = bottomSheetDialog.findViewById(R.id.search_item_box);
                                                                textInputLayout.setVisibility(View.GONE);
                                                                listView.setVisibility(View.GONE);
                                                                MaterialButton materialButton = bottomSheetDialog.findViewById(R.id.search_item_button);
                                                                materialButton.setVisibility(View.GONE);

                                                                InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                                                manager.hideSoftInputFromWindow(view.getWindowToken(), 0);


                                                                //Set issue quantity views visible

                                                                MaterialButton materialButton2 = bottomSheetDialog.findViewById(R.id.item_code_display);
                                                                materialButton2.setVisibility(View.VISIBLE);

                                                                ConstraintLayout constraintLayout1 = bottomSheetDialog.findViewById(R.id.name_box);
                                                                constraintLayout1.setVisibility(View.VISIBLE);


                                                                ConstraintLayout constraintLayout2 = bottomSheetDialog.findViewById(R.id.location_box);
                                                                constraintLayout2.setVisibility(View.VISIBLE);

                                                                ConstraintLayout constraintLayout3 = bottomSheetDialog.findViewById(R.id.store_box);
                                                                constraintLayout3.setVisibility(View.VISIBLE);

                                                                ConstraintLayout constraintLayout4 = bottomSheetDialog.findViewById(R.id.quantity_box);
                                                                constraintLayout4.setVisibility(View.VISIBLE);

                                                                TextInputLayout textInputLayout2 = bottomSheetDialog.findViewById(R.id.quantity_entry_box);
                                                                textInputLayout2.setVisibility(View.VISIBLE);

                                                                MaterialButton materialButton3 = bottomSheetDialog.findViewById(R.id.add_item_button);
                                                                materialButton3.setVisibility(View.VISIBLE);

                                                                //this populates textviews with selected item
                                                                String name = retrievedStock.getName();
                                                                String location = retrievedStock.getLocation();
                                                                String store = retrievedStock.getStore();
                                                                String retrievedquantity = String.valueOf(retrievedStock.getQuantity());
                                                                String retrievedunit = retrievedStock.getUnit();

                                                                String itemCode = retrievedStock.getID();
                                                                materialButton2.setText(itemCode);

                                                                TextView name_tv = bottomSheetView.findViewById(R.id.item_name);
                                                                name_tv.setText(name);
                                                                name_tv.setText(name);
                                                                name_tv.setSelected(true);
                                                                //make textview scrollable
                                                                name_tv.setMovementMethod(new ScrollingMovementMethod());
                                                                Toast.makeText(NewIssue.this, name, Toast.LENGTH_SHORT).show();

                                                                TextView location_tv = bottomSheetView.findViewById(R.id.item_location);
                                                                location_tv.setText(location);

                                                                TextView store_tv = bottomSheetView.findViewById(R.id.item_store);
                                                                store_tv.setText(store);

                                                                TextView qty_tv = bottomSheetView.findViewById(R.id.item_quantity_remaining);
                                                                qty_tv.setText(retrievedquantity+" "+retrievedunit);

                                                                CustomEditText customEditText = bottomSheetDialog.findViewById(R.id.issue_quantity);


                                                                materialButton3.setOnClickListener(new View.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(View v) {
                                                                        //Toast.makeText(getApplicationContext(),String.valueOf(retrievedStock.getID()),Toast.LENGTH_SHORT).show();
                                                                        String squantity = customEditText.getText().toString();
                                                                        try {
                                                                            if (!squantity.equals("") && !squantity.equals("0")){
                                                                                Float quantity = Float.valueOf(squantity);
                                                                                if (quantity <= retrievedStock.getQuantity()) {
                                                                                    DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                                                    dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),quantity,retrievedStock.getStore(),retrievedStock.getLocation(),retrievedStock.getUnit(),null);
                                                                                    Toast.makeText(getApplicationContext(),retrievedStock.getName()+" added successfully",Toast.LENGTH_SHORT).show();

                                                                                    //update listview
                                                                                    runOnUiThread(new Runnable() {
                                                                                        @Override
                                                                                        public void run() {
                                                                                            ongoingIssue();
                                                                                        }
                                                                                    });
                                                                                    bottomSheetDialog.dismiss();
                                                                                    dbHandler1.close();





                                                                                } else if (quantity > retrievedStock.getQuantity()) {
                                                                                    Toast.makeText(getApplicationContext(),"Error: Quantity entered exceeds retrieved quantity ",Toast.LENGTH_LONG).show();

                                                                                }
                                                                            } else {
                                                                                Toast.makeText(getApplicationContext(),"Enter a valid quantity ",Toast.LENGTH_LONG).show();
                                                                            }

                                                                        } catch (Exception e){

                                                                            Toast.makeText(getApplicationContext(),"Enter a valid number",Toast.LENGTH_LONG).show();
                                                                        }




                                                                    }
                                                                });
                                                            }
                                                        });

                                                        dbHandler.close();


                                                    }
                                                });

                                                dbHandler1.close();

                                            } catch (Exception e){
                                                e.printStackTrace();
                                            }




                                        }
                                    }, "false");
                                    getStockTable.execute();

                                } else {

                                    DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                    dbHandler1.clearStockTable();

                                    GetStockTable getStockTable = new GetStockTable(new GetStockTableListener() {
                                        @Override
                                        public void onTaskComplete(JSONArray jsonArray) {
                                            Log.i("checkpoint","Task Completed");
                                            String productDesc = "";
                                            double productQuantity = 0.00;
                                            String productStore = "";
                                            String productLocation = "";
                                            String productUnit = "";

                                            try{
                                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                for (int i = 0; i < jsonArray.length(); i++) {
                                                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                                                    try{
                                                        String itemCode = jsonObject.optString("itemCode");

                                                        String productName = jsonObject.getString("productName");

                                                        productDesc = jsonObject.getString("productDesc");

                                                        if(jsonObject.optString("productQuantity").equals("null")){
                                                            productQuantity = 0;
                                                        } else {
                                                            productQuantity = jsonObject.getDouble("productQuantity");
                                                        }


                                                        //productQuantity = jsonObject.getInt("productQuantity");
                                                        productStore = jsonObject.getString("productStore");
                                                        productLocation = jsonObject.getString("productLocation");
                                                        productUnit = jsonObject.getString("productUnit");
                                                        String imageAvailable = jsonObject.optString("imageAvailable");

                                                        ItemLocationParser itemLocationParser = new ItemLocationParser();
                                                        String[] parsedLocation = itemLocationParser.parseLocation(productLocation);
                                                        productStore = parsedLocation[0];
                                                        productLocation = parsedLocation[1];

                                                        dbHandler1.syncStockTable(itemCode,productName,productDesc,(float)productQuantity,productStore,productLocation,productUnit,imageAvailable);

                                                    } catch (Exception e){
                                                        e.printStackTrace();
                                                    }



                                                }
                                                Log.i("checkpoint","INSERT COMPLETED");

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        Log.i("checkpoint","UI Thread started");
                                                        linearProgressIndicator.setVisibility(View.GONE);

                                                        List<RetrievedStock> fetchedStockTable = new ArrayList<>();
                                                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                                                        SQLiteDatabase db = dbHandler.getReadableDatabase();
                                                        Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit, ImageAvailable FROM stockTable LIMIT 10",null);
                                                        while (cursor.moveToNext()){
                                                            String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                                                            String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                                                            Float quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("ProductQuantity"));
                                                            String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                                                            String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                                                            String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                                                            String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
                                                            String imageAvailable = cursor.getString(cursor.getColumnIndexOrThrow("ImageAvailable"));
                                                            Log.e("Retrieved",name);

                                                            RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,location,store,unit,null,imageAvailable);
                                                            fetchedStockTable.add(retrievedStock);

                                                        }

                                                        ArrayAdapter<RetrievedStock> adapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedStockTable) {
                                                            @Override
                                                            public View getView(int position, View convertView, ViewGroup parent) {
                                                                View view = super.getView(position, convertView, parent);

                                                                TextView store_location = view.findViewById(R.id.store_location);
                                                                TextView product_name = view.findViewById(R.id.product_name);
                                                                TextView product_quantity = view.findViewById(R.id.product_quantity);
                                                                TextView product_location = view.findViewById(R.id.product_location);
                                                                RetrievedStock retrievedStock = getItem(position);
                                                                product_name.setText(retrievedStock.getName());
                                                                product_name.setText(retrievedStock.getName());
                                                                product_name.setSelected(true);
                                                                if (product_name.length() > 15){
                                                                    product_name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                                                    //make textview scrollable
                                                                    //product_name.setMovementMethod(new ScrollingMovementMethod());

                                                                }
                                                                store_location.setText(retrievedStock.getStore());
                                                                product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                                                                product_location.setText(retrievedStock.getLocation());
                                                                Log.i("Item",retrievedStock.getName());


                                                                return view;
                                                            }
                                                        };

                                                        assert listView != null;
                                                        listView.setAdapter(adapter);
                                                        adapter.notifyDataSetChanged();

                                                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                            @Override
                                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                                RetrievedStock retrievedStock = adapter.getItem(position);
                                                                TextInputLayout textInputLayout = bottomSheetDialog.findViewById(R.id.search_item_box);
                                                                textInputLayout.setVisibility(View.GONE);
                                                                listView.setVisibility(View.GONE);
                                                                MaterialButton materialButton = bottomSheetDialog.findViewById(R.id.search_item_button);
                                                                materialButton.setVisibility(View.GONE);

                                                                InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                                                manager.hideSoftInputFromWindow(view.getWindowToken(), 0);


                                                                //Set issue quantity views visible

                                                                MaterialButton materialButton2 = bottomSheetDialog.findViewById(R.id.item_code_display);
                                                                materialButton2.setVisibility(View.VISIBLE);

                                                                ConstraintLayout constraintLayout1 = bottomSheetDialog.findViewById(R.id.name_box);
                                                                constraintLayout1.setVisibility(View.VISIBLE);


                                                                ConstraintLayout constraintLayout2 = bottomSheetDialog.findViewById(R.id.location_box);
                                                                constraintLayout2.setVisibility(View.VISIBLE);

                                                                ConstraintLayout constraintLayout3 = bottomSheetDialog.findViewById(R.id.store_box);
                                                                constraintLayout3.setVisibility(View.VISIBLE);

                                                                ConstraintLayout constraintLayout4 = bottomSheetDialog.findViewById(R.id.quantity_box);
                                                                constraintLayout4.setVisibility(View.VISIBLE);

                                                                TextInputLayout textInputLayout2 = bottomSheetDialog.findViewById(R.id.quantity_entry_box);
                                                                textInputLayout2.setVisibility(View.VISIBLE);

                                                                MaterialButton materialButton3 = bottomSheetDialog.findViewById(R.id.add_item_button);
                                                                materialButton3.setVisibility(View.VISIBLE);

                                                                //this populates textviews with selected item
                                                                String name = retrievedStock.getName();
                                                                String location = retrievedStock.getLocation();
                                                                String store = retrievedStock.getStore();
                                                                String retrievedquantity = String.valueOf(retrievedStock.getQuantity());
                                                                String retrievedunit = retrievedStock.getUnit();

                                                                String itemCode = retrievedStock.getID();
                                                                materialButton2.setText(itemCode);

                                                                TextView name_tv = bottomSheetView.findViewById(R.id.item_name);
                                                                name_tv.setText(name);

                                                                name_tv.setSelected(true);
                                                                if (name.length() > 15){
                                                                    name_tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                                                }
                                                                //Toast.makeText(NewIssue.this, name, Toast.LENGTH_SHORT).show();


                                                                TextView location_tv = bottomSheetView.findViewById(R.id.item_location);
                                                                location_tv.setText(location);

                                                                TextView store_tv = bottomSheetView.findViewById(R.id.item_store);
                                                                store_tv.setText(store);

                                                                TextView qty_tv = bottomSheetView.findViewById(R.id.item_quantity_remaining);
                                                                qty_tv.setText(retrievedquantity+" "+retrievedunit);

                                                                CustomEditText customEditText = bottomSheetDialog.findViewById(R.id.issue_quantity);


                                                                materialButton3.setOnClickListener(new View.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(View v) {
                                                                        //Toast.makeText(getApplicationContext(),String.valueOf(retrievedStock.getID()),Toast.LENGTH_SHORT).show();
                                                                        String squantity = customEditText.getText().toString();
                                                                        try {
                                                                            if (!squantity.equals("") && !squantity.equals("0")){
                                                                                Float quantity = Float.valueOf(squantity);
                                                                                if (quantity <= retrievedStock.getQuantity()) {
                                                                                    DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                                                    dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),quantity,retrievedStock.getStore(),retrievedStock.getLocation(),retrievedStock.getUnit(),null);
                                                                                    Toast.makeText(getApplicationContext(),retrievedStock.getName()+" added successfully",Toast.LENGTH_SHORT).show();

                                                                                    //update listview
                                                                                    runOnUiThread(new Runnable() {
                                                                                        @Override
                                                                                        public void run() {
                                                                                            ongoingIssue();
                                                                                        }
                                                                                    });
                                                                                    bottomSheetDialog.dismiss();

                                                                                    dbHandler1.close();





                                                                                } else if (quantity > retrievedStock.getQuantity()) {
                                                                                    Toast.makeText(getApplicationContext(),"Error: Quantity entered exceeds retrieved quantity ",Toast.LENGTH_LONG).show();

                                                                                }
                                                                            } else {
                                                                                Toast.makeText(getApplicationContext(),"Enter a valid quantity ",Toast.LENGTH_LONG).show();
                                                                            }

                                                                        } catch (Exception e){
                                                                            Toast.makeText(getApplicationContext(),"Enter a valid number",Toast.LENGTH_LONG).show();
                                                                        }




                                                                    }
                                                                });
                                                            }
                                                        });

                                                        dbHandler.close();



                                                    }
                                                });

                                                dbHandler1.close();

                                            } catch (Exception e){
                                                e.printStackTrace();
                                            }




                                        }
                                    }, searchedString);
                                    getStockTable.execute();

                                }
                            }
                        });

                    }
                }, FETCH_DELAY_TIME);


            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedStockTable);
                adapter.notifyDataSetChanged();



            }
        };
        searchString.addTextChangedListener(textWatcher);

        searchString.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode==KeyEvent.KEYCODE_DEL){
                    Log.e("KEYCODE","Back Pressed");
                }
                return true;
            }
        });


        bottomSheetDialog.setCanceledOnTouchOutside(true);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ongoingIssueAdapter.notifyDataSetChanged();
            }
        });

        bottomSheetDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK){
                    bottomSheetDialog.dismiss();
                    ongoingIssueAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });

        AppCompatImageView appCompatImageView = bottomSheetDialog.findViewById(R.id.drag_handle);
        appCompatImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        final View touchOutsideView = bottomSheetDialog.getWindow().getDecorView().findViewById(com.google.android.material.R.id.touch_outside);
        touchOutsideView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                bottomSheetDialog.dismiss();
            }
        });
        bottomSheetDialog.show();

        dbHandler.close();
        db.close();
    }


    public void add_item(View view) {
        DBHandler dbHandler1 = new DBHandler(getApplicationContext());
        dbHandler1.clearStockTable();
        dbHandler1.close();
        showBottomSheet();

    }

    public void goToConfigure(View view) {
        Intent intent = new Intent(NewIssue.this, Configure.class);
        startActivity(intent);
    }


    public void goToCamera(View view) {
        // Launch the camera activity
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File jpgImage = new File(getFilesDir(),"image.jpg");
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", jpgImage));
        startActivityForResult(cameraIntent, 1);

        /*
        Intent intent = new Intent(NewIssue.this, CaptureImage.class);
        startActivityForResult(intent,1);

         */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {

            //Bitmap thumbData = (Bitmap) data.getExtras().get("data");
            Bitmap bitmap;
            String pathname = getFilesDir()+"/image.jpg";
            base64Image = "";
            isImageCaptured = false;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(pathname)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,25, byteArrayOutputStream);
            //save bitmap

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            //Log.d("Image  Byte Array", Arrays.toString(byteArray));
            File jpgImage = new File(getFilesDir(),"image.jpg");
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(jpgImage.getPath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            try {
                fileOutputStream.write(byteArray);
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            base64Image = Base64.getEncoder().encodeToString(byteArray);
            File file = new File(pathname);
            boolean deleteFile = file.delete();
            if(deleteFile){
                Toast.makeText(this, "Image Captured Successfully", Toast.LENGTH_SHORT).show();
                isImageCaptured = true;
                imageCaptureStatus.setText("Image Captured");
                imageCaptureStatus.setTextColor(Color.parseColor("#1cd41c"));
                //remove attribute : app:layout_constraintEnd_toEndOf="parent"
                ConstraintSet constraintSet = new ConstraintSet();
                ConstraintLayout constraintLayout = findViewById(R.id.imageLayout);
                constraintSet.clone(constraintLayout);
                //constraintSet.connect(R.id.imageCaptureStatus,ConstraintSet.END,R.id.image_capture_button,ConstraintSet.START,0);
                constraintSet.clear(R.id.imageCaptureStatus,ConstraintSet.END);
                constraintSet.applyTo(constraintLayout);


                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) imageCaptureStatus.getLayoutParams();
                params.setMargins(25,0,0,0);
                imageCaptureStatus.setLayoutParams(params);

                MaterialButton removeImage = findViewById(R.id.removeImage);
                MaterialButton previewImage = findViewById(R.id.previewImage);
                previewImage.setVisibility(View.VISIBLE);
                removeImage.setVisibility(View.VISIBLE);
                removeImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        imageCaptureStatus.setText("No Image Captured");
                        imageCaptureStatus.setTextColor(Color.parseColor("#ff0000"));
                        ConstraintSet constraintSet = new ConstraintSet();
                        ConstraintLayout constraintLayout = findViewById(R.id.imageLayout);
                        constraintSet.clone(constraintLayout);
                        constraintSet.connect(R.id.imageCaptureStatus,ConstraintSet.END,R.id.imageLayout,ConstraintSet.END,0);
                        constraintSet.applyTo(constraintLayout);

                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) imageCaptureStatus.getLayoutParams();
                        params.setMargins(15,0,15,0);
                        imageCaptureStatus.setLayoutParams(params);

                        MaterialButton removeImage = findViewById(R.id.removeImage);
                        removeImage.setVisibility(View.GONE);
                        previewImage.setVisibility(View.GONE);
                        base64Image = "";
                        isImageCaptured = false;
                        thumbnail.setImageBitmap(null);
                        //android:src="@color/gray"
                        //thumbnail.setBackgroundColor(Color.parseColor("#FFBDBEC2"));
                        thumbnail.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                    }
                });


                thumbnail.setImageBitmap(bitmap);
                //thumbnail.setBackgroundColor(Color.parseColor("#FFBDBEC2"));

                thumbnail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayThumbnail(bitmap);
                    }
                });
                previewImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayThumbnail(bitmap);
                    }
                });
            }
            //Toast.makeText(this, String.valueOf(base64Image), Toast.LENGTH_SHORT).show();
            //Log.d("Image",base64Image);



        }
    }

    private void displayThumbnail(Bitmap bitmap){
        //open dialog to view image
        Dialog dialog = new Dialog(NewIssue.this);
        dialog.setContentView(R.layout.image_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ImageView imageView = dialog.findViewById(R.id.image_dialog);
        imageView.setImageBitmap(bitmap);

        MaterialButton button = dialog.findViewById(R.id.close_dialog);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    public interface GetStockTableListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    public class GetStockTable extends AsyncTask<Void, Void, JSONArray>{
        private GetStockTableListener listener;
        private String stringToQuery;

        public GetStockTable(GetStockTableListener listener, String queryString){
            this.listener = listener;
            this.stringToQuery = queryString;

        }


        @Override
        protected JSONArray doInBackground(Void... voids) {

            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            dbHandler1.clearStaffTable();
            JSONArray jsonArray = null;
            try {
                String sql = "";

                if(stringToQuery.equals("false")){
                    sql = "{\"type\":\"default_stock\"}";
                } else{
                    sql = "{\"type\":\"queried_stock\",\"condition\":\""+stringToQuery+"\"}";
                }

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
                    JSONObject jsonObject1 = new JSONObject(resString);

                    String status1 = jsonObject1.optString("status");
                    resString = jsonObject1.optString("data");

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status1, getApplicationContext(), NewIssue.this);

                    jsonArray = new JSONArray(resString);


                    response.close();

                } catch (Exception e){
                    e.printStackTrace();

                }

            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

            }
            dbHandler1.close();

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

    public interface FetchReceiverInfoListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    public class FetchReceiverInfo extends AsyncTask<Void, Void, JSONArray>{
        private FetchReceiverInfoListener listener;

        public FetchReceiverInfo(FetchReceiverInfoListener listener){
            this.listener = listener;

        }


        @Override
        protected JSONArray doInBackground(Void... voids) {

            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            dbHandler1.clearStaffTable();
            JSONArray jsonArray = null;
            try {
                dbHandler1.clearStaffTable();
                String searchedString = searchStaff.getText().toString();
                String matchesWholeWord = searchedString;
                String startsWith = searchedString+"%";
                String withHyphen = searchedString+"-%";
                String startsWithWordInSentence = "% "+searchedString+"%";
                Log.i("SearchTest","Updated Search Started");
                //Cursor cursor = db.rawQuery(,null);
                String sql = "{\"entry\":\""+searchedString+"\",\"type\":\"staff_fetch\"}";
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

                    Log.d("Response",resString);

                    JSONObject jsonObject1 = new JSONObject(resString);

                    String status1 = jsonObject1.optString("status");
                    resString = jsonObject1.optString("result");

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status1, getApplicationContext(), NewIssue.this);
                    //Log.d("Response",resString);
                    //Log.d("Status",status1);
                    jsonArray = new JSONArray(resString);
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
                        dbHandler1.syncStaffTable(staffID,firstName,middleName,lastName,type,department,canNumber,null,null,null,null);

                    }
                    dbHandler1.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
                listener.onTaskComplete(jsonArray);
            }

        }
    }

    public interface VerifyStaffTaskListener {
        void onTaskComplete(String jsonResponse);
    }

    @SuppressLint("StaticFieldLeak")
    public class VerifyStaffTask extends AsyncTask<Void, Void, String> {
        private VerifyStaffTaskListener listener;
        private String pinHash;

        public VerifyStaffTask(VerifyStaffTaskListener listener, String pinHash) {
            this.listener = listener;
            this.pinHash = pinHash;
        }

        @Override
        protected String doInBackground(Void... voids) {

            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            String resString = "";
            try {
                String sql = "{\"type\":\"staff_verify\",\"condition\":\""+pinHash+"\",\"staff_id\":\""+staffID+"\",\"activity_name\":\"stockIssue\",\"verify_type\":\"self\",\"access_verify\":\"true\"}";

                //returns latest issue id if verified

                //send sql string to api and wait for results.
                try{

                    RequestBody requestBody =  RequestBody.create(sql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .post(requestBody)
                            .build();
                    Response response = okHttpClient.newCall(request).execute();
                    resString = response.body().string();
                    JSONObject jsonObject1 = new JSONObject(resString);

                    String status1 = jsonObject1.optString("status");
                    resString = jsonObject1.optString("data");

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status1, getApplicationContext(), NewIssue.this);
                    response.close();

                } catch (Exception e){
                    e.printStackTrace();

                }

            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

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
            Integer latestID = null;
            try{

                JSONObject jsonObject = new JSONObject(jsonResponse);
                status = jsonObject.getString("status");
                latestID = jsonObject.getInt("lastID");


            } catch (Exception e){
                e.printStackTrace();
            }

            if (status.equals("verified") && latestID != null){

                latestIssueID = latestID;

                //get latest transaction ID, put to ongoingIssueMetaTransactionID, sync other data to ongoingIssueMetaTable
                new_id = latestIssueID+1;
                String issuer_name = dbHandler.getFirstName()+" "+dbHandler.getLastName();

                String new_date;
                String new_time;

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
                        JSONObject jsonObject1 = new JSONObject(resString);

                        String status1 = jsonObject1.optString("status");
                        resString = jsonObject1.optString("data");

                        TokenChecker tokenChecker = new TokenChecker();
                        tokenChecker.checkToken(status1, getApplicationContext(), NewIssue.this);

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


                    TextInputEditText bk = findViewById(R.id.book_number);
                    String bookNumber = bk.getText().toString();
                    String jobNumber = jobNumberEditText.getText().toString();
                    Log.e("RECEIVER DEPARTMENT",": "+receiver_dept);


                    dbHandler.addOngoingIssueMeta(new_id, dbHandler.getIssuerID(),issuer_name,staffID,receiver_full_name,receiver_dept,new_date,new_time);
                    dbHandler.updateOngoingIssueTable(new_id, dbHandler.getIssuerID(),issuer_name,staffID,receiver_full_name,receiver_dept,new_date,new_time,bookNumber, jobNumber);

                } catch (Exception e) {
                    e.printStackTrace();
                }


                SyncQuantityTask syncQuantityTask = new SyncQuantityTask(new SyncQuantityTaskListener() {
                    @Override
                    public void onTaskComplete(String response) {



                        if (!blocktransaction){
                            if(response.equals("success")){
                                bslinearProgressIndicator.setVisibility(View.GONE);
                                Intent intent = new Intent(NewIssue.this, ReceiptPage.class);
                                bsbottomSheetDialog.dismiss();
                                startActivity(intent);
                                mIsCardReaderOpen = false;
                                finish();


                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        bsbottomSheetDialog.dismiss();
                                        Toast.makeText(NewIssue.this, "An error occurred while updating database.", Toast.LENGTH_SHORT).show();
                                        mIsCardReaderOpen = false;
                                    }
                                });
                            }

                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    bsbottomSheetDialog.dismiss();
                                    Toast.makeText(NewIssue.this, "No data was synced", Toast.LENGTH_SHORT).show();
                                    mIsCardReaderOpen = false;
                                }
                            });
                        }

                    }
                });
                syncQuantityTask.execute();
                //linearProgressIndicator.setVisibility(View.GONE);




            } else {
                String message = null;

                if(status.equals("failed")){
                    message = "Wrong Access Card";
                    //nfcAdapter.enableForegroundDispatch(NewIssue.this, pendingIntent, null, null);
                    bsbottomSheetDialog.dismiss();
                    mIsCardReaderOpen = false;
                } else {
                    message = "Could not authenticate";
                    //nfcAdapter.enableForegroundDispatch(NewIssue.this, pendingIntent, null, null);
                    bsbottomSheetDialog.dismiss();
                    mIsCardReaderOpen = false;
                }
                Toast.makeText(NewIssue.this, message, Toast.LENGTH_SHORT).show();
                bslinearProgressIndicator.setVisibility(View.GONE);

            }
            staffInt = null;
            dbHandler.close();

        }
    }


    public interface SyncQuantityTaskListener {
        void onTaskComplete(String response);
    }

    @SuppressLint("StaticFieldLeak")
    public class SyncQuantityTask extends AsyncTask<Void, Void, String> {
        private SyncQuantityTaskListener listener;

        public SyncQuantityTask(SyncQuantityTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected String doInBackground(Void... voids) {
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.openDatabase(getApplicationContext());
            Cursor cursor = db.rawQuery("SELECT * FROM ongoingIssueTable",null);
            JSONObject jsonObjectProductQuantity;
            String message = null;
            String finalresponse = null;
            List<IssueHistoryData> issueHistoryDataList = new ArrayList<>();
            Gson gson = new Gson();

            while (cursor.moveToNext()){

                String itemCode = cursor.getString(cursor.getColumnIndexOrThrow("id")); //represents itemCode
                localquantity = cursor.getFloat(cursor.getColumnIndexOrThrow("ProductQuantity"));
                localname = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));

                String sql1 = "{\"type\":\"sync_issue\",\"condition\":\"compare_quantity\",\"id\":\""+itemCode+"\"}";

                Log.d("Quantity Test", String.valueOf(sql1));

                try{

                    RequestBody requestBody =  RequestBody.create(sql1, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .post(requestBody)
                            .build();
                    Response response = okHttpClient.newCall(request).execute();
                    String resString = response.body().string();
                    JSONObject jsonObject1 = new JSONObject(resString);

                    String status1 = jsonObject1.optString("status");
                    resString = jsonObject1.optString("data");

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status1, getApplicationContext(), NewIssue.this);

                    Log.d("Quantity Test", String.valueOf(resString));

                    jsonObjectProductQuantity = new JSONObject(resString);

                    dbquantity = Float.valueOf(jsonObjectProductQuantity.getString("productQuantity"));
                    Log.i("QTY", String.valueOf(dbquantity));

                    if(dbquantity>=localquantity){
                        blocktransaction = false;

                    } else if (localquantity>dbquantity) {
                        blocktransaction = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(NewIssue.this, "Quantity of "+localname+" exceeds quantity in database.", Toast.LENGTH_LONG).show();
                                Toast.makeText(NewIssue.this, "Please update with new quantity of "+localname, Toast.LENGTH_LONG).show();
                            }
                        });
                        break;
                    } else{
                        blocktransaction = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(NewIssue.this, "Quantity of "+localname+" has changed in database.", Toast.LENGTH_LONG).show();
                                Toast.makeText(NewIssue.this, "Please  update with new quantity of "+localname, Toast.LENGTH_LONG).show();
                            }
                        });
                        break;
                    }

                    if(blocktransaction){
                        break;
                    }


                    response.close();

                } catch (Exception e){
                    e.printStackTrace();

                }

            }

            if (!blocktransaction){

                Cursor cursorx = db.rawQuery("SELECT * FROM ongoingIssueTable",null);

                while (cursorx.moveToNext()) {

                    Boolean isFirst = false;
                    Boolean isLast = false;

                    if(cursorx.isFirst()){
                        isFirst = true;
                    } else {
                        isFirst = false;
                    }

                    if(cursorx.isLast()){
                        isLast = true;
                    } else{
                        isLast = false;
                    }


                    String idx = cursorx.getString(cursorx.getColumnIndexOrThrow("id")); //represents itemCode
                    localquantity = cursorx.getFloat(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                    localname = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));

                    //for issue history
                    Integer TransactionID = cursorx.getInt(cursorx.getColumnIndexOrThrow("TransactionID"));

                    String ProductName = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));
                    String ProductDesc = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductDesc"));
                    Float ProductQuantity = cursorx.getFloat(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                    String ProductStore = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductStore"));
                    String ProductLocation = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductLocation"));

                    Integer IssuerID = cursorx.getInt(cursorx.getColumnIndexOrThrow("IssuerID"));
                    String IssuerName = cursorx.getString(cursorx.getColumnIndexOrThrow("IssuerName"));
                    Integer ReceiverID = cursorx.getInt(cursorx.getColumnIndexOrThrow("ReceiverID"));
                    String ReceiverName = cursorx.getString(cursorx.getColumnIndexOrThrow("ReceiverName"));
                    String ReceiverDept = cursorx.getString(cursorx.getColumnIndexOrThrow("ReceiverDepartment"));
                    String TransactionDate = cursorx.getString(cursorx.getColumnIndexOrThrow("TransactionDate"));
                    String TransactionTime = cursorx.getString(cursorx.getColumnIndexOrThrow("TransactionTime"));
                    String BookNumber = cursorx.getString(cursorx.getColumnIndexOrThrow("BookNumber"));
                    String unit = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductUnit"));
                    String jobnumber = cursorx.getString(cursorx.getColumnIndexOrThrow("JobNumber"));




                    String sql2 = "{\"type\":\"sync_issue\",\"condition\":\"compare_quantity\",\"id\":\""+idx+"\"}";

                    try{

                        RequestBody requestBody =  RequestBody.create(sql2, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                        Request request = new Request.Builder()
                                .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                                .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                .post(requestBody)
                                .build();
                        Response response = okHttpClient.newCall(request).execute();
                        String resString = response.body().string();
                        JSONObject jsonObject1 = new JSONObject(resString);
                        Log.d("Retrieval Test", String.valueOf(resString));

                        String status1 = jsonObject1.optString("status");
                        resString = jsonObject1.optString("data");

                        TokenChecker tokenChecker = new TokenChecker();
                        tokenChecker.checkToken(status1, getApplicationContext(), NewIssue.this);
                        Log.d("Quantity Test", String.valueOf(resString));

                        jsonObjectProductQuantity = new JSONObject(resString);
                        dbquantity = (float) jsonObjectProductQuantity.getDouble("productQuantity");
                        Log.i("QTY", String.valueOf(dbquantity));

                        if(dbquantity>=localquantity){
                            blocktransaction = false;


                        } else if (localquantity>dbquantity) {
                            blocktransaction = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(NewIssue.this, "Quantity of "+localname+" exceeds quantity in database.", Toast.LENGTH_LONG).show();
                                    Toast.makeText(NewIssue.this, "Please update with new quantity of "+localname, Toast.LENGTH_LONG).show();
                                }
                            });
                            break;
                        } else{
                            blocktransaction = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(NewIssue.this, "Quantity of "+localname+" has changed in database.", Toast.LENGTH_LONG).show();
                                    Toast.makeText(NewIssue.this, "Please  update with new quantity of "+localname, Toast.LENGTH_LONG).show();
                                }
                            });
                            break;
                        }

                        if(blocktransaction){
                            break;
                        }


                        response.close();

                    } catch (Exception e){
                        e.printStackTrace();

                    }



                    if(!blocktransaction){
                        //update quantity and set lastID for stock issue
                        new_quantity = dbquantity-localquantity;
                        Integer updatedID = new_id;
                        //String sql3 = "{\"type\":\"sync_issue\",\"condition\":\"update_quantity\",\"id\":\""+idx+"\",\"new_quantity\":\""+new_quantity+"\",\"new_lastID\":\""+updatedID+"\",\"old_quantity\":\""+localquantity+"\"}";

                        try {

                            /*

                            RequestBody requestBody =  RequestBody.create(sql3, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                            Request request = new Request.Builder()
                                    .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                                    .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                    .post(requestBody)
                                    .build();
                            Response response = okHttpClient.newCall(request).execute();
                            String resString = response.body().string();
                            jsonObjectProductQuantity = new JSONObject(resString);
                            Log.i("Message", "RECEIVED JSON: "+jsonObjectProductQuantity);

                            message = jsonObjectProductQuantity.optString("status");
                            IssueRate = jsonObjectProductQuantity.optString("rate");
                            IssueValue = jsonObjectProductQuantity.optString("totalIssueValue");
                            FifoStatus = jsonObjectProductQuantity.optString("fifo");
                            Log.i("Message", "Issue Quantity Sync Status: "+message);

                            */
                            //String historysql = "{\"rate\":\""+IssueRate+"\",\"value\":\""+IssueValue+"\",\"fifo\":\""+FifoStatus+"\",\"new_quantity\":\""+new_quantity+"\",\"new_lastID\":\""+updatedID+"\",\"old_quantity\":\""+localquantity+"\",\"type\":\"sync_issue\",\"condition\":\"sync_history\",\"TransactionID\":\""+TransactionID+"\",\"id\":\""+idx+"\",\"ProductName\":\""+ProductName+"\",\"ProductDesc\":\""+ProductDesc+"\",\"ProductQuantity\":\""+ProductQuantity+"\",\"ProductStore\":\""+ProductStore+"\",\"ProductLocation\":\""+ProductLocation+"\",\"IssuerID\":\""+IssuerID+"\",\"IssuerName\":\""+IssuerName+"\",\"ReceiverID\":\""+ReceiverID+"\",\"ReceiverName\":\""+ReceiverName+"\",\"ReceiverDepartment\":\""+ReceiverDept+"\",\"TransactionDate\":\""+TransactionDate+"\",\"TransactionTime\":\""+TransactionTime+"\",\"BookNumber\":\""+BookNumber+"\",\"TransactionType\":\"Stock Issue\",\"Balance\":\""+balance+"\",\"ProductUnit\":\""+unit+"\",\"JobNumber\":\""+jobnumber+"\"}";
                            //String historysql = "{\"isFirst\":\""+isFirst+"\",\"isLast\":\""+isLast+"\",\"new_quantity\":\""+new_quantity+"\",\"new_lastID\":\""+updatedID+"\",\"old_quantity\":\""+localquantity+"\",\"type\":\"sync_issue\",\"condition\":\"sync_history\",\"TransactionID\":\""+TransactionID+"\",\"id\":\""+idx+"\",\"ProductName\":\""+ProductName+"\",\"ProductDesc\":\""+ProductDesc+"\",\"ProductQuantity\":\""+ProductQuantity+"\",\"ProductStore\":\""+ProductStore+"\",\"ProductLocation\":\""+ProductLocation+"\",\"IssuerID\":\""+IssuerID+"\",\"IssuerName\":\""+IssuerName+"\",\"ReceiverID\":\""+ReceiverID+"\",\"ReceiverName\":\""+ReceiverName+"\",\"ReceiverDepartment\":\""+ReceiverDept+"\",\"TransactionDate\":\""+TransactionDate+"\",\"TransactionTime\":\""+TransactionTime+"\",\"BookNumber\":\""+BookNumber+"\",\"TransactionType\":\"Stock Issue\",\"Balance\":\""+balance+"\",\"ProductUnit\":\""+unit+"\",\"JobNumber\":\""+jobnumber+"\"}";
                            //rowData.put(new JSONObject(historysql));

                            IssueHistoryData issueHistoryData = new IssueHistoryData();

                            issueHistoryData.setFirst(isFirst);
                            issueHistoryData.setLast(isLast);
                            issueHistoryData.setNewQuantity(new_quantity);
                            issueHistoryData.setNewLastID(Long.valueOf(updatedID));
                            issueHistoryData.setOldQuantity(localquantity);
                            issueHistoryData.setTransactionID(Long.valueOf(TransactionID));
                            issueHistoryData.setProductName(ProductName);
                            issueHistoryData.setProductDesc(ProductDesc);
                            issueHistoryData.setProductQuantity(ProductQuantity);
                            issueHistoryData.setProductStore(ProductStore);
                            issueHistoryData.setProductLocation(ProductLocation);
                            issueHistoryData.setIssuerID(Long.valueOf(IssuerID));
                            issueHistoryData.setIssuerName(IssuerName);
                            issueHistoryData.setReceiverID(Long.valueOf(ReceiverID));
                            issueHistoryData.setReceiverName(ReceiverName);
                            issueHistoryData.setReceiverDepartment(ReceiverDept);
                            issueHistoryData.setTransactionDate(TransactionDate);
                            issueHistoryData.setTransactionTime(TransactionTime);
                            issueHistoryData.setBookNumber(BookNumber);
                            issueHistoryData.setTransactionType("Stock Issue");
                            issueHistoryData.setBalance(balance);
                            issueHistoryData.setProductUnit(unit);
                            issueHistoryData.setJobNumber(jobnumber);
                            issueHistoryData.setId(idx);
                            issueHistoryData.setType("sync_issue");
                            issueHistoryData.setCondition("sync_history");

                            issueHistoryDataList.add(issueHistoryData);

                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    dbHandler.close();

                }

                cursorx.close();


                String rowData = gson.toJson(issueHistoryDataList);
                String data = "{\"data\":"+rowData+",\"type\":\"sync_issue\",\"condition\":\"sync_history\",\"image\":\""+base64Image+"\"}";

                //Log.d("Data",data);

                try{

                    RequestBody requestBody3 =  RequestBody.create(data, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request3 = new Request.Builder()
                            .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .post(requestBody3)
                            .build();
                    Response responsex = okHttpClient.newCall(request3).execute();
                    String resString3 = responsex.body().string();
                    try{
                        JSONObject jsonObject = new JSONObject(resString3);
                        finalresponse = jsonObject.optString("status");



                        TokenChecker tokenChecker = new TokenChecker();
                        tokenChecker.checkToken(finalresponse, getApplicationContext(), NewIssue.this);

                    } catch (Exception e){
                        e.printStackTrace();
                        finalresponse = "failed";
                        blocktransaction = true;
                        cursorx.close();
                    }

                    responsex.close();

                } catch (Exception e){
                    e.printStackTrace();

                }


            }

            dbHandler.close();
            db.close();
            return finalresponse;
        }

        @Override
        protected void onPostExecute(String response) {
            if (listener != null) {
                listener.onTaskComplete(response);
            }

        }
    }
/*
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            //Toast.makeText(this, "NDEF Discovered", Toast.LENGTH_SHORT).show();
            nfcAdapter.enableForegroundDispatch(NewIssue.this, pendingIntent, null, null);


        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            //Toast.makeText(this, "TECH Discovered", Toast.LENGTH_SHORT).show();
            nfcAdapter.enableForegroundDispatch(NewIssue.this, pendingIntent, null, null);
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            //Toast.makeText(this, "TAG Discovered", Toast.LENGTH_SHORT).show();
            //nfcAdapter.enableForegroundDispatch(NewIssue.this, pendingIntent, null, null);
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (tag != null) {

                try {
                    //try reading MifareClassic

                    MifareClassic mifareClassic = MifareClassic.get(tag);
                    if (mifareClassic != null) {

                        try {
                            mifareClassic.connect();
                            byte[] uid = mifareClassic.getTag().getId();
                            String uidString = bytesToHexString(uid);

                            //textView.setText("Serial: "+uidString);

                            String[] techList = mifareClassic.getTag().getTechList();
                            String techListString = Arrays.toString(techList);

                            //textView2.setText("Technologies: "+techListString);

                            //Log.i("MIFARE", uidString);

                            //cardType = "Mifare Classic";


                            //int uidInt = Integer.parseInt(uidString, 16);
                            //uidTV.setText("ID: "+uidInt);

                            BigInteger uidInt = new BigInteger(uidString, 16);
                            //verifyAccessCard(uidInt);
                            new VerifyStaffTask(new VerifyStaffTaskListener() {
                                @Override
                                public void onTaskComplete(String response) {
                                    Log.i("Response",response);
                                }
                            }, String.valueOf(uidInt)).execute();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    } else{

                        IsoDep isoDep = IsoDep.get(tag);

                        try {

                            if (isoDep != null) {
                                isoDep.connect();
                                byte[] uid = isoDep.getTag().getId();
                                String uidString = bytesToHexString(uid);


                                //textView.setText("Serial: "+uidString);

                                String[] techList = isoDep.getTag().getTechList();
                                String techListString = Arrays.toString(techList);

                                //textView2.setText("Technologies: "+techListString);

                                //Log.i("MIFARE", uidString);

                                //cardType = "IsoDep";

                                //convert bigint uid to int

                                BigInteger uidInt = new BigInteger(uidString, 16);
                                //verifyStaffAccessCard(uidInt);
                                //uidTV.setText("ID: "+uidInt);

                                new VerifyStaffTask(new VerifyStaffTaskListener() {
                                    @Override
                                    public void onTaskComplete(String response) {
                                        Log.i("Response",response);
                                    }
                                }, String.valueOf(uidInt)).execute();


                            } else {
                                nfcAdapter.enableForegroundDispatch(NewIssue.this, pendingIntent, null, null);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();

                        } finally {
                            try {
                                if (isoDep != null) {
                                    isoDep.close();
                                }

                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        }

                    }




                } catch (Exception e) {
                    e.printStackTrace();
                }


            }




        }
    }

 */

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
                //Toast.makeText(this, "sw1="+sw1, Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "sw1="+sw2, Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "data="+ Arrays.toString(data), Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "HexString="+bytesToHexString(data), Toast.LENGTH_SHORT).show();
                BigInteger uidInt = new BigInteger(bytesToHexString(data), 16);
                //Toast.makeText(this, "Integer Value: "+String.valueOf(uidInt), Toast.LENGTH_LONG).show();
                if(mIsCardReaderOpen){
                    new VerifyStaffTask(new VerifyStaffTaskListener() {
                        @Override
                        public void onTaskComplete(String response) {
                            Log.i("Response",response);
                        }
                    }, String.valueOf(uidInt)).execute();
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
                                new NewIssue.CardReading().execute();

                            }
                        }
                    };

                    App.BioManager.registerCardStatusListener(onCardStatusListener);
                    Functions.cancel_loader();
                } else {
                    Functions.cancel_loader();
                    Functions.Show_Alert2(NewIssue.this, "Card Opening Error", "Error Opening Card Reader" + resultCode.toString());
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
        boolean isCardReaderOpened = Functions.getSharedPreference(NewIssue.this).getBoolean(Variables.is_card_reader_open, false);

        if (isCardReaderOpened == false) {
            openCardReaderMain();
        } else {
            boolean cardConnection = App.BioManager.cardConnectSync(1000);
            if (cardConnection) {
                onCardStatusListener = new Biometrics.OnCardStatusListener() {
                    @Override
                    public void onCardStatusChange(String s, int prevState, int currentState) {
                        if (Variables.CARD_ABSENT == currentState) {
                            Functions.Show_Alert2(NewIssue.this, "CARD ABSENT", "Place Card on top on the device");

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
}