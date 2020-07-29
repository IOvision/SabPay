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
import com.visionio.sabpay.models.Inventory;
import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    Context context;
    List<Inventory> inventoryList;

    public List<Inventory> getInventoryList() {
        return inventoryList;
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
        holder.inventoryLocation.setText(String.valueOf(current.isOpened()));
        holder.symbol.setSliderAdapter(new SimpleImageAdapter(context){{
            setImageUrls(current.getImages());
        }});
    }

    @Override
    public int getItemCount () {
        return inventoryList.size();
    }

    public class InventoryViewHolder extends RecyclerView.ViewHolder{

        TextView inventoryName, inventoryLocation, quantity;
        SliderView symbol;

        View v;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            inventoryName = itemView.findViewById(R.id.item_inventoryName_tv);
            inventoryLocation = itemView.findViewById(R.id.item_inventoryLocation_tv);
            quantity = itemView.findViewById(R.id.item_qty_tv);
            symbol = itemView.findViewById(R.id.items_image_sv);
        }
    }

}
