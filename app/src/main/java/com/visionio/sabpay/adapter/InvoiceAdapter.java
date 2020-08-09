package com.visionio.sabpay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;
import com.visionio.sabpay.models.Item;
import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder> {

    List<Item> items;
    int position = 0;

    public InvoiceAdapter(List<Item> items) {
        this.items = items;
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
        Item current = items.get(position);

        holder.title.setText(current.getTitle());
        holder.quantity.setText(String.valueOf(current.getQty()));
    }

    @Override
    public int getItemViewType(int position) { return position; }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void add(Item item) {
        items.add(item);
        notifyItemInserted(position++);
    }

    public void setList(List<Item> list){
        this.items = list;
    }

    public class InvoiceViewHolder extends RecyclerView.ViewHolder{
        TextView title, quantity;
        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.invoice_list_item_title);
            quantity = itemView.findViewById(R.id.invoice_list_item_quantity);

        }
    }

}
