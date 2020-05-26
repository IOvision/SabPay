package com.visionio.sabpay.Models;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.adapter.GroupPayTransactionsAdapter;
import com.visionio.sabpay.adapter.TransactionAdapter;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.logging.Handler;

public class Transaction implements Serializable {

    // TODO: fix the null type on @type

    //server fields
    String id;
    Integer amount;
    DocumentReference from;
    DocumentReference to;
    Timestamp timestamp;
    Integer type;

    //offline mapping
    User user_from;
    User user_to;
    String description;
    boolean isSendByMe; // true means I have send money false means i have received

    public Transaction() {
    }

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }


    public User getUser_from() {
        return user_from;
    }

    public void setUser_from(User user_from) {
        this.user_from = user_from;
    }

    public User getUser_to() {
        return user_to;
    }

    public void setUser_to(User user_to) {
        this.user_to = user_to;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSendByMe() {
        return isSendByMe;
    }

    public void setSendByMe(boolean sendByMe) {
        isSendByMe = sendByMe;
    }

    public String getDate(){
        SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sfd.format(timestamp.toDate());
    }

    public void loadUserDataFromReference(final GroupPayTransactionsAdapter adapter){
        from.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    user_from = task.getResult().toObject(User.class);
                    description = "From: \n"+user_from.getName();
                    adapter.notifyDataSetChanged();
                }else{
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });
    }

    public void loadUserDataFromReference(final TransactionAdapter adapter){

        if(type==1){
            (FirebaseFirestore.getInstance()).document(Utils.getPathToUser("/"+to.getPath())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        user_to = task.getResult().toObject(User.class);
                        description = "Gpay to:\n"+user_to.getName();
                        adapter.notifyDataSetChanged();
                    }else{
                        Log.i("Testing", task.getException().getLocalizedMessage());
                    }
                }
            });
            return;
        }

        if(isSendByMe){
            to.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    user_to = documentSnapshot.toObject(User.class);
                    description = "Sent to:\n"+user_to.getName();
                    adapter.notifyDataSetChanged();
                }
            });
        }else{
            from.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    user_from = documentSnapshot.toObject(User.class);
                    description = "Receiver from:\n"+user_from.getName();
                    adapter.notifyDataSetChanged();
                }
            });
        }


    }
}
