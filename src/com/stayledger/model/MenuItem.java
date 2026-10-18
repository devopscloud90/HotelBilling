package com.stayledger.model;

import java.math.BigDecimal;

public class MenuItem {
    private long id;
    private String name;
    private BigDecimal price;
    private String active;

    public MenuItem(long id, String name, BigDecimal price, String active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.active = active;
    }
    public long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getActive() { return active; }
}
