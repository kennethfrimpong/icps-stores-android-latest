package com.icpsltd.stores;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import okhttp3.Response;

public class StockAdjustment extends AppCompatActivity {
    private String URL;
    private String USER;
    private String PASSWORD;

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

    private Integer item_qty;

    private Integer dbquantity;

    private Integer localquantity;

    private Integer new_quantity;

    private boolean blocktransaction = false;
    private String localname;

    private boolean isAdd;
    private boolean isCheckedx = false;

    private String confirmerName;

    private boolean confirmerConfirmed = false;

    private MaterialButtonToggleGroup materialButtonToggleGroup;

    private Integer accessID = 0;

    private OkHttpClient okHttpClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_adjustment);

        TextView asname = findViewById(R.id.firstLastName);
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



        int layout = R.layout.add_issue_item;
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
    }

    private boolean isTouchInsideBottomSheetContent(MotionEvent event) {
        int layout = R.layout.add_issue_item;
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
        Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit FROM ongoingIssueTable",null);
        while (cursor.moveToNext()){
            TextView textView = findViewById(R.id.no_item_txt);
            textView.setVisibility(View.GONE);
            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
            Integer quantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
            String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
            String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
            String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
            Log.e("Retrieved",name);

            RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,store,location,unit);
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
                store_location.setText(retrievedStock.getStore());
                product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                product_location.setText(retrievedStock.getLocation());
                DBHandler dbHandler1 = new DBHandler(getApplicationContext());

                ongoingListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        RetrievedStock retrievedStock = getItem(position);


                        Integer ID = retrievedStock.getID();
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
                                        if(Integer.valueOf(customEditText.getText().toString()) > 0){
                                            //customEditText.setText(String.valueOf(inc+1));
                                            dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),Integer.valueOf(customEditText.getText().toString()),retrievedStock.getStore(),retrievedStock.getLocation(), retrievedStock.getUnit());
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
                                Integer inc = Integer.valueOf(customEditText.getText().toString());
                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                if (Integer.valueOf(customEditText.getText().toString())>0){
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


                            }
                        });
                        decreasebutton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Integer inc = Integer.valueOf(customEditText.getText().toString());
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

    }


    public void create_issue(View view) {
        TextView rmtv = findViewById(R.id.remove);
        EditText bknm = findViewById(R.id.book_number);
        DBHandler dbHandler = new DBHandler(getApplicationContext());

                if(dbHandler.checkOngoingIssueTableEmptiness().equals("notempty")){

                    int layout = R.layout.confirm_transaction_sheet;
                    View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                    bottomSheetDialog.setContentView(bottomSheetView);
                    TextView textView = bottomSheetDialog.findViewById(R.id.receiver_confirm);
                    EditText passpin = bottomSheetDialog.findViewById(R.id.pin_input);
                    LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.verify_progress);


                    MaterialButton confirm = bottomSheetDialog.findViewById(R.id.confirm_issue);
                    confirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            linearProgressIndicator.setVisibility(View.VISIBLE);
                            accessID = Integer.valueOf(passpin.getText().toString());
                            Integer dbid = Integer.valueOf(dbHandler.getAccessID());

                            if (!passpin.getText().toString().equals("")){
                                if(!Objects.equals(accessID, dbid)){
                                    VerifyStaffTask verifyStaffTask = new VerifyStaffTask(new VerifyStaffTaskListener() {
                                        @Override
                                        public void onTaskComplete(Connection connection) {


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
                                                    public void onTaskComplete(Connection connection) {
                                                        if (!blocktransaction){
                                                            linearProgressIndicator.setVisibility(View.VISIBLE);
                                                            SyncHistoryTask syncHistoryTask = new SyncHistoryTask(new SyncHistoryTaskListener() {
                                                                @Override
                                                                public void onTaskComplete(Connection connection) {
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
                                    });
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
                    textView.setText("Ask a colleague to confirm with Access ID");
                    bottomSheetDialog.show();
                } else {
                    Toast.makeText(this, "Add at least 1 item", Toast.LENGTH_SHORT).show();
                }

    }
    public void showBottomSheet(){

        if (isCheckedx){

            int layout = R.layout.add_issue_item;
            View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            bottomSheetDialog.setContentView(bottomSheetView);
            LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.fetch_progress);
            linearProgressIndicator.setVisibility(View.VISIBLE);
            searchString = bottomSheetDialog.findViewById(R.id.stockSearch);
            ListView listView = (ListView) bottomSheetDialog.findViewById(R.id.add_items_listview);
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            searchString.setInputType(InputType.TYPE_CLASS_TEXT);


            FetchStockTable fetchStockTable = new FetchStockTable(new FetchStockTaskListener() {
                @Override
                public void onTaskComplete(Connection connection) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.fetch_progress);
                            linearProgressIndicator.setVisibility(View.GONE);


                            fetchedStockTable = new ArrayList<>();
                            DBHandler dbHandler = new DBHandler(getApplicationContext());
                            SQLiteDatabase db = dbHandler.getReadableDatabase();
                            Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit FROM stockTable LIMIT 5",null);
                            while (cursor.moveToNext()){
                                Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                                String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                                Integer quantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
                                String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                                String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                                String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                                String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
                                Log.e("Retrieved",name);

                                RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,store,location,unit);
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
                                    if(isAdd){
                                        textInputLayout2.setHint("Enter addition quantity");
                                    } else {
                                        textInputLayout2.setHint("Enter deduction quantity");
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
                                                Integer quantity = Integer.valueOf(squantity);

                                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                                dbHandler1.addToOngoingIssue(retrievedStock.getID(),retrievedStock.getName(),retrievedStock.getDescription(),quantity,retrievedStock.getStore(),retrievedStock.getLocation(),retrievedStock.getUnit());
                                                Toast.makeText(getApplicationContext(),retrievedStock.getName()+" added successfully",Toast.LENGTH_SHORT).show();

                                                //update listview
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ongoingIssue();
                                                    }
                                                });
                                                bottomSheetDialog.dismiss();

                                            /*

                                            if (!squantity.equals("") && !squantity.equals("0")){

                                                if (quantity <= retrievedStock.getQuantity()) {


                                                } else if (quantity > retrievedStock.getQuantity()) {
                                                    Toast.makeText(getApplicationContext(),"Error: Quantity entered exceeds retrieved quantity ",Toast.LENGTH_LONG).show();

                                                }
                                            } else {
                                                Toast.makeText(getApplicationContext(),"Enter a valid quantity ",Toast.LENGTH_LONG).show();
                                            }
                                            */

                                            } catch (Exception e){
                                                Toast.makeText(getApplicationContext(),"Enter a valid number",Toast.LENGTH_LONG).show();
                                            }




                                        }
                                    });
                                }
                            });


                        }
                    });
                }
            });
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



                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            String searchedString = searchString.getText().toString();
                            String firstPer = "%"+searchedString;
                            String queryString = firstPer+"%";


                            if (searchString.getText().toString().length() < 1){
                                //fetches default 5 most issued items if search box is empty, change values in fetchStockStable and here to update results number
                                FetchDefaultStockTable fetchDefaultStockTable = new FetchDefaultStockTable(new FetchDefaultStockTaskListener() {
                                    @Override
                                    public void onTaskComplete(Connection connection) {
                                        //LinearProgressIndicator linearProgressIndicator = bottomSheetDialog.findViewById(R.id.fetch_progress);
                                        //linearProgressIndicator.setVisibility(View.GONE);
                                        Log.e("TASK ASYNC","TASK IS DONE");
                                        Log.i("SearchTest", "Search string empty, showing default items");
                                        Cursor cursorx = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit FROM stockTable LIMIT 5",null);
                                        while (cursorx.moveToNext()){
                                            Integer id = cursorx.getInt(cursorx.getColumnIndexOrThrow("id"));
                                            String name = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));
                                            Integer quantity = cursorx.getInt(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                                            String store = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductStore"));
                                            String location = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductLocation"));
                                            String description = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductDesc"));
                                            String unit = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductUnit"));

                                            RetrievedStock retrievedStock = new RetrievedStock(id, name,description,quantity,store,location,unit);
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
                                });
                                fetchDefaultStockTable.execute();



                            } else {
                                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                                dbHandler1.clearStockTable();

                                //if text box is not empty, this fetches 20 items matching the query
                                FetchUpdatedStockTable fetchUpdatedStockTable = new FetchUpdatedStockTable(new FetchUpdatedStockTaskListener() {
                                    @Override
                                    public void onTaskComplete(Connection connection) {

                                        adapter.clear();
                                        adapter.notifyDataSetChanged();
                                        Log.i("SearchTest","Search string updated with "+searchedString);
                                        Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit FROM stockTable WHERE productName LIKE '"+queryString+"' LIMIT 20",null);
                                        while (cursor.moveToNext()){
                                            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                                            String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                                            Log.e("Retrievedx",name);
                                            Integer quantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
                                            String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                                            String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                                            String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                                            String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));

                                            RetrievedStock retrievedStock = new RetrievedStock(id, name,description,quantity,store,location,unit);
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
                                });
                                fetchUpdatedStockTable.execute();

                            }
                        }
                    });


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

        } else{
            Toast.makeText(this, "Select an adjustment type", Toast.LENGTH_SHORT).show();
        }



    }


    public void add_item(View view) {
        DBHandler dbHandler1 = new DBHandler(getApplicationContext());
        dbHandler1.clearStockTable();
        showBottomSheet();

    }


    public interface FetchStockTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStockTable extends AsyncTask<Void, Void, Connection> {
        //fetches default items to stock table on first tap
        private FetchStockTaskListener listener;

        public FetchStockTable(FetchStockTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT * FROM stockTable LIMIT 5";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                while (resultSet.next()){
                    Log.i("StockTable","Row fetched successfully");
                    StockTable stockTable = new StockTable(getApplicationContext());
                    stockTable.syncStockTable(resultSet.getInt("id"),resultSet.getString("productName"),resultSet.getString("productDesc"),resultSet.getInt("productQuantity"),resultSet.getString("productStore"),resultSet.getString("productLocation"),resultSet.getString("productUnit"));

                }
                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

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

    public interface FetchUpdatedStockTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchUpdatedStockTable extends AsyncTask<Void, Void, Connection> {
        private FetchUpdatedStockTaskListener listener;

        public FetchUpdatedStockTable(FetchUpdatedStockTaskListener listener) {
            this.listener = listener;
        }



        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String searchedString = searchString.getText().toString();
                String matchesWholeWord = searchedString;
                String startsWith = searchedString+"%";
                String withHyphen = searchedString+"-%";
                String startsWithWordInSentence = "% "+searchedString+"%";
                Log.i("SearchTest","Updated Search Started");
                //Cursor cursor = db.rawQuery(,null);
                String sql = "SELECT * FROM stockTable WHERE productName LIKE '"+matchesWholeWord+"' OR productName LIKE '"+startsWith+"' OR productName LIKE '"+withHyphen+"' OR productName LIKE '"+startsWithWordInSentence+"' LIMIT 20";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                while (resultSet.next()){
                    Log.i("StockTable","Row fetched successfully "+resultSet.getString("productName"));
                    StockTable stockTable = new StockTable(getApplicationContext());
                    stockTable.syncStockTable(resultSet.getInt("id"),resultSet.getString("productName"),resultSet.getString("productDesc"),resultSet.getInt("productQuantity"),resultSet.getString("productStore"),resultSet.getString("productLocation"),resultSet.getString("productUnit"));

                }
                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

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

    public interface FetchDefaultStockTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchDefaultStockTable extends AsyncTask<Void, Void, Connection> {
        //fetches default items to stock table
        private FetchDefaultStockTaskListener listener;

        public FetchDefaultStockTable(FetchDefaultStockTaskListener listener) {
            this.listener = listener;
        }



        @Override
        protected Connection doInBackground(Void... voids) {

            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT * FROM stockTable LIMIT 5";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                while (resultSet.next()){
                    Log.i("StockTable","Row fetched successfully");
                    StockTable stockTable = new StockTable(getApplicationContext());
                    stockTable.syncStockTable(resultSet.getInt("id"),resultSet.getString("productName"),resultSet.getString("productDesc"),resultSet.getInt("productQuantity"),resultSet.getString("productStore"),resultSet.getString("productLocation"),resultSet.getString("productUnit"));

                }
                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

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

    public interface FetchStaffTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStaffTable extends AsyncTask<Void, Void, Connection> {
        private FetchStaffTaskListener listener;

        public FetchStaffTable(FetchStaffTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                dbHandler1.clearStaffTable();
                String searchedString = searchStaff.getText().toString();
                String matchesWholeWord = searchedString;
                String startsWith = searchedString+"%";
                String withHyphen = searchedString+"-%";
                String startsWithWordInSentence = "% "+searchedString+"%";
                Log.i("SearchTest","Updated Search Started");
                //Cursor cursor = db.rawQuery(,null);
                String sql = "SELECT * FROM staffTable WHERE firstName LIKE '"+matchesWholeWord+"' OR firstName LIKE '"+startsWith+"' OR firstName LIKE '"+withHyphen+"' OR firstName LIKE '"+startsWithWordInSentence+"' OR middleName LIKE '"+startsWith+"' OR middleName LIKE '"+matchesWholeWord+"' OR middleName LIKE '"+startsWithWordInSentence+"' OR lastName LIKE '"+startsWith+"' OR lastName LIKE '"+matchesWholeWord+"' OR lastName LIKE '"+startsWithWordInSentence+"' LIMIT 6";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                while (resultSet.next()){
                    Log.i("StaffTable","Row fetched successfully "+resultSet.getString("firstName")+" "+resultSet.getString("lastName"));
                    dbHandler1.syncStaffTable(resultSet.getInt("staffID"),resultSet.getString("firstName"),resultSet.getString("middleName"),resultSet.getString("lastName"),resultSet.getString("type"),resultSet.getString("department"));
                }
                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("STAFF ADD FAILED", "Error adding staff", e);

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


    public interface VerifyStaffTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class VerifyStaffTask extends AsyncTask<Void, Void, Connection> {
        private VerifyStaffTaskListener listener;

        public VerifyStaffTask(VerifyStaffTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT userID, firstName, lastName FROM userTable WHERE accessID = '"+accessID+"' ";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);

                if(resultSet.next()){
                    resultSet.previous();
                    while (resultSet.next()){
                        staffInt = resultSet.getInt("userID");
                        confirmerName = resultSet.getString("firstName")+" "+resultSet.getString("lastName");
                        confirmerConfirmed = true;

                    }

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StockAdjustment.this, "No such stores staff found", Toast.LENGTH_SHORT).show();
                            confirmerConfirmed = false;

                        }
                    });
                }




                String sqlx = "SELECT lastID FROM metaTable where activityName = 'stockAdd'";
                Statement statementx = connection.createStatement();
                ResultSet resultSetx = statementx.executeQuery(sqlx);
                while (resultSetx.next()){
                    latestIssueID = resultSetx.getInt("lastID");
                }

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("STAFF VERIFY FAILED", "Error verifying staff", e);

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

    public interface SyncHistoryTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class SyncHistoryTask extends AsyncTask<Void, Void, Connection> {
        private SyncHistoryTaskListener listener;

        public SyncHistoryTask(SyncHistoryTaskListener listener) {
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
                    Integer ProductQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
                    String sqlx = "SELECT productQuantity FROM stockTable WHERE id ='"+id+"'";
                    PreparedStatement statementx = connection.prepareStatement(sqlx);
                    ResultSet resultSetx = statementx.executeQuery();
                    while (resultSetx.next()){
                        Integer fetchedQTY = resultSetx.getInt("productQuantity");
                        balance = String.valueOf(fetchedQTY);
                        Log.e("Balance","FetchedQTY "+String.valueOf(fetchedQTY));
                    }



                    String ProductName = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                    String ProductDesc = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
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


                    String AdditionType;
                    String sql;


                    if(isAdd){
                        AdditionType = "Stock Addition";
                        sql = "INSERT INTO adjustmentHistoryTable (`TransactionID`, `id`, `ProductName`, `ProductDesc`, `QuantityAdded`, `ProductStore`, `ProductLocation`, `StaffID`, `StaffName`, `ConfirmerID`, `ConfirmerName`, `TransactionDate`, `TransactionTime`, `Balance`, `ProductUnit`, `TransactionType`) VALUES ('"+TransactionID+"','"+id+"', '"+ProductName+"', '"+ProductDesc+"', '"+ProductQuantity+"', '"+ProductStore+"', '"+ProductLocation+"', '"+IssuerID+"', '"+IssuerName+"','"+staffInt+"', '"+ReceiverName+"', '"+TransactionDate+"', '"+TransactionTime+"','"+balance+"','"+unit+"','"+AdditionType+"');";


                    } else{
                        AdditionType = "Stock Deduction";
                        sql = "INSERT INTO adjustmentHistoryTable (`TransactionID`, `id`, `ProductName`, `ProductDesc`, `QuantityDeducted`, `ProductStore`, `ProductLocation`, `StaffID`, `StaffName`, `ConfirmerID`, `ConfirmerName`, `TransactionDate`, `TransactionTime`, `Balance`, `ProductUnit`, `TransactionType`) VALUES ('"+TransactionID+"','"+id+"', '"+ProductName+"', '"+ProductDesc+"', '"+ProductQuantity+"', '"+ProductStore+"', '"+ProductLocation+"', '"+IssuerID+"', '"+IssuerName+"','"+staffInt+"', '"+ReceiverName+"', '"+TransactionDate+"', '"+TransactionTime+"','"+balance+"','"+unit+"','"+AdditionType+"');";


                    }


                    statement.executeUpdate(sql);



                }

                Integer updatedID = new_id;
                String sqlx = "UPDATE metaTable SET lastID = "+updatedID+" WHERE activityName = 'stockAdd';";
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
        private SyncQuantityTaskListener listener;

        public SyncQuantityTask(SyncQuantityTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.openDatabase(getApplicationContext());
            //Cursor cursor = db.rawQuery("SELECT id, ProductQuantity, ProductName FROM ongoingIssueTable",null);

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {

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
                        if(isAdd){
                            new_quantity = dbquantity+localquantity;
                        } else {
                            new_quantity = dbquantity-localquantity;
                        }

                        Log.i("RT",String.valueOf(dbquantity));
                        String quantity_update = "UPDATE stockTable SET productQuantity ="+new_quantity+" WHERE id='"+idx+"'";
                        PreparedStatement preparedStatement = connection.prepareStatement(quantity_update);
                        preparedStatement.executeUpdate();

                    }
                }
                Integer updatedID = new_id;
                String sqlx = "UPDATE metaTable SET lastID = "+updatedID+" WHERE activityName = 'stockAdd';";
                PreparedStatement statement = connection.prepareStatement(sqlx);
                statement.executeUpdate();

            } catch (Exception e) {
                Log.e("SYNC ADJUSTMENT HISTORY TASK", "Error syncing history", e);

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