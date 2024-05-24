package com.icpsltd.stores.utils;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.icpsltd.stores.activities.AddStaff;
import com.icpsltd.stores.activities.MainActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenChecker {
    public void checkToken(String status, Context context, Activity activity) {
        if(status.equals("client_error")){
            activity.runOnUiThread(()->{
                Toast.makeText(activity, "Error connecting to server", Toast.LENGTH_SHORT).show();
                Toast.makeText(activity, "Are you connected?", Toast.LENGTH_SHORT).show();
            });
        } else if (status.equals("db_error")) {
            activity.runOnUiThread(()->{
                Toast.makeText(activity, "Error connecting to database", Toast.LENGTH_SHORT).show();
                Toast.makeText(activity, "Please contact admin", Toast.LENGTH_SHORT).show();
            });

        } else if (status.equals("error")){
            activity.runOnUiThread(()->{
                Toast.makeText(activity, "Error connecting to server", Toast.LENGTH_SHORT).show();
                Toast.makeText(activity, "Please contact system admin", Toast.LENGTH_SHORT).show();
            });
        } else if (status.equals("success")){

        } else if (status.equals("expired") || status.equals("invalid") || status.equals("missing") || status.equals("restricted")){
            String finalStatus = status;
            activity.runOnUiThread(()->{

                if (finalStatus.equals("expired")){
                    Toast.makeText(activity, "Session Expired", Toast.LENGTH_SHORT).show();
                    Toast.makeText(activity, "Please login again", Toast.LENGTH_SHORT).show();
                } else if (finalStatus.equals("invalid")){
                    Toast.makeText(activity, "Please login again", Toast.LENGTH_SHORT).show();
                } else if (finalStatus.equals("missing")){
                    Toast.makeText(activity, "Please login again", Toast.LENGTH_SHORT).show();
                } else if (finalStatus.equals("restricted")){
                    Toast.makeText(activity, "You do not have access to this application", Toast.LENGTH_SHORT).show();
                    Toast.makeText(activity, "Please contact system admin", Toast.LENGTH_SHORT).show();
                }
                DBHandler dbHandler = new DBHandler(context);
                try {
                    dbHandler.logOut(context);
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
                dbHandler.close();
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                //startActivity(intent);
                activity.finish();
            });
        }
    }
}
