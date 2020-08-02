package com.visionio.sabpay.models;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;

import java.sql.Timestamp;
import java.util.ArrayList;

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
    ArrayList<Item> items = new ArrayList<>();
    Timestamp timestamp;
    String fromInventory;
    String status;
    double amount;
    String userId;
    String transactionId;
    DocumentReference invoice;
    String transaction;

    //todo: add arguments
    public Order() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public void setItems(ArrayList<Item> items) {
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

    public DocumentReference getInvoice() {
        return invoice;
    }

    public void setInvoice(DocumentReference invoice) {
        this.invoice = invoice;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

}
