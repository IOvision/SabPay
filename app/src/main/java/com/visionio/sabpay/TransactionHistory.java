package com.visionio.sabpay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Wave;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.models.Transaction;
import com.visionio.sabpay.adapter.TransactionAdapter;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class TransactionHistory extends AppCompatActivity {

    RecyclerView recyclerView;
    TransactionAdapter adapter;
    ProgressBar progressBar;
    BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        recyclerView = findViewById(R.id.transaction_recycler);
        setBottomNavigationView();
        progressBar = findViewById(R.id.transaction_pb);

         Sprite wave = new Wave();
         progressBar.setIndeterminateDrawable(wave);

        adapter = new TransactionAdapter(new ArrayList<Transaction>());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(new SlideInLeftAnimator());
        recyclerView.setAdapter(adapter);


        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            loadTransactions();
        }
    }

    void loadTransactions(){
        FirebaseFirestore.getInstance().collection("user")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("transaction")
                // TODO: check the filter thing
                //.whereEqualTo("type", 0)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot snapshot: queryDocumentSnapshots){
                    Transaction currentTransaction = snapshot.toObject(Transaction.class);

                    // TODO: fix getType thing and test the transaction item


                    if(currentTransaction.getFrom().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        currentTransaction.setSendByMe(true);
                    }else{
                        currentTransaction.setSendByMe(false);
                    }
                    Log.i("Testing", currentTransaction.getId()+">>"+currentTransaction.isSendByMe());
                    currentTransaction.loadUserDataFromReference(adapter);
                    adapter.add(currentTransaction);
                    progressBar.setVisibility(View.GONE);
                }

            }
        }).addOnFailureListener(e -> Log.i("Testing", e.getLocalizedMessage()));
    }

    void setBottomNavigationView(){
        bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_transaction);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.bottom_app_bar_main_transaction : {
                    return true;
                }
                case R.id.bottom_app_bar_main_group : return true;
                case R.id.bottom_app_bar_main_home : {
                    finish();
                    return true;
                }
                case R.id.bottom_app_bar_main_pay : {

                }
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
