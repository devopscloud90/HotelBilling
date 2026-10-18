package com.stayledger.model;

public class HotelSettings {
    private String propertyName = "";
    private String propertyAddress = "";
    private String propertyPhone = "";
    private String propertyEmail = "";

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String value) { propertyName = value == null ? "" : value; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String value) { propertyAddress = value == null ? "" : value; }
    public String getPropertyPhone() { return propertyPhone; }
    public void setPropertyPhone(String value) { propertyPhone = value == null ? "" : value; }
    public String getPropertyEmail() { return propertyEmail; }
    public void setPropertyEmail(String value) { propertyEmail = value == null ? "" : value; }
}
