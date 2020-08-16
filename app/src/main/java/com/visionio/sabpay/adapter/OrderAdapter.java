package com.visionio.sabpay.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    List<Order> orders;
    int position = 0;
    OnItemClickListener<Order> itemTouchListener;

    public OrderAdapter(List<Order> orders, OnItemClickListener<Order> recyclerItemTouchListener) {
        this.orders = orders;
        setHasStableIds(true);
        itemTouchListener = recyclerItemTouchListener;
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
    public OrderAdapter.OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.order_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderAdapter.OrderViewHolder holder, int position) {
        final Order current = orders.get(position);
        holder.itemView.setOnClickListener(v -> {
            itemTouchListener.onItemClicked(current, position, v);
        });
        holder.inventoryFrom.setText(current.getFromInventoryName());
        holder.date.setText(String.valueOf(current.getTimestamp()));
        holder.date.setText(String.valueOf(current.getTimestamp().toDate()).substring(0, 20));
        holder.status.setText(current.getStatus());
        if(holder.status.getText().toString().equalsIgnoreCase(Order.STATUS_ORDER_CANCELLED)){
            holder.status.setTextColor(Color.RED);
        } else if(holder.status.getText().toString().equalsIgnoreCase(Order.STATUS_ORDER_COMPLETED)) {
            holder.status.setTextColor(Color.GREEN);
        } else if(holder.status.getText().toString().equalsIgnoreCase(Order.STATUS_PAYMENT_PENDING)) {
            holder.status.setTextColor(Color.argb(255, 255, 165, 0));
        } else {
            holder.status.setTextColor(Color.argb(255, 64,224,208));
        }
        holder.amount.setText("\u20B9" + current.getAmount());
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void add(Order order) {
        orders.add(order);
        notifyItemInserted(position++);
    }

    public void allClear() {
        orders.clear();
        position = 0;
        notifyDataSetChanged();
    }

    public class OrderViewHolder extends RecyclerView.ViewHolder{

        TextView inventoryFrom, status, amount, date;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            inventoryFrom = itemView.findViewById(R.id.order_list_item_inventory_from);
            status = itemView.findViewById(R.id.order_list_item_status);
            amount = itemView.findViewById(R.id.order_list_item_amount);
            date = itemView.findViewById(R.id.order_list_item_date);
        }
    }
}
