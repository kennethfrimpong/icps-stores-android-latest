package com.icpsltd.stores.adapterclasses;

public class RetrievedStaff {
    private String firstName;
    private String lastName;

    private Integer id;
    private String department;
    private String type;

    private String middleName;

    private String canNumber;

    private String addedBy;
    private String dateAdded;
    private String timeAdded;
    private String accessIDstatus;

    public RetrievedStaff(Integer staff_id, String staff_firstName, String staff_middleName, String staff_lastName, String staff_department, String staff_type, String canNumber, String addedBy, String dateAdded, String timeAdded, String accessIDstatus){
        this.firstName = staff_firstName;
        this.lastName = staff_lastName;
        this.department = staff_department;
        this.type = staff_type;
        this.id = staff_id;
        this.middleName = staff_middleName;
        this.canNumber = canNumber;
        this.accessIDstatus = accessIDstatus;
        this.addedBy = addedBy;
        this.dateAdded = dateAdded;
        this.timeAdded = timeAdded;
    }

    public String getCanNumber(){
        return canNumber;
    }

    public String getfirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getlastName() {
        return lastName;
    }

    public Integer getID() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDepartment() {
        return department;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public String getTimeAdded() {
        return timeAdded;
    }

    public String getAccessIDstatus() {
        return accessIDstatus;
    }

}
