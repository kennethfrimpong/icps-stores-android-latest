package com.icpsltd.stores.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.icpsltd.stores.biometricactivities.BiometricLogin;
import com.icpsltd.stores.utils.DBHandler;
import com.icpsltd.stores.utils.MyPrefs;
import com.icpsltd.stores.R;

public class HomePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        DBHandler dbHandler = new DBHandler(getApplicationContext());

        MyPrefs myPrefs = new MyPrefs();
        if(!myPrefs.getLoginStatus(getApplicationContext())){
            Toast.makeText(getApplicationContext(),"You need to login first",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomePage.this, BiometricLogin.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        } else{

            TextView tv = findViewById(R.id.welcome);
            TextView pv = findViewById(R.id.privilege);
            tv.setText(getString(R.string.welcome)+dbHandler.getFirstName());
            pv.setText(dbHandler.getPrivilege());
        }
        MaterialCardView materialCardView = findViewById(R.id.new_issue);
        materialCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, NewIssue.class);
                startActivity(intent);
            }
        });
        dbHandler.clearOngoingIssueTable();
        dbHandler.clearOngoingIssueMetaTable();
        dbHandler.clearStaffTable();
        dbHandler.clearStockTable();
        dbHandler.clearStoreTable();
        dbHandler.clearLocationTable();
        dbHandler.clearMoveTable();

    }

    @Override
    protected void onStart() {
        super.onStart();
        MyPrefs myPrefs = new MyPrefs();
        if(!myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(HomePage.this, BiometricLogin.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyPrefs myPrefs = new MyPrefs();
        if(!myPrefs.getLoginStatus(getApplicationContext())){
            Intent intent = new Intent(HomePage.this, BiometricLogin.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

    }

    public void logout(View view) {
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(HomePage.this);
        materialAlertDialogBuilder.setTitle("Logout")
                .setMessage("Would you like to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                        MyPrefs myPrefs = new MyPrefs();
                        dbHandler.logOut();
                        myPrefs.saveLoginStatus(getApplicationContext(),false);
                        Intent intent = new Intent(HomePage.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = materialAlertDialogBuilder.create();
        alertDialog.show();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            onBackPressed();
        }
        return true;
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void goToAddStock(View view) {
        Intent intent = new Intent(HomePage.this, AddStock.class);
        startActivity(intent);
    }

    public void go_to_issue_history(View view) {
        Intent intent = new Intent(HomePage.this, IssueHistory.class);
        startActivity(intent);
    }

    public void goToQBP(View view) {
        Intent intent = new Intent(HomePage.this, QueryByProduct.class);
        startActivity(intent);
    }

    public void goToQBL(View view) {
        Intent intent = new Intent(HomePage.this, QueryByLocation.class);
        startActivity(intent);
    }

    public void goToReturnStock(View view) {
        Intent intent = new Intent(HomePage.this, ReturnStock.class);
        startActivity(intent);
    }

    public void goToStockAdjustment(View view) {
        Intent intent = new Intent(HomePage.this, StockAdjustment.class);
        startActivity(intent);
    }

    public void addNewStaff(View view) {
        Intent intent = new Intent(HomePage.this, AddStaff.class);
        startActivity(intent);
    }
}