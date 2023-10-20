package com.icpsltd.stores.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.icpsltd.stores.R;

public class AddStaff extends AppCompatActivity {

    PendingIntent pendingIntent;

    BottomSheetDialog bottomSheetDialog;

    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

    }

    public void showBottomSheet(){
        int layout = R.layout.login_with_access_sheet;
        View bottomSheetView = LayoutInflater.from(this).inflate(layout, null);

        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);

        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            //NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
            bottomSheetDialog.show();
        } else {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_SHORT).show();
            //nfcAdapter.disableForegroundDispatch(this);
        }

    }


    public void captureAccess(View view) {
        showBottomSheet();
    }
}