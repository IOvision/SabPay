package com.visionio.sabpay.payment;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.SelectedContactsAdapter;
import com.visionio.sabpay.adapter.TransactionStatusAdapter;
import com.visionio.sabpay.api.SabPayNotify;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.interfaces.Payment;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.models.Wallet;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    TextView name_tv, balance_tv;
    RecyclerView payeeList;
    MaterialToolbar materialToolbar;
    TextInputLayout textInputLayout;
    ProgressBar progressBar;
    EditText amount_et;
    Button send;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mRef;
    private DocumentReference senderDocRef;

    LinearLayout balanceHeader;

    RecyclerView recyclerView;
    TransactionStatusAdapter adapter;

    Payment payment;
    SelectedContactsAdapter selectedContactsAdapter;

    Wallet wallet;

    int[] uiManipulator = {1, 0, 0};
    // represents 3 phases of ui. 1 means phase[i] is visible & vice-versa

    boolean isAmountOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        textInputLayout = findViewById(R.id.payment_activity_amount);
        name_tv = findViewById(R.id.payment_receiver_name);
        amount_et = findViewById(R.id.payment_activity_amount_et);
        send = findViewById(R.id.payment_activity_pay);
        materialToolbar = findViewById(R.id.payment_top_bar);

        setSupportActionBar(materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        materialToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        payeeList = findViewById(R.id.payment_receiver_list_rv);
        balance_tv = findViewById(R.id.payment_activity_balance_tv);
        balanceHeader = findViewById(R.id.payment_activity_wallet_header_ll);

        progressBar = findViewById(R.id.payment_activity_transaction_progress);

        recyclerView = findViewById(R.id.payment_activity_transactionStatus_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        adapter = new TransactionStatusAdapter(new ArrayList<Map<String, String>>());

        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();

        payment = Payment.getInstance();
        selectedContactsAdapter = payment.getAdapter();
        selectedContactsAdapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(Contact object, int position, View view) {
                int size = selectedContactsAdapter.getContacts().size();
                if(size==1){
                    Toast.makeText(PaymentActivity.this, "At least 1 payee need to be selected", Toast.LENGTH_LONG).show();
                }else{
                    selectedContactsAdapter.remove(object);
                    name_tv.setText("Paying to "+selectedContactsAdapter.getContacts().size());
                }

            }
        });

        payeeList.setLayoutManager(new LinearLayoutManager(this){{
            setOrientation(RecyclerView.HORIZONTAL);
        }});
        payeeList.setHasFixedSize(false);
        payeeList.setAdapter(selectedContactsAdapter);

        senderDocRef = mRef.collection("user").document(mAuth.getUid());

        name_tv.setText("Paying to "+selectedContactsAdapter.getContacts().size());

        fetchWallet();

        send.setOnClickListener((View v) -> {
            String amount = amount_et.getText().toString().trim();
            if(!isAmountOk){
                if(amount.equals("")){
                    amount_et.setError("Can't be empty");
                }else{
                    amount_et.setError("Insufficient Balance");
                }
            }else{
                updateUi(0, 0, 1);
                payToUserUsingCloudFunction(Integer.parseInt(amount), selectedContactsAdapter.getContacts());
            }

        });

        amount_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Integer amount;
                if(s.toString().trim().equals("")){
                    amount = 0;
                }else{
                    amount = Integer.parseInt(s.toString().trim());
                }
                int total = amount*selectedContactsAdapter.getContacts().size();
                if(total>wallet.getBalance()){
                    amount_et.setError("Insufficient Balance");
                    isAmountOk = false;
                }else{
                    balance_tv.setText(""+(wallet.getBalance()-total));
                    isAmountOk = true;
                }
            }
        });

    }


    void fetchWallet(){
        senderDocRef.collection("wallet").document("wallet").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    wallet = task.getResult().toObject(Wallet.class);
                    updateUi(0, 1, 0); //moving from p1 to p2
                }else{
                    Toast.makeText(PaymentActivity.this, "Error fetching wallet data", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    void addToStatus(String id, Timestamp timestamp, String userName){
        Map<String, String> status = new HashMap<String, String>(){{
            put("id", id);
            put("time", Utils.getDateTime(timestamp));
            put("user", userName);
        }};
        adapter.add(status);
    }

    void payToUserUsingCloudFunction(int amount, List<Contact> payee){
        List<DocumentReference> toDocRef = new ArrayList<>();
        for(Contact c: payee){
            toDocRef.add(mRef.document("user/"+c.getUser().getUid()));
        }
        Map<String, Object> transactionMap = new HashMap<String, Object>(){{
            put("id", senderDocRef.collection("transaction").document().getId());
            put("type", 0);
            put("amount", amount);
            put("from", senderDocRef);
            put("to", toDocRef);
            put("timestamp", new Timestamp(new Date()));
        }};

        senderDocRef.collection("pending_transaction")
                .document("transaction").set(transactionMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    String title = "Money Received";
                    String msg = "Transaction Id: %s\nAmount: Rs. %s\nFrom: %s";
                    int i=0;
                    for(Contact c: payee){
                        int finalI = i;
                        String txId = Utils.getTransactionId(transactionMap.get("id").toString(), finalI);
                        String userName = c.getUser().getName();
                        adapter.add(new HashMap<String, String>(){{
                            put("id", txId);
                            put("to", userName);
                        }});
                        i++;
                        new SabPayNotify.Builder()
                                .setTitle(title)
                                .setMessage(String.format(msg, txId, amount, userName))
                                .send(getApplicationContext(), c.getUser().getUid(), false);

                    }
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onBackPressed();
                            finish();
                        }
                    }, 3000);
                }else{
                }
            }
        });


        final ListenerRegistration[] lr = {null};

        List<Map<String, String>> status = new ArrayList<>();

    }

    void updateUi(int phase_1, int phase_2, int phase_3){
        if(uiManipulator[0]!=phase_1){
            uiManipulator[0] = phase_1;
            updatePhase_1();
        }
        if(uiManipulator[1]!=phase_2){
            uiManipulator[1] = phase_2;
            updatePhase_2();
        }
        if(uiManipulator[2]!=phase_3){
            uiManipulator[2] = phase_3;
            updatePhase_3();
        }
    }
    void updatePhase_1(){
        int visibility = uiManipulator[0];
        if(visibility==0){
            visibility = View.GONE;
        }else if(visibility==1){
            visibility = View.VISIBLE;
        }
        progressBar.setVisibility(visibility);
        balance_tv.setText(wallet.getBalance().toString());
    }
    void updatePhase_2(){
        int visibility = uiManipulator[1];
        if(visibility==0){
            visibility = View.GONE;
        }else if(visibility==1){
            visibility = View.VISIBLE;
        }
        payeeList.setVisibility(visibility);
        name_tv.setVisibility(visibility);
        textInputLayout.setVisibility(visibility);
        send.setVisibility(visibility);
        balanceHeader.setVisibility(visibility);
    }
    void updatePhase_3(){
        int visibility = uiManipulator[2];
        if(visibility==0){
            visibility = View.GONE;
        }else if(visibility==1){
            visibility = View.VISIBLE;
        }
        recyclerView.setVisibility(visibility);
    }
}
