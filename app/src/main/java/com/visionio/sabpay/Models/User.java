package com.visionio.sabpay.Models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {
    private String phoneNumber;
    private int balance;

    public User(String phoneNumber, int balance) {
        this.phoneNumber = phoneNumber;
        this.balance = balance;
    }

    public User() {

    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getBalance() {
        return balance;
    }

    public void incBalance(int amount){
        this.balance += amount;
    }

    public void decBalance(int amount){
        this.balance -= amount;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("phoneNumber", phoneNumber);
        result.put("balance", balance);
        return result;
    }
}
