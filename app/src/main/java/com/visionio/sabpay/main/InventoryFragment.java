package com.visionio.sabpay.main;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.InventoryAdapter;
import com.visionio.sabpay.api.ApiBody;
import com.visionio.sabpay.api.API;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Inventory;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventoryFragment extends Fragment {

    private int locationRequestCode = 1000;
    double within = 3;
    private RecyclerView recyclerView;
    InventoryAdapter adapter;
    FirebaseFirestore mRef;
    ArrayList<Inventory> inventoryArrayList;
    Inventory inventory;
    public InventoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invnetory, container, false);
        recyclerView = view.findViewById(R.id.inventory_recycler);
        ((MainActivity)getActivity()).setTitle("Inventory");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRef = FirebaseFirestore.getInstance();
        inventoryArrayList = new ArrayList<>();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        // todo: fix access location and use it instead of hard coded lat and long
          accessLocation();
//        double lat = 25.283307;
//        double lon = 83.003229;


        //getNearbyInventory(lat, lon, within);
        adapter = new InventoryAdapter(getActivity(), inventoryArrayList);


        adapter.setClickListener(new OnItemClickListener<Inventory>() {
            @Override
            public void onItemClicked(Inventory object, int position, View view) {
                Intent intent = new Intent(getActivity(), InventoryActivity.class);
                String json = object.getJson();
                intent.putExtra("inventory", json);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        //placeOrder();
    }

//    private void loadInventory() {
//
//        adapter = new InventoryAdapter(getActivity(), inventoryArrayList);
//        recyclerView.setAdapter(adapter);
//        if(inventoryArrayList.size() > 0) {
//            inventoryArrayList.clear();
//        }
//        mRef.collection("inventory").get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if(task.isSuccessful()) {
//                            for(DocumentSnapshot querySnapshot: task.getResult()) {
//                                inventory = querySnapshot.toObject(Inventory.class);
//                                inventoryArrayList.add(inventory);
//                            }
//                            adapter = new InventoryAdapter(getActivity(), inventoryArrayList);
//                            recyclerView.setAdapter(adapter);
//                        } else {
//                             Log.d("inventory", task.getException().getLocalizedMessage());
//                        }
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(getActivity(), "problem***********" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

    private void getNearbyInventory(double latitude, double longitude, double distance){
        //gets inventory within distance km
        // ~1 mile of lat and lon in degrees
        distance = distance/1.609; // converting km to miles equivalent
        double lat = 0.0144927536231884;
        double lon = 0.0181818181818182;

        double lowerLat = latitude - (lat * distance);
        double lowerLon = longitude - (lon * distance);

        double greaterLat = latitude + (lat * distance);
        double greaterLon = longitude + (lon * distance);

        GeoPoint lesserGeopoint = new GeoPoint(lowerLat, lowerLon);
        GeoPoint greaterGeopoint = new GeoPoint(greaterLat, greaterLon);

        if(inventoryArrayList.size() > 0) {
            inventoryArrayList.clear();
        }

        Query query = mRef.collection("inventory")
                .whereGreaterThan("location", lesserGeopoint)
                .whereLessThan("location", greaterGeopoint);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    //List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                    for(DocumentSnapshot querySnapshot: task.getResult()) {
                        inventory = querySnapshot.toObject(Inventory.class);
                        inventoryArrayList.add(inventory);
                    }
                    adapter.setInventoryList(inventoryArrayList);
                }else{
                    Log.i("test ", "onComplete: test "+task.getException().getLocalizedMessage());
                }
            }
        });
    }

    void accessLocation(){
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(getContext());

        LocationRequest lr = new LocationRequest();
        lr.setInterval(10000);
        lr.setFastestInterval(3000);
        lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (client != null) {
                    client.removeLocationUpdates(this);
                }
                if(locationResult == null) {
                    Log.d("test", "no latitude and longitude");
                }
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    try{
                        int latestIdx = locationResult.getLocations().size() - 1;
                        Location location = locationResult.getLocations().get(latestIdx);
                        Log.i("test", "Latitude: " + location.getLatitude());
                        Log.i("test","Longitude: " + location.getLongitude());
                        double test_lat = location.getLatitude();
                        double test_long = location.getLongitude();
                        getNearbyInventory(test_lat, test_long, within);
                    }catch (Exception e){
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    locationRequestCode);
            return;
        }
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getContext());
            alert.setTitle("Location Access Request");
            alert.setMessage("Press Ok to turn on location. ");
            alert.setPositiveButton("Ok", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
            alert.setNegativeButton("No", (dialog, which) -> {
                Toast.makeText(getActivity(), "Please turn on location to proceed.", Toast.LENGTH_SHORT).show();
            });
            alert.show();
        } else {
            client.requestLocationUpdates(lr, callback, Looper.getMainLooper());
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessLocation();
                } else {
                    Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

}
