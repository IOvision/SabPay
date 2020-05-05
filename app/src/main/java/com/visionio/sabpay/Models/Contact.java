package com.visionio.sabpay.Models;

import com.google.firebase.firestore.DocumentReference;

public class Contact {

    String id;
    String name;
    String number;

    // this is user object from server to which this contact is mapped to
    User user;
    DocumentReference reference;

    public Contact() {
    }

    public Contact(String id, String name, String number) {
        this.id = id;
        this.name = name;
        this.number = number;
        formatNumber();
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

    public void setNumber(String number) {
        this.number = number;
        formatNumber();
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

    private void formatNumber(){
        /*
        * possible cases
        * 1. 9264966639
        * 2. 09450546077
        * 3. +918196853905
         */
        number = number.replaceAll("\\s", "");

        String reverse = "";
        for(int i=number.length()-1; i>=0; i--){
            if(reverse.length()==10){
                break;
            }
            reverse += number.charAt(i);
        }

        number = "";

        for(int i=reverse.length()-1; i>=0; i--){
            number += reverse.charAt(i);
        }number = "+91"+number;

    }
}
