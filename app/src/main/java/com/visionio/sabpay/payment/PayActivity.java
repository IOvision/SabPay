package com.visionio.sabpay.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.Result;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.visionio.sabpay.Models.Contact;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Utils;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.OffPay.OffpayActivity;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.ContactAdapter;
import com.visionio.sabpay.interfaces.OnContactItemClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class PayActivity extends AppCompatActivity{

    FirebaseAuth mAuth;
    FirebaseFirestore mRef;

    ImageView back;
    EditText et_number;
    ImageView btn_search_pay;
    Button btn_offpay;

    RecyclerView recyclerView;
    ContactAdapter adapter;

    String phoneNumber;
    Integer amount = 0;

    Integer initialWalletAmount;

    PaymentHandler paymentHandler;

    DocumentReference receiverDocRef;

    DocumentReference senderDocRef;

    // scanner objects
    ViewGroup contentFrame;
    ZXingScannerView mScannerView;
    Boolean scannerOpen = true;

    int type = 0;
    String path;

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    static final int FLAG_MODE_DIRECT_PAY = -1;
    static final int FLAG_MODE_SEARCH_AND_PAY = 0;




    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setUp();


    }

    @Override
    public void onResume() {
        super.onResume();

        mScannerView.setResultHandler(new ZXingScannerView.ResultHandler() {
            @Override
            public void handleResult(Result rawResult) {
                String res = rawResult.getText();
                if(Utils.getPaymentType(res)==0){
                    et_number.setText(res);
                }else{
                    type = 1;
                    path = res;
                }
                initPaymentHandler();
                initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
                Log.i("Testing", "Payment type: "+Utils.getPaymentType(res));

            }
        });
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    void setUp(){

        contentFrame = (ViewGroup) findViewById(R.id.content_frame);
        mScannerView = new ZXingScannerView(this);
        mScannerView.setResultHandler(new ZXingScannerView.ResultHandler() {
            @Override
            public void handleResult(Result rawResult) {
                String res = rawResult.getText();
                if(Utils.getPaymentType(res)==0){
                    et_number.setText(res);
                }else{
                    type = 1;
                    path = res;
                }
                initPaymentHandler();
                initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
                Log.i("Testing", "Payment type: "+Utils.getPaymentType(res));

            }
        });
        contentFrame.addView(mScannerView);
        startCamera();


        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        senderDocRef = mRef.collection("user").document(mAuth.getUid());

        back = findViewById(R.id.pay_activity_back_iv);
        et_number = findViewById(R.id.pay_activity_receiverPhone_et);
        btn_search_pay = findViewById(R.id.pay_activity_searchAndPay_iv);
        recyclerView = findViewById(R.id.pay_activity_contacts_rv);
        btn_offpay = findViewById(R.id.btn_offpay);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));

        adapter = new ContactAdapter(this, new ArrayList<Contact>(), new ArrayList<Contact>());
        adapter.setClickListener(new OnContactItemClickListener() {
            @Override
            public void onItemClicked(Contact contact) {
                initPaymentHandler();
                initiateServer(FLAG_MODE_DIRECT_PAY, contact);
            }
        });

        recyclerView.setAdapter(adapter);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btn_search_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initPaymentHandler();
                initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
            }
        });

        et_number.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    hideScanner();
                    showContacts();
                }else {
                    showScanner();
                }
            }
        });

        btn_offpay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PayActivity.this, OffpayActivity.class));
            }
        });

        et_number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length()<1){
                    showScanner();
                    hideKeyboard();
                }
                adapter.applyFilter(s.toString().trim().toLowerCase());
            }
        });

    }

    private void startCamera(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        }else{
            mScannerView.startCamera(0);
        }
    }

    private void showContacts(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            Cursor phones = getApplicationContext().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            while (phones.moveToNext()){
                String id = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                Contact contact = new Contact(id, name, phoneNumber);
                addIfContactIsRegistered(contact);
            }
        }
    }

    private void addIfContactIsRegistered(final Contact contact){
        // FIXME(BUG) contact is shown multiple times
         /*this function checks if contact from local mobile is registered with our app or not
        * if yes then we add it to adapter else do nothing
        *
                \*/
        mRef.collection("user").whereEqualTo("phone", contact.getNumber())
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots.getDocuments().size()==1){
                    DocumentSnapshot snapshot = queryDocumentSnapshots.getDocuments().get(0);
                    User u = snapshot.toObject(User.class);
                    contact.setUser(u);
                    contact.setReference(snapshot.getReference());
                    adapter.add(contact);
                }
            }
        });
    }

    private void initPaymentHandler(){
        paymentHandler = new PaymentHandler(this, PayActivity.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentHandler.showPayStatus();
                if(paymentHandler.getAmount()>=0) {
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
                String res = result.getContents();
                if(Utils.getPaymentType(res)==0){
                    et_number.setText(res);
                }else{
                    type = 1;
                    path = res;
                }
                initPaymentHandler();
                initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
                Log.i("Testing", "Payment type: "+Utils.getPaymentType(result.getContents()));

            }
        } else {
            Toast.makeText(this, "Blank", Toast.LENGTH_SHORT).show();
        }
    }

    void initiateServer(int flag, Contact contact){
        // info: if flag is @FLAG_MODE_SEARCH_AND_PAY then contact will be null
        if(flag == FLAG_MODE_SEARCH_AND_PAY){
            updateVariableData();
            searchUser();
        }else{
            directPay(contact);
        }
        paymentHandler.init();

    }

    void directPay(Contact contact){
        paymentHandler.setLinkedWallet(contact.getUser().getName());
        receiverDocRef = contact.getReference();
    }

    void searchUser(){

        if(type==1){
            mRef.document(Utils.getPathToUser(path)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        paymentHandler.setLinkedWallet(task.getResult().getString("name"));
                        receiverDocRef = mRef.document(path);
                    }else {
                        paymentHandler.showPayStatus();
                        paymentHandler.setError("Invalid group pay id");
                    }
                }
            });
            return;
        }

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
                        //payToUser();
                        if(type==1){
                            payToGroupPayAccountUsingCloudFunction();
                        }else{
                            payToUserUsingCloudFunction();
                        }
                    }
                }else{
                    paymentHandler.setError("Error fetching wallet data");
                }
            }
        });
    }

    void payToGroupPayAccountUsingCloudFunction(){
        final String id = receiverDocRef.collection("transactions").document().getId();
        final Timestamp timestamp = new Timestamp(new Date());
        Map<String, Object> transactionObj = new HashMap<String, Object>(){{
            put("id", id);
            put("from", senderDocRef);
            put("to", receiverDocRef);
            put("amount", amount);
            put("timestamp", timestamp);
            put("type", type);
        }};

        final ListenerRegistration[] lr = {null};

        receiverDocRef.collection("transactions").document(id)
                .set(transactionObj).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(PayActivity.this, "Done", Toast.LENGTH_SHORT).show();

                    lr[0] = senderDocRef.collection("wallet").document("wallet").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                            Wallet wallet = documentSnapshot.toObject(Wallet.class);
                            if(wallet.getBalance() == initialWalletAmount-amount){
                                paymentHandler.setBalance(wallet.getBalance());
                                lr[0].remove();
                            }
                        }
                    });

                    paymentHandler.setTransactionId(id);
                    SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    paymentHandler.setDate(sfd.format(timestamp.toDate()));

                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            paymentHandler.setSuccess("Done");
                        }
                    }, 5000);

                    paymentHandler.setSuccess("Done");

                }else{
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });

    }

    void payToUserUsingCloudFunction(){
        final Transaction transaction = new Transaction();
        transaction.setId(senderDocRef.collection("transaction").document().getId());
        transaction.setFrom(senderDocRef);
        transaction.setTo(receiverDocRef);
        transaction.setAmount(amount);
        transaction.setTimestamp(new Timestamp(new Date()));

        Map<String, Object> transactionMap = new HashMap<String, Object>(){{
            put("id", transaction.getId());
            put("type", 0);
            put("amount", transaction.getAmount());
            put("from", senderDocRef);
            put("to", receiverDocRef);
            put("timestamp", new Timestamp(new Date()));
        }};

        final ListenerRegistration[] lr = {null};


        senderDocRef.collection("pending_transaction").document("transaction")
                .set(transactionMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(PayActivity.this, "Done", Toast.LENGTH_SHORT).show();
                    lr[0] = senderDocRef.collection("wallet").document("wallet").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                            Wallet wallet = documentSnapshot.toObject(Wallet.class);
                            if(wallet.getBalance() == initialWalletAmount-amount){
                                paymentHandler.setBalance(wallet.getBalance());
                                lr[0].remove();
                            }
                        }
                    });

                    paymentHandler.setTransactionId(transaction.getId());
                    SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    paymentHandler.setDate(sfd.format(transaction.getTimestamp().toDate()));

                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            paymentHandler.setSuccess("Done");
                        }
                    }, 5000);




                } else {
                    Toast.makeText(PayActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    void updateVariableData(){
        phoneNumber = Utils.formatNumber(et_number.getText().toString().trim(), 0);

    }


    private void requestPermission(String permission, int code){
        if(ActivityCompat.shouldShowRequestPermissionRationale(PayActivity.this, permission)){

        } else {
            ActivityCompat.requestPermissions(PayActivity.this, new String[]{permission}, code);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            }
            case CAMERA_PERMISSION_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startCamera();
                }
        }
    }

    private void hideScanner(){
        scannerOpen = false;
        mScannerView.stopCamera();
        contentFrame.setVisibility(View.GONE);

        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showScanner(){
        scannerOpen = true;
        recyclerView.setVisibility(View.GONE);

        mScannerView.startCamera();
        contentFrame.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        Log.i("Testing", ""+scannerOpen);
        super.onBackPressed();
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
