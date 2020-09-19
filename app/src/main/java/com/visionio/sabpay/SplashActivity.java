package com.visionio.sabpay;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.authentication.AuthenticationActivity;
import com.visionio.sabpay.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        (new Handler()).postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null){
                startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
            } else if (FirebaseAuth.getInstance().getCurrentUser().getEmail() != null) {
                startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
            finish();
        }, 1500);

    }
}
