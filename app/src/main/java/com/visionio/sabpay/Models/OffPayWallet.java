package com.visionio.sabpay.Models;

import java.io.Serializable;

public class OffPayWallet implements Serializable {

    private Integer balance;

    OffPayWallet() {
        balance = 0;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

}
