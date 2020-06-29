package com.visionio.sabpay.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.visionio.sabpay.R;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;
import com.visionio.sabpay.models.Wallet;

import io.paperdb.Paper;

public class HomeFragment extends Fragment {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore mRef = FirebaseFirestore.getInstance();
    TextView balanceTv;
    ExtendedFloatingActionButton addMoney;
    ProgressBar balance_pb;
    ListenerRegistration listenerRegistration;

    public HomeFragment() {
        // Required empty public constructor
    }

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        balanceTv = view.findViewById(R.id.home_balance);
        addMoney = view.findViewById(R.id.home_add_money);
        balance_pb = view.findViewById(R.id.home_balance_pb);

        Sprite wave = new ThreeBounce();
        balance_pb.setIndeterminateDrawable(wave);
        ((MainActivity)getActivity()).setTitle("Hi, " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setBalanceTv();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    public void setBalanceTv() {
        listenerRegistration = mRef.collection("user").document(mAuth.getUid())
                .collection("wallet").document("wallet")
                .addSnapshotListener((documentSnapshot, e) -> {
                    {
                        Wallet wallet = documentSnapshot.toObject(Wallet.class);
                        balanceTv.setText("\u20B9" + wallet.getBalance().toString());
                        balance_pb.setVisibility(View.GONE);
                        addMoney.setVisibility(View.VISIBLE);
                    }
                });
        Utils.registrations.add(listenerRegistration);
    }
}