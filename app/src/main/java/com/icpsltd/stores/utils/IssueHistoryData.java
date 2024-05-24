package com.icpsltd.stores.utils;

public class IssueHistoryData {
    //                            String historysql = "{\"isFirst\":\""+isFirst+"\",\"isLast\":\""+isLast+"\",\"new_quantity\":\""+new_quantity+"\",\"new_lastID\":\""+updatedID+"\",\"old_quantity\":\""+localquantity+"\",\"type\":\"sync_issue\",\"condition\":\"sync_history\",\"TransactionID\":\""+TransactionID+"\",\"id\":\""+idx+"\",\"ProductName\":\""+ProductName+"\",\"ProductDesc\":\""+ProductDesc+"\",\"ProductQuantity\":\""+ProductQuantity+"\",\"ProductStore\":\""+ProductStore+"\",\"ProductLocation\":\""+ProductLocation+"\",\"IssuerID\":\""+IssuerID+"\",\"IssuerName\":\""+IssuerName+"\",\"ReceiverID\":\""+ReceiverID+"\",\"ReceiverName\":\""+ReceiverName+"\",\"ReceiverDepartment\":\""+ReceiverDept+"\",\"TransactionDate\":\""+TransactionDate+"\",\"TransactionTime\":\""+TransactionTime+"\",\"BookNumber\":\""+BookNumber+"\",\"TransactionType\":\"Stock Issue\",\"Balance\":\""+balance+"\",\"ProductUnit\":\""+unit+"\",\"JobNumber\":\""+jobnumber+"\"}";
    private Boolean isFirst;
    private Boolean isLast;
    private Float newQuantity;
    private Long newLastID;
    private Float oldQuantity;
    private String type;
    private String condition;
    private Long transactionID;
    private String id;
    private String productName;
    private String productDesc;
    private Float productQuantity;
    private String productStore;
    private String productLocation;
    private Long issuerID;
    private String issuerName;
    private Long receiverID;
    private String receiverName;
    private String receiverDepartment;
    private String transactionDate;
    private String transactionTime;
    private String bookNumber;
    private String transactionType;
    private Float balance;
    private String productUnit;
    private String jobNumber;

    public IssueHistoryData() {
    }

    public Boolean getFirst() {
        return isFirst;
    }

    public void setFirst(Boolean first) {
        isFirst = first;
    }

    public Boolean getLast() {
        return isLast;
    }

    public void setLast(Boolean last) {
        isLast = last;
    }

    public Float getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Float newQuantity) {
        this.newQuantity = newQuantity;
    }

    public Long getNewLastID() {
        return newLastID;
    }

    public void setNewLastID(Long newLastID) {
        this.newLastID = newLastID;
    }

    public Float getOldQuantity() {
        return oldQuantity;
    }

    public void setOldQuantity(Float oldQuantity) {
        this.oldQuantity = oldQuantity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Long getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(Long transactionID) {
        this.transactionID = transactionID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDesc() {
        return productDesc;
    }

    public void setProductDesc(String productDesc) {
        this.productDesc = productDesc;
    }

    public Float getProductQuantity() {
        return productQuantity;
    }

    public void setProductQuantity(Float productQuantity) {
        this.productQuantity = productQuantity;
    }

    public String getProductStore() {
        return productStore;
    }

    public void setProductStore(String productStore) {
        this.productStore = productStore;
    }

    public String getProductLocation() {
        return productLocation;
    }

    public void setProductLocation(String productLocation) {
        this.productLocation = productLocation;
    }

    public Long getIssuerID() {
        return issuerID;
    }

    public void setIssuerID(Long issuerID) {
        this.issuerID = issuerID;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public Long getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(Long receiverID) {
        this.receiverID = receiverID;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverDepartment() {
        return receiverDepartment;
    }

    public void setReceiverDepartment(String receiverDepartment) {
        this.receiverDepartment = receiverDepartment;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getBookNumber() {
        return bookNumber;
    }

    public void setBookNumber(String bookNumber) {
        this.bookNumber = bookNumber;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Float getBalance() {
        return balance;
    }

    public void setBalance(Float balance) {
        this.balance = balance;
    }

    public String getProductUnit() {
        return productUnit;
    }

    public void setProductUnit(String productUnit) {
        this.productUnit = productUnit;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }
}
