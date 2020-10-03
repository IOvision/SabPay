package com.visionio.sabpay.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.HashMap;

public class CompressedItem {

    Double cost;
    String img;
    int qty;
    String title;
    String unit;

    // below are all static and excluded functions and attributes
    @Exclude
    public static CompressedItem compress(Item item, String inv_id){
        CompressedItem compressedItem = new CompressedItem();
        compressedItem.setTitle(item.getTitle());
        compressedItem.setUnit(item.getUnit());
        compressedItem.setQty(item.getQty());
        if(item.getImages() != null ) {
            if(item.getImages().size() > 0) {compressedItem.setImg(item.getImages().get(0));}
        }
        compressedItem.setCost(item.getCost(inv_id));
        return compressedItem;
    }

    @Exclude
    public Item toItemClass(){
        Item item = new Item();
        item.setQty(qty);
        item.setTitle(title);
        item.setImages(new ArrayList<String >(){{add(img);}});
        item.setUnit(unit);
        item.setCost(new HashMap<String, Double>(){{put("default", cost);}});
        return item;
    }

    // beyond this is functions that are used by firebase so don't mess with them
    public CompressedItem(){}
    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }
    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
