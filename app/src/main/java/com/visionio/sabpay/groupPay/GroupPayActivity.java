package com.visionio.sabpay.groupPay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.Models.GroupPay;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.GroupPayAdapter;
import com.visionio.sabpay.groupPay.manage.GroupManageActivity;
import com.visionio.sabpay.groupPay.pending.GroupSelectHandler;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class GroupPayActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    GroupPayAdapter adapter;

    FloatingActionButton newGroupPayFab;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    RelativeLayout payContainer;
    RelativeLayout manageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_pay);

        setUp();

    }

    void setUp(){

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        newGroupPayFab = findViewById(R.id.groupPay_activity_newGroupPay_fab);

        payContainer = findViewById(R.id.gpay_menu_payContainer_rl);
        manageContainer = findViewById(R.id.gpay_menu_manageContainer_rl);

        recyclerView = findViewById(R.id.groupPay_activity_gPay_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        adapter = new GroupPayAdapter(this, new ArrayList<GroupPay>());

        //recyclerView.setItemAnimator(new SlideInDownAnimator());
        //recyclerView.setItemAnimator(new SlideInRightAnimator());
        recyclerView.setItemAnimator(new SlideInLeftAnimator());
        //recyclerView.setItemAnimator(new SlideInUpAnimator());

        recyclerView.setAdapter(adapter);

        //loadData();

        newGroupPayFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewGPayHandler handler = new NewGPayHandler(GroupPayActivity.this, mAuth, mRef);
                handler.init();
            }
        });

        manageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GroupPayActivity.this, GroupManageActivity.class));
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
