package com.stayledger.model;

import java.math.BigDecimal;

public class Charge {
    private long id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private long menuItemId;

    public Charge(long id, String description, BigDecimal amount) {
        this(id, description, BigDecimal.ONE, amount, amount);
    }

    public Charge(long id, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        this(id, description, quantity, unitPrice, amount, 0);
    }

    public Charge(long id, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, long menuItemId) {
        this.id = id;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
        this.menuItemId = menuItemId;
    }

    public long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public long getMenuItemId() { return menuItemId; }
}
