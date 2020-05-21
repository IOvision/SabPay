package com.visionio.sabpay.Models;

import android.util.Log;

import java.io.Serializable;

public class User implements Serializable {

    String uid;
    String firstName;
    String lastName;
    String phone;
    String email;
    Boolean login;
    String instanceId;
    Integer offPayBalance;

    public User() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public Boolean getLogin() {
        return login;
    }

    public void setLogin(Boolean login) {
        this.login = login;
    }

    public String getName(){
        return firstName+" "+lastName;
    }

    public void setOffPayBalance(Integer amount) {
        this.offPayBalance = amount;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
