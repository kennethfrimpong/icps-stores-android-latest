package com.icpsltd.stores.activities;

import static com.icpsltd.stores.activities.NewIssue.FETCH_DELAY_TIME;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.barcode.BarcodeScanner;

import com.icpsltd.stores.adapterclasses.RetrievedStaff;
import com.icpsltd.stores.adapterclasses.RetrievedStock;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.R;
import com.icpsltd.stores.adapterclasses.RetrievedLocation;
import com.icpsltd.stores.adapterclasses.RetrievedProductMain;
import com.icpsltd.stores.adapterclasses.RetrievedStore;
import com.icpsltd.stores.utils.ItemLocationParser;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.utils.StockTable;
import com.icpsltd.stores.utils.TokenChecker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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

public class QueryByProduct extends AppCompatActivity {


    private BarcodeScanner scanner1;
    private MaterialAutoCompleteTextView productCode;
    private String qrCodeString;

    private String enteredItemCode;

    private LinearProgressIndicator linearProgressIndicator;
    private TextView loadingText;

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
    private String productID;
    private String product_name;

    private String staffID;
    private String staffName;

    private String itemCode;

    private String function;

    private String moveDate;
    private String moveTime;

    private OkHttpClient okHttpClient;

    List<RetrievedStock> fetchedStock;

    private MyPrefs myPrefs;

    private String fromStore;
    private String fromShelf;
    private String fromLevel;
    private String fromSpace;

    private String address;

    private String toStore;
    private String toShelf;
    private String toLevel;
    private String toSpace;
    AlertDialog alertDialog;

    private Handler shandler;

    private boolean proceedSearch = true;






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
        myPrefs = new MyPrefs();
        shandler = new Handler();


        String apiHost = dbHandler.getApiHost();
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
        fetchProducts();

        try {
            getQrCodeString = getIntent().getStringExtra("qrCode");
            function = getIntent().getStringExtra("function");

            if (getQrCodeString != null) {
                productCode.setText(getQrCodeString);
                itemCode = getQrCodeString;
                queryProduct();
            }

            if (function.equals("ReturnQR")){
                fromStore = getIntent().getStringExtra("fromStore");
                fromShelf = getIntent().getStringExtra("fromShelf");
                fromLevel = getIntent().getStringExtra("fromLevel");
                fromSpace = getIntent().getStringExtra("fromSpace");

                toStore = getIntent().getStringExtra("toStore");
                toShelf = getIntent().getStringExtra("toShelf");
                toLevel = getIntent().getStringExtra("toLevel");
                toSpace = getIntent().getStringExtra("toSpace");

                moveDate = getIntent().getStringExtra("moveDate");
                moveTime = getIntent().getStringExtra("moveTime");

                itemCode = getIntent().getStringExtra("itemCode");
                product_name = getIntent().getStringExtra("productName");
                productCode.setText(itemCode);
                productID = itemCode;

                DBHandler dbHandler1 = new DBHandler(this);
                staffName = dbHandler1.getFirstName()+" "+dbHandler1.getLastName();
                staffID = String.valueOf(dbHandler1.getIssuerID());
                Log.d("itemCode2",itemCode);

                moveFromQR(itemCode, product_name, fromStore, fromShelf, fromLevel, fromSpace, toStore, toShelf,  toLevel, toSpace, staffID, staffName, moveDate, moveTime);

                queryProduct();
            }
        } catch (Exception e) {
            Log.e("Intent ", e.toString());

        }



        String classname = QueryByProduct.class.getName().toString();
        Log.e("Class Name", classname);
        fetchedProductTable = new ArrayList<>();
        adapter = new ArrayAdapter<RetrievedProductMain>(getApplicationContext(), R.layout.issue_history_list, R.id.receiver_name, fetchedProductTable);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //if(KeyEvent.KEYCODE_DEL == 67 || KeyEvent.KEYCODE_FORWARD_DEL == 112){
                //    proceedSearch = true;
                //}


