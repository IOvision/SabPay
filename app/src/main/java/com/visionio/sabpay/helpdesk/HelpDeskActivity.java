package com.visionio.sabpay.helpdesk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class HelpDeskActivity extends AppCompatActivity {

    Spinner category_sp, subject_sp;

    TextInputLayout subject_til, content_til;
    EditText subject_et, content_et;

    Button submit_bt;

    RecyclerView recyclerView;
    TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_desk);

        init();
    }
    void init(){
        category_sp = findViewById(R.id.category_sp);
        subject_sp = findViewById(R.id.subject_sp);
        subject_til = findViewById(R.id.subject_til);
        subject_et = subject_til.getEditText();
        content_til = findViewById(R.id.content_til);
        content_et = content_til.getEditText();
        submit_bt = findViewById(R.id.submit_bt);
        recyclerView = findViewById(R.id.recyclerView);
        setUp();
    }
    void setUp(){
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> subjectAdapter = ArrayAdapter.createFromResource(this,
                R.array.subject, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        submit_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverAction();
            }
        });
        category_sp.setAdapter(categoryAdapter);
        subject_sp.setAdapter(subjectAdapter);
        subject_sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] cat = getResources().getStringArray(R.array.subject);
                String sub = cat[position];
                if(sub.equals("Other")){
                    subject_til.setHint("Enter Custom Subject");
                    subject_et.setText(null);
                }else{
                    subject_til.setHint("Subject");
                    subject_et.setText(sub);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        content_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //contentProcessor(s.toString());
            }
        });
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(new ArrayList<>(), new OnItemClickListener<Transaction>() {
            @Override
            public void onItemClicked(Transaction object, int position, View view) {

            }
        }, false);
        recyclerView.setAdapter(adapter);
        loadTransactions();
    }
    void loadTransactions(){
        FirebaseFirestore.getInstance().collection("user")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("transaction")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot snapshot: queryDocumentSnapshots){
                    Transaction currentTransaction = snapshot.toObject(Transaction.class);
                    if(currentTransaction.getFrom().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        currentTransaction.setSendByMe(true);
                    }else{
                        currentTransaction.setSendByMe(false);
                    }
                    Log.i("Testing", currentTransaction.getId()+">>"+currentTransaction.isSendByMe());
                    currentTransaction.loadUserDataFromReference(adapter);
                    adapter.add(currentTransaction);
                }

            }
        }).addOnFailureListener(e -> Log.i("Testing", e.getLocalizedMessage()));
    }
    void serverAction(){
        String subject = subject_et.getText().toString();
        String content = content_et.getText().toString();
        String category = category_sp.getSelectedItem().toString();
        if(subject.equals("")){
            subject_til.setError("Can't be empty");
            return;
        }else if(content.equals("")){
            subject_til.setErrorEnabled(false);
            content_til.setError("Can't be empty");
            return;
        }else{
            subject_til.setErrorEnabled(false);
            content_til.setErrorEnabled(false);
        }
        FirebaseFirestore mRef = FirebaseFirestore.getInstance();
        mRef.document("complains/meta-data").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    int idIndex = task.getResult().getDouble("idIndex").intValue();
                    String complainId = IdGenerator.getNextId(idIndex);

                    mRef.collection("complains").add(new HashMap<String, Object>(){{
                        put("id", complainId);
                        put("from", FirebaseAuth.getInstance().getUid());
                        put("subject", subject);
                        put("category", category);
                        put("data", content);
                        put("time", new Timestamp(new Date()));
                    }}).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if(task.isSuccessful()){
                                AlertDialog.Builder alert = new AlertDialog.Builder(HelpDeskActivity.this);
                                alert.setTitle("Thank You!!");
                                alert.setMessage("Our expert will contact you soon on your registered mobile/ email.");
                                alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                                alert.show();
                            }else{
                                toast(task.getException().getLocalizedMessage());
                            }
                        }
                    });


                }else{
                    log(task.getException().getLocalizedMessage());
                }
            }
        });
    }
    void contentProcessor(String text){
        log("Pos: "+content_et.getSelectionStart());
        com.visionio.sabpay.helper.TextWatcher watcher = new com.visionio.sabpay.helper.TextWatcher(text, content_et.getSelectionStart());
        watcher.displayContacts();watcher.displayTransaction();

    }
    void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    void log(String txt){
        Log.i("test", "HelpDesk: "+txt);
    }
}