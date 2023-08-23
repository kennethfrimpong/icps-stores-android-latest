package com.icpsltd.stores.setting;

import android.app.AlertDialog;
import android.view.View;

public class DialogResult {
    private View view;
    private AlertDialog alertDialog;

    public DialogResult(View view, AlertDialog alertDialog) {
        this.view = view;
        this.alertDialog = alertDialog;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public AlertDialog getAlertDialog() {
        return alertDialog;
    }

    public void setAlertDialog(AlertDialog alertDialog) {
        this.alertDialog = alertDialog;
    }
}
