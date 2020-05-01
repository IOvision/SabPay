package com.visionio.sabpay;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
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
    TextView wallet;

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
        wallet = findViewById(R.id.main_activity_wallet);

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

        wallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQR();
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

    private void showQR() {
        final Dialog qrCode = new Dialog(MainActivity.this);
        qrCode.requestWindowFeature(Window.FEATURE_NO_TITLE);
        qrCode.setContentView(R.layout.qr_code);

        ImageView qr_code = qrCode.findViewById(R.id.iv_qr);
        Button back = qrCode.findViewById(R.id.btn_qr);

        qr_code.setEnabled(true);
        back.setEnabled(true);

        String data = mAuth.getCurrentUser().getPhoneNumber();
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qr_code.setImageBitmap(bitmap);
        } catch (Exception e){
            e.printStackTrace();
        }
        qrCode.show();
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
