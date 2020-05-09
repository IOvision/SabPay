package com.visionio.sabpay.authentication;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.MainActivity;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.R;

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
                String email = et_email.getText().toString().trim();
                String password = et_password.getText().toString().trim();
                if (email.isEmpty() && password.isEmpty()){
                    et_email.setError("Email cannot be empty");
                    et_password.setError("Password cannot be empty");
                }else if(email.isEmpty()){
                    et_email.setError("Email cannot be empty");
                }else if(password.isEmpty()){
                    et_password.setError("Password cannot be empty");
                }else {
                    progressBar.setVisibility(View.VISIBLE);
                    login(email, password);
                }
            }
        });

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AuthenticationActivity)getActivity()).registerFragment();
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
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    void updateUI(FirebaseUser user) {
        final FirebaseFirestore mRef = FirebaseFirestore.getInstance();
        mRef.collection("user").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    User user = task.getResult().toObject(User.class);
                    if(!user.getLogin()){
                        if(user != null){
                            mRef.collection("user").document(user.getUid()).update("login", true);
                            startActivity(new Intent(getContext(), MainActivity.class));
                            getActivity().finish();
                        }
                    }else{
                        Toast.makeText(getContext(), "User already log-in another device", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
