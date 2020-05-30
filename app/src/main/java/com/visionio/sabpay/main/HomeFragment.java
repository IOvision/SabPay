package com.visionio.sabpay.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.github.ybq.android.spinkit.style.Wave;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.MainInterface;
import com.visionio.sabpay.models.User;

import io.paperdb.Paper;

public class HomeFragment extends Fragment {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    CircularImageView avatar;
    TextView balanceTv;
    TextView name;
    ImageView addMoney;
    ProgressBar balance_pb;
    MainInterface mainListener;

    public void setListener(MainInterface listener){
        mainListener = listener;
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        balanceTv = view.findViewById(R.id.home_bal);
        avatar = view.findViewById(R.id.home_avatar);
        name = view.findViewById(R.id.home_name);
        addMoney = view.findViewById(R.id.home_add_money);
        balance_pb = view.findViewById(R.id.balance_progressBar);

        Sprite wave = new ThreeBounce();
        balance_pb.setIndeterminateDrawable(wave);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainListener.setBalanceTv(balanceTv, balance_pb, addMoney);
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        setAvatar();
        setName();
    }

    void setAvatar(){
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
                Toast.makeText(getContext(), "No Such file or Path found!!", Toast.LENGTH_LONG).show();
            }
        });
    }

    void setName(){
        if (Paper.book(mAuth.getUid()).contains("user")) {
            User user = Paper.book(mAuth.getUid()).read("user");
            name.setText("Hi, " + user.getFirstName());
        } else {
            FirebaseFirestore.getInstance().collection("users").document(mAuth.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    User user = documentSnapshot.toObject(User.class);
                    Paper.book(user.getUid()).write("user", user);
                    name.setText("Hi, " + user.getFirstName());
                }
            });
        }
    }
}