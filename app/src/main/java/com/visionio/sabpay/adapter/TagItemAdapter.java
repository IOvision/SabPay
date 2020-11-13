package com.visionio.sabpay.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.TagsCategory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class TagItemAdapter extends RecyclerView.Adapter<TagItemAdapter.TagItemViewHolder> {
    Context context;
    List<TagsCategory> tags;
    OnItemClickListener<TagsCategory> clickListener;

    public TagItemAdapter(Context context, List<TagsCategory> tags, OnItemClickListener<TagsCategory> clickListener) {
        this.context = context;
        this.tags = tags;
        this.clickListener = clickListener;
        Log.d("testing", "TagItemAdapter: " + tags.size());
    }


    @NonNull
    @Override
    public TagItemAdapter.TagItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tags_list_item, parent, false);
        return new TagItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TagItemAdapter.TagItemViewHolder holder, int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onItemClicked(tags.get(position), position, holder.itemView);
            }
        });
        holder.name.setText(tags.get(position).getId());
        Glide
            .with(holder.itemView)
            .load(tags.get(position).getImage())
            .centerCrop()
            .into(holder.img);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public class TagItemViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name;

        public TagItemViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.tags_list_item_image);
            name = itemView.findViewById(R.id.tags_list_item_name);
        }
    }

}
