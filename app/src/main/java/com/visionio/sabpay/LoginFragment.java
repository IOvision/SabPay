package com.visionio.sabpay;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {

    FirebaseAuth mAuth;
    ProgressBar progressBar;
    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mAuth = FirebaseAuth.getInstance();

        Button btn_login = view.findViewById(R.id.btn_login);
        Button btn_register = view.findViewById(R.id.btn_register);
        final EditText et_email = view.findViewById(R.id.et_login_email);
        final EditText et_password = view.findViewById(R.id.et_login_password);
        progressBar = view.findViewById(R.id.login_progressBar);

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email = et_email.getText().toString().trim();
                String password = et_password.getText().toString().trim();
                if (email.isEmpty() || password.isEmpty())
                    Toast.makeText(getContext(), "email is empty", Toast.LENGTH_SHORT).show();
                if (!email.isEmpty() && !password.isEmpty())
                   login(email, password);
            }
        });

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Authentication)getActivity()).verifyFragment();
            }
        });
        return view;
    }

    void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("Login:", "SignInWithEmail:Success");
                            progressBar.setVisibility(View.INVISIBLE);
                            updateUI(mAuth.getCurrentUser());
                        } else {
                            Log.w("Login:", "signInWithEmail:failure", task.getException());
                            Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    void updateUI(FirebaseUser user) {
        if(user != null){
            startActivity(new Intent(getContext(), MainActivity.class));
        }
    }
}
