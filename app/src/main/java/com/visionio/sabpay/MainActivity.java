package com.visionio.sabpay;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.authentication.Authentication;
import com.visionio.sabpay.payment.Pay;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore mRef;

    TextView nameTv;
    TextView balanceTv;

    Button payBtn;
    Button signOutBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUp();

    }

    void setUp(){
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        nameTv = findViewById(R.id.main_activity_name_tV);
        balanceTv = findViewById(R.id.main_activity_balance_tV);
        payBtn = findViewById(R.id.main_activity_pay_btn);
        signOutBtn = findViewById(R.id.main_activity_signOut_btn);

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
        }

    }

    void loadDataFromServer(){
        mRef.collection("user").document(mAuth.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                User user = documentSnapshot.toObject(User.class);
                nameTv.setText(user.getName());
            }
        });

        mRef.collection("user").document(mAuth.getUid())
                .collection("wallet").document("wallet").addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                Wallet wallet = documentSnapshot.toObject(Wallet.class);
                balanceTv.setText(wallet.getBalance().toString());
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
