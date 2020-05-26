package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.OffPay.OffpayActivity;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.groupPay.GroupPayActivity;
import com.visionio.sabpay.payment.PayActivity;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity{



    BottomNavigationView bottomNavigationView;

    TextView balanceTv;
    Button wallet;

    Button payBtn;
    Button offerBtn;
    Button gPay;
    Button transactions;
    FirebaseAuth mAuth;
    FirebaseFirestore mRef;
    DocumentReference DocRef;
    ListenerRegistration listenerRegistration;


    String phone;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Activity Started." + getCallingActivity());

        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected){
            startActivity(new Intent(this, OffpayActivity.class));
            finish();
        }



        // TODO: after mainActivity show data in list Item of transaction of group pay
        //startActivity(new Intent(this, GroupPayActivity.class));
        if (FirebaseAuth.getInstance().getCurrentUser()!=null){
            setUp();
        } else {
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        }



    }

    void setUp(){
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseFirestore.getInstance();
        sendData();
        loadImage();
        setBottomNavigationView();
        if(mAuth.getUid() == null){
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        }

        balanceTv = findViewById(R.id.main_bal);

        payBtn = findViewById(R.id.main_btn_pay);

        offerBtn = findViewById(R.id.main_btn_offers);
        transactions = findViewById(R.id.main_btn_transactions);

        wallet = findViewById(R.id.main_btn_wallet);
        gPay = findViewById(R.id.main_btn_gpay);

        gPay.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GroupPayActivity.class)));

        payBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PayActivity.class)));

        offerBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, OfferDisplayActivity.class)));

        wallet.setOnClickListener(v -> {
            try {
                showQR();
            } catch (WriterException e) {
                e.printStackTrace();
            }
        });

        transactions.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TransactionHistory.class)));

        if(mAuth.getCurrentUser() != null){
            loadDataFromServer();
        }

        DocRef = mRef.collection("user").document(mAuth.getCurrentUser().getUid());
        DocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User user = task.getResult().toObject(User.class);
                Paper.book("current").write("user",user);

                phone = user.getPhone();
            }
        });

    }



    private void showQR() throws WriterException {
        Dialog qrCode = new Dialog(MainActivity.this);
        qrCode.requestWindowFeature(Window.FEATURE_NO_TITLE);
        qrCode.setContentView(R.layout.qr_code);

        final ImageView qr_code = qrCode.findViewById(R.id.iv_qr);
        qr_code.setEnabled(true);
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = multiFormatWriter.encode(phone, BarcodeFormat.QR_CODE, 400, 400);
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
        qr_code.setImageBitmap(bitmap);
        qrCode.show();
    }


    void loadDataFromServer(){
        listenerRegistration = mRef.collection("user").document(mAuth.getUid())
                .collection("wallet").document("wallet").addSnapshotListener(MainActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                Wallet wallet = documentSnapshot.toObject(Wallet.class);
                balanceTv.setText(wallet.getBalance().toString());
            }
        });

    }

    private void sendData(){
        User user = Paper.book(FirebaseAuth.getInstance().getCurrentUser().getUid()).read("user");
        if (Paper.book("pending").contains("user")){
            User pending = Paper.book("pending").read("user");
            if (user.getUid().equalsIgnoreCase(FirebaseAuth.getInstance().getCurrentUser().getUid()) && pending.getUid().equalsIgnoreCase(user.getUid())){
                user.setOffPayBalance(pending.getOffPayBalance());
                user.setLogin(true);
                FirebaseFirestore.getInstance().collection("user").document(user.getUid()).set(user)
                        .addOnSuccessListener(aVoid -> {
                            Paper.book("pending").delete("user");
                        });
            }
        }
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("Token", "getInstanceId failed", task.getException());
                        return;
                    }
                    user.setInstanceId(task.getResult().getToken());
                    FirebaseFirestore.getInstance().collection("user").document(user.getUid()).set(user);
                });
    }

    void loadImage() {
        final long ONE_MEGABYTE = 1024 * 1024;
        FirebaseStorage.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                });
    }

    void setBottomNavigationView(){
        bottomNavigationView = findViewById(R.id.main_bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.bottom_app_bar_main_transaction : {
                    Intent intent = new Intent(getApplicationContext(), TransactionHistory.class);
                    startActivityForResult(intent, 1);
                    return true;
                }
                case R.id.bottom_app_bar_main_group : return false;
                case R.id.bottom_app_bar_main_home : return true;
                case R.id.bottom_app_bar_main_logout : {
                    signOut();
                    return true;
                }
                case R.id.bottom_app_bar_main_pay : {
                    return false;
                }
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);
    }

    void signOut(){
        mRef.collection("user").document(mAuth.getUid()).update("login", false)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            listenerRegistration.remove();
                            mAuth.signOut();
                            Intent intent = new Intent(getApplicationContext(), AuthenticationActivity.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(MainActivity.this, "Could not sign out", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1){
            bottomNavigationView.setSelectedItemId(R.id.bottom_app_bar_main_home);
            if(resultCode == 1){
                signOut();
            }
        }
    }
}
