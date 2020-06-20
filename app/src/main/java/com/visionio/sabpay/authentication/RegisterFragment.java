package com.visionio.sabpay.authentication;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.visionio.sabpay.R;
import com.visionio.sabpay.helper.TokenManager;
import com.visionio.sabpay.main.MainActivity;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.models.Wallet;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    private TextInputLayout et_otp, et_phonenumber;
    MaterialToolbar materialToolbar;
    private Button btn_register;
    ProgressBar progressBar;

    FirebaseFirestore mRef;
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;

    String mFirstName;
    String mLastName;
    String mEmail;
    String mPassword;
    String mConfirmPassword;
    String mPhoneNumber;

    public RegisterFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        et_phonenumber = view.findViewById(R.id.et_register_phone);
        et_otp = view.findViewById(R.id.et_register_otp);
        materialToolbar = view.findViewById(R.id.main_top_bar);

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
        if(validateData()){
            progressBar.setVisibility(View.VISIBLE);
            mRef.collection("public").document("registeredPhone").get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        List<String> numbers = (List<String>) task.getResult().get("numbers");
                        if(numbers==null){
                            register();
                            return;
                        }
                        if(numbers.contains(mPhoneNumber)){
                            et_phonenumber.setError("Phone Number already registered");
                            progressBar.setVisibility(View.GONE);
                        }else {
                            register();
                        }
                    }else {
                        Utils.toast(getActivity(), task.getException().getMessage(), Toast.LENGTH_LONG);
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
                             //et_email.setError("User with this email already exists");
                             progressBar.setVisibility(View.INVISIBLE);
                        }else{
                            Log.d("Authentication", "Authentication Failed");
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                    }
                });
    }

    private void addFields(){
        final User user = new User();
        user.setUid(firebaseUser.getUid());
        user.setFirstName(mFirstName);
        user.setLastName(mLastName);
        user.setEmail(mEmail);
        user.setPhone(mPhoneNumber);
        user.setOffPayBalance(200);

        user.setLogin(true);


        final Wallet wallet = new Wallet();
        wallet.setBalance(Utils.WELCOME_BALANCE);
        wallet.setLastTransaction(null);

        mRef.collection("user").document(user.getUid()).set(user);

        mRef.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {

                transaction.update(mRef.collection("public").document("registeredPhone"),"number", FieldValue.arrayUnion(mPhoneNumber));

                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(!task.isSuccessful()){
                    Utils.toast(getContext(), task.getException().getMessage(), Toast.LENGTH_LONG);
                    return;
                }

                mRef.collection("user").document(user.getUid())
                        .collection("wallet").document("wallet").set(wallet)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            TokenManager.handleOnLoginSignUp(getContext());
                            startActivity(new Intent(getActivity(), MainActivity.class));
                            getActivity().finish();
                        }else{
                            Utils.toast(getContext(), task.getException().getMessage(), Toast.LENGTH_LONG);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });
    }

    private void updateVariableData(){
        /*mFirstName = capitalize(et_first_name.getText().toString().trim());
        mLastName = capitalize(et_last_name.getText().toString().trim());
        mEmail = et_email.getText().toString().trim();
        mPassword = et_password.getText().toString().trim();
        mConfirmPassword = et_repassword.getText().toString().trim();
        mPhoneNumber = Utils.formatNumber(et_phonenumber.getText().toString().trim(), 0);*/

    }

    private boolean validateData(){
        /* checking empty cases
        if(Utils.isEmpty(mFirstName)){
            et_first_name.setError("Can't be empty");
            return false;
        }
        if(Utils.isEmpty(mLastName)){
            et_last_name.setError("Can't be empty");
            return false;
        }
        if(Utils.isEmpty(mPhoneNumber) || mPhoneNumber.equals("+91")){
            et_phonenumber.setError("Can't be empty");
            return false;
        }
        if(Utils.isEmpty(mEmail)){
            et_email.setError("Can't be empty");
            return false;
        }
        if(Utils.isEmpty(mPassword)){
            et_password.setError("Can't be empty");
            return false;
        }
        if(Utils.isEmpty(mConfirmPassword)){
            et_repassword.setError("Can't be empty");
            return false;
        }

        //phone check
        if(mPhoneNumber.length()!=13){
            et_phonenumber.setError("Invalid number");
            return false;
        }

        //email check
        if(!Utils.isValidEmail(mEmail)){
            et_email.setError("Email badly formatted");
            return false;
        }


        //password miss-match and length check
        if(mPassword.length() < 6){
            et_password.setError("Min 6 digit required");
            return false;
        }
        if(!mPassword.equals(mConfirmPassword)){
            et_repassword.setError("Password didn't match");
            return false;
        }


        */
        return true;
    }

    public static String capitalize(String s) {
        if ((s == null) || (s.trim().length() == 0)) {
            return s;
        }
        s = s.toLowerCase();
        char[] cArr = s.trim().toCharArray();
        cArr[0] = Character.toUpperCase(cArr[0]);
        for (int i = 0; i < cArr.length; i++) {
            if (cArr[i] == ' ' && (i + 1) < cArr.length) {
                cArr[i + 1] = Character.toUpperCase(cArr[i + 1]);
            }
            if (cArr[i] == '-' && (i + 1) < cArr.length) {
                cArr[i + 1] = Character.toUpperCase(cArr[i + 1]);
            }
            if (cArr[i] == '\'' && (i + 1) < cArr.length) {
                cArr[i + 1] = Character.toUpperCase(cArr[i + 1]);
            }
        }
        return new String(cArr);
    }

}
