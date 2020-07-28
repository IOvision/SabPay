package com.visionio.sabpay.models;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class Inventory {

    String id; // id of the inventory
    String name; // inventory name, can be name of the store
    List<String> images; // urls of image of store between 0 - 5
    List<String> items;
    DocumentReference owner; // user/id ref of owner of this inventory
    boolean opened; // tells whether the shop/inventory is open or close
    int totalItems; // tells total number of unique items... for example it shop has 2 bananas and 3 apples so totalItems = 2.
    //String location;
    GeoPoint location;

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public Inventory() {
        totalItems = 0;
        opened = false;
        images = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public DocumentReference getOwner() {
        return owner;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public void setOwner(DocumentReference owner) {
        this.owner = owner;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    @Exclude
    public void addImage(String imageUrl){
        images.add(imageUrl);
    }
    public static class Builder{
        private Inventory i = new Inventory();
        public Inventory build(){
            if(!i.getId().equals("") &&
                    !i.getName().equals("") &&
                    i.getOwner()!=null &&
                    i.getTotalItems()>=0 ){
                return i;
            }
            return null;
        }
        public Builder setId(String id){
            i.setId(id);
            return this;
        }
        public Builder setName(String name){
            i.setName(name);
            return this;
        }
        public Builder setOwner(DocumentReference ownerRef){
            i.setOwner(ownerRef);
            return this;
        }
    }
}
