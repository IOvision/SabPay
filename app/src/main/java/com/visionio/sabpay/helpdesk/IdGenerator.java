package com.visionio.sabpay.helpdesk;

import java.util.ArrayList;
import java.util.List;

public class IdGenerator {

    public static String getNextId(int currentId){
        IdGenerator generator = new IdGenerator("100AA");
        List<String> dt = new ArrayList<>();
        String nxt = generator.next();
        while(nxt.length()==5){
            dt.add(nxt);
            nxt = generator.next();
        }
        return dt.get(currentId);
    }

    String current;

    String number, alphabet;

    private IdGenerator(String current) {
        // current = 000AA
        this.current = current;
        number = current.substring(0, 3);
        alphabet = decode(current.substring(3, 5));
    }

    private String decode(String dt){
        // return integer form of two char "AA" = 6565
        return ""+(int)dt.charAt(0)+(int)dt.charAt(1);
    }

    private String encode(){
        // return string from 4 digit code 6566 = AB
        int p1 = Integer.parseInt(getAlphabetPart(1));
        int p2 = Integer.parseInt(getAlphabetPart(2));
        return ""+(char)p1+(char)p2;
    }

    private String getAlphabetPart(int part){
        // part =1 or part=2 , 6590 = p1->65 p2->90
        String alph = ""+alphabet;
        if(part==1){
            return alph.substring(0, 2);
        }else if(part==2){
            return alph.substring(2, 4);
        }
        return null;
    }

    private void increment(){
        // return increment int of current alphabet
        if(alphabet.equals("9090")){
            number = String.format("%d", Integer.parseInt(number)+1);
            alphabet = "6565";
        }else if(getAlphabetPart(2).equals("90")){
            // 6690 -> 6765
            alphabet = (Integer.parseInt(getAlphabetPart(1))+1)+"65";
        }else{
            alphabet = getAlphabetPart(1)+(Integer.parseInt(getAlphabetPart(2))+1);
        }
    }

    private String next(){
        // return next code
        increment();
        return number+encode();
    }

}

