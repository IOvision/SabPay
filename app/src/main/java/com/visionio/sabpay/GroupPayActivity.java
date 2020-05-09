package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.Models.GroupPay;
import com.visionio.sabpay.adapter.GroupPayAdapter;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

public class GroupPayActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    GroupPayAdapter adapter;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_pay);

        setUp();

    }

    void setUp(){

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

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


    }

    void loadData(){

        mRef.collection("user/"+mAuth.getUid()+"/group_pay/meta-data/transaction")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(DocumentSnapshot snapshot: task.getResult()){
                        GroupPay groupPay = snapshot.toObject(GroupPay.class);
                        groupPay.setPath("user/"+mAuth.getUid()+"/group_pay/meta-data/transaction/");
                        adapter.add(groupPay);
                    }
                }else{
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });
    }


}
