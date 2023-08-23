package com.icpsltd.stores;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import com.google.mlkit.vision.common.InputImage;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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

public class QueryByProduct extends AppCompatActivity {


    private BarcodeScanner scanner1;
    private TextInputEditText productCode;
    private String qrCodeString;

    private String enteredItemCode;

    private LinearProgressIndicator linearProgressIndicator;
    private TextView loadingText;

    private String URL;
    private String USER;
    private String PASSWORD;

    private List<RetrievedProductMain> fetchedProductTable;
    private ArrayAdapter<RetrievedProductMain> adapter;
    private Integer numberOfResults;
    private Integer resultCount;



    private ListView listView;

    private DBHandler dbHandler = new DBHandler(this);

    private String getQrCodeString;



    private LinearLayout linearLayout;
    private ConstraintLayout constraintLayout;

    private TextView showing;

    private ImageView move_item;

    private List<RetrievedStore> fetchedStoreTable;
    private List<RetrievedLocation> fetchedLocationTable;
    private ArrayAdapter<RetrievedStore> arrayAdapter;
    private ArrayAdapter<RetrievedLocation> arrayAdapter2;
    AppCompatSpinner spinner;
    AppCompatSpinner spinner2;

    private ArrayAdapter<RetrievedStore> arrayAdapterx;
    private ArrayAdapter<RetrievedLocation> arrayAdapter2x;
    AppCompatSpinner spinnerx;
    AppCompatSpinner spinner2x;

    private String selectedStoreID;
    private String selectedLocationID;
    private String retrieved_store_id;
    private String retrieved_location_id;
    private Integer productID;
    private String product_name;
    private String fromStoreID;
    private String fromStoreName;
    private String fromLocationID;
    private String fromLocationName;
    private String toStoreID;
    private String toStoreName;
    private String toLocationID;
    private String toLocationName;
    private String staffID;
    private String staffName;

    private String itemCode;

    private String function;

    private String moveDate;
    private String moveTime;

