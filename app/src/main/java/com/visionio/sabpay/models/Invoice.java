package com.visionio.sabpay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;

public class Invoice {

    @Exclude
    public final static String STR_ITEM_COUNT = "Item Count:";
    @Exclude
    public final static String STR_ENTITY_COUNT = "Entity Count:";
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
    float base_amount;
    float discount;
    float total_amount;
    Timestamp timestamp;
    DocumentReference transaction;
    Promotions promo;

    public Invoice() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getBase_amount() {
        return base_amount;
    }

    public void setBase_amount(float base_amount) {
        this.base_amount = base_amount;
    }

    public float getDiscount() {
        return discount;
    }

    public void setDiscount(float discount) {
        this.discount = discount;
    }

    public float getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(float total_amount) {
        this.total_amount = total_amount;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public DocumentReference getTransaction() {
        return transaction;
    }

    public void setTransaction(DocumentReference transaction) {
        this.transaction = transaction;
    }

    public Promotions getPromo() {
        return promo;
    }

    public void setPromo(Promotions promo) {
        this.promo = promo;
    }
}
