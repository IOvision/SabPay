package com.visionio.sabpay.main;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.visionio.sabpay.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class OfferFragment extends Fragment {

    public OfferFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers, container, false);
        ((MainActivity)getActivity()).setTitle("Offer");
        return view;
    }
}
