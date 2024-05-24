package com.icpsltd.stores.activities;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.icpsltd.stores.activities.NewIssue.FETCH_DELAY_TIME;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;
import com.icpsltd.stores.util.Variables;
import com.icpsltd.stores.utils.CustomEditText;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.R;
import com.icpsltd.stores.adapterclasses.RetrievedStaff;
import com.icpsltd.stores.adapterclasses.RetrievedStock;
import com.icpsltd.stores.utils.ItemLocationParser;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.utils.Security;
import com.icpsltd.stores.utils.StockTable;
import com.icpsltd.stores.utils.TokenChecker;

import org.json.JSONArray;
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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

public class StockAdjustment extends AppCompatActivity {

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

    private String balance;

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

    private boolean isAdd;
    private boolean isCheckedx = false;

    private String confirmerName;

    private boolean confirmerConfirmed = false;

    private MaterialButtonToggleGroup materialButtonToggleGroup;

    private Integer accessID = 0;

    private OkHttpClient okHttpClient;

    PendingIntent pendingIntent;

    NfcAdapter nfcAdapter;

    LinearProgressIndicator bslinearProgressIndicator;

    BottomSheetDialog bsbottomSheetDialog;

    private boolean mIsCardReaderOpen = false;
    private static Biometrics.OnCardStatusListener onCardStatusListener;

    int height;

    private MyPrefs myPrefs;

