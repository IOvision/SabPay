package com.visionio.sabpay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

public class SearchListAdapter extends RecyclerView.Adapter<SearchListAdapter.SearchListViewHolder> {

    List<String> name;
    OnItemClickListener<String> listener;

    public SearchListAdapter(ArrayList<String> arrayList, OnItemClickListener<String> listener) {
        this.name = arrayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SearchListAdapter.SearchListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.inventory_search_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SearchListViewHolder holder, int position) {
        String s = name.get(position);
        holder.name_tv.setText(s.split(":")[0]);
        holder.itemView.setOnClickListener(view -> {
            this.listener.onItemClicked(s.split(":")[1], position, holder.itemView);
        });
    }

    @Override
    public int getItemCount() {
        return name.size();
    }

    public void clear() {
        int pos = name.size();
        name.clear();
        notifyItemRangeRemoved(0,pos);
    }

    public void add(String s) {
        name.add(s);
    }

    public class SearchListViewHolder extends RecyclerView.ViewHolder {

        TextView name_tv;

        public SearchListViewHolder(@NonNull View itemView) {
            super(itemView);
            name_tv = itemView.findViewById(R.id.inventory_search_name);
        }
    }
}
