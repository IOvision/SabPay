package com.visionio.sabpay.interfaces;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class Payment {

    public interface PaymentListener{
    }

    private static Payment pInstance;
    private PaymentListener pListener;
    private DocumentReference receiverDocRef;
    private String name;
    private boolean isSuccessful;

    private Payment(DocumentReference a, String b) {
        receiverDocRef = a;
        name = b;
        isSuccessful = false;
    }

    public static Payment createInstance(DocumentReference a, String b) {
        pInstance = new Payment(a,b);
        Log.d("Pay", "getInstance: "+pInstance.getName());
        return pInstance;
    }

    public static Payment getInstance() {
        return pInstance;
    }

    public void setListener(PaymentListener paymentListener){
        pListener = paymentListener;
    }

    public String getName(){
        return pInstance.name;
    }

    public DocumentReference getReceiverDocRef() {
        return pInstance.receiverDocRef;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccess() {
        isSuccessful = true;
    }
}
