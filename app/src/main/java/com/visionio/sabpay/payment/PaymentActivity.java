package com.visionio.sabpay.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.Payment;
import com.visionio.sabpay.models.Transaction;
import com.visionio.sabpay.models.Wallet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity implements Payment.PaymentListener {

    TextView name_tv, transactionId,transactionTime;
    MaterialToolbar materialToolbar;
    TextInputLayout textInputLayout;
    ProgressBar progressBar;
    RelativeLayout transactionDetail;
    EditText amount_et;
    Button send;
    Integer amount, initialWalletAmount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mRef;
    private DocumentReference senderDocRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        textInputLayout = findViewById(R.id.payment_activity_amount);
        name_tv = findViewById(R.id.payment_receiver_name);
        amount_et = findViewById(R.id.payment_activity_amount_et);
        send = findViewById(R.id.payment_activity_pay);
        transactionDetail = findViewById(R.id.transaction_detail);
        transactionId = findViewById(R.id.transaction_id);
        transactionTime = findViewById(R.id.transaction_time);
        materialToolbar = findViewById(R.id.payment_top_bar);
        progressBar = findViewById(R.id.transaction_progress);

        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        senderDocRef = mRef.collection("user").document(mAuth.getUid());

        send.setOnClickListener(v -> {
            if (Payment.getInstance().isSuccessful()){
                finish();
            } else {
                amount = Integer.parseInt(amount_et.getText().toString());
                if (amount >= 0){
                    initiatePayToUser();
                } else {
                    amount_et.setError("Amount cannot be empty");
                }
            }
        });

        name_tv.setText(Payment.getInstance().getName());
    }

    void initiatePayToUser(){
        send.setVisibility(View.INVISIBLE);
        textInputLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        senderDocRef.collection("wallet").document("wallet").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    Wallet wallet = task.getResult().toObject(Wallet.class);
                    initialWalletAmount = wallet.getBalance();
                    if(amount > wallet.getBalance()){
                        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getApplicationContext());
                        alert.setTitle("Not Enough Balance.")
                                .setPositiveButton("Accept", (dialog, which) -> {
                                }).show();
                    }else {
                        payToUserUsingCloudFunction();
                    }
                }else{
                    Toast.makeText(PaymentActivity.this, "Error fetching wallet data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void payToUserUsingCloudFunction(){
        final Transaction transaction = new Transaction();
        transaction.setId(senderDocRef.collection("transaction").document().getId());
        transaction.setFrom(senderDocRef);
        transaction.setTo(Payment.getInstance().getReceiverDocRef());
        transaction.setAmount(amount);
        transaction.setTimestamp(new Timestamp(new Date()));

        Map<String, Object> transactionMap = new HashMap<String, Object>(){{
            put("id", transaction.getId());
            put("type", 0);
            put("amount", transaction.getAmount());
            put("from", senderDocRef);
            put("to", Payment.getInstance().getReceiverDocRef());
            put("timestamp", new Timestamp(new Date()));
        }};

        final ListenerRegistration[] lr = {null};


        senderDocRef.collection("pending_transaction").document("transaction")
                .set(transactionMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    lr[0] = senderDocRef.collection("wallet").document("wallet").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                            Wallet wallet = documentSnapshot.toObject(Wallet.class);
                            if(wallet.getBalance() == initialWalletAmount-amount){
                                //paymentHandler.setBalance(wallet.getBalance());
                                lr[0].remove();
                            }
                        }
                    });

                    //paymentHandler.setTransactionId(transaction.getId());
                    SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    //paymentHandler.setDate(sfd.format(transaction.getTimestamp().toDate()));
                    setTransactionDetails(transaction);

                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setSuccess();
                        }
                    }, 5000);
                } else {
                    Toast.makeText(PaymentActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setSuccess() {
        progressBar.setVisibility(View.GONE);
        transactionDetail.setVisibility(View.VISIBLE);
        materialToolbar.setTitle("Payment Successful");
        send.setText("Done");
        send.setVisibility(View.VISIBLE);
        Payment.getInstance().setSuccess();
    }

    private void setTransactionDetails(Transaction transaction) {
        transactionId.setText(transaction.getId());
        SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        transactionTime.setText(sfd.format(transaction.getTimestamp().toDate()));
    }
}
