package com.visionio.sabpay;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.main.MainActivity;
import com.visionio.sabpay.models.Cart;
import com.visionio.sabpay.models.Inventory;

import io.paperdb.Paper;

public class SplashActivity extends AppCompatActivity {

    ImageView imageView;
    Inventory mInventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseFirestore.getInstance().collection("inventory").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot inventories) {
                        if(inventories.isEmpty()){
                            Log.d("testing", "onSuccess: No inventory found");
                            return;
                        }
                        mInventory = inventories.getDocuments().get(0).toObject(Inventory.class);
                        Cart.getInstance().setInv_id(mInventory.getId());
                        Paper.book().write("json", mInventory.getJson());
                        if (FirebaseAuth.getInstance().getCurrentUser() == null){
                            startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
                        } else if (FirebaseAuth.getInstance().getCurrentUser().getEmail() == null) {
                            startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        }
                        finish();
                    }
                });
    }
}
