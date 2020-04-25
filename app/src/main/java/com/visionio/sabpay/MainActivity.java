package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.visionio.sabpay.Models.User;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button btn_signout, btn_pay;
    TextView tv_name, tv_balance;
    FirebaseUser mUser;
    DatabaseReference sReference;
    ValueEventListener sListener;
    FirebaseAuth mAuth;
    ProgressBar balanceProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, Authentication.class);
            startActivity(intent);
        }
        balanceProgress = findViewById(R.id.balanceProgress);
        btn_signout = findViewById(R.id.btn_signout);
        tv_name = findViewById(R.id.name);
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        tv_balance = findViewById(R.id.tv_balance);
        btn_pay = findViewById(R.id.btn_pay);

        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Pay.class));
            }
        });

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null){
            sReference = FirebaseDatabase.getInstance().getReference().child("users").child(Objects.requireNonNull(mAuth.getCurrentUser().getPhoneNumber()));
            String name = "Hello, ";
            name = name.concat(mUser.getDisplayName());
            tv_name.setText(name);
        }


        btn_signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                checkAuth();
            }
        });

    }

    void checkAuth(){
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, Authentication.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        /*sListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User sender = dataSnapshot.getValue(User.class);
                String balance = "\u20B9 ";
                balance = balance.concat(Integer.toString(sender.getBalance()));
                tv_balance.setText(balance);
                balanceProgress.setVisibility(View.INVISIBLE);
                tv_balance.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Data:", "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(MainActivity.this, "Loading Data Failed!", Toast.LENGTH_SHORT).show();
            }
        };*/
        //sReference.addListenerForSingleValueEvent(sListener);

    }
}
