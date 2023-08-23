package com.icpsltd.stores;

public class RetrievedIssueHistoryMain {
    private String name;
    private Integer id;
    private String department;
    private String date;

    private String time;
    private String bookNumber;

    public RetrievedIssueHistoryMain(Integer transactionID, String receiverName, String receiverDepartment, String transactionDate, String transactionTime, String transactionBookNumber){
        this.name = receiverName;
        this.department = receiverDepartment;
        this.id = transactionID;
        this.date = transactionDate;
        this.time = transactionTime;
        this.bookNumber = transactionBookNumber;
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

    public String getTime(){
        return time;
    }

    public String getBookNumber(){
        return bookNumber;
    }

}
