package com.visionio.sabpay.services;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.R;
import com.visionio.sabpay.helpdesk.HelpDeskActivity;

import java.util.Date;
import java.util.HashMap;

public class FeedbackActivity extends AppCompatActivity {

    ProgressBar progressBar;
    RatingBar ratingBar;
    EditText feedback_note;
    Button feedback_submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ratingBar = findViewById(R.id.feedback_bar);
        feedback_note = findViewById(R.id.feedback_note_et);
        feedback_submit = findViewById(R.id.feedback_submit);
        progressBar = findViewById(R.id.offpay_progressbar);

        feedback_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String note = feedback_note.getText().toString();
                float stars = ratingBar.getRating();

                FirebaseFirestore mRef = FirebaseFirestore.getInstance();
                mRef.collection("feedback").add(new HashMap<String, Object>(){{
                    put("fromUid", FirebaseAuth.getInstance().getUid());
                    put("fromName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                    put("note", note);
                    put("stars", stars);
                    put("time", new Timestamp(new Date()));
                }}).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if(task.isSuccessful()){
                            AlertDialog.Builder alert = new AlertDialog.Builder(FeedbackActivity.this);
                            alert.setTitle("Thank You!!");
                            alert.setMessage("We appreciate your feedback.");
                            alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            });
                            alert.show();
                        }else{
                            Toast.makeText(FeedbackActivity.this, "Exception occurred " + task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

    }
}