package com.visionio.sabpay.group_pay.manageGroup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.ContactAdapter;
import com.visionio.sabpay.adapter.SelectedContactsAdapter;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.enums.REQUEST;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class BottomSheetHandler {

    Context context;
    Activity activity;

    BottomSheetDialog dialog;

    BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    RecyclerView selectedContactsRecyclerView;
    RecyclerView allContactsRecyclerView;
    ContactAdapter allContactAdapter;
    SelectedContactsAdapter selectedContactsAdapter;
    LinearLayout selectedContactsContainer;
    EditText searchContacts;
    EditText groupName;
    Boolean selected=false;

    TextView next;

    FirebaseFirestore mRef;
    FirebaseAuth mAuth;

    REQUEST request;
    List<Contact> selectedContacts;
    String name;
    Group group;

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    public BottomSheetHandler(Context context, Activity activity, REQUEST request, Group group) {
        this.context = context;
        this.activity = activity;
        this.request = request;
        this.group = group;
        this.name = group.getName();
        this.selectedContacts = group.getMembersContactList();
        setUp();

    }

    public BottomSheetHandler(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        setUp();
    }

    private void setUp(){
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.add_friend_layout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        bottomSheetBehavior = dialog.getBehavior();

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState == BottomSheetBehavior.STATE_COLLAPSED){
                    dialog.dismiss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        dialog.getBehavior().setFitToContents(false);
        dialog.getBehavior().setHalfExpandedRatio(0.95f);
        dialog.getBehavior().setPeekHeight(0);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);

        mRef = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        next = dialog.findViewById(R.id.add_member_next_tv);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(request==REQUEST.OPEN_EDITABLE_MODE){
                    updateGroup();
                }else {
                    createGroup();
                }
            }
        });

        if(request == REQUEST.OPEN_EDITABLE_MODE){
            mainEditableMode();
        }else{
            main();
        }


    }


    private void mainEditableMode(){
        next.setText("Update");
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setHalfExpandedRatio(0.95f);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        groupName = dialog.findViewById(R.id.add_member_groupName_et);
        searchContacts = dialog.findViewById(R.id.add_member_search_et);

        selectedContactsContainer = dialog.findViewById(R.id.add_member_selectedContactsContainer_ll);
        selectedContactsContainer.setVisibility(View.VISIBLE);
        groupName.setText(name);

        selectedContactsAdapter = new SelectedContactsAdapter(new ArrayList<Contact>(selectedContacts));
        allContactAdapter = new ContactAdapter(context, new ArrayList<Contact>(), new ArrayList<Contact>());


        selectedContactsRecyclerView = dialog.findViewById(R.id.add_member_selectedContacts_rv);
        allContactsRecyclerView = dialog.findViewById(R.id.add_member_allContacts_rv);

        selectedContactsRecyclerView.setLayoutManager(new LinearLayoutManager(context){{
            setOrientation(RecyclerView.HORIZONTAL);
        }});
        selectedContactsRecyclerView.setHasFixedSize(false);

        allContactsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        allContactsRecyclerView.setHasFixedSize(false);

        allContactsRecyclerView.setItemAnimator(new SlideInLeftAnimator());


        selectedContactsRecyclerView.setAdapter(selectedContactsAdapter);
        allContactsRecyclerView.setAdapter(allContactAdapter);

        allContactAdapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(Contact contact, int position, View v) {
                Log.i("Testing", "Select: "+position);
                for(Contact c: selectedContactsAdapter.getContacts()){
                    if(c.getNumber().equals(contact.getNumber())){
                        Toast.makeText(context, "Already added", Toast.LENGTH_SHORT).show();

                        return;
                    }
                }

                allContactAdapter.select(position);
                selectedContactsAdapter.add(contact);
                if(selectedContactsAdapter.getItemCount()==1 && !selected){
                    selectedContactsContainer.setVisibility(View.VISIBLE);
                    selected=true;
                }
            }

        });

        selectedContactsAdapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(final Contact contact, final int position, View v) {
                v.animate().scaleX(0).scaleY(0).setInterpolator(new DecelerateInterpolator()).setDuration(500).start();
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        allContactAdapter.addUserToContact(contact);
                        selectedContactsAdapter.remove(contact);
                        allContactAdapter.unSelect(contact.positionInAdapter);

                        if(selectedContactsAdapter.getItemCount()==0 && selected){
                            selectedContactsContainer.setVisibility(View.GONE);
                            selected = false;
                        }
                    }
                }, 1000);
            }

        });


        searchContacts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                allContactAdapter.applyFilter(s.toString().trim());
            }
        });
        addContact();

    }

    public ContactAdapter getAllContactAdapter() {
        return allContactAdapter;
    }

    private void main(){
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setHalfExpandedRatio(0.95f);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        groupName = dialog.findViewById(R.id.add_member_groupName_et);
        searchContacts = dialog.findViewById(R.id.add_member_search_et);

        selectedContactsContainer = dialog.findViewById(R.id.add_member_selectedContactsContainer_ll);

        selectedContactsAdapter = new SelectedContactsAdapter(new ArrayList<Contact>());
        allContactAdapter = new ContactAdapter(context, new ArrayList<Contact>(), new ArrayList<Contact>());


        selectedContactsRecyclerView = dialog.findViewById(R.id.add_member_selectedContacts_rv);
        allContactsRecyclerView = dialog.findViewById(R.id.add_member_allContacts_rv);

        selectedContactsRecyclerView.setLayoutManager(new LinearLayoutManager(context){{
            setOrientation(RecyclerView.HORIZONTAL);
        }});
        selectedContactsRecyclerView.setHasFixedSize(false);

        allContactsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        allContactsRecyclerView.setHasFixedSize(false);

        allContactsRecyclerView.setItemAnimator(new SlideInLeftAnimator());


        selectedContactsRecyclerView.setAdapter(selectedContactsAdapter);
        allContactsRecyclerView.setAdapter(allContactAdapter);

        allContactAdapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(Contact contact, int position, View v) {
                Log.i("Testing", "Select: "+position);
                allContactAdapter.select(position);
                selectedContactsAdapter.add(contact);
                if(selectedContactsAdapter.getItemCount()==1 && !selected){
                    selectedContactsContainer.setVisibility(View.VISIBLE);
                    selected=true;
                }
            }

        });

        selectedContactsAdapter.setClickListener(new OnItemClickListener<Contact>() {
            @Override
            public void onItemClicked(final Contact contact, final int position, View v) {
                v.animate().scaleX(0).scaleY(0).setInterpolator(new DecelerateInterpolator()).setDuration(500).start();
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        selectedContactsAdapter.remove(contact);
                        allContactAdapter.unSelect(contact.positionInAdapter);
                        Log.i("Testing", "Select: "+position+" Pos in adapter: "+contact.positionInAdapter);
                        if(selectedContactsAdapter.getItemCount()==0 && selected){
                            selectedContactsContainer.setVisibility(View.GONE);
                            selected = false;
                        }
                    }
                }, 1000);
            }


        });


        searchContacts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                allContactAdapter.applyFilter(s.toString().trim());
            }
        });
       addContact();
    }

    void addContact(){
       for(Contact contact: Utils.deviceContacts){
           allContactAdapter.add(contact);
       }
    }

    void createGroup(){
        if(selectedContactsAdapter.getContacts().size()==0){
            Toast.makeText(context, "Select at least one member", Toast.LENGTH_LONG).show();
        }

        final String name = groupName.getText().toString().trim();
        if(name.equals("") || name==null){
            groupName.setError("Group name can't be null");
            return;
        }

        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle("Creating Group");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();


        final List<Contact> contacts = selectedContactsAdapter.getContacts();
        final String groupId = mRef.collection("groups").document().getId();
        final Map<String, Object> data = new HashMap<String, Object>(){{
            put("id", groupId);
            put("timestamp", new Timestamp(new Date()));
            put("name", name);
            put("size", contacts.size()+1);
            put("admin", mRef.document("user/"+mAuth.getUid()));
        }};

        List<String> numbers = new ArrayList<>();
        for(Contact c: contacts){
            numbers.add(c.getNumber());
        }

        mRef.collection("user").whereIn("phone", numbers).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    List<DocumentReference> references = new ArrayList<>();
                    for(DocumentSnapshot document: task.getResult()){
                        references.add(mRef.document("user/"+document.getId()));
                    }
                    references.add(mRef.document("user/"+mAuth.getUid()));
                    data.put("members", references);
                    Log.i("Testing", "Matches/Size = "+references.size());
                    progress.setProgress(50);

                    mRef.document("groups/"+groupId).set(data)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                progress.setProgress(100);
                                progress.dismiss();
                                destroy();
                                Log.i("Testing", "Group created");
                            }else{
                                Log.i("Testing", task.getException().getLocalizedMessage());
                            }
                        }
                    });

                }else{
                    Log.i("Testing", "Error in queryBlock"+task.getException().getMessage());
                }
            }
        });
    }

    void updateGroup(){
        if(selectedContactsAdapter.getContacts().size()==0){
            Toast.makeText(context, "Select at least one member", Toast.LENGTH_LONG).show();
        }

        final String newGrpName = groupName.getText().toString().trim();
        if(!isDataChanged()){
            if(newGrpName.equals(name)){
                return;
            }else{
                if(newGrpName.equals("") || newGrpName==null){
                    groupName.setError("Group name can't be empty");
                    return;
                }else{

                }
            }
        }else{

            final Map<String, Object> data = new HashMap<String, Object>(){{
                put("id", group.getId());
                put("name", newGrpName);
                put("size", selectedContactsAdapter.getContacts().size()+1);
                put("admin", mRef.document("user/"+mAuth.getUid()));
            }};

            ArrayList<Contact> hasUser = new ArrayList<>();
            ArrayList<Contact> dontHaveUser = new ArrayList<>();

            for(Contact c: selectedContactsAdapter.getContacts()){
                if(c.getUser()!=null){
                    hasUser.add(c);
                }else{
                    dontHaveUser.add(c);
                }
            }

            final ArrayList<DocumentReference> memberRef = new ArrayList<>();
            memberRef.add(mRef.document("user/"+mAuth.getUid()));


            for(Contact c: hasUser){
                memberRef.add(mRef.document("user/"+c.getUser().getUid()));
            }


            List<String> numbers = new ArrayList<>();
            for(Contact c: dontHaveUser){
                numbers.add(c.getNumber());
            }

            if(dontHaveUser.size()!=0){
                mRef.collection("user").whereIn("phone", numbers).get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (DocumentSnapshot document : task.getResult()) {
                                        memberRef.add(mRef.document("user/" + document.getId()));
                                    }
                                    data.put("members", memberRef);
                                    mRef.document("groups/"+group.getId()).set(data)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()){
                                                        destroy();
                                                        Log.i("Testing", "Group created");
                                                    }else{
                                                        Log.i("Testing", task.getException().getLocalizedMessage());
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            }else{
                data.put("members", memberRef);
                mRef.document("groups/"+group.getId()).set(data)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    destroy();
                                    Log.i("Testing", "Group created");
                                }else{
                                    Log.i("Testing", task.getException().getLocalizedMessage());
                                }
                            }
                        });
            }








        }

    }

    private boolean isDataChanged(){


        int totalMatches = 0;

        Log.i("Testing", "Adapter size: "+selectedContactsAdapter.getContacts().size());
        Log.i("Testing", "Group member size: "+selectedContacts.size());


        if (selectedContactsAdapter.getContacts().size()!=selectedContacts.size()){
            return true;
        }

        for(Contact after: selectedContactsAdapter.getContacts()){
            if(after.getUser() == null){
                return true;
            }

            for(Contact before: selectedContacts){
                if(after.getUser().getUid().equals(before.getUser().getUid())){
                    totalMatches ++;
                    break;
                }
            }
        }

        if(totalMatches == selectedContactsAdapter.getContacts().size()){
            return false;
        }

        return true;
    }

    public void show(){
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        dialog.show();
    }

    public void destroy(){
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);
        dialog.dismiss();
    }
}
