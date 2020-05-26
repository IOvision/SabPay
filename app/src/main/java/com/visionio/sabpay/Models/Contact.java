package com.visionio.sabpay.Models;

import android.widget.RadioButton;

import com.google.firebase.firestore.DocumentReference;

public class Contact {

    String id;
    String name;
    String number;

    RadioButton selector;
    public int positionInAdapter; // tells the position of this contact in adapter

    // this is user object from server to which this contact is mapped to
    User user;
    DocumentReference reference;

    public Contact() {
    }

    public Contact(String id, String name, String number) {
        this.id = id;
        this.name = name;
        this.number = Utils.formatNumber(number, 0);;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public RadioButton getSelector() {
        return selector;
    }

    public void setSelector(RadioButton selector) {
        this.selector = selector;
    }

    public void setNumber(String number) {
        this.number = Utils.formatNumber(number, 0);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DocumentReference getReference() {
        return reference;
    }

    public void setReference(DocumentReference reference) {
        this.reference = reference;
    }


}
