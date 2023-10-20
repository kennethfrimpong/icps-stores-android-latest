package com.icpsltd.stores.adapterclasses;

public class RetrievedProductMain {
    private String name;
    private Integer id;
    private String department;
    private String date;

    private String quantity;

    private String type;

    private String staffName;

    private String issuerName;

    private String additionType;

    private String bookNumber;

    private String balance;
    private String transactionTime;

    private String unit;

    public RetrievedProductMain(Integer transactionID, String type, String receiverName, String receiverDepartment, String transactionDate, String productQuantity, String staffName, String issuerName, String additionType, String bookNumber, String balance, String transactionTime, String unit){
        this.name = receiverName;
        this.department = receiverDepartment;
        this.id = transactionID;
        this.date = transactionDate;
        this.quantity = productQuantity;
        this.type = type;
        this.issuerName = issuerName;
        this.additionType = additionType;
        this.staffName = staffName;
        this.bookNumber = bookNumber;
        this.balance = balance;
        this.transactionTime = transactionTime;
        this.unit = unit;
    }

    public String getUnit(){
        return unit;
    }

    public String getBookNumber(){
        return bookNumber;
    }

    public String getBalance(){
        return balance;
    }

    public String getTransactionTime(){
        return transactionTime;
    }

    public String getType(){
        return type;
    }

    public String getName() {
        return name;
    }
    public Integer getID(){
        return id;
    }
    public String getDepartment() {
        return department;
    }
    public String getDate(){
        return date;
    }

    public String getQuantity(){
        return quantity;
    }

    public String getIssuerName(){
        return issuerName;
    }

    public String getAdditionType(){
        return additionType;
    }

    public String getStaffName(){
        return staffName;
    }


}
