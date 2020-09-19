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
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Inventory;
import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    Context context;
    List<Inventory> inventoryList;

    OnItemClickListener<Inventory> clickListener;

    public List<Inventory> getInventoryList() {
        return inventoryList;
    }

    public void setInventoryList(List<Inventory> inventoryList) {
        this.inventoryList = inventoryList;
        notifyDataSetChanged();
    }

    public OnItemClickListener<Inventory> getClickListener() {
        return clickListener;
    }

    public void setClickListener(OnItemClickListener<Inventory> clickListener) {
        this.clickListener = clickListener;
    }

    public InventoryAdapter(Context context, ArrayList<Inventory> inventoryList) {
        this.context = context;
        this.inventoryList = inventoryList;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.inventory_list_item, parent, false);
        return new InventoryViewHolder(v);
    }

    public void onBindViewHolder(@NonNull InventoryAdapter.InventoryViewHolder holder, final int position) {
        final Inventory current = inventoryList.get(position);
        holder.inventoryName.setText(current.getName());
        holder.inventoryAddress.setText(current.getAddress());
        holder.symbol.setSliderAdapter(new SimpleImageAdapter(context){{
            setImageUrls(current.getImages());
        }});
        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(clickListener==null){
                    return;
                }
                clickListener.onItemClicked(current, position, holder.v);
            }
        });
    }

    @Override
    public int getItemCount () {
        return inventoryList.size();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder{

        TextView inventoryName, inventoryAddress, quantity;
        SliderView symbol;

        View v;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            inventoryName = itemView.findViewById(R.id.item_inventoryName_tv);
            inventoryAddress = itemView.findViewById(R.id.item_inventoryAddress_tv);
            quantity = itemView.findViewById(R.id.item_qty_tv);
            symbol = itemView.findViewById(R.id.items_image_sv);
        }
    }

}
