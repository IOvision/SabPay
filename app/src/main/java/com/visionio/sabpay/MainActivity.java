package com.visionio.sabpay;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.Models.OfflineTransaction;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.authentication.Authentication;
import com.visionio.sabpay.payment.Pay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    FirebaseAuth mAuth;
    FirebaseFirestore mRef;

    TextView balanceTv;

    Button payBtn;
    Button signOutBtn;

    RecyclerView recyclerView;
    TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUp();

    }

    void setUp(){
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        balanceTv = findViewById(R.id.main_activity_balance_tV);
        payBtn = findViewById(R.id.main_activity_pay_btn);
        signOutBtn = findViewById(R.id.main_activity_signOut_btn);
        recyclerView = findViewById(R.id.main_activity_transactions_rv);

        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                startActivity(new Intent(MainActivity.this, Authentication.class));
                finish();
            }
        });

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Pay.class));
            }
        });

        if(mAuth.getCurrentUser() != null){
            loadDataFromServer();
            loadTransactions();
        }

        adapter = new TransactionAdapter(new ArrayList<OfflineTransaction>());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(adapter);

    }

    void loadTransactions(){
        mRef.collection("user").document(mAuth.getUid()).collection("transaction")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot snapshot: queryDocumentSnapshots){
                    Transaction currentTransaction = snapshot.toObject(Transaction.class);
                    mapOfflineTransaction(currentTransaction);
                }
            }
        });
    }

    void mapOfflineTransaction(final Transaction transaction){
        final OfflineTransaction offlineTransaction = new OfflineTransaction();

        offlineTransaction.setId(transaction.getId());
        offlineTransaction.setAmount(transaction.getAmount());
        offlineTransaction.setTimestamp(transaction.getTimestamp());

        transaction.getFrom().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                offlineTransaction.setFrom(documentSnapshot.toObject(User.class));
                transaction.getTo().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        offlineTransaction.setTo(documentSnapshot.toObject(User.class));
                        adapter.add(offlineTransaction);
                    }
                });
            }
        });

    }

    void loadDataFromServer(){
        mRef.collection("user").document(mAuth.getUid())
                .collection("wallet").document("wallet").addSnapshotListener(MainActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                Wallet wallet = documentSnapshot.toObject(Wallet.class);
                balanceTv.setText("Rs."+wallet.getBalance().toString());
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, Authentication.class);
            startActivity(intent);
            finish();
        }
    }

}
