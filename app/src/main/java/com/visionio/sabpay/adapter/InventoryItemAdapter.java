package com.visionio.sabpay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarteist.autoimageslider.SliderView;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.CartListener;
import com.visionio.sabpay.models.Item;

import java.util.ArrayList;
import java.util.List;

public class InventoryItemAdapter  extends RecyclerView.Adapter<InventoryItemAdapter.InventoryItemViewHolder> {
    Context context;
    List<Item> itemList;
    CartListener clickListener;
    String inventoryId;

    public void setItemList(List<Item> itemList) {
        if(this.itemList==null){
            this.itemList = itemList;
        }
        else {
            this.itemList.addAll(itemList);
        }
        notifyDataSetChanged();
    }

    public void setClickListener(CartListener clickListener) {
        this.clickListener = clickListener;
    }


    public InventoryItemAdapter(Context context, ArrayList<Item> inventoryList, String inventoryId) {
        this.context = context;
        this.itemList = inventoryList;
        this.inventoryId = inventoryId;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public String get_last_title(){
        if(itemList==null | getItemCount()==0){
            return "+";
        }
        return itemList.get(getItemCount()-1).getTitle();
    }

    @NonNull
    @Override
    public InventoryItemAdapter.InventoryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.inventorty_item_list_item, parent, false);
        return new InventoryItemAdapter.InventoryItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryItemViewHolder holder, int position) {
        final Item current = itemList.get(position);
        holder.inventoryName.setText(current.getTitle());
        holder.quantity.setText(String.format("Stock: %s", current.getQty()));
        holder.inventoryLocation.setText(String.format("Rs. %s/%s", current.getCost(inventoryId), current.getUnit()));
        holder.symbol.setSliderAdapter(new SimpleImageAdapter(context) {{
            setImageUrls(current.getImages());
        }});
        holder.v.setOnClickListener(v -> {
            if(clickListener==null){
                return;
            }
            clickListener.onIncreaseQty(current);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public class InventoryItemViewHolder extends RecyclerView.ViewHolder {

        TextView inventoryName, inventoryLocation, quantity;
        SliderView symbol;

        View v;

        public InventoryItemViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            inventoryName = itemView.findViewById(R.id.item_inventoryName_tv);
            inventoryLocation = itemView.findViewById(R.id.item_inventoryLocation_tv);
            quantity = itemView.findViewById(R.id.item_qty_tv);
            symbol = itemView.findViewById(R.id.items_image_sv);
        }
    }
}