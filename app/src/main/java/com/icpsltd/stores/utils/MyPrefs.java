package com.icpsltd.stores.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

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

    //create encrypted shared preferences
    public SharedPreferences getSecurePreferenceObject(Context context) {
        return context.getSharedPreferences(PREF_FILE, 0);
    }

    public String getToken(Context context) throws GeneralSecurityException, IOException{
        String masterKeys = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedSharedPreferences pref = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                PREF_FILE,
                masterKeys,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        return pref.getString("jwt_token",null);
    }

    public SharedPreferences.Editor getSecurePreferenceEditObject(Context context) throws GeneralSecurityException, IOException {
        //SharedPreferences pref = EncryptedSharedPreferences.getSharedPreference(context, PREF_FILE, 0);
        String masterKeys = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedSharedPreferences pref = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                PREF_FILE,
                masterKeys,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        return pref.edit();
    }



    public void saveToken(Context context, String token) throws GeneralSecurityException, IOException {
        getSecurePreferenceEditObject(context).putString("jwt_token",token).commit();
    }




    public void clearAllData(Context context){
        getPreferenceEditObject(context).clear().commit();
    }

}
