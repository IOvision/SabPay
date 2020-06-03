package com.visionio.sabpay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.visionio.sabpay.models.User;

import java.io.File;

import io.paperdb.Paper;

public class ProfileActivity extends AppCompatActivity {

    EditText fName, lName, email, phone;
    ImageView avatar;
    Button done;
    User user;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid());


        final long ONE_MEGABYTE = 1024 * 1024;
        storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                avatar.setImageBitmap(bmp);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getApplicationContext(), "No Such file or Path found!!", Toast.LENGTH_LONG).show();
            }
        });

        fName = findViewById(R.id.profile_fname);
        lName = findViewById(R.id.profile_lname);
        phone = findViewById(R.id.profile_phone);
        email = findViewById(R.id.profile_email);
        done = findViewById(R.id.profile_done);
        avatar = findViewById(R.id.profile_avatar);

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

        avatar.setOnClickListener(v -> {
            Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 2);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri selectedImage = data.getData();
        String[] filePath = {MediaStore.Images.Media.DATA};
        Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
        c.moveToFirst();
        int columnIndex = c.getColumnIndex(filePath[0]);
        String picturePath = c.getString(columnIndex);
        c.close();
        Log.w("Path:", picturePath + "");
        Uri file = Uri.fromFile(new File(picturePath));
        UploadTask uploadTask = storageReference.putFile(file);

        uploadTask.addOnFailureListener(exception -> {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("Image", "onActivityResult: "+ exception.getCause());
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ProfileActivity.this, "Photo Uploaded!", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
