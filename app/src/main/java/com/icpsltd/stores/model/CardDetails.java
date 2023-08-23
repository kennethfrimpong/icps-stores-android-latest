package com.icpsltd.stores.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.credenceid.icao.GIdData;
import com.credenceid.icao.GhanaIdCardFpTemplateInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class CardDetails implements Serializable {
    private String fullName;
    private String gender;
    private String dateOfBirth;
    private String expiryDate;
    private String cardNumber;
    private Bitmap cardImage;
    private List<GhanaIdCardFpTemplateInfo> ghanaIdCardFpTemplateInfosList = new ArrayList<>();

    private DateFormat dateFormat3 = new SimpleDateFormat("yyyyMMdd");
    private DateFormat dateFormat4 = new SimpleDateFormat("dd/MM/yyyy");

    public CardDetails(GIdData data) {
        if(data.DG1.getMiddleName().equalsIgnoreCase(""))
            fullName = data.DG1.getGivenName()+" "+data.DG1.getSurName();
        else
            fullName = data.DG1.getGivenName() + " " + data.DG1.getMiddleName() + " " + data.DG1.getSurName();
        gender = data.DG1.getGender();
        dateOfBirth = data.DG5.getDateOfBirth();
        expiryDate = data.DG9.getValidityDate();
        cardNumber = data.DG6.getNin();
        cardImage = data.DG2.getFaceImage();
        ghanaIdCardFpTemplateInfosList = data.DG10.getFingers();
    }

    public CardDetails(JSONObject data){
        try {
            fullName = data.getString("forenames") + data.getString("surname");
            gender = data.getString("gender");
            dateOfBirth = data.getString("birthDate");
            expiryDate = data.getString("cardValidTo");
            cardNumber = data.getString("nationalId");

            JSONObject biometricFeedData = new JSONObject(data.optString("biometricFeed"));
            JSONObject faceData = new JSONObject(biometricFeedData.optString("face"));
            byte[] decodedString = Base64.decode(faceData.getString("data"), Base64.DEFAULT);
            cardImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public CardDetails(String fullName, String gender, String dateOfBirth, String expiryDate, String cardNumber, Bitmap cardImage) {
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.expiryDate = expiryDate;
        this.cardNumber = cardNumber;
        this.cardImage = cardImage;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
//        try {
//            if(dateOfBirth!=null) {
//                dateOfBirth = dateFormat4.format(dateFormat3.parse(dateOfBirth));
//            }
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Bitmap getCardImage() {
        return cardImage;
    }

    public void setCardImage(Bitmap cardImage) {
        this.cardImage = cardImage;
    }

    public List<GhanaIdCardFpTemplateInfo> getGhanaIdCardFpTemplateInfosList() {
        return ghanaIdCardFpTemplateInfosList;
    }

    public void setGhanaIdCardFpTemplateInfosList(List<GhanaIdCardFpTemplateInfo> ghanaIdCardFpTemplateInfosList) {
        this.ghanaIdCardFpTemplateInfosList = ghanaIdCardFpTemplateInfosList;
    }

    @Override
    public String toString() {
        return "CardDetails{" +
                "fullName='" + fullName + '\'' +
                ", gender='" + gender + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", cardImage=" + cardImage +
                ", ghanaIdCardFpTemplateInfosList=" + ghanaIdCardFpTemplateInfosList +
                '}';
    }
}
