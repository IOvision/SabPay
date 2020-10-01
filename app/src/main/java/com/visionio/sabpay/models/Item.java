package com.visionio.sabpay.models;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Item {


    String id; // item id
    String title; // title of item like apple, mango, milk
    String description; // item related text like company name
    String unit;// Kg/L/Doz/unit
    int qty; // quantity of item
    List<String> images;
    Map<String, Double> cost; //tells cost of item in particular inventory
    Map<String, Boolean> tags; //tells the tags item belong to

    @Exclude
    int cart_qty = 0;

    public Item() {

    }

    public Double getCost(String inventory_id){
        if(inventory_id==null){
            return cost.get("default");
        }
        if (cost.containsKey(inventory_id)){
            return cost.get(inventory_id);
        }
        return cost.get("default");
    }

    public Map<String, Double> getCost() { return cost; }

    public void setCost(Map<String, Double> cost) { this.cost = cost; }

    public Map<String, Boolean> getTags() {
        return tags;
    }

    public void setTags(HashMap<String, Boolean> tags) {
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
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
        i.setImages(this.getImages());
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
