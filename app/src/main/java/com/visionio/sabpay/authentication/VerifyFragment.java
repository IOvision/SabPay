package com.visionio.sabpay.authentication;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.visionio.sabpay.R;

import java.util.concurrent.TimeUnit;

public class VerifyFragment extends Fragment {

    private final static String TAG = "DEBUG";


    private FirebaseAuth mAuth;
    private Button btn_verify;
    private EditText et_OTP;
    private ProgressBar progressBar;

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

            // Show a message and update the UI
            // ...
        }

        @Override
        public void onCodeSent(@NonNull final String verificationId,
                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
            mVerificationId = verificationId;
        }
    };

    public VerifyFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_verify, container, false);

        mAuth = FirebaseAuth.getInstance();

        progressBar = view.findViewById(R.id.progressBar);
        et_OTP = view.findViewById(R.id.et_otp);
        final EditText et_number = view.findViewById(R.id.et_number);
        Button btn_sendOTP = view.findViewById(R.id.btn_sendOtp);
        btn_verify = view.findViewById(R.id.btn_verify);

        btn_sendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                sentOTP(et_number.getText().toString().trim());
            }
        });

        btn_verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, et_OTP.getText().toString().trim());
                signInWithPhoneAuthCredential(credential);
            }
        });


        return view;
    }

    private void sentOTP(String phone) {
        String mobileNumber = "+91";
        mobileNumber = mobileNumber.concat(phone);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(mobileNumber, 60, TimeUnit.SECONDS, getActivity(), mCallbacks);
    }



    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = task.getResult().getUser();
                            progressBar.setVisibility(View.INVISIBLE);
                            if (user != null){

                            }
                        } else {
                            // Sign in failed, display a message and update the UI
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(getContext(), "Wrong OTP!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }


}
