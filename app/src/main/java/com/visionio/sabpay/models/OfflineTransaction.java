package com.visionio.sabpay.models;

import com.google.firebase.Timestamp;
import com.visionio.sabpay.adapter.TransactionAdapter;

import java.text.SimpleDateFormat;

public class OfflineTransaction {

    String id;
    Integer amount;
    User from;
    User to;
    Timestamp timestamp;
    String date;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public User getFrom() {
        return from;
    }

    public void setFrom(User from) {
        this.from = from;
    }

    public User getTo() {
        return to;
    }

    public void setTo(User to) {
        this.to = to;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        setDate(sfd.format(timestamp.toDate()));

    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void getUserFromReference(final TransactionAdapter adapter){
        /*from.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                user_from = documentSnapshot.toObject(User.class);
                adapter.notify();
            }
        });
        to.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                user_from = documentSnapshot.toObject(User.class);
                adapter.notify();
            }
        });*/
        adapter.notifyDataSetChanged();
    }

}
