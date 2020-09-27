package com.visionio.sabpay.models;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventory implements Serializable {

    String id; // id of the inventory
    String name; // inventory name, can be name of the store
    List<String> images; // urls of image of store between 0 - 5
    List<String> items; // list of ids of items in this inventory
    GeoPoint location;
    DocumentReference owner; // user/id ref of owner of this inventory
    boolean opened; // tells whether the shop/inventory is open or close
    int totalItems; // tells total number of unique items... for example it shop has 2 bananas and 3 apples so totalItems = 2.
    String address;
    List<String> tags; //tells which tags to be included in inventory

    public Inventory() {
        totalItems = 0;
        opened = false;
        images = new ArrayList<>();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        address = "Kia Park Apartments, Veera Desai Road, Shastri Nagar, Andheri West Mumbai, Maharashtra 400102";
        this.address = address;
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

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
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

    public String getJson(){
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("name", name);
        map.put("images", images);
        map.put("items", items);
        map.put("owner", owner.getId());
        map.put("int", totalItems);

        return new Gson().toJson(map);
    }

    public static Inventory formJson(String json){
        HashMap<String, Object> map = new Gson().fromJson(json, HashMap.class);
        Inventory i = new Inventory();
        i.setId(map.get("id").toString());
        i.setName(map.get("name").toString());
        i.setImages((List<String>)map.get("images"));
        i.setItems((List<String>)map.get("items"));
        i.setOwner(FirebaseFirestore.getInstance().document("user/"+map.get("owner").toString()));
        i.setTotalItems((int)((double) map.get("int")));
        return i;
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
                    i.getTotalItems()>=0 &&
                    i.getLocation()!=null){
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
        public Builder setLocation(GeoPoint loc){
            i.setLocation(loc);
            return this;
        }
    }
}
