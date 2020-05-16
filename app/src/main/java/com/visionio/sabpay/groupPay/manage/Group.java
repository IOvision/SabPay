package com.visionio.sabpay.groupPay.manage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.Models.Contact;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Utils;
import com.visionio.sabpay.adapter.SelectedContactsAdapter;

import java.util.ArrayList;
import java.util.List;

public class Group {

    //Firebase objects
    String id;
    String name;
    Integer size;
    Timestamp timestamp;
    DocumentReference admin;
    List<DocumentReference> members;

    // local objects
    RecyclerView membersListView;
    SelectedContactsAdapter memberContactsAdapter;
    private Boolean isDataLoaded=false;
    boolean isMeAdmin = false;

    List<Contact> membersContactList = new ArrayList<>();


    public Group() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public DocumentReference getAdmin() {
        return admin;
    }

    public void setAdmin(DocumentReference admin) {
        this.admin = admin;
    }

    public List<DocumentReference> getMembers() {
        return members;
    }

    public void setMembers(List<DocumentReference> members) {
        this.members = members;
    }

    public List<Contact> getMembersContactList() {
        return membersContactList;
    }

    public void setMembersListView(RecyclerView recyclerView){
        membersListView = recyclerView;
    }

    public void setUpMembersList(Context context){
        if(isDataLoaded){
            return;
        }
        membersListView.setHasFixedSize(true);
        membersListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        memberContactsAdapter = new SelectedContactsAdapter(new ArrayList<Contact>());
        memberContactsAdapter.setType(1);

        membersListView.setAdapter(memberContactsAdapter);

        if(admin.getId().equals(FirebaseAuth.getInstance().getUid())){
            isMeAdmin = true;
        }

        for(DocumentReference reference: members){
            if(reference.getId().equals(FirebaseAuth.getInstance().getUid())){
                Contact contact = new Contact();
                contact.setName("You");
                memberContactsAdapter.add(contact);
                continue;
            }
            reference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        User user = task.getResult().toObject(User.class);
                        Contact contact = new Contact();
                        contact.setNumber(user.getPhone());
                        contact.setName(getLocalName(user));
                        contact.setUser(user);
                        memberContactsAdapter.add(contact);
                        membersContactList.add(contact);
                    }
                }
            });
        }
        isDataLoaded = true;
    }

    private String getLocalName(User user){
        // this function returns the name saved on user's device for his friend
        List<Contact> local = Utils.deviceContacts;
        if(local==null){
            return user.getName();
        }
        for(Contact contact: local){
            if(contact.getNumber().equals(user.getPhone())){
                return contact.getName();
            }
        }
        return user.getName();
    }

}
