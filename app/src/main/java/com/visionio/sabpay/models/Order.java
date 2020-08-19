package com.visionio.sabpay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Order {

    public static class STATUS{
        @Exclude
        public final static String ORDER_RECEIVED = "ORDER RECEIVED";
        @Exclude
        public final static String ORDER_PLACED = "ORDER PLACED";
        @Exclude
        public final static String ORDER_DELIVERED = "ORDER DELIVERED";
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
    String userId;
    String transactionId;
    String invoiceId;

    //todo: add arguments
    public Order() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }


    public String getFromInventoryName() {
        return fromInventoryName;
    }

    public void setFromInventoryName(String fromInventoryName) {
        this.fromInventoryName = fromInventoryName;
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
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
