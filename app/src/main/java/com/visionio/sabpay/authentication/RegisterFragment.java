package com.visionio.sabpay.authentication;


import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.MainActivity;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.Models.Wallet;
import com.visionio.sabpay.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    private final static String TAG = "DEBUG";

    private EditText et_name, et_email, et_password, et_repassword, et_phonenumber;
    private Button btn_register;
    ProgressBar progressBar;

    FirebaseFirestore mRef;
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;
    DocumentReference senderDocRef;
    //FirebaseFirestore db = FirebaseFirestore.getInstance();

    String mName;
    String mEmail;
    String mPassword;
    String mConfirmPassword;
    //String mPhone;
    //String mPhoneNumber;
    String mPhoneNumber;

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
        et_phonenumber = view.findViewById(R.id.et_register_phoneNumber);
        btn_register = view.findViewById(R.id.btn_register);
        progressBar = view.findViewById(R.id.progressBar);

        mRef = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseAuth = FirebaseAuth.getInstance();


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
        }else if(mPhoneNumber.length()!=13){
            et_phonenumber.setError("Invalid Phone Number");
        }else if (!mPassword.equals(mConfirmPassword)){
            et_repassword.setError("Password didn't match");
        }else{
            progressBar.setVisibility(View.VISIBLE);
            mRef.collection("user").whereEqualTo("phone", mPhoneNumber).get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                if(!task.getResult().getDocuments().isEmpty()) {
                                    et_phonenumber.setError("Phone Number already registered");
                                    progressBar.setVisibility(View.GONE);
                                }else{
                                    register();
                                }
                            }else{
                                //
                            }
                        }
                    });
        }
    }



    private void register() {
        firebaseAuth.createUserWithEmailAndPassword(mEmail, mPassword)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d("authentication", "Registered");
                            firebaseUser = firebaseAuth.getCurrentUser();
                            addFields();

                        }else if(task.getException() instanceof FirebaseAuthUserCollisionException){
                             et_email.setError("User with this email already exists");
                             progressBar.setVisibility(View.INVISIBLE);
                        }else{
                            Log.d("Authentication", "Authentication Failed");
                            progressBar.setVisibility(View.GONE);
                        }

                    }
                });
    }

    private void addFields(){
        User user = new User();
        user.setUid(firebaseUser.getUid());
        user.setName(mName);
        user.setEmail(mEmail);
        //user.setPhone(mPhoneNumber);
        user.setPhone(mPhoneNumber);

        //Log.d("getPhone", "getphone function " + user.getPhone());

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
                                final DocumentReference documentReference;
                                documentReference = mRef.collection("user").document(firebaseAuth.getUid());
                                documentReference.addSnapshotListener(getActivity(), new EventListener<DocumentSnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                        Log.d("name display", "details name is " + documentSnapshot.getString("name"));
                                        Log.d("name display", "details phone is " + documentSnapshot.getString("phone"));
                                        Log.d("name display", "detail mail is " + documentSnapshot.getString("email"));

                                    }
                                });

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





/*
    private void addFields(){
        String UID = firebaseUser.getUid();
        Map<String, Object> userList = new HashMap<>();
        userList.put("name", mName);
        userList.put("phone", mPhoneNumber);
        userList.put("email", mEmail);
        userList.put("uid", UID);


        final Wallet wallet = new Wallet();
        wallet.setBalance(0);
        wallet.setLastTransaction(null);

        mRef.collection("user").document(firebaseUser.getUid()).set(userList).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){

                    mRef.collection("user").document(firebaseUser.getUid())
                            .collection("wallet").document("wallet")
                            .set(wallet).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Log.d(TAG, "Document Snapshot added with ID: ");
                                startActivity(new Intent(getActivity(), MainActivity.class));
                                getActivity().finish();
                            }else{
                                Log.w(TAG, "Error adding document: ");
                            }
                        }
                    });

                }else{
                }
            }
        });

    }


 */

/*

    private void updateDatabase(){
        User user = new User();
        user.setUid(firebaseUser.getUid());
        user.setName(mName);
        user.setEmail(mEmail);
        user.setPhone(mPhoneNumber);
        //user.setPhone(mPhone);

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

 */

    private Boolean isValidEmail(String email){
        return ((Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$")).matcher(email)).matches();
    }

    private void updateVariableData(){
        mPhoneNumber = "+91";
        mName = et_name.getText().toString().trim();
        mEmail = et_email.getText().toString().trim();
        mPassword = et_password.getText().toString().trim();
        mConfirmPassword = et_repassword.getText().toString().trim();
        mPhoneNumber += et_phonenumber.getText().toString().trim();
        //mPhone = firebaseUser.getPhoneNumber();
        mConfirmPassword = et_repassword.getText().toString().trim();
    }

}
