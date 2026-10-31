package com.packpilot.trip.model;

public class PackedItem {

    private String id;
    private String category;
    private String name;
    private int quantity;
    private String note;
    private boolean checked;

    public PackedItem() {
    }

    public PackedItem(String id, String category, String name, int quantity, String note, boolean checked) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.quantity = quantity;
        this.note = note;
        this.checked = checked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
