package com.visionio.sabpay.models;

import com.google.firebase.firestore.DocumentReference;

public class Wallet {

    Double balance;
    Integer offPayBalance;
    DocumentReference lastTransaction;

    public Wallet() {
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public DocumentReference getLastTransaction() {
        return lastTransaction;
    }

    public void setLastTransaction(DocumentReference lastTransaction) {
        this.lastTransaction = lastTransaction;
    }
}
