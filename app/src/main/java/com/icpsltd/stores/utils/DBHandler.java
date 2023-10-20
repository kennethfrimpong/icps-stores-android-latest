package com.icpsltd.stores.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "storesdb";
    private static final int DB_VERSION = 1;
    private static final String USER_TABLE = "currentUserTable";
    private static final String ID = "id";
    private static final String firstName = "firstName";
    private static final String lastName = "lastName";
    private static final String privilege = "privilege";

    private static final String sessionToken = "sessionToken";
    
    //RETRIEVED / UPDATED STOCK TABLE FROM DB

    private static final String stockProductName = "ProductName";

    private static final String stockProductDesc = "ProductDesc";

    private static final String stockProductStore = "ProductStore";

    private static final String stockProductQuantity = "ProductQuantity";

    private static final String stockProductLocation = "ProductLocation";

    private static final String stockID = "id";

    private static final String TABLE = "stockTable";
    
    //ONGOING STOCK TABLE
    private static final String ongoingIssueProductName = "ProductName";

    private static final String ongoingIssueProductDesc = "ProductDesc";

    private static final String ongoingIssueProductStore = "ProductStore";

    private static final String ongoingIssueProductQuantity = "ProductQuantity";

    private static final String ongoingIssueProductLocation = "ProductLocation";

    private static final String ongoingIssueID = "id";

    private static final String ongoingIssueTABLE = "ongoingIssueTable";

    //ongoing issue metadata
    private static final String ongoingIssueMetaTable = "ongoingIssueMetaTable";

    private static final String ongoingIssueMetaTime = "ongoingIssueMetaTime";

    private static final String ongoingIssueMetaDate = "ongoingIssueMetaDate";

    private static final String ongoingIssueMetaIssuerID = "ongoingIssueMetaIssuerID";

    private static final String ongoingIssueMetaIssuer = "ongoingIssueMetaIssuer";

    private static final String ongoingIssueMetaReceiverID = "ongoingIssueMetaReceiverID";

    private static final String ongoingIssueMetaReceiver = "ongoingIssueMetaReceiver";

    private static final String ongoingIssueMetaTransactionID = "ongoingIssueMetaTransactionID";

    //Staff Table

    private static final String STAFFTABLE = "staffTable";



    private static final String staffFirstName = "staffFirstName";

    private static final String staffMiddleName = "staffMiddleName";

    private static final String staffLastName = "staffLastName";

    private static final String staffType = "staffType";

    private static final String staffDepartment = "staffDepartment";

    private static final String staffID = "staffID";


    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        //use this for only new tables, remove already existing tables during upgrade. alter table columns in onUpgrade

        String query = "CREATE TABLE " + USER_TABLE + " ("
                + ID + " INTEGER PRIMARY KEY, "
                + firstName + " TEXT,"
                + lastName + " TEXT,"
                + privilege + " TEXT,"
                + "accessID" + " TEXT,"
                + sessionToken + " INTEGER UNIQUE)";
        db.execSQL(query);

        String quer = "CREATE TABLE " + TABLE + " ("
                + stockID + " TEXT PRIMARY KEY, "
                + stockProductName + " TEXT,"
                + stockProductDesc + " TEXT,"
                + stockProductQuantity + " INTEGER,"
                + stockProductStore + " TEXT,"
                + "ProductUnit"+" TEXT,"+
                stockProductLocation + " TEXT)";
        db.execSQL(quer);

        String createIssueHistory = "CREATE TABLE " + "issueHistoryTable" + " ("
                + "id" + " INTEGER, "
                + "ProductName" + " TEXT,"
                + "ProductDesc" + " TEXT,"
                + "ProductQuantity" + " INTEGER,"
                + "ProductUnit" + " TEXT,"
                + "ProductStore" + " TEXT,"
                + "ProductLocation" + " TEXT,"
                + "IssueDate" + " TEXT,"
                + "IssueTime" + " TEXT,"
                + "BookNumber" + " TEXT," +
                "ReceiverDepartment" + " TEXT)";
        db.execSQL(createIssueHistory);

        String createIssueHistoryMain = "CREATE TABLE " + "issueHistoryTableMain" + " ("
                + "id" + " INTEGER, "
                + "TransactionDate" + " TEXT,"
                + "TransactionTime" + " TEXT,"
                + "BookNumber" + " TEXT,"
                + "ReceiverName" + " TEXT,"
                +"ReceiverDepartment" + " TEXT)";
        db.execSQL(createIssueHistoryMain);


        String createCredentialsTable = "CREATE TABLE " + "credentialsTable" + " ("
                + "ApiHost" + " TEXT ,"
                + "ApiPort" + " TEXT,"
                + "DatabaseHost" + " TEXT,"
                + "DatabaseName" + " TEXT,"
                + "DatabaseUsername" + " TEXT,"
                + "DatabasePassword" + " TEXT,"
                + "SessionToken" + " INTEGER UNIQUE)";
        db.execSQL(createCredentialsTable);

        String createStoreTable = "CREATE TABLE " + "storeTable" + " ("
                + "id" + " INTEGER PRIMARY KEY, "
                + "StoreName" + " TEXT,"
                + "StoreLocation" + " TEXT,"
                +"StoreDescription" + " TEXT)";
        db.execSQL(createStoreTable);

        String createLocationTable = "CREATE TABLE " + "locationTable" + " ("
                + "id" + " INTEGER PRIMARY KEY, "
                + "LocationName" + " TEXT,"
                + "storeID" + " TEXT,"
                + "LocationStore" + " TEXT,"
                +"LocationDescription" + " TEXT)";
        db.execSQL(createLocationTable);

        String createProductMain = "CREATE TABLE " + "productTableMain" + " ("
                + "id" + " INTEGER, "
                + "TransactionDate" + " TEXT,"
                + "TransactionTime" + " TEXT,"
                + "TransactionType" + " TEXT,"
                + "AdditionType" + " TEXT,"
                + "ProductQuantity" + " TEXT,"
                + "ProductUnit" + " TEXT,"
                + "Balance" + " TEXT,"
                + "IssuerName" + " TEXT,"
                + "StaffName" + " TEXT,"
                + "BookNumber" + " TEXT,"
                + "ReceiverName" + " TEXT,"
                +"ReceiverDepartment" + " TEXT)";
        db.execSQL(createProductMain);

        String ongissue = "CREATE TABLE " + ongoingIssueTABLE + " ("
                + ongoingIssueID + " TEXT PRIMARY KEY, "
                + ongoingIssueProductName + " TEXT,"
                + ongoingIssueProductDesc + " TEXT,"
                + ongoingIssueProductQuantity + " INTEGER,"
                + ongoingIssueProductStore + " TEXT,"
                + ongoingIssueProductLocation + " TEXT,"
                + "ProductUnit"+" TEXT,"
                + "TransactionID"+" INTEGER,"
                + "IssuerID"+" INTEGER,"
                + "IssuerName"+" TEXT,"
                + "ReceiverID"+" INTEGER,"
                + "ReceiverName"+" TEXT,"
                + "ReceiverDepartment"+" TEXT,"
                + "TransactionDate"+" TEXT,"
                + "TransactionTime"+" TEXT,"
                + "BookNumber"+" TEXT)";
        db.execSQL(ongissue);

        String stafftable = "CREATE TABLE " + STAFFTABLE + " ("
                + staffID + " INTEGER PRIMARY KEY, "
                + staffFirstName + " TEXT,"
                + staffMiddleName + " TEXT,"
                + staffLastName + " TEXT,"
                + "canNumber" + " TEXT,"
                + staffType + " TEXT,"
                + staffDepartment + " TEXT)";
        db.execSQL(stafftable);

        String ongoingIssueMeta = " CREATE TABLE " + ongoingIssueMetaTable + " ("
                + ongoingIssueMetaTransactionID + " INTEGER,"
                + ongoingIssueMetaIssuerID + " INTEGER,"
                + ongoingIssueMetaIssuer + " TEXT,"
                + ongoingIssueMetaReceiverID + " INTEGER,"
                + ongoingIssueMetaReceiver + " TEXT,"
                + "ongoingIssueMetaReceiverDepartment" + " TEXT,"
                + ongoingIssueMetaDate + " TEXT,"
                + ongoingIssueMetaTime +" TEXT)";
        db.execSQL(ongoingIssueMeta);

        String createMoveTable = "CREATE TABLE " + "moveHistoryTable" + " ("
                + "ProductID" + " INTEGER PRIMARY KEY, "
                + "ProductName" + " TEXT,"
                + "FromStoreID" + " TEXT,"
                + "FromStoreName" + " TEXT,"
                + "FromLocationID" + " TEXT,"
                + "FromLocationName" + " TEXT,"
                + "ToStoreID" + " TEXT,"
                + "ToStoreName" + " TEXT,"
                + "ToLocationID" + " TEXT,"
                + "ToLocationName" + " TEXT,"
                + "StaffID" + " TEXT,"
                + "StaffName" + " TEXT,"
                + "MoveDate" + " TEXT,"
                + "MoveTime" + " TEXT,"
                + "SessionToken"+" INTEGER UNIQUE)";
        db.execSQL(createMoveTable);

    }

    public SQLiteDatabase openDatabase(Context context){
        String dbpath = context.getDatabasePath("storesdb").getPath();
        return SQLiteDatabase.openDatabase(dbpath,null,SQLiteDatabase.OPEN_READWRITE);
    }
    public void syncMoveTable(Integer productID, String productName, String fromStoreID, String fromStoreName, String fromLocationID, String fromLocationName, String toStoreID, String toStoreName, String toLocationID, String toLocationName, String staff_id, String staff_name, String moveDate, String moveTime ) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("ProductID", productID);
        values.put("ProductName", productName);
        values.put("FromStoreID",fromStoreID);
        values.put("FromStoreName",fromStoreName);
        values.put("FromLocationID",fromLocationID);
        values.put("FromLocationName",fromLocationName);
        values.put("ToStoreID",toStoreID);
        values.put("ToStoreName",toStoreName);
        values.put("ToLocationID",toLocationID);
        values.put("ToLocationName",toLocationName);
        values.put("StaffID",staff_id);
        values.put("StaffName",staff_name);
        values.put("MoveDate",moveDate);
        values.put("MoveTime",moveTime);
        values.put("SessionToken",1);


        db.insertWithOnConflict("moveHistoryTable", null, values,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void updateOngoingIssueTable(Integer transactionID, Integer issuerID, String issuerName, Integer receiverID, String receiverName, String receiverDept,  String transactionDate, String transactionTime, String bookNumber) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("TransactionID", transactionID);
        values.put("IssuerID", issuerID);
        values.put("IssuerName", issuerName);
        values.put("ReceiverID", receiverID);
        values.put("ReceiverName", receiverName);
        values.put("ReceiverDepartment", receiverDept);
        values.put("TransactionDate", transactionDate);
        values.put("TransactionTime", transactionTime);
        values.put("BookNumber",bookNumber);
        db.beginTransaction();
        try{
            db.update(ongoingIssueTABLE, values,null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }


    }

    public void addUserSession(String userID, String userFirstName, String userLastName, String userPrivilege, Integer sessionInt, String accessID) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(ID, userID);
        values.put(firstName, userFirstName);
        values.put(lastName, userLastName);
        values.put(privilege, userPrivilege);
        values.put(sessionToken, sessionInt);
        values.put("accessID",accessID);

        db.insertWithOnConflict(USER_TABLE, null, values,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addOngoingIssueMeta(Integer transactionID, Integer issuerID, String issuerName, Integer receiverID, String receiverName, String receiverDept, String transactionDate, String transactionTime ) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(ongoingIssueMetaTransactionID, transactionID);
        values.put(ongoingIssueMetaIssuerID, issuerID);
        values.put(ongoingIssueMetaIssuer, issuerName);
        values.put(ongoingIssueMetaReceiverID, receiverID);
        values.put(ongoingIssueMetaReceiver, receiverName);
        values.put("ongoingIssueMetaReceiverDepartment", receiverDept);
        values.put(ongoingIssueMetaDate, transactionDate);
        values.put(ongoingIssueMetaTime, transactionTime);
        db.insert(ongoingIssueMetaTable, null, values);
    }

    public void logOut(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM currentUsertable");
        db.execSQL("DELETE  FROM stockTable");
        db.execSQL("DELETE  FROM staffTable");
        db.execSQL("DELETE  FROM ongoingIssueTable");
        db.execSQL("DELETE FROM ongoingIssueMetaTable");
        db.execSQL("DELETE FROM issueHistoryTable");
        db.execSQL("DELETE FROM issueHistoryTableMain");
        db.execSQL("DELETE FROM productTableMain");
        db.execSQL("DELETE FROM storeTable");
        db.execSQL("DELETE FROM locationTable");
        db.execSQL("DELETE FROM moveHistoryTable");

    }

    public void clearOngoingIssueMetaTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM ongoingIssueMetaTable");
    }

    public void clearStockTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM stockTable");
    }
    public void clearMoveTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM moveHistoryTable");
    }

    public void clearStoreTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM storeTable");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
        if (oldVersion > 2){
            Log.i("StockTable","Greater than 3 executed");
            /*

            String quer = "CREATE TABLE " + TABLE + " ("
                    + stockID + " INTEGER, "
                    + stockProductName + " TEXT,"
                    + stockProductDesc + " TEXT,"
                    + stockProductQuantity + " INTEGER,"
                    + stockProductStore + " TEXT," +
                    stockProductLocation + " TEXT)";
            db.execSQL(quer);

             */
        } else if (oldVersion < 2) {
            Log.i("StockTable","Less than 3 executed");

        }
    }

    public void syncStockTable(String itemCode, String productName, String productDesc, Integer productQuantity, String productStore, String productLocation, String productUnit) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(stockID, itemCode);
        values.put(stockProductName, productName);
        values.put(stockProductDesc, productDesc);
        values.put(stockProductQuantity, productQuantity);
        values.put(stockProductStore, productStore);
        values.put(stockProductLocation, productLocation);
        values.put("ProductUnit",productUnit);

        db.insertWithOnConflict(TABLE, null, values,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getFirstName() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT firstName FROM currentUserTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        return cursor.getString(0);

    }

    public ContentValues getSavedCredentials() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT ApiHost,ApiPort,DatabaseHost,DatabaseName,DatabaseUsername,DatabasePassword FROM credentialsTable WHERE SessionToken = 1",null);
        ContentValues contentValues = new ContentValues();

        if (cursor != null) {
            cursor.moveToFirst();
            contentValues.put("ApiHost",cursor.getString(cursor.getColumnIndexOrThrow("ApiHost")));
            contentValues.put("ApiPort",cursor.getString(cursor.getColumnIndexOrThrow("ApiPort")));
            contentValues.put("DatabaseHost",cursor.getString(cursor.getColumnIndexOrThrow("DatabaseHost")));
            contentValues.put("DatabaseName",cursor.getString(cursor.getColumnIndexOrThrow("DatabaseName")));
            contentValues.put("DatabaseUsername",cursor.getString(cursor.getColumnIndexOrThrow("DatabaseUsername")));
            contentValues.put("DatabasePassword",cursor.getString(cursor.getColumnIndexOrThrow("DatabasePassword")));
        }

        return contentValues;

    }

    public void syncAppCredentials(String ApiHost, String ApiPort, String DatabaseHost, String DatabaseName, String DatabaseUsername, String DatabasePassword) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("ApiHost",ApiHost);
        contentValues.put("ApiPort",ApiPort);
        contentValues.put("DatabaseHost",DatabaseHost);
        contentValues.put("DatabaseName",DatabaseName);
        contentValues.put("DatabaseUsername",DatabaseUsername);
        contentValues.put("DatabasePassword",DatabasePassword);
        contentValues.put("SessionToken",1);

        db.insertWithOnConflict("credentialsTable", null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getApiHost() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT ApiHost FROM credentialsTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public String getApiPort() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT ApiPort FROM credentialsTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public String getDatabaseHost() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DatabaseHost FROM credentialsTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public String getDatabaseName() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DatabaseName FROM credentialsTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public String getDatabaseUsername() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DatabaseUsername FROM credentialsTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public String getDatabasePassword() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DatabasePassword FROM credentialsTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public String getLastName() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT lastName FROM currentUserTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public Integer getIssuerID() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM currentUserTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getInt(0);

    }
    public String getPrivilege() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT privilege FROM currentUserTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public String getAccessID() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT accessID FROM currentUserTable WHERE sessionToken = 1",null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor.getString(0);

    }

    public void addToOngoingIssue(String ID, String productName, String productDesc, Integer productQuantity, String productStore, String productLocation, String unit) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(stockID, ID);
        values.put(stockProductName, productName);
        values.put(stockProductDesc, productDesc);
        values.put(stockProductQuantity, productQuantity);
        values.put(stockProductStore, productStore);
        values.put(stockProductLocation, productLocation);
        values.put("ProductUnit",unit);

        db.insertWithOnConflict(ongoingIssueTABLE, null, values,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clearStaffTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM staffTable");
    }

    public void clearLocationTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM locationTable");
    }

    public void issueHistoryTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM issueHistoryTable");
    }
    public void issueHistoryTableMain(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM issueHistoryTableMain");
    }
    public void clearProductTableMain(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM productTableMain");
    }

    public void deleteFromOngoingIssueTable(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM ongoingIssueTable WHERE id ='"+id+"'");
    }

    public void clearOngoingIssueTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE  FROM ongoingIssueTable");
    }

    public String checkOngoingIssueTableEmptiness(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM ongoingIssueTable",null);
        String empty = "empty";
        String notempty = "notempty";
        if(cursor.moveToNext()){
            return notempty;
        }
        return empty;
    }
    public Integer checkOngoingIssueItemQuantity(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT ProductQuantity FROM stockTable WHERE id ='"+id+"'",null);
        if(cursor.moveToNext()){
            Integer quantity = cursor.getInt(cursor.getColumnIndexOrThrow("ProductQuantity"));
            return quantity;
        }
        return -1;
    }
    public void syncStaffTable(Integer staffiD, String firstName, String middleName, String lastName, String type, String department, String canNumber) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(staffID, staffiD);
        values.put(staffFirstName, firstName);
        values.put(staffMiddleName,middleName);
        values.put(staffLastName, lastName);
        values.put(staffType, type);
        values.put(staffDepartment, department);
        values.put("canNumber", canNumber);

        db.insertWithOnConflict(STAFFTABLE, null, values,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void syncStoreTable(Integer id, String storeName, String storeLocation, String storeDescription) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("id", id);
        values.put("storeName", storeName);
        values.put("storeLocation",storeLocation);
        values.put("storeDescription", storeDescription);

        db.insertWithOnConflict("storeTable", null, values,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void syncLocationTable(Integer id, String locationName, Integer storeID, String locationStore, String locationDescription) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("id", id);
        values.put("LocationName", locationName);
        values.put("StoreID",storeID);
        values.put("LocationStore", locationStore);
        values.put("LocationDescription", locationDescription);

        db.insert("locationTable", null, values);
    }

    public void syncIssueHistoryTable(Integer ID, String productName, String productDesc, Integer productQuantity, String productStore, String productLocation, String receiverDepartment, String transactionDate, String transactionTime, String bookNumber, String unit) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("id", ID);
        values.put("ProductName", productName);
        values.put("ProductDesc", productDesc);
        values.put("ProductQuantity", productQuantity);
        values.put("ProductStore", productStore);
        values.put("ProductStore", productStore);
        values.put("ProductLocation", productLocation);
        values.put("IssueDate", transactionDate);
        values.put("IssueTime", transactionTime);
        values.put("BookNumber", bookNumber);
        values.put("ProductUnit",unit);

        db.insert("issueHistoryTable", null, values);
    }


    public void syncIssueHistoryTableMain(Integer transactionID, String receiverName, String receiverDepartment, String transactionDate, String transactionTime, String bookNumber) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("id", transactionID);
        values.put("ReceiverName", receiverName);
        values.put("ReceiverDepartment", receiverDepartment);
        values.put("TransactionDate", transactionDate);
        values.put("TransactionTime", transactionTime);
        values.put("BookNumber", bookNumber);
        db.insert("issueHistoryTableMain", null, values);
    }

    public void syncProductTableMainIssue(Integer transactionID, String transactionType, String receiverName, String receiverDepartment, String issuerName, String transactionDate, String transactionTime, String productQuantity, String bookNumber, String balance, String unit) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("id", transactionID);
        values.put("ReceiverName", receiverName);
        values.put("IssuerName", issuerName);
        values.put("ReceiverDepartment", receiverDepartment);
        values.put("TransactionDate", transactionDate);
        values.put("TransactionTime", transactionTime);
        values.put("TransactionType", transactionType);
        values.put("ProductQuantity", productQuantity);
        values.put("BookNumber", bookNumber);
        values.put("Balance", balance);
        values.put("ProductUnit",unit);
        db.insert("productTableMain", null, values);
    }

    public void syncProductTableMainAddition(Integer transactionID, String transactionType, String staffName, String additionType, String transactionDate, String transactionTime, String quantityAdded, String balance, String bookNumber, String unit) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("id", transactionID);
        values.put("StaffName", staffName);
        values.put("AdditionType", additionType);
        values.put("TransactionDate", transactionDate);
        values.put("TransactionTime", transactionTime);
        values.put("TransactionType", transactionType);
        values.put("ProductQuantity", quantityAdded);
        values.put("Balance", balance);
        values.put("BookNumber", bookNumber);
        values.put("ProductUnit",unit);
        db.insert("productTableMain", null, values);
    }



}
