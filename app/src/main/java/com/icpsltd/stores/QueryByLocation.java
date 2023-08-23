package com.icpsltd.stores;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.tech.NfcBarcode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QueryByLocation extends AppCompatActivity {

    private String URL;
    private String USER;
    private String PASSWORD;
    private List<RetrievedStore> fetchedStoreTable;
    private List<RetrievedLocation> fetchedLocationTable;
    private ArrayAdapter<RetrievedStore> arrayAdapter;
    private ArrayAdapter<RetrievedLocation> arrayAdapter2;
    AppCompatSpinner spinner;
    AppCompatSpinner spinner2;

    private ArrayAdapter<RetrievedStock> arrayAdapter3;
    private List<RetrievedStock> fetchedStockTable;
    private ListView listView;
    private String selectedStoreID;
    private String selectedLocationID;
    private MaterialAutoCompleteTextView materialAutoCompleteTextView;
    private LinearProgressIndicator linearProgressIndicator;

    private TextView loadingText;
    private LinearProgressIndicator loadingBar;

    private String locationIDQR;
    private String storeIDQR;

    private TextView showing_results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_by_location);
        spinner = findViewById(R.id.spinner);
        spinner2 = findViewById(R.id.spinner2);
        listView = findViewById(R.id.location_listview);
        linearProgressIndicator = findViewById(R.id.progressBar);
        loadingBar = findViewById(R.id.fetch_progress);
        loadingText = findViewById(R.id.loading);
        showing_results = findViewById(R.id.showing_results);

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


        try {
            locationIDQR = getIntent().getStringExtra("locationid");
            storeIDQR = getIntent().getStringExtra("storeid");
            if (locationIDQR != null && storeIDQR != null) {
                selectedLocationID = locationIDQR;
                selectedStoreID = storeIDQR;

                View view = new View(getApplicationContext());
                search(view);
                getStoreQR();

            } else{
                getStore();
            }
        } catch (Exception e) {

        }


        
        

        
        
    }

    public void getStoreQR(){
        linearProgressIndicator.setVisibility(View.VISIBLE);
        FetchStoreTable fetchStoreTable = new FetchStoreTable(new FetchStoreTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


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


                arrayAdapter = new ArrayAdapter<RetrievedStore>(getApplicationContext(),R.layout.location_list_layout,R.id.store_name, fetchedStoreTable){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);

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

                for (int i = 0; i < arrayAdapter.getCount(); i++) {
                    RetrievedStore currentItem = arrayAdapter.getItem(i);
                    String storeID = currentItem.getId().toString();
                    if(storeID.equals(storeIDQR)){
                        spinner.setSelection(i);
                        break;
                    }
                }


                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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


                        arrayAdapter2 = new ArrayAdapter<RetrievedLocation>(getApplicationContext(),R.layout.location_list_layout_location,R.id.location_name, fetchedLocationTable){
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);

                                RetrievedLocation retrievedLocation = (RetrievedLocation) getItem(position);
                                TextView location_name = view.findViewById(R.id.location_name);
                                ImageView store_icon = view.findViewById(R.id.store_icon);
                                selectedLocationID = String.valueOf(retrievedLocation.getId());



                                location_name.setText(retrievedLocation.getLocationName());
                                if(retrievedLocation.getStoreID().equals(2)){
                                    store_icon.setImageResource(R.drawable.one);
                                } else if (retrievedLocation.getStoreID().equals(3)) {
                                    store_icon.setImageResource(R.drawable.two);
                                } else if (retrievedLocation.getStoreID().equals(4)) {
                                    store_icon.setImageResource(R.drawable.three);
                                } else if (retrievedLocation.getStoreID().equals(1)) {
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
                        for (int i = 0; i < arrayAdapter2.getCount(); i++) {
                            RetrievedLocation currentItem = arrayAdapter2.getItem(i);
                            String locationID = currentItem.getId().toString();
                            if(locationID.equals(locationIDQR)){
                                spinner2.setSelection(i);
                                break;
                            }
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                arrayAdapter.notifyDataSetChanged();
            }
        });
        fetchStoreTable.execute();
    }

    public void getStore(){
        linearProgressIndicator.setVisibility(View.VISIBLE);
        FetchStoreTable fetchStoreTable = new FetchStoreTable(new FetchStoreTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


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


                arrayAdapter = new ArrayAdapter<RetrievedStore>(getApplicationContext(),R.layout.location_list_layout,R.id.store_name, fetchedStoreTable){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);

                        //RetrievedStore retrievedStore1 = getItem(position);
                        RetrievedStore retrievedStore1 = (RetrievedStore) getItem(position);
                        TextView store_name = view.findViewById(R.id.store_name);
                        TextView store_floor = view.findViewById(R.id.store_floor);
                        ImageView store_icon = view.findViewById(R.id.store_icon);

                        //Toast.makeText(QueryByLocation.this, "Selected ID:"+String.valueOf(retrievedStore1.getId()), Toast.LENGTH_SHORT).show();




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

                                RetrievedLocation retrievedLocation = (RetrievedLocation) getItem(position);
                                TextView location_name = view.findViewById(R.id.location_name);
                                ImageView store_icon = view.findViewById(R.id.store_icon);
                                selectedLocationID = String.valueOf(retrievedLocation.getId());

                                //Toast.makeText(QueryByLocation.this, "Selected ID:"+String.valueOf(retrievedStore1.getId()), Toast.LENGTH_SHORT).show();




                                location_name.setText(retrievedLocation.getLocationName());
                                if(retrievedLocation.getStoreID().equals(2)){
                                    store_icon.setImageResource(R.drawable.one);
                                } else if (retrievedLocation.getStoreID().equals(3)) {
                                    store_icon.setImageResource(R.drawable.two);
                                } else if (retrievedLocation.getStoreID().equals(4)) {
                                    store_icon.setImageResource(R.drawable.three);
                                } else if (retrievedLocation.getStoreID().equals(1)) {
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

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                arrayAdapter.notifyDataSetChanged();
            }
        });
        fetchStoreTable.execute();
    }

    public void startQR(View view) {
        String classname = QueryByLocation.class.getName().toString();
        Intent intent = new Intent(QueryByLocation.this, ScanQR.class);
        intent.putExtra("class", classname);
        startActivity(intent);
    }

    public void search(View view) {
        loadingBar.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("Searching...");
        showing_results.setVisibility(View.INVISIBLE);
        if(arrayAdapter3 != null){
        arrayAdapter3.clear();
        arrayAdapter3.notifyDataSetChanged();
        }
        FetchStockTable fetchStockTable = new FetchStockTable(new FetchStockTaskListener() {
            @Override
            public void onTaskComplete(Connection connection) {
                loadingBar.setVisibility(View.GONE);
                loadingText.setVisibility(View.GONE);

                fetchedStockTable = new ArrayList<>();
                DBHandler dbHandler = new DBHandler(getApplicationContext());
                SQLiteDatabase db = dbHandler.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit FROM stockTable",null);
                if(cursor.getCount() == 0){
                    loadingText.setVisibility(View.VISIBLE);
                    loadingText.setText("No Results Found");
                }
                while (cursor.moveToNext()){
                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                    Integer quantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
                    String store = cursor.getString(cursor.getColumnIndexOrThrow("ProductStore"));
                    String location = cursor.getString(cursor.getColumnIndexOrThrow("ProductLocation"));
                    String description = cursor.getString(cursor.getColumnIndexOrThrow("ProductDesc"));
                    String unit = cursor.getString(cursor.getColumnIndexOrThrow("ProductUnit"));

                    RetrievedStock retrievedStock = new RetrievedStock(id,name,description,quantity,store,location,unit);
                    fetchedStockTable.add(retrievedStock);

                }
                showing_results.setVisibility(View.VISIBLE);
                if(fetchedStockTable.size() == 1){
                    showing_results.setText("Showing "+fetchedStockTable.size()+" result");
                } else {
                showing_results.setText("Showing "+fetchedStockTable.size()+" results");
                }
                arrayAdapter3 = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedStockTable) {
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
                listView.setAdapter(arrayAdapter3);
                arrayAdapter3.notifyDataSetChanged();


            }
        });
        fetchStockTable.execute();
    }


    public interface FetchStoreTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStoreTable extends AsyncTask<Void, Void, Connection> {
        private QueryByLocation.FetchStoreTaskListener listener;

        public FetchStoreTable(QueryByLocation.FetchStoreTaskListener listener) {
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


    public interface FetchStockTaskListener {
        void onTaskComplete(Connection connection);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStockTable extends AsyncTask<Void, Void, Connection> {
        private QueryByLocation.FetchStockTaskListener listener;

        public FetchStockTable(QueryByLocation.FetchStockTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected Connection doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "";

                if(selectedLocationID.substring(1).equals("000")){
                    sql = "SELECT * FROM stockTable WHERE storeID = '"+selectedStoreID+"'";
                } else {
                    sql = "SELECT * FROM stockTable WHERE locationID = '"+selectedLocationID+"'";
                }

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
}