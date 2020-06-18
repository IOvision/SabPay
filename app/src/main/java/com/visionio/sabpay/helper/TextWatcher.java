package com.visionio.sabpay.helper;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import kotlin.Triple;

public class TextWatcher {

    int cursor;
    String raw;
    String[] array;
    List<Triple<String, Integer, Integer>> transactions = new ArrayList<>();
    List<Pair<String, Integer>> contacts = new ArrayList<>();

    public TextWatcher(String data, int cursor) {
        raw = data;
        this.cursor = cursor;
        //transactions = raw.split("#");
        //contacts = raw.split("@");
        array = raw.split(" ");
        int pos = 0;
        for (String curr : array) {
            if (curr.charAt(0) == '@') {
                contacts.add(new Pair<>(curr, pos));
            } else if (curr.charAt(0) == '#') {
                transactions.add(new Triple<>(curr, pos, curr.length()));
            }
            pos += curr.length() + 1;
        }
    }

    public void displayTransaction() {
        for (Triple<String, Integer, Integer> a : transactions) {
            log("Transaction: " + a.getFirst() + " " + a.getSecond() + " " + a.getThird());
        }
    }

    public void displayContacts() {
        for (Pair<String, Integer> a : contacts) {
            log("Contact: " + a.first + " " + a.second);
        }
    }

    void log(String txt) {
        Log.i("test", "HelpDesk: " + txt);
    }
}
