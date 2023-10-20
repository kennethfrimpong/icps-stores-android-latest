package com.icpsltd.stores.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.R;
import com.icpsltd.stores.adapterclasses.RetrievedLocation;
import com.icpsltd.stores.adapterclasses.RetrievedStock;
import com.icpsltd.stores.adapterclasses.RetrievedStore;
import com.icpsltd.stores.utils.StockTable;

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

public class QueryByLocation extends AppCompatActivity {

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

    private OkHttpClient okHttpClient;

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
                        runOnUiThread(()->{ Toast.makeText(QueryByLocation.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

    public void getStoreQR(){
        linearProgressIndicator.setVisibility(View.VISIBLE);
        FetchStoreTable fetchStoreTable = new FetchStoreTable(new FetchStoreTaskListener() {
            @Override
            public void onTaskComplete(JSONArray jsonArray) {

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
            public void onTaskComplete(JSONArray jsonArray) {

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
            public void onTaskComplete(JSONArray jsonArray) {
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
                    String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
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
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStoreTable extends AsyncTask<Void, Void, JSONArray> {
        private QueryByLocation.FetchStoreTaskListener listener;

        public FetchStoreTable(QueryByLocation.FetchStoreTaskListener listener) {
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
                        .post(requestBody)
                        .build();
                Response response = okHttpClient.newCall(request).execute();
                String resString = response.body().string();

                jsonArray1 = new JSONArray(resString);
                response.close();


               RequestBody requestBody1 =  RequestBody.create(locationsql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
               Request request1 = new Request.Builder()
                        .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                        .post(requestBody1)
                        .build();
                Response response1 = okHttpClient.newCall(request1).execute();
                String resString1 = response1.body().string();

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


    public interface FetchStockTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStockTable extends AsyncTask<Void, Void, JSONArray> {
        private QueryByLocation.FetchStockTaskListener listener;

        public FetchStockTable(QueryByLocation.FetchStockTaskListener listener) {
            this.listener = listener;
        }


        @Override
        protected JSONArray doInBackground(Void... voids) {
            DBHandler dbHandler1 = new DBHandler(getApplicationContext());
            dbHandler1.clearStockTable();
            JSONArray jsonArray;
            try {
                String queryType = "";
                if(selectedLocationID.substring(1).equals("000")){
                    queryType = "allStore";
                } else {
                    queryType = "specificLocation";

                }

                String sql = "{\"type\":\"queryByLocation\",\"condition\":\"getStock\",\"queryType\":\""+queryType+"\",\"selectedStoreID\":\""+selectedStoreID+"\",\"selectedLocationID\":\""+selectedLocationID+"\"}";

                RequestBody requestBody =  RequestBody.create(sql, okhttp3.MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://"+ dbHandler1.getApiHost()+":"+dbHandler1.getApiPort()+"/api/v1/fetch")
                        .post(requestBody)
                        .build();
                Response response = okHttpClient.newCall(request).execute();
                String resString = response.body().string();

                jsonArray = new JSONArray(resString);
                response.close();

                for (int i = 0; i < jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String itemCode = jsonObject.optString("itemCode");
                    String productName = jsonObject.optString("productName");
                    String productDesc = jsonObject.optString("productDesc");
                    String productStore = jsonObject.optString("productStore");
                    String productLocation = jsonObject.optString("productLocation");
                    String productUnit = jsonObject.optString("productUnit");
                    int productQuantity = jsonObject.optInt("productQuantity");
                    dbHandler1.syncStockTable(itemCode,productName,productDesc,productQuantity,productStore,productLocation,productUnit);
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

        }
    }
}