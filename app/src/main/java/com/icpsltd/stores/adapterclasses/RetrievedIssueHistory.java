package com.icpsltd.stores.adapterclasses;

public class RetrievedIssueHistory {
    private String name;
    private String description;

    private Integer id;
    private Integer quantity;
    private String department;
    private String store;
    private String date;

    private String location;

    private String unit;

    public RetrievedIssueHistory(Integer product_id, String product_name, String product_description, Integer product_quantity, String receiver_department, String product_store, String product_location, String issue_date, String unit){
        this.name = product_name;
        this.description = product_description;
        this.quantity = product_quantity;
        this.department = receiver_department;
        this.store = product_store;
        this.location = product_location;
        this.id = product_id;
        this.date = issue_date;
        this.unit = unit;
    }

    public String getUnit(){
        return unit;
    }

    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }



    public Integer getQuantity() {
        return quantity;
    }

    public Integer getID(){
        return id;
    }

    public String getDepartment() {
        return department;
    }

    public String getStore() {
        return store;
    }

    public String getDate(){
        return date;
    }

    public String getLocation(){
        return location;
    }

}
