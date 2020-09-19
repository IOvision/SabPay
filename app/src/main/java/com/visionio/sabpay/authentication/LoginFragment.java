package com.visionio.sabpay.authentication;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.CubeGrid;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.R;
import com.visionio.sabpay.helper.TokenManager;
import com.visionio.sabpay.main.MainActivity;
import com.visionio.sabpay.models.User;

import java.util.Objects;

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
                        updateUI(Objects.requireNonNull(mAuth.getCurrentUser()));
                    } else {
                        Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                        progressOff();
                    }
                });
    }

    void updateUI(FirebaseUser user) {
        final FirebaseFirestore mRef = FirebaseFirestore.getInstance();
        mRef.collection("user").document(user.getUid()).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                User user1 = Objects.requireNonNull(task.getResult()).toObject(User.class);
                storeData(user1);
                TokenManager.handleOnLoginSignUp(Objects.requireNonNull(getActivity()).getApplicationContext());
                progressOff();

                startActivity(new Intent(getContext(), MainActivity.class));
                getActivity().finish();
                /*if(!user1.getLogin()){
                    storeData(user1);
                    TokenManager.handleOnLoginSignUp(getActivity().getApplicationContext());
                    startActivity(new Intent(getContext(), MainActivity.class));
                    getActivity().finish();
                }else{
                    Toast.makeText(getContext(), "User already log-in another device", Toast.LENGTH_SHORT).show();
                    progressOff();
                }*/
            }else{
                Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                progressOff();
            }
        });
    }

    private void storeData(User user) {
        Paper.book("user").write("user",user);
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
