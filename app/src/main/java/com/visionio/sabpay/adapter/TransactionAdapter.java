package com.visionio.sabpay.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.visionio.sabpay.Models.OfflineTransaction;
import com.visionio.sabpay.Models.Transaction;
import com.visionio.sabpay.Models.User;
import com.visionio.sabpay.R;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    List<OfflineTransaction> transactions;

    FirebaseAuth mAuth;

    public TransactionAdapter(List<OfflineTransaction> transactions) {
        this.transactions = transactions;
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_list_item, parent, false);
        return new TransactionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        OfflineTransaction current = transactions.get(position);
        setData(current, holder);

    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void add(OfflineTransaction transaction){
        transactions.add(transaction);
        notifyDataSetChanged();
    }

    private void setData(OfflineTransaction current, TransactionViewHolder holder){
        User from = current.getFrom();
        User to = current.getTo();

        if(to.getUid().equals(mAuth.getUid())){
            // received case
            holder.description.setText("Receiver from:\n"+from.getName());
            holder.amount.setTextColor(Color.GREEN);
            holder.amount.setText("+ Rs. "+current.getAmount());
            holder.imageView.setRotation(180);
        }else{
            // sent case
            holder.description.setText("Sent to:\n"+to.getName());
            holder.amount.setText("- Rs. "+current.getAmount());
        }

        holder.dateTime.setText(current.getDate());
    }



    public class TransactionViewHolder extends RecyclerView.ViewHolder{

        ImageView imageView;
        TextView description;
        TextView amount;
        TextView dateTime;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.transaction_list_item_icon_iv);
            description = itemView.findViewById(R.id.transaction_list_item_description_tv);
            amount = itemView.findViewById(R.id.transaction_list_item_amount_tv);
            dateTime = itemView.findViewById(R.id.transaction_list_item_dateTime_tv);
        }
    }

}
