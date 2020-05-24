package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.OffPay.OffpayActivity;
import com.visionio.sabpay.adapter.TransactionAdapter;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.groupPay.GroupPayActivity;
import com.visionio.sabpay.payment.PayActivity;

import java.util.ArrayList;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity{

    FirebaseAuth mAuth;
    FirebaseFirestore mRef;
    DocumentReference DocRef;
    CircularImageView avatar;

    TextView balanceTv;
    Button wallet;
    TextView name;

    Button payBtn;
    ImageView signOutBtn;
    Button offerBtn;
    Button gPay;
    Button transactions;

    ListenerRegistration  listenerRegistration;

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

        if(mAuth.getUid() == null){
            startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
            finish();
        }

        name = findViewById(R.id.main_header_name);
        balanceTv = findViewById(R.id.main_bal);

        payBtn = findViewById(R.id.main_btn_pay);
        signOutBtn = findViewById(R.id.main_signout);
        offerBtn = findViewById(R.id.main_btn_offers);
        transactions = findViewById(R.id.main_btn_transactions);
        avatar = findViewById(R.id.main_avatar);

        wallet = findViewById(R.id.main_btn_wallet);
        gPay = findViewById(R.id.main_btn_gpay);

        signOutBtn.setOnClickListener(v -> signOut());

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
                name.setText(user.getFirstName());
                phone = user.getPhone();
            }
        });

        avatar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, avatar, ViewCompat.getTransitionName(avatar));
            startActivity(intent, options.toBundle());
        });
    }

    void signOut(){
        mRef.collection("user").document(mAuth.getUid()).update("login", false)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    listenerRegistration.remove();
                    mAuth.signOut();
                    startActivity(new Intent(MainActivity.this, AuthenticationActivity.class));
                    finish();
                }else{
                    Toast.makeText(MainActivity.this, "Could not sign out", Toast.LENGTH_SHORT).show();
                }
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
        name.setText(mAuth.getCurrentUser().getDisplayName());
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
        FirebaseInstanceId a = FirebaseInstanceId.getInstance();
        user.setInstanceId(a.getId());
        FirebaseFirestore.getInstance().collection("user").document(user.getUid()).set(user);
    }
}
