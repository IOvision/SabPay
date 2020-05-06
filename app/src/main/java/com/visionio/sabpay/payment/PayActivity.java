package com.visionio.sabpay.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.visionio.sabpay.Models.Contact;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.ContactAdapter;
import com.visionio.sabpay.interfaces.OnContactItemClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PayActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore mRef;

    ImageView back, qr_scan;
    EditText et_number;
    Button btn_pay;

    RecyclerView recyclerView;
    ContactAdapter adapter;

    String phoneNumber;
    Integer amount = 0;

    Integer initialWalletAmount;

    PaymentHandler paymentHandler;

    DocumentReference receiverDocRef;

    DocumentReference senderDocRef;

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    static final int FLAG_MODE_DIRECT_PAY = -1;
    static final int FLAG_MODE_SEARCH_AND_PAY = 0;

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
        recyclerView = findViewById(R.id.pay_activity_contacts_rv);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        adapter = new ContactAdapter(this, new ArrayList<Contact>(), new ArrayList<Contact>());
        adapter.setClickListener(new OnContactItemClickListener() {
            @Override
            public void onItemClicked(Contact contact) {
                initPaymentHandler();
                initiateServer(FLAG_MODE_DIRECT_PAY, contact);
            }
        });

        recyclerView.setAdapter(adapter);


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
                onBackPressed();
            }
        });

        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initPaymentHandler();
                initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
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
                adapter.applyFilter(s.toString().trim().toLowerCase());
            }
        });

        showContacts();
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
        /* this function checks if contact from local mobile is registered with our app or not
        * if yes then we add it to adapter else do nothing
        * */
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

    private void openScanner() {
        new IntentIntegrator(PayActivity.this).initiateScan();
    }

    private void initPaymentHandler(){
        paymentHandler = new PaymentHandler(this, PayActivity.this, new View.OnClickListener() {
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
                initiateServer(FLAG_MODE_SEARCH_AND_PAY, null);
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
                        payToUserUsingCloudFunction();
                    }
                }else{
                    paymentHandler.setError("Error fetching wallet data");
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


        Map<String, Object> value = new HashMap<>();
        value.put("id", transaction.getId());
        value.put("amount", transaction.getAmount());
        value.put("from", transaction.getFrom());
        value.put("to", transaction.getTo());
        value.put("timestamp", ServerValue.TIMESTAMP);

        senderDocRef.collection("pending_transaction").document("transaction")
                .set(value).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(PayActivity.this, "Done", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(PayActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
        phoneNumber += et_number.getText().toString().trim().replaceAll("\\s", "");
    }

    private boolean checkPermission(String permission){
        int result = ContextCompat.checkSelfPermission(PayActivity.this, permission);
        if( result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
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
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openScanner();
                }
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            }
        }
    }

    @Override
    public void onBackPressed() {
        int v= 1;
        super.onBackPressed();

    }
}
