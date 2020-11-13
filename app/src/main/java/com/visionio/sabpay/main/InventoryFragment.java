package com.visionio.sabpay.main;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.CartItemAdapter;
import com.visionio.sabpay.adapter.InventoryAdapter;
import com.visionio.sabpay.adapter.TagItemAdapter;
import com.visionio.sabpay.api.ApiBody;
import com.visionio.sabpay.api.API;
import com.visionio.sabpay.api.SabPayNotify;
import com.visionio.sabpay.interfaces.CartListener;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Cart;
import com.visionio.sabpay.models.CompressedItem;
import com.visionio.sabpay.models.Inventory;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Order;
import com.visionio.sabpay.models.TagsCategory;
import com.visionio.sabpay.models.User;
import com.visionio.sabpay.models.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.paperdb.Paper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventoryFragment extends Fragment {

    private RecyclerView recyclerView;
    FirebaseFirestore mRef;
    LottieAnimationView animation;

    TagItemAdapter adapter;
    ArrayList<TagsCategory> mArrayList;

    public InventoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invnetory, container, false);
        recyclerView = view.findViewById(R.id.inventory_recycler);
        animation = view.findViewById(R.id.animationView);
        ((MainActivity)getActivity()).setTitle("Store");
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRef = FirebaseFirestore.getInstance();
        mArrayList = new ArrayList<>();
        mRef.collection("tags").get()
                .addOnSuccessListener(documentSnapshots -> {
                    if (documentSnapshots.isEmpty()) {
                        Log.d("testing", "onViewCreated: List Empty");
                        return;
                    }
                    for(DocumentSnapshot documentSnapshot: documentSnapshots) {
                        TagsCategory tagsCategory = documentSnapshot.toObject(TagsCategory.class);
                        mArrayList.add(tagsCategory);
                    }
                    animation.setVisibility(View.GONE);
                    recyclerView.setHasFixedSize(true);
                    recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
                    adapter = new TagItemAdapter(view.getContext(), mArrayList, new OnItemClickListener<TagsCategory>() {
                        @Override
                        public void onItemClicked(TagsCategory object, int position, View view) {
                            Bundle b = new Bundle();
                            b.putStringArrayList("tags", object.getTags());
                            b.putString("header", object.getId());
                            Intent i = new Intent(getActivity(), InventoryActivity.class);
                            i.putExtras(b);
                            startActivity(i);
                        }
                    });
                    recyclerView.setAdapter(adapter);
                });
    }
}
