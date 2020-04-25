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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.MainActivity;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.R;

import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    private final static String TAG = "DEBUG";

    private EditText et_name, et_email, et_password, et_repassword;
    private Button btn_register;

    FirebaseFirestore mRef;
    FirebaseUser firebaseUser;

    String mName;
    String mEmail;
    String mPassword;
    String mConfirmPassword;
    String mPhone;

    public RegisterFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        et_name = view.findViewById(R.id.et_register_name);
        et_email = view.findViewById(R.id.et_register_email);
        et_password = view.findViewById(R.id.et_register_password);
        et_repassword = view.findViewById(R.id.et_register_repassword);
        btn_register = view.findViewById(R.id.btn_register);

        mRef = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUser();
            }
        });


        return view;
    }

    private void updateUser() {
        updateVariableData();
        if(!isValidEmail(mEmail)){
            et_email.setError("Invalid email");
        }else if (!mPassword.equals(mConfirmPassword)){
            et_repassword.setError("Password didn't match");
        }else{
            registerEmail();
        }
    }

    private void registerEmail(){
        firebaseUser.updateEmail(mEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    registerPassword();
                }else{
                }
            }
        });
    }

    private void registerPassword(){
        firebaseUser.updatePassword(mPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    registerDisplayName();
                }else{
                }
            }
        });
    }

    private void registerDisplayName(){
        UserProfileChangeRequest changeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(mName).build();
        firebaseUser.updateProfile(changeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    updateDatabase();
                }else{
                }
            }
        });
    }

    private void updateDatabase(){
        User user = new User();
        user.setName(mName);
        user.setEmail(mEmail);
        user.setPhone(mPhone);

        final Wallet wallet = new Wallet();
        wallet.setBalance(0);
        wallet.setLastTransaction(null);

        mRef.collection("user").document(firebaseUser.getUid()).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){

                    mRef.collection("user").document(firebaseUser.getUid())
                            .collection("wallet").document("wallet")
                            .set(wallet).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                startActivity(new Intent(getActivity(), MainActivity.class));
                                getActivity().finish();
                            }else{
                            }
                        }
                    });

                }else{
                }
            }
        });
    }

    private Boolean isValidEmail(String email){
        return ((Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$")).matcher(email)).matches();
    }

    private void updateVariableData(){
        mName = et_name.getText().toString().trim();
        mEmail = et_email.getText().toString().trim();
        mPassword = et_password.getText().toString().trim();
        mConfirmPassword = et_repassword.getText().toString().trim();
        mPhone = firebaseUser.getPhoneNumber();
        mConfirmPassword = et_repassword.getText().toString().trim();
    }

}
