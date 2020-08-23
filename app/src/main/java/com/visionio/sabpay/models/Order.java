package com.visionio.sabpay.models;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Order {

    public static class STATUS{
        @Exclude
        public final static String ORDER_RECEIVED = "ORDER RECEIVED";
        @Exclude
        public final static String ORDER_PLACED = "ORDER PLACED";
        @Exclude
        public final static String ORDER_DELIVERED = "ORDER DELIVERED";
        @Exclude
        public final static String ORDER_CANCELLED = "ORDER CANCELLED";
        @Exclude
        public final static String ORDER_COMPLETE = "ORDER COMPLETE";
    }

    /*
    To check if payment is done:
    */
    @Exclude
    public Boolean isPaymentDone(){
        return invoiceId != null;
    }

    String orderId;
    List<Item> items = new ArrayList<>();
    Timestamp timestamp;
    String fromInventory;
    String fromInventoryName;
    String status;
    double amount;
    String transactionId;
    String invoiceId;
    Boolean active; // True: invoiceId is null/ payment is not done and status is delivered
    Map<String, String> user;

    //todo: add arguments
    public Order() {

    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void updateActiveState(){
        if(status.equalsIgnoreCase(STATUS.ORDER_COMPLETE)){
            active = false;
            return;
        }
        if(!status.equalsIgnoreCase(STATUS.ORDER_DELIVERED)){
            active = true;
        }else{
            active = !isPaymentDone();
        }
    }

    public String getFromInventoryName() {
        return fromInventoryName;
    }

    public void setFromInventoryName(String fromInventoryName) {
        this.fromInventoryName = fromInventoryName;
    }

    public Map<String, String> getUser() {
        return user;
    }

    public void setUser(Map<String, String> user) {
        this.user = user;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getFromInventory() {
        return fromInventory;
    }

    public void setFromInventory(String fromInventory) {
        this.fromInventory = fromInventory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        updateActiveState();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }
}

