package com.visionio.sabpay.authentication;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.CubeGrid;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.R;
import com.visionio.sabpay.helper.TokenManager;
import com.visionio.sabpay.main.MainActivity;
import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import java.util.ArrayList;
import java.util.List;

import io.paperdb.Paper;

public class LoginFragment extends Fragment {

    FirebaseAuth mAuth;
    RelativeLayout progress, loginForm;
    SpinKitView progressBar;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        mAuth = FirebaseAuth.getInstance();
        container.removeAllViews();
        Button btn_login = view.findViewById(R.id.btn_login);

        final EditText et_email = view.findViewById(R.id.login_email);
        final EditText et_password = view.findViewById(R.id.login_password);
        progress = view.findViewById(R.id.login_progress);
        loginForm = view.findViewById(R.id.login_form);
        progressBar = view.findViewById(R.id.login_progressBar);
        Sprite sprite = new CubeGrid();
        progressBar.setIndeterminateDrawable(sprite);

        btn_login.setOnClickListener(v -> {
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
                progressOn();
                login(email, password);
            }
        });

        return view;
    }

    void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Login:", "SignInWithEmail:Success");
                        updateUI(mAuth.getCurrentUser());
                    } else {
                        Log.w("Login:", "signInWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                        progressOff();
                    }
                });
    }

    void updateUI(FirebaseUser user) {
        final FirebaseFirestore mRef = FirebaseFirestore.getInstance();
        mRef.collection("user").document(user.getUid()).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                User user1 = task.getResult().toObject(User.class);
                if(!user1.getLogin()){
                    mRef.collection("user").document(user1.getUid()).update("login", true);
                    storeData(user1);
                    TokenManager.handle(getContext());
                    startActivity(new Intent(getContext(), MainActivity.class));
                    getActivity().finish();
                }else{
                    Toast.makeText(getContext(), "User already log-in another device", Toast.LENGTH_SHORT).show();
                    progressOff();
                }
            }else{
                Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                progressOff();
            }
        });

    }

    private void storeData(User user) {
        Paper.book(user.getUid()).write("user",user);
    }

    void progressOn(){
        loginForm.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    void progressOff(){
        loginForm.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
    }
}
