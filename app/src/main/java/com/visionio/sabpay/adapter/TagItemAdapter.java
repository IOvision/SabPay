package com.visionio.sabpay.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class TagItemAdapter extends RecyclerView.Adapter<TagItemAdapter.TagItemViewHolder> {
    Context context;
    List<String> tags;
    HashMap<String, String> urls;

    public TagItemAdapter(Context context, List<String> tags, HashMap<String, String> urls) {
        this.context = context;
        this.tags = tags;
        this.urls = urls;
    }


    @NonNull
    @Override
    public TagItemAdapter.TagItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tags_list_item, parent, false);

        return new TagItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TagItemAdapter.TagItemViewHolder holder, int position) {
        holder.name.setText(tags.get(position));
        // TODO: load im
        URL url = null;
        try {
            url = new URL(urls.get(tags.get(position)));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Bitmap mIcon_val = null;
        try {
            mIcon_val = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder.img.setImageBitmap(mIcon_val);
    }

    @Override
    public int getItemCount() {
        return 0;
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