                shandler.removeCallbacksAndMessages(null);
                shandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchProducts();
                        //search();
                    }
                }, FETCH_DELAY_TIME);


            }

            @Override
            public void afterTextChanged(Editable s) {
                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.productFetchProgress);
                linearProgressIndicator.setVisibility(View.VISIBLE);
                if (productCode.getText().toString().equals("")){
                    linearProgressIndicator.setVisibility(View.GONE);
                    move_item.setVisibility(View.GONE);
                }


            }
        };

        productCode.addTextChangedListener(textWatcher);


    }

    private void fetchProducts(){
        //if (proceedSearch){
        //    search();
        //}

        FetchStockTable fetchStockTable = new FetchStockTable(new FetchStockTaskListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //LinearProgressIndicator linearProgressIndicator = findViewById(R.id.staffFetchProgress);
                        //linearProgressIndicator.setVisibility(View.GONE);
                        fetchedStock = new ArrayList<>();
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        SQLiteDatabase db = dbHandler.getReadableDatabase();
                        Cursor cursor = db.rawQuery("SELECT * FROM stockTable LIMIT 6",null);
                        while (cursor.moveToNext()){
                            String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                            String productName = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                            String productDesc = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));

                            Float productQuantity = cursor.getFloat(cursor.getColumnIndexOrThrow("ProductQuantity"));
                            String productStore = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                            String productUnit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));
                            String productLocation = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                            String imageAvailable = cursor.getString(cursor.getColumnIndexOrThrow("ImageAvailable"));

                            RetrievedStock retrievedStock = new RetrievedStock(id,productName,productDesc,productQuantity,productLocation,productStore,productUnit,null,imageAvailable);
                            fetchedStock.add(retrievedStock);

                        }

                        dbHandler.close();
                        db.close();
                    }
                });


                ArrayAdapter<RetrievedStock> arrayAdapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(),R.layout.stock_list_layout,R.id.item_code, fetchedStock){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);

                        RetrievedStock retrievedStock = getItem(position);
                        TextView item_code = view.findViewById(R.id.item_code);
                        TextView item_name = view.findViewById(R.id.item_name);
                        TextView item_location = view.findViewById(R.id.item_location);

                        item_name.setText(retrievedStock.getName());
                        item_name.setSelected(true);
                        item_code.setText(retrievedStock.getID());
                        item_location.setText(retrievedStock.getStore());

                        //Selected receiver
                        productCode = findViewById(R.id.item_code_input);
                        //productCode.setEnabled(true);


                        productCode.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                //proceedSearch = false;
                                //Toast.makeText(QueryByProduct.this, "Item Clicked", Toast.LENGTH_SHORT).show();
                                RetrievedStock retrievedStock1 = getItem(position);
                                //MaterialAutoCompleteTextView materialAutoCompleteTextView1 = productCode;
                                productCode.setText(retrievedStock1.getID());
                                productCode.setError(null);
                                address = retrievedStock1.getLocation();
                                itemCode = retrievedStock1.getID();
                                //proceedSearch = false;
                                //search();
                                queryProduct();
                                //proceedSearch = false;
                                move_item.setVisibility(View.VISIBLE);
                                retrieved_location_id = retrievedStock1.getLocation();
                                retrieved_store_id = retrievedStock1.getStore();
                                Log.i("++ Location ID", retrieved_location_id);
                                Log.i("Store ID", retrieved_store_id);
                                ///MyPrefs myPrefs = new MyPrefs();
                                //myPrefs.saveReceiverFirstName();
                                //receiver_name = retrievedStaff1.getfirstName();
                                //receiver_dept = retrievedStaff1.getDepartment();
                                //staffID = retrievedStaff1.getID();
                                //TextView textView = findViewById(R.id.remove);
                                //textView.setVisibility(View.VISIBLE);
                                //productCode.setEnabled(false);

                            }
                        });

                        return view;
                    }
                };

                productCode.setThreshold(0);
                productCode.setAdapter(arrayAdapter);
                arrayAdapter.notifyDataSetChanged();

            }
        });
        fetchStockTable.execute();
    }

    public void moveFromQR(String productID, String product_name, String fromStore, String fromShelf, String fromLevel, String fromSpace, String toStore, String toShelf, String toLevel, String toSpace, String staffID, String staffName, String moveDate, String moveTime){
        DBHandler dbHandler1 = new DBHandler(getApplicationContext());

        dbHandler1.syncMoveTable(productID,product_name,fromStore,fromShelf,fromLevel,fromSpace,toStore,toShelf,toLevel,toSpace,staffID,staffName,moveDate,moveTime);
        SyncMoveHistoryTask syncMoveHistoryTask = new SyncMoveHistoryTask(new SyncMoveHistoryTaskListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {
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
        try{
            alertDialog.dismiss();
        } catch (Exception e){
            e.printStackTrace();
        }

        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    public void startMoveQR(View view) {
        String classname = QueryByProduct.class.getName().toString();
        Intent intent = new Intent(QueryByProduct.this, ScanQR.class);
        intent.putExtra("class", classname);
        intent.putExtra("fromStore", fromStore);
        intent.putExtra("fromShelf",fromShelf);
        intent.putExtra("fromLevel",fromLevel);
        intent.putExtra("fromSpace",fromSpace);
        intent.putExtra("productName",product_name);
        intent.putExtra("function","MoveQR");
        intent.putExtra("itemCode",itemCode);
        Log.d("itemCode0",itemCode);
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
                public void onTaskComplete(JSONArray jsonArray) {

                    SQLiteDatabase db = dbHandler.getReadableDatabase();
                    Cursor cursor = db.rawQuery("SELECT * FROM stockTable WHERE UPPER(ProductName)=? OR id=?", new String[]{enteredItemCode.toUpperCase(), enteredItemCode});
                    while (cursor.moveToNext()){
                        TextView productName = findViewById(R.id.product_name);
                        TextView productQuantity = findViewById(R.id.product_quantity);
                        TextView productStore = findViewById(R.id.product_store);
                        TextView productLocation = findViewById(R.id.product_location);
                        product_name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                        productID = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                        address = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                        String store1 = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));

                        productStore.setText(store1);
                        productLocation.setText(address);

                        /*
                        if(getQrCodeString != null){
                            address = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                        }

                         */

                        /*

                        try{
                            if(!address.contains("-") || !address.contains("SHELF") || !address.contains("LEVEL") || !address.contains("SPACE")){
                                Log.d("Exception",address);
                                throw new Exception();

                            }

                            move_item.setVisibility(View.VISIBLE);

                            String[] addressArray = address.split("-");
                            String store1 = addressArray[0];
                            store1 = store1.trim();
                            store1 = store1.replace("_"," ");
                            String shelf1 = addressArray[1];
                            shelf1 = shelf1.trim();
                            String level1 = addressArray[2];
                            level1 = level1.trim();
                            String space1 = addressArray[3];
                            space1 = space1.replace(" ","");
                            //itemCode = addressArray[4];

                            String shelfNumber = shelf1.substring(5);
                            String levelNumber = level1.substring(5);
                            String spaceNumber = space1.substring(5);

                            productStore.setText(store1);
                            String productLocationString = "Shelf: "+shelfNumber+", Level: "+levelNumber+", Space: "+spaceNumber;
                            productLocation.setText(productLocationString);

                        } catch (Exception e) {
                            Log.d("Exception","Raised");
                            e.printStackTrace();
                            productStore.setText("N/A");
                            productLocation.setText("N/A");
                        }

                         */


                        productName.setText(product_name);
                        productName.setSelected(true);
                        productQuantity.setText(cursor.getString(cursor.getColumnIndexOrThrow("ProductQuantity"))+" "+cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit")));
                        //productStore.setText(cursor.getString(cursor.getColumnIndexOrThrow("ProductStore")));
                        //productLocation.setText(cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation")));
                    }

                    FetchProductMainTable fetchProductMainTable = new FetchProductMainTable(new FetchProductMainTaskListener() {
                        @Override
                        public void onTaskComplete(JSONArray jsonArray1) {
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

    public void search() {
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
        try{
            adapter = new ArrayAdapter<RetrievedProductMain>(getApplicationContext(), R.layout.issue_history_list, R.id.receiver_name, fetchedProductTable);
            adapter.clear();
            adapter.notifyDataSetChanged();
            itemCode = productCode.getText().toString();
        } catch (Exception e){
            e.printStackTrace();
        }

        //queryProduct();
        //populate materialautocomplete textview with product names
        //then query product on selection
    }

    public void move_product(View view) {

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(QueryByProduct.this);
        View view1 = getLayoutInflater().inflate(R.layout.move_product, null);
        materialAlertDialogBuilder.setView(view1);
        alertDialog = materialAlertDialogBuilder.create();
        TextView item_name = view1.findViewById(R.id.item_name);
        item_name.setText(product_name);
        item_name.setSelected(true);

        TextView store_name = view1.findViewById(R.id.store_name);
        TextView shelf = view1.findViewById(R.id.shelf);
        TextView level = view1.findViewById(R.id.level);
        TextView space = view1.findViewById(R.id.space);

        try{
            if(!address.contains("-") || !address.contains("SHELF") || !address.contains("LEVEL") || !address.contains("SPACE")){
                throw new Exception();
            }
            String[] addressArray = address.split("-");
            String store1 = addressArray[0];
            store1 = store1.trim();
            store1 = store1.replace("_"," ");
            String shelf1 = addressArray[1];
            shelf1 = shelf1.trim();
            String level1 = addressArray[2];
            level1 = level1.trim();
            String space1 = addressArray[3];
            space1 = space1.replace(" ","");
            //itemCode = addressArray[4];

            String shelfNumber = shelf1.substring(5);
            String levelNumber = level1.substring(5);
            String spaceNumber = space1.substring(5);

            store_name.setText(store1);
            shelf.setText("Shelf: "+shelfNumber);
            level.setText("Level: "+levelNumber);
            space.setText("Space: "+spaceNumber);
        } catch (Exception e){
            e.printStackTrace();
            store_name.setText("N/A");
            shelf.setText("Shelf: N/A");
            level.setText("Level: N/A");
            space.setText("Space: N/A");
        }



        /*
        LinearProgressIndicator linearProgressIndicator1 = view1.findViewById(R.id.move_progress_indicator);
        linearProgressIndicator1.setVisibility(View.VISIBLE);

        spinner = view1.findViewById(R.id.to_store_spinner);
        spinner2 = view1.findViewById(R.id.to_location_spinner);

        spinnerx = view1.findViewById(R.id.from_store_spinner);
        spinner2x = view1.findViewById(R.id.from_location_spinner);

        FetchStoreTable fetchStoreTable = new FetchStoreTable(new FetchStoreTaskListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {

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
                    //String storeID = currentItem.getId().toString();
                    String storeName = currentItem.getStoreName();
                    if(storeName.equals(retrieved_store_id)){
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
                            //String locationID = currentItem.getId().toString();
                            String locationName = currentItem.getLocationName();
                            //Log.d("Location ID", retrieved_location_id);
                            if(locationName.equals(retrieved_location_id)){
                                spinner2x.setSelection(i);
                                spinner2x.setEnabled(false);
                                fromLocationID = String.valueOf(currentItem.getId());
                                fromLocationName = String.valueOf(currentItem.getLocationName());
                                Log.d("From Location", currentItem.getLocationName());
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
                                .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                //.post(requestBody)
                                .build();
                        Response response = okHttpClient.newCall(request).execute();
                        String resString = response.body().string();
                        Log.i("Response",resString);

                        JSONObject jsonObject3 = new JSONObject(resString);

                        String status1 = jsonObject3.optString("status");
                        resString = jsonObject3.optString("data");

                        TokenChecker tokenChecker = new TokenChecker();
                        tokenChecker.checkToken(status1, getApplicationContext(), QueryByProduct.this);

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
                    public void onTaskComplete(JSONArray jsonArray) {
                        alertDialog.dismiss();
                        Toast.makeText(QueryByProduct.this, " Moved Successfully", Toast.LENGTH_SHORT).show();

                    }
                });
                syncMoveHistoryTask.execute();
            }
        });
        */

        alertDialog.show();

    }


    public interface FetchStockTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStockTable extends AsyncTask<Void, Void, JSONArray> {
        //fetches default items to stock table on first tap
        private QueryByProduct.FetchStockTaskListener listener;

        public FetchStockTable(QueryByProduct.FetchStockTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected JSONArray doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            JSONArray jsonArray;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // move_item.setVisibility(View.GONE);
                }
            });
            try  {
                enteredItemCode = productCode.getText().toString();
                String sql = "{\"type\":\"queryByProduct\",\"condition\":\"getStockTable\",\"enteredItemCode\":\""+enteredItemCode+"\"}";
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
                tokenChecker.checkToken(status1, getApplicationContext(), QueryByProduct.this);

                jsonArray = new JSONArray(resString);
                response.close();

                if (jsonArray.length() > 0){

                    for (int i = 0; i < jsonArray.length(); i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Log.d("JSON",jsonObject.toString());
                       // retrieved_location_id = jsonObject.optString("locationID");
                       // retrieved_store_id = jsonObject.optString("storeID");
                        String itemCode = jsonObject.optString("itemCode");
                        String productName = jsonObject.optString("productName");
                        String productDesc = jsonObject.optString("productDesc");
                        String productStore = jsonObject.optString("productStore");
                        String productLocation = jsonObject.optString("productLocation");
                        String productUnit = jsonObject.optString("productUnit");
                        double productQuantity = jsonObject.optDouble("productQuantity");
                        String imageAvailable = jsonObject.optString("imageAvailable");

                        ItemLocationParser itemLocationParser = new ItemLocationParser();
                        String[] parsedLocation = itemLocationParser.parseLocation(productLocation);
                        productStore = parsedLocation[0];
                        productLocation = parsedLocation[1];

                        dbHandler1.syncStockTable(itemCode,productName,productDesc,(float)productQuantity,productStore,productLocation,productUnit,imageAvailable);
                    }


                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            move_item.setVisibility(View.GONE);
                            productCode.setError("Item not found");
                            //Toast.makeText(QueryByProduct.this, "Item not found", Toast.LENGTH_SHORT).show();
                        }
                    });

                }


            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            if (listener != null) {
                listener.onTaskComplete(jsonArray);
            }
            LinearProgressIndicator linearProgressIndicator = findViewById(R.id.productFetchProgress);
            linearProgressIndicator.setVisibility(View.INVISIBLE);
            Log.d("Fetch Complete", retrieved_location_id+" - "+retrieved_store_id);

        }
    }


    public interface FetchProductMainTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchProductMainTable extends AsyncTask<Void, Void, JSONArray> {
        private QueryByProduct.FetchProductMainTaskListener listener;

        public FetchProductMainTable(QueryByProduct.FetchProductMainTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected JSONArray doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearProductTableMain();
            JSONArray jsonArray1;
            JSONArray jsonArray2;
            try {

                String sql1 = "{\"type\":\"queryByProduct\",\"condition\":\"getProductMain\",\"main_type\":\"issue\",\"enteredItemCode\":\""+enteredItemCode+"\"}";
                RequestBody requestBody =  RequestBody.create(sql1, okhttp3.MediaType.parse("application/json; charset=utf-8"));
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
                tokenChecker.checkToken(status1, getApplicationContext(), QueryByProduct.this);

                jsonArray1 = new JSONArray(resString);
                response.close();

                for (int i = 0; i < jsonArray1.length(); i++){
                    JSONObject jsonObject = jsonArray1.getJSONObject(i);
                    int TransactionID = jsonObject.optInt("TransactionID");
                    String TransactionType = jsonObject.optString("TransactionType");
                    String ReceiverName = jsonObject.optString("ReceiverName");
                    String ReceiverDepartment = jsonObject.optString("ReceiverDepartment");
                    String IssuerName = jsonObject.optString("IssuerName");
                    String TransactionDate = jsonObject.optString("TransactionDate");
                    String TransactionTime = jsonObject.optString("TransactionTime");
                    String ProductQuantity = jsonObject.optString("ProductQuantity");
                    String BookNumber = jsonObject.optString("BookNumber");
                    String Balance = jsonObject.optString("Balance");
                    String ProductUnit = jsonObject.optString("ProductUnit");
                    dbHandler1.syncProductTableMainIssue(TransactionID,TransactionType,ReceiverName,ReceiverDepartment,IssuerName,TransactionDate,TransactionTime,ProductQuantity,BookNumber,Balance,ProductUnit);
                }


                String sql2 = "{\"type\":\"queryByProduct\",\"condition\":\"getProductMain\",\"main_type\":\"addition\",\"enteredItemCode\":\""+enteredItemCode+"\"}";
                RequestBody requestBody2 =  RequestBody.create(sql2, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request2 = new Request.Builder()
                        .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                        .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                        .post(requestBody2)
                        .build();
                Response response2 = okHttpClient.newCall(request2).execute();
                String resString2 = response2.body().string();

                JSONObject jsonObject4 = new JSONObject(resString2);

                String status2 = jsonObject4.optString("status");
                resString2 = jsonObject4.optString("data");


                tokenChecker.checkToken(status2, getApplicationContext(), QueryByProduct.this);

                jsonArray2 = new JSONArray(resString2);
                response.close();

                for (int i = 0; i < jsonArray2.length(); i++){
                    JSONObject jsonObject = jsonArray2.getJSONObject(i);
                    int TransactionID = jsonObject.optInt("TransactionID");
                    String TransactionType = jsonObject.optString("TransactionType");
                    String StaffName = jsonObject.optString("StaffName");
                    String AdditionType = jsonObject.optString("AdditionType");
                    String QuantityAdded = jsonObject.optString("QuantityAdded");
                    String TransactionDate = jsonObject.optString("TransactionDate");
                    String TransactionTime = jsonObject.optString("TransactionTime");
                    String BookNumber = jsonObject.optString("BookNumber");
                    String Balance = jsonObject.optString("Balance");
                    String ProductUnit = jsonObject.optString("ProductUnit");
                    dbHandler1.syncProductTableMainAddition(TransactionID,TransactionType,StaffName,AdditionType,TransactionDate,TransactionTime,QuantityAdded,Balance,BookNumber,ProductUnit);
                }


            } catch (Exception e) {
                Log.e("STOCK ADD FAILED", "Error adding stock", e);

            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            if (listener != null) {
                listener.onTaskComplete(jsonArray);
            }
            //LinearProgressIndicator linearProgressIndicator = findViewById(R.id.productFetchProgress);
            //linearProgressIndicator.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    public interface FetchStoreTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStoreTable extends AsyncTask<Void, Void, JSONArray> {
        private QueryByProduct.FetchStoreTaskListener listener;

        public FetchStoreTable(QueryByProduct.FetchStoreTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStoreTable();
            dbHandler1.clearLocationTable();
            JSONArray jsonArray1;
            JSONArray jsonArray2;

            try  {

                String storesql = "{\"type\":\"queryByLocation\",\"condition\":\"getStoreTable\"}";
                String locationsql = "{\"type\":\"queryByLocation\",\"condition\":\"getLocationTable\"}";

                RequestBody requestBody =  RequestBody.create(storesql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
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
                tokenChecker.checkToken(status1, getApplicationContext(), QueryByProduct.this);

                jsonArray1 = new JSONArray(resString);
                response.close();


                RequestBody requestBody1 =  RequestBody.create(locationsql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request1 = new Request.Builder()
                        .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                        .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                        .post(requestBody1)
                        .build();
                Response response1 = okHttpClient.newCall(request1).execute();
                String resString1 = response1.body().string();

                JSONObject jsonObject5 = new JSONObject(resString1);

                String status2 = jsonObject5.optString("status");
                resString1 = jsonObject5.optString("data");

                tokenChecker.checkToken(status2, getApplicationContext(), QueryByProduct.this);


                jsonArray2 = new JSONArray(resString1);
                response.close();



                for (int i=0; i<jsonArray1.length(); i++){
                    JSONObject jsonObject1 = jsonArray1.getJSONObject(i);
                    int id = jsonObject1.optInt("id");
                    String StoreName = jsonObject1.optString("StoreName");
                    String StoreLocation = jsonObject1.optString("StoreLocation");
                    String StoreDescription = jsonObject1.optString("StoreDescription");
                    dbHandler1.syncStoreTable(id,StoreName,StoreLocation,StoreDescription);

                }

                for (int j=0; j<jsonArray2.length(); j++){
                    JSONObject jsonObject2 = jsonArray2.getJSONObject(j);
                    int id = jsonObject2.optInt("id");
                    int storeid = jsonObject2.optInt("storeID");
                    String locationName = jsonObject2.optString("locationName");
                    String locationStore = jsonObject2.optString("locationStore");
                    String locationDescription = jsonObject2.optString("locationDescription");
                    dbHandler1.syncLocationTable(id,locationName,storeid,locationStore,locationDescription);

                }

            } catch (Exception e) {
                Log.e("STORE ADD FAILED", "Error adding store", e);

            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            if (listener != null) {
                listener.onTaskComplete(jsonArray);
            }
        }
    }


    public interface SyncMoveHistoryTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class SyncMoveHistoryTask extends AsyncTask<Void, Void, JSONArray> {
        private QueryByProduct.SyncMoveHistoryTaskListener listener;

        public SyncMoveHistoryTask(QueryByProduct.SyncMoveHistoryTaskListener listener) {
            this.listener = listener;
        }
        Boolean transactionSuccess = true;

        @Override
        protected JSONArray doInBackground(Void... voids) {
            DBHandler dbHandler = new DBHandler(getApplicationContext());
            SQLiteDatabase db = dbHandler.openDatabase(getApplicationContext());

            try {

                Cursor cursor = db.rawQuery("SELECT * FROM moveHistoryTable",null);




                while (cursor.moveToNext()){

                    String productIDx = cursor.getString(cursor.getColumnIndexOrThrow("ProductID"));
                    String product_namex = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                    String fromStorex = cursor.getString(cursor.getColumnIndexOrThrow("FromStore"));
                    String fromShelfx = cursor.getString(cursor.getColumnIndexOrThrow("FromShelf"));
                    String fromLevelx = cursor.getString(cursor.getColumnIndexOrThrow("FromLevel"));
                    String fromSpacex = cursor.getString(cursor.getColumnIndexOrThrow("FromSpace"));
                    String toStorex = cursor.getString(cursor.getColumnIndexOrThrow("ToStore"));
                    String toShelfx = cursor.getString(cursor.getColumnIndexOrThrow("ToShelf"));
                    String MoveDate = cursor.getString(cursor.getColumnIndexOrThrow("MoveDate"));
                    String MoveTime = cursor.getString(cursor.getColumnIndexOrThrow("MoveTime"));
                    String toLevelx = cursor.getString(cursor.getColumnIndexOrThrow("ToLevel"));
                    String toSpacex = cursor.getString(cursor.getColumnIndexOrThrow("ToSpace"));

                    String insertSql = "{\"type\":\"queryByProduct\",\"condition\":\"syncMoveHistory\",\"ProductID\":\""+productIDx+"\",\"ProductName\":\""+product_namex+"\",\"FromStore\":\""+fromStorex+"\",\"FromShelf\":\""+fromShelfx+"\",\"FromLevel\":\""+fromLevelx+"\",\"FromSpace\":\""+fromSpacex+"\",\"ToStore\":\""+toStorex+"\",\"ToShelf\":\""+toShelfx+"\",\"ToLevel\":\""+toLevelx+"\",\"ToSpace\":\""+toSpacex+"\",\"StaffID\":\""+staffID+"\",\"StaffName\":\""+staffName+"\",\"MoveDate\":\""+MoveDate+"\",\"MoveTime\":\""+MoveTime+"\",\"productID\":\""+productID+"\"}";
                    RequestBody requestBody =  RequestBody.create(insertSql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/fetch")
                            .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                            .post(requestBody)
                            .build();
                    Response response = okHttpClient.newCall(request).execute();
                    String resString = response.body().string();

                    //Log.d("ReturnFromApi",resString);

                    JSONObject jsonObject3 = new JSONObject(resString);

                    String status1 = jsonObject3.optString("status");
                    resString = jsonObject3.optString("data");

                    TokenChecker tokenChecker = new TokenChecker();
                    tokenChecker.checkToken(status1, getApplicationContext(), QueryByProduct.this);

                    JSONObject jsonObject = new JSONObject(resString);
                    response.close();

                    if(jsonObject.optString("status").equals("success")){
                        transactionSuccess = true;
                    } else if (jsonObject.optString("status").equals("failed")) {
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
        protected void onPostExecute(JSONArray jsonArray) {

            if (listener != null && transactionSuccess) {
                listener.onTaskComplete(jsonArray);
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