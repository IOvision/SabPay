package com.visionio.sabpay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Order {

    @Exclude
    public final static String STATUS_ORDER_PLACED = "ORDER PLACED";
    @Exclude
    public final static String STATUS_PAYMENT_PENDING = "PAYMENT PENDING";
    @Exclude
    public final static String STATUS_ORDER_CANCELLED = "ORDER CANCELLED";
    @Exclude
    public final static String STATUS_ORDER_COMPLETED = "ORDER COMPLETED";


    String orderId;
    List<Item> items = new ArrayList<>();
    Timestamp timestamp;
    String fromInventory;
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
