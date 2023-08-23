package com.icpsltd.stores;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class StockTable extends SQLiteOpenHelper {

    private static final String DB_NAME = "storesdb";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "stockTable";
    private static final String stockProductName = "ProductName";

    private static final String stockProductDesc = "ProductDesc";

    private static final String stockProductStore = "ProductStore";

    private static final String stockProductQuantity = "ProductQuantity";

    private static final String stockProductLocation = "ProductLocation";

    private static final String stockID = "id";

    public StockTable(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }


    public void syncStockTable(Integer ID, String productName, String productDesc, Integer productQuantity, String productStore, String productLocation, String productUnit) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(stockID, ID);
        values.put(stockProductName, productName);
        values.put(stockProductDesc, productDesc);
        values.put(stockProductQuantity, productQuantity);
        values.put(stockProductStore, productStore);
        values.put(stockProductLocation, productLocation);
        values.put("ProductUnit",productUnit);

        db.insertWithOnConflict(TABLE, null, values,SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);

    }
}
