package com.visionio.sabpay.Models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class Transaction {

    DocumentReference from;
    DocumentReference to;
    Timestamp timestamp;

    public Transaction() {
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
