package com.visionio.sabpay.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Cart {
    private Map<String, Integer> quantity;
    private List<Item> itemList;
    private String inv_id;

    public Cart(String inv_id) {
        quantity = new HashMap<>();
        itemList = new ArrayList<>();
        this.inv_id = inv_id;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public int getQuantity(String item_id) {
        return quantity.get(item_id);
    }

    public Map<String, Integer> getQuantity() {
        return quantity;
    }

    public void addItem(Item item) {
        if(itemList.contains(item)) {
            quantity.put(item.getId(), quantity.get(item.getId()) + 1);
        } else {
            quantity.put(item.getId(), 1);
            itemList.add(item);
        }
    }

    public void decreaseItem(Item item) {
        if(quantity.get(item.getId()) == 1) {
            itemList.remove(item);
            quantity.remove(item.getId());
        } else {
            quantity.put(item.getId(), quantity.get(item.getId()) - 1);
        }
    }

    public int getItemCount() {
        int i = 0;
        for(Item item: itemList) i += quantity.get(item.getId());
        return i;
    }

    public List<CompressedItem> getCompressedItem(){
        List<CompressedItem> compressedItems = new ArrayList<>();
        for(Item it: itemList){
            CompressedItem item = CompressedItem.compress(it, inv_id);
            item.setQty(getQuantity(it.getId()));
            compressedItems.add(item);
        }
        return compressedItems;
    }
    

    public double getAmount() {
        double cost = 0;
        for(Item item: itemList) cost += getQuantity(item.getId()) * item.getCost(inv_id);
        return cost;
    }

}