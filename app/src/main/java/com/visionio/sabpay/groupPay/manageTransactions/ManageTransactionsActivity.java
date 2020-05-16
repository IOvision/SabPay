package com.visionio.sabpay.groupPay.manageTransactions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.Models.GroupPay;
import com.visionio.sabpay.R;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class ManageTransactionsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    GroupPayAdapter adapter;

    FloatingActionButton newGroupPayFab;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_manage_transaction);

        setUp();

    }

    void setUp(){

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        newGroupPayFab = findViewById(R.id.groupPay_activity_newGroupPay_fab);

        recyclerView = findViewById(R.id.groupPay_activity_gPay_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        adapter = new GroupPayAdapter(this, new ArrayList<GroupPay>());

        //recyclerView.setItemAnimator(new SlideInDownAnimator());
        //recyclerView.setItemAnimator(new SlideInRightAnimator());
        recyclerView.setItemAnimator(new SlideInLeftAnimator());
        //recyclerView.setItemAnimator(new SlideInUpAnimator());

        recyclerView.setAdapter(adapter);

        loadData();

        newGroupPayFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewGPayHandler handler = new NewGPayHandler(ManageTransactionsActivity.this, mAuth, mRef);
                handler.init();
            }
        });


    }

    // /user/qA3urwCl8qMAFpbXvD1MW1hzbsL2/group_pay/meta-data/transaction/ZEAUSEXwtliWZ8XDIx8T

    void loadData(){

        mRef.collection("user/"+mAuth.getUid()+"/group_pay/meta-data/transaction")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(DocumentSnapshot snapshot: task.getResult()){
                        GroupPay groupPay = snapshot.toObject(GroupPay.class);
                        adapter.add(groupPay);
                    }
                }else{
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });
    }


}
