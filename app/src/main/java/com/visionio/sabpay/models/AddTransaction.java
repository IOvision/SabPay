package com.visionio.sabpay.models;


public class AddTransaction {
    final String amount;//

    final String customerID;//
    String orderID;//
    String url;
    String checksum;//

    public AddTransaction(String Amount, String customerID) {
        this.amount = Amount + ".00";
        this.customerID = customerID;

    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getOrderID() {
        return orderID;
    }

    public String getCustomerID(){
        return customerID;
    }

    public String getAmount() {
        return amount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setChecksum(String response) {
        this.checksum = response;
    }

    public String getChecksum() { return checksum; }

}