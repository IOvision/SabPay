package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
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
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Utils;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.groupPay.manageGroup.GroupManageActivity;
import com.visionio.sabpay.groupPay.manageTransactions.ManageTransactionsActivity;
import com.visionio.sabpay.groupPay.pending.PendingPaymentActivity;
import com.visionio.sabpay.payment.PayActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;


    RelativeLayout movable;
    int screen = 0;// 0 for sab and 1 for group
    int height = 0;

    TextView sabText;

    //groupPay views
    Button myGroup;
    Button manageTransactions;
    Button pendingPayments;

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

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        height = size.y;

        movable = findViewById(R.id.manActivity_background_fL);
        sabText = findViewById(R.id.activity_main_headingSab_tV);

        loadContacts();

        myGroup = findViewById(R.id.activity_main_manageGroups_bt);
        manageTransactions = findViewById(R.id.activity_main_manageTransactions_bt);
        pendingPayments = findViewById(R.id.activity_main_pendingPayments_bt);

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

        myGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GroupManageActivity.class));
            }
        });

        manageTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ManageTransactionsActivity.class));
            }
        });

        pendingPayments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PendingPaymentActivity.class));
            }
        });

        gPayFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this, GroupPayActivity.class));
                alterState();
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

    private void alterState(){

        if(screen==0){
            showGroupPay();
        }else{
            showSabPay();
        }


    }

    private void showGroupPay(){
        // 1.Hide sab, 2.Show group, 3.Change fab icon
        screen=1;

        myGroup.setVisibility(View.VISIBLE);
        manageTransactions.setVisibility(View.VISIBLE);
        pendingPayments.setVisibility(View.VISIBLE);

        myGroup.animate().alpha(1).setDuration(1000).start();
        manageTransactions.animate().alpha(1).setDuration(1000).start();
        pendingPayments.animate().alpha(1).setDuration(1000).start();

        sabText.setAlpha(0);
        sabText.setText("Group");
        sabText.animate().alpha(1).setInterpolator(new DecelerateInterpolator()).setDuration(1000).start();
        gPayFab.setImageResource(R.drawable.ic_up);
        movable.animate().translationY(height*0.8f).setInterpolator(new DecelerateInterpolator()).setDuration(1000).start();
    }

    private void showSabPay(){
        screen=0;

        myGroup.animate().alpha(0).setDuration(1000).start();
        manageTransactions.animate().alpha(0).setDuration(1000).start();
        pendingPayments.animate().alpha(0).setDuration(1000).start();

        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                myGroup.setVisibility(View.GONE);
                manageTransactions.setVisibility(View.GONE);
                pendingPayments.setVisibility(View.GONE);
            }
        }, 1000);

        sabText.setAlpha(0);
        sabText.setText("Sab");
        sabText.animate().alpha(1).setInterpolator(new DecelerateInterpolator()).setDuration(500).start();
        gPayFab.setImageResource(R.drawable.ic_group_white_24dp);
        movable.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).setDuration(500).start();

    }

}
