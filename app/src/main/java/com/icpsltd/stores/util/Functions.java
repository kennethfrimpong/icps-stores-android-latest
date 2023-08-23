package com.icpsltd.stores.util;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.icao.GIdData;
import com.credenceid.icao.GhanaIdCardFpTemplateInfo;
import com.credenceid.icao.ICAOReadIntermediateCode;
import com.icpsltd.stores.R;
import com.icpsltd.stores.model.CardDetails;
import com.icpsltd.stores.setting.DialogResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Functions {
    private static AlertDialog alertDialog;

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

}
