package com.visionio.sabpay.groupPay.pending;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.Models.GroupPay;
import com.visionio.sabpay.R;
import com.visionio.sabpay.groupPay.manage.Group;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.ArrayList;

public class GroupSelectHandler {

    Context context;

    Dialog dialog;

    AlertDialog alertDialog;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    GroupPay groupPay;

    //dialog views
    EditText searchEdtText;
    RecyclerView groupRecyclerView;

    GroupSelectorAdapter adapter;

    public GroupSelectHandler(Context context) {
        this.context = context;
        setUp();
    }

    private void setUp(){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.group_select_layout);
        dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        searchEdtText = dialog.findViewById(R.id.group_select_layout_search_et);
        groupRecyclerView = dialog.findViewById(R.id.group_select_layout_groupList_rv);

        groupRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        groupRecyclerView.setHasFixedSize(false);



        adapter = new GroupSelectorAdapter(new ArrayList<Group>(), new OnItemClickListener<Group>() {
            @Override
            public void onItemClicked(Group object, int position, View view) {
                alertDialog = new AlertDialog.Builder(context)
                        .setTitle("Split among")
                        .setMessage("Group Name: "+object.getName()+"\nTotal Members: "+object.getSize())
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i("Testing", "Negative");
                            }
                        })
                        .setPositiveButton("Split", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i("Testing", "Positive");
                            }
                        }).create();
                alertDialog.show();
            }
        });

        groupRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL));
        groupRecyclerView.setAdapter(adapter);



        loadGroups();
        show();
    }

    private void split(Group group){

    }

    private void loadGroups(){
        DocumentReference myAdminRef = mRef.document("user/"+mAuth.getUid());
        mRef.collection("groups")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereEqualTo("admin", myAdminRef).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(DocumentSnapshot snapshot: task.getResult()){
                                adapter.add(snapshot.toObject(Group.class));
                            }
                        }else{
                            Log.i("Testing", task.getException().getLocalizedMessage());
                        }
                    }
                });
    }

    private void show(){
        dialog.show();
    }

    private void destroy(){
        dialog.dismiss();
    }


}
