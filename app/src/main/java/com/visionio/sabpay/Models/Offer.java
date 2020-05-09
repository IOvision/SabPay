package com.visionio.sabpay.Models;

public class Offer {

    String offerName;
    String offerDescription;

    public Offer() {
    }

    public Offer(String offerName, String offerDescription){
        this.offerName = offerName;
        this.offerDescription = offerDescription;
    }

    public String getOfferName() { return offerName; }

    public String getOfferDescription() { return offerDescription; }

    public void setOfferName(String offerName) { this.offerName = offerName; }

    public void setOfferDescription(String offerDescription) { this.offerDescription = offerDescription; }

}
