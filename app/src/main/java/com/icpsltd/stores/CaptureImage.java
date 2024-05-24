package com.icpsltd.stores;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
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
import com.icpsltd.stores.activities.NewIssue;
import com.icpsltd.stores.activities.ScanQR;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.utils.TokenChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Request;
import okhttp3.Response;

public class CaptureImage extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;

    private PreviewView previewView;

    private ImageCapture imageCapture;

    private static final String TAG = "Image Capture";

    private ImageView captureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_image);
        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);

        captureButton.setOnClickListener(v -> captureImage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(cameraProvider != null){
            cameraProvider.unbindAll();
        }
        finish();

        Intent intent = new Intent(CaptureImage.this, NewIssue.class);
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

    }

    //method to capture image when capture button is clicked
    private void captureImage() {
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(getRotation())
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);

        String pathname = getFilesDir() + "/image.jpg";

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(new File(pathname)).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                cameraProvider.unbindAll();
                finish();
                Toast.makeText(CaptureImage.this, pathname, Toast.LENGTH_SHORT).show();

                //compress image to reduce size



                boolean compressStatus = false;
                String base64Image = "";
                try {
                    compressStatus = compressImage(pathname);
                } catch (IOException e) {
                    Toast.makeText(CaptureImage.this, "Error compressing image", Toast.LENGTH_SHORT).show();
                }
                if(!compressStatus){
                    Toast.makeText(CaptureImage.this, "Error Compressing image", Toast.LENGTH_SHORT).show();
                } else{
                    base64Image = Base64.getEncoder().encodeToString(pathname.getBytes());
                    File file = new File(pathname);
                    file.delete();
                }


                Intent intent = new Intent(CaptureImage.this, NewIssue.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("image", base64Image);
                startActivity(intent);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(CaptureImage.this, "Error capturing image", Toast.LENGTH_SHORT).show();
            }

        });

    }

    private Boolean compressImage(String pathname) throws IOException {
        try{
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(pathname)));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            File jpgImage = new File(getFilesDir(),"image.jpg");
            FileOutputStream fileOutputStream = new FileOutputStream(jpgImage.getPath());
            fileOutputStream.write(byteArray);
            fileOutputStream.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

}