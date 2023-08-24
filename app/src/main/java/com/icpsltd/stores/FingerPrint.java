package com.icpsltd.stores;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.icao.GhanaIdCardFpTemplateInfo;
import com.google.android.material.card.MaterialCardView;
import com.icpsltd.stores.model.CardDetails;
import com.icpsltd.stores.model.CardDetailsSummary;
import com.icpsltd.stores.util.App;
import com.icpsltd.stores.util.Functions;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FingerPrint extends AppCompatActivity {

    private CardDetails cardDetails = null;
    private byte[] mFingerprintOneFMDTemplate = null;

    private CardDetailsSummary cardDetailToHold = null;
    private static List<GhanaIdCardFpTemplateInfo> ghanaIdCardFpTemplateInfosList = new ArrayList<>();

    private int selectedFingerIndex = 0;

    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private AlertDialog alertDialog;

    private MaterialCardView fingerOne,fingerTwo,fingerThree,fingerFour;
    private TextView login_helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger_print);
        login_helper = findViewById(R.id.login_tv);
        fingerOne = findViewById(R.id.finger_one);
        fingerTwo = findViewById(R.id.finger_two);
        fingerThree = findViewById(R.id.finger_three);
        fingerFour = findViewById(R.id.finger_four);

        runOnUiThread(()->{
            login_helper.setText("Choose finger to scan");
            Toast.makeText(getApplicationContext(),"Choose finger to scan",Toast.LENGTH_SHORT).show();

            fingerOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.GREEN);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.BLACK);
                }
            });

            fingerTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.GREEN);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.BLACK);

                }
            });

            fingerThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.GREEN);
                    fingerFour.setStrokeColor(Color.BLACK);

                }
            });

            fingerFour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fingerOne.setStrokeColor(Color.BLACK);
                    fingerTwo.setStrokeColor(Color.BLACK);
                    fingerThree.setStrokeColor(Color.BLACK);
                    fingerFour.setStrokeColor(Color.GREEN);

                }
            });

        });
    }

    public void restart_login(View view) {
        Intent intent = new Intent(FingerPrint.this,FingerPrint.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    private class CardReading extends AsyncTask<Object, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Functions.Show_loader(FingerPrint.this, false, false);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
//
            //cardDetails = Functions.readGhanaCard(FingerPrint.this, canNumber, finger_1, finger_2, finger_3, finger_4);
            //Functions.arrangeFingers(finger_1,finger_2,finger_3,finger_4,cardDetails.getGhanaIdCardFpTemplateInfosList());
            //Functions.openFingerPrintReader();
            App.BioManager.openFingerprintReader(mFingerprintOpenCloseListener);

            return true;
        }

        protected void onPostExecute() {
            Functions.cancel_loader();

        }



    }

    private Biometrics.FingerprintReaderStatusListener mFingerprintOpenCloseListener =
            new Biometrics.FingerprintReaderStatusListener() {
                @Override
                public void
                onOpenFingerprintReader(Biometrics.ResultCode resultCode,
                                        String hint) {

                    if (hint != null && !hint.isEmpty())
                    {
                        Functions.show_toast(FingerPrint.this,hint);

                    }

                    if (OK == resultCode) {
                        Functions.show_toast(FingerPrint.this,"Finger Open Successful");

                        Functions.cancel_loader();
                    }

                    else if (FAIL == resultCode) {
                        Functions.cancel_loader();
                    }
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void
                onCloseFingerprintReader(Biometrics.ResultCode resultCode,
                                         Biometrics.CloseReasonCode closeReasonCode) {

                    if (OK == resultCode) {


                    }
                    else if (FAIL == resultCode) {

                    }
                }

            };


    private void captureFingerprintOne(ImageView imageView) {

        mFingerprintOneFMDTemplate = null;
        ghanaIdCardFpTemplateInfosList = Functions.cardDetails.getGhanaIdCardFpTemplateInfosList();
        cardDetailToHold = new CardDetailsSummary(Functions.cardDetails);
        App.BioManager.grabFingerprint(Biometrics.ScanType.SINGLE_FINGER, new Biometrics.OnFingerprintGrabbedNewListener() {
            @Override
            public void onFingerprintGrabbed(Biometrics.ResultCode resultCode, Bitmap bitmap, byte[] bytes, String s) {
                if (OK == resultCode) {
                    if (null != bitmap)
                        imageView.setImageBitmap(bitmap);

                    //mStatusTextView.setText("WSQ File: " + wsqFilepath);
                    //mInfoTextView.setText("WSQ Quality: " + i);

                    /* Create template from fingerprint image. */
                    AsynucCreateFMDTemplate(bitmap);
                    //createFMDTemplate(bitmap);
                }
                if(resultCode == INTERMEDIATE)
                {
                    if (null != bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }

            @Override
            public void onCloseFingerprintReader(Biometrics.ResultCode resultCode, Biometrics.CloseReasonCode closeReasonCode) {

            }
        });


    }

    private void AsynucCreateFMDTemplate(Bitmap bitmap)
    {
        new AsyncTask<Object, Boolean, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Functions.Show_loader(FingerPrint.this, false, false);
            }
            @Override
            protected Boolean doInBackground(Object... objects) {
                createFMDTemplate(bitmap);
                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                // Functions.cancel_loader();
            }


        }.execute();
    }

    private void createFMDTemplate(Bitmap bitmap)
    {
        App.BioManager.convertToFMD(bitmap, Biometrics.FMDFormat.ISO_19794_2_2005, new Biometrics.OnConvertToFMDListener() {
            @Override
            public void onConvertToFMD(Biometrics.ResultCode resultCode, byte[] bytes) {
                if(resultCode == OK)
                {
                    mFingerprintOneFMDTemplate = Arrays.copyOf(bytes, bytes.length);
                    if (mFingerprintOneFMDTemplate != null )
                    {
                        selectedFingerIndex = selectedFingerIndex -1;
                        byte[] data = ghanaIdCardFpTemplateInfosList.get(selectedFingerIndex).getMinutiae();
                        matchFMDTemplates(data,mFingerprintOneFMDTemplate);
                    }
                }
                else if (resultCode == INTERMEDIATE)
                {

                }
                else if(resultCode == FAIL)
                {

                }
            }
        });
    }

    private void matchFMDTemplates(byte[] templateOne,
                                   byte[] templateTwo) {
        //Functions.show_toast(FingerPrintActivity.this,"STARTING VERIFICATION");
        App.BioManager.compareFMD(templateOne, templateTwo, Biometrics.FMDFormat.ISO_19794_2_2005, new Biometrics.OnCompareFMDListener() {
            @Override
            public void onCompareFMD(Biometrics.ResultCode resultCode, float v) {
                Functions.cancel_loader();
                String responseTimestamp = dateFormat.format(new Date());

                if(resultCode == OK)
                {
                    Functions.show_toast(FingerPrint.this,"FINISHED VERIFICATION");
                    if(v >70.0)
                    {

                        try {


                            JSONObject nhisData = new JSONObject();
                            nhisData.put("bioMatchResult","MFP");
                            nhisData.put("cardNo",cardDetailToHold.getCardNumber());
                            nhisData.put("cardType","GHANACARD");

                        }catch (Exception e)
                        {
                            Functions.cancel_loader();
                            e.printStackTrace();
                            alertDialog = Functions.showDialogWithOneButton(
                                    FingerPrint.this,
                                    "Status Check not completed, Communication Error",
                                    R.drawable.not_verified,
                                    view -> alertDialog.dismiss());
                            alertDialog.show();
                        }


//                        Intent intent = new Intent(FingerPrintActivity.this, CardDetailsActivity.class);
//                        intent.putExtra(Variables.cardDetial, cardDetailToHold);
//                        intent.putExtra(Variables.offline_kyc, offlineKyc);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
//                        finish();
                    }
                    else{

                        alertDialog = Functions.showNotVerifiedDialog(FingerPrint.this, "12345", view -> {

                            alertDialog.dismiss();
                            App.BioManager.closeFingerprintReader();
                            App.BioManager.cardCloseCommand();
                            App.BioManager.cardDisconnectSync(1);
                            Intent intent = new Intent(FingerPrint.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }, view -> {
                            alertDialog.dismiss();
                        });
                        alertDialog.show();
                    }

                }
                else if (resultCode == INTERMEDIATE){

                }
                else if (resultCode == FAIL){

                }
            }
        });
    }


    public static List<GhanaIdCardFpTemplateInfo> getGhanaIdCardFpTemplateInfosList() {
        return ghanaIdCardFpTemplateInfosList;
    }

}