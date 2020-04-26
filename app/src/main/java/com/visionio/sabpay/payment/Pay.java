package com.visionio.sabpay.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.visionio.sabpay.MainActivity;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Pay extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore mRef;

    ImageView back;
    EditText et_number, et_amount;
    Button btn_pay;

    String phoneNumber = "+91";
    Integer amount = 0;

    Integer initialWalletAmount;

    PaymentHandler paymentHandler;

    DocumentReference receiverDocRef;

    DocumentReference senderDocRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);

        setUp();

    }

    void setUp(){
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        senderDocRef = mRef.collection("user").document(mAuth.getUid());

        back = findViewById(R.id.pay_activity_back_iv);
        et_number = findViewById(R.id.pay_activity_receiverPhone_et);
        et_amount = findViewById(R.id.pay_activity_amount_et);
        btn_pay = findViewById(R.id.pay_activity_pay_btn);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Pay.this, MainActivity.class));
            }
        });

        paymentHandler = new PaymentHandler(this, Pay.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentHandler.showPayStatus();
                initiatePayToUser();
            }
        });

        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateServer();
            }
        });

    }

    void initiateServer(){
        updateVariableData();
        if(amount == 0){
            et_amount.setError("Amount can't be empty");
            return;
        }
        paymentHandler.init();
        searchUser();
    }

    void searchUser(){
        mRef.collection("user").whereEqualTo("phone", phoneNumber).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                    paymentHandler.setLinkedWallet(snapshot.getString("name"));
                    receiverDocRef = snapshot.getReference();

                }else{
                    paymentHandler.showPayStatus();
                    paymentHandler.setError("No wallet linked to this number!!");
                }
            }
        });
    }

    void initiatePayToUser(){
        senderDocRef.collection("wallet").document("wallet").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    Wallet wallet = task.getResult().toObject(Wallet.class);
                    initialWalletAmount = wallet.getBalance();
                    if(amount > wallet.getBalance()){
                        paymentHandler.setError("Insufficient balance\\n");
                        paymentHandler.setBalance(wallet.getBalance());
                    }else {
                        payToUser();
                    }
                }else{
                    paymentHandler.setError("Error fetching wallet data");
                }
            }
        });
    }

    void payToUser(){
        paymentHandler.setSuccess("Performing transaction");
        final DocumentReference receiversTransaction = receiverDocRef.collection("transaction").document();


        final Transaction transaction = new Transaction();
        transaction.setId(receiversTransaction.getId());
        transaction.setFrom(senderDocRef);
        transaction.setTo(receiverDocRef);
        transaction.setAmount(amount);

        mRef.runTransaction(new com.google.firebase.firestore.Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull com.google.firebase.firestore.Transaction firebaseTransaction) throws FirebaseFirestoreException {

                DocumentReference sendersTransaction = senderDocRef.collection("transaction").document(transaction.getId());

                firebaseTransaction.set(receiversTransaction, transaction);
                firebaseTransaction.update(receiversTransaction, "timestamp", FieldValue.serverTimestamp());

                firebaseTransaction.set(sendersTransaction, transaction);
                firebaseTransaction.update(sendersTransaction, "timestamp", FieldValue.serverTimestamp());


                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    paymentHandler.setTransactionId(transaction.getId());
                    updateSenderWallet(transaction);
                }else{
                    paymentHandler.setError(task.getException().getLocalizedMessage());
                }
            }
        });

    }

    void updateSenderWallet(final Transaction transaction){
        paymentHandler.setSuccess("Updating wallet");
        final DocumentReference senderLastTransaction = senderDocRef.collection("transaction")
                .document(transaction.getId());

        senderLastTransaction.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                paymentHandler.setDate(sfd.format(documentSnapshot.getTimestamp("timestamp").toDate()));
            }
        });


        mRef.runTransaction(new com.google.firebase.firestore.Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull com.google.firebase.firestore.Transaction transaction) throws FirebaseFirestoreException {
                DocumentReference walletRef = senderDocRef.collection("wallet").document("wallet");
                transaction.update(walletRef, "balance", FieldValue.increment(-amount));
                transaction.update(walletRef, "lastTransaction", senderLastTransaction);
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){

                    updateReceiverWallet(transaction);
                }else{
                    paymentHandler.setError("Sender wallet error");
                }
            }
        });

    }

    void updateReceiverWallet(Transaction transaction){
        final DocumentReference receiverLastTransaction = receiverDocRef.collection("transaction")
                .document(transaction.getId());

        mRef.runTransaction(new com.google.firebase.firestore.Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull com.google.firebase.firestore.Transaction transaction) throws FirebaseFirestoreException {
                DocumentReference walletRef = receiverDocRef.collection("wallet").document("wallet");
                transaction.update(walletRef, "balance", FieldValue.increment(amount));
                transaction.update(walletRef, "lastTransaction", receiverLastTransaction);
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){


                    paymentHandler.setBalance(initialWalletAmount-amount);
                    paymentHandler.setSuccess("Done");
                }else{
                    paymentHandler.setError("Receiver wallet error");
                }
            }
        });
    }


    void updateVariableData(){
        phoneNumber += et_number.getText().toString().trim();
        try{
            amount = Integer.parseInt(et_amount.getText().toString());
        }catch (Exception e){

        }

    }
}
