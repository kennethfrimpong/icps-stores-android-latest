package com.icpsltd.stores.model;

import java.io.Serializable;


public class CardDetailsSummary implements Serializable {
    private String fullName;
    private String gender;
    private String dateOfBirth;
    private String expiryDate;
    private String cardNumber;

    private String cccNumber;



    public CardDetailsSummary(CardDetails data) {
        fullName = data.getFullName();
        gender = data.getGender();
       // dateOfBirth = data.getDateOfBirth();
        expiryDate = data.getExpiryDate();
        cardNumber = data.getCardNumber();

    }

//    public CardDetailsSummary(KycResponse kycResponse) {
//        fullName = kycResponse.getFullName();
//        gender = kycResponse.getGender();
//        dateOfBirth = kycResponse.getBirthDate();
//        expiryDate = kycResponse.getCardValidTo();
//        cardNumber = kycResponse.getNationalId();
//    }

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

    public String getCccNumber() {
        return cccNumber;
    }

    public void setCccNumber(String cccNumber) {
        this.cccNumber = cccNumber;
    }

    @Override
    public String toString() {
        return "CardDetails{" +
                "fullName='" + fullName + '\'' +
                ", gender='" + gender + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", cardNumber='" + cardNumber + '\'' +


                '}';
    }
}
