package com.visionio.sabpay.main;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.group_pay.pending.PendingPaymentActivity;
import com.visionio.sabpay.interfaces.MainInterface;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.Transaction;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.models.Wallet;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MainInterface {

    FragmentManager fragmentManager;
    FrameLayout frameLayout;
    FragmentTransaction fragmentTransaction;
    MaterialToolbar materialToolbar;
    BottomNavigationView bottomNavigationView;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    ListenerRegistration listenerRegistration;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (mAuth.getUid() != null) {
            setUp();
        } else {
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);
    }

    void setUp() {

        frameLayout = findViewById(R.id.main_frame);
        bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        materialToolbar = findViewById(R.id.main_top_bar);

        materialToolbar.setOnMenuItemClickListener(v -> {
            signOut();
            return true;
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.bottom_app_bar_main_home: {
                    home();
                    return true;
                }
                case R.id.bottom_app_bar_main_pay: {
                    pay();
                    return true;
                }
                case R.id.bottom_app_bar_main_group: {
                    groupPay();
                    return true;
                }
                case R.id.bottom_app_bar_main_transaction: {
                    transactionHistory();
                    return true;
                }
                case R.id.bottom_app_bar_main_offers: {
                    offers();
                    return true;
                }
            }
            return false;
        });
        loadContacts();
    }

    @Override
    protected void onStart() {
        super.onStart();
        home();
    }

    private void offers() {
        OfferFragment fragment = new OfferFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void transactionHistory() {
        TransactionHistoryFragment fragment = new TransactionHistoryFragment();
        fragment.setListener(this);
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void groupPay() {
        Toast.makeText(this, "groupPay", Toast.LENGTH_SHORT).show();
        GroupPayFragment fragment = new GroupPayFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void pay() {
        PayFragment fragment = new PayFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void home() {
        materialToolbar.setTitle("Home");
        HomeFragment fragment = new HomeFragment();
        fragment.setListener(this);
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    void signOut() {
        mRef.collection("user").document(mAuth.getUid()).update("login", false)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mAuth.signOut();
                        Log.d("signOut", "signOut: "+listenerRegistration);
                        Intent intent = new Intent(getApplicationContext(), AuthenticationActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Could not sign out", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void setBalanceTv(TextView tv, ProgressBar balance_pb, ImageView addMoney) {
        listenerRegistration = mRef.collection("user").document(mAuth.getUid())
                .collection("wallet").document("wallet").addSnapshotListener((documentSnapshot, e) -> {
                    {
                        Wallet wallet = documentSnapshot.toObject(Wallet.class);
                        tv.setText("\u20B9" + wallet.getBalance().toString());
                        balance_pb.setVisibility(View.GONE);
                        addMoney.setVisibility(View.VISIBLE);
                    }
                });
    }

    public void loadTransactions(TransactionAdapter adapter, ProgressBar progressBar){
        FirebaseFirestore.getInstance().collection("user")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("transaction")
                // TODO: check the filter thing
                //.whereEqualTo("type", 0)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot snapshot: queryDocumentSnapshots){
                    Transaction currentTransaction = snapshot.toObject(Transaction.class);

                    // TODO: fix getType thing and test the transaction item
                    if(currentTransaction.getFrom().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        currentTransaction.setSendByMe(true);
                    }else{
                        currentTransaction.setSendByMe(false);
                    }
                    Log.d("Testing1", currentTransaction.getFrom().getId()+">>"+currentTransaction.isSendByMe());
                    currentTransaction.loadUserDataFromReference(adapter);
                    adapter.add(currentTransaction);
                    progressBar.setVisibility(View.GONE);
                }

            }
        }).addOnFailureListener(e -> Log.i("Testing", e.getLocalizedMessage()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void setTitle(String title){
        materialToolbar.setTitle(title);
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

    void startPendingPayment(){
        startActivityForResult(new Intent(MainActivity.this, PendingPaymentActivity.class), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1){
            Log.d("ActivityResult", "onActivityResult: Result Acquired!");
            groupPay();
        }
    }
}

/*
    void setUp() {
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();
        sendData();
        loadImage();
        setView();
        if(mAuth.getUid() == null){
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        }
    wallet.setOnClickListener(v -> {
            try {
                showQR();
            } catch (WriterException e) {
                e.printStackTrace();
            }
        });



        if(mAuth.getCurrentUser() != null){
            loadDataFromServer();
        }
        if(Paper.book(FirebaseAuth.getInstance().getCurrentUser().getUid()).contains("user")){
            User user = Paper.book(FirebaseAuth.getInstance().getCurrentUser().getUid()).read("user");
            phone = user.getPhone();
            appBarLayout.setTitle("Hi, " + user.getFirstName());
        } else {
            DocRef = mRef.collection("user").document(mAuth.getCurrentUser().getUid());
            DocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    User user = task.getResult().toObject(User.class);
                    Paper.book("current").write("user",user);
                    phone = user.getPhone();
                    appBarLayout.setTitle("Hi, " + user.getFirstName());
                }
            });
        }
    }

    private void showQR() throws WriterException {
        Dialog qrCode = new Dialog(MainActivity.this);
        qrCode.requestWindowFeature(Window.FEATURE_NO_TITLE);
        qrCode.setContentView(R.layout.qr_code);

        final ImageView qr_code = qrCode.findViewById(R.id.iv_qr);
        qr_code.setEnabled(true);
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = multiFormatWriter.encode(phone, BarcodeFormat.QR_CODE, 400, 400);
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
        qr_code.setImageBitmap(bitmap);
        qrCode.show();
    }



    }

    void setView(){

    bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.bottom_app_bar_main_transaction : {
                    Intent intent = new Intent(getApplicationContext(), TransactionHistory.class);
                    startActivityForResult(intent, 1);
                    return true;
                }
                case R.id.bottom_app_bar_main_discount : return false;
                case R.id.bottom_app_bar_main_group : return false;
                case R.id.bottom_app_bar_main_home : return true;
                case R.id.bottom_app_bar_main_pay : {
                    return false;

                }
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);
    }


    }
}
*/