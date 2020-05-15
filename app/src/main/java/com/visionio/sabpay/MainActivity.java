package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.visionio.sabpay.Models.Contact;
import com.visionio.sabpay.Models.OfflineTransaction;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Utils;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.groupPay.GroupPayActivity;
import com.visionio.sabpay.payment.PayActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;


    FirebaseAuth mAuth;
    FirebaseFirestore mRef;
    DocumentReference DocRef;

    TextView balanceTv;
    TextView wallet;

    Button payBtn;
    Button signOutBtn;
    Button offerBtn;

    FloatingActionButton gPayFab;

    RecyclerView recyclerView;
    TransactionAdapter adapter;

    ListenerRegistration  listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Activity Started." + getCallingActivity());

        // TODO: after mainActivity show data in list Item of transaction of group pay
        //startActivity(new Intent(this, GroupPayActivity.class));

        setUp();


    }

    void setUp() {
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        if(mAuth.getUid() == null){
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        }

        loadContacts();

        balanceTv = findViewById(R.id.main_activity_balance_tV);
        payBtn = findViewById(R.id.main_activity_pay_btn);
        signOutBtn = findViewById(R.id.main_activity_signOut_btn);
        offerBtn = findViewById(R.id.main_activity_offer_btn);
        recyclerView = findViewById(R.id.main_activity_transactions_rv);
        wallet = findViewById(R.id.main_activity_wallet);
        gPayFab = findViewById(R.id.activity_main_gpay_fab);

        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        gPayFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GroupPayActivity.class));

            }
        });

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PayActivity.class));
            }
        });

        offerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OfferDisplayActivity.class));
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

        adapter = new TransactionAdapter(new ArrayList<Transaction>());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(adapter);

    }

    void showGpayMenu(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.grouppay_menu_layout);
        dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        (dialog.findViewById(R.id.gpay_menu_payContainer_rl)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        (dialog.findViewById(R.id.gpay_menu_payContainer_rl)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        (dialog.findViewById(R.id.gpay_menu_payContainer_rl)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        (dialog.findViewById(R.id.gpay_menu_payContainer_rl)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        dialog.show();
    }

    void signOut(){
        mRef.collection("user").document(mAuth.getUid()).update("login", false)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    listenerRegistration.remove();
                    mAuth.signOut();
                    startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
                    finish();
                }else{
                    Toast.makeText(MainActivity.this, "Could not sign out", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showQR() {
        final Dialog qrCode = new Dialog(MainActivity.this);
        qrCode.requestWindowFeature(Window.FEATURE_NO_TITLE);
        qrCode.setContentView(R.layout.qr_code);

        final ImageView qr_code = qrCode.findViewById(R.id.iv_qr);
        qr_code.setEnabled(true);

        DocRef = mRef.collection("user").document(mAuth.getCurrentUser().getUid());
        DocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    User user = task.getResult().toObject(User.class);
                    MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                    try {
                        BitMatrix bitMatrix = multiFormatWriter.encode(user.getPhone(), BarcodeFormat.QR_CODE, 400, 400);
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                        qr_code.setImageBitmap(bitmap);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    qrCode.show();
                } else {
                    Log.d("Document", "get failed", task.getException());
                }
            }
        });
    }

    void loadTransactions(){
        mRef.collection("user").document(mAuth.getUid()).collection("transaction")
                // TODO: check the filter thing
                //.whereEqualTo("type", 0)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot snapshot: queryDocumentSnapshots){
                    Transaction currentTransaction = snapshot.toObject(Transaction.class);

                    // TODO: fix getType thing and test the transaction item


                    if(currentTransaction.getFrom().getId().equals(mAuth.getUid())){
                        currentTransaction.setSendByMe(true);
                    }else{
                        currentTransaction.setSendByMe(false);
                    }
                    Log.i("Testing", currentTransaction.getFrom().getId()+">>"+currentTransaction.isSendByMe());
                    currentTransaction.loadUserDataFromReference(adapter);
                    adapter.add(currentTransaction);
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("Testing", e.getLocalizedMessage());
            }
        });
    }

    void loadDataFromServer(){
        listenerRegistration = mRef.collection("user").document(mAuth.getUid())
                .collection("wallet").document("wallet").addSnapshotListener(MainActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                Wallet wallet = documentSnapshot.toObject(Wallet.class);
                balanceTv.setText("Rs."+wallet.getBalance().toString());
            }
        });

    }

    private void loadContacts(){

        final List<Contact> contactList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.

            mRef.collection("public").document("registeredPhone").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        List<String> numbers = (List<String>) task.getResult().get("number");

                        Cursor phones = getApplicationContext().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
                        while (phones.moveToNext()){
                            String id = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                            Contact contact = new Contact(id, name, phoneNumber);
                            if(numbers.contains(contact.getNumber())){
                                contactList.add(contact);
                            }

                        }

                        Utils.deviceContacts = contactList;

                    }else{

                    }
                }
            });

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted
                    loadContacts();
                }
        }
    }

}
