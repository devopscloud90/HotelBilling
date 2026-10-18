package com.stayledger.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private long id;
    private String number;
    private String invoiceType = "ROOM";
    private String guestName;
    private String phone;
    private String email;
    private String roomNumber;
    private String roomType;
    private Date checkIn;
    private Date checkOut;
    private int guestCount;
    private BigDecimal nightlyRate = BigDecimal.ZERO;
    private int nights;
    private BigDecimal roomSubtotal = BigDecimal.ZERO;
    private BigDecimal extrasSubtotal = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal paid = BigDecimal.ZERO;
    private BigDecimal balance = BigDecimal.ZERO;
    private String paymentMethod;
    private Date issuedOn;
    private Timestamp createdAt;
    private String propertyName;
    private String propertyAddress;
    private String propertyPhone;
    private String propertyEmail;
    private List<Charge> charges = new ArrayList<Charge>();

    public long getId() { return id; }
    public void setId(long v) { id = v; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String v) { invoiceType = v; }
    public String getNumber() { return number; }
    public void setNumber(String v) { number = v; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String v) { guestName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String v) { roomNumber = v; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String v) { roomType = v; }
    public Date getCheckIn() { return checkIn; }
    public void setCheckIn(Date v) { checkIn = v; }
    public Date getCheckOut() { return checkOut; }
    public void setCheckOut(Date v) { checkOut = v; }
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int v) { guestCount = v; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal v) { nightlyRate = v; }
    public int getNights() { return nights; }
    public void setNights(int v) { nights = v; }
    public BigDecimal getRoomSubtotal() { return roomSubtotal; }
    public void setRoomSubtotal(BigDecimal v) { roomSubtotal = v; }
    public BigDecimal getExtrasSubtotal() { return extrasSubtotal; }
    public void setExtrasSubtotal(BigDecimal v) { extrasSubtotal = v; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { subtotal = v; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal v) { discount = v; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal v) { taxRate = v; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal v) { tax = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { total = v; }
    public BigDecimal getPaid() { return paid; }
    public void setPaid(BigDecimal v) { paid = v; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal v) { balance = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { paymentMethod = v; }
    public Date getIssuedOn() { return issuedOn; }
    public void setIssuedOn(Date v) { issuedOn = v; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp v) { createdAt = v; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String v) { propertyName = v; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String v) { propertyAddress = v; }
    public String getPropertyPhone() { return propertyPhone; }
    public void setPropertyPhone(String v) { propertyPhone = v; }
    public String getPropertyEmail() { return propertyEmail; }
    public void setPropertyEmail(String v) { propertyEmail = v; }
    public List<Charge> getCharges() { return charges; }
    public void setCharges(List<Charge> v) { charges = v; }

    public String getStatus() {
        if (balance == null || balance.signum() <= 0) return "Paid";
        return paid != null && paid.signum() > 0 ? "Partially paid" : "Unpaid";
    }
}
