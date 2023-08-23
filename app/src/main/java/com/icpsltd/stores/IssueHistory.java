package com.icpsltd.stores;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IssueHistory extends AppCompatActivity {

    private List<RetrievedIssueHistoryMain> fetchedIssueHistoryTable;
    private ArrayAdapter<RetrievedIssueHistoryMain> adapter;
    private String URL;
    private String USER;
    private String PASSWORD;
    private String startDate;
    private String endDate;

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
        URL = "jdbc:mysql://"+dbHost+":3306/"+dbName;
        Log.i("URL",URL);
        USER = dbUser;
        PASSWORD = dbPass;
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

        //fetch last 10 issues
        getIssueHistoryMain();

    }

    public void getIssueHistoryMain(){
        ListView listView = findViewById(R.id.issue_history_listview);
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        FetchIssueHistoryMainTable fetchIssueHistoryMainTable = new FetchIssueHistoryMainTable(new FetchIssueHistoryMainTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {

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
                                    public void onTaskComplete(Connection connection) {
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
        });
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

        FetchUpdatedIssueHistoryMainTable fetchUpdatedIssueHistoryMainTable = new FetchUpdatedIssueHistoryMainTable(new FetchUpdatedIssueHistoryMainTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {
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
                                alertDialog.show();


                                FetchIssueHistoryTable fetchIssueHistoryTable = new FetchIssueHistoryTable(new FetchIssueHistoryTaskListener() {
                                    @Override
                                    public void onTaskComplete(Connection connection) {
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
        });
        fetchUpdatedIssueHistoryMainTable.execute();
    }

    public void search_history(View view) {

        getUpdatedStockHistory();


    }

    public interface FetchIssueHistoryTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchIssueHistoryTable extends AsyncTask<Void, Void, Connection> {
        //fetches default items to stock table on first tap
        private IssueHistory.FetchIssueHistoryTaskListener listener;

        public FetchIssueHistoryTable(IssueHistory.FetchIssueHistoryTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.issueHistoryTable();

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT id, ProductName, ProductDesc, ProductQuantity, ProductStore, ProductLocation, ReceiverDepartment, TransactionDate, TransactionTime, BookNumber, ProductUnit  FROM issueHistoryTable WHERE TransactionID = '"+transactionID+"' ORDER BY TransactionNumber DESC";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                if (resultSet.next()){

                    resultSet.previous();
                    while (resultSet.next()){
                        Log.i("StockTable","Row fetched successfully");
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        dbHandler.syncIssueHistoryTable(resultSet.getInt("id"),resultSet.getString("ProductName"),resultSet.getString("ProductDesc"),resultSet.getInt("ProductQuantity"),resultSet.getString("ProductStore"),resultSet.getString("ProductLocation"),resultSet.getString("ReceiverDepartment"),resultSet.getString("TransactionDate"),resultSet.getString("TransactionTime"),resultSet.getString("BookNumber"),resultSet.getString("ProductUnit"));

                    }

                } else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            /**
                            TextView tv = findViewById(R.id.fetch_issue_history_textview);
                            TextView loading = findViewById(R.id.fetch_issue_history_loading);
                            LinearProgressIndicator linearProgressIndicator = findViewById(R.id.fetch_issue_history_progress);
                            tv.setVisibility(View.GONE);
                            linearProgressIndicator.setVisibility(View.GONE);
                            //tv.setVisibility(View.VISIBLE);
                            TextView showing = findViewById(R.id.number_of_results);
                            showing.setVisibility(View.VISIBLE);
                             **/
                        }
                    });

                }

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("ISSUE HISTORY SYNC", "Error syncing issue history", e);

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


    public interface FetchIssueHistoryMainTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchIssueHistoryMainTable extends AsyncTask<Void, Void, Connection> {
        //fetches default items to stock table on first tap
        private IssueHistory.FetchIssueHistoryMainTaskListener listener;

        public FetchIssueHistoryMainTable(IssueHistory.FetchIssueHistoryMainTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.issueHistoryTableMain();

            Integer isid = dbHandler1.getIssuerID();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT DISTINCT TransactionID, MAX(ReceiverName) ReceiverName,MAX(ReceiverDepartment) ReceiverDepartment,MAX(TransactionDate) TransactionDate,MAX(TransactionTime) TransactionTime,MAX(BookNumber) BookNumber FROM issueHistoryTable WHERE issuerID ='"+isid+"' GROUP BY TransactionID ORDER BY MAX(TransactionNumber) DESC, TransactionID LIMIT 10";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                if (resultSet.next()){
                    TextView tv = findViewById(R.id.fetch_issue_history_textview);
                    tv.setVisibility(View.GONE);

                    resultSet.previous();
                    while (resultSet.next()){
                        Log.i("MainTransactionHistory","Row fetched successfully");
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        dbHandler.syncIssueHistoryTableMain(resultSet.getInt("TransactionID"),resultSet.getString("ReceiverName"),resultSet.getString("ReceiverDepartment"),resultSet.getString("TransactionDate"),resultSet.getString("TransactionTime"),resultSet.getString("BookNumber"));
                    }

                } else{
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

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("ISSUE HISTORY SYNC", "Error syncing issue history", e);

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

    public interface FetchUpdatedIssueHistoryMainTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchUpdatedIssueHistoryMainTable extends AsyncTask<Void, Void, Connection> {
        //fetches default items to stock table on first tap
        private IssueHistory.FetchUpdatedIssueHistoryMainTaskListener listener;

        public FetchUpdatedIssueHistoryMainTable(IssueHistory.FetchUpdatedIssueHistoryMainTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.issueHistoryTableMain();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });

            Integer isid = dbHandler1.getIssuerID();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT DISTINCT TransactionID, MAX(ReceiverName) ReceiverName,MAX(ReceiverDepartment) ReceiverDepartment,MAX(TransactionDate) TransactionDate,MAX(TransactionTime) TransactionTime,MAX(BookNumber) BookNumber FROM issueHistoryTable WHERE issuerID ='"+isid+"' AND TransactionDate BETWEEN '"+startDate+"' AND '"+endDate+"' GROUP BY TransactionID ORDER BY MAX(TransactionNumber) ASC, TransactionID";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);
                Log.i("RESULT SET",""+sql);
                if (resultSet.next()){
                    TextView tv = findViewById(R.id.fetch_issue_history_textview);
                    tv.setVisibility(View.GONE);

                    resultSet.previous();
                    while (resultSet.next()){
                        Log.i("MainTransactionHistory","Row fetched successfully");
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        dbHandler.syncIssueHistoryTableMain(resultSet.getInt("TransactionID"),resultSet.getString("ReceiverName"),resultSet.getString("ReceiverDepartment"),resultSet.getString("TransactionDate"),resultSet.getString("TransactionTime"),resultSet.getString("BookNumber"));
                    }

                } else{
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

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                Log.e("ISSUE HISTORY SYNC", "Error syncing issue history", e);

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