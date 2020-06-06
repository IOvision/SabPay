package com.visionio.sabpay.payment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.TransactionStatusAdapter;
import com.visionio.sabpay.interfaces.Payment;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.Transaction;
import com.visionio.sabpay.models.Wallet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    TextView name_tv, balance_tv;
    CircularImageView avatar;
    MaterialToolbar materialToolbar;
    TextInputLayout textInputLayout;
    ProgressBar progressBar;
    EditText amount_et;
    Button send;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mRef;
    private DocumentReference senderDocRef;

    LinearLayout balanceHeader;

    RecyclerView recyclerView;
    TransactionStatusAdapter adapter;

    List<Contact> payee;

    Wallet wallet;

    int[] uiManipulator = {1, 0, 0};
    // represents 3 phases of ui. 1 means phase[i] is visible & vice-versa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        textInputLayout = findViewById(R.id.payment_activity_amount);
        name_tv = findViewById(R.id.payment_receiver_name);
        amount_et = findViewById(R.id.payment_activity_amount_et);
        send = findViewById(R.id.payment_activity_pay);
        materialToolbar = findViewById(R.id.payment_top_bar);

        avatar = findViewById(R.id.payment_receiver_avatar);
        balance_tv = findViewById(R.id.payment_activity_balance_tv);
        balanceHeader = findViewById(R.id.payment_activity_wallet_header_ll);

        progressBar = findViewById(R.id.payment_activity_transaction_progress);

        recyclerView = findViewById(R.id.payment_activity_transactionStatus_rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TransactionStatusAdapter(new ArrayList<>());

        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        payee = Payment.getInstance().getPayee();

        senderDocRef = mRef.collection("user").document(mAuth.getUid());

        name_tv.setText("Paying to "+payee.size());


        fetchWallet();

        send.setOnClickListener((View v) -> {
            updateUi(0, 0, 1);
        });

        amount_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Integer amount;
                if(s.toString().trim().equals("")){
                    amount = 0;
                }else{
                    amount = Integer.parseInt(s.toString().trim());
                }
                int total = amount*payee.size();
                if(total>wallet.getBalance()){
                    amount_et.setError("Insufficient Balance");
                }else{
                    balance_tv.setText(""+(wallet.getBalance()-total));
                }
            }
        });

    }

    void initiatePayToUser(){
        send.setVisibility(View.INVISIBLE);
        textInputLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

    }

    void fetchWallet(){
        senderDocRef.collection("wallet").document("wallet").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    wallet = task.getResult().toObject(Wallet.class);
                    updateUi(0, 1, 0); //moving from p1 to p2
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
        //transaction.setTo(Payment.getInstance().getReceiverDocRef());
        //transaction.setAmount(amount);
        transaction.setTimestamp(new Timestamp(new Date()));

        Map<String, Object> transactionMap = new HashMap<String, Object>(){{
            put("id", transaction.getId());
            put("type", 0);
            put("amount", transaction.getAmount());
            put("from", senderDocRef);
            //put("to", Payment.getInstance().getReceiverDocRef());
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
                            /*if(wallet.getBalance() == initialWalletAmount-amount){
                                //paymentHandler.setBalance(wallet.getBalance());
                                lr[0].remove();
                            }*/
                        }
                    });

                    //paymentHandler.setTransactionId(transaction.getId());
                    SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    //paymentHandler.setDate(sfd.format(transaction.getTimestamp().toDate()));

                } else {
                    Toast.makeText(PaymentActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void updateUi(int phase_1, int phase_2, int phase_3){
        if(uiManipulator[0]!=phase_1){
            uiManipulator[0] = phase_1;
            updatePhase_1();
        }
        if(uiManipulator[1]!=phase_2){
            uiManipulator[1] = phase_2;
            updatePhase_2();
        }
        if(uiManipulator[2]!=phase_3){
            uiManipulator[2] = phase_3;
            updatePhase_3();
        }
    }

    void updatePhase_1(){
        int visibility = uiManipulator[0];
        if(visibility==0){
            visibility = View.GONE;
        }else if(visibility==1){
            visibility = View.VISIBLE;
        }
        progressBar.setVisibility(visibility);
        balance_tv.setText(wallet.getBalance().toString());
    }

    void updatePhase_2(){
        int visibility = uiManipulator[1];
        if(visibility==0){
            visibility = View.GONE;
        }else if(visibility==1){
            visibility = View.VISIBLE;
        }
        avatar.setVisibility(visibility);
        name_tv.setVisibility(visibility);
        textInputLayout.setVisibility(visibility);
        send.setVisibility(visibility);
        balanceHeader.setVisibility(visibility);
    }

    void updatePhase_3(){
        int visibility = uiManipulator[2];
        if(visibility==0){
            visibility = View.GONE;
        }else if(visibility==1){
            visibility = View.VISIBLE;
        }
        recyclerView.setVisibility(visibility);
    }
}
