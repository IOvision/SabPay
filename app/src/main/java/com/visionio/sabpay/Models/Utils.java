package com.visionio.sabpay.Models;

import android.content.Context;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;

public class Utils {

    public static int WELCOME_BALANCE = 500;
    public static List<Contact> deviceContacts;

    public static String[] decodePathFromQr(String qrData){
        return qrData.split("/");
    }

    public static String getPathToUser(String qrData){
        String[] data = decodePathFromQr(qrData);
        return "/"+data[1]+"/"+data[2];
    }

    public static int getPaymentType(String qrData){
        // 9773636695
        // /user/qA3urwCl8qMAFpbXvD1MW1hzbsL2/group_pay/meta-data/transaction/ZEAUSEXwtliWZ8XDIx8T

        if(qrData.length()>15){
            return 1;
        }

        return 0;

    }

    public static String formatNumber(String number, int returnType){

        // info: return type 0 for with country code, and -1 for without country code

        /*
         * possible cases
         * 1. 9264966639
         * 2. 09450546077
         * 3. +918196853905
         */
        number = number.replaceAll("\\s", "");

        String reverse = "";
        for(int i=number.length()-1; i>=0; i--){
            if(reverse.length()==10){
                break;
            }
            reverse += number.charAt(i);
        }

        number = "";

        for(int i=reverse.length()-1; i>=0; i--){
            number += reverse.charAt(i);
        }

        if(returnType==0){
            return "+91"+number;
        }
        return number;
    }

    public static boolean isEmpty(String s){
        if(s.equals("") || s.equals(null)){
            return true;
        }
        return false;
    }

    public static Boolean isValidEmail(String email){
        return ((Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$")).matcher(email)).matches();
    }

    public static void toast(Context context, String message, int length){
        Toast.makeText(context, message, length).show();
    }

}
