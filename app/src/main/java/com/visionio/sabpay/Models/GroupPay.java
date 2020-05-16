package com.visionio.sabpay.Models;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.model.Document;
import com.visionio.sabpay.adapter.GroupPayTransactionsAdapter;

import java.util.ArrayList;
import java.util.List;

public class GroupPay {

    String id;
    Integer amount;
    DocumentReference from;
    DocumentReference to;
    Boolean active;
    Integer parts;
    Timestamp timestamp;
    List<Integer> ledger;

    // transactions object
    FirebaseFirestore mRef;
    GroupPayTransactionsAdapter adapter;
    RecyclerView recyclerView;

    // persistent objects
    boolean isTransactionLoaded = false;

    public GroupPay() {
    }

    public String getId() {
        return id;
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

    public void loadTransaction(Context context, final ProgressBar progressBar){
        if(isTransactionLoaded){
            return;
        }

        mRef = FirebaseFirestore.getInstance();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL));

        adapter = new GroupPayTransactionsAdapter(new ArrayList<Transaction>());

        recyclerView.setAdapter(adapter);


        String path = "user/"+ FirebaseAuth.getInstance().getUid()+"/group_pay/meta-data/transaction/"+id+"/transaction";

        mRef.collection(path).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for(DocumentSnapshot snapshot: task.getResult()){
                    Transaction transaction = snapshot.toObject(Transaction.class);
                    transaction.loadUserDataFromReference(adapter);
                    adapter.add(transaction);
                    progressBar.setVisibility(View.GONE);
                    isTransactionLoaded = true;
                }
            }
        });

    }

}
