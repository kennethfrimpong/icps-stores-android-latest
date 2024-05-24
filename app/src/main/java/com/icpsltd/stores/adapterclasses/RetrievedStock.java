package com.icpsltd.stores.adapterclasses;

public class RetrievedStock {
    private String name;
    private String description;

    private String id;
    private Float quantity;
    private String location;
    private String store;

    private String unit;

    private String additionID;

    private String imageAvailable;

    public RetrievedStock(String itemCode, String product_name, String product_description, Float product_quantity, String product_location, String product_store, String unit, String AdditionID, String imageAvailable){
        this.name = product_name;
        this.description = product_description;
        this.quantity = product_quantity;
        this.location = product_location;
        this.store = product_store;
        this.id = itemCode;
        this.unit = unit;
        this.additionID = AdditionID;
        this.imageAvailable = imageAvailable;
    }

    public String getImageAvailable(){
        return this.imageAvailable;
    }

    public String getAdditionID(){
        return this.additionID;
    }

    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }



    public Float getQuantity() {
        return quantity;
    }

    public String getID(){
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
