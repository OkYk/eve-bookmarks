package com.eve.copilot.dto;

public class LootEntry {

    public static final LootEntry BLANK = new LootEntry(0, "");
    private int quantity;
    private String name;

    public LootEntry(int quantity, String name) {
        this.quantity = quantity;
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
