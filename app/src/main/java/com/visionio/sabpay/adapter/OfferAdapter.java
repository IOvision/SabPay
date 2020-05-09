package com.visionio.sabpay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.Models.Contact;
import com.visionio.sabpay.Models.Offer;
import com.visionio.sabpay.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    private ArrayList<Offer> offerArrayList;

    public OfferAdapter(ArrayList<Offer> offerArrayList){
        this.offerArrayList = offerArrayList;
    }

    public void add(Offer offer){
        offerArrayList.add(offer);
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /////////////////////////////////////////////////////////
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.offer_list_item, parent, false);
        return new OfferViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer currentOffer = offerArrayList.get(position);

        holder.name.setText(currentOffer.getOfferName());
        holder.description.setText(currentOffer.getOfferDescription());

    }


    @Override
    public int getItemCount() {
        return offerArrayList.size();
    }


    public class OfferViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView description;
        View itemView;

        public OfferViewHolder(@NonNull View itemView){
            super(itemView);
            this.itemView = itemView;
            ///////////////////////////////////////////////////////////////////////////
            name = itemView.findViewById(R.id.offer_item_name_tv);
            description = itemView.findViewById(R.id.offer_item_description_tv);
        }
    }

}
