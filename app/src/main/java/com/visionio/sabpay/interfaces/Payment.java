package com.visionio.sabpay.interfaces;

import com.visionio.sabpay.models.Contact;

import java.util.List;

public class Payment {

    private static Payment pInstance;

    private List<Contact> contacts;


    private Payment() {

    }

    public static Payment getInstance() {
        if(pInstance != null){
            return pInstance;
        }
        pInstance = new Payment();
        return  pInstance;
    }

    public void addPayee(List<Contact> contacts){
        this.contacts = contacts;
    }

    public List<Contact> getPayee(){
        return contacts;
    }

}
