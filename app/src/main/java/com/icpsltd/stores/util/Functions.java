package com.icpsltd.stores.util;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.icao.GIdData;
import com.credenceid.icao.GhanaIdCardFpTemplateInfo;
import com.credenceid.icao.ICAOReadIntermediateCode;
import com.gmail.samehadar.iosdialog.CamomileSpinner;
import com.icpsltd.stores.R;
import com.icpsltd.stores.model.CardDetails;
import com.icpsltd.stores.setting.DialogResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.mrapp.android.dialog.MaterialDialog;

public class Functions {
    private static AlertDialog alertDialog;
    private static MaterialDialog.Builder dialogBuilder = null;
    private static MaterialDialog dialogMaterial = null;

    public static CardDetails cardDetails = null;

    public static DateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public static void Show_Alert(Context context, String title, String Message) {
        alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(Message)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private static DialogResult getDialog(Context context, int layout) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customLayout = inflater.inflate(layout, null);
        alertDialog.setView(customLayout);
        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(false);
        return new DialogResult(customLayout, alert);
    }

    public static AlertDialog showDialogWithOneButton(Context context, String content, int imageResource, View.OnClickListener okListener) {
        DialogResult dialogResult = getDialog(context, R.layout.dialog_with_one_button);
        TextView txtContent = dialogResult.getView().findViewById(R.id.txtContent);
        txtContent.setText(content);
        ImageView imageView = dialogResult.getView().findViewById(R.id.imageView2);
        imageView.setImageDrawable(context.getResources().getDrawable(imageResource));
        Button btnOk = dialogResult.getView().findViewById(com.icpsltd.stores.R.id.btn_ok);
        btnOk.setOnClickListener(okListener);
        return dialogResult.getAlertDialog();
    }



    public static CardDetails readGhanaCard(Context context, String canNumber, TextView finger1, TextView finger2, TextView finger3, TextView finger4) {
        String startTime = dateTimeFormat.format(new Date());

        App.BioManager.readSmartCard(canNumber, null, "GhanaNID", new Biometrics.GNIDReadListener() {

            @Override
            public void onGIDRead(Biometrics.ResultCode resultCode, ICAOReadIntermediateCode stage, String hint, GIdData data) {
                if (ICAOReadIntermediateCode.BAC == stage) {
                    if (FAIL == resultCode) {

                        String[] exceptions = hint.split(":");
                        switch (exceptions[1].trim()) {
                            case "com.c10n.scalibur.tr3110.TerminalAuthenticationException":
                                Functions.Show_Alert(context, "Terminal Certificate Error", "Kindly contact system administrator for assistance.");

                                break;
                            case "com.c10n.scalibur.WrongPinException":
                                Functions.Show_Alert(context, "CAN Number Error", "Wrong CAN Number, Please Check and Re-enter CAN number");

                                break;
                            default:
//                                Functions.Show_Alert(context, "APP Error", "Contact system administrator.");


                                break;
                        }


                    }


                }

                if (FAIL == resultCode) {
                    Log.d("ERROR_ISC", hint);
                    if (hint.contains(":")) {

                        String[] exceptions = hint.split(":");
                        for (String exception : exceptions) {
                            Log.d("EX_CANSS", exception);
                        }
                        switch (exceptions[1].trim()) {
                            case "com.c10n.scalibur.tr3110.TerminalAuthenticationException":
                                alertDialog = showDialogWithOneButton(context, "Terminal Certificate Error \nKindly contact system administrator for assistance.", R.drawable.not_verified, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        alertDialog.dismiss();

                                    }
                                });
                                alertDialog.show();

                                break;
                            case "com.c10n.scalibur.WrongPinException":
                                alertDialog = showDialogWithOneButton(context, "Wrong CAN Number\n Please Check and Re-enter CAN number.", R.drawable.not_verified, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        alertDialog.dismiss();

                                    }
                                });
                                alertDialog.show();
                                break;
                            default:
                                alertDialog = showDialogWithOneButton(context, "Please ensure that the right CAN Number is provided and try again \n OR \n Contact system administrator.", R.drawable.not_verified, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        alertDialog.dismiss();

                                    }
                                });
                                alertDialog.show();
                                break;
                        }
                    } else {
                        alertDialog = showDialogWithOneButton(context, "Please Reboot Device or \n Contact system administrator.", R.drawable.not_verified, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                alertDialog.dismiss();

                            }
                        });
                        alertDialog.show();

                    }


                }


                cardDetails = new CardDetails(data);
                if (ICAOReadIntermediateCode.DG10 == stage) {
                    if (OK == resultCode) {

                        List<GhanaIdCardFpTemplateInfo> ghanaIdCardFpList = data.DG10.getFingers();

                        for (int i = 0; i < ghanaIdCardFpList.size(); i++) {
                            if (i == 0) {
                                finger1.setText(ghanaIdCardFpList.get(i).getPositionName());
                            }
                            if (i == 1) {
                                finger2.setText(ghanaIdCardFpList.get(i).getPositionName());
                            }
                            if (i == 2) {
                                finger3.setText(ghanaIdCardFpList.get(i).getPositionName());
                            }
                            if (i == 3) {
                                finger4.setText(ghanaIdCardFpList.get(i).getPositionName());
                            }
                        }
                    }
                    //fpTemplateInfos.get(0).

                }
                // cardDetails = cardDetail;

            }
        });
        return cardDetails;

    }

    public static Dialog dialog;

    public static void Show_loader(Context context, boolean outside_touch, boolean cancleable) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.item_dialog_loading_view);
        dialog.getWindow().setBackgroundDrawable(context.getResources().getDrawable(R.drawable.d_round_white_background));


        CamomileSpinner loader = dialog.findViewById(R.id.loader);
        loader.start();


        if (!outside_touch)
            dialog.setCanceledOnTouchOutside(false);

        if (!cancleable)
            dialog.setCancelable(false);

        dialog.show();
    }


    public static void cancel_loader() {
        if (dialog != null) {
            dialog.cancel();
        }
    }

    public static AlertDialog showNotVerifiedDialog(Context context, String shortID, View.OnClickListener okListener, View.OnClickListener retryListener) {
        DialogResult dialogResult = getDialog(context, R.layout.dialog_failed);

        TextView trnxId = dialogResult.getView().findViewById(R.id.txtShortId);
        trnxId.setText(shortID.toUpperCase());
        Button btnOk = dialogResult.getView().findViewById(R.id.btn_ok);
        Button btnRetry = dialogResult.getView().findViewById(R.id.btn_retry);

        btnOk.setOnClickListener(okListener);
        btnRetry.setOnClickListener(retryListener);

        return dialogResult.getAlertDialog();
    }

    public static void show_toast(Context context, String msg) {
        //if(Variables.is_toast_enable) {
        Toast.makeText(context, "" + msg, Toast.LENGTH_LONG).show();
        //}
    }

    public static void Show_Alert2(Context context, String title, String Message) {
        dialogBuilder = new MaterialDialog.Builder(context);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(Message);
        dialogMaterial = dialogBuilder.create();
        dialogMaterial.show();
    }

    public static void hide_Alert2() {
        if (dialogMaterial != null && dialogMaterial.isShowing()) {
            dialogMaterial.dismiss();
        }
    }

    public static SharedPreferences getSharedPreference(Context context) {
        if (Variables.sharedPreferences == null) {
            Variables.sharedPreferences = context.getSharedPreferences(Variables.pref_name, Context.MODE_PRIVATE);
        }
        return Variables.sharedPreferences;
    }

    public static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences.Editor editor = getSharedPreference(context).edit();
        return editor;
    }

}
