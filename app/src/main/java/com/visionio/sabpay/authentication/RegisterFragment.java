package com.visionio.sabpay.authentication;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.visionio.sabpay.R;
import com.visionio.sabpay.helper.TokenManager;
import com.visionio.sabpay.main.MainActivity;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.models.Wallet;

import java.util.concurrent.TimeUnit;

import io.paperdb.Paper;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    TextInputLayout til1, til2;
    TextView firstTV, secondTV;
    Button next;
    ProgressBar progressBar;
    int state = 0; //1-Phone Verification 2-Email 3-Password 4-Name


    FirebaseFirestore mRef;

    String mName;
    String mFirstName;
    String mLastName;
    String mEmail;
    String mPassword;
    String mConfirmPassword;
    String mPhoneNumber;

    private String mVerificationId;

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        String TAG = "Phone:";

        @Override
        public void onVerificationCompleted(PhoneAuthCredential credential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.

            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                // ...

            } else if (e instanceof FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                // ...

            }
            Log.i("test", e.getLocalizedMessage());

            // Show a message and update the UI
            // ...
        }

        @Override
        public void onCodeSent(@NonNull final String verificationId,
                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
            mVerificationId = verificationId;
            nextState();
        }
    };

    public RegisterFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        firstTV = view.findViewById(R.id.register_textView3);
        secondTV = view.findViewById(R.id.register_textView4);

        til1 = view.findViewById(R.id.register_til1);
        til2 = view.findViewById(R.id.register_til2);

        progressBar = view.findViewById(R.id.register_progress_bar);

        next = view.findViewById(R.id.register_btn_next);
        next.setOnClickListener(v -> {
           buttonStateManager();
        });

        mRef = FirebaseFirestore.getInstance();
        stateManager();
        return view;
    }

    private void buttonStateManager() {
        if (state==0){
            mPhoneNumber = "+91";
            mPhoneNumber = mPhoneNumber.concat(til1.getEditText().getText().toString());
            if(Utils.isEmpty(mPhoneNumber) || mPhoneNumber.equals("+91")) {
                til1.setError("Can't be empty");
            } else {
                if (mPhoneNumber.length() != 13) {
                    til1.setError("Invalid number");
                } else {
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(mPhoneNumber, 60, TimeUnit.SECONDS, getActivity(), mCallbacks);
                }
            }
        }
        else if(state==1) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, til2.getEditText().getText().toString().trim());
            signInWithPhoneAuthCredential(credential);
        }
        else if (state==2){
            mEmail = til1.getEditText().getText().toString();
            if(Utils.isEmpty(mEmail)){
                til1.setError("Can't be empty");
            } else {
                nextState();
            }
        }
        else if (state==3){
            mPassword = til1.getEditText().getText().toString();
            mConfirmPassword = til2.getEditText().getText().toString();
            if (!mPassword.equals(mConfirmPassword))
                til2.setError("Password do not match.");
            else
                nextState();
        }
        else if (state==4){
            mName = til1.getEditText().getText().toString();
            if (Utils.isEmpty(mName)){
                til1.setError("Name cannot be empty");
            } else {
                mName = capitalize(mName);
                mFirstName = "";
                mLastName = "";
                if(mName.split("\\w+").length>1){

                    mLastName = mName.substring(mName.lastIndexOf(" ")+1);
                    mFirstName = mName.substring(0, mName.lastIndexOf(' '));
                }
                else{
                    mFirstName = mName;
                }
                nextState();
            }
        }
    }

    private void addFields(){
        firstTV.setText("Logging you in.");
        til1.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        final User user = new User();
        user.setUid(FirebaseAuth.getInstance().getUid());
        user.setFirstName(mFirstName);
        user.setLastName(mLastName);
        user.setEmail(mEmail);
        user.setPhone(mPhoneNumber);
        user.setOffPayBalance(200);

        user.setLogin(true);
        Paper.book("user").write("user",user);

        final Wallet wallet = new Wallet();
        wallet.setBalance(Utils.WELCOME_BALANCE);
        wallet.setLastTransaction(null);

        FirebaseAuth.getInstance().getCurrentUser().updateEmail(mEmail).addOnCompleteListener(task -> {
           if (task.isSuccessful()){
               FirebaseAuth.getInstance().getCurrentUser().updatePassword(mPassword).addOnCompleteListener(task1 -> {
                   if (task1.isSuccessful()){
                       UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                               .setDisplayName(mName)
                               .build();
                       FirebaseAuth.getInstance().getCurrentUser().updateProfile(profileUpdates)
                               .addOnCompleteListener(task2 -> {
                                   if (!task2.isSuccessful()){
                                   }
                               });
                   } else {
                   }
               });
           } else {
           }
        });

        mRef.collection("user").document(user.getUid()).set(user);

        mRef.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) {
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
                        }
                    }
                });
            }
        });
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

    public void prevState(){
        --state;
        stateManager();
    }

    public void nextState(){
        ++state;
        stateManager();
    }

    public void stateManager(){
        if (state==0)
            phone();
        else if (state==1)
            code();
        else if (state==2)
            email();
        else if (state==3)
            password();
        else if (state==4)
            name();
        else if (state==5)
            addFields();
    }

    private void code() {
        secondTV.setVisibility(View.VISIBLE);
        til2.setVisibility(View.VISIBLE);
    }

    private void name() {
        til1.getEditText().setText("");
        til2.setVisibility(View.GONE);
        firstTV.setText("What's your name?");
        til1.setHint("Name");
    }

    private void password() {
        til1.getEditText().setText("");
        til2.getEditText().setText("");
        firstTV.setText("Security comes First");
        til2.setVisibility(View.VISIBLE);
        til1.setHint("Enter your password");
        til2.setHint("Confirm your password");
    }

    private void email() {
        til1.getEditText().setText("");
        secondTV.setVisibility(View.GONE);
        til2.setVisibility(View.GONE);
        til1.setPrefixText("");
        til1.setHint("Email");
        firstTV.setText(R.string.register_email);
    }

    private void phone() {
        secondTV.setVisibility(View.GONE);
        til2.setVisibility(View.GONE);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null){
                            nextState();
                            progressBar.setVisibility(View.GONE);
                        }
                    } else {
                        // Sign in failed, display a message and update the UI
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(getContext(), "Wrong OTP!", Toast.LENGTH_SHORT).show();
                            prevState();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
