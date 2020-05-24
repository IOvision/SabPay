package com.visionio.sabpay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.firestore.FirebaseFirestore;
import com.visionio.sabpay.Models.User;

import io.paperdb.Paper;

public class ProfileActivity extends AppCompatActivity {

    EditText fName, lName, email, phone;
    Button done;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        fName = findViewById(R.id.profile_fname);
        lName = findViewById(R.id.profile_lname);
        phone = findViewById(R.id.profile_phone);
        email = findViewById(R.id.profile_email);
        done = findViewById(R.id.profile_done);

        user = Paper.book("current").read("user");

        fName.setText(user.getFirstName());
        lName.setText(user.getLastName());
        phone.setText(user.getPhone());
        phone.setFocusable(false);
        email.setText(user.getEmail());

        done.setOnClickListener(v -> {
            if (!fName.getText().toString().equalsIgnoreCase(user.getFirstName())){
                user.setFirstName(fName.getText().toString());
            }
            if (!lName.getText().toString().equalsIgnoreCase(user.getLastName())){
                user.setLastName(lName.getText().toString());
            }
            if (!email.getText().toString().equalsIgnoreCase(user.getEmail())){
                user.setEmail(email.getText().toString());
            }
            Paper.book("current").write("user",user);
            FirebaseFirestore.getInstance().collection("user").document(user.getUid()).set(user);
            finish();
        });
    }
}
