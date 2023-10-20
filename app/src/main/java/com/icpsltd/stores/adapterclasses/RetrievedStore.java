package com.icpsltd.stores.adapterclasses;

public class RetrievedStore {
    private String storeName;
    private String storeLocation;

    private Integer id;
    private String storeDescription;

    public RetrievedStore(Integer id, String storeNamex, String storeLocationx, String storeDescriptionx){
        this.id = id;
        this.storeName = storeNamex;
        this.storeLocation = storeLocationx;
        this.storeDescription = storeDescriptionx;
    }

    public Integer getId() {
        return id;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getStoreLocation() {
        return storeLocation;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

}
