package com.visionio.sabpay.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

import java.util.List;

public class Item {


    String id; // item id
    List<String> inventories;
    String inventory_id; // inventory id to which this items belong
    String title; // title of item like apple, mango, milk
    String description; // item related text like company name
    String unit;// Kg/L/Doz/unit
    int qty; // quantity of item
    double cost; // price of item
    String category;
    List<String> images;

    @Exclude
    int cart_qty = 0;

    public Item() {

    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getInventories() {
        return inventories;
    }

    public void setInventories(List<String> inventories) {
        this.inventories = inventories;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public String getInventory_id() {
        return inventory_id;
    }

    public void setInventory_id(String inventory_id) {
        this.inventory_id = inventory_id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    @Exclude
    public Item copy(){
        Item i = new Item();
        i.setId(this.getId());
        i.setTitle(this.getTitle());
        i.setQty(this.getQty());
        i.setCost(this.getCost());
        i.setDescription(this.getDescription());
        i.setUnit(this.getUnit());
        i.setInventory_id(this.getInventory_id());
        i.setImages(this.getImages());
        i.setCategory(this.getCategory());
        //i.setCart_qty(this.getCart_qty());
        return i;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        try{
            Item comp = (Item)obj;
            assert comp != null;
            return this.getId().equals(comp.getId());
        }catch (Exception e){
            return super.equals(obj);
        }
    }

    @Exclude
    public int getCart_qty() {
        return cart_qty;
    }
    @Exclude
    public void setCart_qty(int cart_qty) {
        this.cart_qty = cart_qty;
    }
}
