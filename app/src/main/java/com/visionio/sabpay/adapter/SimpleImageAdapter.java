package com.visionio.sabpay.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.smarteist.autoimageslider.SliderViewAdapter;
import com.visionio.sabpay.R;

import java.util.ArrayList;
import java.util.List;

public class SimpleImageAdapter extends SliderViewAdapter<SimpleImageAdapter.ImageViewHolder> {

    Context context;
    List<String> imageUrls;

    public SimpleImageAdapter(Context context) {
        this.context = context;
        imageUrls = new ArrayList<>();
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_image_view, parent, false);
        return new ImageViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder viewHolder, int position) {
        Glide.with(context).load(imageUrls.get(position)).centerCrop().into(viewHolder.itemView);
    }

    void add(String url){
        imageUrls.add(url);
        notifyDataSetChanged();
    }

    public void setImageUrls(List<String> imageUrls) {
        for(String url: imageUrls){
            add(url);
        }
    }

    @Override
    public int getCount() {
        return imageUrls.size();
    }

    class ImageViewHolder extends SliderViewAdapter.ViewHolder {

        ImageView itemView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView.findViewById(R.id.imageView);
        }
    }
}
