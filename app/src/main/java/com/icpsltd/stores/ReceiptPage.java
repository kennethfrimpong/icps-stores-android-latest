package com.icpsltd.stores;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ReceiptPage extends AppCompatActivity {
    private ListView ongoingListview;
    private List<RetrievedStock> fetchedOngoingIssueTable;
    private ArrayAdapter<RetrievedStock> ongoingIssueAdapter;

    private String title;
    private String staffName;
    private String returnerName;
    private String returnerDepartment;
    private String itemsReturned;

    private Boolean isReturn = false;
    private Boolean isAdjustment = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_page);

        try{

            if(getIntent().getStringExtra("title").equals("Return Stock")){

                title = getIntent().getStringExtra("title");
                staffName = getIntent().getStringExtra("staffName");
                returnerName = getIntent().getStringExtra("returnerName");
                returnerDepartment = getIntent().getStringExtra("returnerDept");
                itemsReturned = getIntent().getStringExtra("itemsReturned");
                isReturn = true;

            } else if (getIntent().getStringExtra("type").equals("Adjustment")) {
                title = getIntent().getStringExtra("title");
                staffName = getIntent().getStringExtra("staffName");
                returnerName = getIntent().getStringExtra("returnerName");
                returnerDepartment = getIntent().getStringExtra("returnerDept");
                itemsReturned = getIntent().getStringExtra("itemsReturned");
                isAdjustment = true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }


        DBHandler dbHandler = new DBHandler(getApplicationContext());
        if (dbHandler.checkOngoingIssueTableEmptiness().equals("empty")){
            Intent intent = new Intent(ReceiptPage.this, HomePage.class);
            startActivity(intent);
        }

        TextView title1 = findViewById(R.id.title);
        TextView issuername =findViewById(R.id.staffName);
        TextView receivername = findViewById(R.id.returnerName);
        TextView receiverdept = findViewById(R.id.returnerDepartment);
        TextView itemsReturned1 = findViewById(R.id.itemsReturned);

        if(isReturn||isAdjustment){
            title1.setText(title);
            issuername.setText(staffName);
            receivername.setText(returnerName);
            receiverdept.setText(returnerDepartment);
            itemsReturned1.setText(itemsReturned);
        }



        dbHandler.openDatabase(ReceiptPage.this);
        SQLiteDatabase db = dbHandler.openDatabase(getApplicationContext());
        Cursor cursor = db.rawQuery("SELECT * FROM ongoingIssueMetaTable",null);

        TextView transactionID = findViewById(R.id.transaction_id);
        TextView issuerName = findViewById(R.id.issuer_name);
        TextView receiverName = findViewById(R.id.receiver_name);
        TextView receiverDept = findViewById(R.id.receiver_department);
        TextView name = findViewById(R.id.firstLastName);

        if(isAdjustment){
            receiverDept.setVisibility(View.GONE);
            receiverdept.setVisibility(View.GONE);
        } else if (isReturn) {
            receiverDept.setVisibility(View.VISIBLE);
            receiverdept.setVisibility(View.VISIBLE);
        }

        while (cursor.moveToNext()){

            transactionID.setText("#"+cursor.getString(cursor.getColumnIndexOrThrow("ongoingIssueMetaTransactionID")));
            issuerName.setText(cursor.getString(cursor.getColumnIndexOrThrow("ongoingIssueMetaIssuer")));
            receiverName.setText(cursor.getString(cursor.getColumnIndexOrThrow("ongoingIssueMetaReceiver")));
            name.setText("as "+cursor.getString(cursor.getColumnIndexOrThrow("ongoingIssueMetaIssuer")));
            receiverDept.setText(cursor.getString(cursor.getColumnIndexOrThrow("ongoingIssueMetaReceiverDepartment")));

        }

        ongoingListview = findViewById(R.id.ongoing_item_list);

        fetchedOngoingIssueTable = new ArrayList<>();
        //load existing ongoing issue
        Cursor cursorx = db.rawQuery("SELECT id, productName, productQuantity, productStore, productlocation, productDesc, productUnit FROM ongoingIssueTable",null);
        while (cursorx.moveToNext()){

            Integer idx = cursorx.getInt(cursorx.getColumnIndexOrThrow("id"));
            String namex = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductName"));
            Integer quantityx = cursorx.getInt(cursorx.getColumnIndexOrThrow("ProductQuantity"));
            String storex = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductStore"));
            String locationx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductLocation"));
            String descriptionx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductDesc"));
            String unitx = cursorx.getString(cursorx.getColumnIndexOrThrow("ProductUnit"));
            Log.e("Retrieved",namex);

            RetrievedStock retrievedStock = new RetrievedStock(idx,namex,descriptionx,quantityx,storex,locationx,unitx);
            fetchedOngoingIssueTable.add(retrievedStock);

        }
        ongoingIssueAdapter = new ArrayAdapter<RetrievedStock>(getApplicationContext(), R.layout.add_item_list, R.id.product_name, fetchedOngoingIssueTable) {
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

        assert ongoingListview != null;
        ongoingListview.setAdapter(ongoingIssueAdapter);
        ongoingIssueAdapter.notifyDataSetChanged();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        dbHandler.clearOngoingIssueTable();
        dbHandler.clearStockTable();
        dbHandler.clearStaffTable();
        dbHandler.clearOngoingIssueMetaTable();

        Intent intent = new Intent(ReceiptPage.this, HomePage.class);
        startActivity(intent);
    }

    public void goHome(View view) {
        DBHandler dbHandler = new DBHandler(getApplicationContext());
        dbHandler.clearOngoingIssueTable();
        dbHandler.clearStockTable();
        dbHandler.clearStaffTable();
        dbHandler.clearOngoingIssueMetaTable();

        Intent intent = new Intent(ReceiptPage.this, HomePage.class);
        startActivity(intent);
    }
}