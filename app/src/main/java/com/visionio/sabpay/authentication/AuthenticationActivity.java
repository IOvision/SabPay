package com.visionio.sabpay.authentication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.R;

public class AuthenticationActivity extends AppCompatActivity {

    Fragment fragment;
    FragmentManager fragmentManager;
    FrameLayout frameLayout;
    FragmentTransaction fragmentTransaction;
    protected FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        mAuth = FirebaseAuth.getInstance();

        frameLayout = findViewById(R.id.flFragment);
        loginFragment();


    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    public void loginFragment() {

        fragment = new LoginFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        frameLayout.removeAllViews();
        fragmentTransaction.add(R.id.flFragment, fragment);
        fragmentTransaction.commit();
    }

    /*public void verifyFragment(){
        fragment = new VerifyFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();   //Fo
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.replace(R.id.flFragment, fragment);
        fragmentTransaction.commit();
    }*/

    public void registerFragment(){
        fragment = new RegisterFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.replace(R.id.flFragment, fragment);
        fragmentTransaction.commit();
    }


}
