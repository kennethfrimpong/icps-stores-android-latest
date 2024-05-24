package com.icpsltd.stores.utils;

import android.util.Log;

public class ItemLocationParser {
    public String[] parseLocation(String productLocation){
        String address = productLocation;
        String productStore;

        try{

            if(!address.contains("-") || !address.contains("SHELF") || !address.contains("LEVEL") || !address.contains("SPACE")){
                Log.d("Exception",address);
                throw new Exception();

            }

            String[] addressArray = address.split("-");
            String store1 = addressArray[0];
            store1 = store1.trim();
            store1 = store1.replace("_"," ");
            String shelf1 = addressArray[1];
            shelf1 = shelf1.trim();
            String level1 = addressArray[2];
            level1 = level1.trim();
            String space1 = addressArray[3];
            space1 = space1.replace(" ","");

            String shelfNumber = shelf1.substring(5);
            String levelNumber = level1.substring(5);
            String spaceNumber = space1.substring(5);

            productStore = store1;
            String productLocationString = "Shelf: "+shelfNumber+", Level: "+levelNumber+", Space: "+spaceNumber;
            productLocation = productLocationString;


            return new String[]{productStore,productLocation};

        } catch (Exception e) {
            Log.d("Exception","Raised");
            e.printStackTrace();
            productStore = "N/A";
            productLocation = "N/A";
            return new String[]{productStore,productLocation};
        }
    }
}
