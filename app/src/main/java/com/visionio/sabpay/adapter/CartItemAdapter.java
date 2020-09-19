package com.visionio.sabpay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.CartListener;
import com.visionio.sabpay.models.Item;

import java.util.List;
import java.util.Map;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    List<Item> itemList;
    Map<String, Integer> quantity;
    Context context;
    CartListener clickListener;

    public CartItemAdapter(List<Item> itemList, Context context, Map<String, Integer> quantity) {
        this.itemList = itemList;
        this.context = context;
        this.quantity = quantity;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }


    public void setClickListener(CartListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CartItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        Item curr = itemList.get(position);

        holder.detail_tv.setText(String.format("%s ₹%s/%s", curr.getTitle(), curr.getCost(), curr.getUnit()));
        holder.qty.setText(String.valueOf(quantity.get(curr.getId())));
        holder.increase_btn.setOnClickListener(v -> {
            clickListener.onIncreaseQty(curr);
            holder.qty.setText(String.valueOf(quantity.get(curr.getId())));
        });
        holder.decrease_btn.setOnClickListener(v -> {
            clickListener.onDecreaseQty(curr);
            holder.qty.setText(String.valueOf(quantity.get(curr.getId())));
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class CartItemViewHolder extends RecyclerView.ViewHolder{

        TextView detail_tv;
        Button qty, increase_btn, decrease_btn;
        View v;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            detail_tv = itemView.findViewById(R.id.cart_item_detail_tv);
            qty = itemView.findViewById(R.id.cart_item_qty_text);
            increase_btn = itemView.findViewById(R.id.cart_item_increase_qty);
            decrease_btn = itemView.findViewById(R.id.cart_item_decrease_qty);
        }
    }
}
