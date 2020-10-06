package com.visionio.sabpay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.visionio.sabpay.R;
import com.visionio.sabpay.models.CompressedItem;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.InvoiceViewHolder> {

    List<CompressedItem> items;
    int position = 0;
    Context context;

    public OrderItemAdapter(List<CompressedItem> items, Context context) {
        this.items = items;
        this.context = context;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) { return position; }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InvoiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.invoice_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        CompressedItem current = items.get(position);

        holder.title.setText(current.getTitle());
        holder.quantity.setText(String.format("Qty %s", current.getQty()));
        holder.cost.setText(String.format("\u20B9%s", current.getCost()));
        Glide.with(context).load(current.getImg()).centerCrop().into(holder.image);
    }

    @Override
    public int getItemViewType(int position) { return position; }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void add(CompressedItem item) {
        items.add(item);
        notifyItemInserted(position++);
    }

    public void setList(List<CompressedItem> list){
        this.items = list;
    }

    public class InvoiceViewHolder extends RecyclerView.ViewHolder{
        TextView title, quantity, cost;
        ImageView image;
        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.invoice_list_item_title);
            quantity = itemView.findViewById(R.id.invoice_list_item_quantity_tv);
            cost = itemView.findViewById(R.id.invoice_list_item_price_tv);
            image = itemView.findViewById(R.id.invoice_list_item_image_iv);

        }
    }

}
