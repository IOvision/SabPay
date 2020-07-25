package com.visionio.sabpay.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.visionio.sabpay.R;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class InventoryFragment extends Fragment {

    private int locationRequestCode = 1000;
    double within = 6;

    FirebaseFirestore mRef;



    public InventoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invnetory, container, false);
        ((MainActivity)getActivity()).setTitle("Inventory");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRef = FirebaseFirestore.getInstance();
        accessLocation();
    }

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

        Query query = mRef.collection("inventory")
                .whereGreaterThan("location", lesserGeopoint)
                .whereLessThan("location", greaterGeopoint);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                    Log.i("test ", "onComplete: test "+snapshots.size());
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
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    try{
                        int latestIdx = locationResult.getLocations().size() - 1;
                        Location location = locationResult.getLocations().get(latestIdx);
                        Log.i("test", "Lattitude: " + location.getLatitude());
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
        client.requestLocationUpdates(lr, callback, Looper.getMainLooper());

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
