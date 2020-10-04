package com.visionio.sabpay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.List;

public class Invoice {

    @Exclude
    public final static String STR_ITEM_COUNT = "Item Count:";
    @Exclude
    public final static String STR_ENTITY_COUNT = "Total item Count:";
    @Exclude
    public final static String STR_ORDER_FROM = "Order From:";
    @Exclude
    public final static String STR_BASE_AMOUNT = "Base Amount:";
    @Exclude
    public final static String STR_DELIVERY_CHARGE = "Delivery Charge:";
    @Exclude
    public final static String STR_TOTAL = "Total:";
    @Exclude
    public final static String STR_DISCOUNT = "Discount:";
    @Exclude
    public final static String STR_PAYABLE_AMOUNT = "Payable Amount:";

    String id;
    double amount;
    Timestamp timestamp;
    String transactionId;
    List<CompressedItem> items;

    public Invoice() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CompressedItem> getItems() {
        return items;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setItems(List<CompressedItem> items) {
        this.items = items;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransaction(String transactionId) {
        this.transactionId = transactionId;
    }

    @Exclude
    public static Invoice getInstance(List<CompressedItem> items){
        Invoice invoice = new Invoice();
        double amount = 0.0;
        for(CompressedItem item: items){
            amount += item.getCost();
        }
        invoice.setAmount(amount);
        invoice.setItems(items);
        return invoice;
    }

}
