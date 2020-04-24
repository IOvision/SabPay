package com.visionio.sabpay;


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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.visionio.sabpay.Models.User;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {


    private EditText et_name, et_email, et_password, et_repassword;
    private Button btn_register;
    DatabaseReference mDatabase;
    FirebaseUser user;

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

        mDatabase = FirebaseDatabase.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUser(user);
            }
        });

        return view;
    }

    private void updateUser(final FirebaseUser user) {
        user.updatePassword(et_password.getText().toString().trim())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Password:", "User password updated.");
                            user.updateEmail(et_email.getText().toString().trim())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d("Email:", "User email address updated.");
                                                UserProfileChangeRequest changes = new UserProfileChangeRequest.Builder()
                                                        .setDisplayName(et_name.getText().toString().trim())
                                                        .build();
                                                user.updateProfile(changes)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Log.d("Name:", "User profile updated.");
                                                                    User lUser = new User(user.getPhoneNumber(), 100);
                                                                    mDatabase.child("users").child(user.getPhoneNumber()).setValue(lUser);
                                                                    startActivity(new Intent(getContext(), MainActivity.class));
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        } else {
                            Log.w("Password:", task.getException());
                        }
                    }
                });
    }
}
