package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.visionio.sabpay.Models.User;

public class Pay extends AppCompatActivity {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    DatabaseReference sReference, rReference;
    ImageView back;
    EditText et_number, et_amount;
    Button btn_pay;
    User sender;
    ProgressBar progressBar;
    ValueEventListener sListener, rListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);

        sReference = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getPhoneNumber());

        back = findViewById(R.id.back);
        et_number = findViewById(R.id.et_reciever);
        et_amount = findViewById(R.id.et_amount);
        btn_pay = findViewById(R.id.btn_pay_pay);
        progressBar = findViewById(R.id.payProgress);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Pay.this, MainActivity.class));
            }
        });

        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                final int amount = Integer.parseInt(et_amount.getText().toString());
                String number = "+91";
                number = number.concat(et_number.getText().toString());
                rReference = FirebaseDatabase.getInstance().getReference().child("users").child(number);
                rListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User receiver = dataSnapshot.getValue(User.class);
                        if ((sender.getBalance() - amount) >= 0){
                            sender.decBalance(amount);
                            receiver.incBalance(amount);
                            FirebaseDatabase.getInstance().getReference().child("users").child(sender.getPhoneNumber()).setValue(sender);
                            FirebaseDatabase.getInstance().getReference().child("users").child(receiver.getPhoneNumber()).setValue(receiver);
                            Toast.makeText(Pay.this, "Payment Successful!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Pay.this, "Not Enough Balance", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w("Data:", "loadPost:onCancelled", databaseError.toException());
                        Toast.makeText(Pay.this, "Loading Data Failed!", Toast.LENGTH_SHORT).show();
                    }
                };
                rReference.addListenerForSingleValueEvent(rListener);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                sender = dataSnapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Data:", "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(Pay.this, "Loading Data Failed!", Toast.LENGTH_SHORT).show();
            }
        };
        sReference.addListenerForSingleValueEvent(sListener);

    }
}
