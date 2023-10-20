package com.icpsltd.stores.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.icpsltd.stores.R;
import com.icpsltd.stores.adapterclasses.RetrievedIssueHistory;
import com.icpsltd.stores.adapterclasses.RetrievedIssueHistoryMain;
import com.icpsltd.stores.utils.DBHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

public class IssueHistory extends AppCompatActivity {

    private List<RetrievedIssueHistoryMain> fetchedIssueHistoryTable;
    private ArrayAdapter<RetrievedIssueHistoryMain> adapter;
    private String startDate;
    private String endDate;

    private OkHttpClient okHttpClient;

    private Integer transactionID;

    private ListView issueHistoryListview;

    private Integer numberOfResults;
    private Integer resultCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_history);
        TextView asname = findViewById(R.id.firstLastName);
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        String dbHost = dbHandler.getDatabaseHost();
        String dbName = dbHandler.getDatabaseName();
        String dbUser = dbHandler.getDatabaseUsername();
        String dbPass = dbHandler.getDatabasePassword();

        asname.setText("as "+dbHandler.getFirstName()+" "+dbHandler.getLastName());

        MaterialAutoCompleteTextView materialAutoCompleteTextView = findViewById(R.id.start_date);
        TextInputEditText materialAutoCompleteTextView1 = findViewById(R.id.end_date);

        materialAutoCompleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDatePicker.Builder<Long> dateBuilder = MaterialDatePicker.Builder.datePicker();
                dateBuilder.setSelection(MaterialDatePicker.thisMonthInUtcMilliseconds());
                dateBuilder.setTextInputFormat(new SimpleDateFormat("yyyy-MM-dd"));
                final MaterialDatePicker materialDatePicker = dateBuilder.build();
                dateBuilder.setTitleText("Start Date");
                materialDatePicker.show(getSupportFragmentManager(),"MATERIAL_DATE_PICKER");
                materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
                    @Override
                    public void onPositiveButtonClick(Object selection) {
                        Date selectedDate = new Date((Long) selection);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String formattedDate = simpleDateFormat.format(selectedDate);
                        materialAutoCompleteTextView.setText(formattedDate);
                    }
                });
            }
        });
        materialAutoCompleteTextView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDatePicker.Builder dateBuilder = MaterialDatePicker.Builder.datePicker();
                dateBuilder.setSelection(MaterialDatePicker.thisMonthInUtcMilliseconds());
                final MaterialDatePicker materialDatePicker = dateBuilder.build();
                dateBuilder.setTitleText("End Date");
                materialDatePicker.show(getSupportFragmentManager(),"MATERIAL_DATE_PICKER");
                materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
                    @Override
                    public void onPositiveButtonClick(Object selection) {
                        Date selectedDate = new Date((Long) selection);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String formattedDate = simpleDateFormat.format(selectedDate);
                        materialAutoCompleteTextView1.setText(formattedDate);
                    }
                });
            }
        });

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
                    if (hostname.equals(dbHandler.getApiHost())){
                        return true;
                    } else {
                        runOnUiThread(()->{ Toast.makeText(IssueHistory.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

        //fetch last 10 issues
        getIssueHistoryMain();

    }

    public void getIssueHistoryMain(){

        ListView listView = findViewById(R.id.issue_history_listview);
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        FetchIssueHistoryMainTable fetchIssueHistoryMainTable = new FetchIssueHistoryMainTable(new FetchIssueHistoryMainTaskListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.fetch_issue_history_progress);
                        linearProgressIndicator.setVisibility(View.GONE);
                        TextView loading = findViewById(R.id.fetch_issue_history_loading);
                        loading.setVisibility(View.GONE);

                        fetchedIssueHistoryTable = new ArrayList<>();
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        SQLiteDatabase db = dbHandler.getReadableDatabase();
                        Cursor cursor = db.rawQuery("SELECT id, ReceiverName, ReceiverDepartment, TransactionDate, TransactionTime, BookNumber FROM issueHistoryTableMain LIMIT 10",null);
                        numberOfResults = 1;
                        while (cursor.moveToNext()){
                            resultCount = numberOfResults++;
                            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                            String name = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverName"));
                            String department = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverDepartment"));
                            String date = cursor.getString(cursor.getColumnIndexOrThrow("TransactionDate"));
                            String time = cursor.getString(cursor.getColumnIndexOrThrow("TransactionTime"));
                            String bookNumber = cursor.getString(cursor.getColumnIndexOrThrow("BookNumber"));
                            Log.e("Retrieved",name);

                            RetrievedIssueHistoryMain retrievedIssueHistoryMain = new RetrievedIssueHistoryMain(id,name,department,date,time,bookNumber);
                            fetchedIssueHistoryTable.add(retrievedIssueHistoryMain);
                        }
                        TextView showing = findViewById(R.id.number_of_results);
                        if (resultCount != null && resultCount==1){
                            showing.setText("Showing 1 result");
                        } else {
                            showing.setText("Showing "+String.valueOf(resultCount)+" results");
                        }
                        adapter = new ArrayAdapter<RetrievedIssueHistoryMain>(getApplicationContext(), R.layout.issue_history_list, R.id.receiver_name, fetchedIssueHistoryTable) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);

                                TextView id = view.findViewById(R.id.issue_transaction_id);
                                TextView product_name = view.findViewById(R.id.receiver_name);
                                TextView receiver_department = view.findViewById(R.id.receiver_department);
                                TextView date = view.findViewById(R.id.issue_date);


                                RetrievedIssueHistoryMain retrievedIssueHistory = getItem(position);
                                product_name.setText(retrievedIssueHistory.getName());
                                receiver_department.setText(retrievedIssueHistory.getDepartment());
                                date.setText(retrievedIssueHistory.getDate());
                                id.setText("#"+String.valueOf(retrievedIssueHistory.getID()));



                                return view;
                            }
                        };

                        assert listView != null;
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        DBHandler dbHandler1 = new DBHandler(getApplicationContext());

                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                RetrievedIssueHistoryMain retrievedIssueHistoryMain = adapter.getItem(position);
                                transactionID = retrievedIssueHistoryMain.getID();


                                View editLayout = getLayoutInflater().inflate(R.layout.view_issue_history, null);
                                MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(IssueHistory.this);
                                materialAlertDialogBuilder.setTitle("Issue #"+retrievedIssueHistoryMain.getID())
                                        .setView(editLayout)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {




                                            }
                                        });
                                AlertDialog alertDialog = materialAlertDialogBuilder.create();
                                TextView transaction_date = editLayout.findViewById(R.id.transaction_date);
                                TextView transaction_time = editLayout.findViewById(R.id.transaction_time);
                                TextView book_number = editLayout.findViewById(R.id.book_number);
                                TextView receiver_name = editLayout.findViewById(R.id.receiver_name);
                                TextView receiver_dept = editLayout.findViewById(R.id.receiver_department);

                                transaction_date.setText(retrievedIssueHistoryMain.getDate());
                                transaction_time.setText(retrievedIssueHistoryMain.getTime());
                                book_number.setText(retrievedIssueHistoryMain.getBookNumber());
                                receiver_name.setText(retrievedIssueHistoryMain.getName());
                                receiver_dept.setText(retrievedIssueHistoryMain.getDepartment());
                                alertDialog.show();
                                
                                
                                FetchIssueHistoryTable fetchIssueHistoryTable = new FetchIssueHistoryTable(new FetchIssueHistoryTaskListener() {
                                    @Override
                                    public void onTaskComplete(JSONArray jsonArray1) {
                                        issueHistoryListview = editLayout.findViewById(R.id.fetchedHistoryListview);
                                        LinearProgressIndicator linearProgressIndicator1 = editLayout.findViewById(R.id.fetch_issue_history_progress);
                                        TextView loading = editLayout.findViewById(R.id.fetch_issue_history_loading);
                                        linearProgressIndicator1.setVisibility(View.GONE);
                                        loading.setVisibility(View.GONE);

                                        List<RetrievedIssueHistory> fetchedOngoingIssueTable = new ArrayList<>();
                                        //load existing ongoing issue
                                        Cursor cursorx = db.rawQuery("SELECT id, productName, productQuantity, productStore, productLocation, productDesc, IssueDate, ReceiverDepartment, productUnit FROM issueHistoryTable",null);
                                        while (cursorx.moveToNext()){

                                            Integer idx = cursorx.getInt(cursorx.getColumnIndexOrThrow("id"));
                                            String namex = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));
                                            String departmentx = cursorx.getString(cursorx.getColumnIndexOrThrow("ReceiverDepartment"));
                                            Integer quantityx = cursorx.getInt(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                                            String storex = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductStore"));
                                            String locationx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductLocation"));
                                            String issueDatex = cursorx.getString(cursorx.getColumnIndexOrThrow("IssueDate"));
                                            String descriptionx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductDesc"));
                                            String unit = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductUnit"));
                                            Log.e("Retrieved",namex);

                                            RetrievedIssueHistory retrievedStock = new RetrievedIssueHistory(idx,namex,descriptionx,quantityx,departmentx,locationx,storex,issueDatex,unit);
                                            fetchedOngoingIssueTable.add(retrievedStock);

                                        }
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                            }
                                        });

                                        ArrayAdapter<RetrievedIssueHistory> ongoingIssueAdapter = new ArrayAdapter<RetrievedIssueHistory>(getApplicationContext(), R.layout.issue_item_item_list, R.id.product_name, fetchedOngoingIssueTable) {
                                            @Override
                                            public View getView(int position, View convertView, ViewGroup parent) {
                                                View view = super.getView(position, convertView, parent);

                                                TextView store_location = view.findViewById(R.id.store_location);
                                                TextView product_name = view.findViewById(R.id.product_name);
                                                TextView product_quantity = view.findViewById(R.id.product_quantity);
                                                TextView product_location = view.findViewById(R.id.product_location);
                                                RetrievedIssueHistory retrievedStock = getItem(position);
                                                product_name.setText(retrievedStock.getName());
                                                store_location.setText(retrievedStock.getStore());
                                                product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity())+" "+retrievedStock.getUnit());
                                                product_location.setText(retrievedStock.getLocation());

                                                return view;
                                            }
                                        };
                                        assert issueHistoryListview != null;
                                        issueHistoryListview.setAdapter(ongoingIssueAdapter);
                                        ongoingIssueAdapter.notifyDataSetChanged();
                                        
                                    }
                                });
                                fetchIssueHistoryTable.execute();

                            }
                        });


                    }
                });


            }
        },"unspecified");
        fetchIssueHistoryMainTable.execute();
        
        /**
        FetchIssueHistoryTable fetchIssueHistoryTable = new FetchIssueHistoryTable(new FetchIssueHistoryTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {

                
            }
        });
        fetchIssueHistoryTable.execute();
         **/

    }

    public void getUpdatedStockHistory(){

        MaterialAutoCompleteTextView materialAutoCompleteTextView = findViewById(R.id.start_date);
        TextInputEditText materialAutoCompleteTextView1 = findViewById(R.id.end_date);
        startDate = materialAutoCompleteTextView.getText().toString();
        endDate = materialAutoCompleteTextView1.getText().toString();
        ListView listView = findViewById(R.id.issue_history_listview);
        listView.setVisibility(View.GONE);
        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.fetch_issue_history_progress);
        linearProgressIndicator.setVisibility(View.VISIBLE);
        TextView loading = findViewById(R.id.fetch_issue_history_loading);
        loading.setVisibility(View.VISIBLE);
        TextView showing = findViewById(R.id.number_of_results);
        showing.setVisibility(View.GONE);

        FetchIssueHistoryMainTable fetchUpdatedIssueHistoryMainTable = new FetchIssueHistoryMainTable(new FetchIssueHistoryMainTaskListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setVisibility(View.VISIBLE);
                        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.fetch_issue_history_progress);
                        linearProgressIndicator.setVisibility(View.GONE);
                        TextView loading = findViewById(R.id.fetch_issue_history_loading);
                        loading.setVisibility(View.GONE);
                        TextView showing = findViewById(R.id.number_of_results);
                        showing.setVisibility(View.VISIBLE);

                        fetchedIssueHistoryTable = new ArrayList<>();
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        SQLiteDatabase db = dbHandler.getReadableDatabase();
                        Cursor cursor = db.rawQuery("SELECT id, ReceiverName, ReceiverDepartment, TransactionDate, TransactionTime, BookNumber FROM issueHistoryTableMain",null);
                        resultCount =0;
                        numberOfResults = 1;

                        while (cursor.moveToNext()){
                            resultCount = numberOfResults++;
                            Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                            String name = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverName"));
                            String department = cursor.getString(cursor.getColumnIndexOrThrow("ReceiverDepartment"));
                            String date = cursor.getString(cursor.getColumnIndexOrThrow("TransactionDate"));
                            String time = cursor.getString(cursor.getColumnIndexOrThrow("TransactionTime"));
                            String bookNumber = cursor.getString(cursor.getColumnIndexOrThrow("BookNumber"));
                            Log.e("Retrieved",name);

                            RetrievedIssueHistoryMain retrievedIssueHistoryMain = new RetrievedIssueHistoryMain(id,name,department,date,time,bookNumber);
                            fetchedIssueHistoryTable.add(retrievedIssueHistoryMain);

                        }
                        //TextView showing = findViewById(R.id.number_of_results);
                        if (resultCount==1){
                            showing.setText("Showing 1 result");
                        } else {
                            showing.setText("Showing "+String.valueOf(resultCount)+" results");
                        }

                        adapter = new ArrayAdapter<RetrievedIssueHistoryMain>(getApplicationContext(), R.layout.issue_history_list, R.id.receiver_name, fetchedIssueHistoryTable) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);

                                TextView id = view.findViewById(R.id.issue_transaction_id);
                                TextView product_name = view.findViewById(R.id.receiver_name);
                                TextView receiver_department = view.findViewById(R.id.receiver_department);
                                TextView date = view.findViewById(R.id.issue_date);


                                RetrievedIssueHistoryMain retrievedIssueHistory = getItem(position);
                                product_name.setText(retrievedIssueHistory.getName());
                                receiver_department.setText(retrievedIssueHistory.getDepartment());
                                date.setText(retrievedIssueHistory.getDate());
                                id.setText("#"+String.valueOf(retrievedIssueHistory.getID()));



                                return view;
                            }
                        };

                        assert listView != null;
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();

                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                RetrievedIssueHistoryMain retrievedIssueHistoryMain = adapter.getItem(position);
                                transactionID = retrievedIssueHistoryMain.getID();


                                View editLayout = getLayoutInflater().inflate(R.layout.view_issue_history, null);
                                MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(IssueHistory.this);
                                materialAlertDialogBuilder.setTitle("Issue #"+retrievedIssueHistoryMain.getID())
                                        .setView(editLayout)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {




                                            }
                                        });
                                AlertDialog alertDialog = materialAlertDialogBuilder.create();
                                TextView transaction_date = editLayout.findViewById(R.id.transaction_date);
                                TextView transaction_time = editLayout.findViewById(R.id.transaction_time);
                                TextView book_number = editLayout.findViewById(R.id.book_number);
                                TextView receiver_name = editLayout.findViewById(R.id.receiver_name);
                                TextView receiver_dept = editLayout.findViewById(R.id.receiver_department);

                                transaction_date.setText(retrievedIssueHistoryMain.getDate());
                                transaction_time.setText(retrievedIssueHistoryMain.getTime());
                                book_number.setText(retrievedIssueHistoryMain.getBookNumber());
                                receiver_name.setText(retrievedIssueHistoryMain.getName());
                                receiver_dept.setText(retrievedIssueHistoryMain.getDepartment());

                                receiver_dept.setSelected(true);
                                receiver_name.setSelected(true);
                                alertDialog.show();


                                FetchIssueHistoryTable fetchIssueHistoryTable = new FetchIssueHistoryTable(new FetchIssueHistoryTaskListener() {
                                    @Override
                                    public void onTaskComplete(JSONArray jsonArray1) {
                                        issueHistoryListview = editLayout.findViewById(R.id.fetchedHistoryListview);
                                        LinearProgressIndicator linearProgressIndicator1 = editLayout.findViewById(R.id.fetch_issue_history_progress);
                                        TextView loading = editLayout.findViewById(R.id.fetch_issue_history_loading);
                                        linearProgressIndicator1.setVisibility(View.GONE);
                                        loading.setVisibility(View.GONE);

                                        List<RetrievedIssueHistory> fetchedOngoingIssueTable = new ArrayList<>();
                                        //load existing ongoing issue
                                        Cursor cursorx = db.rawQuery("SELECT id, productName, productQuantity, productStore, productLocation, productDesc, IssueDate, ReceiverDepartment, productUnit FROM issueHistoryTable",null);
                                        while (cursorx.moveToNext()){

                                            Integer idx = cursorx.getInt(cursorx.getColumnIndexOrThrow("id"));
                                            String namex = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));
                                            String departmentx = cursorx.getString(cursorx.getColumnIndexOrThrow("ReceiverDepartment"));
                                            Integer quantityx = cursorx.getInt(cursorx.getColumnIndexOrThrow("ProductQuantity"));
                                            String storex = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductStore"));
                                            String locationx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductLocation"));
                                            String issueDatex = cursorx.getString(cursorx.getColumnIndexOrThrow("IssueDate"));
                                            String descriptionx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductDesc"));
                                            String unitx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductUnit"));
                                            Log.e("Retrieved",namex);

                                            RetrievedIssueHistory retrievedStock = new RetrievedIssueHistory(idx,namex,descriptionx,quantityx,departmentx,locationx,storex,issueDatex,unitx);
                                            fetchedOngoingIssueTable.add(retrievedStock);

                                        }
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                            }
                                        });

                                        ArrayAdapter<RetrievedIssueHistory> ongoingIssueAdapter = new ArrayAdapter<RetrievedIssueHistory>(getApplicationContext(), R.layout.issue_item_item_list, R.id.product_name, fetchedOngoingIssueTable) {
                                            @Override
                                            public View getView(int position, View convertView, ViewGroup parent) {
                                                View view = super.getView(position, convertView, parent);

                                                TextView store_location = view.findViewById(R.id.store_location);
                                                TextView product_name = view.findViewById(R.id.product_name);
                                                TextView product_quantity = view.findViewById(R.id.product_quantity);
                                                TextView product_location = view.findViewById(R.id.product_location);
                                                RetrievedIssueHistory retrievedStock = getItem(position);
                                                product_name.setText(retrievedStock.getName());
                                                store_location.setText(retrievedStock.getStore());
                                                product_quantity.setText("Qty: "+String.valueOf(retrievedStock.getQuantity()));
                                                product_location.setText(retrievedStock.getLocation());

                                                return view;
                                            }
                                        };
                                        assert issueHistoryListview != null;
                                        issueHistoryListview.setAdapter(ongoingIssueAdapter);
                                        ongoingIssueAdapter.notifyDataSetChanged();

                                    }
                                });
                                fetchIssueHistoryTable.execute();

                            }
                        });


                    }
                });
            }
        },"specified");
        fetchUpdatedIssueHistoryMainTable.execute();
    }

    public void search_history(View view) {

        getUpdatedStockHistory();


    }

    public interface FetchIssueHistoryTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchIssueHistoryTable extends AsyncTask<Void, Void, JSONArray> {
        private IssueHistory.FetchIssueHistoryTaskListener listener;

        public FetchIssueHistoryTable(IssueHistory.FetchIssueHistoryTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected JSONArray doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.issueHistoryTable();
            String sql = null;
            JSONArray jsonArray = null;

            try {
                sql = "{\"type\":\"getIssueHistory\",\"condition\":\"getTransactionDetails\",\"transactionID\":\""+transactionID+"\"}";

                RequestBody requestBody =  RequestBody.create(sql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                        .post(requestBody)
                        .build();
                Response response = okHttpClient.newCall(request).execute();
                String resString = response.body().string();

                jsonArray = new JSONArray(resString);
                response.close();


                try{

                    DBHandler dbHandler = new DBHandler(getApplicationContext());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        int id = jsonObject.getInt("id");
                        String ProductName = jsonObject.getString("ProductName");
                        String ProductDesc = jsonObject.getString("ProductDesc");
                        int ProductQuantity = jsonObject.getInt("ProductQuantity");
                        String ProductStore = jsonObject.getString("ProductStore");
                        String ProductLocation = jsonObject.getString("ProductLocation");
                        String ReceiverDepartment = jsonObject.getString("ReceiverDepartment");
                        String TransactionDate = jsonObject.getString("TransactionDate");
                        String TransactionTime = jsonObject.getString("TransactionTime");
                        String BookNumber = jsonObject.getString("BookNumber");
                        String ProductUnit = jsonObject.getString("ProductUnit");
                        dbHandler.syncIssueHistoryTable(id,ProductName,ProductDesc,ProductQuantity,ProductStore,ProductLocation,ReceiverDepartment,TransactionDate,TransactionTime,BookNumber,ProductUnit);

                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

            } catch (Exception e) {
                Log.e("ISSUE HISTORY SYNC", "Error syncing issue history", e);

            }
            return jsonArray;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            if (listener != null) {
                listener.onTaskComplete(jsonArray);
            }

        }
    }


    public interface FetchIssueHistoryMainTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchIssueHistoryMainTable extends AsyncTask<Void, Void, JSONArray> {
        //fetches default items to stock table on first tap
        private IssueHistory.FetchIssueHistoryMainTaskListener listener;
        private String duration_type;

        public FetchIssueHistoryMainTable(IssueHistory.FetchIssueHistoryMainTaskListener listener, String duration_type) {
            this.listener = listener;
            this.duration_type = duration_type;
        }


        @Override
        protected JSONArray doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.issueHistoryTableMain();
            JSONArray jsonArray = null;
            String sql = null;

            Integer isid = dbHandler1.getIssuerID();
            try {

                if(duration_type.equals("unspecified")){
                    sql = "{\"type\":\"getIssueHistory\",\"condition\":\"getHistory\",\"isid\":\""+isid+"\",\"duration_type\":\"unspecified\"}";
                } else if(duration_type.equals("specified")){

                    sql = "{\"type\":\"getIssueHistory\",\"condition\":\"getHistory\",\"isid\":\""+isid+"\",\"duration_type\":\"specified\",\"start_date\":\""+startDate+"\",\"end_date\":\""+endDate+"\"}";
                }

                RequestBody requestBody =  RequestBody.create(sql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                        .post(requestBody)
                        .build();
                Response response = okHttpClient.newCall(request).execute();
                String resString = response.body().string();

                jsonArray = new JSONArray(resString);
                response.close();


                try{

                    DBHandler dbHandler = new DBHandler(getApplicationContext());
                    for (int i = 0; i < jsonArray.length(); i++) {

                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String status = null;
                        status = jsonObject.optString("status");
                        if(status != null && status.equals("failed")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView tv = findViewById(R.id.fetch_issue_history_textview);
                                    TextView loading = findViewById(R.id.fetch_issue_history_loading);
                                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.fetch_issue_history_progress);
                                    linearProgressIndicator.setVisibility(View.GONE);
                                    loading.setVisibility(View.GONE);
                                    tv.setVisibility(View.VISIBLE);
                                    TextView showing = findViewById(R.id.number_of_results);
                                    showing.setVisibility(View.VISIBLE);
                                }
                            });

                        }

                        int TransactionID = jsonObject.getInt("TransactionID");
                        String ReceiverName = jsonObject.getString("ReceiverName");

                        String ReceiverDepartment = jsonObject.getString("ReceiverDepartment");
                        String TransactionDate = jsonObject.getString("TransactionDate");
                        String TransactionTime = jsonObject.getString("TransactionTime");
                        String BookNumber = jsonObject.getString("BookNumber");
                        dbHandler.syncIssueHistoryTableMain(TransactionID,ReceiverName,ReceiverDepartment,TransactionDate,TransactionTime,BookNumber);

                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

            } catch (Exception e) {
                Log.e("ISSUE HISTORY SYNC", "Error syncing issue history", e);

            }
            return jsonArray;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            if (listener != null) {
                listener.onTaskComplete(jsonArray);
            }

        }
    }

}