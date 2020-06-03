package com.visionio.sabpay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.visionio.sabpay.models.Offer;
import com.visionio.sabpay.adapter.OfferAdapter;

import java.util.ArrayList;

public class OfferDisplayActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    //RecyclerView.Adapter adapter;
    OfferAdapter adapter;
    RecyclerView.LayoutManager layoutManager;
    //OfferAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_display);

        recyclerView = findViewById(R.id.offerDisplay_activity_offer_rv);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);


        adapter = new OfferAdapter(new ArrayList<Offer>());

        recyclerView.setAdapter(adapter);

        String name = "Name";
        String description = "Description";

        Offer offer = new Offer(name, description);

        adapter.add(offer);


    }
}