    private OkHttpClient okHttpClient;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_by_product);

        linearLayout = findViewById(R.id.app_content_layout);
        constraintLayout = findViewById(R.id.camera_layout);
        productCode = findViewById(R.id.item_code_input);
        linearProgressIndicator = findViewById(R.id.fetch_progress);
        loadingText = findViewById(R.id.loading);
        showing = findViewById(R.id.showing_results);
        move_item = findViewById(R.id.move_product);

        String dbHost = dbHandler.getDatabaseHost();
        String dbName = dbHandler.getDatabaseName();
        String dbUser = dbHandler.getDatabaseUsername();
        String dbPass = dbHandler.getDatabasePassword();
        String apiHost = dbHandler.getApiHost();
        URL = "jdbc:mysql://"+dbHost+":3306/"+dbName;
        Log.i("URL",URL);
        USER = dbUser;
        PASSWORD = dbPass;


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
                        runOnUiThread(()->{ Toast.makeText(QueryByProduct.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

        String name = dbHandler.getFirstName() + " " + dbHandler.getLastName();
        TextView nameview = findViewById(R.id.firstLastName);
        nameview.setText("as " + name);
        dbHandler.clearStockTable();

        listView = findViewById(R.id.transaction_history_listview);

        try {
            getQrCodeString = getIntent().getStringExtra("qrCode");
            function = getIntent().getStringExtra("function");

            if (getQrCodeString != null) {
                productCode.setText(getQrCodeString);
                itemCode = getQrCodeString;
                queryProduct();
            }

            if (function.equals("ReturnQR")){
                fromStoreID = getIntent().getStringExtra("fromStoreID");
                fromStoreName = getIntent().getStringExtra("fromStoreName");
                fromLocationID = getIntent().getStringExtra("fromLocationID");
                fromLocationName = getIntent().getStringExtra("fromLocationName");

                toStoreID = getIntent().getStringExtra("toStoreID");
                toStoreName = getIntent().getStringExtra("toStoreName");
                toLocationID = getIntent().getStringExtra("toLocationID");
                toLocationName = getIntent().getStringExtra("toLocationName");

                moveDate = getIntent().getStringExtra("moveDate");
                moveTime = getIntent().getStringExtra("moveTime");

                itemCode = getIntent().getStringExtra("itemCode");
                product_name = getIntent().getStringExtra("productName");
                productCode.setText(itemCode);
                productID = Integer.valueOf(itemCode);

                DBHandler dbHandler1 = new DBHandler(this);
                staffName = dbHandler1.getFirstName()+" "+dbHandler1.getLastName();
                staffID = String.valueOf(dbHandler1.getIssuerID());

                moveFromQR(Integer.valueOf(itemCode), product_name, fromStoreID, fromStoreName, fromLocationID, fromLocationName, toStoreID, toStoreName, toLocationID, toLocationName, staffID, staffName, moveDate, moveTime);

                queryProduct();
            }
        } catch (Exception e) {
            Log.e("Intent ", e.toString());

        }

        String classname = QueryByProduct.class.getName().toString();
        Log.e("Class Name", classname);
        fetchedProductTable = new ArrayList<>();
        adapter = new ArrayAdapter<RetrievedProductMain>(getApplicationContext(), R.layout.issue_history_list, R.id.receiver_name, fetchedProductTable);


    }

    public void moveFromQR(Integer productID, String product_name, String fromStoreID, String fromStoreName, String fromLocationID, String fromLocationName, String toStoreID, String toStoreName, String toLocationID, String toLocationName, String staffID, String staffName, String moveDate, String moveTime){
        DBHandler dbHandler1 = new DBHandler(getApplicationContext());

        dbHandler1.syncMoveTable(productID,product_name,fromStoreID,fromStoreName,fromLocationID,fromLocationName,toStoreID,toStoreName,toLocationID,toLocationName,staffID,staffName,moveDate,moveTime);
        SyncMoveHistoryTask syncMoveHistoryTask = new SyncMoveHistoryTask(new SyncMoveHistoryTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {
                //alertDialog.dismiss();
                Toast.makeText(QueryByProduct.this, " Moved Successfully", Toast.LENGTH_SHORT).show();

            }
        });
        syncMoveHistoryTask.execute();
    }



    public void startQR(View view) {
        String classname = QueryByProduct.class.getName().toString();
        TextView productName = findViewById(R.id.product_name);
        TextView productQuantity = findViewById(R.id.product_quantity);
        TextView productStore = findViewById(R.id.product_store);
        TextView productLocation = findViewById(R.id.product_location);
        showing.setText("");

        productName.setText("");
        productQuantity.setText("");
        productStore.setText("");
        productLocation.setText("");
        productCode.setError(null);
        adapter.clear();
        adapter.notifyDataSetChanged();
        linearLayout.setVisibility(View.GONE);
        constraintLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(QueryByProduct.this, ScanQR.class);
        intent.putExtra("class", classname);
        intent.putExtra("function","ScanProduct");
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    public void startMoveQR(View view) {
        String classname = QueryByProduct.class.getName().toString();
        Intent intent = new Intent(QueryByProduct.this, ScanQR.class);
        intent.putExtra("class", classname);
        intent.putExtra("fromLocationID", fromLocationID);
        intent.putExtra("fromLocationName",fromLocationName);
        intent.putExtra("fromStoreID",fromStoreID);
        intent.putExtra("fromStoreName",fromStoreName);
        intent.putExtra("productName",product_name);
        intent.putExtra("function","MoveQR");
        intent.putExtra("itemCode",itemCode);
        startActivity(intent);

    }


    public void queryProduct(){

        linearProgressIndicator.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("Loading...");
        enteredItemCode = productCode.getText().toString();
        if(!enteredItemCode.isEmpty()){
            FetchStockTable fetchStockTable = new FetchStockTable(new FetchStockTaskListener() {
                @Override
                public void onTaskComplete(Connection connection) {

                    SQLiteDatabase db = dbHandler.getReadableDatabase();
                    Cursor cursor = db.rawQuery("SELECT * FROM stockTable",null);
                    while (cursor.moveToNext()){
                        TextView productName = findViewById(R.id.product_name);
                        TextView productQuantity = findViewById(R.id.product_quantity);
                        TextView productStore = findViewById(R.id.product_store);
                        TextView productLocation = findViewById(R.id.product_location);
                        product_name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                        productID = cursor.getInt(cursor.getColumnIndexOrThrow("id"));


                        productName.setText(product_name);
                        productQuantity.setText(cursor.getString(cursor.getColumnIndexOrThrow("ProductQuantity"))+" "+cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit")));
                        productStore.setText(cursor.getString(cursor.getColumnIndexOrThrow("ProductStore")));
                        productLocation.setText(cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation")));
                    }

                    FetchProductMainTable fetchProductMainTable = new FetchProductMainTable(new FetchProductMainTaskListener() {
                        @Override
                        public void onTaskComplete(Connection connection) {
                            linearProgressIndicator.setVisibility(View.GONE);
                            loadingText.setVisibility(View.GONE);

                            listView.setVisibility(View.VISIBLE);
                            showing.setVisibility(View.VISIBLE);

                            fetchedProductTable = new ArrayList<>();
                            DBHandler dbHandler = new DBHandler(getApplicationContext());
                            SQLiteDatabase db = dbHandler.getReadableDatabase();
                            Cursor cursor = db.rawQuery("SELECT id, TransactionType, ReceiverName, ReceiverDepartment, TransactionDate, ProductQuantity, StaffName, AdditionType, issuerName, BookNumber, Balance, TransactionTime, ProductUnit FROM productTableMain ORDER BY TransactionDate DESC",null);
                            resultCount =0;
                            numberOfResults = 1;

                            while (cursor.moveToNext()){
                                resultCount = numberOfResults++;
                                Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                                String name = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverName"));
                                String department = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverDepartment"));
                                String date = cursor.getString(cursor.getColumnIndexOrThrow("TransactionDate"));
                                String quantity = cursor.getString(cursor.getColumnIndexOrThrow("ProductQuantity"));
                                String type = cursor.getString(cursor.getColumnIndexOrThrow("TransactionType"));
                                String staffName = cursor.getString(cursor.getColumnIndexOrThrow("StaffName"));
                                String issuerName = cursor.getString(cursor.getColumnIndexOrThrow("IssuerName"));
                                String additionType = cursor.getString(cursor.getColumnIndexOrThrow("AdditionType"));
                                String bookNumber = cursor.getString(cursor.getColumnIndexOrThrow("BookNumber"));
                                String balance = cursor.getString(cursor.getColumnIndexOrThrow("Balance"));
                                String transactionTime = cursor.getString(cursor.getColumnIndexOrThrow("TransactionTime"));
                                String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));

                                //Log.e("Retrieved",name);

                                RetrievedProductMain retrievedProductMain = new RetrievedProductMain(id,type,name,department,date,quantity,staffName,issuerName,additionType,bookNumber,balance,transactionTime,unit);
                                fetchedProductTable.add(retrievedProductMain);

                            }

                            if (resultCount==1){
                                showing.setText("Showing 1 result");
                            } else if (resultCount==0) {
                                showing.setVisibility(View.GONE);
                                loadingText.setVisibility(View.VISIBLE);
                                loadingText.setText("No results found");
                            } else {
                                showing.setText("Showing "+String.valueOf(resultCount)+" results");
                            }

                            adapter = new ArrayAdapter<RetrievedProductMain>(getApplicationContext(), R.layout.issue_history_list, R.id.receiver_name, fetchedProductTable) {
                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    View view = super.getView(position, convertView, parent);

                                    TextView id = view.findViewById(R.id.issue_transaction_id);
                                    TextView product_name = view.findViewById(R.id.receiver_name);
                                    TextView receiver_department = view.findViewById(R.id.receiver_department);
                                    TextView date = view.findViewById(R.id.issue_date);
                                    TextView quantity = view.findViewById(R.id.quantity);
                                    quantity.setVisibility(View.VISIBLE);


                                    RetrievedProductMain retrievedProduct = getItem(position);


                                    date.setText(retrievedProduct.getDate());
                                    id.setText("#"+String.valueOf(retrievedProduct.getID()));
                                    quantity.setText("Qty: "+retrievedProduct.getQuantity());
                                    ImageView imageView = view.findViewById(R.id.shipping_box);

                                    if(retrievedProduct.getType().equals("Stock Issue")){
                                        receiver_department.setText(retrievedProduct.getDepartment());
                                        product_name.setText(retrievedProduct.getName());
                                        imageView.setImageResource(R.drawable.make_issue_red);
                                    } else if (retrievedProduct.getType().equals("Stock Addition")){
                                        product_name.setText(retrievedProduct.getStaffName());
                                        receiver_department.setText(retrievedProduct.getAdditionType());
                                        if(retrievedProduct.getAdditionType().equals("Stock Return")){
                                            imageView.setImageResource(R.drawable.stock_return1);
                                        } else {
                                            imageView.setImageResource(R.drawable.add_stock_green);
                                        }
                                        product_name.setText(retrievedProduct.getStaffName());
                                    }






                                    return view;
                                }
                            };

                            assert listView != null;
                            listView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    RetrievedProductMain retrievedProductMain = (RetrievedProductMain) parent.getItemAtPosition(position);

                                    View editLayout = getLayoutInflater().inflate(R.layout.view_product_details, null);
                                    MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(QueryByProduct.this);
                                    materialAlertDialogBuilder
                                            .setView(editLayout)
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {




                                                }
                                            });
                                    AlertDialog alertDialog = materialAlertDialogBuilder.create();
                                    TextView transaction_type_id = editLayout.findViewById(R.id.transaction_type_id);
                                    TextView transaction_date = editLayout.findViewById(R.id.transaction_date);
                                    TextView transaction_time = editLayout.findViewById(R.id.transaction_time);
                                    TextView book_number = editLayout.findViewById(R.id.book_number);
                                    TextView receiver_name = editLayout.findViewById(R.id.receiver_name);
                                    TextView receiver_dept = editLayout.findViewById(R.id.receiver_department);
                                    TextView quantity_requested = editLayout.findViewById(R.id.quantity_requested);
                                    TextView balance = editLayout.findViewById(R.id.balance);
                                    TextView issuerName = editLayout.findViewById(R.id.issuer_name);
                                    TextView qty_added = editLayout.findViewById(R.id.quantity_added);

                                    //boxes
                                    ConstraintLayout requested = editLayout.findViewById(R.id.quantity_requested_box);
                                    ConstraintLayout added = editLayout.findViewById(R.id.quantity_added_box);
                                    //ConstraintLayout issuer = editLayout.findViewById(R.id.issuer_box);
                                    ConstraintLayout receiver = editLayout.findViewById(R.id.receiver_box);
                                    ConstraintLayout receiver_department = editLayout.findViewById(R.id.department_box);
                                    String type = "";


                                    if(retrievedProductMain.getType().equals("Stock Issue")){
                                        issuerName.setText(retrievedProductMain.getIssuerName());
                                        requested.setVisibility(View.VISIBLE);
                                        added.setVisibility(View.GONE);
                                        type = retrievedProductMain.getType();
                                    } else if (retrievedProductMain.getType().equals("Stock Addition")){
                                        issuerName.setText(retrievedProductMain.getStaffName());
                                        qty_added.setText(retrievedProductMain.getQuantity()+" "+retrievedProductMain.getUnit());
                                        requested.setVisibility(View.GONE);
                                        added.setVisibility(View.VISIBLE);
                                        receiver.setVisibility(View.GONE);
                                        receiver_department.setVisibility(View.GONE);
                                        if (retrievedProductMain.getAdditionType().equals("Stock Return")){
                                            type = "Stock Return";
                                        } else {
                                            type = retrievedProductMain.getType();
                                        }

                                    }

                                    transaction_date.setText(retrievedProductMain.getDate());
                                    transaction_type_id.setText(type+" #"+String.valueOf(retrievedProductMain.getID()));
                                    quantity_requested.setText(retrievedProductMain.getQuantity()+" "+retrievedProductMain.getUnit());
                                    transaction_time.setText(retrievedProductMain.getTransactionTime());
                                    book_number.setText(retrievedProductMain.getBookNumber());
                                    receiver_name.setText(retrievedProductMain.getName());
                                    receiver_dept.setText(retrievedProductMain.getDepartment());
                                    balance.setText(retrievedProductMain.getBalance()+" "+retrievedProductMain.getUnit());
                                    alertDialog.show();


                                }
                            });
                           // move_item.setVisibility(View.VISIBLE);
                        }
                    });
                    fetchProductMainTable.execute();


                }
            });
            fetchStockTable.execute();

        }else{

            productCode.setError("Enter item code");
            linearProgressIndicator.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
        }
    }

    public void search(View view) {
        TextView productName = findViewById(R.id.product_name);
        TextView productQuantity = findViewById(R.id.product_quantity);
        TextView productStore = findViewById(R.id.product_store);
        TextView productLocation = findViewById(R.id.product_location);
        showing.setText("");

        productName.setText("");
        productQuantity.setText("");
        productStore.setText("");
        productLocation.setText("");
        productCode.setError(null);
        adapter.clear();
        adapter.notifyDataSetChanged();
        try{
            itemCode = productCode.getText().toString();
        } catch (Exception e){
            e.printStackTrace();
        }

        queryProduct();
    }

    public void move_product(View view) {

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(QueryByProduct.this);
        View view1 = getLayoutInflater().inflate(R.layout.move_product, null);
        materialAlertDialogBuilder.setView(view1);
        AlertDialog alertDialog = materialAlertDialogBuilder.create();
        TextView item_name = view1.findViewById(R.id.move_item_name);
        item_name.setText("Move "+product_name);
        LinearProgressIndicator linearProgressIndicator1 = view1.findViewById(R.id.move_progress_indicator);
        linearProgressIndicator1.setVisibility(View.VISIBLE);

        spinner = view1.findViewById(R.id.to_store_spinner);
        spinner2 = view1.findViewById(R.id.to_location_spinner);

        spinnerx = view1.findViewById(R.id.from_store_spinner);
        spinner2x = view1.findViewById(R.id.from_location_spinner);

        FetchStoreTable fetchStoreTable = new FetchStoreTable(new FetchStoreTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linearProgressIndicator1.setVisibility(View.INVISIBLE);


                        fetchedStoreTable = new ArrayList<>();
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        SQLiteDatabase db = dbHandler.getReadableDatabase();
                        Cursor cursor = db.rawQuery("SELECT * FROM storeTable",null);

                        while (cursor.moveToNext()){
                            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                            String name = cursor.getString(cursor.getColumnIndexOrThrow("StoreName"));
                            String location = cursor.getString(cursor.getColumnIndexOrThrow("StoreLocation"));
                            String description = cursor.getString(cursor.getColumnIndexOrThrow("StoreDescription"));

                            RetrievedStore retrievedStore = new RetrievedStore(id,name,location,description);
                            fetchedStoreTable.add(retrievedStore);

                        }
                        linearProgressIndicator.setVisibility(View.INVISIBLE);
                    }
                });

                arrayAdapterx = new ArrayAdapter<RetrievedStore>(getApplicationContext(),R.layout.location_list_layout,R.id.store_name, fetchedStoreTable){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);

                        RetrievedStore retrievedStore1 = (RetrievedStore) getItem(position);
                        TextView store_name = view.findViewById(R.id.store_name);
                        TextView store_floor = view.findViewById(R.id.store_floor);
                        ImageView store_icon = view.findViewById(R.id.store_icon);


                        store_name.setText(retrievedStore1.getStoreName());
                        store_floor.setText(retrievedStore1.getStoreLocation());
                        fromStoreID = String.valueOf(retrievedStore1.getId());
                        fromStoreName = String.valueOf(retrievedStore1.getStoreName());

                        store_name.setTextColor(Color.GRAY);
                        store_floor.setTextColor(Color.GRAY);
                        if(store_floor.getText().equals("First Floor")){
                            store_icon.setImageResource(R.drawable.one_gray);
                        } else if (store_floor.getText().equals("Second Floor")) {
                            store_icon.setImageResource(R.drawable.two_gray);
                        } else if (store_floor.getText().equals("Third Floor")) {
                            store_icon.setImageResource(R.drawable.three_gray);
                        } else if (retrievedStore1.getId().equals(1)) {
                            store_icon.setImageResource(R.drawable.g_gray);
                        }

                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        return getView(position, convertView, parent);

                    }
                };

                spinnerx.setAdapter(arrayAdapterx);

                for (int i = 0; i < arrayAdapterx.getCount(); i++) {
                    RetrievedStore currentItem = arrayAdapterx.getItem(i);
                    String storeID = currentItem.getId().toString();
                    if(storeID.equals(retrieved_store_id)){
                        spinnerx.setSelection(i);
                        spinnerx.setEnabled(false);
                        break;
                    }
                }

                spinnerx.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        RetrievedStore retrievedStore = (RetrievedStore) parent.getItemAtPosition(position);
                        selectedStoreID = String.valueOf(retrievedStore.getId());





                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                fetchedLocationTable = new ArrayList<>();
                                DBHandler dbHandler = new DBHandler(getApplicationContext());
                                SQLiteDatabase db = dbHandler.getReadableDatabase();
                                Cursor cursor = db.rawQuery("SELECT * FROM locationTable WHERE storeID='"+selectedStoreID+"' ORDER BY id ASC",null);
                                while (cursor.moveToNext()){
                                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                                    String location_name = cursor.getString(cursor.getColumnIndexOrThrow("LocationName"));
                                    Integer storesID = cursor.getInt(cursor.getColumnIndexOrThrow("storeID"));
                                    String location_store = cursor.getString(cursor.getColumnIndexOrThrow("LocationStore"));
                                    String description = cursor.getString(cursor.getColumnIndexOrThrow("LocationDescription"));

                                    RetrievedLocation retrievedLocation = new RetrievedLocation(id,location_name,storesID,location_store,description);
                                    fetchedLocationTable.add(retrievedLocation);

                                }
                                linearProgressIndicator.setVisibility(View.INVISIBLE);
                            }
                        });


                        arrayAdapter2x = new ArrayAdapter<RetrievedLocation>(getApplicationContext(),R.layout.location_list_layout_location,R.id.location_name, fetchedLocationTable){
                            @Override
                            public View getView(int positionx, View convertView, ViewGroup parent) {
                                View view = super.getView(positionx, convertView, parent);

                                RetrievedLocation retrievedLocation = (RetrievedLocation)  getItem(positionx);
                                TextView location_name = view.findViewById(R.id.location_name);
                                ImageView store_icon = view.findViewById(R.id.store_icon);
                                selectedLocationID = String.valueOf(retrievedLocation.getId());






                                location_name.setText(retrievedLocation.getLocationName());
                                location_name.setTextColor(Color.GRAY);
                                if(retrievedLocation.getStoreID().equals(2)){
                                    store_icon.setImageResource(R.drawable.one_gray);
                                } else if (retrievedLocation.getStoreID().equals(3)) {
                                    store_icon.setImageResource(R.drawable.two_gray);
                                } else if (retrievedLocation.getStoreID().equals(4)) {
                                    store_icon.setImageResource(R.drawable.three_gray);
                                } else if (retrievedLocation.getStoreID().equals(1)) {
                                    store_icon.setImageResource(R.drawable.g_gray);
                                }

                                return view;
                            }
                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                return getView(position, convertView, parent);

                            }
                        };

                        spinner2x.setAdapter(arrayAdapter2x);

                        for (int i = 0; i < arrayAdapter2x.getCount(); i++) {
                            RetrievedLocation currentItem = arrayAdapter2x.getItem(i);
                            String locationID = currentItem.getId().toString();
                            if(locationID.equals(retrieved_location_id)){
                                spinner2x.setSelection(i);
                                spinner2x.setEnabled(false);
                                fromLocationID = String.valueOf(currentItem.getId());
                                fromLocationName = String.valueOf(currentItem.getLocationName());
                                break;
                            }
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                arrayAdapterx.notifyDataSetChanged();



                arrayAdapter = new ArrayAdapter<RetrievedStore>(getApplicationContext(),R.layout.location_list_layout,R.id.store_name, fetchedStoreTable){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);

                        //RetrievedStore retrievedStore1 = getItem(position);
                        RetrievedStore retrievedStore1 = (RetrievedStore) getItem(position);
                        TextView store_name = view.findViewById(R.id.store_name);
                        TextView store_floor = view.findViewById(R.id.store_floor);
                        ImageView store_icon = view.findViewById(R.id.store_icon);


                        store_name.setText(retrievedStore1.getStoreName());
                        store_floor.setText(retrievedStore1.getStoreLocation());
                        if(store_floor.getText().equals("First Floor")){
                            store_icon.setImageResource(R.drawable.one);
                        } else if (store_floor.getText().equals("Second Floor")) {
                            store_icon.setImageResource(R.drawable.two);
                        } else if (store_floor.getText().equals("Third Floor")) {
                            store_icon.setImageResource(R.drawable.three);
                        } else if (retrievedStore1.getId().equals(1)) {
                            store_icon.setImageResource(R.drawable.g);
                        }

                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        return getView(position, convertView, parent);

                    }
                };

                spinner.setAdapter(arrayAdapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        RetrievedStore retrievedStore = (RetrievedStore) parent.getItemAtPosition(position);
                        selectedStoreID = String.valueOf(retrievedStore.getId());
                        toStoreID = String.valueOf(retrievedStore.getId());
                        toStoreName = String.valueOf(retrievedStore.getStoreName());


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                fetchedLocationTable = new ArrayList<>();
                                DBHandler dbHandler = new DBHandler(getApplicationContext());
                                SQLiteDatabase db = dbHandler.getReadableDatabase();
                                Cursor cursor = db.rawQuery("SELECT * FROM locationTable WHERE storeID='"+selectedStoreID+"' ORDER BY id ASC",null);
                                while (cursor.moveToNext()){
                                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                                    String location_name = cursor.getString(cursor.getColumnIndexOrThrow("LocationName"));
                                    Integer storesID = cursor.getInt(cursor.getColumnIndexOrThrow("storeID"));
                                    String location_store = cursor.getString(cursor.getColumnIndexOrThrow("LocationStore"));
                                    String description = cursor.getString(cursor.getColumnIndexOrThrow("LocationDescription"));

                                    RetrievedLocation retrievedLocation = new RetrievedLocation(id,location_name,storesID,location_store,description);
                                    fetchedLocationTable.add(retrievedLocation);

                                }
                                linearProgressIndicator.setVisibility(View.INVISIBLE);
                            }
                        });


                        arrayAdapter2 = new ArrayAdapter<RetrievedLocation>(getApplicationContext(),R.layout.location_list_layout_location,R.id.location_name, fetchedLocationTable){
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);

                                RetrievedLocation retrievedLocation1 = (RetrievedLocation) getItem(position);
                                TextView location_name = view.findViewById(R.id.location_name);
                                ImageView store_icon = view.findViewById(R.id.store_icon);
                                selectedLocationID = String.valueOf(retrievedLocation1.getId());



                                location_name.setText(retrievedLocation1.getLocationName());
                                if(retrievedLocation1.getStoreID().equals(2)){
                                    store_icon.setImageResource(R.drawable.one);
                                } else if (retrievedLocation1.getStoreID().equals(3)) {
                                    store_icon.setImageResource(R.drawable.two);
                                } else if (retrievedLocation1.getStoreID().equals(4)) {
                                    store_icon.setImageResource(R.drawable.three);
                                } else if (retrievedLocation1.getStoreID().equals(1)) {
                                    store_icon.setImageResource(R.drawable.g);
                                }

                                return view;
                            }
                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                return getView(position, convertView, parent);

                            }
                        };

                        spinner2.setAdapter(arrayAdapter2);

                        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                RetrievedLocation retrievedLocation = (RetrievedLocation) parent.getItemAtPosition(position);
                                selectedLocationID = String.valueOf(retrievedLocation.getId());
                                toLocationID = String.valueOf(retrievedLocation.getId());
                                toLocationName = String.valueOf(retrievedLocation.getLocationName());
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });



                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                arrayAdapter.notifyDataSetChanged();

            }
        });
        fetchStoreTable.execute();

        MaterialButton materialButton = view1.findViewById(R.id.move);
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBHandler dbHandler1 = new DBHandler(getApplicationContext());
                staffName = dbHandler1.getFirstName()+" "+dbHandler1.getLastName();
                staffID = String.valueOf(dbHandler1.getIssuerID());

                String new_date;
                String new_time;

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future<String> futureResult = executorService.submit(() -> {
                    //OkHttpClient okHttpClient = new OkHttpClient();
                    try{
                        Request request = new Request.Builder()
                                .url("https://"+dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/tst/getDateTime")
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

                    dbHandler1.syncMoveTable(productID,product_name,fromStoreID,fromStoreName,fromLocationID,fromLocationName,toStoreID,toStoreName,toLocationID,toLocationName,staffID,staffName,new_date,new_time);



                } catch (Exception e) {
                    e.printStackTrace();
                }


                SyncMoveHistoryTask syncMoveHistoryTask = new SyncMoveHistoryTask(new SyncMoveHistoryTaskListener() {
                    @Override
                    public void onTaskComplete(Connection connection) {
                        alertDialog.dismiss();
                        Toast.makeText(QueryByProduct.this, " Moved Successfully", Toast.LENGTH_SHORT).show();

                    }
                });
                syncMoveHistoryTask.execute();
            }
        });


        alertDialog.show();

    }


    public interface FetchStockTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStockTable extends AsyncTask<Void, Void, Connection> {
        //fetches default items to stock table on first tap
        private QueryByProduct.FetchStockTaskListener listener;

        public FetchStockTable(QueryByProduct.FetchStockTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    move_item.setVisibility(View.GONE);
                }
            });
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT * FROM stockTable WHERE id = '"+enteredItemCode+"' LIMIT 1";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                if (resultSet.next()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            move_item.setVisibility(View.VISIBLE);
                        }
                    });

                    resultSet.previous();
                    while (resultSet.next()){
                        Log.i("StockTable","Row fetched successfully");
                        retrieved_location_id = String.valueOf(resultSet.getInt("locationID"));
                        retrieved_store_id = String.valueOf(resultSet.getInt("storeID"));
                        StockTable stockTable = new StockTable(getApplicationContext());
                        stockTable.syncStockTable(resultSet.getInt("id"),resultSet.getString("productName"),resultSet.getString("productDesc"),resultSet.getInt("productQuantity"),resultSet.getString("productStore"),resultSet.getString("productLocation"),resultSet.getString("productUnit"));

                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            move_item.setVisibility(View.GONE);
                            productCode.setError("Item not found");
                            Toast.makeText(QueryByProduct.this, "Item not found", Toast.LENGTH_SHORT).show();
                        }
                    });

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


    public interface FetchProductMainTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchProductMainTable extends AsyncTask<Void, Void, Connection> {
        private QueryByProduct.FetchProductMainTaskListener listener;

        public FetchProductMainTable(QueryByProduct.FetchProductMainTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearProductTableMain();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT DISTINCT TransactionID, MAX(TransactionType) TransactionType, MAX(ReceiverName) ReceiverName,MAX(ReceiverDepartment) ReceiverDepartment,MAX(IssuerName) IssuerName,MAX(TransactionDate) TransactionDate,MAX(TransactionTime) TransactionTime,MAX(ProductQuantity) ProductQuantity,MAX(BookNumber) BookNumber,MAX(Balance) Balance,MAX(ProductUnit) ProductUnit FROM issueHistoryTable WHERE id ='"+enteredItemCode+"' GROUP BY TransactionID ORDER BY MAX(TransactionNumber) DESC, TransactionID";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);

                while (resultSet.next()){
                    Log.i("StockTable","Row fetched successfully");
                    dbHandler1.syncProductTableMainIssue(resultSet.getInt("TransactionID"),resultSet.getString("TransactionType"),resultSet.getString("ReceiverName"),resultSet.getString("ReceiverDepartment"),resultSet.getString("IssuerName"),resultSet.getString("TransactionDate"),resultSet.getString("TransactionTime"),resultSet.getString("ProductQuantity"),resultSet.getString("BookNumber"),resultSet.getString("Balance"),resultSet.getString("ProductUnit"));
                }



                String sqlx = "SELECT DISTINCT TransactionID, MAX(TransactionType) TransactionType, MAX(StaffName) StaffName,MAX(AdditionType) AdditionType,MAX(TransactionDate) TransactionDate,MAX(TransactionTime) TransactionTime,MAX(QuantityAdded) QuantityAdded,MAX(Balance) Balance,MAX(BookNumber) BookNumber,MAX(ProductUnit) ProductUnit FROM additionHistoryTable WHERE id ='"+enteredItemCode+"' GROUP BY TransactionID ORDER BY MAX(TransactionNumber) DESC, TransactionID";
                Statement statementx = connection.createStatement();
                ResultSet resultSetx = statementx.executeQuery(sqlx);
                Log.i("RESULT SET",""+sqlx);

                while (resultSetx.next()){
                    Log.i("StockTable","Row fetched successfully");
                    dbHandler1.syncProductTableMainAddition(resultSetx.getInt("TransactionID"),resultSetx.getString("TransactionType"),resultSetx.getString("StaffName"),resultSetx.getString("AdditionType"),resultSetx.getString("TransactionDate"),resultSetx.getString("TransactionTime"),resultSetx.getString("QuantityAdded"),resultSetx.getString("Balance"),resultSetx.getString("BookNumber"),resultSetx.getString("ProductUnit"));
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





    @Override
    protected void onResume() {
        super.onResume();
    }

    public interface FetchStoreTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStoreTable extends AsyncTask<Void, Void, Connection> {
        private QueryByProduct.FetchStoreTaskListener listener;

        public FetchStoreTable(QueryByProduct.FetchStoreTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStoreTable();
            dbHandler1.clearLocationTable();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT * FROM storeTable";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                while (resultSet.next()){
                    dbHandler1.syncStoreTable(resultSet.getInt("id"),resultSet.getString("StoreName"),resultSet.getString("StoreLocation"),resultSet.getString("StoreDescription"));
                }

                String sqlx = "SELECT * FROM locationTable";
                Statement statementx = connection.createStatement();
                ResultSet resultSetx = statementx.executeQuery(sqlx);
                Log.i("RESULT SET",""+sqlx);
                while (resultSetx.next()){
                    dbHandler1.syncLocationTable(resultSetx.getInt("id"),resultSetx.getString("locationName"),resultSetx.getInt("storeID"),resultSetx.getString("locationStore"),resultSetx.getString("locationDescription"));
                }


                resultSet.close();
                resultSetx.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("STORE ADD FAILED", "Error adding store", e);

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

    public interface SyncMoveHistoryTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class SyncMoveHistoryTask extends AsyncTask<Void, Void, Connection> {
        private QueryByProduct.SyncMoveHistoryTaskListener listener;

        public SyncMoveHistoryTask(QueryByProduct.SyncMoveHistoryTaskListener listener) {
            this.listener = listener;
        }
        Boolean transactionSuccess = true;

        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.openDatabase(getApplicationContext());




            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                Statement statement = connection.createStatement();
                Cursor cursor = db.rawQuery("SELECT * FROM moveHistoryTable",null);


                while (cursor.moveToNext()){
                    Integer productIDx = cursor.getInt(cursor.getColumnIndexOrThrow("ProductID"));
                    String product_namex = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                    String fromStoreIDx = cursor.getString(cursor.getColumnIndexOrThrow("FromStoreID"));
                    String fromStoreNamex = cursor.getString(cursor.getColumnIndexOrThrow("FromStoreName"));
                    String fromLocationIDx = cursor.getString(cursor.getColumnIndexOrThrow("FromLocationID"));
                    String fromLocationNamex = cursor.getString(cursor.getColumnIndexOrThrow("FromLocationName"));
                    String toStoreIDx = cursor.getString(cursor.getColumnIndexOrThrow("ToStoreID"));
                    String toStoreNamex = cursor.getString(cursor.getColumnIndexOrThrow("ToStoreName"));
                    String MoveDate = cursor.getString(cursor.getColumnIndexOrThrow("MoveDate"));
                    String MoveTime = cursor.getString(cursor.getColumnIndexOrThrow("MoveTime"));
                    String toLocationIDx = cursor.getString(cursor.getColumnIndexOrThrow("ToLocationID"));
                    String toLocationNamex = cursor.getString(cursor.getColumnIndexOrThrow("ToLocationName"));


                    String sql = "INSERT INTO moveHistoryTable (`ProductID`, `ProductName`, `FromStoreID`, `FromStoreName`, `FromLocationID`, `FromLocationName`, `ToStoreID`, `ToStoreName`, `ToLocationID`, `ToLocationName`, `StaffID`, `StaffName`, `MoveDate`, `MoveTime`) VALUES ('"+productIDx+"','"+product_namex+"', '"+fromStoreIDx+"', '"+fromStoreNamex+"', '"+fromLocationIDx+"', '"+fromLocationNamex+"', '"+toStoreIDx+"', '"+toStoreNamex+"', '"+toLocationIDx+"','"+toLocationNamex+"', '"+staffID+"', '"+staffName+"', '"+MoveDate+"', '"+MoveTime+"');";
                    statement.executeUpdate(sql);

                }

                if(transactionSuccess){

                    try{
                        String sqlx = "UPDATE stockTable SET storeID = '"+toStoreID+"', productStore = '"+toStoreName+"', locationID = '"+toLocationID+"', productLocation = '"+toLocationName+"' WHERE id = '"+productID+"';";
                        statement.executeUpdate(sqlx);
                    } catch (Exception e) {
                        Log.e("SYNC MOVE HISTORY TASK", "Error syncing move history", e);
                        transactionSuccess = false;
                    }

                }


            } catch (Exception e) {
                Log.e("SYNC MOVE HISTORY TASK", "Error syncing move history", e);
                transactionSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Connection connection) {
            if (listener != null && transactionSuccess) {
                listener.onTaskComplete(connection);
                queryProduct();
            }
            if(!transactionSuccess){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(QueryByProduct.this, "Error: Couldn't move "+product_name, Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }
    }



}