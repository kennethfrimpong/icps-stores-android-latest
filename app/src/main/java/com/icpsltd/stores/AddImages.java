package com.icpsltd.stores;

import static com.icpsltd.stores.activities.NewIssue.FETCH_DELAY_TIME;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.icpsltd.stores.activities.NewIssue;
import com.icpsltd.stores.activities.QueryByProduct;
import com.icpsltd.stores.adapterclasses.RetrievedStock;
import com.icpsltd.stores.utils.DBHandler;
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
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

public class AddImages extends AppCompatActivity {
    private String base64Image = "";
    private boolean isImageCaptured = false;
    private ImageView thumbnail;

    private ConstraintLayout actionsContainer;
    private TextView stock_name;
    private TextView imageAvailable;
    private TextView searchHelperText;

    List<RetrievedStock> fetchedStock;

    private MaterialAutoCompleteTextView productCode;
    private OkHttpClient okHttpClient;

    private Handler sHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_images);
        DBHandler dbHandler = new DBHandler(this);
        thumbnail = findViewById(R.id.thumbnail);
        actionsContainer = findViewById(R.id.actionsContainer);
        stock_name = findViewById(R.id.stock_name);
        imageAvailable = findViewById(R.id.imageAvailable);
        searchHelperText = findViewById(R.id.searchHelperText);

        productCode = findViewById(R.id.item_code_input);

        sHandler = new Handler();

        TextView asname = findViewById(R.id.firstLastName);
        asname.setText("as "+dbHandler.getFirstName()+" "+dbHandler.getLastName());

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
                        runOnUiThread(()->{ Toast.makeText(AddImages.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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
        fetchProducts();

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
                        fetchProducts();
                    }
                }, FETCH_DELAY_TIME);
                //fetchProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {
                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.productFetchProgress);
                linearProgressIndicator.setVisibility(View.VISIBLE);
                if (productCode.getText().toString().equals("")){
                    linearProgressIndicator.setVisibility(View.GONE);
                }

            }
        };

        productCode.addTextChangedListener(textWatcher);
    }

    private void fetchProducts(){

        AddImages.FetchStockTable fetchStockTable = new AddImages.FetchStockTable(new AddImages.FetchStockTaskListener() {
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
                                RetrievedStock retrievedStock1 = getItem(position);
                                productCode.setText(retrievedStock1.getID());
                                searchHelperText.setVisibility(View.GONE);
                                stock_name.setVisibility(View.VISIBLE);
                                imageAvailable.setVisibility(View.VISIBLE);
                                stock_name.setText(retrievedStock1.getName());
                                MaterialButton uploadButton = findViewById(R.id.uploadButton);

                                if(retrievedStock1.getImageAvailable().equals("1")){
                                    imageAvailable.setText("Image Available");
                                    imageAvailable.setTextColor(Color.parseColor("#33691E"));
                                    uploadButton.setText("Update");
                                    try {
                                        getImage();
                                    } catch (GeneralSecurityException e) {
                                        throw new RuntimeException(e);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else if (retrievedStock1.getImageAvailable().equals("0")) {
                                    imageAvailable.setText("No Image Available");
                                    imageAvailable.setTextColor(Color.parseColor("#E30613"));
                                    uploadButton.setText("Upload");
                                } else {
                                    imageAvailable.setText("Could not determine image availability");
                                    imageAvailable.setTextColor(Color.parseColor("#FFC107"));
                                    uploadButton.setText("Upload");
                                }

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

    private void getImage() throws GeneralSecurityException, IOException {
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        String enteredItemCode = productCode.getText().toString()+".jpg";
        MyPrefs myPrefs = new MyPrefs();
        Request request = new Request.Builder()
                .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/content/stock/"+enteredItemCode)
                .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                .get()
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //receive .jpg returned by server
                    Response response = okHttpClient.newCall(request).execute();
                    Log.d("Response",response.toString());
                    assert response.body() != null;
                    Log.d("Response",response.body().byteStream().toString());

                    InputStream inputStream = response.body().byteStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            thumbnail.setImageBitmap(bitmap);
                            thumbnail.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    displayThumbnail(bitmap);
                                }
                            });
                        }
                    });

                    response.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            thumbnail.setImageResource(R.drawable.no_img);
                        }
                    });
                }
            }
        }).start();
    }

    private void uploadImage() throws GeneralSecurityException, IOException {
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        String enteredItemCode = productCode.getText().toString()+".jpg";
        MyPrefs myPrefs = new MyPrefs();
        String body = "{\"base64Image\":\""+base64Image+"\"}";
        RequestBody requestBody =  RequestBody.create(body, okhttp3.MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://"+ dbHandler.getApiHost()+":"+dbHandler.getApiPort()+"/api/v1/upload/stock/"+enteredItemCode)
                .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                .post(requestBody)
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //receive .jpg returned by server
                    Response response = okHttpClient.newCall(request).execute();
                    String resString = response.body().string();
                    JSONObject jsonObject = new JSONObject(resString);
                    String status = jsonObject.optString("status");
                    Log.d("Response",resString.toString());
                    MaterialButton uploadButton = findViewById(R.id.uploadButton);
                    if (status.equals("success")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddImages.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                                imageAvailable.setText("Image Available");
                                imageAvailable.setTextColor(Color.parseColor("#33691E"));
                                uploadButton.setText("Update");
                                base64Image = "";
                                //MaterialButton uploadButton = findViewById(R.id.upload)
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddImages.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    response.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AddImages.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public void goToCamera(View view) {
        // Launch the camera activity
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File jpgImage = new File(getFilesDir(),"image.jpg");
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", jpgImage));
        startActivityForResult(cameraIntent, 1);

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

                MaterialButton removeImage = findViewById(R.id.removeImage);
                MaterialButton previewImage = findViewById(R.id.previewImage);
                MaterialButton retakeImage = findViewById(R.id.retakeImage);
                actionsContainer.setVisibility(View.VISIBLE);
                searchHelperText.setVisibility(View.GONE);
                stock_name.setVisibility(View.VISIBLE);
                removeImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        MaterialButton removeImage = findViewById(R.id.removeImage);
                        actionsContainer.setVisibility(View.GONE);
                        stock_name.setVisibility(View.GONE);
                        searchHelperText.setVisibility(View.VISIBLE);
                        base64Image = "";
                        isImageCaptured = false;
                        thumbnail.setImageBitmap(null);

                        thumbnail.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                    }
                });

                retakeImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View view = new View(AddImages.this);
                        goToCamera(view);
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




        }
    }

    private void displayThumbnail(Bitmap bitmap){
        //open dialog to view image
        Dialog dialog = new Dialog(AddImages.this);
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

    public void upload(View view) {
        if (isImageCaptured && !base64Image.equals("")){
            try {
                uploadImage();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
        }
    }

    public interface FetchStockTaskListener {
        void onTaskComplete(JSONArray jsonArray);
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchStockTable extends AsyncTask<Void, Void, JSONArray> {
        //fetches default items to stock table on first tap
        private AddImages.FetchStockTaskListener listener;

        public FetchStockTable(AddImages.FetchStockTaskListener listener) {
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
                String enteredItemCode = productCode.getText().toString();
                MyPrefs myPrefs = new MyPrefs();
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
                tokenChecker.checkToken(status1, getApplicationContext(), AddImages.this);

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
                            //move_item.setVisibility(View.GONE);
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
            //Log.d("Fetch Complete", retrieved_location_id+" - "+retrieved_store_id);

        }
    }
}