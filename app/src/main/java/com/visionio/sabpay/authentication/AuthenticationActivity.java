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
    Button btn_login, btn_signup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        mAuth = FirebaseAuth.getInstance();

        frameLayout = findViewById(R.id.flFragment);

        setup();






    }

    @Override
    protected void onResume() {
        super.onResume();
        setup();
    }

    private void setup() {
        LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.layout_authentication, null);
        frameLayout.addView(view);

        btn_login = view.findViewById(R.id.authentication_button_login);
        btn_signup = view.findViewById(R.id.authentication_button_signup);

        btn_login.setOnClickListener(v -> loginFragment());
        btn_signup.setOnClickListener(v -> registerFragment());
    }

    public void loginFragment() {

        fragment = new LoginFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);
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
