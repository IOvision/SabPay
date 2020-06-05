package com.visionio.sabpay.main;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.R;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.group_pay.pending.PendingPaymentActivity;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

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
            /*switch (item.getItemId()) {
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
            }*/
            if (item.getItemId() == R.id.bottom_app_bar_main_group){
                groupPay();
            } else if (item.getItemId() == R.id.bottom_app_bar_main_home){
                home();
            } else if (item.getItemId() == R.id.bottom_app_bar_main_pay){
                pay();
            } else if (item.getItemId() == R.id.bottom_app_bar_main_transaction){
                transactionHistory();
            } else if (item.getItemId() == R.id.bottom_app_bar_main_offers){
                offers();
            }
            return true;
        });
        loadContacts();

    }

    @Override
    protected void onStart() {
        super.onStart();
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
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    private void groupPay() {
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
    protected void onDestroy() {
        super.onDestroy();
    }

    void setTitle(String title){
        materialToolbar.setTitle(title);
    }

    private List<Contact> getAllLocalContacts(){
        List<Contact> contacts = new ArrayList<>();
        Cursor phones = getApplicationContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        while (phones.moveToNext()){
            String id = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            Contact contact = new Contact(id, name, phoneNumber);
            contacts.add(contact);

        }

        return contacts;
    }

    private List<String> getNumberArray(List<Contact> contacts){
        List<String> numbers = new ArrayList<>();
        for(Contact c: contacts){
            numbers.add(c.getNumber());
        }
        return numbers;
    }

    private void loadContacts(){

        final List<Contact> contactList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.

            List<Contact> allContacts = getAllLocalContacts();
            List<String> numbers = getNumberArray(allContacts);


            mRef.collection("user").whereIn("phone", numbers).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful()){
                        for(DocumentSnapshot snapshot: task.getResult()){
                            User user = snapshot.toObject(User.class);
                            for(Contact c: allContacts){
                                if(c.getNumber().equals(user.getPhone())){
                                    c.setUser(user);
                                    contactList.add(c);
                                }
                            }
                        }
                        Utils.deviceContacts = contactList;
                    }else{
                        Log.d("Error", task.getException().getLocalizedMessage());
                    }
                }
            });

/*
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
*/
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
            bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_group);
        }
    }
}