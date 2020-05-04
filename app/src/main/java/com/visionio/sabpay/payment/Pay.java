package com.visionio.sabpay.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.visionio.sabpay.MainActivity;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Pay extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore mRef;

    ImageView back, qr_scan;
    EditText et_number;
    Button btn_pay;

    String phoneNumber;
    Integer amount = 0;

    Integer initialWalletAmount;

    PaymentHandler paymentHandler;

    DocumentReference receiverDocRef;

    DocumentReference senderDocRef;

    private static final int CAMERA_PERMISSION_CODE = 101;

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
        btn_pay = findViewById(R.id.pay_activity_pay_btn);
        qr_scan = findViewById(R.id.pay_activity_qrcode_scan);

        qr_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT > 23){
                    if (checkPermission(Manifest.permission.CAMERA)){
                        openScanner();
                    } else {
                        requestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
                    }
                } else {
                    openScanner();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Pay.this, MainActivity.class));
                finish();
            }
        });



        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initPaymentHandler();
                initiateServer();
            }
        });

    }
    private void openScanner() {
        new IntentIntegrator(Pay.this).initiateScan();
    }

    private void initPaymentHandler(){
        paymentHandler = new PaymentHandler(this, Pay.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentHandler.showPayStatus();
                if(paymentHandler.getAmount() != -1) {
                    amount = paymentHandler.getAmount();
                    initiatePayToUser();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null){
            if (result.getContents()==null){
                Toast.makeText(this, "Blank", Toast.LENGTH_SHORT).show();
            } else {
                et_number.setText(result.getContents());
                initiateServer();
            }
        } else {
            Toast.makeText(this, "Blank", Toast.LENGTH_SHORT).show();
        }
    }

    void initiateServer(){
        updateVariableData();
        paymentHandler.init();
        searchUser();
    }




    void searchUser(){
/*
        mRef.collection("user").whereEqualTo("phone", phoneNumber).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot document : task.getResult()){
                                Log.d("pay.java", document.getId() + " => "+ document.getData());
                            }
                        }else{
                            Log.d("pay.java", "error getting document: " + task.getException());
                        }
                    }
                });

 */



        mRef.collection("user").whereEqualTo("phone", phoneNumber).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if(!task.getResult().getDocuments().isEmpty()){
                        DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                        paymentHandler.setLinkedWallet(snapshot.getString("name"));
                        receiverDocRef = snapshot.getReference();
                    }else{
                        paymentHandler.showPayStatus();
                        paymentHandler.setError("No wallet linked to this number!!");
                    }

                }else{
                    //
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
        phoneNumber = "+91";
        phoneNumber += et_number.getText().toString().trim();
    }

    private boolean checkPermission(String permission){
        int result = ContextCompat.checkSelfPermission(Pay.this, permission);
        if( result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission(String permission, int code){
        if(ActivityCompat.shouldShowRequestPermissionRationale(Pay.this, permission)){

        } else {
            ActivityCompat.requestPermissions(Pay.this, new String[]{permission}, code);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openScanner();
                }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Pay.this, MainActivity.class));
    }
}
