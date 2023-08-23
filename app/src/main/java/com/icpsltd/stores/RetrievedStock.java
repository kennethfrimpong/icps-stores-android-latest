package com.icpsltd.stores;

public class RetrievedStock {
    private String name;
    private String description;

    private Integer id;
    private Integer quantity;
    private String location;
    private String store;

    private String unit;

    public RetrievedStock(Integer product_id, String product_name, String product_description, Integer product_quantity, String product_location, String product_store, String unit){
        this.name = product_name;
        this.description = product_description;
        this.quantity = product_quantity;
        this.location = product_location;
        this.store = product_store;
        this.id = product_id;
        this.unit = unit;
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

    public String getLocation() {
        return location;
    }

    public String getStore() {
        return store;
    }

    public String getUnit(){
        return this.unit;
    }

}