    private Handler sHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_adjustment);
        myPrefs = new MyPrefs();
        sHandler = new Handler();


        /*

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);
        nfcAdapter =  NfcAdapter.getDefaultAdapter(this);

         */

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;

        TextView asname = findViewById(R.id.firstLastName);
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        String apiHost = dbHandler.getApiHost();
        dbHandler.clearOngoingIssueTable();

        asname.setText("as "+dbHandler.getFirstName()+" "+dbHandler.getLastName());
        ongoingListview = findViewById(R.id.ongoing_item_list);
        ongoingListview.setVisibility(View.VISIBLE);
        ongoingIssue();


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
                        runOnUiThread(()->{ Toast.makeText(StockAdjustment.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

        materialButtonToggleGroup = findViewById(R.id.adjustmentType);
        materialButtonToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {

                if(isChecked){
                    switch (checkedId){
                        case R.id.add:
                            isAdd = true;
                            isCheckedx = true;
                            break;

                        case R.id.deduct:
                            isAdd = false;
                            isCheckedx = true;
                            break;
                    }
                }
            }
        });



        int layout = R.layout.add_adjustment_item;
        View bottomSheet = LayoutInflater.from(this).inflate(layout, null);


        //BottomSheetDialog bottomSheet = findViewById(R.id.add); // Replace with the ID of your bottom sheet view

        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getCurrentFocus() != null) {
                    // Check if the touch event is inside the bottom sheet's content view
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

        int layout = R.layout.add_adjustment_item;
        View bottomSheet = LayoutInflater.from(this).inflate(layout, null);
        int bottomSheetHeight = (int) (bottomSheet.getHeight() * 0.6); // Assume the bottom sheet occupies the bottom 60% of the screen
        int[] location = new int[2];
        bottomSheet.getLocationOnScreen(location);
        int bottomSheetTop = location[1];
        int bottomSheetBottom = bottomSheetTop + bottomSheetHeight;

        float touchX = event.getRawX();
        float touchY = event.getRawY();

        return touchX >= bottomSheet.getLeft() && touchX <= bottomSheet.getRight()
                && touchY >= bottomSheetTop && touchY <= bottomSheetBottom;

    }



    public void ongoingIssue(){

        fetchedOngoingIssueTable = new ArrayList<>();
        //load existing ongoing issue
        DBHandler dbHandler = new DBHandler(getApplicationContext());

        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit, AdditionID FROM ongoingIssueTable",null);
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
            String additionID = cursor.getString(cursor.getColumnIndexOrThrow("AdditionID"));
            Log.e("Retrieved",name);

            RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,location,store,unit,additionID,null);
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
                store_location.setText(retrievedStock.getStore());
                product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit()+" | (ID: "+retrievedStock.getAdditionID()+")");
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

                        //TextView item_title = new TextView(StockAdjustment.this);
                        //item_title.setText(retrievedStock.getName());
                        //item_title.setTypeface(ResourcesCompat.getFont(StockAdjustment.this, R.font.sfprodisplaybold));

                        View editLayout = getLayoutInflater().inflate(R.layout.edit_item, null);
                        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(StockAdjustment.this);
                        materialAlertDialogBuilder.setTitle("Edit "+retrievedStock.getName())
                                .setView(editLayout)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        CustomEditText customEditText = editLayout.findViewById(R.id.issue_quantity_update);
                                        if(Float.valueOf(customEditText.getText().toString()) > 0){
                                            //customEditText.setText(String.valueOf(inc+1));
                                            dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),Float.valueOf(customEditText.getText().toString()),retrievedStock.getStore(),retrievedStock.getLocation(), retrievedStock.getUnit(),retrievedStock.getAdditionID());
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

                                        dbHandler1.close();

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
                                 //Toast.makeText(StockAdjustment.this,"NOT FOUND",Toast.LENGTH_SHORT).show();
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

                dbHandler1.close();
                return view;
            }
        };

        assert ongoingListview != null;
        ongoingListview.setAdapter(ongoingIssueAdapter);
        ongoingIssueAdapter.notifyDataSetChanged();

        dbHandler.close();
        db.close();

    }


    public void create_issue(View view) {

        EditText adjustmentReason = findViewById(R.id.adjustmentReason);
        String reason = adjustmentReason.getText().toString();

        if(reason.length() < 5 ){
            Toast.makeText(this, "Enter a valid reason", Toast.LENGTH_SHORT).show();
            return;
        }


        TextView rmtv = findViewById(R.id.remove);
        EditText bknm = findViewById(R.id.book_number);
        DBHandler dbHandler = new DBHandler(getApplicationContext());

                if(dbHandler.checkOngoingIssueTableEmptiness().equals("notempty")){
                    mIsCardReaderOpen = true;

                    if(mIsCardReaderOpen){

                        //nfcAdapter.enableForegroundDispatch(StockAdjustment.this, pendingIntent, null, null);
                        int layout = R.layout.confirm_transaction_sheet;
                        View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);
                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                        bottomSheetDialog.setContentView(bottomSheetView);
                        TextView textView = bottomSheetDialog.findViewById(R.id.receiver_confirm);
                        EditText passpin = bottomSheetDialog.findViewById(R.id.pin_input);
                        LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.verify_progress);
                        bslinearProgressIndicator = linearProgressIndicator;
                        bsbottomSheetDialog = bottomSheetDialog;

                        /*


                        MaterialButton confirm = bottomSheetDialog.findViewById(R.id.confirm_issue);
                        confirm.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                linearProgressIndicator.setVisibility(View.VISIBLE);
                                accessID = Integer.valueOf(passpin.getText().toString());
                                Integer dbid = Integer.valueOf(dbHandler.getAccessID());

                                if (!passpin.getText().toString().equals("")){
                                    if(!Objects.equals(accessID, dbid)){
                                        Security security = new Security();
                                        String pinHash = "";

                                        try {
                                            pinHash = security.createMD5Hash(passpin.getText().toString());
                                        } catch (NoSuchAlgorithmException e) {
                                            throw new RuntimeException(e);

                                        }

                                        VerifyStaffTask verifyStaffTask = new VerifyStaffTask(new VerifyStaffTaskListener() {
                                            @Override
                                            public void onTaskComplete(String jsonResponse) {



                                                if (confirmerConfirmed){
                                                    Toast.makeText(StockAdjustment.this, "Confirmed as "+confirmerName, Toast.LENGTH_SHORT).show();

                                                    textView.setText("Confirmed as "+confirmerName);
                                                    new_id = latestIssueID+1;
                                                    String issuer_name = dbHandler.getFirstName()+" "+dbHandler.getLastName();
                                                    //Calendar calendar = Calendar.getInstance();

                                                    //SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
                                                    //SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
                                                    //String new_date = date.format(calendar.getTime());
                                                    //String new_time = time.format(calendar.getTime());
                                                    //TextInputEditText bk = findViewById(R.id.book_number);
                                                    //String bookNumber = bk.getText().toString();


                                                    String new_date;
                                                    String new_time;

                                                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                                                    Future<String> futureResult = executorService.submit(() -> {
                                                        //OkHttpClient okHttpClient = new OkHttpClient();
                                                        try{
                                                            Request request = new Request.Builder()
                                                                    .url("https://"+dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/tst/getDateTime")
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


                                                        Log.e("RECEIVER DEPARTMENT",": "+receiver_dept);

                                                        dbHandler.addOngoingIssueMeta(new_id, dbHandler.getIssuerID(),issuer_name,staffInt,confirmerName,null,new_date,new_time);
                                                        dbHandler.updateOngoingIssueTable(new_id, dbHandler.getIssuerID(),issuer_name,staffInt,confirmerName,null,new_date,new_time,null);


                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }


                                                    SyncQuantityTask syncQuantityTask = new SyncQuantityTask(new SyncQuantityTaskListener() {
                                                        @Override
                                                        public void onTaskComplete(String response) {

                                                            if (!blocktransaction){

                                                                linearProgressIndicator.setVisibility(View.VISIBLE);

                                                                Intent intent = new Intent(StockAdjustment.this, ReceiptPage.class);
                                                                intent.putExtra("staffName","Staff Name:");
                                                                intent.putExtra("returnerDept","");
                                                                intent.putExtra("type","Adjustment");
                                                                intent.putExtra("returnerName","Confirmer:");
                                                                if(isAdd){
                                                                    intent.putExtra("itemsReturned","Items Added");
                                                                    intent.putExtra("title","Adjust Stock (Addition)");
                                                                } else {
                                                                    intent.putExtra("itemsReturned","Items Deducted");
                                                                    intent.putExtra("title","Adjust Stock (Deduction)");
                                                                }

                                                                startActivity(intent);

                                                            /*
                                                            SyncHistoryTask syncHistoryTask = new SyncHistoryTask(new SyncHistoryTaskListener() {
                                                                @Override
                                                                public void onTaskComplete(Connection connection) {


                                                                }
                                                            });
                                                            syncHistoryTask.execute();

                                                            } else {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        bottomSheetDialog.dismiss();
                                                                        Toast.makeText(StockAdjustment.this, "No data was synced", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                            }

                                                        }
                                                    });
                                                    syncQuantityTask.execute();

                                                } else {
                                                    //Toast.makeText(StockAdjustment.this, "", Toast.LENGTH_SHORT).show();
                                                    linearProgressIndicator.setVisibility(View.GONE);

                                                }

                                            }
                                        },pinHash);
                                        verifyStaffTask.execute();
                                    } else {
                                        linearProgressIndicator.setVisibility(View.GONE);
                                        Toast.makeText(StockAdjustment.this, "You cannot verify this transaction yourself", Toast.LENGTH_SHORT).show();
                                        Toast.makeText(StockAdjustment.this, "Ask a colleague to verify", Toast.LENGTH_SHORT).show();

                                    }



                                } else {
                                    Toast.makeText(StockAdjustment.this, "Pin field cannot be empty", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


                         */

                        new OpenCardReaderAsync().execute();

                        textView.setText("Ask a colleague to confirm with Access ID");
                        bottomSheetDialog.show();

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
                    }


                } else {
                    Toast.makeText(this, "Add at least 1 item", Toast.LENGTH_SHORT).show();
                }

                dbHandler.close();

    }


    public void showBottomSheet(){

        if (isCheckedx){

            int layout = R.layout.add_adjustment_item;
            View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            bottomSheetDialog.setContentView(bottomSheetView);

            BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
            bottomSheetBehavior.setPeekHeight((int) (height*0.9));

            LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.fetch_progress);
            linearProgressIndicator.setVisibility(View.VISIBLE);
            searchString = bottomSheetDialog.findViewById(R.id.stockSearch);
            ListView listView = (ListView) bottomSheetDialog.findViewById(R.id.add_items_listview);
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            searchString.setInputType(InputType.TYPE_CLASS_TEXT);


            GetStockTable fetchStockTable = new GetStockTable(new GetStockTableListener() {
                @Override
                public void onTaskComplete(JSONArray jsonArray) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.fetch_progress);
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
                                    store_location.setText(retrievedStock.getStore());
                                    product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                                    product_location.setText(retrievedStock.getLocation());


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


                                    //Set adjustment quantity views visible

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

                                    MaterialButton itemCodeDisplay = bottomSheetDialog.findViewById(R.id.item_code_display);
                                    itemCodeDisplay.setText(retrievedStock.getID());

                                    TextInputLayout textInputLayout2 = bottomSheetDialog.findViewById(R.id.quantity_entry_box);
                                    ConstraintLayout constraintLayout5 = bottomSheetDialog.findViewById(R.id.id_qty_entry_layout);
                                    constraintLayout5.setVisibility(View.VISIBLE);
                                    if(isAdd){
                                        textInputLayout2.setHint("Addition quantity");
                                    } else {
                                        textInputLayout2.setHint("Deduction quantity");
                                    }
                                    textInputLayout2.setVisibility(View.VISIBLE);

                                    MaterialButton materialButton3 = bottomSheetDialog.findViewById(R.id.add_item_button);
                                    materialButton3.setVisibility(View.VISIBLE);

                                    //this populates textviews with selected item
                                    String name = retrievedStock.getName();
                                    String location = retrievedStock.getLocation();
                                    String store = retrievedStock.getStore();
                                    String retrievedquantity = String.valueOf(retrievedStock.getQuantity());
                                    String retrievedunit = retrievedStock.getUnit();

                                    TextView name_tv = bottomSheetView.findViewById(R.id.item_name);
                                    name_tv.setText(name);

                                    name_tv.setSelected(true);
                                    if (name.length() > 15){
                                        name_tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                    }

                                    TextView location_tv = bottomSheetView.findViewById(R.id.item_location);
                                    location_tv.setText(location);

                                    TextView store_tv = bottomSheetView.findViewById(R.id.item_store);
                                    store_tv.setText(store);

                                    TextView qty_tv = bottomSheetView.findViewById(R.id.item_quantity_remaining);
                                    qty_tv.setText(retrievedquantity+" "+retrievedunit);

                                    CustomEditText customEditText = bottomSheetDialog.findViewById(R.id.issue_quantity);

                                    TextInputEditText additionIDedit = bottomSheetDialog.findViewById(R.id.transaction_id);



                                    materialButton3.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            String additionID = additionIDedit.getText().toString();

                                            String squantity = customEditText.getText().toString();
                                            if(additionID.equals("")){
                                                Toast.makeText(StockAdjustment.this, "Enter an addition Transaction ID", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            if(squantity.equals("")){
                                                Toast.makeText(StockAdjustment.this, "Enter a quantity", Toast.LENGTH_SHORT).show();
                                                return;
                                            }


                                            try {
                                                Float quantity = Float.valueOf(squantity);
                                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),quantity,retrievedStock.getStore(),retrievedStock.getLocation(),retrievedStock.getUnit(), additionID);
                                                Toast.makeText(getApplicationContext(),retrievedStock.getName()+" added successfully",Toast.LENGTH_SHORT).show();
                                                //update listview
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ongoingIssue();
                                                    }
                                                });
                                                bottomSheetDialog.dismiss();


                                            } catch (Exception e){
                                                Toast.makeText(getApplicationContext(),"Enter a valid number",Toast.LENGTH_LONG).show();
                                                e.printStackTrace();
                                            }

                                        }
                                    });
                                }
                            });

                            dbHandler.close();



                        }
                    });
                }
            },"false");
            fetchStockTable.execute();



            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedStockTable);
                            adapter.clear();
                            adapter.notifyDataSetChanged();


                        }
                    });




                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    sHandler.removeCallbacksAndMessages(null);
                    sHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    String searchedString = searchString.getText().toString();
                                    String firstPer = "%"+searchedString;
                                    String queryString = firstPer+"%";


                                    if (searchString.getText().toString().length() < 1){
                                        //fetches default 5 most issued items if search box is empty, change values in fetchStockStable and here to update results number
                                        GetStockTable fetchDefaultStockTable = new GetStockTable(new GetStockTableListener() {
                                            @Override
                                            public void onTaskComplete(JSONArray jsonArray) {
                                                //LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.fetch_progress);
                                                //linearProgressIndicator.setVisibility(View.GONE);
                                                Log.e("TASK ASYNC","TASK IS DONE");
                                                Log.i("SearchTest", "Search string empty, showing default items");

                                                DBHandler dbHandler = new DBHandler(getApplicationContext());
                                                SQLiteDatabase db = dbHandler.getReadableDatabase();

                                                Cursor cursorx = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit, ImageAvailable FROM stockTable LIMIT 10",null);
                                                while (cursorx.moveToNext()){
                                                    String id = cursorx.getString(cursorx.getColumnIndexOrThrow("id"));
                                                    String name = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));
                                                    Float quantity = cursorx.getFloat(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                                                    String store = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductStore"));
                                                    String location = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductLocation"));
                                                    String description = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductDesc"));
                                                    String unit = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductUnit"));
                                                    String imageAvailable = cursorx.getString(cursorx.getColumnIndexOrThrow("ImageAvailable"));

                                                    RetrievedStock retrievedStock = new RetrievedStock(id, name,description,quantity,location,store,unit,null,imageAvailable);
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
                                                        store_location.setText(retrievedStock.getStore());
                                                        product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                                                        product_location.setText(retrievedStock.getLocation());

                                                        return view;
                                                    }
                                                };

                                                assert listView != null;
                                                listView.setAdapter(adapter);
                                                adapter.notifyDataSetChanged();
                                                db.close();
                                                dbHandler.close();

                                            }
                                        }, "false");
                                        fetchDefaultStockTable.execute();



                                    } else {
                                        DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                        dbHandler1.clearStockTable();

                                        //if text box is not empty, this fetches 20 items matching the query
                                        GetStockTable fetchUpdatedStockTable = new GetStockTable(new GetStockTableListener() {
                                            @Override
                                            public void onTaskComplete(JSONArray jsonArray) {

                                                adapter.clear();
                                                adapter.notifyDataSetChanged();
                                                Log.i("SearchTest","Search string updated with "+searchedString);
                                                SQLiteDatabase db = dbHandler1.getReadableDatabase();
                                                Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit, ImageAvailable FROM stockTable WHERE productName LIKE '"+queryString+"' LIMIT 20",null);
                                                while (cursor.moveToNext()){
                                                    String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                                                    String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                                                    Log.e("Retrievedx",name);
                                                    Float quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("ProductQuantity"));
                                                    String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                                                    String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                                                    String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                                                    String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
                                                    String imageAvailable = cursor.getString(cursor.getColumnIndexOrThrow("ImageAvailable"));

                                                    RetrievedStock retrievedStock = new RetrievedStock(id, name,description,quantity,location,store,unit,null,imageAvailable);
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
                                                        store_location.setText(retrievedStock.getStore());
                                                        product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                                                        product_location.setText(retrievedStock.getLocation());

                                                        return view;
                                                    }
                                                };

                                                assert listView != null;
                                                listView.setAdapter(adapter);

                                                adapter.notifyDataSetChanged();

                                            }
                                        },searchedString);
                                        fetchUpdatedStockTable.execute();

                                        dbHandler1.close();



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

        } else{
            Toast.makeText(this, "Select an adjustment type", Toast.LENGTH_SHORT).show();
        }



    }


    public void add_item(View view) {
        DBHandler dbHandler1 = new DBHandler(getApplicationContext());
        dbHandler1.clearStockTable();
        showBottomSheet();
        dbHandler1.close();

    }

    public interface VerifyStaffTaskListener {
        void onTaskComplete(String jsonResponse);
    }

    @SuppressLint("StaticFieldLeak")
    public class VerifyStaffTask extends AsyncTask<Void, Void, String> {
        private StockAdjustment.VerifyStaffTaskListener listener;
        private String pinHash;

        public VerifyStaffTask(StockAdjustment.VerifyStaffTaskListener listener, String pinHash) {
            this.listener = listener;
            this.pinHash = pinHash;
        }

        @Override
        protected String doInBackground(Void... voids) {

            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            String resString = "";

            Integer issuerID = dbHandler1.getIssuerID();
            if (issuerID != null){
                try {
                    String sql = "{\"type\":\"staff_verify\",\"accessID\":\""+pinHash+"\",\"activity_name\":\"stockIssue\",\"verify_type\":\"other_staff\",\"access_verify\":\"true\",\"pin_verify\":\"false\",\"staffID\":\""+issuerID+"\"}";

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

                        JSONObject jsonObject3 = new JSONObject(resString);

                        String status1 = jsonObject3.optString("status");
                        resString = jsonObject3.optString("data");

                        TokenChecker tokenChecker = new TokenChecker();
                        tokenChecker.checkToken(status1, getApplicationContext(), StockAdjustment.this);
                        JSONObject jsonObject = null;

                        try{
                            jsonObject = new JSONObject(resString);
                        } catch (Exception e){
                            e.printStackTrace();
                            confirmerConfirmed = false;
                            blocktransaction = true;
                        }


                        if(jsonObject.optString("status").equals("success")){
                            staffInt = jsonObject.optInt("userID");
                            confirmerName = jsonObject.optString("firstName")+" "+jsonObject.optString("lastName");
                            latestIssueID = jsonObject.optInt("latestID");
                            confirmerConfirmed = true;

                        } else if (jsonObject.optString("status").equals("failed")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    bsbottomSheetDialog.dismiss();
                                    Toast.makeText(StockAdjustment.this, "User not found", Toast.LENGTH_SHORT).show();
                                    Toast.makeText(StockAdjustment.this, "No data was synced", Toast.LENGTH_SHORT).show();
                                }
                            });

                            confirmerConfirmed = false;
                            blocktransaction = true;

                        }else if (jsonObject.optString("status").equals("failedx")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    bsbottomSheetDialog.dismiss();
                                    Toast.makeText(StockAdjustment.this, "You cannot verify yourself", Toast.LENGTH_SHORT).show();
                                    Toast.makeText(StockAdjustment.this, "Ask a collegue to confirm", Toast.LENGTH_SHORT).show();
                                }
                            });

                            confirmerConfirmed = false;
                            blocktransaction = true;

                        }
                        else{

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(StockAdjustment.this, "An error occured", Toast.LENGTH_SHORT).show();
                                    Toast.makeText(StockAdjustment.this, "No data was synced", Toast.LENGTH_SHORT).show();
                                }
                            });

                            confirmerConfirmed = false;
                            blocktransaction = true;

                        }

                        response.close();

                        //HANDLE WHEN NOT ID FOUND

                    } catch (Exception e){
                        e.printStackTrace();

                    }

                } catch (Exception e) {
                    Log.e("STOCK ADJUST FAILED", "Error adjusting stock", e);

                }

            } else {
                Toast.makeText(StockAdjustment.this, "Could not get current user ID", Toast.LENGTH_SHORT).show();
            }

            dbHandler1.close();



            return resString;

        }

        @Override
        protected void onPostExecute(String jsonResponse) {
            if (listener != null) {
                listener.onTaskComplete(jsonResponse);
                DBHandler dbHandler = new DBHandler(getApplicationContext());

                if (confirmerConfirmed){
                    Toast.makeText(StockAdjustment.this, "Confirmed as "+confirmerName, Toast.LENGTH_SHORT).show();
                    TextView textView = bsbottomSheetDialog.findViewById(R.id.receiver_confirm);

                    textView.setText("Confirmed as "+confirmerName);
                    new_id = latestIssueID+1;
                    String issuer_name = dbHandler.getFirstName()+" "+dbHandler.getLastName();
                    //Calendar calendar = Calendar.getInstance();

                    //SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
                    //SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
                    //String new_date = date.format(calendar.getTime());
                    //String new_time = time.format(calendar.getTime());
                    //TextInputEditText bk = findViewById(R.id.book_number);
                    //String bookNumber = bk.getText().toString();


                    String new_date;
                    String new_time;

                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    Future<String> futureResult = executorService.submit(() -> {
                        //OkHttpClient okHttpClient = new OkHttpClient();
                        try{
                            Request request = new Request.Builder()
                                    .url("https://"+dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/tst/getDateTime")
                                    .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                    //.post(requestBody)
                                    .build();
                            Response response = okHttpClient.newCall(request).execute();
                            String resString = response.body().string();

                            JSONObject jsonObject3 = new JSONObject(resString);

                            String status1 = jsonObject3.optString("status");
                            resString = jsonObject3.optString("data");

                            TokenChecker tokenChecker = new TokenChecker();
                            tokenChecker.checkToken(status1, getApplicationContext(), StockAdjustment.this);

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


                        Log.e("RECEIVER DEPARTMENT",": "+receiver_dept);

                        dbHandler.addOngoingIssueMeta(new_id, dbHandler.getIssuerID(),issuer_name,staffInt,confirmerName,null,new_date,new_time);
                        dbHandler.updateOngoingIssueTable(new_id, dbHandler.getIssuerID(),issuer_name,staffInt,confirmerName,null,new_date,new_time,null,null);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    SyncQuantityTask syncQuantityTask = new SyncQuantityTask(new SyncQuantityTaskListener() {
                        @Override
                        public void onTaskComplete(String response) {

                            if (!blocktransaction){

                                bslinearProgressIndicator.setVisibility(View.VISIBLE);

                                Intent intent = new Intent(StockAdjustment.this, ReceiptPage.class);
                                intent.putExtra("staffName","Staff Name:");
                                intent.putExtra("returnerDept","");
                                intent.putExtra("type","Adjustment");
                                intent.putExtra("returnerName","Confirmed By:");
                                if(isAdd){
                                    intent.putExtra("itemsReturned","Items Added");
                                    intent.putExtra("title","Adjust Stock (Addition)");
                                } else {
                                    intent.putExtra("itemsReturned","Items Deducted");
                                    intent.putExtra("title","Adjust Stock (Deduction)");
                                }

                                bsbottomSheetDialog.dismiss();
                                mIsCardReaderOpen = false;
                                startActivity(intent);
                                finish();

                                                            /*
                                                            SyncHistoryTask syncHistoryTask = new SyncHistoryTask(new SyncHistoryTaskListener() {
                                                                @Override
                                                                public void onTaskComplete(Connection connection) {


                                                                }
                                                            });
                                                            syncHistoryTask.execute();
                                                            */
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        bsbottomSheetDialog.dismiss();
                                        Toast.makeText(StockAdjustment.this, "No data was synced", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        }
                    });
                    syncQuantityTask.execute();

                } else {
                    //Toast.makeText(StockAdjustment.this, "", Toast.LENGTH_SHORT).show();
                    bslinearProgressIndicator.setVisibility(View.GONE);

                }

                dbHandler.close();


            }

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
            String message = null;
            String responsex = null;
            JSONArray rowData = new JSONArray();
            //Cursor cursor = db.rawQuery("SELECT id, ProductQuantity, ProductName FROM ongoingIssueTable",null);

            try {

                Cursor cursorx = db.rawQuery("SELECT * FROM ongoingIssueTable",null);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(StockAdjustment.this, "Syncing "+String.valueOf(cursorx.getCount())+" items", Toast.LENGTH_SHORT).show();

                    }
                });

                while (cursorx.moveToNext()){

                    String idx = cursorx.getString(cursorx.getColumnIndexOrThrow("id"));
                    localquantity = cursorx.getFloat(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                    localname = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));

                    //values for history syncing
                    Integer TransactionID = cursorx.getInt(cursorx.getColumnIndexOrThrow("TransactionID"));
                    Float ProductQuantity = cursorx.getFloat(cursorx.getColumnIndexOrThrow("ProductQuantity"));


                    String ProductName = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));
                    String ProductDesc = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductDesc"));
                    String ProductStore = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductStore"));
                    String ProductLocation = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductLocation"));

                    Integer IssuerID = cursorx.getInt(cursorx.getColumnIndexOrThrow("IssuerID"));
                    String IssuerName = cursorx.getString(cursorx.getColumnIndexOrThrow("IssuerName"));
                    String ReceiverName = cursorx.getString(cursorx.getColumnIndexOrThrow("ReceiverName"));
                    String TransactionDate = cursorx.getString(cursorx.getColumnIndexOrThrow("TransactionDate"));
                    String TransactionTime = cursorx.getString(cursorx.getColumnIndexOrThrow("TransactionTime"));
                    String unit = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductUnit"));
                    String additionID = cursorx.getString(cursorx.getColumnIndexOrThrow("AdditionID"));


                    String getDBQuantitySQl = "{\"type\":\"sync_adjustment\",\"condition\":\"compare_quantity\",\"id\":\""+idx+"\"}";


                    try{

                        //migrate to one api call

                        RequestBody requestBody =  RequestBody.create(getDBQuantitySQl, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                        Request request = new Request.Builder()
                                .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                                .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                .post(requestBody)
                                .build();
                        Response response = okHttpClient.newCall(request).execute();
                        String resString = response.body().string();

                        JSONObject jsonObject3 = new JSONObject(resString);

                        String status1 = jsonObject3.optString("status");
                        resString = jsonObject3.optString("data");

                        TokenChecker tokenChecker = new TokenChecker();
                        tokenChecker.checkToken(status1, getApplicationContext(), StockAdjustment.this);

                        JSONObject jsonObjectProductQuantity = new JSONObject(resString);

                        dbquantity = (float)jsonObjectProductQuantity.getDouble("productQuantity");
                        Log.i("QTY", "DB QUANTITY = "+String.valueOf(dbquantity));
                        Log.i("QTY", "LOCAL QUANTITY = "+String.valueOf(localquantity));
                        response.close();
                        String AdditionType = null;

                        if(isAdd){
                            new_quantity = dbquantity+localquantity;
                            AdditionType = "Stock Addition";
                        } else {
                            new_quantity = dbquantity-localquantity;
                            AdditionType = "Stock Deduction";
                        }

                        Log.i("QTY", "NEW QUANTITY = "+String.valueOf(new_quantity));
                        Integer updatedID = new_id;

                        //String sql3 = "{\"type\":\"sync_adjustment\",\"condition\":\"update_quantity\",\"id\":\""+idx+"\",\"new_quantity\":\""+new_quantity+"\",\"new_lastID\":\""+updatedID+"\"}";
                        try{

                            /*

                            RequestBody requestBody1 =  RequestBody.create(sql3, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                            Request request1 = new Request.Builder()
                                    .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                                    .post(requestBody1)
                                    .build();
                            Response response1= okHttpClient.newCall(request1).execute();
                            String resString1 = response1.body().string();
                            jsonObjectProductQuantity = new JSONObject(resString1);
                            message = jsonObjectProductQuantity.optString("status");

                            Log.i("Message", message);

                             */

                            String isLast = "false";
                            String isFirst = "false";

                            if (cursorx.isLast()){
                                isLast = "true";
                            } else {
                                isLast = "false";
                            }

                            if (cursorx.isFirst()){
                                isFirst = "true";
                            } else {
                                isFirst = "false";
                            }

                            EditText adjustmentReason = findViewById(R.id.adjustmentReason);
                            String reason = adjustmentReason.getText().toString();
                            String historysql = "{\"isFirst\":\""+isFirst+"\",\"isLast\":\""+isLast+"\",\"new_quantity\":\""+new_quantity+"\",\"new_lastID\":\""+updatedID+"\",\"additionID\":\""+additionID+"\",\"reason\":\""+reason+"\",\"type\":\"sync_adjustment\",\"condition\":\"sync_history\",\"TransactionType\":\""+AdditionType+"\",\"TransactionID\":\""+TransactionID+"\",\"id\":\""+idx+"\",\"ProductName\":\""+ProductName+"\",\"ProductDesc\":\""+ProductDesc+"\",\"Quantity\":\""+ProductQuantity+"\",\"ProductStore\":\""+ProductStore+"\",\"ProductLocation\":\""+ProductLocation+"\",\"StaffID\":\""+IssuerID+"\",\"StaffName\":\""+IssuerName+"\",\"ConfirmerID\":\""+staffInt+"\",\"ConfirmerName\":\""+ReceiverName+"\",\"TransactionDate\":\""+TransactionDate+"\",\"TransactionTime\":\""+TransactionTime+"\",\"Balance\":\""+new_quantity+"\",\"ProductUnit\":\""+unit+"\"}";
                            Log.d("Adjustment SQL", historysql);

                            rowData.put(new JSONObject(historysql));

                            /*


                            */


                        } catch (Exception e){
                            e.printStackTrace();
                        }



                    } catch (Exception e){
                        e.printStackTrace();

                    }
                }

                //send sql string to api and wait for results.
                String data = "{\"type\":\"sync_adjustment\",\"condition\":\"sync_history\",\"data\":"+rowData.toString()+"}";
                Log.d("ROW DATA", rowData.toString());
                try{

                    RequestBody requestBodyx =  RequestBody.create(data, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request requestx = new Request.Builder()
                            .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .post(requestBodyx)
                            .build();
                    Response responsey = okHttpClient.newCall(requestx).execute();
                    String resStringx = responsey.body().string();
                    Log.e("RESPONSE", resStringx);

                    JSONObject jsonObject3 = new JSONObject(resStringx);

                    String status2 = jsonObject3.optString("status");
                    resStringx = jsonObject3.optString("data");

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status2, getApplicationContext(), StockAdjustment.this);

                    TextView adjustmentStatusText = findViewById(R.id.adjustmentStatusText);

                    try{
                        JSONObject jsonObject = new JSONObject(resStringx);
                        responsex = jsonObject.optString("status");
                        if (responsex.equals("success")){
                            blocktransaction = false;
                        } else if (responsex.equals("failedx")) {
                            blocktransaction = true;


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(StockAdjustment.this, String.valueOf(jsonObject.optString("message")), Toast.LENGTH_SHORT).show();
                                    adjustmentStatusText.setText(jsonObject.optString("message")+" No data was synced");
                                    adjustmentStatusText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    cursorx.close();
                                    db.close();


                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            adjustmentStatusText.setText("");

                                        }
                                    }, 8000);

                                }
                            });

                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        responsex = "failed";
                        blocktransaction = true;
                    }

                    responsey.close();

                } catch (Exception e){
                    e.printStackTrace();

                }

                cursorx.close();

            } catch (Exception e) {
                Log.e("SYNC ADJUSTMENT HISTORY TASK", "Error syncing history", e);

            }

            try{
                if (!responsex.equals("success")) {
                    blocktransaction = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView adjustmentStatusText = findViewById(R.id.adjustmentStatusText);
                            Toast.makeText(StockAdjustment.this, "An error occured", Toast.LENGTH_SHORT).show();
                            adjustmentStatusText.setText("An error occured \n " + " No data was synced");
                            adjustmentStatusText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                            db.close();

                            //ongoingListview.getChildAt(0).setBackgroundColor(Color.RED);
                        }
                    });

                }
            } catch (Exception e){
                e.printStackTrace();
            }




            dbHandler.close();
            db.close();
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            if (listener != null) {
                listener.onTaskComplete(response);
            }

        }
    }


    public interface GetStockTableListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    public class GetStockTable extends AsyncTask<Void, Void, JSONArray>{
        private StockAdjustment.GetStockTableListener listener;
        private String stringToQuery;

        public GetStockTable(StockAdjustment.GetStockTableListener listener, String queryString){
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

                    JSONObject jsonObject3 = new JSONObject(resString);

                    String status1 = jsonObject3.optString("status");
                    resString = jsonObject3.optString("data");

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status1, getApplicationContext(), StockAdjustment.this);

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
                DBHandler dbHandler1 = new DBHandler(getApplicationContext());

                try {


                    for (int i = 0; i < jsonArray.length(); i++) {

                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        Log.d("JSON", jsonObject.toString());

                        String itemCode = jsonObject.optString("itemCode");
                        String productName = jsonObject.optString("productName");

                        String productDesc = jsonObject.optString("productDesc");
                        double productQuantity = jsonObject.optDouble("productQuantity");
                        String productStore = jsonObject.optString("productStore");
                        String productLocation = jsonObject.optString("productLocation");
                        String productUnit = jsonObject.optString("productUnit");
                        String imageAvailable = jsonObject.optString("imageAvailable");

                        ItemLocationParser itemLocationParser = new ItemLocationParser();
                        String[] parsedLocation = itemLocationParser.parseLocation(productLocation);
                        productStore = parsedLocation[0];
                        productLocation = parsedLocation[1];

                        dbHandler1.syncStockTable(itemCode,productName,productDesc,(float) productQuantity,productStore,productLocation,productUnit,imageAvailable);

                    }



                } catch (Exception e){
                    e.printStackTrace();
                }
                dbHandler1.close();
                listener.onTaskComplete(jsonArray);

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
            nfcAdapter.enableForegroundDispatch(StockAdjustment.this, pendingIntent, null, null);


        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            //Toast.makeText(this, "TECH Discovered", Toast.LENGTH_SHORT).show();
            nfcAdapter.enableForegroundDispatch(StockAdjustment.this, pendingIntent, null, null);
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

                            Log.i("MIFARE", uidString);

                            //cardType = "Mifare Classic";


                            //int uidInt = Integer.parseInt(uidString, 16);
                            //uidTV.setText("ID: "+uidInt);

                            BigInteger uidInt = new BigInteger(uidString, 16);
                            //verifyAccessCard(uidInt);
                            new StockAdjustment.VerifyStaffTask(new StockAdjustment.VerifyStaffTaskListener() {
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

                                Log.i("MIFARE", uidString);

                                //cardType = "IsoDep";

                                //convert bigint uid to int

                                BigInteger uidInt = new BigInteger(uidString, 16);
                                //verifyStaffAccessCard(uidInt);
                                //uidTV.setText("ID: "+uidInt);

                                new StockAdjustment.VerifyStaffTask(new StockAdjustment.VerifyStaffTaskListener() {
                                    @Override
                                    public void onTaskComplete(String response) {
                                        Log.i("Response",response);
                                    }
                                }, String.valueOf(uidInt)).execute();


                            } else {
                                nfcAdapter.enableForegroundDispatch(StockAdjustment.this, pendingIntent, null, null);
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
                    new StockAdjustment.VerifyStaffTask(new StockAdjustment.VerifyStaffTaskListener() {
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
                                new StockAdjustment.CardReading().execute();

                            }
                        }
                    };

                    App.BioManager.registerCardStatusListener(onCardStatusListener);
                    Functions.cancel_loader();
                } else {
                    Functions.cancel_loader();
                    Functions.Show_Alert2(StockAdjustment.this, "Card Opening Error", "Error Opening Card Reader" + resultCode.toString());
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
        boolean isCardReaderOpened = Functions.getSharedPreference(StockAdjustment.this).getBoolean(Variables.is_card_reader_open, false);

        if (isCardReaderOpened == false) {
            openCardReaderMain();
        } else {
            boolean cardConnection = App.BioManager.cardConnectSync(1000);
            if (cardConnection) {
                onCardStatusListener = new Biometrics.OnCardStatusListener() {
                    @Override
                    public void onCardStatusChange(String s, int prevState, int currentState) {
                        if (Variables.CARD_ABSENT == currentState) {
                            Functions.Show_Alert2(StockAdjustment.this, "CARD ABSENT", "Place Card on top on the device");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        dbHandler.clearOngoingIssueTable();
    }


}