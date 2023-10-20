package com.icpsltd.stores.adapterclasses;

public class RetrievedLocation {
    private String locationName;
    private String locationStore;

    private Integer id;
    private Integer storeID;

    private String locationDescription;

    public RetrievedLocation(Integer id, String locationName, Integer storeID, String locationStore, String locationDescription){
        this.id = id;
        this.locationName = locationName;
        this.storeID = storeID;
        this.locationStore = locationStore;
        this.locationDescription = locationDescription;
    }

    public Integer getId() {
        return id;
    }

    public Integer getStoreID() {
        return storeID;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocationStore() {
        return locationStore;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

}
