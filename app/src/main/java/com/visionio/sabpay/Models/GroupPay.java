package com.visionio.sabpay.Models;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.adapter.GroupPayTransactionsAdapter;

import java.util.ArrayList;
import java.util.List;

public class GroupPay {

    String id;
    Integer amount;
    Boolean active;
    Integer parts;
    Timestamp timestamp;
    List<Integer> ledger;

    // transactions object
    String path;
    FirebaseFirestore mRef;
    GroupPayTransactionsAdapter adapter;
    RecyclerView recyclerView;

    public GroupPay() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getParts() {
        return parts;
    }

    public void setParts(Integer parts) {
        this.parts = parts;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public List<Integer> getLedger() {
        return ledger;
    }

    public void setLedger(List<Integer> ledger) {
        this.ledger = ledger;
    }

    public void setRecyclerView(RecyclerView recyclerView){
        this.recyclerView = recyclerView;
    }

    public void loadTransaction(Context context){
        mRef = FirebaseFirestore.getInstance();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(false);

        adapter = new GroupPayTransactionsAdapter(new ArrayList<Transaction>());

        recyclerView.setAdapter(adapter);

        Log.i("Testing", path+id+"/transactions");

        mRef.collection(path+id+"/transactions").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for(DocumentSnapshot snapshot: task.getResult()){
                    Transaction transaction = snapshot.toObject(Transaction.class);
                    adapter.add(transaction);
                }
            }
        });

    }

}
