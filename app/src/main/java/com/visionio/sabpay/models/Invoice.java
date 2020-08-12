package com.visionio.sabpay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.List;

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
    double base_amount;
    double discount;
    double total_amount;
    Timestamp timestamp;
    String transaction;
    Promotions promo;
    List<Item> items;

    public Invoice() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getBase_amount() {
        return base_amount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void setBase_amount(double base_amount) {
        this.base_amount = base_amount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(double total_amount) {
        this.total_amount = total_amount;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Promotions getPromo() {
        return promo;
    }

    public void setPromo(Promotions promo) {
        this.promo = promo;
        applyPromo();
    }

    @Exclude
    public void setAmounts(double base_amount, double discountPercent){
        this.base_amount = base_amount;
        this.discount = discountPercent;
        this.total_amount= base_amount - (base_amount*discountPercent/100.0);
    }

    @Exclude
    public static Invoice fromItems(List<Item> items){
        Invoice invoice = new Invoice();
        invoice.setAmounts(Utils.getBaseAmount(items), 0);
        invoice.setItems(items);
        return invoice;
    }

    @Exclude
    private void applyPromo(){
        if (promo==null){
            return;
        }
        if(promo.getType()==Promotions.FLAT_DISCOUNT){
            discount = (promo.getValue()/base_amount)*100;
            if(discount>100){
                discount = 100;
            }
        }else if (promo.getType()==Promotions.PERCENTAGE_DISCOUNT){
            setAmounts(base_amount, promo.value);
        }
    }
}
