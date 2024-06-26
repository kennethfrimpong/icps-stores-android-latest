package com.icpsltd.stores.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.icpsltd.stores.R;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.utils.TokenChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

public class ScanQR extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;

    private PreviewView previewView;

    private Boolean isScanned = false;

    private Boolean isLocation = false;
    private Boolean isICPS = false;

    private Boolean isProduct = false;

    private String classname;

    private String moveDate;
    private String moveTime;

    private Class<?> clazz;

    Handler handler = new Handler();
    Runnable runnable;

    private String func;
    private TextView tv;
    private static final String TAG = "Barcode Scanner";

    private String toLocationID;
    private String toLocationName;
    private String toStoreName;
    private String toStoreID;

    private String fromShelf;
    private String fromLevel;
    private String fromSpace;
    private String fromStore;

    private String address;

    private String fromLocationID;
    private String fromLocationName;
    private String fromStoreName;
    private String fromStoreID;

    private String itemCode;

    private String productName;

    private OkHttpClient okHttpClient;

    private String apiHost;
    private String apiPort;

    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);
        func="empty";

        previewView = findViewById(R.id.previewView);

        SwitchCompat switchCompat = findViewById(R.id.flashSwitch);
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector);
            if(camera.getCameraInfo().hasFlashUnit()) {
                camera.getCameraControl().enableTorch(isChecked);
            }

        });
        classname = getIntent().getStringExtra("class");
        try {
            clazz = Class.forName(classname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try{
            func = getIntent().getStringExtra("function");
            fromShelf = getIntent().getStringExtra("fromShelf");
            fromStore = getIntent().getStringExtra("fromStore");
            fromLevel = getIntent().getStringExtra("fromLevel");
            fromSpace = getIntent().getStringExtra("fromSpace");
            itemCode = getIntent().getStringExtra("itemCode");
            productName = getIntent().getStringExtra("productName");

            tv = findViewById(R.id.scan_textview);
            if(func != null){
                if(func.equals("MoveQR")){
                    tv.setText("Scan Destination QR");


                }
            }
        } catch (Exception e){
            Log.e("Exception", String.valueOf(e));
        }

        DBHandler dbHandler = new DBHandler(getApplicationContext());
        apiHost = dbHandler.getApiHost();
        apiPort = dbHandler.getApiPort();


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
                        runOnUiThread(()->{ Toast.makeText(ScanQR.this, "SSL certificate hostname mismatch", Toast.LENGTH_SHORT).show(); });

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

    @Override
    protected void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(runnable,5000);
                isLocation = false;
                isICPS = false;
                isProduct = false;
            }
        }, 5000);

        super.onResume();
        startCamera();
        isScanned = false;

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cameraProvider.unbindAll();
        finish();

        Intent intent = new Intent(ScanQR.this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraProvider.unbindAll();
        finish();
    }

    public void startCamera() {
        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            getPermissions();
        }
    }

    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        for (int code : grantResults) {
            if (code == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                getPermissions();
                return;
            }
        }

        if (requestCode == PERMISSION_CODE) {
            setupCamera();
            startCamera();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        int lensFacing = CameraSelector.LENS_FACING_BACK;
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "cameraProviderFuture.addListener Error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        builder.setTargetRotation(getRotation());

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind preview", e);
        }
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setTargetRotation(getRotation());

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try {
            cameraProvider
                    .bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind analysis", e);
        }
    }

    protected int getRotation() throws NullPointerException {
        previewView = findViewById(R.id.previewView);
        if (previewView != null && previewView.getDisplay() != null) {
            return previewView.getDisplay().getRotation();
        } else {

            return 0;
        }
    }



    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {


        if (image.getImage() == null) return;

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );


        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE)
                .build();


        BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);
        final int[] value = {1};
        Intent intent = new Intent(this, clazz);


        barcodeScanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {


                        if (barcodes.size() > 0 && !isScanned) {
                            Barcode barcode = barcodes.get(0);
                            String rawValue = barcode.getRawValue();
                            JSONObject json = null;
                            String locationid = null;
                            String storeid = null;
                            String item_code = null;
                            MyPrefs myPrefs = new MyPrefs();

                            try {
                                json = new JSONObject(rawValue);

                                if(json.optString("projectid").equals("9015")){

                                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                                    Future<String> futureResult = executorService.submit(() -> {
                                        //OkHttpClient okHttpClient = new OkHttpClient();
                                        try{
                                            Request request = new Request.Builder()
                                                    .url("https://"+apiHost+":"+apiPort+"/api/v1/tst/getDateTime")
                                                    .addHeader("Authorization",myPrefs.getToken(getApplicationContext()))
                                                    //.post(requestBody)
                                                    .build();
                                            Response response = okHttpClient.newCall(request).execute();
                                            String resString = response.body().string();

                                            JSONObject jsonObject3 = new JSONObject(resString);

                                            String status1 = jsonObject3.optString("status");
                                            resString = jsonObject3.optString("data");

                                            TokenChecker tokenChecker = new TokenChecker();
                                            tokenChecker.checkToken(status1, getApplicationContext(), ScanQR.this);

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
                                        moveDate = jsonObject.optString("date");
                                        moveTime = jsonObject.optString("time");

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }




                                    //valid icps qr code

                                    if (classname.equals("com.icpsltd.stores.activities.QueryByLocation")){

                                        if(json.optString("scanid").equals("002")){
                                            address = json.optString("address");
                                            String[] addressArray = address.split("-");
                                            String store = addressArray[0];
                                            store = store.trim();
                                            store = store.replace("_"," ");


                                            String shelf = addressArray[1];
                                            shelf = shelf.trim();
                                            String level = addressArray[2];
                                            level = level.trim();
                                            String space = addressArray[3];
                                            space = space.replace(" ","");

                                            String shelfNumber = shelf.substring(5);
                                            String levelNumber = level.substring(5);
                                            String spaceNumber = space.substring(5);

                                            //locationid = json.optString("locationid");
                                            //storeid = json.optString("storeid");
                                            Toast.makeText(ScanQR.this, "Space : "+spaceNumber+", Level : "+levelNumber+", Shelf : "+shelfNumber, Toast.LENGTH_SHORT).show();
                                            image.close();
                                            barcodeScanner.close();
                                            cameraProvider.unbindAll();
                                            finish();
                                            intent.putExtra("store", store);
                                            intent.putExtra("shelf", shelfNumber);
                                            intent.putExtra("level", levelNumber);
                                            intent.putExtra("space", spaceNumber);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                            isScanned = true;
                                        } else {
                                            if(!isLocation){
                                                Toast.makeText(ScanQR.this, "Find a valid Location QR", Toast.LENGTH_SHORT).show();
                                                isLocation = true;
                                            }
                                        }

                                    } else if (classname.equals("com.icpsltd.stores.activities.QueryByProduct")) {


                                        if(json.optString("scanid").equals("001") && func != null && func.equals("ScanProduct")){

                                            item_code = json.optString("item_code");
                                            image.close();
                                            barcodeScanner.close();
                                            cameraProvider.unbindAll();
                                            finish();
                                            intent.putExtra("qrCode", item_code);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                            isScanned = true;


                                        } else if (json.optString("scanid").equals("002") && func != null && func.equals("MoveQR")) {



                                            address = json.optString("address");
                                            String[] addressArray = address.split("-");
                                            String store = addressArray[0];
                                            store = store.trim();
                                            store = store.replace("_"," ");
                                            String shelf = addressArray[1];
                                            shelf = shelf.trim();
                                            String level = addressArray[2];
                                            level = level.trim();
                                            String space = addressArray[3];
                                            space = space.replace(" ","");

                                            String shelfNumber = shelf.substring(5);
                                            String levelNumber = level.substring(5);
                                            String spaceNumber = space.substring(5);

                                            //toStoreID = json.optString("storeid");
                                            //toStoreName = json.optString("store");
                                            //toLocationID = json.optString("locationid");
                                            toStoreName = store;
                                            toLocationName = "Space "+spaceNumber+" of Level "+levelNumber+" in Shelf "+shelfNumber;

                                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(ScanQR.this);
                                            materialAlertDialogBuilder.setTitle("Move "+productName);
                                            materialAlertDialogBuilder.setMessage("Are you sure you want to move "+productName+" to " + toLocationName + " at " + toStoreName + "?");
                                            isScanned=true;
                                            String finalStore = store;
                                            String finalShelf = shelfNumber;
                                            String finalLevel = levelNumber;
                                            String finalSpace = spaceNumber;
                                            materialAlertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                    image.close();
                                                    barcodeScanner.close();
                                                    cameraProvider.unbindAll();
                                                    finish();

                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    intent.putExtra("function", "ReturnQR");

                                                    intent.putExtra("toShelf", finalShelf);
                                                    intent.putExtra("toStore", finalStore);
                                                    intent.putExtra("toLevel", finalLevel);
                                                    intent.putExtra("toSpace", finalSpace);

                                                    intent.putExtra("fromShelf", fromShelf);
                                                    intent.putExtra("fromStore", fromStore);
                                                    intent.putExtra("fromLevel", fromLevel);
                                                    intent.putExtra("fromSpace", fromSpace);

                                                    intent.putExtra("productName",productName);

                                                    intent.putExtra("moveDate",moveDate);
                                                    intent.putExtra("moveTime",moveTime);
                                                    intent.putExtra("itemCode",itemCode);
                                                    Log.d("itemCode1",itemCode);

                                                    alertDialog.dismiss();
                                                    startActivity(intent);
                                                    isScanned = true;


                                                }
                                            });
                                            materialAlertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    isScanned = false;
                                                }
                                            });

                                            alertDialog = materialAlertDialogBuilder.create();
                                            alertDialog.show();


                                        } else {
                                            if(!isProduct){
                                                if (func.equals("MoveQR")){
                                                    Toast.makeText(ScanQR.this, "Find a valid Destination QR", Toast.LENGTH_SHORT).show();

                                                } else {
                                                    Toast.makeText(ScanQR.this, "Find a valid Product QR", Toast.LENGTH_SHORT).show();

                                                }
                                                isProduct = true;
                                            }
                                        }


                                    } else {
                                        //how did you get here ?


                                    }

                                }
                            } catch (JSONException e) {
                                if (!isICPS){
                                    Toast.makeText(ScanQR.this, "Find a valid ICPS Stores QR", Toast.LENGTH_SHORT).show();
                                    isICPS = true;
                                }
                                //throw new RuntimeException(e);
                            }


                        } else {

                        }



                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        image.close();

                    }
                });
    }


}