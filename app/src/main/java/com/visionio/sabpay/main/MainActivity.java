package com.visionio.sabpay.main;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.R;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.group_pay.pending.PendingPaymentActivity;
import com.visionio.sabpay.helper.InvoiceGenerator;
import com.visionio.sabpay.helper.TokenManager;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity{

    FragmentManager fragmentManager;
    FrameLayout frameLayout;
    FragmentTransaction fragmentTransaction;
    MaterialToolbar materialToolbar;
    BottomNavigationView bottomNavigationView;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    private BroadcastReceiver mMessageReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Alert", "Creating Alert");
            showAlert(intent);
        }
    };

    private void showAlert(Intent intent) {
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
        alert.setTitle(intent.getStringExtra("value1"));
        alert.setMessage(intent.getStringExtra("value2"));
        alert.setPositiveButton("Ok", (dialog, which) -> {
            dialog.dismiss();
        });
        alert.show();
    }

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
    public void onBackPressed() {
        if(bottomNavigationView.getSelectedItemId() == R.id.bottom_app_bar_main_home) {
            super.onBackPressed();
        } else {
            bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReciever,
                new IntentFilter("myFunction"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReciever);
    }

    void setUp() {
        TokenManager.handle(this);

        InvoiceGenerator generator = new InvoiceGenerator("new.pdf", null, getApplicationContext());
        boolean isSuccess = generator.generate();


        frameLayout = findViewById(R.id.main_frame);
        bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        materialToolbar = findViewById(R.id.main_top_bar);

        materialToolbar.setOnMenuItemClickListener(v -> {
            signOut();
            return true;
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.bottom_app_bar_main_group){
                groupPay();
            } else if (item.getItemId() == R.id.bottom_app_bar_main_home){
                //offers();
                home();
            } else if (item.getItemId() == R.id.bottom_app_bar_main_pay){
                if(Utils.deviceContacts==null){
                  Toast.makeText(MainActivity.this, "Contact still loading", Toast.LENGTH_SHORT).show();
                }else{
                    pay();
                }
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
        InventoryFragment fragment = new InventoryFragment();
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
        for(ListenerRegistration registration: Utils.registrations){
            registration.remove();
        }
        mRef.collection("user").document(mAuth.getUid()).update(new HashMap<String, Object>(){{
            //put("login", false);
            put("instanceId", null);
        }})
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mAuth.signOut();
                        Intent intent = new Intent(getApplicationContext(), AuthenticationActivity.class);
                        startActivity(intent);
                        finishAffinity();
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

    List<Contact> getAllLocalContacts(){
            List<Contact> contacts = new ArrayList<>();
            Cursor phones = getApplicationContext().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            while (phones.moveToNext()) {
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
            if (!numbers.contains(c.getNumber()))
                numbers.add(c.getNumber());
        }
        return numbers;
    }

    private void loadContacts(){
        Paper.book().delete("contacts");
        if (Paper.book().contains("contacts")){
            Utils.deviceContacts = Paper.book().read("contacts");
        } else {
            final List<Contact> contactList = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
                //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
            } else {
                // Android version is lesser than 6.0 or the permission is already granted.
                Toast.makeText(this, "loading contacts.", Toast.LENGTH_SHORT).show();
                List<Contact> allContacts = getAllLocalContacts();
                List<String> numbers = getNumberArray(allContacts);

                if(numbers.size()==0){
                    Utils.deviceContacts = new ArrayList<>();
                    return;
                }

                mRef.document("/public/registeredPhone").get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){
                                    List<String> numRegList;
                                    try{
                                        numRegList = (List<String>) task.getResult().get("number");
                                    }catch (Exception e){
                                        numRegList = new ArrayList<>();
                                    }
                                    List<Contact> commonContacts = new ArrayList<>();
                                    for(String numReg: numRegList){
                                        for(Contact inDeviceContact: allContacts){
                                            if(inDeviceContact.getNumber().equals(numReg)){
                                                mRef.collection("user").whereEqualTo("phone", inDeviceContact.getNumber()).get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                           @Override
                                                           public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                               if (task.isSuccessful()) {
                                                                   if (!task.getResult().getDocuments().isEmpty()) {
                                                                       DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                                                                       User user = snapshot.toObject(User.class);
                                                                       inDeviceContact.setName(user.getName());
                                                                       inDeviceContact.setNumber(user.getPhone());
                                                                       inDeviceContact.setUser(user);
                                                                   }
                                                               }
                                                           }
                                                        });
                                                commonContacts.add(inDeviceContact);
                                                break;
                                            }
                                        }
                                    }
                                    Utils.deviceContacts = contactList;
                                    Paper.book().write("contacts", commonContacts);
                                    Toast.makeText(MainActivity.this, "Contacts loaded.", Toast.LENGTH_SHORT).show();
                                }else{
                                    Log.i("test", task.getException().getLocalizedMessage());
                                }
                            }
                        });


            }
        }
    }

    void startPendingPayment(){
        startActivityForResult(new Intent(MainActivity.this, PendingPaymentActivity.class), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1){
            bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_group);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}