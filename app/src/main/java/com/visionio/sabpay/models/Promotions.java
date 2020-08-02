package com.visionio.sabpay.models;

import com.google.firebase.firestore.Exclude;

public class Promotions {
    @Exclude
    public final static int FLAT_DISCOUNT = 100;
    @Exclude
    public final static int PERCENTAGE_DISCOUNT = 101;

    int code; // promo code
    String data; // description of promo
    String tAndC; // constraints
    double value; // percent or flat off
    int type; // @FLAT_DISCOUNT AND PERCENTAGE_DISCOUNT

    public Promotions() {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String gettAndC() {
        return tAndC;
    }

    public void settAndC(String tAndC) {
        this.tAndC = tAndC;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
