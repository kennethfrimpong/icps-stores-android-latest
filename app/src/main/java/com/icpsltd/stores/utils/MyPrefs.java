package com.icpsltd.stores.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPrefs {
    private static final String PREF_FILE = "session";
    private static final String PURCHASE_KEY = "loggedin";

    private static final String receiverFirstName = "receiverFirstName";

    public SharedPreferences getPreferenceObject(Context context) {
        return context.getSharedPreferences(PREF_FILE, 0);
    }

    public SharedPreferences.Editor getPreferenceEditObject(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_FILE, 0);
        return pref.edit();
    }

    public boolean getLoginStatus(Context context){
        return getPreferenceObject(context).getBoolean( PURCHASE_KEY,false);
    }

    public void saveLoginStatus(Context context, boolean value){
        getPreferenceEditObject(context).putBoolean(PURCHASE_KEY,value).commit();
    }

}
