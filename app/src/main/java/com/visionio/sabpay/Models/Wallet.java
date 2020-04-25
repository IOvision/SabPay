package com.visionio.sabpay.Models;

import com.google.firebase.firestore.DocumentReference;

public class Wallet {

    Integer balance;
    DocumentReference lastTransaction;

    public Wallet() {
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public DocumentReference getLastTransaction() {
        return lastTransaction;
    }

    public void setLastTransaction(DocumentReference lastTransaction) {
        this.lastTransaction = lastTransaction;
    }
}
