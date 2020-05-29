package com.visionio.sabpay.group_pay.manageGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.enums.REQUEST;

import java.util.ArrayList;
import java.util.List;

public class GroupManageActivity extends AppCompatActivity {


    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    FrameLayout addButton;
    ViewPager2 groupsViewPager;
    GroupAdapter groupsAdapter;

    TextView noGroupHeading;
    SwipeRefreshLayout refreshLayout;

    FirebaseFirestore mRef;

    BottomSheetHandler newGroupHandler;

    BottomSheetHandler groupEditorHandler;
    int openedGroup = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_manage);

        setUp();
    }

    void setUp(){
        mRef = FirebaseFirestore.getInstance();
        addButton = findViewById(R.id.gpay_manage_newGroup_fl);
        groupsViewPager = findViewById(R.id.gpay_manage_groupViewPager);
        noGroupHeading = findViewById(R.id.gpay_manage_noGrpHeading_tv);
        refreshLayout = findViewById(R.id.gpay_manage_swipeRefresh_sl);
        groupsAdapter = new GroupAdapter(this, new ArrayList<Group>(), new OnItemClickListener<Group>() {
            @Override
            public void onItemClicked(Group object, int position, View view) {
                Toast.makeText(GroupManageActivity.this, object.getName(), Toast.LENGTH_SHORT).show();
                if(openedGroup==-1 || openedGroup!=position){
                    groupEditorHandler =
                            new BottomSheetHandler(GroupManageActivity.this, GroupManageActivity.this,
                                    REQUEST.OPEN_EDITABLE_MODE, object);
                    openedGroup = position;
                }
                groupEditorHandler.show();

            }

        });

        setUpViewPager();



        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(newGroupHandler == null ){newGroupHandler = new BottomSheetHandler(GroupManageActivity.this, GroupManageActivity.this);}

                newGroupHandler.show();
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadGroups();
            }
        });


    }

    void setUpViewPager(){
        groupsViewPager.setAdapter(groupsAdapter);
        groupsViewPager.setClipToPadding(false);
        groupsViewPager.setClipChildren(false);
        groupsViewPager.setOffscreenPageLimit(1);
        groupsViewPager.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);


        CompositePageTransformer pageTransformer = new CompositePageTransformer();
        pageTransformer.addTransformer(new MarginPageTransformer(120));
        pageTransformer.addTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float r = 1 -Math.abs(position);
                page.setScaleY(0.85f + r * 0.15f);
                page.setAlpha(0.43f + r);
            }
        });

        groupsViewPager.setPageTransformer(pageTransformer);

        loadGroups();
    }

    private void loadGroups(){
        DocumentReference myRef = mRef.document("user/"+ FirebaseAuth.getInstance().getUid());
        mRef.collection("groups").whereArrayContains("members", myRef).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            if(task.getResult().size()>=1){
                                noGroupHeading.setVisibility(View.GONE);
                            }
                            List<Group> groups = new ArrayList<>();
                            for(DocumentSnapshot snapshot: task.getResult()){
                                Group group = snapshot.toObject(Group.class);
                                groups.add(group);
                            }
                            groupsAdapter.addAllGroup(groups);
                            refreshLayout.setRefreshing(false);
                            Toast.makeText(GroupManageActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                        }else{
                            Log.i("Testing", task.getException().getLocalizedMessage());
                        }
                    }
                });
    }




}
