package com.visionio.sabpay.Models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class Transaction {

    String id;
    DocumentReference from;
    DocumentReference to;
    Timestamp timestamp;

    public Transaction() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DocumentReference getFrom() {
        return from;
    }

    public void setFrom(DocumentReference from) {
        this.from = from;
    }

    public DocumentReference getTo() {
        return to;
    }

    public void setTo(DocumentReference to) {
        this.to = to;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
