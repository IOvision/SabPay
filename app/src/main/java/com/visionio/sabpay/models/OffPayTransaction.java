package com.visionio.sabpay.models;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OffPayTransaction implements Serializable {

    String fromDocRef;
    String toDocRef;
    String code;
    Integer amount;

    public OffPayTransaction(byte[] bytes){
        ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream in = new ObjectInputStream(byteIn);
            Map<String, Object> data2 = (Map<String, Object>) in.readObject();
            fromDocRef = (String) data2.get("fromDocRef");
            amount = (Integer) data2.get("amount");
            toDocRef = FirebaseAuth.getInstance().getUid();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public OffPayTransaction(String fromDocRef, int amount) {
        this.fromDocRef = fromDocRef;
        this.amount = amount;
    }

    public byte[] toBytes() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("fromDocRef", this.fromDocRef);
        data.put("amount", this.amount);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(byteOut);
            out.writeObject(data);
            return byteOut.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getAmount() {
        return amount;
    }
}
