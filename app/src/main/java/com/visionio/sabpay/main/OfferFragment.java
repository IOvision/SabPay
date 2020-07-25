package com.visionio.sabpay.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.visionio.sabpay.R;
import com.visionio.sabpay.adapter.OfferAdapter;
import com.visionio.sabpay.models.Offer;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class OfferFragment extends Fragment {

    RecyclerView recyclerView;
    ArrayList<Offer> yo;
    OfferAdapter offerAdapter;

    public OfferFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers, container, false);
        ((MainActivity)getActivity()).setTitle("Offer");
        offerAdapter = new OfferAdapter(new ArrayList<Offer>());

        Offer a = new Offer("Test Offer1", "This offer is for testing, it serves no real purpose.");
        Offer b = new Offer("Test Offer2", "This offer is also for testing, it also serves no real purpose.");
        offerAdapter.add(a);
        offerAdapter.add(b);

        recyclerView = view.findViewById(R.id.offer_recycler);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(offerAdapter);
    }
}
