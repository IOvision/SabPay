package com.visionio.sabpay.interfaces;

import com.visionio.sabpay.adapter.SelectedContactsAdapter;

public class Payment {

    private static Payment pInstance;

    private SelectedContactsAdapter adapter;


    private Payment() {

    }

    public static Payment getInstance() {
        if(pInstance != null){
            return pInstance;
        }
        pInstance = new Payment();
        return  pInstance;
    }

    public SelectedContactsAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(SelectedContactsAdapter adapter) {
        this.adapter = adapter;
    }
}
