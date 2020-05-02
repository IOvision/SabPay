package com.visionio.sabpay.Models;

import android.util.Log;

public class User {

    String uid;
    String name;
    String phone;
    String email;

    public User() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        Log.d("setPhone ", "Entered into setPhone function");
        this.phone = phone;
    }

    public String getPhone(){ return phone;}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
