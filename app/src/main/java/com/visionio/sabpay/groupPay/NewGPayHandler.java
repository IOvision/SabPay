package com.visionio.sabpay.groupPay;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firestore.v1.Document;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.visionio.sabpay.Models.GroupPay;
import com.visionio.sabpay.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NewGPayHandler {

    Context context;

    Dialog dialog;

    // dialog views
    TextView heading;
    EditText amount;
    Button submit;
    ImageView qrCode;

    //db objects
    FirebaseAuth mAuth;
    DocumentReference userDocRef;

    String qrText;

    public NewGPayHandler(Context context, FirebaseAuth mAuth, FirebaseFirestore mRef) {
        this.context = context;
        this.mAuth = mAuth;
        this.userDocRef = mRef.document("user/"+mAuth.getUid());
        setUp();
    }

    void setUp(){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.new_group_pay_layout);
        dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        heading = dialog.findViewById(R.id.new_gPay_description_tv);
        amount = dialog.findViewById(R.id.new_gPay_amount_et);
        submit = dialog.findViewById(R.id.new_gPay_submit_bt);
        qrCode = dialog.findViewById(R.id.new_gPay_qr_iv);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                heading.setText("Server action in progress");
                initiateGroupPay();
            }
        });

    }

    //'/user/{userId}/group_pay/meta-data/transaction/{transactiosId}/transactions/{id}'

    private void initiateGroupPay(){
        final GroupPay groupPay = getNewGroupPayObject();
        final String id = userDocRef.collection("group_pay/meta-data/transaction").document().getId();

        Map<String, Object> gPayTransactionObject = new HashMap<String, Object>(){{
            put("id", id);
            put("active", groupPay.getActive());
            put("ledger", groupPay.getLedger());
            put("parts", groupPay.getParts());
            put("amount", Integer.parseInt(amount.getText().toString().trim()));
            put("timestamp", new Timestamp(new Date()));
        }};

        userDocRef.collection("group_pay/meta-data/transaction")
                .document(id).set(gPayTransactionObject).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    qrText = "/"+userDocRef.getPath()+"/group_pay/meta-data/transaction/"+id;
                    showQr();
                }else {
                    Log.i("Testing", task.getException().getLocalizedMessage());
                }
            }
        });

    }

    private void showQr(){
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(qrText, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCode.setImageBitmap(bitmap);
        } catch (Exception e){
            e.printStackTrace();
        }
        submit.setText("Exit");
        heading.setText("Scan to pay");
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        qrCode.setVisibility(View.VISIBLE);
    }

    private GroupPay getNewGroupPayObject(){
        GroupPay groupPay = new GroupPay();
        groupPay.setActive(true);
        groupPay.setLedger(new ArrayList<Integer>());
        groupPay.setParts(0);

        return groupPay;
    }

    public void init(){
        dialog.show();
    }


}
